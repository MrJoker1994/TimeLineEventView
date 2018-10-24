package io.git.zjoker.timelineeventview.ui.event.model;

public class Event {
    public static final int STATUS_NORMAL = 0;
    public static final int STATUS_EDITING = 1;
    public static final int STATUS_SCALING_TOP = 2;
    public static final int STATUS_SCALING_BOTTOM = 3;


    public long timeStart;
    public long timeTaken;
    public int status = STATUS_NORMAL;

    public Event(long timeStart, long timeTaken) {
        this.timeStart = timeStart;
        this.timeTaken = timeTaken;
    }

    public Event(long timeStart, long timeTaken, int status) {
        this.timeStart = timeStart;
        this.timeTaken = timeTaken;
        this.status = status;
    }


    public void changeToNormal() {
        this.status = STATUS_NORMAL;
    }

    public void changeToEdit() {
        this.status = STATUS_EDITING;
    }

    public void changeToScaleTop() {
        this.status = STATUS_SCALING_TOP;
    }

    public void changeToScaleBottom() {
        this.status = STATUS_SCALING_BOTTOM;
    }

    public void moveBy(long timeAdjust) {
        this.timeStart += timeAdjust;
    }

    public void scaleTopBy(long timeAdjust) {
        moveBy(timeAdjust);
        scaleBottomBy(-timeAdjust);
    }

    public void scaleBottomBy(long timeAdjust) {
        timeTaken += timeAdjust;
    }
}
