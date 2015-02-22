package com.intercom.video.twoway;


import org.junit.Test;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

import static junit.framework.Assert.assertEquals;

public class NetworkDiscoveryTest {

    @Test
    public void byteToIpAddressTest() throws UnknownHostException {
        int sample = 1234567;
        int test1 = sample;
        int test2 = sample;
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            test1 = Integer.reverseBytes(test1);
        }

        byte[] ipByteArray = BigInteger.valueOf(test1).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            ipAddressString = null;
        }


        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++) {
            quads[k] = (byte) ((test2 >> k * 8) & 0xFF);
        }
        InetAddress inetAddress = InetAddress.getByAddress(quads);

        String ipAddressString2 = inetAddress.getHostAddress();

        assertEquals(ipAddressString, ipAddressString2);
    }
}
