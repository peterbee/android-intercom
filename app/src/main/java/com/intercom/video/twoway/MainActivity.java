package com.intercom.video.twoway;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity
{

    ControlConstants constants = new ControlConstants();

    // used in logcat logging
    String TAG = "Two-Way:";

    /*
    Handles all networking stuff
     */
    Tcp tcpEngine = new Tcp();

    /*
    captures jpeg frames from camera and converts them to bytes for transmission
     */
    CameraJpegCapture cameraJpegCapture;


    /*
    Some helpful things (screen unlock, etc) that shouldn't go in main activity because it will be too much and messy
     */
    static Utilities utilities;

    /*
    Handles all the video and audio streaming stuff
     */
    static VideoStreaming streamingEngine;

    /*
    Used to attempt to connect to another device
     */
    static Button connectButton;

    /*
    These buttons and checkbox are present in settings_menu layout
    sm = Settings Menu
     */
    static Button smSave, smCancel;
    static ImageButton smImageButtonBack;
    static CheckBox smCheckBoxUseCamaraView;
    static ImageView smDeviceAvatar;
    static TextView smDeviceNic, smLableDeviceNicL;

    /*
    Keeps track of what current layout id is
     */
    int currentLayoutId;

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

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection listenerServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            utilities.showToastMessage("Connected to service");
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
            utilities.showToastMessage("Disconnected from service");
            serviceIsBoundToActivity = false;
        }
    };

    public void startListenerService()
    {
        Intent service = new Intent(utilities.mainContext, ListenerService.class);
        startService(service);
    }

    void setupButtons()
    {
        connectButton = (Button) findViewById(R.id.connectButton);
        ipAddressEditText = (EditText) findViewById(R.id.ipAddressEditText);
        connectButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                establishConnection();
            }
        });

        // Settings Menu Controls


        //smSave=(Button)findViewById(R.id.settings_menu_button_save);
        /*
        smCancel=(Button)findViewById(R.id.settings_menu_button_cancel);
        smCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                // TODO: get stakeholder definition on what this does
                // cancel =?
                // setting up to return to main screen
                // possible other implementation = revert all values to what they were
                setContentView(R.layout.activity_main);
            }
        });
        */


    }


    // attach listener to imageButton
    // used in settings menu
    public void addListenerToImageButton()
    {
        smImageButtonBack = (ImageButton) findViewById(R.id.settings_menu_imagebutton_back);

        smImageButtonBack.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                // TODO: BUG: this does not work
                setContentView(R.layout.activity_main);
            }
        });
    }

    public void settingsMenuBackButtonPressed(View view)
    {
        setContentView(R.layout.activity_main);
    }


    /*
    Attempts to establish the tcp connection to another device
     */
    void establishConnection()
    {
        String ipAddress = ipAddressEditText.getText().toString();

        // this just unlocks and turns on the other device via service
//        tcpEngine.connectToDevice(ipAddress);

        // and this starts transmitting our video
//        streamingEngine.startVideoBroadcast();


        streamingEngine.connectToDevice(ipAddress);
        cameraJpegCapture = new CameraJpegCapture(streamingEngine);
        cameraJpegCapture.startCam();
    }

    /*
    keeps track of current layout id as Int
     */
    @Override
    public void setContentView(int layoutResID)
    {
        this.currentLayoutId = layoutResID;
        super.setContentView(layoutResID);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        streamingEngine = new VideoStreaming();
        utilities = new Utilities(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        setupButtons();
        startListenerService();

        ImageView jpegTestImageView = (ImageView)findViewById(R.id.jpegTestImageView);
        streamingEngine.listenForMJpegConnection(jpegTestImageView);
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

        utilities.showToastMessage("Intent Received - " + intent.getExtras().getString("COMMAND"));

        // simply start the activity and turn on the screen, nothing more
        if (COMMAND_STRING.equals(constants.INTENT_COMMAND_START_ACTIVITY))
        {
            utilities.forceWakeUpUnlock();
        }

        // tells us to start streaming a remote video source
        if (COMMAND_STRING.equals(constants.INTENT_COMMAND_START_STREAMING))
        {
            utilities.forceWakeUpUnlock();
//            MainActivity.streamingEngine.playVideoStream(intent.getExtras().getString("EXTRA_DATA"));
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
        //back from settings is main screen
        if (this.currentLayoutId == R.layout.settings_menu)
        {
            setContentView(R.layout.activity_main);
        }
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
            case R.id.action_view_profile:
                setContentView(R.layout.settings_menu);
                return true;

            case R.id.action_home:
                setContentView(R.layout.activity_main);
                return true;

            case R.id.action_find_peers:
                Log.i(TAG, " wifiPeerDiscovery button pushed ( step 1) \n"); //debug
                // TODO:do wifi peer discovery
                return true;

            case R.id.action_listen:
                return true;

            case R.id.action_connect:
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
