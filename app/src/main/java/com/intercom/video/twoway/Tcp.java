package com.intercom.video.twoway;

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
    private int LISTENING_SERVICE_PORT = 1025;

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
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            tcpOut.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            bufferedTcpIn.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            bufferedTcpOut.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            tcpSocket.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            tcpServerSocket.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        connectionState=DISCONNECTED;
    }

    /*
   Listen for a connection.  This should only be called from a seperate thread so the main thread isnt blocked
    */
    void listenForConnection()
    {
        try
        {
            System.out.println("Listening for connection!");

            closeConnection();

            tcpServerSocket = new ServerSocket(getLISTENING_SERVICE_PORT());
            tcpSocket = tcpServerSocket.accept();
            tcpIn = tcpSocket.getInputStream();
            tcpOut = tcpSocket.getOutputStream();
            bufferedTcpOut = new BufferedWriter(new OutputStreamWriter(tcpOut));
            bufferedTcpIn = new BufferedReader(new InputStreamReader(tcpIn));


            // if we got here with no exception we can assume we are connected
            connectionState = CONNECTED;

        } catch (Exception e)
        {
            e.printStackTrace();
        }

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
                    closeConnection();

                    tcpSocket = new Socket(ipAddress, getLISTENING_SERVICE_PORT());
                    tcpIn= tcpSocket.getInputStream();
                    tcpOut= tcpSocket.getOutputStream();
                    bufferedTcpOut=new BufferedWriter(new OutputStreamWriter(tcpOut));
                    bufferedTcpIn=new BufferedReader(new InputStreamReader(tcpIn));


                    // if we got here with no exception we can assume we are connected
                    connectionState=CONNECTED;
                    MainActivity.usefulStuff.ShowToastMessage("CONNECTED TO SERVER!");

                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    MainActivity.usefulStuff.ShowToastMessage("Error Connecting");
                }

                // close connection since this is only a demonstration
                closeConnection();
            }
        };

        openConnectionThread.start();

    }


}
