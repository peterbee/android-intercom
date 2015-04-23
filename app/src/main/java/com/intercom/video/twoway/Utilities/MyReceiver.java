package com.intercom.video.twoway.Utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.intercom.video.twoway.Services.ListenerService;

/**
 * @Author Sean luther on 4/12/15.
 * A helper class for receiving intents
 */
public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent service = new Intent(context, ListenerService.class);
            context.startService(service);
        }
    }
}