package com.intercom.video.twoway;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.session.MediaController;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import net.majorkernelpanic.streaming.gl.SurfaceView;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity
{
    /*
    Handles all networking stuff
     */
    Tcp tcpEngine = new Tcp();

    /*
    Some helpful things (screen unlock, etc) that shouldnt go in main activity
     */
    static UsefulStuff usefulStuff;

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
            usefulStuff.ShowToastMessage("Connected to service");
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
            usefulStuff.ShowToastMessage("Disconnected from service");
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
//        usefulStuff.startVideoBroadcast(1234, (SurfaceView)findViewById(R.id.surfaceView));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        usefulStuff = new UsefulStuff(this);

        setContentView(R.layout.activity_main);
        setupButtons();
        startListenerService();

        usefulStuff.ShowToastMessage("oncreate called!");
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent); 
        setIntent(intent);

        usefulStuff.ShowToastMessage("New Intent Received");

        usefulStuff.forceWakeUpUnlock();
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
//        listenerService.stopListeningForConnections();
//        stopService(new Intent(this, ListenerService.class));
        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        switch (item.getItemId())
        {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}