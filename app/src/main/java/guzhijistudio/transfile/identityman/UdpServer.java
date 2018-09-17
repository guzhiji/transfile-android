package guzhijistudio.transfile.identityman;

import guzhijistudio.transfile.utils.SocketUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UdpServer extends Thread {

    public interface UdpServerListener {
        void onEnter(String ip, String name);
        void onQuit(String ip);
    }

    private boolean running = false;
    private final ConcurrentHashMap<String, Long> ips = new ConcurrentHashMap<>();
    private final MulticastSocket socket;
    private final UdpServerListener listener;
    private final Thread bgThread = new Thread() {
        @Override
        public void run() {
            try {
                while (running) {
                    removeExpired();
                    sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    public UdpServer(SocketAddress addr, InetAddress group, UdpServerListener listener) throws IOException {
        this.socket = new MulticastSocket(addr);
        // this.socket.setTimeToLive(64);
        this.socket.joinGroup(group);
        this.listener = listener;
    }

    private void removeExpired() {
        long t = System.currentTimeMillis();
        LinkedList<String> expired = new LinkedList<>();
        for (Map.Entry<String, Long> entry : ips.entrySet()) {
            if (t - entry.getValue() > 30000)
                expired.add(entry.getKey());
        }
        for (String expiredIp : expired) {
            ips.remove(expiredIp);
            listener.onQuit(expiredIp);
        }
    }

    @Override
    public void start() {
        running = true;
        bgThread.start();
        super.start();
    }

    public void shutdown() {
        running = false;
        socket.close();
    }

    @Override
    public void run() {
        byte[] data = new byte[256], buf = new byte[256];
        DatagramPacket dp = new DatagramPacket(data, data.length);
        while (running) {
            try {
                socket.receive(dp);
                long t = System.currentTimeMillis();
                SocketUtils.BufPos pos = new SocketUtils.BufPos();
                String cmd = SocketUtils.toStr(data, buf, pos);
                String ip = SocketUtils.toStr(data, buf, pos);
                String name = SocketUtils.toStr(data, buf, pos);
                if (ip != null && name != null) {
                    if ("quit".equalsIgnoreCase(cmd)) {
                        listener.onQuit(ip);
                        ips.remove(ip);
                    } else {
                        if (!ips.containsKey(ip))
                            listener.onEnter(ip, name);
                        ips.put(ip, t);
                    }
                }
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
    }

}
