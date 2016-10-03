package io.github.qutang.sensing;

/**
 * Created by Qu on 9/30/2016.
 */
public class SaveProgressEvent {
    public final int value;
    public final String name;
    public SaveProgressEvent(String name, int current) {
        value = current;
        this.name = name;
    }
}
