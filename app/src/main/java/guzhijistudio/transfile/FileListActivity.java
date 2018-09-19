package guzhijistudio.transfile;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import guzhijistudio.transfile.identityman.Broadcaster;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileListActivity extends AppCompatActivity {

    private static class FileItem {
        final private File file;
        private boolean done;

        public FileItem(File file) {
            this.file = file;
        }

        public File getFile() {
            return file;
        }

        public boolean isDone() {
            return done;
        }

        public void setDone(boolean done) {
            this.done = done;
        }

        @Override
        public int hashCode() {
            return file.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof FileItem)
                return file.equals(((FileItem) obj).file);
            if (obj instanceof File)
                return file.equals(obj);
            return false;
        }

    }

    private class FileListAdaptor extends BaseAdapter {

        @Override
        public int getCount() {
            return mode == 0 ? sendingFiles.size() : receivedFiles.size();
        }

        @Override
        public Object getItem(int i) {
            return mode == 0 ? sendingFiles.get(i) : receivedFiles.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = View.inflate(FileListActivity.this, R.layout.listview_item_file, null);
            }
            TextView fileNameText = view.findViewById(R.id.fileNameText);
            TextView fileSizeText = view.findViewById(R.id.fileSizeText);
            ProgressBar fileProgress = view.findViewById(R.id.fileProgress);
            FileItem file = mode == 0 ? sendingFiles.get(i) : receivedFiles.get(i);
            fileNameText.setText(file.getFile().getName());
            fileSizeText.setText(formatSize(file.getFile().length()));
            fileProgress.setMax(10000);
            fileProgress.setProgress(file.isDone() ? 10000 : 0);
            view.setTag(file.getFile());
            return view;
        }
    }

    private final Handler handler = new Handler();
    private final ArrayList<FileItem> sendingFiles = new ArrayList<>();
    private final ArrayList<FileItem> receivedFiles = new ArrayList<>();
    private String deviceIp = null;
    private int mode = 0;
    private FileReceiver fileReceiver;
    private Broadcaster broadcaster;
    final private ExecutorService fileSenders = Executors.newFixedThreadPool(2);
    final private FileReceiver.FileReceiverListener frListener = new FileReceiver.FileReceiverListener() {

        @Override
        public void onFileReceived(final File file) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    for (FileItem fileItem : receivedFiles) {
                        if (fileItem.getFile().equals(file)) {
                            fileItem.setDone(true);
                            break;
                        }
                    }
                    Toast.makeText(getApplicationContext(), file.getName() + " received", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onFile(final File file) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    FileItem fileItem = new FileItem(file);
                    if (!receivedFiles.contains(fileItem)) {
                        receivedFiles.add(fileItem);
                        if (mode == 1) fileListView.setAdapter(new FileListAdaptor());
                    }
                }
            });
        }

        @Override
        public void onMsg(String msg) {
            showMessageFromThread(msg);
        }

        @Override
        public void onError(String msg) {
            showMessageFromThread(msg);
        }

        @Override
        public void onProgress(final File file, final long received, final long total) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (mode == 1) {
                        View itemView = findFileItemView(file);
                        if (itemView != null) {
                            ProgressBar bar = itemView.findViewById(R.id.fileProgress);
                            bar.setMax(10000);
                            bar.setProgress((int) (10000.0 * received / total));
                            TextView txt = itemView.findViewById(R.id.fileSizeText);
                            txt.setText(String.format("%s / %s", formatSize(received), formatSize(total)));
                        }
                    }
                }
            });
        }
    };
    final private FileSender.FileSenderListener fsListener = new FileSender.FileSenderListener() {
        @Override
        public void onFileSent(final File file) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    for (FileItem fileItem : sendingFiles) {
                        if (fileItem.getFile().equals(file)) {
                            fileItem.setDone(true);
                            break;
                        }
                    }
                    Toast.makeText(getApplicationContext(), file.getName() + " sent", Toast.LENGTH_SHORT).show();
                }
            });
            showMessageFromThread(file.getName() + " sent");
        }

        @Override
        public void onError(final File file, final String msg) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (deviceIp != null && file != null) {
                        fileSenders.submit(new FileSender(deviceIp, 8889, file.getAbsolutePath(), fsListener));
                    }
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onProgress(final File file, final long sent, final long total) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (mode == 0) {
                        View itemView = findFileItemView(file);
                        if (itemView != null) {
                            ProgressBar bar = itemView.findViewById(R.id.fileProgress);
                            bar.setMax(10000);
                            bar.setProgress((int) (10000.0 * sent / total));
                            TextView txt = itemView.findViewById(R.id.fileSizeText);
                            txt.setText(String.format("%s / %s", formatSize(sent), formatSize(total)));
                        }
                    }
                }
            });
        }
    };
    final private BottomNavigationView.OnNavigationItemSelectedListener nItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.navigation_send:
                    mode = 0;
                    fileListView.setAdapter(new FileListAdaptor());
                    addFileButton.setVisibility(View.VISIBLE);
                    sendAllButton.setVisibility(View.VISIBLE);
                    return true;
                case R.id.navigation_receive:
                    mode = 1;
                    fileListView.setAdapter(new FileListAdaptor());
                    addFileButton.setVisibility(View.INVISIBLE);
                    sendAllButton.setVisibility(View.INVISIBLE);
                    return true;
            }
            return false;
        }
    };
    private Button addFileButton;
    private Button sendAllButton;
    private ListView fileListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        checkPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, true, 1);

        BottomNavigationView navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(nItemSelectedListener);

        addFileButton = findViewById(R.id.addFileButton);
        addFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                startActivityForResult(i, 1);
            }
        });

        sendAllButton = findViewById(R.id.sendAllButton);
        sendAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(FileListActivity.this, DeviceListActivity.class);
                startActivityForResult(i, 2);
            }
        });

        fileListView = findViewById(R.id.fileListView);
        fileListView.setAdapter(new FileListAdaptor());

        String deviceName = getIntent().getStringExtra("device_name");
        SocketAddress groupAddr = new InetSocketAddress("224.0.0.255", 8888);
        broadcaster = new Broadcaster(deviceName, groupAddr);
        broadcaster.start();

        try {
            fileReceiver = new FileReceiver(8889,
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    frListener);
            fileReceiver.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (broadcaster != null)
            broadcaster.shutdown();
        if (fileReceiver != null)
            fileReceiver.shutdown();
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

    private void showMessageFromThread(final String msg) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static String getPath(Context context, Uri uri) {
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

    private static String formatSize(long size) {
        float s = size;
        if (s < 1024)
            return Math.round(s * 100) / 100.0 + " bytes";
        s /= 1024;
        if (s < 1024)
            return Math.round(s * 100) / 100.0 + " Kb";
        s /= 1024;
        if (s < 1024)
            return Math.round(s * 100) / 100.0 + " Mb";
        s /= 1024;
        return Math.round(s * 100) / 100.0 + " Gb";
    }

    private View findFileItemView(File file) {
        for (int i = 0; i < fileListView.getChildCount(); i++) {
            View itemView = fileListView.getChildAt(i);
            if (file.equals(itemView.getTag()))
                return itemView;
        }
        return null;
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
                            File file = new File(filepath);
                            FileItem fileItem = new FileItem(file);
                            if (!sendingFiles.contains(fileItem)) {
                                sendingFiles.add(fileItem);
                                fileListView.setAdapter(new FileListAdaptor());
                            }
                        }
                    }
                }
                break;
            case 2:
                if (resultCode == RESULT_OK && data != null) {
                    deviceIp = data.getStringExtra("device_ip");
                    for (FileItem file : sendingFiles) {
                        if (!file.isDone())
                            fileSenders.submit(new FileSender(deviceIp, 8889, file.getFile().getAbsolutePath(), fsListener));
                    }
                }
                break;
        }
    }

}
