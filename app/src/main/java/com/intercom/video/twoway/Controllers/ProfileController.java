package com.intercom.video.twoway.Controllers;

import android.graphics.Bitmap;
import android.util.Log;

import com.intercom.video.twoway.Interfaces.UpdateDeviceListInterface;
import com.intercom.video.twoway.MainActivity;
import com.intercom.video.twoway.Models.ContactsEntity;
import com.intercom.video.twoway.Network.NetworkDiscovery;
import com.intercom.video.twoway.Network.ProfileSender;
import com.intercom.video.twoway.Network.ProfileServer;
import com.intercom.video.twoway.Utilities.SharedPreferenceAccessor;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
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

    private ConcurrentHashMap<String, ContactsEntity> devices;
    private ContactsEntity currentDevice;
    private SharedPreferenceAccessor sharedPreferenceAccessor;
    private final Object lock = new Object();
    private ArrayList<String> currentlyRetrievingIps;
    private ExecutorService executor;
    private ProfileServer profileServer;
    private Thread serverThread;
    private String ip;
    private UpdateDeviceListInterface mainActivityCallback;
    private NetworkDiscovery network;

    /*Passes wifi manager and bitmap pic for now for testing purposes, need to already have device
        Profile set up
    */
    public ProfileController(MainActivity mainActivity, String ip, NetworkDiscovery nd) {
        network = nd;
        this.ip = ip;
        this.executor = Executors.newCachedThreadPool();
        //Hard Coding Values for testing Purposes
        this.devices = new ConcurrentHashMap<>();
        this.sharedPreferenceAccessor = new SharedPreferenceAccessor(mainActivity);
        refreshDeviceProfile();
        this.currentlyRetrievingIps = new ArrayList<String>();
        profileServer = new ProfileServer(this);
        serverThread = new Thread(profileServer, "ProfileServer");
        serverThread.start();
        this.mainActivityCallback = mainActivity;
    }

    public void killProfileServer()
    {
        profileServer.killProfileServer();
    }

    //Add Contact To Master List
    public void addContact(String ip, ContactsEntity contactToAdd) {
        //String ipToSave = splitIpFromPort(ip);

        if (!this.devices.containsKey(contactToAdd.getIp())) {
            this.devices.put(contactToAdd.getIp(), contactToAdd);
            Log.d("Profile Controller", "Device " + currentDevice.getDeviceName() + " received " +
                    contactToAdd.getDeviceName() + " w00t. ");
        } else {
            Log.i("Duplicate Contact", "Profile Controller attempted to add a duplicate contact");
        }
    }

    public void sendDeviceInfoByIp(final String ip) {
        Runnable profileSender = new ProfileSender(ip, this.currentDevice);
        executor.execute(profileSender);
    }

    //
    public void receiveDeviceInfoByIp(String ip) {
        String freshIp = splitIpFromPort(ip);
        if (this.devices.containsKey(freshIp)) {
            return;
        }
        this.profileServer.requestProfile(freshIp);
    }

    private String splitIpFromPort(String ip) {
        if (ip.contains(":")) {
            String[] splitIp = ip.split(":");
            if (splitIp[0].contains("/")) {
                return splitIp[0].split("/")[1];
            }
            return splitIp[0];
        } else {
            return ip;
        }
    }

    public void refreshDeviceProfile() {
        Bitmap devicePicture = loadProfilePictureFromPreferences();
        String deviceName = getDeviceNickname();
        this.currentDevice = new ContactsEntity(deviceName, devicePicture, network.getMyIp());
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

    public ContactsEntity getProfile() {
        return this.currentDevice;
    }

    public ContactsEntity updateProfilePicture(Bitmap picture) {
        this.currentDevice.setPicture(picture);
        saveProfile();
        return this.currentDevice;
    }

    public ContactsEntity updateProfileName(String name) {
        this.currentDevice.setDeviceName(name);
        saveProfile();
        return this.currentDevice;
    }

    private void saveProfile() {
        this.sharedPreferenceAccessor.writeStringToSharedPrefs(
                SharedPreferenceAccessor.PROFILE_PICTURE,
                ContactsEntity.encodePictureToBase64(this.currentDevice.getPicture()), SharedPreferenceAccessor.SETTINGS_MENU);
        this.sharedPreferenceAccessor.writeStringToSharedPrefs(
                SharedPreferenceAccessor.DEVICE_NICKNAME,
                this.currentDevice.getDeviceName(), SharedPreferenceAccessor.SETTINGS_MENU);
    }

    public ContactsEntity getProfileByIp(String ip) {
        if (this.devices != null && !this.devices.isEmpty()) {
            if (this.devices.containsKey(ip)) {
                return devices.get(ip);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private boolean checkIfProfileCanBeRetrieved(String ip) {
        synchronized (lock) {
            if (currentlyRetrievingIps.contains(ip)) {
                return false;
            } else {
                currentlyRetrievingIps.add(ip);
                return true;
            }
        }
    }

    public void removeIpFromLockedList(String ip) {
        synchronized (lock) {
            currentlyRetrievingIps.remove(ip);
        }
    }

    public ConcurrentHashMap<String, ContactsEntity> getDeviceList() {
        return this.devices;
    }

    public void updateDeviceList() {
        mainActivityCallback.updateDeviceListFromHashMap(this.devices);
    }

    public void getProfilesFromDiscoveredIPs(String[] ips, MainActivity mainActivity) {
        for (String ip : ips) {
            receiveDeviceInfoByIp(ip);
        }
    }
}