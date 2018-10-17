package io.git.zjoker.timelinerecyclerview.ui.event.model;

public class EventModel {
    public long timeStart;
    public long timeEnd;
    public boolean scalling;
    public boolean draing;


    public EventModel(long timeStart, long timeEnd) {
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
    }
}
