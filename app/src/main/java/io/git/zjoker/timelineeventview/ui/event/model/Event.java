package io.git.zjoker.timelineeventview.ui.event.model;

import java.util.Calendar;
import java.util.Objects;

public class Event implements Cloneable {
    public long id;
    public long timeStart;
    public long timeTaken;
    public CharSequence text = "sdkflsjdlfjskldjfklsdjfklsdjfkldsjklf我岁分开祭祀的快乐佛教四大克利夫兰科技";

    public Event(long timeStart, long timeTaken) {
        id = System.currentTimeMillis();
        this.timeStart = timeStart;
        this.timeTaken = timeTaken;
    }

    public long getTimeEnd() {
        return timeStart + timeTaken;
    }

    @Override
    public Event clone() {
        Event event = new Event(timeStart, timeTaken);
        event.id = id;
        event.text = text;
        return event;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return id == event.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


}
