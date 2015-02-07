package com.intercom.video.twoway;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class ListenerService extends Service 
{
    private Tcp serviceTcpEngine = new Tcp();

    private boolean listeningForConnections = false;

    // we don't use this but the service wants to be assigned an id when it is created
	private final int SERVICE_ID = 12345;

	// Binder given to clients
	private final IBinder mBinder = new LocalBinder();

	/**
	 * Class used for the client Binder. Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder
	{
		ListenerService getService()
		{
			// Return this instance of LocalService so clients can call public
			// methods
			return ListenerService.this;
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
        System.out.println("New service created!");
		// If we get killed, after returning from here, restart the service
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return mBinder;
	}


	@Override
	public void onCreate()
	{
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this,
                12345, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Resources res = this.getResources();
        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.service_icon)
                .setLargeIcon(BitmapFactory.decodeResource(res
                        , R.drawable.service_icon))
                .setTicker("listener service")
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentTitle("listener service")
                .setContentText("listener service");
        Notification n = builder.build();


        startForeground(SERVICE_ID, n);

		super.onCreate();
	}


    public void stopListeningForConnections()
    {
        listeningForConnections=false;
    }

    /*
    start listening for connections from other devices
     */
    public void startListeningForConnections()
    {
        Thread listenForConnectionThread;
        if(!listeningForConnections)
        {
            listeningForConnections = true;
            listenForConnectionThread = new Thread()
            {
                public void run()
                {
                    while (listeningForConnections)
                    {
                        serviceTcpEngine.listenForConnection();

                        // now just close the connection since this is only proof of concept
                        serviceTcpEngine.closeConnection();

                        // start the main activity
                        startMainActivity();
                    }
                }
            };
            listenForConnectionThread.start();
        }
    }

    /*
    Starts the main activity from the service
     */
    public void startMainActivity()
    {
        Intent startMainActivityIntent = new Intent(getBaseContext(), MainActivity.class);
        startMainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplication().startActivity(startMainActivityIntent);
    }

}