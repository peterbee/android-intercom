package com.intercom.video.twoway;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.content.SharedPreferences;

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
    Handles all the video and audio streaming stuff.
    streamingEngine1 is used for the first device (that initiated the connection) to act as a server and streamingEngine2 to act as a client

    streamingEngine1 is used for the second device (that did not initiate the connection) to act as a client and streamingEngine2 to act as a server

     */
    static VideoStreaming streamingEngine1;

    static VideoStreaming streamingEngine2;

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


    public void settingsMenuBackButtonPressed(View view)
    {
        setContentView(R.layout.activity_main);
    }


    /*
    This is called when we click the establish connection button
    Attempts to establish the tcp connection to another device
    This starts our streaming server and tells the other device to connect to us
     */
    void establishConnection()
    {
        String ipAddress = ipAddressEditText.getText().toString();

        ImageView jpegTestImageView = (ImageView)findViewById(R.id.jpegTestImageView);

        streamingEngine1.listenForMJpegConnection(jpegTestImageView);

        // this unlocks and turns on the other device via service
        tcpEngine.connectToDevice(ipAddress, 1);
    }

    /*
    This is like establishConnection() except is run when when a connection intent is received
     */
    void establishConnectionOnIntent(String ipAddress)
    {
        ImageView jpegTestImageView = (ImageView)findViewById(R.id.jpegTestImageView);

        streamingEngine2.listenForMJpegConnection(jpegTestImageView);

        // this just unlocks and turns on the other device via service
        tcpEngine.connectToDevice(ipAddress, 2);
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

        streamingEngine1 = new VideoStreaming();
        streamingEngine2 = new VideoStreaming();

        utilities = new Utilities(this);

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

        utilities.showToastMessage("Intent Received - " + intent.getExtras().getString("COMMAND"));

        // simply start the activity and turn on the screen, nothing more
        if (COMMAND_STRING.equals(constants.INTENT_COMMAND_START_ACTIVITY))
        {
            utilities.forceWakeUpUnlock();
        }

        // tells us to connect to the remote server and start feeding it our video
        // then start our own remote server and tel the other device to connect
        if (COMMAND_STRING.equals(constants.INTENT_COMMAND_START_STREAMING_FIRST))
        {
            utilities.forceWakeUpUnlock();

            // connect to the remote device and start streaming
            streamingEngine1.connectToDevice(intent.getExtras().getString("EXTRA_DATA"));
            cameraJpegCapture = new CameraJpegCapture(streamingEngine1);
            cameraJpegCapture.startCam();

            // now start our server and tell the other to connect to us
            establishConnectionOnIntent(intent.getExtras().getString("EXTRA_DATA"));
        }

        // tells us to connect to the remote server, this happens second after we have already started our own server and told them to connect
        // the difference between this and INTENT_COMMAND_START_STREAMING_FIRST is that we dont start a new server and tell the other to connect because we already did that
        if (COMMAND_STRING.equals(constants.INTENT_COMMAND_START_STREAMING_SECOND))
        {
            utilities.forceWakeUpUnlock();

            // connect to the remote device and start streaming
            streamingEngine2.connectToDevice(intent.getExtras().getString("EXTRA_DATA"));
            cameraJpegCapture = new CameraJpegCapture(streamingEngine2);
            cameraJpegCapture.startCam();
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
                // TODO: test settings menu listeners
                activateSettingsMenuListeners();
                doRememberDeviceNic();
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

    public void activateSettingsMenuListeners()
        {
            /*
            auto-triggered when device nic is changed
             */
        EditText deviceNIC =(EditText)findViewById(R.id.settings_menu_editText_deviceNic);

        // auto-save on text change for deviceNIC in settings menu
            deviceNIC.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {}
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count)
                {
                    //TODO : create variable and save new nic
                    // note: task refactored into setDeviceNic
                   setDeviceNic(s.toString());
                }

            });
        }

        public void setDeviceNic (String newDeviceNic)
        {
            //TODO: test store device nic as a settings variable
            Log.i(TAG,"Device NIC stored --> "+ newDeviceNic);
            String PREFS_NAME="SETTINGS MENU";
            SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("device_nic", newDeviceNic);
            // Apply the edits!
            editor.apply();
        }

        public String getDeviceNic ()
        {
            //TODO: test pull from settings variable
            Log.i(TAG,"getDeviceNic Called ");
            String PREFS_NAME="SETTINGS MENU";
            SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
            Log.i(TAG,"DeviceNic recovered: "+settings.getString("device_nic","0"));
            return settings.getString("device_nic","0");
        }

        public void doRememberDeviceNic()
        {
            String mDeviceNic=getDeviceNic ();
            EditText mEditText=(EditText)findViewById(R.id.settings_menu_editText_deviceNic);
            mEditText.setText(mDeviceNic);
            return;
        }


}
