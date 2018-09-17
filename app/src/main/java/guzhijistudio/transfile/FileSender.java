package guzhijistudio.transfile;

import guzhijistudio.transfile.utils.SocketUtils;

import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class FileSender extends Thread {

    public interface FileSenderListener {
        void onFileSent(String filename);
        void onError(String msg);
        void onProgress(long sent, long total);
    }

    private final String ip;
    private final int port;
    private final String filename;
    private final FileSenderListener listener;

    public FileSender(String ip, int port, String filename, FileSenderListener listener) {
        this.ip = ip;
        this.port = port;
        this.filename = filename;
        this.listener = listener;
    }

    @Override
    public void run() {
        byte[] buf = new byte[1024];
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port));
            try {
                OutputStream os = socket.getOutputStream();
                try {
                    SocketUtils.writeString(os, "file");
                    SocketUtils.writeFile(os, buf, filename, new SocketUtils.Progress() {
                        @Override
                        public void onProgress(long progress, long total) {
                            listener.onProgress(progress, total);
                        }
                    });
                    File f = new File(filename);
                    listener.onFileSent(f.getName());
                    SocketUtils.writeString(os, "close");
                } finally {
                    os.close();
                }
            } finally {
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            listener.onError(e.getMessage());
        }
    }
}
