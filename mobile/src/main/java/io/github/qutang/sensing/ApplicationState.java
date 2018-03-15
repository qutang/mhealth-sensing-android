package io.github.qutang.sensing;

import android.*;
import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;

import com.github.mikephil.charting.data.LineData;

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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Qu on 9/28/2016.
 */

public class ApplicationState {
    private static ApplicationState state;

    private ApplicationState(){}
    public static ApplicationState getState(){
        if(state == null){
            state = new ApplicationState();
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

    private Context mContext;

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

    public ArrayList<DataPoint> phoneAccelData = new ArrayList<>();
    public ArrayList<DataPoint> phoneAccelAlterBuffer = new ArrayList<>();
    public ArrayList<DataPoint> phoneSamplingRateData = new ArrayList<>();

    public synchronized void addPhoneAccelDataPoint(long ts, float[] values){
        float[] scaled = new float[3];
        for(int i = 0; i < values.length; i++){
            scaled[i] = values[i] / 9.81f;
        }
        if(isWriting){
            phoneAccelAlterBuffer.add(new DataPoint(ts, scaled, "phone_accel"));
        }else{
            if(!phoneAccelAlterBuffer.isEmpty()){
                if(phoneAccelData.isEmpty()) {
                    System.out.println("The alternative buffer has some data: " + phoneAccelAlterBuffer.size() + ", copy to the original buffer");
                    for (DataPoint data : phoneAccelAlterBuffer) {
                        phoneAccelData.add(data);
                    }
                    phoneAccelAlterBuffer.clear();
                }else{
                    System.out.println("The alternative buffer and orignal buffer both have some data, something is wrong");
                }
            }
            phoneAccelData.add(new DataPoint(ts, scaled, "phone_accel"));
        }
    }

    public synchronized void addPhoneSamplingRateDataPoint(long ts, float sr){
        phoneSamplingRateData.add(new DataPoint(ts, new float[]{sr}, "phone_sr"));
    }

    public synchronized void saveData(final Context mContext){
        isWriting = true;
        AsyncExecutor.create().execute(new AsyncExecutor.RunnableEx() {
            @Override
            public void run() throws Exception {
                savePhoneAccelData(mContext);
                isWriting = false;
                savePhoneSamplingRateData();
                saveSensorInfo(phoneAccelSensor, android.os.Build.MODEL.replace(" ", "") + ".meta.csv");
                zipEverything();
                EventBus.getDefault().post(new SnackBarMessageEvent("Saved", false));
            }
        });
    }

    public synchronized void saveOnTheFly(final Context mContext){
        isWriting = true;
        AsyncExecutor.create().execute(new AsyncExecutor.RunnableEx() {
            @Override
            public void run() throws Exception {
                savePhoneAccelData(mContext);
                isWriting = false;
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

    private void savePhoneAccelData(Context mContext) throws IOException {
        System.out.println("Start writing data");
        File folder = new File(Environment.getExternalStorageDirectory() + "/sensing/phone/MasterSynced");
        if(!folder.exists()){
            folder.mkdirs();
        }
        if(phoneAccelData.size() <= 0) return;
        BufferedWriter writer = null;
        int count = 0;
        int previous = 0;
        int current;
        String lastHour = "";
        String filename;
        File filepath;
        for(DataPoint datapoint : phoneAccelData){
            filename = android.os.Build.MODEL.replace(" ", "") + "-AccelerometerCalibrated-NA." + this.getIMEI(mContext) + "-AccelerometerCalibrated." + datapoint.toTimestampString() + ".sensor.csv";
            filepath = new File(folder + "/" + filename);
            if(!lastHour.equals(datapoint.toHourString())){
                lastHour = datapoint.toHourString();
                if(filepath.exists()){
                    writer = new BufferedWriter(new FileWriter(filepath, true));
                }else {
                    writer = new BufferedWriter(new FileWriter(filepath, false));
                    writer.write("HEADER_TIME_STAMP,X,Y,Z");
                    writer.write(System.lineSeparator());
                }
            }
            if(writer == null){
                writer = new BufferedWriter(new FileWriter(filepath, false));
                writer.write("HEADER_TIME_STAMP,X,Y,Z");
                writer.write(System.lineSeparator());
            }
            count++;
            writer.write(datapoint.toString());
            writer.write(System.lineSeparator());
            if((current = Math.round(count / (float)phoneAccelData.size() * 100)) != previous){
                previous = current;
                EventBus.getDefault().post(new SnackBarMessageEvent("Saving phone accelerometer data: " + current + "%", false));
            }
        }
        writer.flush();
        writer.close();
        phoneAccelData.clear();
        System.out.println("Finish writing data");
    }

    private void savePhoneSamplingRateData() throws IOException {
        File savedFile = new File(Environment.getExternalStorageDirectory() + "/sensing/phone/Derived/", "SamplingRate.feature.csv");
        if(!savedFile.getParentFile().exists()) savedFile.getParentFile().mkdirs();
        if(savedFile.exists()) savedFile.delete();
        if(phoneSamplingRateData.size() <= 0) return;
        BufferedWriter writer = new BufferedWriter(new FileWriter(savedFile, false));
        writer.write("HEADER_TIME_STAMP,SR");
        writer.write(System.lineSeparator());
        int count = 0;
        int previous = 0;
        int current;
        for(DataPoint datapoint : phoneSamplingRateData){
            count++;
            writer.write(datapoint.toString());
            writer.write(System.lineSeparator());
            if((current = Math.round(count / (float)phoneSamplingRateData.size() * 100)) != previous){
                previous = current;
                EventBus.getDefault().post(new SnackBarMessageEvent("Saving phone sampling rate data: " + current + "%", false));
            }
        }
        writer.flush();
        writer.close();
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
