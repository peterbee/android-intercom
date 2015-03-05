package com.intercom.video.twoway;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.view.LayoutInflater;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends ActionBarActivity
{
    // fragment variables here
    public static FragmentTransaction ft=null;
    MyListFrag frag0=null;
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

    /*
    list of all discovered IP adresses
    used in fragment_main to populate list
     */
    ArrayList<String> mUrlList_asArrayList = new ArrayList<String>();
    public static String[] mUrlList_as_StringArray= new String[] { "default.1.1.0",
        "default.1.1.1", "10.1.1.2", "10.1.1.3","10.1.1.4","10.1.1.5","10.1.1.6","10.1.1.7",
        "10.1.1.8", "10.1.1.9", "10.1.1.10","10.1.1.11","10.1.1.12","10.1.1.13","10.1.1.14"  };

    //TODO remember to remove these default values after testing^^^


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

        // TODO: this will not be needed when UI is changed and NSD + list + video are integrated
        setContentView(R.layout.activity_main);
        setupButtons();

        setContentView(R.layout.fragment_main);
        startListenerService();

        // TODO: NetworkDiscovery integration
        setupNetworkDiscovery();

        // TODO: fragment code
        frag0 = new MyListFrag();
        ft = getFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container, frag0, "MAIN_FRAGMENT");
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    /*
    triggers network discovery
    gets ArrayList with IPs and pushed to list in fragment main.
     */
    public void setupNetworkDiscovery()
    {
        //TODO: move this into NetworkDiscovery class
    //WifiManager mWifi= (WifiManager) getSystemService(Context.WIFI_SERVICE);
    //NetworkDiscovery mNetworkDiscovery=new NetworkDiscovery(mWifi);
    //mNetworkDiscovery.startNetworkDiscovery();
    //mUrlList_asArrayList = mNetworkDiscovery.getUrlList();

    //    ArrayList<String> mUrlList_asArrayList = new ArrayList<String>();
    // update initial list of discovered IPs
    // also need to happen every time the view is called
    //    mUrlList_as_StringArray=convertArrayListToStringArray(mUrlList_asArrayList);

   }


    // gets latest list of discovered IPs from network discovery and sets teh global variable
    public void setIpList(String[] newIpList)
    {
    mUrlList_as_StringArray=newIpList;
    }

    public void setIpList (ArrayList mArrayList)
    {
    String[] mStringArrayIpList= convertArrayListToStringArray(mArrayList);
    setIpList(mStringArrayIpList);
    }

    //todo: can have NetworkDiscovery return string list instead
    //todo: can also move this to utilities
    public String[] convertArrayListToStringArray (ArrayList mArrayList)
    {
        String[] mStringArray= new String[]{};
        for (int i=0; i<mArrayList.size();i++)
        {
            mStringArray[i]=(String)mArrayList.get(i);
        }

        return mStringArray;
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
                activateSettingsMenuListeners();
                doRememberDeviceNic();
                doRememberCameraViewFlag();
                return true;

            case R.id.action_home:
                setContentView(R.layout.activity_main);
                return true;

            case R.id.action_find_peers:
                showDeviceList();
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
                   setDeviceNic(s.toString());
                }
            });


        CheckBox useCameraView = (CheckBox)findViewById(R.id.settings_menu_checkBox_usecamaraview);
        // auto-save on checkbox flag change
            useCameraView.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    setUseCameraViewFlag(isChecked);

                }
            });

            }


    public void setUseCameraViewFlag(boolean isChecked)
        {
        String PREFS_NAME="SETTINGS MENU";
        SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 1);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("use_camera_view",isChecked);
        editor.apply();  // Apply the edits!
        }

    public boolean getUseCameraViewFlag()
        {
        String PREFS_NAME="SETTINGS MENU";
        SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 1);
            return settings.getBoolean("use_camera_view",false);
        }

    public void doRememberCameraViewFlag()
        {
            boolean mCameraFlag=getUseCameraViewFlag();
            CheckBox useCameraView = (CheckBox)findViewById(R.id.settings_menu_checkBox_usecamaraview);
            useCameraView.setChecked(mCameraFlag);
            return;
        }


    public void setDeviceNic (String newDeviceNic)
        {
            //Log.i(TAG,"Device NIC stored --> "+ newDeviceNic);
            String PREFS_NAME="SETTINGS MENU";
            SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("device_nic", newDeviceNic);
            editor.apply(); // Apply the edits!
        }

    public String getDeviceNic ()
        {
            // Log.i(TAG,"getDeviceNic Called ");
            String PREFS_NAME="SETTINGS MENU";
            SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
            // Log.i(TAG,"DeviceNic recovered: "+settings.getString("device_nic","0"));
            return settings.getString("device_nic","0");
        }

    public void doRememberDeviceNic()
        {
            String mDeviceNic=getDeviceNic ();
            EditText mEditText=(EditText)findViewById(R.id.settings_menu_editText_deviceNic);
            mEditText.setText(mDeviceNic);
            return;
        }

// method to call list fragment to screen from fragment_mail layout
    public void showDeviceList()
        {
        setContentView(R.layout.fragment_main);
        frag0 = new MyListFrag();
        ft = getFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container, frag0, "MAIN_FRAGMENT");
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
        }



// <--- List fragment code for device list menu ---> //

    public static class MyListFrag extends ListFragment {
        String[] values;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
            return super.onCreateView( inflater,  container, savedInstanceState);
        }


        @Override
        public void onActivityCreated(Bundle b) {
            super.onActivityCreated(b);

            //TODO: call update IP list here
            values = mUrlList_as_StringArray;
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_list_item_1, mUrlList_as_StringArray);
            setListAdapter(adapter);
        } //onActivityCreated close bracket





        //////////////////////////

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            Log.i(TAG, "Position " +position + " was clicked\n" + v);
            String deviceIP = ((TextView) v).getText().toString();
            Log.i("ListItemSelected: ", deviceIP);
            Toast.makeText(getActivity(), "Option " + position + " clicked", Toast.LENGTH_SHORT).show();

            selectDetail(deviceIP);
        }


        private void selectDetail(String deviceIP) {
            //TODO: go to screen 3 and start 2-way stream from selected device
        }



        public static class MyImageFragment extends Fragment {

            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                     Bundle savedInstanceState) {
                View myFragmentView = inflater.inflate(R.layout.fragment_main, container, false);

                return myFragmentView;
            }

        }


    }

////////	//////////// list fragment code ends here

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container,
                    false);
            return rootView;
        }
    }


}
