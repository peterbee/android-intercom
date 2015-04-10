package com.intercom.video.twoway;

//Android API imports

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

import com.intercom.video.twoway.Controllers.ProfileController;
import com.intercom.video.twoway.Fragments.DeviceListFrag;
import com.intercom.video.twoway.Fragments.SettingsFragment;
import com.intercom.video.twoway.Interfaces.UpdateDeviceListInterface;
import com.intercom.video.twoway.Models.ContactsEntity;
import com.intercom.video.twoway.Network.NetworkDiscovery;
import com.intercom.video.twoway.Network.Tcp;
import com.intercom.video.twoway.Services.ListenerService;
import com.intercom.video.twoway.Streaming.Audio;
import com.intercom.video.twoway.Streaming.CameraJpegCapture;
import com.intercom.video.twoway.Streaming.VideoStreaming;
import com.intercom.video.twoway.Utilities.ControlConstants;
import com.intercom.video.twoway.Utilities.SharedPreferenceAccessor;
import com.intercom.video.twoway.Utilities.Utilities;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

//Local code imports
//Java Imports

/**
 * @version 1.0.1
 *          Main activity of the Application.
 * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
 * @Implements DeviceListFrag.onListItemSelectedListener, SettingsFragment.ProfileControllerTransferInterface,UpdateDeviceListInterface
 */
