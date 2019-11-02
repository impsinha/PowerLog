package com.example.powerlog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static android.os.Environment.getExternalStorageDirectory;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2;
    private Timer t;
    private final int millisec=100;
    private Context context;

    private TextView batteryPerEl;
    private TextView batteryTempEl;
    private Switch dataRecEl;

    private int batteryPer;
    private float batteryTemp;
    private File file;

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            float temp = (float)(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,
                    0))/10;
            batteryTempEl.setText(String.valueOf(temp)+" °C");
            batteryPerEl.setText(String.valueOf(level)+" %");
            batteryTemp = temp;
            batteryPer = level;
        }
    };
    private DataLogger dataLogger = new DataLogger();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        batteryPerEl = (TextView) this.findViewById(R.id.batteryTxt);
        batteryTempEl = (TextView) this.findViewById(R.id.batteryTemp);
        dataRecEl = (Switch) this.findViewById(R.id.dataRec);
        dataRecEl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d("SWITCH",String.valueOf(isChecked));
                if(isChecked && checkStoragePermission() && createDirFile()){
                    dataLogger = new FileLogger();
                    Toast.makeText(context, "Recording started in file "+
                            file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                }
                else{
                    dataRecEl.setChecked(false);
                    dataLogger = new DataLogger();
                }
            }
        });
        runTimer();
    }

    /**
     * Change Battery Level in view (done by calling BroadcastReceiver object method
     */
    private void changeBatteryTxt(){
        this.registerReceiver(this.mBatInfoReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }
    /**
     * Start Running the Timer
     */
    private void runTimer(){
        stopTimer();
        t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                changeBatteryTxt();
                saveLog();
            }
        },0,millisec);
    }

    /**
     * Stop running the timer
     */
    private void stopTimer(){
        if(t!=null) t.cancel();
    }
    /**
     * Handle Logger Interaction
     */
    private void saveLog(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String format = simpleDateFormat.format(new Date());
        String log = format+" | "+String.valueOf(batteryPer)+" | "+String.valueOf(batteryTemp);
//        Log.d("DATA LOG",log);
        if(!dataLogger.appendLog(log)){
            Toast.makeText(this, "Unable to save to file", Toast.LENGTH_SHORT).show();
            dataRecEl.setChecked(false);
        }
    }
    /**
     * check permission
     */
    private boolean checkStoragePermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "Permission needed to save recorded data in file",
                        Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "Permission needed to read recorded data in file",
                        Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) return false;
        return true;
    }
    /**
     * create directory and file
     */
    private boolean createDirFile(){
        File tempDIR = new File(getExternalStorageDirectory()+"/PowerLog");
        file = new File(tempDIR,"power_log.txt");
        if(!tempDIR.exists()) {
            if (!tempDIR.mkdirs()) return false;
        }
        if(!file.exists()){
            try {
                file.createNewFile();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        FileLogger.setFilePath(file);
        return true;
    }
}

class DataLogger{
    public boolean appendLog(String text){return true;}
    public String getFilePath(){return "";}
}

class FileLogger extends DataLogger{
    private static File logFile;
    private BufferedWriter buf;
    public String getFilePath(){return logFile.getAbsolutePath();}
    public static void setFilePath(File logFile){FileLogger.logFile = logFile;}
    public boolean appendLog(String text)
    {
        try {
            buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }
}