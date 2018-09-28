package guzhijistudio.transfile.android;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import guzhijistudio.transfile.file.FileReceiver;
import guzhijistudio.transfile.identity.Broadcaster;
import guzhijistudio.transfile.utils.Constants;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

public class FileReceiverService extends Service {

    private AtomicInteger count = null;
    private FileReceiver fileReceiver;
    private Broadcaster broadcaster = null;
    final private FileReceiver.FileReceiverListener frListener = new FileReceiver.FileReceiverListener() {

        @Override
        public void onFileReceived(File file) {
            count.decrementAndGet();
            Intent i = new Intent(Constants.ACTION_FILE_RECEIVER);
            i.putExtra("type", Constants.FILE_RECEIVER_DONE);
            i.putExtra("file", file);
            LocalBroadcastManager.getInstance(FileReceiverService.this).sendBroadcast(i);
        }

        @Override
        public void onFile(File file) {
            count.incrementAndGet();
            Intent i = new Intent(Constants.ACTION_FILE_RECEIVER);
            i.putExtra("type", Constants.FILE_RECEIVER_START);
            i.putExtra("file", file);
            LocalBroadcastManager.getInstance(FileReceiverService.this).sendBroadcast(i);
        }

        @Override
        public void onMsg(String msg) {

        }

        @Override
        public void onError(String msg) {
            count.getAndDecrement();
            Intent i = new Intent(Constants.ACTION_FILE_RECEIVER);
            i.putExtra("type", Constants.FILE_RECEIVER_ERROR);
            i.putExtra("msg", msg);
            LocalBroadcastManager.getInstance(FileReceiverService.this).sendBroadcast(i);
        }

        @Override
        public void onProgress(File file, long received, long total) {
            Intent i = new Intent(Constants.ACTION_FILE_RECEIVER);
            i.putExtra("type", Constants.FILE_RECEIVER_PROGRESS);
            i.putExtra("file", file);
            i.putExtra("received", received);
            i.putExtra("total", total);
            LocalBroadcastManager.getInstance(FileReceiverService.this).sendBroadcast(i);
        }

    };

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences pref = getSharedPreferences("config", MODE_PRIVATE);
        String deviceName = pref.getString("device_name", null);
        String groupAddr = pref.getString("group_addr", Constants.IDENTITY_GROUP_ADDR);
        String dir = pref.getString("dir", null);
        if (deviceName == null || dir == null) {
            stopSelf();
            return;
        }
        try {
            count = new AtomicInteger();
            fileReceiver = new FileReceiver(
                    Constants.FILE_SERVER_PORT,
                    new File(dir),
                    frListener);
            fileReceiver.start();
            broadcaster = new Broadcaster(
                    deviceName,
                    new InetSocketAddress(groupAddr, Constants.IDENTITY_SERVER_PORT));
            broadcaster.start();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        if (!FileListActivity.isAlive && count.get() <= 0) {
                            stopSelf();
                            break;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        if (fileReceiver != null) fileReceiver.shutdown();
        if (broadcaster != null) broadcaster.shutdown();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
