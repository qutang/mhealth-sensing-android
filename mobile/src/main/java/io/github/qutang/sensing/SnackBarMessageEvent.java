package io.github.qutang.sensing;

/**
 * Created by Qu on 9/29/2016.
 */
public class SnackBarMessageEvent {
    public final String message;
    public boolean show;

    public SnackBarMessageEvent(String message, boolean show) {
        this.message = message;
        this.show = show;
    }
}
