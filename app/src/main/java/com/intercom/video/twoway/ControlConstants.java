package com.intercom.video.twoway;

/*
This class contains constants used for controlling stuff throughout the app
includes:

- Command constants sent as intents from service to activity
- Command constants sent over network from device to device
- Stream setting constants (bitrate, resolution etc)

 */
public class ControlConstants {
    // commands sent via intent to the activity from the service
    final String INTENT_COMMAND_START_ACTIVITY = "START_ACTIVITY";
    final String INTENT_COMMAND_START_STREAMING_FIRST = "START_STREAMING_FIRST";
    final String INTENT_COMMAND_START_STREAMING_SECOND = "START_STREAMING_SECOND";

    final String INTENT_COMMAND_STOP_STREAMING = "STOP_STREAMING";

    // commands sent over network from device to device
    final String NETWORK_COMMAND_START_STREAMING = "START_STREAMING";
    final String NETWORK_COMMAND_STOP_STREAMING = "STOP_STREAMING";

    ControlConstants() {

    }
}
