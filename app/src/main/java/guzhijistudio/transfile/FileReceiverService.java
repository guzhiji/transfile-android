package guzhijistudio.transfile;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import guzhijistudio.transfile.file.FileReceiver;
import guzhijistudio.transfile.utils.Constants;

import java.io.File;
import java.io.IOException;

public class FileReceiverService extends Service {

    private FileReceiver fileReceiver;
    final private FileReceiver.FileReceiverListener frListener = new FileReceiver.FileReceiverListener() {

        @Override
        public void onFileReceived(File file) {
            Intent i = new Intent(Constants.ACTION_FILE_RECEIVER);
            i.putExtra("type", Constants.FILE_RECEIVER_DONE);
            i.putExtra("file", file);
            LocalBroadcastManager.getInstance(FileReceiverService.this).sendBroadcast(i);
        }

        @Override
        public void onFile(File file) {
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
        try {
            fileReceiver = new FileReceiver(
                    Constants.FILE_SERVER_PORT,
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    frListener);
            fileReceiver.start();
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        if (fileReceiver != null) fileReceiver.shutdown();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
