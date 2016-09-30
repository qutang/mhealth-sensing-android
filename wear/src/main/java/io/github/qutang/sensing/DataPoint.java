package io.github.qutang.sensing;

import java.text.SimpleDateFormat;

/**
 * Created by Qu on 9/29/2016.
 */

public class DataPoint {
    public long ts;
    public float[] values;

    public static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public DataPoint(long ts, float[] values){
        this.ts = ts;
        this.values = values;
    }

    @Override
    public synchronized String toString(){
        String str = "";
        str += formatter.format(ts);
        for(float value : values){
            str += "," + String.format("%.3f", value);
        }
        return str;
    }
}
