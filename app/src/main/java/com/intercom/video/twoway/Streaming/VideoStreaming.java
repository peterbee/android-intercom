package com.intercom.video.twoway.Streaming;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.intercom.video.twoway.Utilities.Utilities;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

/*
This class contains things that deal with transmitting and receiving video / audio streams
 */

public class VideoStreaming
{

    private int LISTENING_SERVICE_PORT = 2049;
    private Utilities utilities;

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

    boolean connected;

    Bitmap receivedBitmap;

    Object sendFrameLock = new Object();
    Object decodeFrameLock = new Object();

    Audio audioEngine;

    public VideoStreaming(Audio a, Utilities utilities)
    {
        this.utilities = utilities;
        connected = false;
        audioEngine=a;
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
    public void closeConnection()
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
    static int receivedcount=0;

    /*
   Listen for a connection.  This should only be called from a seperate thread so the main thread isnt blocked
    */
    public void listenForMJpegConnection(final ImageView jpegImageView)
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

                    // if we got here with no exception we can assume we are connected
                    connected = true;

                    System.out.println("Connection has been established!  Now entering image capture loop, yay!");
                    byte[] imageSizeData = new byte[4];
                    byte[] audioSizeData = new byte[4];

                    long imageSize;
                    long audioSize;

                    while (connected)
                    {

                        // read in an integer (4 bytes) of data to determine the size of the jpeg we are about to receive
                        tcpIn.read(imageSizeData, 0, 4);
                        // read in an integer (4 bytes) of data to determine the size of the jpeg we are about to receive
                        tcpIn.read(audioSizeData, 0, 4);

                        // fancy bitshifting and ORing to put the 4 bytes we just read into an integer
                        imageSize = packBytes(imageSizeData[3], imageSizeData[2], imageSizeData[1], imageSizeData[0]);
                        audioSize = packBytes(audioSizeData[3], audioSizeData[2], audioSizeData[1], audioSizeData[0]);

                        int totalJpegBytesReceived = 0;
                        int totalAudioBytesReceived = 0;

                        // one megabyte, no way a frame will be bigger than that
                        byte[] receivedJpegByteArray = new byte[1024 * 1024];
                        byte[] receivedAudioByteArray = new byte[1024 * 1024];

                        // read the jpeg portion of the message
                        // keep reading the stream until all bytes of this image have been read
                        // reads in chunks of 1024 bytes at a time
                        while (totalJpegBytesReceived < imageSize)
                        {
                            if (imageSize - totalJpegBytesReceived > 1024)
                                totalJpegBytesReceived += tcpIn.read(receivedJpegByteArray, totalJpegBytesReceived, 1024);
                            else
                                totalJpegBytesReceived += tcpIn.read(receivedJpegByteArray, totalJpegBytesReceived, (int) imageSize - totalJpegBytesReceived);
                        }


                        // read the audio portion of the message
                        // keep reading the stream until all bytes of this audio have been read
                        // reads in chunks of 1024 bytes at a time
                        while (totalAudioBytesReceived < audioSize)
                        {
                            if (audioSize - totalAudioBytesReceived > 1024)
                                totalAudioBytesReceived += tcpIn.read(receivedAudioByteArray, totalAudioBytesReceived, 1024);
                            else
                                totalAudioBytesReceived += tcpIn.read(receivedAudioByteArray, totalAudioBytesReceived, (int) audioSize - totalAudioBytesReceived);
                        }

                        // asynchronously decode the audio and video so that shit doesnt back up and cause increasing delays
                        decodeFrameAndAudioAsynch(totalJpegBytesReceived, receivedJpegByteArray, totalAudioBytesReceived, receivedAudioByteArray, jpegImageView);
                    }

                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };

        listenForConnectionThread.start();
    }



    // this is used to so that we dont run multiple decode threads at once
    // this is better than a synchronized section because synchonized sections will stack up and wait for others to finish
    // this simple returns the method so nothing gets backed up
    boolean currentlyDecodingFrame;

    // asynchronously decode a frame of video and audio so that we don't cause tcp stack to get backed up with data
    // during the decode process
    public void decodeFrameAndAudioAsynch(final int jpegLength, final byte[] jpegData, final int audioLength, final byte[] audioData, final ImageView jpegImageView)
    {
        Thread decodeFrameThread = new Thread()
        {
            public void run()
            {
                if (!currentlyDecodingFrame)
                {
                    currentlyDecodingFrame = true;
                    try
                    {
                        receivedBitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegLength);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    ((Activity) (utilities.mainContext)).runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                jpegImageView.setImageBitmap(receivedBitmap);
                            } catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    });
                    try
                    {
                        audioEngine.playAudioChunk(Arrays.copyOf(audioData, audioLength));
                    } catch (Exception e)
                    {

                    }
                }
                currentlyDecodingFrame=false;
            }
        };

        decodeFrameThread.start();
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
    static int sentcount=0;
    void sendJpegFrame(final byte[] jpegDataByteArray, final byte[] audioDataByteArray)
    {

        new Thread(new Runnable()
        {
            public void run()
            {
                    int jpegDataLength = jpegDataByteArray.length;
                    int audioDataLength = audioDataByteArray.length;
                    byte jpegDataLengthBytes[] = unPackBytes(jpegDataLength);
                    byte audioDataLengthBytes[] = unPackBytes(audioDataLength);

                    byte packetSizePrefixBytes[] = combineByteArrays(jpegDataLengthBytes, audioDataLengthBytes);

                    byte audioAndJpegDataCombined[] = combineByteArrays(jpegDataByteArray, audioDataByteArray);
                    byte dataToSend[] = combineByteArrays(packetSizePrefixBytes, audioAndJpegDataCombined);

                try
                    {
                        tcpOut.write(dataToSend);
                        tcpOut.flush();
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
            }
        }).start();

    }

    /*
    Try to connect to the other device, this must be done before we send any Jpeg frames
     */
    public void connectToDevice(final String ipAddress)
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