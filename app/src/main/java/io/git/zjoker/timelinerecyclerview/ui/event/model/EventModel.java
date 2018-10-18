package io.git.zjoker.timelinerecyclerview.ui.event.model;

public class EventModel {
    public static final int STATUS_NORMAL = 0;
    public static final int STATUS_EDITING = 1;
//    public static final int STATUS_SCALING = 2;


    public long timeStart;
    public long timeEnd;
    public int status = STATUS_NORMAL;


    public EventModel(long timeStart, long timeEnd) {
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
    }

    public EventModel(long timeStart, long timeEnd, int status) {
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.status = status;
    }


    public void changeToNormal() {
        this.status = STATUS_NORMAL;
    }
}
