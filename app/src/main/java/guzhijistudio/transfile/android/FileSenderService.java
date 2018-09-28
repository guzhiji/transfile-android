package guzhijistudio.transfile.android;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import guzhijistudio.transfile.file.FileSender;
import guzhijistudio.transfile.utils.Constants;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class FileSenderService extends Service {

    private AtomicInteger count = null;
    private ExecutorService fileSenders;
    final private FileSender.FileSenderListener fsListener = new FileSender.FileSenderListener() {
        @Override
        public void onStart(File file) {
            if (count == null) count = new AtomicInteger();
            count.incrementAndGet();
            Intent i = new Intent(Constants.ACTION_FILE_SENDER);
            i.putExtra("type", Constants.FILE_SENDER_START);
            i.putExtra("file", file);
            LocalBroadcastManager.getInstance(FileSenderService.this).sendBroadcast(i);
        }

        @Override
        public void onFileSent(File file) {
            count.decrementAndGet();
            Intent i = new Intent(Constants.ACTION_FILE_SENDER);
            i.putExtra("type", Constants.FILE_SENDER_DONE);
            i.putExtra("file", file);
            LocalBroadcastManager.getInstance(FileSenderService.this).sendBroadcast(i);
        }

        @Override
        public void onError(File file, String msg) {
            if (file != null && count != null) count.decrementAndGet();
            Intent i = new Intent(Constants.ACTION_FILE_SENDER);
            i.putExtra("type", Constants.FILE_SENDER_ERROR);
            i.putExtra("file", file);
            i.putExtra("msg", msg);
            LocalBroadcastManager.getInstance(FileSenderService.this).sendBroadcast(i);
        }

        @Override
        public void onProgress(File file, long sent, long total) {
            Intent i = new Intent(Constants.ACTION_FILE_SENDER);
            i.putExtra("type", Constants.FILE_SENDER_PROGRESS);
            i.putExtra("file", file);
            i.putExtra("sent", sent);
            i.putExtra("total", total);
            LocalBroadcastManager.getInstance(FileSenderService.this).sendBroadcast(i);
        }

    };

    @Override
    public void onCreate() {
        fileSenders = Executors.newFixedThreadPool(2);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (count != null && count.get() <= 0) {
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
    }

    @Override
    public void onDestroy() {
        if (fileSenders != null) fileSenders.shutdown();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String deviceIp = intent.getStringExtra("device_ip");
        String filePath = intent.getStringExtra("file_path");
        fileSenders.submit(new FileSender(deviceIp, Constants.FILE_SERVER_PORT, filePath, fsListener));
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
