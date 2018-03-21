package io.github.qutang.sensing.shared;

import java.text.SimpleDateFormat;
import java.util.Locale;

import io.github.qutang.sensing.shared.Utility;

/**
 * Created by Qu on 9/29/2016.
 */

public class DataPoint {
    public final long ts;
    public final float[] values;
    public final String name;

    public DataPoint(long ts, float[] values, String name){
        this.ts = ts;
        this.values = values;
        this.name = name;
    }

    @Override
    public synchronized String toString(){
        String str = "";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        str += formatter.format(ts);
        for(float value : values){
            str += "," + String.format("%.3f", value);
        }
        return str;
    }

    public synchronized String toTimestampString() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH");
        String str = formatter.format(ts) + "-00-00-000";
        String tz = Utility.formatCurrentTimeZone();
        return str + "-" + tz;
    }

    public synchronized String toHourString() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH");
        String str = formatter.format(ts);
        return str;
    }
}
