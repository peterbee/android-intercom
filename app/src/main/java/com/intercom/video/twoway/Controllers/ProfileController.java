package com.intercom.video.twoway.Controllers;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.intercom.video.twoway.Models.ContactsEntity;
import com.intercom.video.twoway.Network.NetworkConstants;
import com.intercom.video.twoway.Network.ProfileSender;
import com.intercom.video.twoway.Network.ProfileServer;
import com.intercom.video.twoway.Network.Tcp;
import com.intercom.video.twoway.Utilities.SharedPreferenceAccessor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controls access to profiles, as well as sending and receiving profiles
 */
public class ProfileController {
    private static int INITIAL_PORT = 1025;
    private static int TRANSFER_PORT = 1024;
    private static int PREPARE_FOR_ENTRY = 1;
    private static int PREPARED_FOR_ENTRY = 2;

    private ConcurrentHashMap<String, ContactsEntity> contacts;
    private ContactsEntity currentDevice;
    private InputStream tcpIn;
    private OutputStream tcpOut;
    private Socket socketIn;
    private Socket sendDeviceSocket;
    private Socket receiveDeviceInitiationSocket;
    private ServerSocket receiveDeviceServerSocket;
    private ServerSocket serverSocket;
    private DataOutputStream receiveDeviceInitiationStream;
    private Socket receiveDeviceSocket;
    private ObjectOutputStream objectOut;
    private ObjectInputStream objectIn;
    private Tcp tcpEngine;
    private SharedPreferenceAccessor sharedPreferenceAccessor;
    private ArrayList<Runnable> runningThreads;
    private final Object lock = new Object();
    private ArrayList<String> currentlyRetrievingIps;
    private ExecutorService executor;

    /*Passes wifi manager and bitmap pic for now for testing purposes, need to already have device
        Profile set up
    */
    public ProfileController(Context context)
    {
        this.executor = Executors.newCachedThreadPool();
        this.tcpEngine = new Tcp();
        //Hard Coding Values for testing Purposes
        this.contacts = new ConcurrentHashMap<>();
        this.sharedPreferenceAccessor = new SharedPreferenceAccessor(context);
        refreshDeviceProfile();
        this.currentlyRetrievingIps = new ArrayList<String>();

    }

    //Add Contact To Master List
    public void addContact(String ip, ContactsEntity contactToAdd)
    {
        if(!this.contacts.containsKey(ip))
        {
            this.contacts.put(ip, contactToAdd);
            Log.d("Profile Controller", "Device " + currentDevice.getDeviceName() + " received " +
                    contactToAdd.getDeviceName() + " w00t. ");
        }
        else
        {
            Log.i("Duplicate Contact", "Profile Controller attempted to add a duplicate contact");
        }
    }

    public void sendDeviceInfoByIp(final String ip)
    {
        Runnable profileSender = new ProfileSender(ip, this.currentDevice);
        executor.execute(profileSender);
    }

    //
    public void receiveDeviceInfoByIp(String ip)
    {
        if(this.contacts.containsKey(ip)) {
            return;
        }
        Runnable profileServer = new ProfileServer(this, ip);
        executor.execute(profileServer);
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

    public ContactsEntity getProfileByIp(String ip) {
        if (this.contacts != null && !this.contacts.isEmpty()){
            if(this.contacts.containsKey(ip))
            {
                return contacts.get(ip);
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    private boolean checkIfProfileCanBeRetrieved(String ip)
    {
        synchronized (lock)
        {
            if(currentlyRetrievingIps.contains(ip))
            {
                return false;
            }
            else
            {
                currentlyRetrievingIps.add(ip);
                return true;
            }
        }
    }

    public void removeIpFromLockedList(String ip)
    {
        synchronized (lock)
        {
            currentlyRetrievingIps.remove(ip);
        }
    }
}