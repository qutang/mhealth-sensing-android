package io.github.qutang.sensing.shared_android;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.Log;

import java.io.File;

import io.github.qutang.sensing.shared.FileWalker;

public class MediaScanner
        implements MediaScannerConnection.MediaScannerConnectionClient {

    private static final String TAG = "MediaScanner";

    private String mFilename;
    private String mMimetype;
    private File mFile;
    private FileWalker mWalker;
    private MediaScannerConnection mConn;

    public MediaScanner
            (Context ctx, File file, String mimetype) {
        this.mFilename = file.getAbsolutePath();
        this.mFile = file;
        this.mWalker = new FileWalker(new FileWalker.FileWalkerCallback() {
            @Override
            public void onFileWalkerDetected(File f) {
                mConn.scanFile(f.getAbsolutePath(), mMimetype);
            }
        });
        mConn = new MediaScannerConnection(ctx, this);
    }

    public void scan(){
        mConn.connect();
    }

    @Override
    public void onMediaScannerConnected() {
        Log.i(TAG, "Media Scanner connected, start scanning " + mFilename);
        if(mFile.isDirectory()){
            mWalker.walk(mFile);
        }
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        Log.i(TAG, "Media Scanner finished:" + path);
        mConn.disconnect();
    }
}
