package com.intercom.video.twoway;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
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

import com.intercom.video.twoway.Models.ContactsEntity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends ActionBarActivity implements MyListFrag.onListItemSelectedListener {
    Uri mCurrentPhotoPath;
    final int UPDATE_PROFILE_PICTURE = 445;
    //used with callback from list fragment
    EditText mText;

    // app verions
    String appVersion = "1.0.0";

    // Connct to network discovery
    NetworkDiscovery mNetworkDiscovery;

    // fragment variables here
    public static FragmentTransaction ft = null;
    MyListFrag frag0 = null;
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
    These buttons and checkbox are present in settings_menu layout
    sm = Settings Menu
     */
    static CheckBox smCheckBoxUseCamaraView;
    static ImageView smDeviceAvatar;
    static TextView smDeviceNic, smLableDeviceNic;

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
    public static String[] mUrlList_as_StringArray = new String[]{"Original initialized",
            "default.1.1.1", "10.1.1.2", "10.1.1.3", "10.1.1.4", "10.1.1.5", "10.1.1.6", "10.1.1.7",
            "10.1.1.8", "10.1.1.9", "10.1.1.10", "10.1.1.11", "10.1.1.12", "10.1.1.13", "10.1.1.14"};

    //TODO remember to remove these default values after testing^^^


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


    public void settingsMenuBackButtonPressed(View view) {
        setContentView(R.layout.activity_main);
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
                if (checked)
                    mic = true;
                else
                    mic = false;
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                setContentView(R.layout.settings_menu);
                activateSettingsMenuListeners();
                loadProfilePictureFromPreferences();
                doRememberDeviceNic();
                doRememberCameraViewFlag();
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

    public void activateSettingsMenuListeners() {
            /*
            auto-triggered when device nic is changed
             */
        EditText deviceNIC = (EditText) findViewById(R.id.settings_menu_editText_deviceNic);

        // auto-save on text change for deviceNIC in settings menu
        deviceNIC.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setDeviceNic(s.toString());
            }
        });


        CheckBox useCameraView = (CheckBox) findViewById(R.id.settings_menu_checkBox_usecamaraview);
        // auto-save on checkbox flag change
        useCameraView.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setUseCameraViewFlag(isChecked);

            }
        });

        final ImageView profilePic = (ImageView) findViewById(R.id.imageView_device_avatar);
        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takeProfilePicture();
            }
        });

    }


    public void setUseCameraViewFlag(boolean isChecked) {
        String PREFS_NAME = "SETTINGS MENU";
        SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 1);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("use_camera_view", isChecked);
        editor.apply();  // Apply the edits!
    }

    public boolean getUseCameraViewFlag() {
        String PREFS_NAME = "SETTINGS MENU";
        SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 1);
        return settings.getBoolean("use_camera_view", false);
    }

    public void doRememberCameraViewFlag() {
        boolean mCameraFlag = getUseCameraViewFlag();
        CheckBox useCameraView = (CheckBox) findViewById(R.id.settings_menu_checkBox_usecamaraview);
        useCameraView.setChecked(mCameraFlag);
    }


    public void setDeviceNic(String newDeviceNic) {
        //Log.i(TAG,"Device NIC stored --> "+ newDeviceNic);
        String PREFS_NAME = "SETTINGS MENU";
        SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("device_nic", newDeviceNic);
        editor.apply(); // Apply the edits!
    }

    public String getDeviceNic() {
        // Log.i(TAG,"getDeviceNic Called ");
        String PREFS_NAME = "SETTINGS MENU";
        SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        // Log.i(TAG,"DeviceNic recovered: "+settings.getString("device_nic","0"));
        return settings.getString("device_nic", "0");
    }

    public void doRememberDeviceNic() {
        String mDeviceNic = getDeviceNic();
        EditText mEditText = (EditText) findViewById(R.id.settings_menu_editText_deviceNic);
        mEditText.setText(mDeviceNic);
        return;
    }

    // method to call list fragment to screen from fragment_mail layout
    public void showDeviceList() {
        setContentView(R.layout.fragment_main);
        frag0 = new MyListFrag();
        ft = getFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container, frag0, "MAIN_FRAGMENT");
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

    //These were just in the main method in my old repository... should have been a little bit smarter
    //With how i handled that, but oh well

    //Method called from settings fragment to take a profile picture for you device
    public void takeProfilePicture(){
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "ProfilePicture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
        mCurrentPhotoPath = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentPhotoPath);
        startActivityForResult(intent, UPDATE_PROFILE_PICTURE);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String imageFileName = "ProfilePicture";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
//        mCurrentPhotoPath = new Uri.Builder("file:" + image.getAbsolutePath());
        return image;
    }

    private String loadFromSharedPreferences(String prefsName, String settingsTitle)
    {
        // Log.i(TAG,"getDeviceNic Called ");
        String preferencesToReturn;
        SharedPreferences settings = getApplicationContext().getSharedPreferences(prefsName, 0);
        // Log.i(TAG,"DeviceNic recovered: "+settings.getString("device_nic","0"));
        preferencesToReturn = settings.getString(settingsTitle, "0");
        return preferencesToReturn;

    }

    //Called when going to settings layout, or when a device wants the contacts entity
    private void setProfilePicture() {
        // Returns the Uri for a photo stored on disk given the fileName
        ImageView profilePic = (ImageView) findViewById(R.id.imageView_device_avatar);
        try {
//            File f = new File(mCurrentPhotoPath);
//            Uri contentUri = Uri.fromFile(f);
            Bitmap bitmap = BitmapFactory.decodeFile(getRealPathFromURI(mCurrentPhotoPath));
            //Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentUri);
            profilePic.setImageBitmap(bitmap);
            saveProfilePictures(bitmap);
        }
        catch(Exception e)
        {
            Log.d("No Image", "Profile Image does not exist");
        }
    }

    private void loadProfilePictureFromPreferences()
    {
        String picture = loadFromSharedPreferences("SETTINGS MENU", "Profile Picture");

        if(picture.equals("0"))
        {
            Log.d("No Profile Picture", "There is no saved Profile picture");
        }
        else
        {
            // Returns the Uri for a photo stored on disk given the fileName
            ImageView profilePic = (ImageView) findViewById(R.id.imageView_device_avatar);
            try {
                Bitmap bitmap = ContactsEntity.decodePictureFromBase64(picture);
                profilePic.setImageBitmap(bitmap);
            }
            catch(Exception e)
            {
                Log.d("No Image", "Profile Image does not exist");
            }
        }
    }

    //Returns the actual path that can be used to grab the picture.
    public String getRealPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = this.getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst())
        {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    private void saveProfilePictures(Bitmap picture)
    {
        writeToSharedPrefs("Profile Picture", ContactsEntity.encodePictureToBase64(picture), "SETTINGS MENU");
    }

    public void writeToSharedPrefs(String title, String toWrite, String preferenceName)
    {
        SharedPreferences settings = getApplicationContext().getSharedPreferences(preferenceName, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(title, toWrite);
        editor.apply();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent)
    {
        if(resultCode == Activity.RESULT_OK)
        {
            if(requestCode == UPDATE_PROFILE_PICTURE)
            {
                setProfilePicture();
            }
        }
    }
}
