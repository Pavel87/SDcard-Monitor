package com.pacmac.sdcheck;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

//TODO IMPLEMENT ABOUT

    private static final String SETTING = "setting";
    private static final String IS_RUNNING = "isRunning";
    private static final String IS_BOOT = "isBoot";
    Button start, stop, export;
    TextView logView;
    ScrollView scrollText;
    BroadcastReceiver serviceBroadcastReceiver;
    private final String fileName = "log.txt";
    private final String TAG = "PACMAC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        export = (Button) findViewById(R.id.export);

        logView = (TextView) findViewById(R.id.log);
        scrollText = (ScrollView) findViewById(R.id.scrollText);

        SharedPreferences setting = getSharedPreferences(SETTING, MODE_PRIVATE);
        boolean isRunning = setting.getBoolean(IS_RUNNING, false);
        if (isRunning) {
            // button images change base on
            start.setEnabled(false);
            stop.setEnabled(true);
            if(Build.VERSION.SDK_INT <21) {
                start.setBackground(getResources().getDrawable(R.drawable.play_button_dis));
                stop.setBackground(getResources().getDrawable(R.drawable.stop_button));
            }else {
                start.setBackground(getResources().getDrawable(R.drawable.play_button_dis,null));
                stop.setBackground(getResources().getDrawable(R.drawable.stop_button,null));
            }
        }

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //save state and restore value in autostartreceiver
                SharedPreferences setting = getSharedPreferences(SETTING, MODE_PRIVATE);
                SharedPreferences.Editor editor = setting.edit();
                editor.putBoolean(IS_RUNNING, true);
                editor.commit();

                Intent intent = new Intent(getApplicationContext(), SDCardWatcher.class);
                intent.putExtra(IS_BOOT, false);

                stop.setEnabled(true);
                start.setEnabled(false);
                if(Build.VERSION.SDK_INT <21) {
                    start.setBackground(getResources().getDrawable(R.drawable.play_button_dis));
                    stop.setBackground(getResources().getDrawable(R.drawable.stop_button));
                }else {
                    start.setBackground(getResources().getDrawable(R.drawable.play_button_dis,null));
                    stop.setBackground(getResources().getDrawable(R.drawable.stop_button,null));
                }
                startService(intent);
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //save state and restore value in autostartreceiver
                SharedPreferences setting = getSharedPreferences(SETTING, MODE_PRIVATE);
                SharedPreferences.Editor editor = setting.edit();
                editor.putBoolean(IS_RUNNING, false);
                editor.commit();

                Intent intent = new Intent(getApplicationContext(), SDCardWatcher.class);
                intent.putExtra(IS_BOOT, false);

                stop.setEnabled(false);
                start.setEnabled(true);
                if(Build.VERSION.SDK_INT <21) {
                    stop.setBackground(getResources().getDrawable(R.drawable.stop_button_dis));
                    start.setBackground(getResources().getDrawable(R.drawable.play_button));
                }
                else{
                    stop.setBackground(getResources().getDrawable(R.drawable.stop_button_dis, null));
                    start.setBackground(getResources().getDrawable(R.drawable.play_button, null));
                }
                startService(intent);
            }
        });

        export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File src = new File(getApplicationContext().getFilesDir(), fileName);
                File dst = new File(getApplicationContext().getExternalFilesDir(null), fileName);

                if (copyFile(src, dst))
                    Toast.makeText(getApplicationContext(), "Log saved: " + getExternalFilesDir(null) +"/" +fileName, Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getApplicationContext(), "No Log Available", Toast.LENGTH_SHORT).show();


            }
        });


        serviceBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setResultCode(Activity.RESULT_OK);
                updateView();
            }
        };
    }

    @Override
    protected void onResume() {
        registerReceiver(serviceBroadcastReceiver, new IntentFilter("com.pacmac.sdcheck.UPDATE"));
        updateView();
        super.onResume();
    }

    private void updateView() {
        //read file from internal
        File file = new File(getApplicationContext().getFilesDir(), fileName);
        if (file.exists()) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

                String line;
                StringBuilder sb = new StringBuilder("");
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
                bufferedReader.close();
                logView.setText(sb);
                scrollText.smoothScrollTo(0, logView.getBottom());
            } catch (FileNotFoundException e) {
                Log.i(TAG, "Log file not found");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(serviceBroadcastReceiver);
    }


    private boolean copyFile(File src, File dst) {

        try {
            InputStream is = new FileInputStream(src);
            OutputStream os = new FileOutputStream(dst);
            byte[] buff = new byte[1024];
            int len;
            while ((len = is.read(buff)) > 0) {
                os.write(buff, 0, len);
            }
            is.close();
            os.close();
        } catch (Exception ex) {
            Log.d(TAG, "Log File Not Available");
            return false;
        }

        return true;
    }


    // MENU
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_delete) {

            File file = new File(getApplicationContext().getFilesDir(), fileName);
            if (file.exists()) {
                file.delete();
                Toast.makeText(getApplicationContext(), "Log file has been deleted", Toast.LENGTH_SHORT).show();
                logView.setText("");
            }
            return true;
        }
        if (id == R.id.action_about) {
            openDialogAbout();
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    private void openDialogAbout() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this).
                setTitle("About").setMessage("Purpose of this app is to watch and record the behavior of SDcard.\n\n" +
                "Developed by pacmac.").setCancelable(true).setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();

    }

}
