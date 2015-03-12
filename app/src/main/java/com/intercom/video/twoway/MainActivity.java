package com.intercom.video.twoway;

import android.app.FragmentTransaction;
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
import android.widget.CheckBox;
import android.widget.ImageView;

import java.util.ArrayList;

public class MainActivity extends ActionBarActivity implements DeviceListFrag.onListItemSelectedListener {
    public SharedPreferenceAccessor sharedPreferenceAccessor;
    //used with callback from list fragment

    // app verions
    String appVersion="1.0.0";

    // Connect to network discovery
    NetworkDiscovery mNetworkDiscovery;

    // fragment variables here
    public static FragmentTransaction ft = null;
    DeviceListFrag deviceListFrag = null;
    SettingsFragment settingsFrag = null;
    //
    // frag variables ^^^

    ControlConstants constants = new ControlConstants();

    // used in logcat logging
    static String TAG = "Two-Way:";

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
    VideoStreaming streamingEngine1, streamingEngine2;
    Audio audioEngine;

    /*
    Keeps track of what current layout id is
     */
    int currentLayoutId;


    volatile static ListenerService listenerService;
    volatile static boolean serviceIsBoundToActivity = false;

    /*
    list of all discovered IP adresses
    used in fragment_main to populate list
     */
    ArrayList<String> mUrlList_asArrayList = new ArrayList<String>();
    public static String[] mUrlList_as_StringArray= new String[] { "Original initialized",
        "default.1.1.1", "10.1.1.2", "10.1.1.3","10.1.1.4","10.1.1.5","10.1.1.6","10.1.1.7",
        "10.1.1.8", "10.1.1.9", "10.1.1.10","10.1.1.11","10.1.1.12","10.1.1.13","10.1.1.14"  };

    //TODO remember to remove these default values after testing^^^

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferenceAccessor = new SharedPreferenceAccessor(this);

        audioEngine = new Audio();

        streamingEngine1 = new VideoStreaming(audioEngine);
        streamingEngine2 = new VideoStreaming(audioEngine);

        utilities = new Utilities(this);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        //getActionBar().setTitle(appVersion);

        setContentView(R.layout.fragment_main);
        startListenerService();
        setupNetworkDiscovery();

