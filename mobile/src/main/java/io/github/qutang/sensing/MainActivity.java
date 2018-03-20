package io.github.qutang.sensing;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.greenrobot.eventbus.util.AsyncExecutor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.button_record) FloatingActionButton recordButton;
    @BindView(R.id.button_save) FloatingActionButton saveButton;
    private ApplicationState state;
    private Snackbar notification;
    private Timer timer;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        context = this;
        state = ApplicationState.getState(context);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateSnackBar(SnackBarMessageEvent event){
        notification.setText(event.message);
        if(event.show){
            notification.show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @OnClick(R.id.button_record)
    public void record(View view){
        if(state.isRecording){ // before recording stop
            recordButton.setImageResource(R.drawable.ic_play_dark);
            stopService(new Intent(MainActivity.this, SensingService.class));
        }else{ // before recording start
            EventBus.getDefault().post(new ResetChartEvent());
            state.resetRecordingStatus();
//            state.phoneAccelData.clear();
//            state.isWriting = false;
//            state.phoneAccelAlterBuffer.clear();
//            state.phoneSamplingRateData.clear();
//            state.setElapsedSeconds(0);
            createSnackBarNotification(view);
            startService(new Intent(MainActivity.this, SensingService.class));
            recordButton.setImageResource(R.drawable.quantum_ic_stop_white_24);
        }
        state.setRecording(!state.isRecording);
    }

    private void createSnackBarNotification(View view){
        notification = Snackbar.make(view, "Recording: " + "00:00:00", Snackbar.LENGTH_INDEFINITE)
                .setAction("Action", null);
        notification.show();
    }

    @OnClick(R.id.button_save)
    public void shareFile(View view){
        File file = new File(Environment.getExternalStorageDirectory() + "/sensing", "phone.zip");
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String ext = file.getName().substring(file.getName().indexOf(".") + 1);
        String type = mime.getMimeTypeFromExtension(ext);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(type);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
