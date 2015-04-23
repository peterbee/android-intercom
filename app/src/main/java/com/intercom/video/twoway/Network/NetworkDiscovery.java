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

/**
 * @Author Sean Luther, Rob Z.
 * To operate me:
 * NetworkDiscovery netdisco = new NetworkDiscovery();
 * netdisco.start()
 * ArrayList<String> ipList = netdisco.getUrlList();
 *
 * ipList == list of discovered ips
 */
public class NetworkDiscovery extends Thread
{

    private static final int DISCOVERY_PORT = 44444;
    private WifiManager wifi;
    private DatagramSocket socket;
    private String myIp;
    private boolean stop = false;
    private ArrayList<String> ipList;
    private Utilities utilities;
    private boolean discoveryRunning = false;

    /**
     * Constructor
     * Takes in the Utilities object from the Main Activity to maintain correct references
     *
     * @param utilities
     */
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

    public ArrayList<String> getIpList()
    {
        return ipList;
    }

    public void stopNetworkDiscovery()
    {
        this.stop = true;
    }

    public void startNetworkDiscovery()
    {
        this.stop = false;
    }

    /**
     * This is the Run method for the NetworkDiscovery thread
     * The thread has no sleep in it, so it runs as fast as possible, sending out broadcast packets
     * and then calls listenForResponses, rinse and repeat
     */
    public void run()
    {

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

                    String url = listenForResponses(socket);

                    if (url != null)
                    {
                        ipList.add(url);
                    }
                } catch (IOException e) {
                    Log.d("NetworkDiscovery", "unable to send broadcast, IO exception");
                } finally
                {
                    socket.close();
                }

            } catch (SocketException e) {
                Log.d("NetworkDiscovery", "Socket Issue");
            }
            try
            {
                socket.close();
            }
            catch(Exception e) {
                Log.d("NetworkDiscovery", "Unknown Exception");
            }
        }
        if (!socket.isClosed())
        {
            socket.close();
        }


    }

/*  // this code doesnt work on some devices
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

    /**
     * Performs the necessary math on the device.s Ip and the netmask to compute the broadcast
     * address.
     *
     * @param myWifiManager
     * @return
     * @throws IOException
     */
    // this seems to work on most devices
    private InetAddress getBroadcastAddress(WifiManager myWifiManager) throws IOException
    {
        DhcpInfo myDhcpInfo = myWifiManager.getDhcpInfo();
        if (myDhcpInfo == null) {
            Log.d("NetworkDiscovery", "Could not get broadcast address");
            return null;
        }
        int broadcast = (myDhcpInfo.ipAddress & myDhcpInfo.netmask)
                | ~myDhcpInfo.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    /**
     * Sends arbitrary data (the current date) out to the broadcast IP on the port which we wish to
     * discover
     */
    private void sendBroadCast()
    {
        try
        {
            String data = new Date().toString();
            socket.setBroadcast(true);
            DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(),
                    getBroadcastAddress(wifi), DISCOVERY_PORT);
            socket.send(packet);
        } catch (Exception e) {
            Log.d("NetworkDiscovery", "Couldn't send broadcast");
        }
        Log.i("NetworkDiscovery", "Sent broadcast");
    }

    /**
     * Listens on the discovery port for arbitrary data until the socket times out
     * @param socket
     * @return
     * @throws IOException
     */
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
                if (ip.equals(myIp) || ipList.contains(ip)) {
                    Log.i("NetworkDiscovery", "Known Ip:" + ip + " discovered");
                } else
                {
                    payload = new String(packet.getData(), 0, packet.getLength());
                    Log.i("NetworkDiscovery", "received: " + payload + " ip: " + ip);
                    return ip;
                }
            }
        } catch (SocketTimeoutException e) {
            Log.d("NetworkDiscovery", "timed out");
        }
        return null;
    }

    public String getMyIp()
    {
        int ipAddress = wifi.getConnectionInfo().getIpAddress();
        InetAddress inetAddress = ipIntToInet(ipAddress);
        if (inetAddress == null)
        {
            Log.d("NetworkDiscovery", "Can't get ip address from DHCP");
            return null;
        }
        String ipAddressString = inetAddress.getHostAddress();
        Log.i("NetworkDiscovery", "My ip found: " + ipAddressString);
        return ipAddressString;
    }

    /**
     * Converts IP address stored as int to Inet
     * @param ipAddress
     * @return
     */
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

    /**
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * This method activates our Network discovery engine.
     */
    public void setupNetworkDiscovery() {
        start();

        // Sean removed this code (charles) because it didn't seem to do anything
        // and in order to put network discovery in the service we were no longer able to
        // pass it main activity
        /*
        mainActivity.mUrlList_asArrayList = getIpList();

        ArrayList<String> mUrlList_asArrayList = new ArrayList<String>();
        // update initial list of discovered IPs
        // also need to happen every time the view is called
        MainActivity.mUrlList_as_StringArray = mainActivity.utilities.convertArrayListToStringArray(mUrlList_asArrayList);
        mainActivity.utilities.setIpList(MainActivity.mUrlList_as_StringArray);
        */

    }
}
