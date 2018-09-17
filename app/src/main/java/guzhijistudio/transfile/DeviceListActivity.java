package guzhijistudio.transfile;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import guzhijistudio.transfile.identityman.Broadcaster;
import guzhijistudio.transfile.identityman.UdpServer;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class DeviceListActivity extends AppCompatActivity {

    private static class DeviceItem {
        private String ip;
        private String name;

        public DeviceItem(String ip, String name) {
            this.ip = ip;
            this.name = name;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public int hashCode() {
            return ip == null ? 0 : ip.hashCode();
        }

        @Override
        public boolean equals(Object another) {
            String ip2;
            if (another instanceof DeviceItem)
                ip2 = ((DeviceItem) another).ip;
            else if (another instanceof String)
                ip2 = (String) another;
            else
                return false;
            return ip != null && ip.equals(ip2);
        }
    }

    private class DevicesListAdaptor extends BaseAdapter {

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int i) {
            return devices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = View.inflate(DeviceListActivity.this, R.layout.listview_item_device, null);
            }
            TextView deviceNameText = view.findViewById(R.id.deviceNameText);
            TextView deviceIpText = view.findViewById(R.id.deviceIpText);
            final DeviceItem device = devices.get(i);
            deviceNameText.setText(device.getName());
            deviceIpText.setText(device.getIp());
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectedDeviceIp = device.getIp();
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(i, 1);
                }
            });
            return view;
        }
    }

    private final Handler handler = new Handler();
    private final ArrayList<DeviceItem> devices = new ArrayList<>();
    private FileReceiver fileReceiver;
    private UdpServer server;
    private Broadcaster broadcaster;
    private Dialog progressDialog;
    private String selectedDeviceIp = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        checkPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, true, 1);

        final ListView deviceListView = findViewById(R.id.deviceListView);
        deviceListView.setAdapter(new DevicesListAdaptor());

        progressDialog = new Dialog(this, R.style.ShadedDialog);
        progressDialog.setContentView(R.layout.dialog_progress);
        progressDialog.setCancelable(false);

        FileReceiver.FileReceiverListener frListener = new FileReceiver.FileReceiverListener() {
            @Override
            public void onFile(final String filename) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                        Toast.makeText(getApplicationContext(), filename + " received successfully", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onMsg(final String msg) {
                showMessageFromThread(msg);
            }

            @Override
            public void onError(final String msg) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onProgress(final long received, final long total) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!progressDialog.isShowing()) progressDialog.show();
                        ProgressBar bar = progressDialog.findViewById(R.id.progressBar);
                        bar.setMax(10000);
                        bar.setProgress((int) (10000.0 * received / total));
//                        TextView txt = progressDialog.findViewById(R.id.textView);
//                        txt.setText(Math.round(10000.0f * received / total) / 100.0 + "%");
                    }
                });
            }
        };

        UdpServer.UdpServerListener usListener = new UdpServer.UdpServerListener() {
            @Override
            public void onEnter(String ip, String name) {
                Log.i("udpserver", "enter:" + ip + "," + name);
                final DeviceItem item = new DeviceItem(ip, name);
                if (!devices.contains(item)) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            devices.add(item);
                            deviceListView.setAdapter(new DevicesListAdaptor());
                        }
                    });
                }
            }

            @Override
            public void onQuit(final String ip) {
                Log.i("udpserver", "quit:" + ip);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        devices.remove(new DeviceItem(ip, ""));
                        deviceListView.setAdapter(new DevicesListAdaptor());
                    }
                });
            }
        };

        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (!iface.isLoopback()) {
                    Enumeration<InetAddress> addrs = iface.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        Log.i("localaddr", iface.getDisplayName() + ":" + addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        try {
            fileReceiver = new FileReceiver(8889,
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    frListener);
            fileReceiver.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String deviceName = getIntent().getStringExtra("device_name");
        SocketAddress groupAddr = new InetSocketAddress("224.0.0.255", 8888);
        broadcaster = new Broadcaster(deviceName, groupAddr);
        broadcaster.start();
        try {
            SocketAddress addr = new InetSocketAddress("0.0.0.0", 8888);
            InetAddress group = InetAddress.getByName("224.0.0.255");
            server = new UdpServer(addr, group, usListener);
            server.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fileReceiver != null)
            fileReceiver.shutdown();
        if (broadcaster != null)
            broadcaster.shutdown();
        if (server != null)
            server.shutdown();
    }

    public static String getPath(Context context, Uri uri) {
        Log.i("filesender", uri.toString());
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { MediaStore.MediaColumns.DATA };
            Cursor cursor;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null) {
                    try {
                        int column_index = cursor.getColumnIndexOrThrow(projection[0]);
                        if (cursor.moveToFirst()) {
                            return cursor.getString(column_index);
                        }
                    } finally {
                        cursor.close();
                    }
                }
            } catch (Exception e) {
                Log.e("filesender", e.getMessage());
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    private void showMessageFromThread(final String msg) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    protected boolean checkPermissions(String[] permissions, boolean doRequest, int requestCode) {
        ArrayList<String> toRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                toRequest.add(permission);
        }
        if (toRequest.isEmpty()) return true;
        if (doRequest)
            ActivityCompat.requestPermissions(this, toRequest.toArray(new String[0]), requestCode);
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK && data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        String filepath = getPath(getApplicationContext(), uri);
                        if (filepath != null) {
                            progressDialog.show();
                            FileSender.FileSenderListener listener = new FileSender.FileSenderListener() {
                                @Override
                                public void onFileSent(final String filename) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            progressDialog.dismiss();
                                            Toast.makeText(getApplicationContext(), filename + " sent successfully", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }

                                @Override
                                public void onError(final String msg) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            progressDialog.dismiss();
                                            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }

                                @Override
                                public void onProgress(final long sent, final long total) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            ProgressBar bar = progressDialog.findViewById(R.id.progressBar);
                                            bar.setMax(10000);
                                            bar.setProgress((int) (10000.0 * sent / total));
//                                            TextView txt = progressDialog.findViewById(R.id.textView);
//                                            txt.setText(Math.round(10000.0f * sent / total) / 100.0 + "%");
                                        }
                                    });
                                }
                            };
                            new FileSender(selectedDeviceIp, 8889, filepath, listener).start();
                        } else {
                            Toast.makeText(getApplicationContext(), "file path not found", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

}
