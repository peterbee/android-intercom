package com.intercom.video.twoway;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/*
This class contains useful stuff that we dont want to put in main activity because it will be big and messy
 */
public class UsefulStuff
{
    /*
    forces screen wake up.  Need to make this not use depricated features
     */
    static void forceWakeUp()
    {
        PowerManager pm = (PowerManager) MainActivity.context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
        wakeLock.acquire();
    }

    /*
    forces unlock.  Need to make this not use depricated features
     */
    static void forceUnlock()
    {
        KeyguardManager keyguardManager = (KeyguardManager) MainActivity.context.getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock keyguardLock =  keyguardManager.newKeyguardLock("TAG");
        keyguardLock.disableKeyguard();
    }


}
