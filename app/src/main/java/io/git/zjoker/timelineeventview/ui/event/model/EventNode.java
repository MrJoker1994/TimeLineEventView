package io.git.zjoker.timelineeventview.ui.event.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class EventNode {
    public Event event;
    public int level;
    public List<EventNode> childNodes;
    public List<EventNode> sameNodes;

    public EventNode(Event event, int level) {
        this();
        this.event = event;
        this.level = level;
    }

    public EventNode() {
        childNodes = new LinkedList<>();
        sameNodes = new LinkedList<>();
    }

    public void appendChildNode(EventNode eventNode) {
        eventNode.level = level + 1;
        childNodes.add(eventNode);
    }

    public void appendSameNode(EventNode eventNode) {
        eventNode.level = level;
        sameNodes.add(eventNode);
    }

    @Override
    public String toString() {
        return "EventNode{" +
                "event=" + event +
                ", level=" + level +
                ", childNodes=" + childNodes +
                '}';
    }
}
