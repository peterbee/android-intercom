package com.intercom.video.twoway;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;
import android.view.Window;
import android.view.WindowManager;

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

    public UsefulStuff()
    {

    }

    /*
    forces screen wake up and unlock
    */
    void forceWakeUpUnlock()
    {
        Window window = ((Activity)MainActivity.context).getWindow();

        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


}
