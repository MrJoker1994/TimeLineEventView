package io.git.zjoker.timelineeventview.ui.event.util;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import io.git.zjoker.timelineeventview.ui.event.model.Event;
import io.git.zjoker.timelineeventview.ui.event.model.EventNode;
import io.git.zjoker.timelineeventview.ui.widget.TimeLineEventView;
import io.git.zjoker.timelineeventview.util.ViewUtil;

import static io.git.zjoker.timelineeventview.ui.event.model.Event.STATUS_EDITING;
import static io.git.zjoker.timelineeventview.ui.event.model.Event.STATUS_NORMAL;
import static io.git.zjoker.timelineeventview.ui.event.model.Event.STATUS_SCALING_TOP;

public class EventHelper {
    private List<Event> events;
    private WeakReference<TimeLineEventView> timeLineEventViewWR;
    private Paint eventSolidP;
    private Paint eventEditP;
    private Paint eventDragHandlerP;
    private GestureDetector gestureDetector;
    public static final long DEFAULT_EVENT_TIME_TAKEN = 50 * 60 * 1000;

    private int editingPostion = -1;
    private float dragHandlerRadius;
    private EventAdjustListener eventAdjustListener;
    private boolean hasEventUnderTouch;

    private float touchMoveDistance;
    private float lastTouchY;

    private ValueAnimator scrollAnimator;
    private EventNode eventNode;

    public EventHelper() {
        this.events = new ArrayList<>();
        eventSolidP = new Paint();
        eventSolidP.setStyle(Paint.Style.FILL);
        eventSolidP.setColor(Color.parseColor("#AA9999AA"));

        eventEditP = new Paint();
        eventEditP.setStyle(Paint.Style.FILL);
        eventEditP.setColor(Color.parseColor("#FFAAAAFF"));

        eventDragHandlerP = new Paint();
        eventDragHandlerP.setStyle(Paint.Style.STROKE);
        eventDragHandlerP.setStrokeWidth(ViewUtil.dpToPx(2));
        eventDragHandlerP.setColor(Color.parseColor("#FFAAAAFF"));

        dragHandlerRadius = ViewUtil.dpToPx(15);
    }

    public void setEventAdjustListener(EventAdjustListener eventAdjustListener) {
        this.eventAdjustListener = eventAdjustListener;
    }

    private EventNode buildEventTree() {
        List<Event> tmpEvent = new ArrayList<>(events);
        Collections.sort(tmpEvent, new Comparator<Event>() {
            @Override
            public int compare(Event o1, Event o2) {
                return (int) (o1.timeStart - o2.timeStart);
            }
        });

        EventNode rootNode = new EventNode();
        if (tmpEvent.size() == 0) {

        } else if (tmpEvent.size() == 1) {
            rootNode.appendChildNode(new EventNode(tmpEvent.get(0), 1));
        } else {
            List<EventNode> nodes = new ArrayList<>(tmpEvent.size());
            for (int i = 0; i < tmpEvent.size(); i++) {
                nodes.add(new EventNode(tmpEvent.get(i), 1));
            }

            for (int i = 0; i < nodes.size(); i++) {
                EventNode node1 = nodes.get(i);//正在找寻parent的node
                RectF rect1 = getV().getRectOnTimeLine(node1.event.timeStart, node1.event.timeTaken);
                int maxLevel = 0;
                EventNode parentNode = null;
                boolean hasSameNode = false;
                for (int j = 0; j < i; j++) {
                    EventNode node2 = nodes.get(j);//前面已经构建好的node
                    RectF rect2 = getV().getRectOnTimeLine(node2.event.timeStart, node2.event.timeTaken);
                    if (rect1.equals(rect2)) {
                        hasSameNode = true;
                        node2.appendSameNode(node1);
                        break;
                    } else if (RectF.intersects(rect1, rect2)) {
                        if (node2.level > maxLevel) {
                            maxLevel = node2.level;
                            parentNode = node2;
                        }
                    }
                }
                if (parentNode != null) {
                    parentNode.appendChildNode(node1);
                    Log.d("ParentNode", parentNode.childNodes.toString());
                } else if (!hasSameNode) {
                    rootNode.appendChildNode(node1);
                }

            }
        }
        Log.d("EventNode", rootNode.toString());
        return rootNode;
    }

