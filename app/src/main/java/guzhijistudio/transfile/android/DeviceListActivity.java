package guzhijistudio.transfile.android;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import guzhijistudio.transfile.identity.UdpServer;
import guzhijistudio.transfile.utils.Constants;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

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
                    Intent i = new Intent();
                    i.putExtra("device_ip", device.getIp());
                    DeviceListActivity.this.setResult(RESULT_OK, i);
                    finish();
                }
            });
            return view;
        }
    }

    private final Handler handler = new Handler();
    private final ArrayList<DeviceItem> devices = new ArrayList<>();
    private final UdpServer.UdpServerListener usListener = new UdpServer.UdpServerListener() {
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
    private ListView deviceListView;
    private UdpServer server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        deviceListView = findViewById(R.id.deviceListView);
        deviceListView.setAdapter(new DevicesListAdaptor());

        try {
            SocketAddress addr = new InetSocketAddress("0.0.0.0", Constants.IDENTITY_SERVER_PORT);
            String sGroupAddr = getSharedPreferences("config", MODE_PRIVATE)
                    .getString("group_addr", Constants.IDENTITY_GROUP_ADDR);
            InetAddress groupAddr = InetAddress.getByName(sGroupAddr);
            server = new UdpServer(addr, groupAddr, usListener);
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
        if (server != null)
            server.shutdown();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
