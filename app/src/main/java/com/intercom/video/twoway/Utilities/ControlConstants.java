package com.intercom.video.twoway.Utilities;

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
    public static final String INTENT_COMMAND_START_ACTIVITY = "START_ACTIVITY";
    public static final String INTENT_COMMAND_START_STREAMING_FIRST = "START_STREAMING_FIRST";
    public static final String INTENT_COMMAND_START_STREAMING_SECOND = "START_STREAMING_SECOND";

    public static final String INTENT_COMMAND_STOP_STREAMING = "STOP_STREAMING";

    // commands sent over network from device to device
    public static final String NETWORK_COMMAND_START_STREAMING = "START_STREAMING";
    public static final String NETWORK_COMMAND_STOP_STREAMING = "STOP_STREAMING";
    public static final String INTENT_COMMAND_TRANSFER_PROFILE = "TRANSFER_PROFILE";
}
