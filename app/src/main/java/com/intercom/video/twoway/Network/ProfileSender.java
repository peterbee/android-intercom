package com.intercom.video.twoway.Network;

import android.net.Network;
import android.util.Log;

import com.intercom.video.twoway.Models.ContactsEntity;

import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by charles on 4/3/15.
 */
public class ProfileSender implements Runnable{
    private Socket sendProfileSocket;
    private ObjectOutputStream profileOut;
    private final String ip;
    private final ContactsEntity contactToSend;

    public ProfileSender(String ip, ContactsEntity contactToSend)
    {
        this.ip = ip;
        this.contactToSend = contactToSend;
    }

    @Override
    public void run()
    {
        try
        {
            sendProfileSocket = new Socket(ip, NetworkConstants.PROFILE_TRANSFER_PROPER_PORT);
            profileOut = new ObjectOutputStream(sendProfileSocket.getOutputStream());
            profileOut.writeObject(contactToSend);
            profileOut.flush();
        }
        catch(Exception e)
        {
            Log.d("Profile Controller", "Error sending profile over tcp");
        }
        finally{
            closeOutgoingConnections();
        }
    }

    private void closeOutgoingConnections()
    {
        try
        {
            sendProfileSocket.close();

        }catch (Exception e)
        {
            Log.d("Profile Controller", "Could not close sendDeviceSocket");
        }
        try
        {
            profileOut.close();

        }catch (Exception e)
        {
            Log.d("Profile Controller", "Could not close objectOut Socket");
        }
    }
}