public class MainActivity extends ActionBarActivity implements
        DeviceListFrag.onListItemSelectedListener, SettingsFragment.ProfileControllerTransferInterface,
        UpdateDeviceListInterface {
    //--------------BEGIN VARIABLE DECLARATION---------------------------------
    public ProfileController profileController; // Object for sending and receiving profiles
    public SharedPreferenceAccessor sharedPreferenceAccessor;
    //used with callback from list fragment
    String appVersion = "1.0.1";// app versions
    NetworkDiscovery mNetworkDiscovery; // Connect to network discovery
    // fragment variables here
    public static FragmentTransaction ft = null;
    DeviceListFrag deviceListFrag = null;
    SettingsFragment settingsFrag = null;
    // used in logcat logging
    public static String TAG = "Two-Way:";
    Tcp tcpEngine = new Tcp(); //Object for Tcp class
    CameraJpegCapture cameraJpegCapture; //Object for CameraJpegCapture class
    public Utilities utilities; // Utilities object
    /*
    streamingEngine1 is used for the first device (that initiated the connection) to act as a server and streamingEngine2 to act as a client
    streamingEngine1 is used for the second device (that did not initiate the connection) to act as a client and streamingEngine2 to act as a server
     */
    VideoStreaming streamingEngine1, streamingEngine2;
    Audio audioEngine; //object for captuing and playing audio
    public static boolean mic = false; // flag for whether or not to capture audio

    int currentLayoutId; //Keeps track of what current layout id is
    volatile static ListenerService listenerService; //Object for referencing the Listener service
    volatile static boolean serviceIsBoundToActivity = false; //Control flag for determining whther or not the service dies with the app
    ArrayList<String> mUrlList_asArrayList = new ArrayList<String>(); //Array for storing IP addresses
    public static String[] mUrlList_as_StringArray;


    //-------------------BEGIN MAJOR LIFECYCLE METHODS--------------------------------

    /**
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * Initial lifecycle method call.  Should be creating all permanent objects here that do not
     * need to be killed or re-instantiated.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferenceAccessor = new SharedPreferenceAccessor(this); //creates an accessor for our storage
        utilities = new Utilities(this); // instantiates our utilities object.
        mUrlList_as_StringArray = new String[0];

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);// ensures that the app does not let the screen blank out while active

        setContentView(R.layout.activity_main);
        setContentView(R.layout.fragment_main);
        startListenerService(); //starts the listener service
        setupNetworkDiscovery();//starts network discovery
        profileController = new ProfileController(this, mNetworkDiscovery.getMyIp(), mNetworkDiscovery); //starts our profile controller

        // fragment code to allow for settigns fragment and profile fragment
        deviceListFrag = new DeviceListFrag();
        deviceListFrag.setProfileController(this.profileController);
        ft = getFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container, deviceListFrag, "MAIN_FRAGMENT");
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    /**
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * Next lifecycle method, this is where we instantiate all items that need to be recreated
     * when the app comes back into focus
     */
    @Override
    public void onResume() {
        super.onResume();

        utilities = new Utilities(this);
        audioEngine = new Audio();


//        setupNetworkDiscovery();

        streamingEngine1 = new VideoStreaming(audioEngine, utilities);
        streamingEngine2 = new VideoStreaming(audioEngine, utilities);

        Intent theService = new Intent(this, ListenerService.class);
        bindService(theService, listenerServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * This is where we kill all items that need to be removed when the application loses focus
     */
    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * This is where we gracefully terminate all aspects of the application when the application
     * closes
     */
    @Override
    public void onStop() {
        streamingEngine1.closeConnection();
        streamingEngine2.closeConnection();
        tcpEngine.closeConnection();
        mNetworkDiscovery.stopNetworkDiscovery();

        super.onStop();
    }

    /**
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * This is where we terminate all items that could potentially remain after the application
     * is closed, such as the listener service
     */
    @Override
    public void onDestroy() {
        // if we wanted to not destroy the service on activity destroy we could comment this out
        listenerService.stopListeningForConnections();
        stopService(new Intent(this, ListenerService.class));
        super.onDestroy();
    }

    //----------------BEGIN MINOR LIFECYCLE METHODS------------------------------

    /**
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * method to call list fragment to screen from fragment_mail layout
     */
    public void showDeviceList() {
        setContentView(R.layout.fragment_main);
        deviceListFrag = new DeviceListFrag();
        deviceListFrag.setProfileController(this.profileController);
        getProfilesFromDiscoveredIPs(mUrlList_as_StringArray);
        ft = getFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container, deviceListFrag, "MAIN_FRAGMENT");
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    /**
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * Shows the settings fragment
     */
    public void showSettings() {
        settingsFrag = new SettingsFragment();
        ft = getFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container, settingsFrag, "MAIN_FRAGMENT");
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    /**
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * This is where we handle messages (as intents) from the service
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // return if no extras so we dont crash trying to retrieve them
        if(intent.getExtras()==null)
            return;

        String COMMAND_STRING = intent.getExtras().getString("COMMAND");

        // if no string return so no crash
        if(COMMAND_STRING==null)
            return;

        utilities.showToastMessage("Intent Received - " + intent.getExtras().getString("COMMAND"));

        // simply start the activity and turn on the screen, nothing more
        if (COMMAND_STRING.equals(ControlConstants.INTENT_COMMAND_START_ACTIVITY)) {
            utilities.forceWakeUpUnlock();
        }

        // tells us to connect to the remote server and start feeding it our video
        // then start our own remote server and tel the other device to connect
        if (COMMAND_STRING.equals(ControlConstants.INTENT_COMMAND_START_STREAMING_FIRST)) {
            // TODO: set autoswitch to main layout =true
            setContentView(R.layout.activity_main);
            utilities.forceWakeUpUnlock();

            audioEngine.startAudioCapture();

            // connect to the remote device and start streaming
            streamingEngine1.connectToDevice(intent.getExtras().getString("EXTRA_DATA"));
            cameraJpegCapture = new CameraJpegCapture(streamingEngine1, audioEngine, utilities);
            cameraJpegCapture.startCam();

            // now start our server and tell the other to connect to us
            establishConnectionOnIntent(intent.getExtras().getString("EXTRA_DATA"));
        }

        // tells us to connect to the remote server, this happens second after we have already started our own server and told them to connect
        // the difference between this and INTENT_COMMAND_START_STREAMING_FIRST is that we dont start a new server and tell the other to connect because we already did that
        if (COMMAND_STRING.equals(ControlConstants.INTENT_COMMAND_START_STREAMING_SECOND)) {
            utilities.forceWakeUpUnlock();

            audioEngine.startAudioCapture();

            // connect to the remote device and start streaming
            streamingEngine2.connectToDevice(intent.getExtras().getString("EXTRA_DATA"));
            cameraJpegCapture = new CameraJpegCapture(streamingEngine2, audioEngine, utilities);
            cameraJpegCapture.startCam();
        }


    }

    /**
     * @param menu
     * @return
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * Inflate the menu; this adds items to the action bar if it is present.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * @param layoutResID
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * keeps track of current layout id as Int
     */
    @Override
    public void setContentView(int layoutResID) {
        this.currentLayoutId = layoutResID;
        super.setContentView(layoutResID);
    }

    //--------------BEGIN ACTION HANDLER CALLBACK METHODS--------------------------

    /**
     * @param deviceIP
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * official android code, Container Activity must implement this interface
     */
    // TODO: not sure if this is implemented right
    public void onListItemSelectedListener(String deviceIP) {
        setContentView(R.layout.activity_main);
        establishConnection(deviceIP);
        Log.i(TAG, " <---===establish connection called from listener ===--->");
    }

    /**
     * @param deviceIP
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * This method is executed when list item is clicked and ip selected
     */
    public void onListItemSelected(String deviceIP) {
        setContentView(R.layout.activity_main);
//        mText = (EditText) findViewById(R.id.ipAddressEditText);
//        mText.setText(deviceIP);
        establishConnection(deviceIP);
        Log.i(TAG, " <---===establish connection called from selected===--->");
    }

    @Override
    public void onBackPressed()
    {
        Log.d("back pressed", "Closing connection on back pressed");
        streamingEngine1.closeConnection();
        streamingEngine2.closeConnection();
        tcpEngine.closeConnection();
        mNetworkDiscovery.stopNetworkDiscovery();

        //back from settings is main screen
        if (this.currentLayoutId == R.layout.settings_menu) {
            setContentView(R.layout.activity_main);
        }
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
                mUrlList_asArrayList = mNetworkDiscovery.getIpList();

                // update initial list of discovered IPs
                // also need to happen every time the view is called
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

    /**
     * @param view
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * Action handler callback for monitoring when the Mic checkbox is changed
     */
    public void onCheckboxClicked(View view) {

        boolean checked = ((CheckBox) view).isChecked();


        switch (view.getId()) {
            case R.id.checkBox:
                mic = checked;
                break;
        }
    }
    //------------------BEGIN SERVICE METHODS-----------------------------------
    /**
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection listenerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            utilities.showToastMessage("Connected to service");
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            ListenerService.LocalBinder binder = (ListenerService.LocalBinder) service;
            listenerService = binder.getService();
            listenerService.setProfileController(profileController);

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

    /**
     * @param ipAddress
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * This is called when we click the establish connection button attempts to establish the
     * tcp connection to another device this starts our streaming server and tells the other
     * device to connect to us
     */
    void establishConnection(String ipAddress) {
//        String ipAddress = ipAddressEditText.getText().toString();
        Log.i(TAG, " <---===establish connection called ===--->");
        ImageView jpegTestImageView = (ImageView) findViewById(R.id.jpegTestImageView);
        streamingEngine1.listenForMJpegConnection(jpegTestImageView);

        // this unlocks and turns on the other device via service
        tcpEngine.connectToDevice(ipAddress, 1);
    }

    /**
     * @param ipAddress
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * This is like establishConnection() except is run when when a connection intent is received
     */
    void establishConnectionOnIntent(String ipAddress) {
        ImageView jpegTestImageView = (ImageView) findViewById(R.id.jpegTestImageView);

        streamingEngine2.listenForMJpegConnection(jpegTestImageView);

        // this just unlocks and turns on the other device via service
        tcpEngine.connectToDevice(ipAddress, 2);
    }

    //------------ BEGIN PROFILE SERVICE METHODS--------------------------------
    @Override
    public ProfileController retrieveProfileController() {
        return profileController;
    }

    @Override
    public void updateDeviceListFromHashMap(ConcurrentHashMap<String, ContactsEntity> deviceList)
    {
        if(deviceListFrag != null) {
            //deviceListFrag.updateIpListFromProfileHashMap(this.profileController.getDeviceList());
        }
    }

    private void getProfilesFromDiscoveredIPs(String[] ips)
    {
        for(String ip : ips)
        {
            this.profileController.receiveDeviceInfoByIp(ip);
        }
    }

    //----------------BEGIN RANDOM HELPER METHODS----------------------

    /**
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * This method activates our Network discovery engine.
     */
    public void setupNetworkDiscovery() {
        //TODO: move this into NetworkDiscovery class
        //WifiManager mWifi= (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mNetworkDiscovery = new NetworkDiscovery(utilities);
        mNetworkDiscovery.start();
        mUrlList_asArrayList = mNetworkDiscovery.getIpList();

        ArrayList<String> mUrlList_asArrayList = new ArrayList<String>();
        // update initial list of discovered IPs
        // also need to happen every time the view is called
        mUrlList_as_StringArray = convertArrayListToStringArray(mUrlList_asArrayList);
        setIpList(mUrlList_as_StringArray);

    }

    /**
     * @param newIpList
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * gets latest list of discovered IPs from network discovery and sets teh global variable
     */
    public void setIpList(String[] newIpList) {
        mUrlList_as_StringArray = newIpList;
    }

    /**
     * @param mArrayList
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * method for populating mStringArrayIpList
     */
    public void setIpList(ArrayList<String> mArrayList) {
        String[] mStringArrayIpList = convertArrayListToStringArray(mArrayList);
        setIpList(mStringArrayIpList);
    }

    /**
     * @param mArrayList
     * @return String[] composed of all elements in mArrayList
     * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
     * method for converting an Arraylist into a String[].  Used for converting the Ip addresses
     * received from NetworkDiscovery to a easier to use format at this time.
     */
    //todo: can have NetworkDiscovery return string list instead
    //todo: can also move this to utilities
    public String[] convertArrayListToStringArray(ArrayList<String> mArrayList) {
        String[] mStringArray = new String[mArrayList.size()];

//        System.out.println("size of this shit = "+mArrayList.size());

        System.err.println("pre for loop");
        for (int i = 0; i < mArrayList.size(); i++) {
            Log.i("MainActivity_line511", "what the fuck for loop");
            mStringArray[i] = mArrayList.get(i);
        }

        return mStringArray;
    }


}
