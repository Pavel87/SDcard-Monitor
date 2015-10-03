package com.pacmac.sdcheck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by pacmac on 9/26/2015.
 */
public class AutoStartReceiver extends BroadcastReceiver {

    private final String TAG = "PACMAC";
    private static final String IS_BOOT = "isBoot";

    @Override
    public void onReceive(Context context, Intent intent) {

        //Log.i(TAG, "starting SDCardWatcher service");
        Intent myIntent = new Intent(context, SDCardWatcher.class);
        myIntent.putExtra(IS_BOOT, true);
        context.startService(myIntent);
    }
}
