package io.github.qutang.sensing;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;

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
import io.github.qutang.sensing.shared_android.MediaScanner;

/**
 * Created by Qu on 9/28/2016.
 */

public class ApplicationState {
    private static ApplicationState state;
    private DataSet phoneAccelDataSet;
    private DataSet phoneSamplingRateDataSet;
    private Context mContext;
    private MediaScanner mScanner;

    private ApplicationState(Context mContext){
        this.mContext = mContext;
        this.phoneAccelDataSet = new DataSet(android.os.Build.MODEL.replace(" ", ""), getIMEI(mContext), "AccelerometerCalibrated", "sensor", "HEADER_TIME_STAMP,X,Y,Z");
        this.phoneSamplingRateDataSet = new DataSet(android.os.Build.MODEL.replace(" ", ""), getIMEI(mContext), "AccelerometerSamplingRate", "feature", "HEADER_TIME_STAMP,SR");
        this.phoneAccelDataSet.registerProgressListener(new DataSet.OnDataSaveProgressListener() {
            @Override
            public void updateProgress(int percent) {
                EventBus.getDefault().post(new SnackBarMessageEvent("Saving phone accelerometer data: " + percent + "%", false));
            }
        });
        this.phoneSamplingRateDataSet.registerProgressListener(new DataSet.OnDataSaveProgressListener() {
            @Override
            public void updateProgress(int percent) {
                EventBus.getDefault().post(new SnackBarMessageEvent("Saving phone accelerometer sampling rate data: " + percent + "%", false));
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

    public String phoneAccelSensor = "";
    public synchronized void setPhoneAccelSensor(String sensorInfo){
        this.phoneAccelSensor = sensorInfo;
    }



    public boolean isWriting = false;

    public int phoneAccelDelay = SensorManager.SENSOR_DELAY_GAME;
    // NEXUS 4
    // game delay is 50 Hz
    // normal and UI delay is 5 Hz
    // fastest is 200 Hz

    // MOTO G
    // fastest is 100 Hz
    // normal delay is 5 Hz
    // game delay is 50 Hz
    // UI delay is 15 Hz

    public synchronized void setPhoneAccelDelay(int sensorDelay){
        phoneAccelDelay = sensorDelay;
    }


    public void resetRecordingStatus(){
        this.phoneAccelDataSet.reset();
        this.phoneSamplingRateDataSet.reset();
        this.elapsedSeconds = 0;
    }

    public synchronized void addPhoneAccelDataPoint(long ts, float[] values){
        float[] scaled = new float[3];
        for(int i = 0; i < values.length; i++){
            scaled[i] = values[i] / 9.81f;
        }
        DataPoint point = new DataPoint(ts, scaled, "PhoneAccel");
        phoneAccelDataSet.addDataPoint(point);
    }

    public synchronized void savePhoneAccelData(){
        AsyncExecutor.create().execute(new AsyncExecutor.RunnableEx() {
            @Override
            public void run() throws Exception {
            String root = Environment.getExternalStorageDirectory() + "/sensing/phone/MasterSynced";
            phoneAccelDataSet.startSaving();
            phoneAccelDataSet.saveData(root);
            phoneAccelDataSet.stopSaving();
            }
        });
    }

    public synchronized void addPhoneSamplingRateDataPoint(long ts, float sr){
        DataPoint point = new DataPoint(ts, new float[]{sr}, "PhoneSR");
        phoneSamplingRateDataSet.addDataPoint(point);
    }

    public synchronized void savePhoneSamplingRateData(){
        AsyncExecutor.create().execute(new AsyncExecutor.RunnableEx() {
            @Override
            public void run() throws Exception {
                String root = Environment.getExternalStorageDirectory() + "/sensing/phone/Derived";
                phoneSamplingRateDataSet.startSaving();
                phoneSamplingRateDataSet.saveData(root);
                phoneSamplingRateDataSet.stopSaving();
            }
        });
    }

    public synchronized void saveData(final Context mContext){
        mScanner = new MediaScanner(mContext, new File(Environment.getExternalStorageDirectory() + "/sensing/"), null);
        AsyncExecutor.create().execute(new AsyncExecutor.RunnableEx() {
            @Override
            public void run() throws Exception {
                String root = Environment.getExternalStorageDirectory() + "/sensing/phone/MasterSynced";
                phoneAccelDataSet.startSaving();
                phoneAccelDataSet.saveData(root);
                phoneAccelDataSet.stopSaving();
                root = Environment.getExternalStorageDirectory() + "/sensing/phone/Derived/";
                phoneSamplingRateDataSet.startSaving();
                phoneSamplingRateDataSet.saveData(root);
                phoneSamplingRateDataSet.stopSaving();
                saveSensorInfo(phoneAccelSensor, android.os.Build.MODEL.replace(" ", "") + ".meta.csv");
                zipEverything();
                EventBus.getDefault().post(new SnackBarMessageEvent("Saved", false));
                mScanner.scan();
            }
        });
    }

    private void zipEverything() throws ZipException {
        EventBus.getDefault().post(new SnackBarMessageEvent("Zipping files...", false));
        File zipFile = new File(Environment.getExternalStorageDirectory() + "/sensing", "phone.zip");
        if(!zipFile.getParentFile().exists()) zipFile.getParentFile().mkdirs();
        if(zipFile.exists()) zipFile.delete();
        ZipFile zip = new ZipFile(zipFile);
        ZipParameters paras = new ZipParameters();
        paras.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        paras.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_FASTEST);
        paras.setIncludeRootFolder(false);
        zip.addFolder(Environment.getExternalStorageDirectory() + "/sensing/phone", paras);
    }

    private String getIMEI(Context mContext) {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return "UnknownIMEI";
        }else{
            TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            return telephonyManager.getDeviceId();
        }
    }

    private void saveSensorInfo(String sensorInfo, String filename) throws IOException {
        File savedFile = new File(Environment.getExternalStorageDirectory() + "/sensing/phone/Derived/", filename);
        if(!savedFile.getParentFile().exists()) savedFile.getParentFile().mkdirs();
        if(savedFile.exists()) savedFile.delete();
        BufferedWriter writer = new BufferedWriter(new FileWriter(savedFile, false));
        writer.write(sensorInfo);
        writer.write(System.lineSeparator());
        writer.flush();
        writer.close();
    }
}
