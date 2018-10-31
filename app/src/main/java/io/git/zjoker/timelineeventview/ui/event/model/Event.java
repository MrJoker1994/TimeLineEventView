package io.git.zjoker.timelineeventview.ui.event.model;

public class Event implements Cloneable {
    public long timeStart;
    public long timeTaken;
    public CharSequence text = "sdkflsjdlfjskldjfklsdjfklsdjfkldsjklf我岁分开祭祀的快乐佛教四大克利夫兰科技";

    public Event(long timeStart, long timeTaken) {
        this.timeStart = timeStart;
        this.timeTaken = timeTaken;
    }

    public long getTimeEnd() {
        return timeStart + timeTaken;
    }

    @Override
    public Event clone() {
        Event event = new Event(timeStart,timeTaken);
        event.text = text;
        return event;
    }
}
