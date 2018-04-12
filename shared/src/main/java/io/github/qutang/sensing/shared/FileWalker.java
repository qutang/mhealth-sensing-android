package io.github.qutang.sensing.shared;

import java.io.File;

public class FileWalker {

    private FileWalkerCallback mCallback;

    public FileWalker(FileWalkerCallback callback){
        mCallback = callback;
    }

    public interface FileWalkerCallback {
        public void onFileWalkerDetected(File f);
    }

    public void walk(File root) {

        File[] list = root.listFiles();

        for (File f : list) {
            if (f.isDirectory()) {
                walk(f);
            }
            else {
                mCallback.onFileWalkerDetected(f);
            }
        }
    }
}
