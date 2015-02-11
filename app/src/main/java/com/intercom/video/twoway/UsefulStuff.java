package com.intercom.video.twoway;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.widget.MediaController;
import android.net.Uri;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.VideoView;

import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

import java.util.UUID;

/*
This class contains useful stuff that we dont want to put in main activity because it will be big and messy
 */
public class UsefulStuff
{
    static Context mainContext;

    public UsefulStuff(Context c)
    {
        mainContext=c;
    }

    /*
    forces screen wake up and unlock
    TODO make this work without depricated features.
    TODO make this method less shitty and figure out the right way to do it
    */
    void forceWakeUpUnlock()
    {
        KeyguardManager km = (KeyguardManager) mainContext.getSystemService(Context.KEYGUARD_SERVICE);
        final KeyguardManager.KeyguardLock kl = km .newKeyguardLock("MyKeyguardLock");
        kl.disableKeyguard();

        PowerManager pm = (PowerManager) mainContext.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "MyWakeLock");
        wakeLock.acquire();


        Window window = ((Activity)mainContext).getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /*
     * This returns a unique UUID string that should be unique to this device
     * just in case we need it at some point in the future
     */
    static String getDeviceId()
    {
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
    static void showToastMessage(final String message)
    {
        ((Activity)mainContext).runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(mainContext, message, Toast.LENGTH_LONG).show();
            }
        });
    };
}
