package com.intercom.video.twoway;

import android.net.DhcpInfo;
import android.net.wifi.WifiManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;

public class NetworkDiscovery extends Thread {

    private static final int DISCOVERY_PORT = 44444;
    private static final int TIMEOUT_MS = 500;
    private String data = "payload" + new Date().toString();
    private WifiManager wifi;
    private DatagramSocket socket;

    public NetworkDiscovery(WifiManager wifi) {
        this.wifi = wifi;
    }

    public void run() {
        try {
            socket = new DatagramSocket(DISCOVERY_PORT);
            socket.setBroadcast(true);
            socket.setSoTimeout(TIMEOUT_MS);

            try {
                sendBroadCast(wifi);
                listenForResponses(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    //gets broadcast address from router's DHCP
    public InetAddress getBroadcastAddress(WifiManager wifi) throws UnknownHostException {
        DhcpInfo dhcp = wifi.getDhcpInfo();
        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++) {
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        }
        InetAddress inetAddress = InetAddress.getByAddress(quads);
        System.out.println("host address: " + inetAddress.getHostAddress());
        return inetAddress;

    }

    public void sendBroadCast(WifiManager wifi) throws IOException {
        socket.setBroadcast(true);
        DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(),
                getBroadcastAddress(wifi), DISCOVERY_PORT);
        socket.send(packet);
    }

    private void listenForResponses(DatagramSocket socket) throws IOException {
        byte[] buf = new byte[1024];
        String s = "";
        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                s = new String(packet.getData(), 0, packet.getLength());
                System.out.println("received: " + s + " Inet: " + packet.getAddress().getHostAddress());
            }
        } catch (SocketTimeoutException e) {
            System.out.println("timed out");
        }
    }
}
