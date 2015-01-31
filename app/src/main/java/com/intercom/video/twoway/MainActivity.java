package com.intercom.video.twoway;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity
{
    /*
    Handles all networking stuff
     */
    Tcp tcpEngine = new Tcp();

    /*
    Some helpful things (screen unlock, etc) that shouldnt go in main activity
     */
    UsefulStuff usefulStuff = new UsefulStuff();


    /*
    Used to attempt to connect to another device
     */
    Button connectButton;

    /*
    Opens and closes video link between devices
     */
    Button videoLinkButton;

    /*
    Used to enter ip address of other device for connecting
     */
    EditText ipAddressEditText;

    /*
    Spinning progress circle, is visible when waiting to establish connections
     */
    ProgressBar connectionProgressBar;

    static Context context;

    public void startDemonstrationUnlockTimer(View v)
    {
        ShowToastMessage("Unlocking Screen in 5 Seconds....");

        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                ((Activity)context).runOnUiThread(new Runnable()
                {
                    public void run()
                    {
                        usefulStuff.forceWakeUpUnlock();
                    }
                });
            }

        }, 5000);
    }

    void setupButtons()
    {
        connectButton=(Button)findViewById(R.id.connectButton);
        videoLinkButton=(Button)findViewById(R.id.video_link_button);
        ipAddressEditText=(EditText)findViewById(R.id.ipAddressEditText);
        connectionProgressBar=(ProgressBar)findViewById(R.id.connectionProgressBar);

        connectButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                establishConnection();
            }
        });

        videoLinkButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                ShowToastMessage("Feature Not Yet Implemented");
            }
        });
    }

    void hideAllButtons()
    {
        connectButton.setVisibility(View.INVISIBLE);
        videoLinkButton.setVisibility(View.INVISIBLE);
        ipAddressEditText.setVisibility(View.INVISIBLE);
        connectionProgressBar.setVisibility(View.INVISIBLE);
    }

    /*
    Called when menu item connect is pressed
     */
    void unHideConnectButtons()
    {
        hideAllButtons();
        ipAddressEditText.setVisibility(View.VISIBLE);
        connectButton.setVisibility(View.VISIBLE);
    }

     /*
    Called when menu item listen is pressed
     */
    void listenForConnection()
    {
        hideAllButtons();
        connectionProgressBar.setVisibility(View.VISIBLE);
        tcpEngine.listenForConnection();
    }


    /*
    Attempts to establish the tcp connection to another device
     */
    void establishConnection()
    {
        String ipAddress=ipAddressEditText.getText().toString();

        tcpEngine.connectToDevice(ipAddress);
    }

    /*
    Lets us show a toast message from any thread
     */
    static void ShowToastMessage(final String message)
    {
        ((Activity)context).runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    };


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context=this;
        setupButtons();
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
            case R.id.action_listen:
               listenForConnection();
                return true;
            case R.id.action_connect:
                unHideConnectButtons();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}