package com.intercom.video.twoway.Services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.intercom.video.twoway.Controllers.ProfileController;
import com.intercom.video.twoway.MainActivity;
import com.intercom.video.twoway.Network.NetworkConstants;
import com.intercom.video.twoway.Network.NetworkDiscovery;
import com.intercom.video.twoway.Network.Tcp;
import com.intercom.video.twoway.R;
import com.intercom.video.twoway.Utilities.ControlConstants;
import com.intercom.video.twoway.Utilities.Utilities;

/**
 * @Author Cole Risch, Sean Luther, Eric Van Gelder, Charles Toll, Alex Gusan, Robert V.
 * This class creates a Foreground service, the least likely type of service to be killed
 * by the android system.  Service starts on boot.  Network Discovery runs in this service so that the
 * device is discoverable at all times.
 */
public class ListenerService extends Service 
{
    private ProfileController profileController;
    private Tcp serviceTcpEngine = new Tcp();
    private boolean listeningForConnections = false;
    private final int SERVICE_ID = 12345; // we don't use this but the service wants to be assigned an id when it is created
    private final IBinder mBinder = new LocalBinder();// Binder given to clients
    ControlConstants constants = new ControlConstants();
    public NetworkDiscovery mNetworkDiscovery; // Network Discovery Object

    /**
	 * Class used for the client Binder. Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder 
	{
		public ListenerService getService()
		{
			// Return this instance of LocalService so clients can call public
			// methods
			return ListenerService.this;
		}
	}

    /**
     * Lifecycle method that is called when service is to be started
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
        startListeningForConnections();

        Utilities u = new Utilities(this); //TODO: this should be passed the Utilities object from MainActivity
        mNetworkDiscovery = new NetworkDiscovery(u);
        mNetworkDiscovery.setupNetworkDiscovery();

        // If we get killed, after returning from here, restart the service
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return mBinder;
    }


    /**
     * Lifecycle method that is called when the service is created. Note that the service icon
     * and style are set in here.
     */
    @Override
	public void onCreate()
	{
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this,
                12345, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Resources res = this.getResources();
        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.service_icon)
                .setLargeIcon(BitmapFactory.decodeResource(res
                        , R.drawable.service_icon))
                .setTicker("listener service")
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentTitle("listener service")
                .setContentText("listener service");
        Notification n = builder.build();


        startForeground(SERVICE_ID, n);


		super.onCreate();
	}

    @Override
    public void onDestroy()
    {
        mNetworkDiscovery.stopNetworkDiscovery();
        stopListeningForConnections();
    }

    /**
     * Clean up threads and tcp, called in onDestroy
     */
    public void stopListeningForConnections()
    {
        listeningForConnections=false;
        serviceTcpEngine.closeConnection();
    }

    /**
     * start listening for connections from other devices and decide what
     * to do based on what command they send us.
     * This is where we communicate back with the main activity via an intent
     * and start the main activity if it is dead and start a video connection etc
     */
    public void startListeningForConnections()
    {
        Thread listenForConnectionThread;
        if(!listeningForConnections)
        {
            listeningForConnections = true;
            listenForConnectionThread = new Thread()
            {
                public void run()
                {
                    while (listeningForConnections)
                    {
                        try
                        {
                            int connectionStage = serviceTcpEngine.listenForConnection();

                            // extract just the ip address from ip address and port combo string
                            // this would be cooler if done with regular expressions
                            String RemoteAddress = serviceTcpEngine.lastRemoteIpAddress;
                            String newRemoteAddress = RemoteAddress.substring(1, RemoteAddress.indexOf(":"));

                            // tells us to connect to the remote server and start feeding it our video
                            // then start our own remote server and tel the other device to connect
                            if (connectionStage == 1)
                                sendCommandToActivity(constants.INTENT_COMMAND_START_STREAMING_FIRST, newRemoteAddress);

                            // tells us to connect to the remote server, this happens second after we have already started our own server and told them to connect
                            // the difference between this and INTENT_COMMAND_START_STREAMING_FIRST is that we dont start a new server and tell the other to connect because we already did that
                            if (connectionStage == 2)
                                sendCommandToActivity(constants.INTENT_COMMAND_START_STREAMING_SECOND, newRemoteAddress);

                            if (connectionStage == NetworkConstants.PROFILE)
                            {
                                // profileController can be null if service started on boot
                                try
                                {
                                    profileController.sendDeviceInfoByIp(newRemoteAddress);
                                } catch (Exception e)
                                {

                                }
                            }
                        }
                        catch(Exception e)
                        {

                        }
                        // now just close the connection so we can listen for more
                        serviceTcpEngine.closeConnection();
                    }
                }
            };
            listenForConnectionThread.start();
        }
    }

    /**
     * send a command to the activity
     * This is the services primary means of communicating with the activity
     * this also starts the activity if it has been killed and brings it to the foreground
     * @param command the command string we are sending the activity
     * @param extra any extra data we need to send the activity, usually an ip address of a remote device
     */
    public void sendCommandToActivity(String command, String extra)
    {
        Intent startMainActivityIntent = new Intent(getBaseContext(), MainActivity.class);
        startMainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startMainActivityIntent.putExtra("COMMAND", command);
        startMainActivityIntent.putExtra("EXTRA_DATA", extra);

        getApplication().startActivity(startMainActivityIntent);
    }

    public void setProfileController(ProfileController pc)
    {
        if(pc == null) {
            Log.d("ListenerService", "ProfileController was NULL in setProfileController");
        }
        else {
            this.profileController = pc;
        }
    }
}