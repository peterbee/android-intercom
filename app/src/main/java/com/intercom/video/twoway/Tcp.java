package com.intercom.video.twoway;

import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/*
This class does all the tcp and networking stuff
 */
public class Tcp
{
    private final int DEFAULT_PORT = 22;

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

    Tcp()
    {
        connectionState=DISCONNECTED;
    }

     /*
    Listen for a connection.  This is done in a seperate thread so we do not block the main UI thread
     */
    void listenForConnection()
    {

        Thread listenForConnectionThread = new Thread()
        {
            public void run()
            {
                try
                {
                    System.out.println("Listening for connection!");
                    tcpServerSocket = new ServerSocket(DEFAULT_PORT);
                    tcpSocket = tcpServerSocket.accept();
                    tcpIn=tcpSocket.getInputStream();
                    tcpOut=tcpSocket.getOutputStream();
                    bufferedTcpOut=new BufferedWriter(new OutputStreamWriter(tcpOut));
                    bufferedTcpIn=new BufferedReader(new InputStreamReader(tcpIn));


                    // if we got here with no exception we can assume we are connected
                    connectionState=CONNECTED;

                    // prove that we are actually connected
                    MainActivity.ShowToastMessage("Connected! Client says: "+bufferedTcpIn.readLine());

                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };

        listenForConnectionThread.start();
    }

    /*
    Connect to a device.  This is done in a seperate thread so we do not block the main UI thread
     */
    void connectToDevice(final String ipAddress)
    {

        Thread openConnectionThread = new Thread()
        {
            public void run()
            {
                try
                {
                    tcpSocket = new Socket(ipAddress, DEFAULT_PORT);
                    tcpIn= tcpSocket.getInputStream();
                    tcpOut= tcpSocket.getOutputStream();
                    bufferedTcpOut=new BufferedWriter(new OutputStreamWriter(tcpOut));
                    bufferedTcpIn=new BufferedReader(new InputStreamReader(tcpIn));


                    // if we got here with no exception we can assume we are connected
                    connectionState=CONNECTED;
                    MainActivity.ShowToastMessage("CONNECTED TO SERVER, SAYING HELLO!");

                    // send a message to prove we are connected
                    bufferedTcpOut.write("HELLO!\n");
                    bufferedTcpOut.flush();
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        };

        openConnectionThread.start();

    }


}
