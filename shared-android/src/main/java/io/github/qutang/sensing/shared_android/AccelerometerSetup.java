package io.github.qutang.sensing.shared_android;

import android.content.Context;

import io.github.qutang.sensing.shared.DataSet;

/**
 * Created by tqshe on 3/23/2018.
 */

public class AccelerometerSetup extends SensorSetup {
    public boolean enableRaw;
    public boolean enableSamplingRate;

    private DataSet rawData;
    private DataSet srData;

    private int saveDelay = 60 * 1000;

    public AccelerometerSetup(Context mContext){
        super(mContext);
        String modelName = getModelName();
        String imei = getIMEI(mContext);
        rawData = new DataSet(modelName, imei, "AccelerometerCalibrated", "sensor", "HEADER_TIME_STAMP,X,Y,Z");
        srData = new DataSet(modelName, imei, "AccelerometerSamplingRate", "feature", "HEADER_TIME_STAMP,SR");
    }

    public void registerRawListener(DataSet.OnDataSaveProgressListener listener){
        rawData.registerProgressListener(listener);
    }

    public void registerSRListener(DataSet.OnDataSaveProgressListener listener) {
        srData.registerProgressListener(listener);
    }
}