    private void buildEventTree(EventNode node, Event event, List<Event> events) {
        Iterator<Event> iterator = events.iterator();
        RectF parentRect = getV().getRectOnTimeLine(event.timeStart, event.timeTaken);

        while (iterator.hasNext()) {
            Event childEvent = iterator.next();
            RectF childRect = getV().getRectOnTimeLine(childEvent.timeStart, childEvent.timeTaken);
            if (parentRect.equals(childRect)) {//一毛一样
                iterator.remove();
                EventNode childNode = new EventNode();
                childNode.event = childEvent;
                childNode.level = node.level;
                node.appendSameNode(childNode);
            } else if (RectF.intersects(parentRect, childRect)) {//有重叠
                iterator.remove();
                EventNode childNode = new EventNode();
                childNode.event = childEvent;
                childNode.level = node.level + 1;
                node.appendChildNode(childNode);
                buildEventTree(childNode, childEvent, new LinkedList<>(events));
            } else {//没有重叠 往上追述找到parent
//                    EventNode curParent = node.parentNode;
//                    EventNode childNode = new EventNode();
//                    childNode.event = childEvent;
//                    while (curParent != null) {
//                        if (curParent.event == null || RectF.intersects(getV().getRectOnTimeLine(curParent.event.timeStart, curParent.event.timeTaken), childRect)) {
//                            iterator.remove();
//                            childNode.level = curParent.level + 1;
//                            curParent.appendChildNode(childNode);
//                            break;
//                        }
//                        curParent = curParent.parentNode;
//                    }
//                    buildEventTree(childNode, childEvent, new LinkedList<>(events));
            }
        }
    }

