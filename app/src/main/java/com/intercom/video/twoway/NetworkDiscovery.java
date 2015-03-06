package com.intercom.video.twoway;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;

/*
To operate me:
NetworkDiscovery netdisco = new NetworkDiscovery();
netdisco.start()
ArrayList<String> ipList = netdisco.getUrlList();

ipList == list of discovered ips
 */
public class NetworkDiscovery extends Thread {

    private static final int DISCOVERY_PORT = 44444;
    private static final int LISTENING_TIMEOUT_MS = 500;
    private static final int OPPORTUNITY_TIMEOUT_MS = 200;
    private WifiManager wifi;
    private DatagramSocket socket;
    private String myIp;
    private boolean stop = false;
    private ArrayList<String> ipList;

    public NetworkDiscovery() {
        this.wifi = (WifiManager) MainActivity.utilities.mainContext.getSystemService(Context.WIFI_SERVICE);
        ipList = new ArrayList<String>();
    }

    //get list here
    public ArrayList<String> getIpList() {
        return ipList;
    }

    //stop me here
    public void stopNetworkDiscovery() {
        this.stop = true;
    }

    //start me here
    public void startNetworkDiscovery() {
        this.stop = false;
    }


    public void run() {
        while (!stop) {
            try {
                myIp = getMyIp();
                socket = new DatagramSocket(DISCOVERY_PORT);
                socket.setBroadcast(true);
                socket.setSoTimeout(LISTENING_TIMEOUT_MS);

                try {
                    sendBroadCast();
                    try {
                        this.sleep(OPPORTUNITY_TIMEOUT_MS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String url = listenForResponses(socket);
                    if (url != null)
                        ipList.add(url);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    socket.close();
                }

            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

    }

    private InetAddress getBroadcastAddress(WifiManager wifi) throws UnknownHostException {
        DhcpInfo dhcp = wifi.getDhcpInfo();
        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        //TODO null handleing
        InetAddress inetAddress = ipIntToInet(broadcast);
        System.out.println("host address: " + inetAddress.getHostAddress());
        return inetAddress;
    }

    private void sendBroadCast() throws IOException {
        String data = new Date().toString();
        socket.setBroadcast(true);
        DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(),
                getBroadcastAddress(wifi), DISCOVERY_PORT);
        socket.send(packet);
    }

    private String listenForResponses(DatagramSocket socket) throws IOException {
        byte[] buf = new byte[1024];
        String payload;
        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String ip = packet.getAddress().getHostAddress();
                if (ip.equals(myIp)) {
                    System.out.println("received my packet");
                    return null;
                }
                payload = new String(packet.getData(), 0, packet.getLength());
                System.out.println("received: " + payload + " ip: " + ip);
                return ip;
            }
        } catch (SocketTimeoutException e) {
            System.out.println("timed out");
        }
        return null;
    }

    private String getMyIp() {
        int ipAddress = wifi.getConnectionInfo().getIpAddress();
        InetAddress inetAddress = ipIntToInet(ipAddress);
        String ipAddressString = inetAddress.getHostAddress();
        return ipAddressString;
    }

    private InetAddress ipIntToInet(int ipAddress) {
        InetAddress inetAddress;
        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }
        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();
        try {
            inetAddress = InetAddress.getByAddress(ipByteArray);
        } catch (UnknownHostException ex) {
            inetAddress = null;
        }
        return inetAddress;
    }


}
