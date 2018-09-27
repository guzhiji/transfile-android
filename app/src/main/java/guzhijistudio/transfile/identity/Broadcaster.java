package guzhijistudio.transfile.identity;

import guzhijistudio.transfile.utils.Constants;
import guzhijistudio.transfile.utils.SocketUtils;
import java.io.IOException;

import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class Broadcaster extends Thread {

    private final SocketAddress groupAddr;
    private final String name;
    private boolean running = false;

    public Broadcaster(String name, SocketAddress groupAddr) {
        this.name = name;
        this.groupAddr = groupAddr;
    }

    @Override
    public void start() {
        running = true;
        super.start();
    }

    public void shutdown() {
        running = false;
    }

    private boolean isIPv4(String ip) {
        if (ip == null) return false;
        String[] nums = ip.split("\\.");
        if (nums.length != 4) return false;
        try {
            for (String num : nums) {
                int n = Integer.parseInt(num);
                if (n < 0 || n > 255)
                    return false;
            }
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private List<DatagramSocket> prepareSockets() throws SocketException {
        List<DatagramSocket> sockets = new ArrayList<>();
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        while (ifaces.hasMoreElements()) {
            NetworkInterface iface = ifaces.nextElement();
            if (!iface.isLoopback() && iface.isUp()) {
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (isIPv4(addr.getHostAddress())) {
                        DatagramSocket socket = new DatagramSocket(Constants.IDENTITY_LOCAL_PORT, addr);
                        socket.setBroadcast(true);
                        sockets.add(socket);
                        System.out.println(addr.getHostAddress());
                    }
                }
            }
        }
        return sockets;
    }

    @Override
    public void run() {
        byte[] data = new byte[256];
        try {
            List<DatagramSocket> sockets = prepareSockets();
            while (running) {
                for (DatagramSocket socket : sockets) {
                    try {
                        socket.connect(groupAddr);
                        SocketUtils.BufPos pos = new SocketUtils.BufPos();
                        SocketUtils.writeString(data, "enter", pos);
                        SocketUtils.writeString(data, socket.getLocalAddress().getHostAddress(), pos);
                        SocketUtils.writeString(data, name, pos);
                        socket.send(new DatagramPacket(data, data.length));
                        // socket.close();
                    } catch (IOException e) {
                        // e.printStackTrace();
                    }
                }
                sleep(1000);
            }
            for (DatagramSocket socket : sockets) {
                try {
                    socket.connect(groupAddr);
                    SocketUtils.BufPos pos = new SocketUtils.BufPos();
                    SocketUtils.writeString(data, "quit", pos);
                    SocketUtils.writeString(data, socket.getLocalAddress().getHostAddress(), pos);
                    SocketUtils.writeString(data, name, pos);
                    socket.send(new DatagramPacket(data, data.length));
                } catch (IOException e) {
                    // e.printStackTrace();
                } finally {
                    socket.close();
                }
            }
        } catch (SocketException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
