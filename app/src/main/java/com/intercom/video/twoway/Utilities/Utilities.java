package com.intercom.video.twoway.Utilities;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.intercom.video.twoway.MainActivity;

import java.util.ArrayList;
import java.util.UUID;

/*
This class contains useful stuff that we dont want to put in main activity because it will be big and messy
 */
public class Utilities {
    public Context mainContext;
    private WifiManager wifi;

    public Utilities(Context c) {
        mainContext = c;
    }

    /*
    forces screen wake up and unlock
    TODO make this work without depricated features.
    TODO make this method less shitty and figure out the right way to do it
    */
    public void forceWakeUpUnlock() {

        System.out.println("About to force wakeup unlock");
        KeyguardManager km = (KeyguardManager) mainContext.getSystemService(Context.KEYGUARD_SERVICE);
        final KeyguardManager.KeyguardLock kl = km.newKeyguardLock("MyKeyguardLock");
        kl.disableKeyguard();

        PowerManager pm = (PowerManager) mainContext.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "MyWakeLock");
        wakeLock.acquire();


        Window window = ((Activity) mainContext).getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /*
     * This returns a unique UUID string that should be unique to this device
     * just in case we need it at some point in the future
     */
    public String getDeviceId() {
        final TelephonyManager tm = (TelephonyManager) mainContext
                .getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = ""
                + android.provider.Settings.Secure.getString(
                mainContext.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(),
                ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String deviceId = deviceUuid.toString();

        return deviceId;
    }

    /*
    Lets us show a toast message from any thread
    */
    public void showToastMessage(final String message) {
        ((Activity) mainContext).runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(mainContext, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /*
    send a command to the activity
    This will probably be our primary means of communicating with the activity
    this also wakes the activity and turns on the screen
     */
    public void sendCommandToActivity(String command, String extra) {
        Intent startMainActivityIntent = new Intent(this.mainContext, MainActivity.class);
        startMainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startMainActivityIntent.putExtra("COMMAND", command);
        startMainActivityIntent.putExtra("EXTRA_DATA", extra);

        this.mainContext.startActivity(startMainActivityIntent);
    }

    /**
     * @param mArrayList
     * @return String[] composed of all elements in mArrayList
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * method for converting an Arraylist into a String[].  Used for converting the Ip addresses
     * received from NetworkDiscovery to a easier to use format at this time.
     */
    //todo: can have NetworkDiscovery return string list instead
    //todo: can also move this to utilities
    public String[] convertArrayListToStringArray(ArrayList<String> mArrayList) {
        String[] mStringArray = new String[mArrayList.size()];

//        System.out.println("size of this shit = "+mArrayList.size());

        System.err.println("pre for loop");
        for (int i = 0; i < mArrayList.size(); i++) {
            Log.i("MainActivity_line511", "what the fuck for loop");
            mStringArray[i] = mArrayList.get(i);
        }

        return mStringArray;
    }

    /**
     * @param mArrayList
     * @param mainActivity
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * method for populating mStringArrayIpList
     */
    public void setIpList(ArrayList<String> mArrayList, MainActivity mainActivity) {
        String[] mStringArrayIpList = convertArrayListToStringArray(mArrayList);
        mainActivity.utilities.setIpList(mStringArrayIpList);
    }

    /**
     * @param newIpList
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * gets latest list of discovered IPs from network discovery and sets teh global variable
     */
    public void setIpList(String[] newIpList) {
        MainActivity.mUrlList_as_StringArray = newIpList;
    }
}
