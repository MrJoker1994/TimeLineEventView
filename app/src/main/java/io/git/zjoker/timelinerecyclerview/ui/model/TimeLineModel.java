package io.git.zjoker.timelinerecyclerview.ui.model;

public class TimeLineModel {
    public long timeStamp;
    public float yOffset;
    public String timeTip;

    public TimeLineModel(float yOffset, String timeTip) {
        this.yOffset = yOffset;
        this.timeTip = timeTip;
    }

    public TimeLineModel(long timeStamp, float yOffset) {
        this.timeStamp = timeStamp;
        this.yOffset = yOffset;
    }
}
