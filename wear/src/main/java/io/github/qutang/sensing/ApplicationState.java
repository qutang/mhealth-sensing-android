package io.github.qutang.sensing;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.util.AsyncExecutor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import io.github.qutang.sensing.shared.DataPoint;
import io.github.qutang.sensing.shared.DataSet;

/**
 * Created by Qu on 9/28/2016.
 */

public class ApplicationState {
    private static ApplicationState state;
    private DataSet wearAccelDataSet;
    private DataSet wearSamplingRateDataSet;
    private Context mContext;
    public static final String TAG = "WearApplicationState";

    private ApplicationState(Context mContext){
        this.mContext = mContext;
        this.wearAccelDataSet = new DataSet(android.os.Build.MODEL.replace(" ", ""), Build.SERIAL, "AccelerometerCalibrated", "sensor", "HEADER_TIME_STAMP,X,Y,Z");
        this.wearSamplingRateDataSet = new DataSet(android.os.Build.MODEL.replace(" ", ""), Build.SERIAL, "AccelerometerSamplingRate", "feature", "HEADER_TIME_STAMP,SR");
        this.wearAccelDataSet.registerProgressListener(new DataSet.OnDataSaveProgressListener() {
            @Override
            public void updateProgress(int percent) {
                EventBus.getDefault().post(new ContentUpdateEvent("Saving wear accelerometer data: " + percent + "%"));
            }
        });
        this.wearSamplingRateDataSet.registerProgressListener(new DataSet.OnDataSaveProgressListener() {
            @Override
            public void updateProgress(int percent) {
                EventBus.getDefault().post(new ContentUpdateEvent("Saving wear accelerometer sampling rate data: " + percent + "%"));
            }
        });
    }

    public static ApplicationState getState(Context mContext){
        if(state == null){
            state = new ApplicationState(mContext);
        }
        return state;
    }

    public boolean isRecording = false;
    public synchronized void setRecording(boolean isRecording){
        this.isRecording = isRecording;
    }

    public int elapsedSeconds = 0;
    public synchronized void setElapsedSeconds(int seconds){
        this.elapsedSeconds = seconds;
    }

    public String wearAccelSensor = "";
    public synchronized void setWearAccelSensor(String sensorInfo){
        this.wearAccelSensor = sensorInfo;
    }



    public boolean isWriting = false;

    public int wearAccelDelay = SensorManager.SENSOR_DELAY_FASTEST;
    // NEXUS 4
    // game delay is 50 Hz
    // normal and UI delay is 5 Hz
    // fastest is 200 Hz

    // MOTO G
    // fastest is 100 Hz
    // normal delay is 5 Hz
    // game delay is 50 Hz
    // UI delay is 15 Hz

    public synchronized void setWearAccelDelay(int sensorDelay){
        wearAccelDelay = sensorDelay;
    }


    public void resetRecordingStatus(){
        this.wearAccelDataSet.reset();
        this.wearSamplingRateDataSet.reset();
        this.elapsedSeconds = 0;
    }

    public synchronized void addWearAccelDataPoint(long ts, float[] values){
        float[] scaled = new float[3];
        for(int i = 0; i < values.length; i++){
            scaled[i] = values[i] / 9.81f;
        }
        DataPoint point = new DataPoint(ts, scaled, "WearAccel");
        wearAccelDataSet.addDataPoint(point);
    }

    public synchronized void saveWearAccelData(){
        AsyncExecutor.create().execute(new AsyncExecutor.RunnableEx() {
            @Override
            public void run() throws Exception {
            String root = Environment.getExternalStorageDirectory() + "/sensing/wear/MasterSynced";
            wearAccelDataSet.startSaving();
            wearAccelDataSet.saveData(root);
            wearAccelDataSet.stopSaving();
            }
        });
    }

    public synchronized void addWearSamplingRateDataPoint(long ts, float sr){
        DataPoint point = new DataPoint(ts, new float[]{sr}, "WearSR");
        wearSamplingRateDataSet.addDataPoint(point);
    }

    public synchronized void saveWearSamplingRateData(){
        AsyncExecutor.create().execute(new AsyncExecutor.RunnableEx() {
            @Override
            public void run() throws Exception {
                String root = Environment.getExternalStorageDirectory() + "/sensing/wear/Derived";
                wearSamplingRateDataSet.startSaving();
                wearSamplingRateDataSet.saveData(root);
                wearSamplingRateDataSet.stopSaving();
            }
        });
    }

    public synchronized void saveData(final Context mContext){
        AsyncExecutor.create().execute(new AsyncExecutor.RunnableEx() {
            @Override
            public void run() throws Exception {
                String root = Environment.getExternalStorageDirectory() + "/sensing/wear/MasterSynced";
                wearAccelDataSet.startSaving();
                wearAccelDataSet.saveData(root);
                wearAccelDataSet.stopSaving();
                root = Environment.getExternalStorageDirectory() + "/sensing/wear/Derived/";
                wearSamplingRateDataSet.startSaving();
                wearSamplingRateDataSet.saveData(root);
                wearSamplingRateDataSet.stopSaving();
                saveSensorInfo(wearAccelSensor, android.os.Build.MODEL.replace(" ", "") + ".meta.csv");
                zipEverything();
                EventBus.getDefault().post(new ContentUpdateEvent("Saved"));
            }
        });
    }

    private void zipEverything() throws ZipException {
        EventBus.getDefault().post(new ContentUpdateEvent("Zipping files..."));
        File zipFile = new File(Environment.getExternalStorageDirectory() + "/sensing", "wear.zip");
        if(!zipFile.getParentFile().exists()) zipFile.getParentFile().mkdirs();
        if(zipFile.exists()) zipFile.delete();
        ZipFile zip = new ZipFile(zipFile);
        ZipParameters paras = new ZipParameters();
        paras.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        paras.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_FASTEST);
        paras.setIncludeRootFolder(false);
        zip.addFolder(Environment.getExternalStorageDirectory() + "/sensing/wear", paras);
    }

    private void saveSensorInfo(String sensorInfo, String filename) throws IOException {
        File savedFile = new File(Environment.getExternalStorageDirectory() + "/sensing/wear/Derived/", filename);
        if(!savedFile.getParentFile().exists()) savedFile.getParentFile().mkdirs();
        if(savedFile.exists()) savedFile.delete();
        BufferedWriter writer = new BufferedWriter(new FileWriter(savedFile, false));
        writer.write(sensorInfo);
        writer.write(System.lineSeparator());
        writer.flush();
        writer.close();
    }
}
