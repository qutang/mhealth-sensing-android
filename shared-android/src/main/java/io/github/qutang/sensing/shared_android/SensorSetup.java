package io.github.qutang.sensing.shared_android;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;

import java.util.ArrayList;

import io.github.qutang.sensing.shared.DataSet;

/**
 * Created by tqshe on 3/23/2018.
 */

public class SensorSetup {
    public String info;
    public int delay;
    public String name;
    public boolean isRecording;
    public boolean isSaving;
    private Context mContext;

    public SensorSetup(Context mContext){
        this.info = "";
        this.delay = SensorManager.SENSOR_DELAY_GAME;
        this.name = "";
        this.isRecording = false;
        this.isSaving = false;
        this.mContext = mContext;
    }

    protected String getIMEI(Context mContext) {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return "UnknownIMEI";
        }else{
            TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            return telephonyManager.getDeviceId();
        }
    }

    protected String getModelName() {
        return android.os.Build.MODEL.replace(" ", "");
    }
}
