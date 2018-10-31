package io.git.zjoker.timelineeventview.ui.event.model;

public class EventCache {
    public static final int STATUS_EDITING = 1;
    public static final int STATUS_SCALING_TOP = 2;
    public static final int STATUS_SCALING_BOTTOM = 3;

    public Event originEvent;
    public Event newEvent;
    public float originX;
    public float newX;
    public int status = STATUS_EDITING;

    private EventCache(Event event, int status, float originX) {
        this.originEvent = event;
        this.newEvent = originEvent.clone();
        this.status = status;
        this.originX = originX;
        newX = originX;
    }

    public static EventCache build(Event event, float touchX) {
        return new EventCache(event, STATUS_EDITING, touchX);
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

    public void moveBy(float moveX, long timeAdjust) {
        newX += moveX;
        newEvent.timeStart += timeAdjust;
    }

    public void scaleTopBy(long timeAdjust) {
        moveBy(0, timeAdjust);
        scaleBottomBy(-timeAdjust);
    }

    public void scaleBottomBy(long timeAdjust) {
        newEvent.timeTaken += timeAdjust;
    }

    public void refreshOrigin() {
        originEvent.timeStart = newEvent.timeStart;
        originEvent.timeTaken = newEvent.timeTaken;
    }

    public void reset() {
        newX = originX;
    }
}
