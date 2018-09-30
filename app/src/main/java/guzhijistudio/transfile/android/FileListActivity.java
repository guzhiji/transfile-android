package guzhijistudio.transfile.android;

import android.Manifest;
import android.app.AlertDialog;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import guzhijistudio.transfile.utils.Constants;
import guzhijistudio.transfile.utils.ContentUtil;
import guzhijistudio.transfile.utils.PermUtil;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

public class FileListActivity extends AppCompatActivity {

    private final static int REQUEST_GET_FILE = 1;
    private final static int REQUEST_GET_DEVICE = 2;
    private final static int REQUEST_SETTINGS = 3;
    private final static int MODE_SEND = 0;
    private final static int MODE_RECEIVE = 1;
    public static boolean isAlive = false;
    private static class FileItem implements Serializable {
        final private File file;
        private boolean done;
        private boolean progressing;

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

        public boolean isProgressing() {
            return progressing;
        }

        public void setProgressing(boolean progressing) {
            this.progressing = progressing;
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
            return mode == MODE_SEND ? sendingFiles.size() : receivedFiles.size();
        }

        @Override
        public Object getItem(int i) {
            return mode == MODE_SEND ? sendingFiles.get(i) : receivedFiles.get(i);
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
            final FileItem file = mode == MODE_SEND ? sendingFiles.get(i) : receivedFiles.get(i);
            fileNameText.setText(file.getFile().getName());
            fileSizeText.setText(formatSize(file.getFile().length()));
            fileProgress.setMax(10000);
            if (file.isDone())
                fileProgress.setProgress(10000);
            else if (!file.isProgressing())
                fileProgress.setProgress(0);
            view.setTag(file.getFile());
            // registerForContextMenu(view);
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (!file.isProgressing()) {
                        PopupMenu popup = new PopupMenu(FileListActivity.this, view);
                        popup.getMenuInflater().inflate(R.menu.fileitem, popup.getMenu());
                        MenuItem menuItemResend = popup.getMenu().findItem(R.id.fileitem_resend);
                        menuItemResend.setVisible(mode == MODE_SEND && deviceIp != null);
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                switch (menuItem.getItemId()) {
                                    case R.id.fileitem_resend:
                                        Intent i = new Intent(FileListActivity.this, FileSenderService.class);
                                        i.putExtra("device_ip", deviceIp);
                                        i.putExtra("file_path", file.getFile().getAbsolutePath());
                                        startService(i);
                                        return true;
                                    case R.id.fileitem_remove:
                                        if (!file.isProgressing()) {
                                            switch (mode) {
                                                case MODE_SEND: sendingFiles.remove(file); break;
                                                case MODE_RECEIVE: receivedFiles.remove(file); break;
                                            }
                                            fileListView.setAdapter(new FileListAdaptor());
                                        }
                                        return true;
                                    default:
                                        return false;
                                }
                            }
                        });
                        popup.show();
                        return true;
                    }
                    return false;
                }
            });
            return view;
        }
    }

    private ArrayList<FileItem> sendingFiles;
    private ArrayList<FileItem> receivedFiles;
    private String deviceIp = null;
    private int mode = MODE_SEND;
    final private BottomNavigationView.OnNavigationItemSelectedListener nItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.navigation_send:
                    mode = MODE_SEND;
                    fileListView.setAdapter(new FileListAdaptor());
                    addFileButton.setVisibility(View.VISIBLE);
                    sendAllButton.setVisibility(View.VISIBLE);
                    return true;
                case R.id.navigation_receive:
                    mode = MODE_RECEIVE;
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

        PermUtil.checkPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, true, 1);

        if (savedInstanceState != null)
            sendingFiles = (ArrayList<FileItem>) savedInstanceState.getSerializable("sendingFiles");
        if (sendingFiles == null)
            sendingFiles = new ArrayList<>();
        if (savedInstanceState != null)
            receivedFiles = (ArrayList<FileItem>) savedInstanceState.getSerializable("receivedFiles");
        if (receivedFiles == null)
            receivedFiles = new ArrayList<>();
        if (savedInstanceState != null)
            mode = savedInstanceState.getInt("mode");

        BottomNavigationView navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(nItemSelectedListener);

        addFileButton = findViewById(R.id.addFileButton);
        addFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                startActivityForResult(i, REQUEST_GET_FILE);
            }
        });

        sendAllButton = findViewById(R.id.sendAllButton);
        sendAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(FileListActivity.this, DeviceListActivity.class);
                startActivityForResult(i, REQUEST_GET_DEVICE);
            }
        });

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (Intent.ACTION_SEND.equals(intent.getAction())) {
                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                String path = ContentUtil.getPath(this, uri);
                if (path != null) {
                    Log.i("filesharing", path);
                    File file = new File(path);
                    if (file.canRead()) sendingFiles.add(new FileItem(file));
                }
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
                ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                Log.i("filesharing", uris.size() + "");
                for (Uri uri : uris) {
                    String path = ContentUtil.getPath(this, uri);
                    if (path != null) {
                        Log.i("filesharing", path);
                        File file = new File(path);
                        if (file.canRead()) sendingFiles.add(new FileItem(file));
                    }
                }
            }
        }
        fileListView = findViewById(R.id.fileListView);
        fileListView.setAdapter(new FileListAdaptor());

        IntentFilter fileReceiverIntentFilter = new IntentFilter(Constants.ACTION_FILE_RECEIVER);
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                File file = (File) intent.getSerializableExtra("file");
                switch (intent.getIntExtra("type", 0)) {
                    case Constants.FILE_RECEIVER_START:
                        boolean found = false;
                        for (FileItem fileItem : receivedFiles) {
                            if (fileItem.getFile().equals(file)) {
                                fileItem.setDone(false);
                                fileItem.setProgressing(true);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            FileItem fileItem = new FileItem(file);
                            fileItem.setDone(false);
                            fileItem.setProgressing(true);
                            receivedFiles.add(fileItem);
                            if (mode == MODE_RECEIVE) fileListView.setAdapter(new FileListAdaptor());
                        }
                        break;
                    case Constants.FILE_RECEIVER_DONE:
                        for (FileItem fileItem : receivedFiles) {
                            if (fileItem.getFile().equals(file)) {
                                fileItem.setDone(true);
                                fileItem.setProgressing(false);
                                break;
                            }
                        }
                        Toast.makeText(getApplicationContext(), getString(R.string.main_file_received, file.getName()), Toast.LENGTH_SHORT).show();
                        break;
                    case Constants.FILE_RECEIVER_PROGRESS:
                        if (mode == MODE_RECEIVE) {
                            long received = intent.getLongExtra("received", 0);
                            long total = intent.getLongExtra("total", 0);
                            if (!showProgress(file, received, total)) {
                                FileItem fileItem = new FileItem(file);
                                if (!receivedFiles.contains(fileItem)) {
                                    fileItem.setProgressing(true);
                                    fileItem.setDone(false);
                                    receivedFiles.add(fileItem);
                                    fileListView.setAdapter(new FileListAdaptor());
                                    showProgress(file, received, total);
                                }
                            }
                        }
                        break;
                    case Constants.FILE_RECEIVER_ERROR:
                        Toast.makeText(getApplicationContext(), intent.getStringExtra("msg"), Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }, fileReceiverIntentFilter);

        IntentFilter fileSenderIntentFilter = new IntentFilter(Constants.ACTION_FILE_SENDER);
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                File file = (File) intent.getSerializableExtra("file");
                switch (intent.getIntExtra("type", 0)) {
                    case Constants.FILE_SENDER_START:
                        boolean found = false;
                        for (FileItem fileItem : sendingFiles) {
                            if (fileItem.getFile().equals(file)) {
                                fileItem.setDone(false);
                                fileItem.setProgressing(true);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            FileItem fileItem = new FileItem(file);
                            fileItem.setDone(false);
                            fileItem.setProgressing(true);
                            sendingFiles.add(fileItem);
                            if (mode == MODE_SEND) fileListView.setAdapter(new FileListAdaptor());
                        }
                        break;
                    case Constants.FILE_SENDER_PROGRESS:
                        if (mode == MODE_SEND) {
                            long sent = intent.getLongExtra("sent", 0);
                            long total = intent.getLongExtra("total", 0);
                            if (!showProgress(file, sent, total)) {
                                FileItem fileItem = new FileItem(file);
                                if (!sendingFiles.contains(fileItem)) {
                                    fileItem.setProgressing(true);
                                    fileItem.setDone(false);
                                    sendingFiles.add(fileItem);
                                    fileListView.setAdapter(new FileListAdaptor());
                                    showProgress(file, sent, total);
                                }
                            }
                        }
                        break;
                    case Constants.FILE_SENDER_DONE:
                        for (FileItem fileItem : sendingFiles) {
                            if (fileItem.getFile().equals(file)) {
                                fileItem.setDone(true);
                                fileItem.setProgressing(false);
                                break;
                            }
                        }
                        Toast.makeText(getApplicationContext(), getString(R.string.main_file_sent, file.getName()), Toast.LENGTH_SHORT).show();
                        break;
                    case Constants.FILE_SENDER_ERROR:
                        for (FileItem fileItem : sendingFiles) {
                            if (fileItem.getFile().equals(file)) {
                                fileItem.setDone(false);
                                fileItem.setProgressing(false);
                                break;
                            }
                        }
                        Toast.makeText(getApplicationContext(), intent.getStringExtra("msg"), Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }, fileSenderIntentFilter);

        startService(new Intent(this, FileReceiverService.class));
        FileListActivity.isAlive = true;
    }

    @Override
    protected void onDestroy() {
        FileListActivity.isAlive = false;
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences pref = getSharedPreferences("config", MODE_PRIVATE);
        if (!pref.contains("device_name")) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.main_config_title)
                    .setMessage(R.string.main_config_message)
                    .setPositiveButton(R.string.button_config, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(FileListActivity.this, ConfigActivity.class);
                            intent.putExtra("initialization", true);
                            startActivityForResult(intent, REQUEST_SETTINGS);
                        }
                    })
                    .setNegativeButton(R.string.button_exit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .create()
                    .show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("sendingFiles", sendingFiles);
        outState.putSerializable("receivedFiles", receivedFiles);
        outState.putInt("mode", mode);
        super.onSaveInstanceState(outState);
    }

    private boolean showProgress(File file, long progress, long total) {
        View itemView = findFileItemView(file);
        if (itemView == null) return false;
        ProgressBar bar = itemView.findViewById(R.id.fileProgress);
        bar.setMax(10000);
        bar.setProgress((int) (10000.0 * progress / total));
        TextView txt = itemView.findViewById(R.id.fileSizeText);
        txt.setText(String.format("%s / %s", formatSize(progress), formatSize(total)));
        return true;
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
            case REQUEST_GET_FILE:
                if (resultCode == RESULT_OK && data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        String filepath = ContentUtil.getPath(getApplicationContext(), uri);
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
            case REQUEST_GET_DEVICE:
                if (resultCode == RESULT_OK && data != null) {
                    deviceIp = data.getStringExtra("device_ip");
                    for (FileItem file : sendingFiles) {
                        if (!file.isDone() && !file.isProgressing()) {
                            Intent i = new Intent(FileListActivity.this, FileSenderService.class);
                            i.putExtra("device_ip", deviceIp);
                            i.putExtra("file_path", file.getFile().getAbsolutePath());
                            startService(i);
                        }
                    }
                }
                break;
            case REQUEST_SETTINGS:
                if (resultCode == RESULT_OK) {
                    Intent i = new Intent(this, FileReceiverService.class);
                    stopService(i);
                    startService(i);
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setting, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.setting_setting:
                Intent i = new Intent(this, ConfigActivity.class);
                i.putExtra("initialization", false);
                startActivityForResult(i, REQUEST_SETTINGS);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
