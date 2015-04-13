package com.intercom.video.twoway.Network;

import android.util.Log;

import com.intercom.video.twoway.Controllers.ProfileController;
import com.intercom.video.twoway.Models.ContactsEntity;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by charles on 4/3/15.
 */
public class ProfileServer implements Runnable{
    private Socket receiveDeviceSocket;
    private ServerSocket receiveDeviceServerSocket;
    private ObjectInputStream profileIn;
    private final ProfileController callingController;

    public ProfileServer(ProfileController cc)
    {
        this.callingController = cc;
    }

    boolean profileServerRunning;
    public void killProfileServer()
    {
        profileServerRunning=false;
        try
        {
            receiveDeviceServerSocket.close();
        }
        catch(Exception e)
        {

        }

        try
        {
            receiveDeviceSocket.close();
        }
        catch(Exception e)
        {

        }
    }
    private void initiateProfileServer()
    {
        try {
            receiveDeviceServerSocket = new ServerSocket(
                    NetworkConstants.PROFILE_TRANSFER_PROPER_PORT);
            profileServerRunning=true;
            while(profileServerRunning) { //TODO: change this to stop on a flag
                receiveDeviceSocket = receiveDeviceServerSocket.accept();
                profileIn = new ObjectInputStream(receiveDeviceSocket.getInputStream());

                try {
                    ContactsEntity incoming = (ContactsEntity) profileIn.readObject();
                    callingController.addContact(
                            receiveDeviceSocket.getRemoteSocketAddress().toString(), incoming);
                    callingController.updateDeviceList();
                } catch (ClassCastException e) {
                    System.err.println("Done fucked up casting profile");
                    //Log.d("Profile Controller", "Error casting class sent over tcp");
                } catch (ClassNotFoundException e) {
//                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
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
                    System.err.println("Done fucked up receiving profile: Null Pointer");
                    e.printStackTrace(System.err);
                    Log.d("Profile Controller", "There was an error Receiving profiles over tcp");
                } catch (Exception e) {
                    System.err.println("Done fucked up receiving profile: General Exception");
                    e.printStackTrace(System.err);
                    System.err.println(e.getMessage());
                    Log.d("Profile Controller", "There was an error Receiving profiles over tcp");
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
