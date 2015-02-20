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
    Socket tcpSocket;

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

    boolean connected;

    static Bitmap receivedBitmap;

    static Object sendFrameLock = new Object();

    VideoStreaming()
    {
        connected = false;
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

        connected = false;
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
                    connected = true;

                    System.out.println("Connection has been established!  Now entering image capture loop, yay!");
                    byte[] imageSizeData = new byte[4];
                    long imageSize;
                    while (connected)
                    {
                        // read in an integer (4 bytes) of data to determine the size of the jpeg we are about to receive
                        tcpIn.read(imageSizeData, 0, 4);

                        // fancy bitshifting and ORing to put the 4 bytes we just read into an integer
                        imageSize = packBytes(imageSizeData[3], imageSizeData[2], imageSizeData[1], imageSizeData[0]);

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


    // packs 4 bytes into a long
    long packBytes(int b4, int b3, int b2, int b1)
    {
        return ((0xFFL & b4) << 24) | ((0xFFL & b3) << 16) | ((0xFFL & b2) << 8) | (0xFFL & b1);
    }

    // unpack first 4 bytes of a long into a byte array
    byte[] unPackBytes(long value)
    {
        byte bytes[] = new byte[4];

        bytes[3] = (byte) ((value & 0xFF000000) >> 24);
        bytes[2] = (byte) ((value & 0x00FF0000) >> 16);
        bytes[1] = (byte) ((value & 0x0000FF00) >> 8);
        bytes[0] = (byte) ((value & 0x000000FF) >> 0);

        return bytes;
    }

    byte[] combineByteArrays(byte a1[], byte[] a2)
    {
        byte dataCombined[] = new byte[a1.length + a2.length ];


        // concatenate both arrays for sending
        for (int i = 0; i < a1.length; i++)
            dataCombined[i] = a1[i];
        for (int i = a1.length; i < a1.length + a2.length; i++)
        {
            dataCombined[i] = a2[i - a1.length];
        }

        return dataCombined;
    }

    void sendJpegFrame(final byte[] jpegDataByteArray)
    {

        new Thread(new Runnable()
        {
            public void run()
            {
                synchronized(this)
                {

                    int dataLength = jpegDataByteArray.length;
                    byte dataLengthBytes[] = unPackBytes(dataLength);

                    byte dataToSend[] = combineByteArrays(dataLengthBytes, jpegDataByteArray);

                    try
                    {
                        tcpOut.write(dataToSend);
                        tcpOut.flush();
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
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
                    connected = true;

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