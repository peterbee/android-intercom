package com.intercom.video.twoway;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.text.Layout;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.UUID;

/*
This class contains useful stuff that we dont want to put in main activity because it will be big and messy
 */
public class UsefulStuff
{
    public UsefulStuff()
    {

    }

    /*
    forces screen wake up and unlock
    TODO make this work without depricated features.
    */
    void forceWakeUpUnlock()
    {
        PowerManager pm = (PowerManager) MainActivity.context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
        wakeLock.acquire();

        KeyguardManager keyguardManager = (KeyguardManager) MainActivity.context.getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
        keyguardLock.disableKeyguard();
    }

    /*
     * This returns a unique UUID string that should be unique to this device
     * just in case we need it at some point in the future
     */
    static String getDeviceId()
    {
        final TelephonyManager tm = (TelephonyManager) MainActivity.context
                .getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = ""
                + android.provider.Settings.Secure.getString(
               MainActivity.context.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(),
                ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String deviceId = deviceUuid.toString();

        return deviceId;
    }

    /*
    Lets us show a toast message from any thread
    */
    static void ShowToastMessage(final String message)
    {
        ((Activity)MainActivity.context).runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(MainActivity.context, message, Toast.LENGTH_LONG).show();
            }
        });
    };
}
