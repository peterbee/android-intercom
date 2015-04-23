package com.intercom.video.twoway.Network;

import android.util.Log;

import com.intercom.video.twoway.Controllers.ProfileController;
import com.intercom.video.twoway.Models.ContactsEntity;

import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Charles Toll on 4/3/15.
 * This class creates a standing network listener for receiving profiles from other devices.
 */
public class ProfileServer implements Runnable{
    private Socket receiveDeviceSocket;
    private ServerSocket receiveDeviceServerSocket;
    private ObjectInputStream profileIn;
    private final ProfileController callingController;
    boolean profileServerRunning; //flag for gracefully killing the server

    /**
     * Constructor
     *
     * @param cc The ProfileController that created this server
     */
    public ProfileServer(ProfileController cc)
    {
        this.callingController = cc;
    }

    /**
     * method for gracefully killing the profile server.  Closes all sockets and sets
     * profileServerRunning = false;
     */
    public void killProfileServer()
    {
        profileServerRunning=false;
        try
        {
            receiveDeviceServerSocket.close();
        }
        catch(Exception e) {
            Log.d("ProfileServer", "Could not close receiveDeviceServerSocket");
        }

        try
        {
            receiveDeviceSocket.close();
        }
        catch(Exception e) {
            Log.d("ProfileServer", "Could not close receiveDeviceSocket");
        }
    }

    /**
     * Sets up the listener and accepts all incoming connections.  Attempts to convert all incoming
     * data to ContactsEntity.
     */
    private void initiateProfileServer()
    {
        try {
            receiveDeviceServerSocket = new ServerSocket(
                    NetworkConstants.PROFILE_TRANSFER_PROPER_PORT);
            profileServerRunning = true;
            while (profileServerRunning) {
                receiveDeviceSocket = receiveDeviceServerSocket.accept();
                profileIn = new ObjectInputStream(receiveDeviceSocket.getInputStream());

                try {
                    ContactsEntity incoming = (ContactsEntity) profileIn.readObject();
                    callingController.addContact(
                            receiveDeviceSocket.getRemoteSocketAddress().toString(), incoming);
                    callingController.updateDeviceList();
                } catch (ClassCastException e) {
                    Log.d("ProfileServer", "Object could not be cast to ContactsEntity");
                } catch (ClassNotFoundException e) {
                    Log.d("ProfileServer", "Class could not be found?");
                }
            }

        } catch (Exception e) {
            Log.d("ProfileServer", "General exception in initiateProfileServer");
        }

    }

    /**
     * Requests a device to send their Profile information
     * @param ip The ip address of the device that we wish to ask for the profile of
     */
    public void requestProfile(final String ip){
        Thread requestProfile = new Thread()
        {
            public void run() {
                try {
                    Socket receiveDeviceInitiationSocket = new Socket(ip, NetworkConstants.PROFILE_TRANSFER_INITIAL_PORT);
                    DataOutputStream receiveDeviceInitiationStream = new DataOutputStream(
                            receiveDeviceInitiationSocket.getOutputStream());
                    receiveDeviceInitiationStream.write(NetworkConstants.PROFILE);
                    receiveDeviceInitiationStream.flush();
                    receiveDeviceInitiationStream.close();
                    receiveDeviceInitiationSocket.close();
                } catch (NullPointerException e) {
                    Log.d("ProfileServer", "Received no data (data was NULL)");
                    e.printStackTrace(System.err);
                } catch (Exception e) {
                    Log.d("ProfileServer", "Threw general exception in requestProfile");
                    e.printStackTrace(System.err);
                } finally {
                    callingController.removeIpFromLockedList(ip);
                }
            }
        };
        requestProfile.start();
    }

    @Override
    public void run()
    {
        initiateProfileServer();
    }
}
