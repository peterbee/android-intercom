package com.intercom.video.twoway.Fragments;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.intercom.video.twoway.Controllers.ProfileController;
import com.intercom.video.twoway.MainActivity;
import com.intercom.video.twoway.Models.ContactsEntity;

import java.util.concurrent.ConcurrentHashMap;

public class DeviceListFrag extends ListFragment {
    ProfileController profileController;
    String[] values = MainActivity.mUrlList_as_StringArray;
    String[] deviceIPs;
    //Button connectButton;
    private ArrayAdapter<String> adapter;

    // Used to pass action back to MainActivity
    onListItemSelectedListener mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    /**
     * Lifecycle activity which is called when the activity that contains the fragment is created.
     * currently this activity populates the list adapter with all values discovered from
     * Network Discovery.
     *
     * @param b
     */
    @Override
    public void onActivityCreated(Bundle b) {
        super.onActivityCreated(b);


        try
        {
            //TODO: call update IP list here
            values = MainActivity.mUrlList_as_StringArray;

            if (values != null)
            {

                updateIpListFromProfileHashMap(this.profileController.getDeviceList());
            }

            adapter = new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_list_item_1, values);
            setListAdapter(adapter);
        }
        catch(Exception e)
        {
//            e.printStackTrace();
        }
    }

    /**
     * Action handler for handling item selections
     * @param l the ListView object
     * @param v the View
     * @param position The index of the item selected
     * @param id the ID of the selected item
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

        Log.i(MainActivity.TAG, "Position " + position + " was clicked\n" + v);
        String deviceIP = ((TextView) v).getText().toString();
        Log.i("ListItemSelected: ", deviceIP);
        Toast.makeText(getActivity(), "Option " + position + " clicked", Toast.LENGTH_SHORT).show();
        selectDetail(deviceIPs[position]);
    }

    /**
     * This is a call back method for use in Main Activity
     * @param deviceIP
     */
    private void selectDetail(String deviceIP) {
        // set layout and populate acquired IP in editText
        // TODO:will set edit text to not editable from UI later
//        getActivity().setContentView(R.layout.activity_main);
//        EditText mText = (EditText) getActivity().findViewById(R.id.ipAddressEditText);
//        mText.setText(deviceIP);

        // TODO: this sends a call to MainActivity with IP info
        mListener.onListItemSelected(deviceIP);
    }

    /**
     * Official android code: Container Activity ( MainActivity in this case )must implement this
     * interface
     */
    public interface onListItemSelectedListener {
        public void onListItemSelected(String deviceIP);
    }

    /**
     * LifeCycle activity that is called when the fragment is attached to the main activity
     * @param activity
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (onListItemSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + "CUSTOM ERROR: must implement onListItemSelectedListener");
        }
    }

    /**
     * Helper method for populating discovered device profiles.
     * @param ips
     */
    private void getDeviceProfiles(String[] ips) {
        for (String ip : ips) {
            try {
                profileController.receiveDeviceInfoByIp(ip);
            } catch (Exception e) {
                Log.e("Profile Sharing", "String ip was null within ips");
            }
        }
    }

    /**
     * Helper method for setting the Profile Controller
     * @param pc
     */
    public void setProfileController(ProfileController pc) {
        this.profileController = pc;
    }

    /**
     * Updates the IP list from the profile hashmap so that  profiles are displayed instead of IPs.
     * @param devices
     */
    public void updateIpListFromProfileHashMap(
            ConcurrentHashMap<String, ContactsEntity> devices) {
        if (values == null || devices == null) {
            return;
        }
        String[] profiles = new String[values.length];
        deviceIPs = values.clone();

        if (values.length > 0) {
            for (int i = 0; i < profiles.length; i++) {
                if (devices.containsKey(deviceIPs[i])) {
                    profiles[i] = devices.get(deviceIPs[i]).getDeviceName();
                } else {
                    profiles[i] = deviceIPs[i];
                }
            }
        }
        values = profiles;
    }

    /**
     * Updates the ipList variable
     * @param ipList
     */
    public void updateIpList(String[] ipList) {
        this.values = ipList;
    }
}

