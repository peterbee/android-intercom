package com.intercom.video.twoway.Controllers;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.intercom.video.twoway.Models.ContactsEntity;
import com.intercom.video.twoway.Network.NetworkConstants;
import com.intercom.video.twoway.Network.Tcp;
import com.intercom.video.twoway.Utilities.SharedPreferenceAccessor;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Must be in Listener Service - Proof of concept
 * Assumes there
 */
public class ProfileController {
    private static int PORT = 6644;
    private static int PREPARE_FOR_ENTRY = 1;
    private static int PREPARED_FOR_ENTRY = 2;

    private HashMap<String, ContactsEntity> contacts;
    private ContactsEntity currentDevice;
    private InputStream tcpIn;
    private OutputStream tcpOut;
    private Socket tcpSocket;
    private ServerSocket serverSocket;
    private ObjectOutputStream objectOut;
    private ObjectInputStream objectIn;
    private Tcp tcpEngine;
    private SharedPreferenceAccessor sharedPreferenceAccessor;

    /*Passes wifi manager and bitmap pic for now for testing purposes, need to already have device
        Profile set up
    */
    public ProfileController(Context context)
    {
        tcpEngine = new Tcp();
        //Hard Coding Values for testing Purposes
        contacts = new HashMap<>();
        this.sharedPreferenceAccessor = new SharedPreferenceAccessor(context);
        refreshDeviceProfile();
    }

    //Add Contact To Master List
    public synchronized void addContact(String ip, ContactsEntity contactToAdd)
    {
        if(!this.contacts.containsKey(ip))
        {
            this.contacts.put(ip, contactToAdd);
        }
        else
        {
            Log.i("Duplicate Contact", "Profile Controller attempted to add a duplicate contact");
        }
    }

    //Some Collection of profiles
    //Dunno if it's stored on the device
    public void sendDeviceInfoByIp(String ip)
    {
        Thread deviceProfileTransfer = new Thread() {
            public void run(){
                try
                {
                    int response;
                    boolean entry = false;
                    serverSocket = new ServerSocket(PORT);
                    tcpSocket = serverSocket.accept();
                    objectOut = new ObjectOutputStream(tcpSocket.getOutputStream());
                    objectIn = new ObjectInputStream(tcpSocket.getInputStream());

                    tcpOut.write(ProfileController.PREPARE_FOR_ENTRY);
                    tcpOut.flush();

                    response = tcpIn.read();

                    if(response == ProfileController.PREPARE_FOR_ENTRY)
                    {
                        entry = true;
                    }

                    if(entry) {
                        objectOut.writeObject(currentDevice);
                        objectOut.flush();
                    }
                }
                catch(Exception e)
                {
                    Log.i("Bad Profile Transfer", "There was an error transferring profiles");
                }
            }
        };
    }

    public void receiveDeviceInfoByIP(final String ip)
    {
        Thread deviceProfileTransfer = new Thread() {
            public void run(){
                try
                {
                    int response;
                    boolean entry = false;
                    serverSocket = new ServerSocket(PORT);
                    tcpSocket = serverSocket.accept();
                    objectOut = new ObjectOutputStream(tcpSocket.getOutputStream());
                    objectIn = new ObjectInputStream(tcpSocket.getInputStream());

                    response = tcpIn.read();

                    if(response == ProfileController.PREPARE_FOR_ENTRY)
                    {
                        tcpOut.write(ProfileController.PREPARE_FOR_ENTRY);
                        tcpOut.flush();
                        entry = true;
                    }

                    if(entry) {
                        addContact(ip, (ContactsEntity) objectIn.readObject());
                    }
                }
                catch(Exception e)
                {
                    Log.i("Bad Profile Transfer", "There was an error transferring profiles");
                }
            }
        };
    }

    private void refreshDeviceProfile()
    {
        Bitmap devicePicture = loadProfilePictureFromPreferences();
        String deviceName = getDeviceNickname();
        this.currentDevice = new ContactsEntity(deviceName, devicePicture);
    }

    private Bitmap loadProfilePictureFromPreferences() {
        String picture = this.sharedPreferenceAccessor.loadStringFromSharedPreferences(
                SharedPreferenceAccessor.SETTINGS_MENU, SharedPreferenceAccessor.PROFILE_PICTURE);
        if (picture.equals(SharedPreferenceAccessor.NO_SUCH_SAVED_PREFERENCE)) {
            Log.d("No Profile Picture", "There is no saved Profile picture");
            return null;
        } else {
            // Returns the Uri for a photo stored on disk given the fileName
            try {
                Bitmap bitmap = ContactsEntity.decodePictureFromBase64(picture);
                return bitmap;
            } catch (Exception e) {
                Log.d("No Image", "Profile Image does not exist");
                return null;
            }
        }
    }

    private String getDeviceNickname() {
        return this.sharedPreferenceAccessor.loadStringFromSharedPreferences(
                SharedPreferenceAccessor.SETTINGS_MENU,
                SharedPreferenceAccessor.DEVICE_NICKNAME);
    }

    public ContactsEntity getProfile()
    {
        return this.currentDevice;
    }

    public ContactsEntity updateProfilePicture(Bitmap picture)
    {
        this.currentDevice.setPicture(picture);
        saveProfile();
        return this.currentDevice;
    }

    public ContactsEntity updateProfileName(String name)
    {
        this.currentDevice.setDeviceName(name);
        saveProfile();
        return this.currentDevice;
    }

    private void saveProfile()
    {
        this.sharedPreferenceAccessor.writeStringToSharedPrefs(
                SharedPreferenceAccessor.PROFILE_PICTURE,
                ContactsEntity.encodePictureToBase64(this.currentDevice.getPicture()), SharedPreferenceAccessor.SETTINGS_MENU);
        this.sharedPreferenceAccessor.writeStringToSharedPrefs(
                SharedPreferenceAccessor.DEVICE_NICKNAME,
                this.currentDevice.getDeviceName(),SharedPreferenceAccessor.SETTINGS_MENU);
    }

}