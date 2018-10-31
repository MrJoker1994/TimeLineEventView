package io.git.zjoker.timelineeventview.ui.event.model;

public class EventCache {
    public static final int STATUS_EDITING = 1;
    public static final int STATUS_SCALING_TOP = 2;
    public static final int STATUS_SCALING_BOTTOM = 3;

    public Event originEvent;
    public Event newEvent;
    public int status = STATUS_EDITING;

    private EventCache(Event event, int status) {
        this.originEvent = event;
        this.newEvent = originEvent.clone();
        this.status = status;
    }

    public static EventCache build(Event event) {
        return new EventCache(event,STATUS_EDITING);
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
        newEvent.timeStart += timeAdjust;
    }

    public void scaleTopBy(long timeAdjust) {
        moveBy(timeAdjust);
        scaleBottomBy(-timeAdjust);
    }

    public void scaleBottomBy(long timeAdjust) {
        newEvent.timeTaken += timeAdjust;
    }

    public void refreshOrigin() {
        originEvent.timeStart = newEvent.timeStart;
        originEvent.timeTaken = newEvent.timeTaken;
    }
}
