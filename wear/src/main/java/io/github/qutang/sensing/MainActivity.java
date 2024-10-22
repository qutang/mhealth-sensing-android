package io.github.qutang.sensing;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends Activity {

    @BindView(R.id.btn_watch_record)
    Button recordButton;

    @BindView(R.id.watch_title) TextView title;
    @BindView(R.id.watch_content) TextView content;

    private ApplicationState state;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                Log.i(TAG, "onLayoutInflatedListener");
                ButterKnife.bind(MainActivity.this,stub);
                Log.i(TAG, String.valueOf(state.isRecording));
                if(state.isRecording){
                    recordButton.setText("Stop");
                    title.setText("Recording...");
                }else{
                    recordButton.setText("Start");
                    title.setText("Ready to record");
                    content.setText("");
                }
            }
        });
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        state = ApplicationState.getState(this);

    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        EventBus.getDefault().register(this);
        super.onStart();
    }


    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @OnClick(R.id.btn_watch_record)
    public void record(View view){
        if(state.isRecording){
//            stopService(new Intent(MainActivity.this, SensingService.class));
            state.stopRecording();
            recordButton.setText("Start");
            title.setText("Saving data...");
        }else{
            state.resetRecordingStatus();
//            startService(new Intent(MainActivity.this, SensingService.class));
            state.startRecording();
            recordButton.setText("Stop");
            title.setText("Recording...");
            content.setText("00:00:00" + ", 0 Hz");
        }
//        state.setRecording(!state.isRecording);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateContent(ContentUpdateEvent event){
        content.setText(event.content);
    }
}
