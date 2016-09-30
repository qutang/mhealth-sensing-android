package io.github.qutang.sensing;

import android.hardware.SensorManager;
import android.os.Environment;
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

    public String watchAccelSensor = "";
    public synchronized void setWatchAccelSensor(String sensorInfo){
        this.watchAccelSensor = sensorInfo;
    }

    public int watchAccelDelay = SensorManager.SENSOR_DELAY_UI;
    // NEXUS 4
    // game delay is 50 Hz
    // normal and UI delay is 5 Hz
    // fastest is 200 Hz

    // MOTO G
    // fastest is 100 Hz
    // normal delay is 5 Hz
    // game delay is 50 Hz
    // UI delay is 15 Hz

    public synchronized void setWatchAccelDelay(int sensorDelay){
        watchAccelDelay = sensorDelay;
    }

    public ArrayList<DataPoint> watchAccelData = new ArrayList<>();

    public synchronized void addWatchAccelDataPoint(long ts, float[] values){
        float[] scaled = new float[3];
        for(int i = 0; i < values.length; i++){
            scaled[i] = values[i] / 9.81f;
        }
        watchAccelData.add(new DataPoint(ts, scaled));
    }

    public synchronized void saveData(){
        AsyncExecutor.create().execute(new AsyncExecutor.RunnableEx() {
            @Override
            public void run() throws Exception {
                saveWatchAccelData();
                saveSensorInfo(watchAccelSensor, "WatchAccelerometer.meta.csv");
                zipEverything();
                EventBus.getDefault().post(new SaveFinishedEvent());
                Log.i("saved", "saved");
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

    private void saveWatchAccelData()  {
        File savedFile = new File(Environment.getExternalStorageDirectory() + "/sensing/data", "WatchAccelerometer.sensor.csv");
        if(!savedFile.getParentFile().exists()) savedFile.getParentFile().mkdirs();
        if(savedFile.exists()) savedFile.delete();
        if(watchAccelData.size() <= 0) return;
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(savedFile, false));
            writer.write("HEADER_TIME_STAMP,X,Y,Z");
            writer.write(System.lineSeparator());
            int count = 0;
            int previous = 0;
            int current;
            for(DataPoint datapoint : watchAccelData){
                count++;
                writer.write(datapoint.toString());
                writer.write(System.lineSeparator());
                if((current = Math.round(count / (float) watchAccelData.size() * 100)) != previous){
                    previous = current;
                    EventBus.getDefault().post(new SaveProgressEvent(current));
                }
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
