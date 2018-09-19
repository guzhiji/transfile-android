package guzhijistudio.transfile.identity;

import guzhijistudio.transfile.utils.SocketUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;

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
    @Override
    public void run() {
        byte[] data = new byte[256];
        try {
            // MulticastSocket socket = new MulticastSocket();
            // socket.setTimeToLive(64);
            DatagramSocket socket = new DatagramSocket();
            // socket.connect(groupAddr);
            while (running) {
                try {
                    socket.connect(groupAddr);
                    SocketUtils.BufPos pos = new SocketUtils.BufPos();
                    SocketUtils.writeString(data, "enter", pos);
                    SocketUtils.writeString(data, socket.getLocalAddress().getHostAddress(), pos);
                    SocketUtils.writeString(data, name, pos);
                    socket.send(new DatagramPacket(data, data.length));
                    // socket.close();
                } catch (Exception e) {
                    // e.printStackTrace();
                }
                sleep(1000);
            }
            socket.connect(groupAddr);
            SocketUtils.BufPos pos = new SocketUtils.BufPos();
            SocketUtils.writeString(data, "quit", pos);
            SocketUtils.writeString(data, socket.getLocalAddress().getHostAddress(), pos);
            SocketUtils.writeString(data, name, pos);
            socket.send(new DatagramPacket(data, data.length));
            socket.close();
        } catch (UnsupportedEncodingException ignored) {
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
