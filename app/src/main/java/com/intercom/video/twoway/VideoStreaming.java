package com.intercom.video.twoway;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/*
This class contains things that deal with transmitting and receiving video / audio streams
 */

public class VideoStreaming
{

    private int LISTENING_SERVICE_PORT = 2049;

    /*
    Used when we are the client
     */
    private Socket tcpSocket;

    /*
   Used for accepting connections when we are the server
    */
    ServerSocket tcpServerSocket;

    // Lower level streams
    // good for transfering raw bytes of video data
    InputStream tcpIn;
    OutputStream tcpOut;

    // higher level readers and writers
    // good for transfering text
    BufferedReader bufferedTcpIn;
    BufferedWriter bufferedTcpOut;

    int connectionState;
    final int DISCONNECTED = 1;
    final int CONNECTED = 2;

    static Bitmap receivedBitmap;


    VideoStreaming()
    {
        connectionState = DISCONNECTED;
    }


    public int getLISTENING_SERVICE_PORT()
    {
        return LISTENING_SERVICE_PORT;
    }

    public void setLISTENING_SERVICE_PORT(int LISTENING_SERVICE_PORT)
    {
        this.LISTENING_SERVICE_PORT = LISTENING_SERVICE_PORT;
    }

    /*
    Close the streams and socket
     */
    void closeConnection()
    {
        try
        {
            tcpIn.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            tcpOut.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            bufferedTcpIn.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            bufferedTcpOut.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            tcpSocket.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            tcpServerSocket.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        connectionState = DISCONNECTED;
    }

    /*
   Listen for a connection.  This should only be called from a seperate thread so the main thread isnt blocked
    */
    void listenForMJpegConnection(final ImageView jpegImageView)
    {
        Thread listenForConnectionThread = new Thread()
        {
            public void run()
            {
                try
                {
                    System.out.println("Listening for MJpeg connection!");

                    closeConnection();

                    tcpServerSocket = new ServerSocket(getLISTENING_SERVICE_PORT());
                    tcpSocket = tcpServerSocket.accept();
                    tcpIn = tcpSocket.getInputStream();
                    tcpOut = tcpSocket.getOutputStream();
                    bufferedTcpOut = new BufferedWriter(new OutputStreamWriter(tcpOut));
                    bufferedTcpIn = new BufferedReader(new InputStreamReader(tcpIn));

                    // if we got here with no exception we can assume we are connected
                    connectionState = CONNECTED;

                    System.out.println("Connection has been established!  Now entering image capture loop, yay!");
                    byte[] imageSizeData = new byte[4];
                    long imageSize;
                    while (connectionState == CONNECTED)
                    {
                        // read in an integer (4 bytes) of data to determine the size of the jpeg we are about to receive
                        tcpIn.read(imageSizeData, 0, 4);

                        // fancy bitshifting and ORing to put the 4 bytes we just read into an integer
                        imageSize = pack(imageSizeData[3], imageSizeData[2], imageSizeData[1], imageSizeData[0]);


                        System.out.println("Bytes = " + imageSizeData[3] + " " + imageSizeData[2] + " " + imageSizeData[1] + " " + imageSizeData[0]);
                        System.out.println("Received image of size " + imageSize);

                        int totalBytesReceived = 0;

                        // one megabyte, no way a frame will be bigger than that
                        byte[] receivedByteArray = new byte[1024 * 1024];

                        // keep reading the stream until all bytes of this image have been read
                        // reads in chunks of 1024 bytes at a time
                        while (totalBytesReceived < imageSize)
                        {
                            if (imageSize - totalBytesReceived > 1024)
                                totalBytesReceived += tcpIn.read(receivedByteArray, totalBytesReceived, 1024);
                            else
                                totalBytesReceived += tcpIn.read(receivedByteArray, totalBytesReceived, (int) imageSize - totalBytesReceived);
                        }

                        // send a single byte back to confirm we received a frame
//                        tcpOut.write(0);
//                        tcpOut.flush();

                        receivedBitmap = BitmapFactory.decodeByteArray(receivedByteArray, 0, totalBytesReceived);
                        ((Activity) (MainActivity.utilities.mainContext)).runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                jpegImageView.setImageBitmap(receivedBitmap);
                            }
                        });

                    }

                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };

        listenForConnectionThread.start();
    }

