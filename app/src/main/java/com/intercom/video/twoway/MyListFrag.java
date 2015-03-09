package com.intercom.video.twoway;

import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MyListFrag extends ListFragment {
    String[] values;
    Button connectButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        return super.onCreateView( inflater,  container, savedInstanceState);
    }


    @Override
    public void onActivityCreated(Bundle b) {
        super.onActivityCreated(b);

        //TODO: call update IP list here
        values = MainActivity.mUrlList_as_StringArray;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, MainActivity.mUrlList_as_StringArray);
        setListAdapter(adapter);
    } //onActivityCreated close bracket


    //////////////////////////

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Log.i(MainActivity.TAG, "Position " + position + " was clicked\n" + v);
        String deviceIP = ((TextView) v).getText().toString();
        Log.i("ListItemSelected: ", deviceIP);
        Toast.makeText(getActivity(), "Option " + position + " clicked", Toast.LENGTH_SHORT).show();
        selectDetail(deviceIP);
    }


    private void selectDetail(String deviceIP) {
        //TODO:I'm sure there is a better way to do this
        // I'm simulating a button click after setting ip Address in an Edit Text

        getActivity().setContentView(R.layout.activity_main);

        EditText mText = (EditText) getActivity().findViewById(R.id.ipAddressEditText);
        mText.setText(deviceIP);

       // not sure how to make this work
       // MainActivity.establishConnection();

        connectButton = (Button) getActivity().findViewById(R.id.connectButton);
        connectButton.performClick();
        connectButton.setPressed(true);
        connectButton.invalidate();
        connectButton.setPressed(false);
        connectButton.invalidate();

    }

}