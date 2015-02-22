package com.intercom.video.twoway;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;

public class ListenerService extends Service {
    // we don't use this but the service wants to be assigned an id when it is created
    private final int SERVICE_ID = 12345;
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    ControlConstants constants = new ControlConstants();
    private Tcp serviceTcpEngine = new Tcp();
    private boolean listeningForConnections = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // If we get killed, after returning from here, restart the service
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
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

    public void stopListeningForConnections() {
        listeningForConnections = false;
    }

    /*
    start listening for connections from other devices
     */
    public void startListeningForConnections() {
        NetworkDiscovery networkDiscovery = new NetworkDiscovery((WifiManager) getSystemService(Context.WIFI_SERVICE));
        networkDiscovery.start();
        Thread listenForConnectionThread;
        if (!listeningForConnections) {
            listeningForConnections = true;
            listenForConnectionThread = new Thread() {
                public void run() {
                    while (listeningForConnections) {
                        int connectionStage = serviceTcpEngine.listenForConnection();

                        // extract just the ip address from ip address and prot combo string
                        // this would be cooler if done with regular expressions
                        String RemoteAddress = serviceTcpEngine.lastRemoteIpAddress;
                        String newRemoteAddress = RemoteAddress.substring(1, RemoteAddress.indexOf(":"));

                        // tell main activity to start streaming the remote video
                        if (connectionStage == 1)
                            sendCommandToActivity(constants.INTENT_COMMAND_START_STREAMING_FIRST, newRemoteAddress);
                        if (connectionStage == 2)
                            sendCommandToActivity(constants.INTENT_COMMAND_START_STREAMING_SECOND, newRemoteAddress);

                        // now just close the connection since this is only proof of concept
                        serviceTcpEngine.closeConnection();
                    }
                }
            };
            listenForConnectionThread.start();
        }
    }

    /*
    send a command to the activity
    This will probably be our primary means of communicating with the activity
    this also wakes the activity and turns on the screen
     */
    public void sendCommandToActivity(String command, String extra) {
        Intent startMainActivityIntent = new Intent(getBaseContext(), MainActivity.class);
        startMainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startMainActivityIntent.putExtra("COMMAND", command);
        startMainActivityIntent.putExtra("EXTRA_DATA", extra);

        getApplication().startActivity(startMainActivityIntent);
    }

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        ListenerService getService() {
            // Return this instance of LocalService so clients can call public
            // methods
            return ListenerService.this;
        }
    }
}