    static boolean sending;
    /*
    sends the length of the jpeg data followed by the data
     */

    public static long pack(int c1, int c2, int c3, int c4)
    {
        return ((0xFFL & c1) << 24) | ((0xFFL & c2) << 16) | ((0xFFL & c3) << 8) | (0xFFL & c4);
    }

    void sendJpegFrame(final byte[] jpegDataByteArray)
    {
        if(!sending)
        new Thread(new Runnable()
        {
            public void run()
            {
                sending=true;
                int dataLength = jpegDataByteArray.length;

                byte dataLengthBytes[] = new byte[4];

                // pack the integer dataLength into 4 bytes
                dataLengthBytes[3] = (byte) ((dataLength & 0xFF000000) >> 24);
                dataLengthBytes[2] = (byte) ((dataLength & 0x00FF0000) >> 16);
                dataLengthBytes[1] = (byte) ((dataLength & 0x0000FF00) >> 8);
                dataLengthBytes[0] = (byte) ((dataLength & 0x000000FF) >> 0);


                byte dataToSend[] = new byte[jpegDataByteArray.length + dataLengthBytes.length];

                System.out.println("Bytes = " + dataLengthBytes[3] + " " + dataLengthBytes[2] + " " + dataLengthBytes[1] + " " + dataLengthBytes[0]);
                System.out.println("size of actual jpeg data to send = " + jpegDataByteArray.length);
                long unpacktest = pack(dataLengthBytes[3], dataLengthBytes[2], dataLengthBytes[1], dataLengthBytes[0]);
                System.out.println("size of unpacked jpeg data to send = " + unpacktest);

                // concatenate both arrays for sending
                for (int i = 0; i < dataLengthBytes.length; i++)
                    dataToSend[i] = dataLengthBytes[i];
                for (int i = dataLengthBytes.length; i < jpegDataByteArray.length + dataLengthBytes.length; i++)
                {
                    dataToSend[i] = jpegDataByteArray[i - dataLengthBytes.length];
                }

                System.out.println("About to send jpeg of length = " + jpegDataByteArray.length + " total length = " + dataToSend.length);
                try
                {
                    tcpOut.write(dataToSend);
                    tcpOut.flush();
                    // read a single byte confirming they received our frame before we continue
//                    tcpIn.read();
                } catch (Exception e)
                {
                    e.printStackTrace();
                }

                sending=false;
            }
        }).start();

    }

    /*
    Try to connect to the other device, this must be done before we send any Jpeg frames
     */
    void connectToDevice(final String ipAddress)
    {
        Thread openConnectionThread = new Thread()
        {
            public void run()
            {
                try
                {
                    closeConnection();

                    System.out.println("Trying to connect to remote device for streaming");
                    tcpSocket = new Socket(ipAddress, getLISTENING_SERVICE_PORT());
                    tcpIn = tcpSocket.getInputStream();
                    tcpOut = tcpSocket.getOutputStream();
                    bufferedTcpOut = new BufferedWriter(new OutputStreamWriter(tcpOut));
                    bufferedTcpIn = new BufferedReader(new InputStreamReader(tcpIn));


                    // if we got here with no exception we can assume we are connected
                    connectionState = CONNECTED;

                    System.out.println("Connected to remote device for streaming!");
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };

        openConnectionThread.start();

    }

    /*
    Returns the ip address of the remote device we are connected to
     */
    String getRemoteIpAddress()
    {

        return tcpSocket.getRemoteSocketAddress().toString();
    }


}