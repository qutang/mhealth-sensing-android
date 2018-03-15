package io.github.qutang.sensing;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
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
    private java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private long previousSr = 0;
    private long previousReport = 0;
    private long previousSave = 0;
    private NotificationManager nm;
    private Context mContext;

    @Override
    public void onFlushCompleted(Sensor sensor) {

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        long ts = 0;
        if(Math.abs(sensorEvent.timestamp / 1000000L - System.currentTimeMillis()) < 3600000){
            // the sensor event timestamp is the actual time
            ts = sensorEvent.timestamp / 1000000L;
        }else{
            ts = System.currentTimeMillis()
                    + (sensorEvent.timestamp - System.nanoTime()) / 1000000L;
        }
        counter++;
        if(previousSr == 0) previousSr = ts;
        if(ts - previousSr >= 1000){
            ApplicationState.getState().setElapsedSeconds(ApplicationState.getState().elapsedSeconds + 1);
            Log.i("Accel Sampling rate", String.valueOf(counter));
            ApplicationState.getState().addPhoneSamplingRateDataPoint(ts, counter);
            previousSr = ts;
            counter = 0;
            String timer = String.format("%02d:%02d:%02d",
                    ApplicationState.getState().elapsedSeconds / 3600,
                    ApplicationState.getState().elapsedSeconds % 3600 / 60,
                    ApplicationState.getState().elapsedSeconds % 3600 % 60);
            EventBus.getDefault().post(new SnackBarMessageEvent("Recording: " + timer
                    , false));
            Notification notification = createForegroundNotification(timer);
            nm.notify(NOTIFICATION_ID, notification);

        }
        if(previousReport == 0) previousReport = ts;
        if(ts - previousReport >= 50){ // chart updating at 20 Hz
            previousReport = ts;
            EventBus.getDefault().post(sensorEvent);
        }
        if(previousSave == 0) previousSave = ts;
        if(ts - previousSave >= 1000 * 60){
            previousSave = ts;
            ApplicationState.getState().saveOnTheFly(mContext);
        }
        ApplicationState.getState().addPhoneAccelDataPoint(ts, sensorEvent.values);
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
        ApplicationState.getState().setPhoneAccelSensor(getPhoneSensorInfo());
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        counter = 0;
        mContext = getApplicationContext();
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
                .append("Delay").append(ApplicationState.getState().phoneAccelDelay).append(System.lineSeparator());
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
        mSensorManager.registerListener(this, mSensor, ApplicationState.getState().phoneAccelDelay);
        ApplicationState.getState().setElapsedSeconds(0);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this, mSensor);
        // Cancel the persistent notification.
        stopForeground(true);
        // Tell the user we stopped.
        Toast.makeText(this, "Stopped sensing", Toast.LENGTH_SHORT).show();
        save();
    }

    public void save(){
        EventBus.getDefault().post(new SnackBarMessageEvent("Saving data: 0%", false));
        ApplicationState.getState().saveData(mContext);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    private Notification createForegroundNotification(String contentText){
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = "Sensing using phone accelerometer";

        // The PendingIntent to launch our activity if the user selects this notification

        // Set the info for the views that show in the notification panel.
        Intent notifyIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        Notification notification = new Notification.Builder(this)
                .setContentTitle(text)
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentText(contentText)  // the contents of the entry
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_media_play)
                .build();
        return notification;
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        Notification notification = createForegroundNotification("00:00:00");

        // Send the notification.
        startForeground(NOTIFICATION_ID, notification);
    }
}
