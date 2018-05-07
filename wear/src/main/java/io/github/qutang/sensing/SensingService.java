package io.github.qutang.sensing;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.os.PowerManager;
import android.os.SystemClock;
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
    private SimpleDateFormat formatter;
    private long previousSr = 0;
    private long previousTs = 0;
    private int previousPhoneAccelSave = 0;
    private NotificationManager nm;
    private Context mContext;
    private PowerManager.WakeLock mWakeLock;
    private ApplicationState mState;
    private static final String TAG = "SensingService";
    private int flushCount;
    private double flushSr;
    private int skipSample = 0;

    @Override
    public void onFlushCompleted(Sensor sensor) {
        Log.i(TAG, "Flush is completed");
        Log.i(TAG, "Flushed " + flushCount + " sensor events");
        Log.i(TAG, "Flushing sampling rate: " + flushCount / flushSr);
        Log.i(TAG, "Flushed " + flushSr + " seconds data");
        mWakeLock.release();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        long ts = 0;
        flushCount++;

        /*
        *
        * https://stackoverflow.com/questions/5500765/accelerometer-sensorevent-timestamp
        * https://developer.android.com/reference/android/os/SystemClock.html#elapsedRealtimeClock()
        *
        * */

        if(Math.abs(sensorEvent.timestamp / 1000000L - System.currentTimeMillis()) < 60000){
            // the sensor event timestamp is the actual time
            ts = sensorEvent.timestamp / 1000000L;
        }else{
            ts = System.currentTimeMillis()
                    + (sensorEvent.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000L;
        }

        if(skipSample == 1){
            skipSample = 0;
            return;
        }else{
            counter++;
            skipSample = 1;
        }

        if(previousSr == 0) previousSr = ts;
        if(previousTs == 0) previousTs = ts;
        else{
            flushSr = (flushSr + (ts - previousTs) / 1000.0);
            previousTs = ts;
        }

        if(previousPhoneAccelSave == 0) previousPhoneAccelSave = Integer.valueOf(new SimpleDateFormat("mm").format(ts));
        int currentMinute = Integer.valueOf(new SimpleDateFormat("mm").format(ts));
        if(currentMinute != previousPhoneAccelSave){
            previousPhoneAccelSave = currentMinute;
            mState.saveWearAccelData();
            mState.saveWearSamplingRateData();
        }

        if(ts - previousSr >= 1000){
            mState.setElapsedSeconds(mState.elapsedSeconds + 1);
            Log.i(TAG, "Accel Sampling rate, " + String.valueOf(counter));
            mState.addWearSamplingRateDataPoint(ts, counter);
            previousSr = ts;
            counter = 0;
            String timer = String.format("%02d:%02d:%02d",
                    mState.elapsedSeconds / 3600,
                    mState.elapsedSeconds % 3600 / 60,
                    mState.elapsedSeconds % 3600 % 60);
            EventBus.getDefault().post(new ContentUpdateEvent("Recording: " + timer));
            Notification notification = createForegroundNotification(timer);
            nm.notify(NOTIFICATION_ID, notification);

        }
        mState.addWearAccelDataPoint(ts, sensorEvent.values);
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
        flushCount = 0;
        flushSr = 0;
        mContext = getApplicationContext();
        mState = ApplicationState.getState(mContext);
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        counter = 0;
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "SensingService");
        formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    }

    public String getWearSensorInfo(){
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
                .append("Delay,").append(mState.wearAccelDelay).append(System.lineSeparator());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.append("DynamicSensor,").append(mSensor.isDynamicSensor()).append(System.lineSeparator())
                    .append("ID,").append(mSensor.getId()).append(System.lineSeparator());
        }
        return builder.toString();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent.getAction());

        if(intent.getAction().equals("start")){
            Log.i(TAG, "Aquired wakelock");
            mWakeLock.acquire();
            Log.i(TAG, "Start sensing service");
            start();
        }else if(intent.getAction().equals("stop")){
            Log.i(TAG, "Stop sensing service");
            stopSelf();
        }else if(intent.getAction().equals("flush")){
            Log.i(TAG, "Aquired wakelock");
            Log.i(TAG, "Flushing sensing service");
            flushCount = 0;
            flushSr = 0;
            previousTs = 0;
            if(mSensorManager != null) {
                mSensorManager.flush(this);
            }else{

                start();
            }
        }
        return START_STICKY;
    }

    public void start() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        String wearAccelInfo = getWearSensorInfo();
        Log.i(TAG, wearAccelInfo);
        mState.setWearAccelSensor(wearAccelInfo);
        showNotification();
        Log.i(TAG, "Started foreground service");
        boolean batchMode = mSensorManager.registerListener(this, mSensor, mState.wearAccelDelay);
        if(batchMode) {
            Log.i(TAG, "Registered sensor event listener in batch mode");
        }else {
            Log.i(TAG, "Registered in continuous mode");
        }
        mState.setElapsedSeconds(0);
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this, mSensor);
        Log.i(TAG, "Unregistered sensor event listener");
        // Cancel the persistent notification.
        stopForeground(true);
        Log.i(TAG, "Stopped foreground service");
        mWakeLock.release();
        Log.i(TAG, "Released wakelock");
        // Tell the user we stopped.
        Toast.makeText(this, "Stopped sensing", Toast.LENGTH_SHORT).show();
        save();
    }

    public void save(){
        EventBus.getDefault().post(new ContentUpdateEvent("Saving data: 0%"));
        mState.saveData(mContext);
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
                .setSmallIcon(android.R.drawable.ic_media_play)
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
