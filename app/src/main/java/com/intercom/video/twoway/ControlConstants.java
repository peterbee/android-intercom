package com.intercom.video.twoway;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.UUID;

/*
This class contains constants used for controlling stuff throughout the app
includes:

- Command constants sent as intents from service to activity
- Command constants sent over network from device to device
- Stream setting constants (bitrate, resolution etc)

 */
public class ControlConstants
{
    // commands sent via intent to the activity from the service
    final String INTENT_COMMAND_START_ACTIVITY = "START_ACTIVITY";
    final String INTENT_COMMAND_START_STREAMING_TRANSMITTING = "START_STREAMING_TRANSMITTING";
    final String INTENT_COMMAND_START_STREAMING_RECEIVING = "START_STREAMING_RECEIVING";

    final String INTENT_COMMAND_STOP_STREAMING = "STOP_STREAMING";

    // commands sent over network from device to device
    final String NETWORK_COMMAND_START_STREAMING = "START_STREAMING";
    final String NETWORK_COMMAND_STOP_STREAMING = "STOP_STREAMING";

    // stream settings
    final int X_RESOLUTION = 320;
    final int Y_RESOLUTION = 240;
    final int FRAMERATE = 20;
    final int BITRATE = 500000;

    ControlConstants()
    {

    }
}
