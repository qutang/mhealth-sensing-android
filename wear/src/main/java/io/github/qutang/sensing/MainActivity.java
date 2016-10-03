package io.github.qutang.sensing;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.view.WatchViewStub;
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

    private static final int WRITE_PERMISSION = 1;

    @BindView(R.id.btn_watch_record)
    Button recordButton;

    @BindView(R.id.watch_title) TextView title;
    @BindView(R.id.watch_content) TextView content;

    private final ApplicationState state = ApplicationState.getState();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                ButterKnife.bind(MainActivity.this,stub);
            }
        });
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStart() {
        EventBus.getDefault().register(this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @OnClick(R.id.btn_watch_record)
    public void record(View view){
        if(state.isRecording){
            stopService(new Intent(MainActivity.this, SensingService.class));
            recordButton.setText("Start");
            save();
            title.setText("Saving data...");
        }else{
            ApplicationState.getState().watchAccelData.clear();
            ApplicationState.getState().watchSamplingRateData.clear();
            startService(new Intent(MainActivity.this, SensingService.class));
            recordButton.setText("Stop");
            title.setText("Recording...");
            content.setText("00:00:00" + ", 0 Hz");
        }
        state.setRecording(!state.isRecording);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSaveFinished(SaveFinishedEvent event){
        title.setText("Ready to record");
        Toast.makeText(this, "Data saved", Toast.LENGTH_SHORT);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSaveProgres(SaveProgressEvent event){
        title.setText("Saving " + event.name + ": " + event.value + "%");
    }

    private void save(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_PERMISSION);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }else{
            state.saveData();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case WRITE_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    state.saveData();
                } else {
                    title.setText("Ready to record");
                    Toast.makeText(this, "Can't save data", Toast.LENGTH_SHORT);
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateSamplingRate(ContentUpdateEvent event){
        content.setText(event.content);
    }
}