    public void attach(TimeLineEventView timeLineEventView) {
        timeLineEventViewWR = new WeakReference<>(timeLineEventView);
        gestureDetector = new GestureDetector(timeLineEventView.getContext(), new GestureDetector.SimpleOnGestureListener() {

            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
                float touchX = e.getX();
                float touchY = getYWithScroll(e.getY());

                if (getEventEditing() == null) {
                    Event eventUnderTouch = getEventUnderTouch(touchX, touchY);
                    if (eventUnderTouch == null) {
                        eventUnderTouch = createEvent(e.getY());
                        eventUnderTouch.changeToEdit();
                        events.add(eventUnderTouch);
                    } else {
                        eventUnderTouch.changeToEdit();
                    }
                    hasEventUnderTouch = true;
                    if (eventAdjustListener != null) {
                        eventAdjustListener.onEventAdjusting(eventUnderTouch.timeStart);
                    }
                    invalidate();
                }
            }

            @Override
            public boolean onDown(MotionEvent e) {
                float touchX = e.getX();
                float touchY = getYWithScroll(e.getY());
                Event eventEditing = getEventEditing();
                if (eventEditing != null) {
                    if (isTopScalerUnderTouch(eventEditing, touchX, touchY)) {
                        eventEditing.changeToScaleTop();
                        hasEventUnderTouch = true;
                    } else if (isBottomScalerUnderTouch(eventEditing, touchX, touchY)) {
                        eventEditing.changeToScaleBottom();
                        hasEventUnderTouch = true;
                    } else if (isEventUnderTouch(eventEditing, touchX, touchY)) {
                        eventEditing.changeToEdit();
                        hasEventUnderTouch = true;
                    } else {
                        hasEventUnderTouch = false;
                    }
                } else {
                    hasEventUnderTouch = false;
                }
                return hasEventUnderTouch;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                boolean hasEditingEvent = getEventEditing() != null;
                if (hasEditingEvent && !hasEventUnderTouch) {
                    resetEventStatus();
                    if (eventAdjustListener != null) {
                        eventAdjustListener.onEventAdjustEnd();
                    }
                    invalidate();
                }
                return super.onSingleTapUp(e);
            }
        });
    }

    private float getYWithScroll(float touchY) {
        return touchY + getV().getScrollY();
    }

    private boolean isEventUnderTouch(Event eventModel, float touchX, float touchY) {
        RectF rectF = getV().getRectOnTimeLine(eventModel.timeStart, eventModel.timeTaken);
        return rectF.contains(touchX, touchY);
    }

    private void invalidate() {
        getV().invalidate();
    }

    private Event getEventUnderTouch(float touchX, float touchY) {
        for (int i = 0; i < events.size(); i++) {
            Event eventModel = events.get(i);
            if (isEventUnderTouch(eventModel, touchX, touchY)) {
                return eventModel;
            }
        }
        return null;
    }

    private boolean isTopScalerUnderTouch(Event editingEvent, float touchX, float touchY) {
        RectF rectF = getV().getRectOnTimeLine(editingEvent.timeStart, editingEvent.timeTaken);

        PointF topDragHandlerPoint = getTopScallerPoint(rectF);
        return getScalerRectF(topDragHandlerPoint.x, topDragHandlerPoint.y).contains(touchX, touchY);
    }

    private boolean isBottomScalerUnderTouch(Event editingEvent, float touchX, float touchY) {
        RectF rectF = getV().getRectOnTimeLine(editingEvent.timeStart, editingEvent.timeTaken);
        PointF topDragHandlerPoint = getBottomScallerPoint(rectF);
        return getScalerRectF(topDragHandlerPoint.x, topDragHandlerPoint.y).contains(touchX, touchY);
    }

    private boolean isInRect(RectF rectF, float x, float y) {
        return rectF.contains(x, getYWithScroll(y));
    }

    private RectF getScalerRectF(float x, float y) {
        return new RectF(x - dragHandlerRadius, y - dragHandlerRadius, x + dragHandlerRadius, y + dragHandlerRadius);
    }


    private Event createEvent(float touchY) {
        long timeStart = getV().getTimeByOffsetY(touchY);
        return new Event(timeStart, DEFAULT_EVENT_TIME_TAKEN, STATUS_EDITING);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        gestureDetector.onTouchEvent(motionEvent);
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchY = motionEvent.getY();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                stopScroll();
                if (hasEventUnderTouch && eventAdjustListener != null) {
                    eventAdjustListener.onEventAdjustEnd();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                touchMoveDistance = motionEvent.getY() - lastTouchY;
                lastTouchY = motionEvent.getY();
                if (hasEventUnderTouch) {
                    if (!checkScroll(lastTouchY)) {
                        checkEditEvent(motionEvent.getX(), lastTouchY, touchMoveDistance);
                    }
                    return true;
                }
                break;
        }
        return hasEventUnderTouch;
    }

    private boolean checkEditEvent(float touchX, float touchY, float moveDistanceY) {
        Event eventEditing = getEventEditing();
        if (moveDistanceY != 0 && eventEditing != null) {
            long timeAdjust = getV().getTimeByDistance(moveDistanceY);
            Log.d("checkEditEvent", timeAdjust + "--" + moveDistanceY);
            if (eventEditing.status == STATUS_EDITING) {
                eventEditing.moveBy(timeAdjust);
            } else if (eventEditing.status == STATUS_SCALING_TOP) {
                eventEditing.scaleTopBy(timeAdjust);
            } else {
                eventEditing.scaleBottomBy(timeAdjust);
            }

            invalidate();
            if (eventAdjustListener != null) {
                eventAdjustListener.onEventAdjusting(getV().getTimeByOffsetY(touchY));
            }
            return true;
        }
        return false;
    }

    private boolean checkScroll(float touchY) {
        int adjustSpace = getV().getHeight() / 8;
        if (touchY < adjustSpace && touchMoveDistance <= 0 && getV().canScroll(false)) {
            startScroll(false);
        } else if (touchY > getV().getHeight() - adjustSpace && touchMoveDistance >= 0 && getV().canScroll(true)) {
            startScroll(true);
            return true;
        } else {
            stopScroll();
        }
        return false;
    }

    private void startScroll(boolean isScrollUp) {
        if (scrollAnimator != null && scrollAnimator.isRunning()) {
            return;
        }

        int from;
        int target;
        if (isScrollUp) {
            from = getV().getScrollY();
            target = getV().getTotalHeight() - getV().getHeight();
        } else {
            from = getV().getScrollY();
            target = 0;
        }
        scrollAnimator = ObjectAnimator.ofInt(from, target).setDuration(Math.abs(target - from));
        scrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int scrollTo = (int) animation.getAnimatedValue();
                if (eventAdjustListener != null) {
                    eventAdjustListener.onEventAdjustWithScroll(scrollTo);
                }
                checkEditEvent(0, lastTouchY, touchMoveDistance);
            }
        });
        scrollAnimator.start();
    }

    private void stopScroll() {
        if (scrollAnimator != null) {
            scrollAnimator.cancel();
        }
    }

    private Event getEventEditing() {
        for (int i = 0; i < events.size(); i++) {
            Event eventModel = events.get(i);
            if (eventModel.status != STATUS_NORMAL) {
                return eventModel;
            }
        }
        return null;
    }

    public void draw(Canvas canvas) {
        EventNode eventNode = buildEventTree();
        if (eventNode.childNodes.size() == 0) {
            return;
        }
        drawEvents(eventNode.childNodes, canvas);
    }

    private void resetEventStatus() {
        for (int i = 0; i < events.size(); i++) {
            events.get(i).changeToNormal();
        }
    }

    private void drawEvents(List<EventNode> nodes, Canvas canvas) {
        for (int i = 0; i < nodes.size(); i++) {
            EventNode eventNode = nodes.get(i);
            drawSingleEvent(eventNode, canvas);
            drawEvents(eventNode.childNodes, canvas);
        }
    }

    private void drawSingleEvent(EventNode node, Canvas canvas) {
        RectF rectF = getV().getRectOnTimeLine(node.event.timeStart, node.event.timeTaken);
        if (node.event.status != STATUS_NORMAL) {
            drawEventOnEdit(canvas, node.event, rectF);
            if (!node.sameNodes.isEmpty()) {
                float rectWidth = rectF.width() / (node.sameNodes.size());
                RectF sameNodeRect = new RectF(rectF.left, rectF.top, rectWidth, rectF.top);
                for (int i = 0; i < node.sameNodes.size(); i++) {
                    sameNodeRect.offsetTo(i * rectWidth, rectF.top);
                    drawEventOnNormal(canvas, sameNodeRect);
                }
            }
        } else {
            rectF.left += (node.level - 1) * 10;
            if (node.sameNodes.isEmpty()) {
                drawEventOnNormal(canvas, rectF);
            } else {
                float rectWidth = rectF.width() / (node.sameNodes.size() + 1);
                RectF sameNodeRect = new RectF(rectF.left, rectF.top, rectF.left + rectWidth, rectF.bottom);
                drawEventOnNormal(canvas, sameNodeRect);
                for (int i = 0; i < node.sameNodes.size(); i++) {
                    sameNodeRect.offset(rectWidth, 0);
                    drawEventOnNormal(canvas, sameNodeRect);
                }
            }
        }
    }

    private void drawEventOnNormal(Canvas canvas, RectF rectF) {
        canvas.drawRect(rectF, eventSolidP);
        canvas.drawRect(rectF.left, rectF.top, rectF.left + ViewUtil.dpToPx(3), rectF.bottom, eventEditP);
    }

    private void drawEventOnEdit(Canvas canvas, Event eventModel, RectF rectF) {
        canvas.drawRect(rectF, eventEditP);
        PointF topDragHandlerPoint = getTopScallerPoint(rectF);
        canvas.drawCircle(topDragHandlerPoint.x, topDragHandlerPoint.y, dragHandlerRadius, eventDragHandlerP);

        PointF bottomDragHandlerPoint = getBottomScallerPoint(rectF);
        canvas.drawCircle(bottomDragHandlerPoint.x, bottomDragHandlerPoint.y, dragHandlerRadius, eventDragHandlerP);
    }

    private PointF getTopScallerPoint(RectF eventRectF) {
        float size = 2 * dragHandlerRadius;
        float topXOffset = eventRectF.right - size;
        float topYOffset = eventRectF.top;
        return new PointF(topXOffset, topYOffset);
    }

    private PointF getBottomScallerPoint(RectF eventRectF) {
        float size = 2 * dragHandlerRadius;
        float bottomXOffset = eventRectF.left + size;
        float bottomYOffset = eventRectF.bottom;
        return new PointF(bottomXOffset, bottomYOffset);
    }

    public TimeLineEventView getV() {
        return timeLineEventViewWR.get();
    }

    public interface EventAdjustListener {
        void onEventAdjusting(long timeAdjust);

        void onEventAdjustEnd();

        void onEventAdjustWithScroll(int scrollTo);
    }

    public void dettach() {
        if (scrollAnimator != null) {
            scrollAnimator.cancel();
        }
        if (timeLineEventViewWR != null) {
            timeLineEventViewWR.clear();
        }
    }
}
