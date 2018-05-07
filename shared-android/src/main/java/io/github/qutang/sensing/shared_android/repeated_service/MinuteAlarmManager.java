package io.github.qutang.sensing.shared_android.repeated_service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

public class MinuteAlarmManager {
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;
    private Context mContext;
    private static final String TAG = "MinuteAlarmManager";

    public MinuteAlarmManager(Context context){
        alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        mContext = context;
    }

    public void start(Class serviceClass) {
        Log.i(TAG, "Start minute alarm for sensing");
        Intent serviceIntent = new Intent(mContext, serviceClass);
        serviceIntent.setAction("flush");
        Intent startServiceIntent = new Intent(mContext, serviceClass);
        startServiceIntent.setAction("start");
        alarmIntent = PendingIntent.getService(mContext, 0, serviceIntent, 0);
        mContext.startService(startServiceIntent);
//        alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                SystemClock.elapsedRealtime() + 60 * 1000, 60 * 1000, alarmIntent);
    }

    public void cancel() {
        if (alarmMgr!= null) {
            alarmMgr.cancel(alarmIntent);
        }
    }
}
