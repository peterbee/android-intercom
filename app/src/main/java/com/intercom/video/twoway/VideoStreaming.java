package com.intercom.video.twoway;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

import java.util.UUID;

/*
This class contains things that deal with transmitting and receiving video / audio streams
 */

public class VideoStreaming
{
    /*
    hides the receiver VideoView and unhides the broadcaster SurfaceView
    */
    void showBroadcasterVideoSurface()
    {
        ((Activity)MainActivity.usefulStuff.mainContext).findViewById(R.id.transmitterVideoView).setVisibility(View.VISIBLE);
        ((Activity)MainActivity.usefulStuff.mainContext).findViewById(R.id.receiverVideoView).setVisibility(View.GONE);
    }

    /*
    shows the receiver VideoView and hides the broadcaster SurfaceView
    */
    void showReceiverVideoSurface()
    {
        ((Activity)MainActivity.usefulStuff.mainContext).findViewById(R.id.transmitterVideoView).setVisibility(View.GONE);
        ((Activity)MainActivity.usefulStuff.mainContext).findViewById(R.id.receiverVideoView).setVisibility(View.VISIBLE);
    }


    /*
    This code is taken from the libstreaming example1 with minor modifications
     */
    void startVideoBroadcast(int port, SurfaceView videoSurface)
    {
        // Sets the port of the RTSP server to 1234
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.usefulStuff.mainContext).edit();
        editor.putString(RtspServer.KEY_PORT, String.valueOf(1234));
        editor.commit();

        // Configures the SessionBuilder
        SessionBuilder.getInstance()
                .setSurfaceView(videoSurface)
                .setPreviewOrientation(90)
                .setContext(MainActivity.usefulStuff.mainContext)
                .setAudioEncoder(SessionBuilder.AUDIO_AAC)
                .setVideoEncoder(SessionBuilder.VIDEO_H264);

        // Starts the RTSP server
        MainActivity.usefulStuff.mainContext.startService(new Intent(MainActivity.usefulStuff.mainContext, RtspServer.class));
    }

    /*
    Sets up an android media player to stream rtsp from server at the given ip / port
    */
    void playVideoBroadcast(int port, String ip, final VideoView myVideoView)
    {
        final int position = 0;
        final ProgressDialog progressDialog;
        MediaController mediaControls = new MediaController(MainActivity.usefulStuff.mainContext);

        // create a progress bar while the video file is loading
        progressDialog = new ProgressDialog(MainActivity.usefulStuff.mainContext);
        // set a title for the progress bar
        progressDialog.setTitle("Rtsp Client Test");
        // set a message for the progress bar
        progressDialog.setMessage("Loading...");
        //set the progress bar not cancelable on users' touch
        progressDialog.setCancelable(false);
        // show the progress bar
        progressDialog.show();

        try
        {
            //set the media controller in the VideoView
            myVideoView.setMediaController(mediaControls);

            //set the uri of the video to be played
            myVideoView.setVideoURI(Uri.parse("rtsp://"+ip+":"+port));
        }
        catch (Exception e)
        {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }

        myVideoView.requestFocus();
        //we also set an setOnPreparedListener in order to know when the video file is ready for playback
        myVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
        {
            public void onPrepared(MediaPlayer mediaPlayer)
            {
                // close the progress bar and play the video
                progressDialog.dismiss();
                //if we have a position on savedInstanceState, the video playback should start from here
                myVideoView.seekTo(position);
                if (position == 0)
                {
                    myVideoView.start();
                }
                else
                {
                    //if we come from a resumed activity, video playback will be paused
                    myVideoView.pause();
                }
            }
        });

    }
}
