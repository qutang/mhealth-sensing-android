package io.github.qutang.sensing;

import android.app.Application;
import android.hardware.SensorManager;
import android.os.Environment;
import android.provider.ContactsContract;

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
import java.util.ArrayList;

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

    public synchronized void addPhoneAccelDataPoint(long ts, float[] values){
        float[] scaled = new float[3];
        for(int i = 0; i < values.length; i++){
            scaled[i] = values[i] / 9.81f;
        }
        phoneAccelData.add(new DataPoint(ts, scaled, "phone_accel"));
    }

    public synchronized void saveData(){
        AsyncExecutor.create().execute(new AsyncExecutor.RunnableEx() {
            @Override
            public void run() throws Exception {
                savePhoneAccelData();
                saveSensorInfo(phoneAccelSensor, "PhoneAccelerometer.meta.csv");
                zipEverything();
                EventBus.getDefault().post(new SnackBarMessageEvent("Saved", false));
            }
        });
    }

    private void zipEverything() throws ZipException {
        File zipFile = new File(Environment.getExternalStorageDirectory() + "/sensing", "data.zip");
        if(!zipFile.getParentFile().exists()) zipFile.getParentFile().mkdirs();
        if(zipFile.exists()) zipFile.delete();
        ZipFile zip = new ZipFile(zipFile);
        ZipParameters paras = new ZipParameters();
        paras.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        paras.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_FASTEST);
        paras.setIncludeRootFolder(false);
        zip.addFolder(Environment.getExternalStorageDirectory() + "/sensing/data", paras);
    }

    private void savePhoneAccelData() throws IOException {
        File savedFile = new File(Environment.getExternalStorageDirectory() + "/sensing/data", "PhoneAccelerometer.sensor.csv");
        if(!savedFile.getParentFile().exists()) savedFile.getParentFile().mkdirs();
        if(savedFile.exists()) savedFile.delete();
        if(phoneAccelData.size() <= 0) return;
        BufferedWriter writer = new BufferedWriter(new FileWriter(savedFile, false));
        writer.write("HEADER_TIME_STAMP,X,Y,Z");
        writer.write(System.lineSeparator());
        int count = 0;
        int previous = 0;
        int current;
        for(DataPoint datapoint : phoneAccelData){
            count++;
            writer.write(datapoint.toString());
            writer.write(System.lineSeparator());
            if((current = Math.round(count / (float)phoneAccelData.size() * 100)) != previous){
                previous = current;
                EventBus.getDefault().post(new SnackBarMessageEvent("Saving data... " + current + "%", false));
            }
        }
        writer.flush();
        writer.close();
    }

    private void saveSensorInfo(String sensorInfo, String filename) throws IOException {
        File savedFile = new File(Environment.getExternalStorageDirectory() + "/sensing/data", filename);
        if(!savedFile.getParentFile().exists()) savedFile.getParentFile().mkdirs();
        if(savedFile.exists()) savedFile.delete();
        BufferedWriter writer = new BufferedWriter(new FileWriter(savedFile, false));
        writer.write(sensorInfo);
        writer.write(System.lineSeparator());
        writer.flush();
        writer.close();
    }
}
