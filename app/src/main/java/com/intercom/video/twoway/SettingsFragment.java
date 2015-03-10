//package com.intercom.video.twoway;
//
//import android.app.Fragment;
//import android.content.SharedPreferences;
//import android.text.Editable;
//import android.text.TextWatcher;
//import android.util.Log;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.widget.CheckBox;
//import android.widget.CompoundButton;
//import android.widget.EditText;
//
///**
// * Created by charles on 3/9/15.
// */
//public class SettingsFragment extends Fragment {
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//// Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//// Handle item selection
//
//        switch (item.getItemId()) {
//            case R.id.action_view_profile:
//                setContentView(R.layout.settings_menu);
//                activateSettingsMenuListeners();
//                doRememberDeviceNic();
//                doRememberCameraViewFlag();
//                return true;
//
//            case R.id.action_home:
//                setContentView(R.layout.activity_main);
//                return true;
//
//            case R.id.action_find_peers:
//                System.out.println("About to run network discovery getIpList");
//                mUrlList_asArrayList = mNetworkDiscovery.getIpList();
//
////                ArrayList<String> mUrlList_asArrayList =  fnew ArrayList<String>();
//
//
//                // update initial list of discovered IPs
//                // also need to happen every time the view is called
//                System.err.println("about to return array list");
//                mUrlList_as_StringArray = convertArrayListToStringArray(mUrlList_asArrayList);
//                setIpList(mUrlList_as_StringArray);
//                showDeviceList();
//                for (String ip : mUrlList_as_StringArray)
//                    Log.i(TAG, "loading to ui IP: " + ip);
//                return true;
//
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }
//
//    public void activateSettingsMenuListeners() {
//            /*
//            auto-triggered when device nic is changed
//             */
//        EditText deviceNIC = (EditText) findViewById(R.id.settings_menu_editText_deviceNic);
//
//        // auto-save on text change for deviceNIC in settings menu
//        deviceNIC.addTextChangedListener(new TextWatcher() {
//            public void afterTextChanged(Editable s) {
//            }
//
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//            }
//
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                setDeviceNic(s.toString());
//            }
//        });
//
//
//        CheckBox useCameraView = (CheckBox) findViewById(R.id.settings_menu_checkBox_usecamaraview);
//        // auto-save on checkbox flag change
//        useCameraView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                setUseCameraViewFlag(isChecked);
//
//            }
//        });
//
//    }
//
////
////    public void setUseCameraViewFlag(boolean isChecked) {
////        String PREFS_NAME = "SETTINGS MENU";
////        SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 1);
////        SharedPreferences.Editor editor = settings.edit();
////        editor.putBoolean("use_camera_view", isChecked);
////        editor.apply();  // Apply the edits!
////    }
////
////    public boolean getUseCameraViewFlag() {
////        String PREFS_NAME = "SETTINGS MENU";
////        SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 1);
////        return settings.getBoolean("use_camera_view", false);
////    }
////
////    public void doRememberCameraViewFlag() {
////        boolean mCameraFlag = getUseCameraViewFlag();
////        CheckBox useCameraView = (CheckBox) findViewById(R.id.settings_menu_checkBox_usecamaraview);
////        useCameraView.setChecked(mCameraFlag);
////        return;
////    }
////
////
////    public void setDeviceNic(String newDeviceNic) {
////        //Log.i(TAG,"Device NIC stored --> "+ newDeviceNic);
////        String PREFS_NAME = "SETTINGS MENU";
////        SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
////        SharedPreferences.Editor editor = settings.edit();
////        editor.putString("device_nic", newDeviceNic);
////        editor.apply(); // Apply the edits!
////    }
////
////    public String getDeviceNic() {
////        // Log.i(TAG,"getDeviceNic Called ");
////        String PREFS_NAME = "SETTINGS MENU";
////        SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
////        // Log.i(TAG,"DeviceNic recovered: "+settings.getString("device_nic","0"));
////        return settings.getString("device_nic", "0");
////    }
////
////    public void doRememberDeviceNic() {
////        String mDeviceNic = getDeviceNic();
////        EditText mEditText = (EditText) findViewById(R.id.settings_menu_editText_deviceNic);
////        mEditText.setText(mDeviceNic);
////        return;
////    }
//}
