package com.intercom.video.twoway.Utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;

/**
 * Created by Charles Toll on 3/10/15.
 *
 */
public class SharedPreferenceAccessor {
    public static String SETTINGS_MENU = "SETTINGS MENU";
    public static String DEVICE_NICKNAME = "device_nickname";
    public static String USE_CAMERA_VIEW = "use_camera_view";
    public static String PROFILE_PICTURE = "profile_picture";
    public static String NO_SUCH_SAVED_PREFERENCE = "0";
    private Context referenceActivity;

    public SharedPreferenceAccessor(Context callingActivity) throws NullPointerException
    {
        if(callingActivity != null ) {
            this.referenceActivity = callingActivity;
        }
        else
        {
            throw new NullPointerException("Activity passed to constructor was null");
        }
    }

    public void writeStringToSharedPrefs(String saveTitle, String toWrite, String preferenceName)
    {
        SharedPreferences settings = referenceActivity.getApplicationContext().
                getSharedPreferences(preferenceName, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(saveTitle, toWrite);
        editor.apply();
    }

    public void writeBooleanToSharedPrefs(String saveTitle,
                                          boolean toWrite, String preferenceName)
    {
        SharedPreferences settings = referenceActivity.getApplicationContext().
                getSharedPreferences(preferenceName, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(saveTitle, toWrite);
        editor.apply();
    }

    public String loadStringFromSharedPreferences(String prefsName, String settingsTitle)
    {
        String preferencesToReturn;
        SharedPreferences settings = referenceActivity.getApplicationContext().
                getSharedPreferences(prefsName, 0);
        preferencesToReturn = settings.getString(settingsTitle,
                SharedPreferenceAccessor.NO_SUCH_SAVED_PREFERENCE);
        return preferencesToReturn;
    }

    public Boolean loadBooleanFromSharedPreferences(String prefsName, String settingsTitle)
    {
        boolean preferencesToReturn;
        SharedPreferences settings = referenceActivity.getApplicationContext().
                getSharedPreferences(prefsName, 0);
        preferencesToReturn = settings.getBoolean(settingsTitle, false);
        return preferencesToReturn;
    }

    public String getIp()
    {
        WifiManager wifi = (WifiManager) referenceActivity.getSystemService(Context.WIFI_SERVICE);
        return "";
    }
}
