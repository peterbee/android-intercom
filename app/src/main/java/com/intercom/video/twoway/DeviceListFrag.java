package com.intercom.video.twoway;

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

public class DeviceListFrag extends ListFragment {
    String[] values;
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
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, MainActivity.mUrlList_as_StringArray);
        setListAdapter(adapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Log.i(MainActivity.TAG, "Position " + position + " was clicked\n" + v);
        String deviceIP = ((TextView) v).getText().toString();
        Log.i("ListItemSelected: ", deviceIP);
        Toast.makeText(getActivity(), "Option " + position + " clicked", Toast.LENGTH_SHORT).show();
        selectDetail(deviceIP);
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


}

