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

public class DeviceListFrag extends ListFragment {
    ProfileController profileController;
    String[] values;
    String[] deviceIPs;
    //Button connectButton;

    // Used to pass action back to MainActivity
    onListItemSelectedListener mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle b) {
        super.onActivityCreated(b);

        //TODO: call update IP list here
        values = MainActivity.mUrlList_as_StringArray;

        if(values != null)
        {
            values = updateIpListFromProfileController(values);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, values);
        setListAdapter(adapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

        Log.i(MainActivity.TAG, "Position " + position + " was clicked\n" + v);
        String deviceIP = ((TextView) v).getText().toString();
        Log.i("ListItemSelected: ", deviceIP);
        Toast.makeText(getActivity(), "Option " + position + " clicked", Toast.LENGTH_SHORT).show();
        selectDetail(deviceIPs[position]);
/*
        if(deviceIP.matches("^([0-9]{1,3}(\.)){3}([0-9]{1.3}"))
        {
            selectDetail(deviceIP);
        }
        else
        {
            deviceIP = profileController.getProfileByName(deviceIP);
            selectDetail(deviceIP);
        }*/
    }


    private void selectDetail(String deviceIP) {
        // set layout and populate acquired IP in editText
        // TODO:will set edit text to not editable from UI later
//        getActivity().setContentView(R.layout.activity_main);
//        EditText mText = (EditText) getActivity().findViewById(R.id.ipAddressEditText);
//        mText.setText(deviceIP);

        // TODO: this sends a call to MainActivity with IP info
        mListener.onListItemSelected(deviceIP);
    }

    //For now whoever implements this interface must handle retrieving the ip from the profile controller
    //if it is not in an ip form right now
    // official android code:
    // Container Activity ( MainActivity in this case )must implement this interface
    public interface onListItemSelectedListener {
        public void onListItemSelected(String deviceIP);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (onListItemSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + "CUSTOM ERROR: must implement onListItemSelectedListener");
        }
    }

    private void getDeviceProfiles(String[] ips)
    {
        for(String ip : ips)
        {
            profileController.receiveDeviceInfoByIp(ip);
        }
    }

    public void setProfileController(ProfileController pc)
    {
        this.profileController = pc;
    }

    public String[] updateIpListFromProfileController(String[] ips)
    {
        String[] profiles = new String[ips.length];
        deviceIPs = ips;

        if(ips.length > 0) {
            getDeviceProfiles(ips);
            int valuesPosition = 0;
            for(String ip : ips)
            {
                if(profileController.getProfileByIp(ip) != null)
                {
                    profiles[valuesPosition] = profileController.getProfileByIp(ip).getDeviceName();
                }
                else
                {
                    profiles[valuesPosition] = ip;
                }
                valuesPosition++;
            }
        }
        return profiles;
    }



}

