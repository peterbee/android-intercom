package com.intercom.video.twoway;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.os.Handler;

import net.majorkernelpanic.streaming.gl.SurfaceView;


public class MainActivity extends Activity
{
    ControlConstants constants = new ControlConstants();

    /*
    Handles all networking stuff
     */
    Tcp tcpEngine = new Tcp();

    /*
    Some helpful things (screen unlock, etc) that shouldn't go in main activity because it will be too much and messy
     */
    static UsefulStuff usefulStuff;

    /*
    Handles all the video and audio streaming stuff
     */
    static VideoStreaming streamingEngine;

    /*
    Used to attempt to connect to another device
     */
    static Button connectButton;

    /*
    Opens and closes video link between devices
     */
    static Button videoLinkButton;

    /*
    Used to enter ip address of other device for connecting
     */
    static EditText ipAddressEditText;

    volatile static ListenerService listenerService;
    volatile static boolean serviceIsBoundToActivity = false;

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection listenerServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            usefulStuff.showToastMessage("Connected to service");
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            com.intercom.video.twoway.ListenerService.LocalBinder binder = (com.intercom.video.twoway.ListenerService.LocalBinder) service;
            listenerService = binder.getService();

            serviceIsBoundToActivity = true;

            // start the service listening for connection
            listenerService.startListeningForConnections();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            usefulStuff.showToastMessage("Disconnected from service");
            serviceIsBoundToActivity = false;
        }
    };

    public void startListenerService()
    {
        Intent service = new Intent(usefulStuff.mainContext, ListenerService.class);
        startService(service);
    }

    void setupButtons()
    {
        connectButton=(Button)findViewById(R.id.connectButton);
        ipAddressEditText=(EditText)findViewById(R.id.ipAddressEditText);

        connectButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                establishConnection();
            }
        });
    }

    /*
    Attempts to establish the tcp connection to another device
     */
    void establishConnection()
    {
        String ipAddress=ipAddressEditText.getText().toString();

        // this just unlocks and turns on the other device via service
        tcpEngine.connectToDevice(ipAddress);

        // and this starts transmitting our video
        streamingEngine.startVideoBroadcast();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        streamingEngine = new VideoStreaming();
        usefulStuff = new UsefulStuff(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        setupButtons();
        startListenerService();
    }

    /*
    This is where we handle messages (as intents) from the service
     */
    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent); 
        setIntent(intent);

        String COMMAND_STRING = intent.getExtras().getString("COMMAND");

        usefulStuff.showToastMessage("Intent Received - " + intent.getExtras().getString("COMMAND"));


        // simply start the activity and turn on the screen, nothing more
        if(COMMAND_STRING.equals(constants.INTENT_COMMAND_START_ACTIVITY))
        {
            usefulStuff.forceWakeUpUnlock();
        }

        // tells us to start streaming a remote video source
        if(COMMAND_STRING.equals(constants.INTENT_COMMAND_START_STREAMING))
        {
            usefulStuff.forceWakeUpUnlock();
            MainActivity.streamingEngine.playVideoStream(intent.getExtras().getString("EXTRA_DATA"));
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Intent theService = new Intent(this, ListenerService.class);
        bindService(theService, listenerServiceConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    public void onPause()
    {

        super.onPause();
    }

    @Override
    public void onStop()
    {

        super.onStop();
    }

    @Override
    public void onDestroy()
    {
        // if we wanted to not destroy the service on activity destroy we could comment this out
        listenerService.stopListeningForConnections();
        stopService(new Intent(this, ListenerService.class));
        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {

    }
}