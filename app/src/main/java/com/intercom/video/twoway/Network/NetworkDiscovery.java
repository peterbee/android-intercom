package com.intercom.video.twoway.Network;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.intercom.video.twoway.Utilities.Utilities;

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
public class NetworkDiscovery extends Thread
{

    private static final int DISCOVERY_PORT = 44444;
    //private static final int LISTENING_TIMEOUT_MS = 500;
    //private static final int OPPORTUNITY_TIMEOUT_MS = 200;
    private WifiManager wifi;
    private DatagramSocket socket;
    private String myIp;
    private boolean stop = false;
    private ArrayList<String> ipList;
    private Utilities utilities;
    private boolean discoveryRunning = false;

    public NetworkDiscovery(Utilities utilities)
    {
        this.utilities = utilities;
        try
        {
            this.wifi = (WifiManager) this.utilities.mainContext.getSystemService(Context.WIFI_SERVICE);
        } catch (NullPointerException e)
        {
            Log.i("NetworkDiscovery", " Not Connected to WIFI"); // TODO: fix BUG-0001
        }
        ipList = new ArrayList<String>();
    }

    //get list here
    public ArrayList<String> getIpList()
    {
        return ipList;
    }

    //stop me here
    public void stopNetworkDiscovery()
    {
        this.stop = true;
    }

    //start me here
    public void startNetworkDiscovery()
    {
        this.stop = false;
    }

    public void startBroadcastThread()
    {
        discoveryRunning = true;

        Thread broadcastThread = new Thread()
        {
            public void run()
            {
                while (discoveryRunning)
                {
                    sendBroadCast();
                    try
                    {
                        // send a broadcast every second
                        Thread.sleep(1000);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        };

        broadcastThread.start();
    }

    /*
    Listen for broadcasts from other devices
     */
    public void startListenerThread()
    {
        discoveryRunning = true;

        Thread listenerThread = new Thread()
        {
            public void run()
            {
                byte[] buf = new byte[1024];
                while (discoveryRunning)
                {
                    try
                    {
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        socket.receive(packet);
                        String ip = packet.getAddress().getHostAddress();

                        Log.d("NetworkDiscovery", "Broadcast received from " + ip);

                        // now add it to our ip list if its not already in the ip list

                        if (!ipList.contains(ip))
                            if(!ip.equals(myIp))
                            ipList.add(ip);

                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        };

        listenerThread.start();
    }

    public void stopDiscovery()
    {
        discoveryRunning = false;
        if (!socket.isClosed())
        {
            socket.close();
        }
    }

    public void run()
    {
        try
        {
            myIp = getMyIp();
            socket = new DatagramSocket(DISCOVERY_PORT);
            socket.setBroadcast(true);
        } catch (Exception e)
        {

        }

        startBroadcastThread();
        startListenerThread();

/*
        while (!stop)
        {
            try
            {
                myIp = getMyIp();
                socket = new DatagramSocket(DISCOVERY_PORT);
                socket.setBroadcast(true);
                socket.setSoTimeout((int) (Math.random() * 400 + 100));


                try
                {
                    sendBroadCast();


                    try
                    {
                        this.sleep(OPPORTUNITY_TIMEOUT_MS);
                    } catch (InterruptedException e)
                    {
                        Log.d("NetworkDiscovery", "Sleep interrupted");
                    }


                    String url = listenForResponses(socket);

                    if (url != null)
                    {
                        ipList.add(url);
                    }
                } catch (IOException e)
                {
                    Log.d("NetworkDiscovery", "IOException?");
                } finally
                {
                    socket.close();
                }

            } catch (SocketException e)
            {
                Log.d("NetworkDiscovery", "Socket Issue");
            } finally
            {
                if (!socket.isClosed())
                {
                    socket.close();
                }
            }
            if (!socket.isClosed())
            {
                socket.close();
            }
        }
        if (!socket.isClosed())
        {
            socket.close();
        }
        */

    }

    // this code doesnt work on some devices
/*
    private InetAddress getBroadcastAddress(WifiManager wifi) throws UnknownHostException
    {
        DhcpInfo dhcp = wifi.getDhcpInfo();
        if (dhcp == null)
        {
            Log.d("NetworkDiscovery", "DHCP null");
        }
        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        InetAddress inetAddress = ipIntToInet(broadcast);

        Log.d("NetworkDiscovery",
                "host address found: " + inetAddress.getHostAddress());
        return inetAddress;
    }

*/

    // this seems to work on all devices
    private InetAddress getBroadcastAddress(WifiManager myWifiManager) throws IOException
    {
        DhcpInfo myDhcpInfo = myWifiManager.getDhcpInfo();
        if (myDhcpInfo == null)
        {
            System.out.println("Could not get broadcast address");
            return null;
        }
        int broadcast = (myDhcpInfo.ipAddress & myDhcpInfo.netmask)
                | ~myDhcpInfo.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }


    private void sendBroadCast()
    {
        try
        {
            String data = new Date().toString();
            socket.setBroadcast(true);
            DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(),
                    getBroadcastAddress(wifi), DISCOVERY_PORT);
            socket.send(packet);
        } catch (Exception e)
        {
            Log.d("NetworkDiscovery", "Couldn't send broadcast");
        }
        Log.d("NetworkDiscovery", "Sent broadcast");
    }

    private String listenForResponses(DatagramSocket socket) throws IOException
    {
        byte[] buf = new byte[1024];
        String payload;
        try
        {
            while (true)
            {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String ip = packet.getAddress().getHostAddress();
                if (ip.equals(myIp) || ipList.contains(ip))
                {
                    socket.send(packet);
                    Log.d("NetworkDiscovery", "Ip known, rebroadcasting: " + ip);
                } else
                {
                    payload = new String(packet.getData(), 0, packet.getLength());
                    Log.d("NetworkDiscovery", "received: " + payload + " ip: " + ip);
                    return ip;
                }
            }
        } catch (SocketTimeoutException e)
        {
            Log.d("NetworkDiscovery", "timed out");
        }
        return null;
    }

    private String getMyIp()
    {
        int ipAddress = wifi.getConnectionInfo().getIpAddress();
        InetAddress inetAddress = ipIntToInet(ipAddress);
        if (inetAddress == null)
        {
            Log.d("NetworkDiscovery", "Can't get ip address from DHCP");
            return null;
        }
        String ipAddressString = inetAddress.getHostAddress();
        Log.d("NetworkDiscovery", "My ip found: " + ipAddressString);
        return ipAddressString;
    }

    private InetAddress ipIntToInet(int ipAddress)
    {
        InetAddress inetAddress = null;
        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN))
        {
            ipAddress = Integer.reverseBytes(ipAddress);
        }
        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();
        try
        {
            inetAddress = InetAddress.getByAddress(ipByteArray);
        } catch (UnknownHostException ex)
        {
            Log.d("NetworkDiscovery", "Can't convert from int to ip");
        }
        return inetAddress;
    }
}
