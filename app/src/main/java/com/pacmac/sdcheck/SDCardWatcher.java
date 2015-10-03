package com.pacmac.sdcheck;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

public class SDCardWatcher extends Service {


    private static final String TAG = "PACMAC";
    private static final String IS_RUNNING = "isRunning";
    private static final String SETTING = "setting";
    private static final int REQUEST_CODE = 8;
    private final int LOG_SIZE = 256000; //256kB
    private final String fileName = "log.txt";
    private boolean isRunning = false;
    private File file;


    private BroadcastReceiver receiver;

    @Override
    public void onCreate() {
        //Log.i(TAG, "Service onCreate");
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                //action = result message  + timestamp
                String action = intent.getAction();
                String result = getResult(action);
                String timestamp = getTimeStamp();
                //Log.d(TAG, timestamp + result);
                openWriteLog(timestamp, result);
                //send info about update to calling activity
                activtyUpdateRequest();
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addAction(Intent.ACTION_MEDIA_CHECKING);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_NOFS);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTABLE);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_SHARED);
        filter.addDataScheme("file");
        registerReceiver(receiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Check if sercvice should run or not
        SharedPreferences setting = getSharedPreferences(SETTING, MODE_PRIVATE);
        isRunning = setting.getBoolean(IS_RUNNING, false);
        boolean isBoot = intent.getExtras().getBoolean("isBoot", false);
        if (isBoot) {
            openWriteLog(getTimeStamp(), "OS REBOOT");
        }
        //Creating new thread for my service
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "+SDCheckService");
                while (isRunning) {
                    try {
                        Thread.sleep(10000);
                    } catch (Exception e) {
                        Log.e(TAG, "Inner Thread error");
                    }
                    // if (isRunning) {
                    // Log.d(TAG, "Service running");
                    // }
                }
                Log.i(TAG, "-SDCheckService");

                //Stop service once it finishes its task
                stopSelf();
            }
        }).start();
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        unregisterReceiver(receiver);
        Log.i(TAG, "--SDCheckService");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final String getResult(String action) {

        switch (action) {
            case Intent.ACTION_MEDIA_MOUNTED:
                return "SDcard is MOUNTED";
            case Intent.ACTION_MEDIA_UNMOUNTED:
                return "SDcard is present but UNMOUNTED";
            case Intent.ACTION_MEDIA_UNMOUNTABLE:
                return "SDcard is present but CANNOT be mounted";
            case Intent.ACTION_MEDIA_BAD_REMOVAL:
                return "SDcard has been removed by user(bad removal)";
            case Intent.ACTION_MEDIA_CHECKING:
                return "SD card is being disk-checked";
            case Intent.ACTION_MEDIA_NOFS:
                return "Incompatible FS (or FS is blank)";
            case Intent.ACTION_MEDIA_REMOVED:
                return "SDcard has been removed";
            case Intent.ACTION_MEDIA_EJECT:
                return "Eject request";
            case Intent.ACTION_MEDIA_SHARED:
                return "SDcard unmounted ->: USB MASS STORAGE";
            default:
                return "Unknown";
        }
    }

    private final String getTimeStamp() {
        Calendar calendar = Calendar.getInstance();
        //  Log.i(TAG,String.format("%02d" , calendar.get(Calendar.HOUR_OF_DAY)) + ":" + String.format("%02d", calendar.get(Calendar.MINUTE))+ ":" + String.format("%03d", calendar.get(Calendar.MILLISECOND)));
        //  Log.i(TAG, String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH)) + "/" + String.format("%02d", calendar.get(Calendar.MONTH)+1) + "/" + calendar.get(Calendar.YEAR));
        return String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH)) + "/" + String.format("%02d", calendar.get(Calendar.MONTH) + 1) + "/" + calendar.get(Calendar.YEAR) +
                " " + String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY)) + ":" + String.format("%02d", calendar.get(Calendar.MINUTE)) + ":" +
                String.format("%02d", calendar.get(Calendar.SECOND)) + "." + String.format("%03d", calendar.get(Calendar.MILLISECOND));
    }


    private final void openWriteLog(final String timeStamp, final String message) {
        // open and write file in another Thread
        new Thread(new Runnable() {
            @Override
            public void run() {

                //prepare the log file
                file = new
                        File(getApplicationContext().getFilesDir(), fileName);
                if (!file.exists()) {
                    try {
                        file.createNewFile();
                        Log.i(TAG, "file created written");
                    } catch (IOException e) {
                        Log.e(TAG, "CANNOT CREATE LOG FILE");
                    }
                }
                try {
                    if (file.length() > LOG_SIZE) {
                        file.delete();
                        file.createNewFile();
                    }
                    //BufferedWriter for performance, true to set append to file flag
                    BufferedWriter buf = new BufferedWriter(new FileWriter(file, true));
                    buf.append(timeStamp + "::" + message);
                    Log.i(TAG, "+ log line");
                    buf.newLine();
                    buf.close();
                } catch (IOException e) {
                    Log.e(TAG, "IO exception during writing log file");
                }

            }
        }).start();
    }

    private final void activtyUpdateRequest() {
        Intent intent = new Intent("com.pacmac.sdcheck.UPDATE");
        intent.putExtra("update", false);
        sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "test REsult");
                int result = getResultCode();
                if (result == Activity.RESULT_CANCELED) {
                    //Activity is not in foreground // show notification
                    sendNotificationSDEvent();
                } else
                    Log.d(TAG, "MainActivity on foreground");

            }
        }, null, Activity.RESULT_CANCELED, null, null);
    }


    private final void sendNotificationSDEvent() {

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, REQUEST_CODE, intent, 0);


        Notification sdEventNotification = new Notification.Builder(this).
                setContentTitle("SDcard Event").
                setContentText("SD status has changed").
                setSmallIcon(R.mipmap.ic_launcher).
                setContentIntent(pIntent).
                build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        sdEventNotification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(0, sdEventNotification);

    }
}