        // fragment code
        deviceListFrag = new DeviceListFrag();
        ft = getFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container, deviceListFrag, "MAIN_FRAGMENT");
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection listenerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
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
        public void onServiceDisconnected(ComponentName arg0) {
            utilities.showToastMessage("Disconnected from service");
            serviceIsBoundToActivity = false;
        }
    };

    public void startListenerService() {
        Intent service = new Intent(utilities.mainContext, ListenerService.class);
        startService(service);
    }

    /*
    This is called when we click the establish connection button
    Attempts to establish the tcp connection to another device
    This starts our streaming server and tells the other device to connect to us
     */
    void establishConnection(String ipAddress) {
//        String ipAddress = ipAddressEditText.getText().toString();
        Log.i(TAG, " <---===establish connection called ===--->");
        ImageView jpegTestImageView = (ImageView) findViewById(R.id.jpegTestImageView);
        streamingEngine1.listenForMJpegConnection(jpegTestImageView);

        // this unlocks and turns on the other device via service
        tcpEngine.connectToDevice(ipAddress, 1);
    }

    /*
    This is like establishConnection() except is run when when a connection intent is received
     */
    void establishConnectionOnIntent(String ipAddress) {
        ImageView jpegTestImageView = (ImageView) findViewById(R.id.jpegTestImageView);

        streamingEngine2.listenForMJpegConnection(jpegTestImageView);

        // this just unlocks and turns on the other device via service
        tcpEngine.connectToDevice(ipAddress, 2);
    }

    /*
    keeps track of current layout id as Int
     */
    @Override
    public void setContentView(int layoutResID) {
        this.currentLayoutId = layoutResID;
        super.setContentView(layoutResID);
    }

    static boolean mic = true;

    public void onCheckboxClicked(View view) {

        boolean checked = ((CheckBox) view).isChecked();


        switch (view.getId()) {
            case R.id.checkBox:
                mic = checked;
                break;
        }
    }



    /*
    triggers network discovery
    gets ArrayList with IPs and pushed to list in fragment main.
     */
    public void setupNetworkDiscovery() {
        //TODO: move this into NetworkDiscovery class
        //WifiManager mWifi= (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mNetworkDiscovery = new NetworkDiscovery();
        mNetworkDiscovery.start();
        mUrlList_asArrayList = mNetworkDiscovery.getIpList();

        ArrayList<String> mUrlList_asArrayList = new ArrayList<String>();
        // update initial list of discovered IPs
        // also need to happen every time the view is called
        mUrlList_as_StringArray = convertArrayListToStringArray(mUrlList_asArrayList);
        setIpList(mUrlList_as_StringArray);

    }


    // gets latest list of discovered IPs from network discovery and sets teh global variable
    public void setIpList(String[] newIpList) {
        mUrlList_as_StringArray = newIpList;
    }

    public void setIpList(ArrayList<String> mArrayList) {
        String[] mStringArrayIpList = convertArrayListToStringArray(mArrayList);
        setIpList(mStringArrayIpList);
    }

    //todo: can have NetworkDiscovery return string list instead
    //todo: can also move this to utilities
    public String[] convertArrayListToStringArray(ArrayList<String> mArrayList) {
        String[] mStringArray = new String[mArrayList.size()];

//        System.out.println("size of this shit = "+mArrayList.size());

        System.err.println("pre for loop");
        for (int i = 0; i < mArrayList.size(); i++) {
            System.err.println("what the fuck for loop");

            mStringArray[i] = mArrayList.get(i);
        }

        return mStringArray;
    }

    /*
    This is where we handle messages (as intents) from the service
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        String COMMAND_STRING = intent.getExtras().getString("COMMAND");

        utilities.showToastMessage("Intent Received - " + intent.getExtras().getString("COMMAND"));

        // simply start the activity and turn on the screen, nothing more
        if (COMMAND_STRING.equals(constants.INTENT_COMMAND_START_ACTIVITY)) {
            utilities.forceWakeUpUnlock();
        }

        // tells us to connect to the remote server and start feeding it our video
        // then start our own remote server and tel the other device to connect
        if (COMMAND_STRING.equals(constants.INTENT_COMMAND_START_STREAMING_FIRST)) {
            // TODO: set autoswitch to main layout =true
            setContentView(R.layout.activity_main);
            utilities.forceWakeUpUnlock();

            audioEngine.startAudioCapture();

            // connect to the remote device and start streaming
            streamingEngine1.connectToDevice(intent.getExtras().getString("EXTRA_DATA"));
            cameraJpegCapture = new CameraJpegCapture(streamingEngine1, audioEngine);
            cameraJpegCapture.startCam();

            // now start our server and tell the other to connect to us
            establishConnectionOnIntent(intent.getExtras().getString("EXTRA_DATA"));
        }

        // tells us to connect to the remote server, this happens second after we have already started our own server and told them to connect
        // the difference between this and INTENT_COMMAND_START_STREAMING_FIRST is that we dont start a new server and tell the other to connect because we already did that
        if (COMMAND_STRING.equals(constants.INTENT_COMMAND_START_STREAMING_SECOND)) {
            utilities.forceWakeUpUnlock();

            audioEngine.startAudioCapture();

            // connect to the remote device and start streaming
            streamingEngine2.connectToDevice(intent.getExtras().getString("EXTRA_DATA"));
            cameraJpegCapture = new CameraJpegCapture(streamingEngine2, audioEngine);
            cameraJpegCapture.startCam();
        }


    }

    @Override
    public void onResume() {
        super.onResume();

        Intent theService = new Intent(this, ListenerService.class);
        bindService(theService, listenerServiceConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        // if we wanted to not destroy the service on activity destroy we could comment this out
        listenerService.stopListeningForConnections();
        stopService(new Intent(this, ListenerService.class));
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        //back from settings is main screen
        if (this.currentLayoutId == R.layout.settings_menu) {
            setContentView(R.layout.activity_main);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
// Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
// Handle item selection

        switch (item.getItemId()) {
            case R.id.action_view_profile:
                showSettings();
                return true;

            case R.id.action_home:
                setContentView(R.layout.activity_main);
                return true;

            case R.id.action_find_peers:
                System.out.println("About to run network discovery getIpList");
                mUrlList_asArrayList = mNetworkDiscovery.getIpList();

//                ArrayList<String> mUrlList_asArrayList =  fnew ArrayList<String>();


                // update initial list of discovered IPs
                // also need to happen every time the view is called
                System.err.println("about to return array list");
                mUrlList_as_StringArray = convertArrayListToStringArray(mUrlList_asArrayList);
                setIpList(mUrlList_as_StringArray);
                showDeviceList();
                for (String ip : mUrlList_as_StringArray)
                    Log.i(TAG, "loading to ui IP: " + ip);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // method to call list fragment to screen from fragment_mail layout
    public void showDeviceList() {
        setContentView(R.layout.fragment_main);
        deviceListFrag = new DeviceListFrag();
        ft = getFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container, deviceListFrag, "MAIN_FRAGMENT");
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    public void showSettings()
    {
        setContentView(R.layout.fragment_main);
        settingsFrag = new SettingsFragment();
        ft = getFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container, settingsFrag, "MAIN_FRAGMENT");
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    // official android code
    // TODO: not sure if this is implemented right
    // Container Activity must implement this interface

    public void onListItemSelectedListener(String deviceIP) {
        setContentView(R.layout.activity_main);
        establishConnection(deviceIP);
        Log.i(TAG, " <---===establish connection called from listener ===--->");
    }

    // This method is executed when list item is clicked and ip selected
    public void onListItemSelected(String deviceIP) {
        setContentView(R.layout.activity_main);
//        mText = (EditText) findViewById(R.id.ipAddressEditText);
//        mText.setText(deviceIP);
        establishConnection(deviceIP);
        Log.i(TAG, " <---===establish connection called from selected===--->");
    }
}
