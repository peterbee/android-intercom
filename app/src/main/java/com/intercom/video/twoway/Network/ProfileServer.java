package com.intercom.video.twoway.Network;

import android.util.Log;

import com.intercom.video.twoway.Controllers.ProfileController;
import com.intercom.video.twoway.Models.ContactsEntity;

import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;

/**
 * Created by charles on 4/3/15.
 */
public class ProfileServer implements Runnable{
    private DataOutputStream receiveDeviceInitiationStream;
    private Socket receiveDeviceSocket;
    private Socket receiveDeviceInitiationSocket;
    private ServerSocket receiveDeviceServerSocket;
    private ObjectInputStream profileIn;
    private final ProfileController callingController;
    private final String ip;

    public ProfileServer(ProfileController cc, String ip)
    {
        this.callingController = cc;
        this.ip = ip;
    }

    private void requestProfile()
    {
        try
        {
            //Start Listening first, just in case why not
            receiveDeviceServerSocket = new ServerSocket(NetworkConstants.PROFILE_TRANSFER_PROPER_PORT);
            System.err.print("1");
            receiveDeviceInitiationSocket = new Socket(ip, NetworkConstants.PROFILE_TRANSFER_INITIAL_PORT);
            System.err.print("2");
            receiveDeviceInitiationStream = new DataOutputStream(
                    receiveDeviceInitiationSocket.getOutputStream());
            System.err.print("3");
            receiveDeviceInitiationStream.write(NetworkConstants.PROFILE);
            System.err.print("4");
            receiveDeviceInitiationStream.flush();
            System.err.print("5");
            receiveDeviceServerSocket.setSoTimeout(5000);
            System.err.print("6");
            receiveDeviceSocket = receiveDeviceServerSocket.accept();
            System.err.print("7");
            receiveDeviceInitiationStream.close();
            System.err.print("8");
            receiveDeviceInitiationSocket.close();
            System.err.print("9");
            profileIn = new ObjectInputStream(receiveDeviceSocket.getInputStream());
            System.err.print("10");
            try {
                ContactsEntity incoming = (ContactsEntity) profileIn.readObject();
                callingController.addContact(ip, incoming);

            }
            catch(ClassCastException e)
            {
                System.err.println("Done fucked up casting profile");
                //Log.d("Profile Controller", "Error casting class sent over tcp");
            }
        }
        catch(NullPointerException e)
        {
            System.err.println("Done fucked up receiving profile: Null Pointer");
            e.printStackTrace(System.err);
            Log.d("Profile Controller", "There was an error Receiving profiles over tcp");
        }
        catch(Exception e)
        {
            System.err.println("Done fucked up receiving profile: General Exception");
            e.printStackTrace(System.err);
            System.err.println(e.getMessage());
            Log.d("Profile Controller","There was an error Receiving profiles over tcp");
        }
        finally{
            callingController.removeIpFromLockedList(ip);
            closeIncomingConnections();
        }
    }

    private void closeIncomingConnections()
    {
        try {
            System.err.println("Closing receiveDeviceServerSocket");
            receiveDeviceServerSocket.close();

        } catch (Exception e) {
            System.err.println("Closing receiveDeviceServerSocket");
            Log.d("Profile Controller", "Could not close receiveDeviceServerSocket");
        }
        try {
            System.err.println("Closing receiveDeviceInitiationSocket");
            receiveDeviceInitiationSocket.close();

        } catch (Exception e) {
            Log.d("Profile Controller", "Could not close receiveDeviceInitiationSocket");
        }
        try {
            System.err.println("Closing receiveDeviceInitiationStream");
            receiveDeviceInitiationStream.close();

        } catch (Exception e) {
            Log.d("Profile Controller", "Could not close receiveDeviceInitiationStream");
        }
        try {
            System.err.println("Closing receiveDeviceSocket");
            receiveDeviceSocket.close();

        } catch (Exception e) {
            Log.d("Profile Controller", "Could not close receiveDeviceSocket");
        }
        try {
            System.err.println("Closing receiveDeviceSocket");
            receiveDeviceSocket.close();

        } catch (Exception e) {
            Log.d("Profile Controller", "Could not close receiveDeviceSocket");
        }
        try {
            System.err.println("Closing profileIn");
            profileIn.close();

        } catch (Exception e) {
            Log.d("Profile Controller", "Could not close objectIn");
        }
    }

    @Override
    public void run()
    {
        requestProfile();
    }
}
