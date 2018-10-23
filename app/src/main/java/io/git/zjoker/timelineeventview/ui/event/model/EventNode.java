package io.git.zjoker.timelineeventview.ui.event.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class EventNode {
    public EventNode parentNode;
    public Event event;
    public int level;
    public List<EventNode> childNodes;
    public List<EventNode> sameNodes;

    public EventNode() {
        childNodes = new LinkedList<>();
        sameNodes = new LinkedList<>();
    }

    public void appendChildNode(EventNode eventNode) {
        childNodes.add(eventNode);
        eventNode.parentNode = this;
    }

    public void appendSameNode(EventNode eventNode) {
        sameNodes.add(eventNode);
    }

    public int getDirectChildCount() {
        return childNodes.size();
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
