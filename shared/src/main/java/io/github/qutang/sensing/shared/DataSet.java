package io.github.qutang.sensing.shared;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by tqshe on 3/19/2018.
 */

public class DataSet {
    private ArrayList<DataPoint> primaryBuffer;
    private ArrayList<DataPoint> secondaryBuffer;
    private String deviceId;
    private String deviceName;
    private String dataType;
    private String fileType;
    private String header;
    private boolean isSaving;
    private OnDataSaveProgressListener progressListener;
    private static final String TAG = "DataSet";

    public interface OnDataSaveProgressListener {
        public void updateProgress(int percent);
    }

    public DataSet(String deviceName, String deviceId, String dataType, String fileType, String header){
        primaryBuffer = new ArrayList<>();
        secondaryBuffer = new ArrayList<>();
        isSaving = false;
        this.deviceName = deviceName;
        this.deviceId = deviceId;
        this.dataType = dataType;
        this.fileType = fileType;
        this.header = header;
    }

    public void registerProgressListener(OnDataSaveProgressListener listener){
        progressListener = listener;
    }

    public boolean isProgressListenerRegistered(){
        return progressListener != null;
    }

    public void reset(){
        this.primaryBuffer.clear();
        this.secondaryBuffer.clear();
        this.isSaving = false;
    }

    public boolean isSaving(){
        return isSaving;
    }

    public void startSaving(){
        this.isSaving = true;
    }

    public void stopSaving(){
        this.isSaving = false;
    }

    public synchronized void addDataPoint(DataPoint sample){
        if(isSaving){
            secondaryBuffer.add(sample);
        }else{
            if(!secondaryBuffer.isEmpty()){
                if(primaryBuffer.isEmpty()) {
                    System.out.println("The secondary buffer has some data: " + secondaryBuffer.size() + ", copy to the primary buffer");
                    for (DataPoint data : secondaryBuffer) {
                        primaryBuffer.add(data);
                    }
                    secondaryBuffer.clear();
                }else{
                    System.out.println("The secondary buffer and primary buffer both have some data, something is wrong");
                }
            }
            primaryBuffer.add(sample);
        }
    }

    public void saveData(String rootFolder) throws IOException {
        System.out.println("Start writing data");
        startSaving();
        File folder = new File(rootFolder);
        if(!folder.exists()){
            folder.mkdirs();
        }
        if(primaryBuffer.size() <= 0) return;
        BufferedWriter writer = null;
        int count = 0;
        int previous = 0;
        int current;
        String lastHour = "";
        String filename;
        File filepath;
        for(DataPoint datapoint : primaryBuffer){
            filename = this.deviceName + "-" + this.dataType + "-NA." + this.deviceId + "-" + this.dataType + "." + datapoint.toTimestampString() + "." + this.fileType + ".csv";
            filepath = new File(folder + "/" + filename);
            if(!lastHour.equals(datapoint.toHourString())){
                lastHour = datapoint.toHourString();
                if(filepath.exists()){
                    writer = new BufferedWriter(new FileWriter(filepath, true));
                }else {
                    writer = new BufferedWriter(new FileWriter(filepath, false));
                    writer.write(this.header);
                    writer.write(System.lineSeparator());
                }
            }
            if(writer == null){
                writer = new BufferedWriter(new FileWriter(filepath, false));
                writer.write(this.header);
                writer.write(System.lineSeparator());
            }
            count++;
            writer.write(datapoint.toString());
            writer.write(System.lineSeparator());
            if((current = Math.round(count / (float)primaryBuffer.size() * 100)) != previous){
                previous = current;
                if(progressListener != null) {
                    progressListener.updateProgress(current);
                }
            }
        }
        writer.flush();
        writer.close();
        primaryBuffer.clear();
        System.out.println("Finish writing data");
        stopSaving();
    }
}
