package com.intercom.video.twoway.Network;

import android.util.Log;

import com.intercom.video.twoway.Models.ContactsEntity;

import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * @author Charles Toll on 4/3/15.
 * This class transmits profile data to other devices
 */
public class ProfileSender implements Runnable{
    private Socket sendProfileSocket;
    private ObjectOutputStream profileOut;
    private final String ip;
    private final ContactsEntity contactToSend;

    /**
     * Constructor
     *
     * @param ip            The destination IP address
     * @param contactToSend ContactsEntity to be sent (typically this devices')
     */
    public ProfileSender(String ip, ContactsEntity contactToSend)
    {
        this.ip = ip;
        this.contactToSend = contactToSend;
    }

    /**
     * Runnable section that creates the connection to the other devices and sends the
     * ContactsEntity
     */
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
        catch(Exception e) {
            Log.d("ProfileSender", "Error sending profile over tcp");
        }
        finally{
            closeOutgoingConnections();
        }
    }

    /**
     * gracefully closes up our socket.
     */
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
