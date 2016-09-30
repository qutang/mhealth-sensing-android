package io.github.qutang.sensing;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;

/**
 * Created by Qu on 9/28/2016.
 */

public class SensingService extends Service implements SensorEventListener2 {


    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION_ID = 5544;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private int counter = 0;
    private int seconds = 0;
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private long previousSr = 0;
    private long previousReport = 0;

    @Override
    public void onFlushCompleted(Sensor sensor) {

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        counter++;
        if(previousSr == 0) previousSr = sensorEvent.timestamp / 1000000;
        if(previousReport == 0) previousReport = sensorEvent.timestamp / 1000000;
        if(sensorEvent.timestamp / 1000000 - previousSr >= 1000){ // sampling rate update every 1s
            previousSr = sensorEvent.timestamp / 1000000;
            Log.i("Sampling rate", String.valueOf(counter));
            EventBus.getDefault().post(new ContentUpdateEvent(
                    String.format("%02d:%02d:%02d",
                            seconds / 3600,
                            seconds % 3600 / 60,
                            seconds % 3600 % 60) + ", " + counter + " Hz"));
            counter = 0;
            seconds++;
            Log.i("Data size", String.valueOf(ApplicationState.getState().watchAccelData.size()));
        }
        if(sensorEvent.timestamp / 1000000 - previousReport >= 50){ // chart updating at 20 Hz
            previousReport = sensorEvent.timestamp / 1000000;
        }
        ApplicationState.getState().addWatchAccelDataPoint(sensorEvent.timestamp / 1000000, sensorEvent.values);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public class LocalBinder extends Binder {
        SensingService getService() {
            return SensingService.this;
        }
    }

    @Override
    public void onCreate() {
        // Display a notification about us starting.  We put an icon in the status bar.
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        ApplicationState.getState().setWatchAccelSensor(getPhoneSensorInfo());
        counter = 0;
        seconds = 0;
    }

    public String getPhoneSensorInfo(){
        StringBuilder builder = new StringBuilder();
        builder.append("FiFoMaxEventCount,").append(mSensor.getFifoMaxEventCount()).append(System.lineSeparator())
                .append("FiFoReservedEventCount,").append(mSensor.getFifoReservedEventCount()).append(System.lineSeparator())
                .append("MaxDelay,").append(mSensor.getMaxDelay()).append(System.lineSeparator())
                .append("MinDelay,").append(mSensor.getMinDelay()).append(System.lineSeparator())
                .append("MaximumRange,").append(mSensor.getMaximumRange()).append(System.lineSeparator())
                .append("Name,").append(mSensor.getName()).append(System.lineSeparator())
                .append("Resolution,").append(mSensor.getResolution()).append(System.lineSeparator())
                .append("ReportingMode,").append(mSensor.getReportingMode()).append(System.lineSeparator())
                .append("Vendor,").append(mSensor.getVendor()).append(System.lineSeparator())
                .append("Version,").append(mSensor.getVersion()).append(System.lineSeparator())
                .append("Power,").append(mSensor.getPower()).append(System.lineSeparator())
                .append("Type,").append(mSensor.getStringType()).append(System.lineSeparator())
                .append("WakeUpSensor,").append(mSensor.isWakeUpSensor()).append(System.lineSeparator())
                .append("Delay").append(ApplicationState.getState().watchAccelDelay).append(System.lineSeparator());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.append("DynamicSensor,").append(mSensor.isDynamicSensor()).append(System.lineSeparator())
                    .append("ID,").append(mSensor.getId()).append(System.lineSeparator());
        }
        return builder.toString();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        showNotification();
        mSensorManager.registerListener(this, mSensor, ApplicationState.getState().watchAccelDelay);
        Toast.makeText(this, "Start sensing", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this, mSensor);
        // Cancel the persistent notification.
        stopForeground(true);
        // Tell the user we stopped.
        Toast.makeText(this, "Stopped sensing", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = "Sensing using phone accelerometer";

        // The PendingIntent to launch our activity if the user selects this notification

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentText(text)  // the contents of the entry
                .build();

        // Send the notification.
        startForeground(NOTIFICATION_ID, notification);
    }
}