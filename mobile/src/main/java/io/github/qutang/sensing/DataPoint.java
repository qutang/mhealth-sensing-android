package io.github.qutang.sensing;

import java.text.SimpleDateFormat;

/**
 * Created by Qu on 9/29/2016.
 */

public class DataPoint {
    public final long ts;
    public final float[] values;
    public final String name;

    public static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public DataPoint(long ts, float[] values, String name){
        this.ts = ts;
        this.values = values;
        this.name = name;
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
