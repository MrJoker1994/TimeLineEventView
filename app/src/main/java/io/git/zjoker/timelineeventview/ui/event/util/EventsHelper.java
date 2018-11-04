package io.git.zjoker.timelineeventview.ui.event.util;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import io.git.zjoker.timelineeventview.ui.event.model.Event;
import io.git.zjoker.timelineeventview.ui.event.model.EventCache;
import io.git.zjoker.timelineeventview.ui.event.model.EventNode;
import io.git.zjoker.timelineeventview.ui.widget.TimeLineEventView;
import io.git.zjoker.timelineeventview.util.ViewUtil;

import static io.git.zjoker.timelineeventview.ui.event.model.EventCache.STATUS_EDITING;
import static io.git.zjoker.timelineeventview.ui.event.model.EventCache.STATUS_SCALING_TOP;

public class EventsHelper {
    private WeakReference<TimeLineEventView> timeLineEventViewWR;

    private Paint eventSolidP;
    private TextPaint eventContentP;

    private float eventPadding;
    private float eventLevelWidth;//每一级缩进的宽度\

    @ColorInt
    public static final int NORMAL_SOLID_COLOR = Color.parseColor("#AA9999AA");
    @ColorInt
    public static final int EDIT_SOLID_COLOR = Color.parseColor("#FFAAAAFF");
    @ColorInt
    public static final int SCALLER_SOLID_COLOR = Color.WHITE;
    @ColorInt
    public static final int SCALLER_STROKE_COLOR = Color.parseColor("#FFAAAAFF");

    public EventsHelper() {
        eventSolidP = new Paint();
        eventSolidP.setStyle(Paint.Style.FILL);
        eventSolidP.setColor(NORMAL_SOLID_COLOR);

        eventContentP = new TextPaint();
        eventContentP.setColor(Color.WHITE);
        eventContentP.setTextSize(ViewUtil.spToPx(15));
        eventContentP.setStyle(Paint.Style.FILL);

        eventPadding = ViewUtil.dpToPx(8);
        eventLevelWidth = ViewUtil.dpToPx(5);
    }

    private EventNode buildEventTree() {
        List<Event> tmpEvent = new ArrayList<>(getV().getEvents());
        Collections.sort(tmpEvent, eventComparator);

        EventNode rootNode = new EventNode();
        if (tmpEvent.size() == 0) {
            return rootNode;
        }

        if (tmpEvent.size() == 1) {
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
                for (int j = 0; j < i; j++) {//和i之前的node挨个做对比，找出自己的所在位置
                    EventNode node2 = nodes.get(j);//前面已经构建好的node
                    RectF rect2 = getV().getRectOnTimeLine(node2.event.timeStart, node2.event.timeTaken);
                    if (rect1.equals(rect2)) {//完全重叠
                        hasSameNode = true;
                        node2.appendSameNode(node1);
                        break;
                    } else if (RectF.intersects(rect1, rect2)) {//有重叠
                        if (node2.level > maxLevel) {//找到深度最大的那个作为父节点
                            maxLevel = node2.level;
                            parentNode = node2;
                        }
                    }
                }
                if (parentNode != null) {
                    parentNode.appendChildNode(node1);
                    Log.d("ParentNode", parentNode.childNodes.toString());
                } else if (!hasSameNode) {//如果没有找到自己的位置，就属于是第一级
                    rootNode.appendChildNode(node1);
                }

            }
        }
        Log.d("EventNode", rootNode.toString());
        return rootNode;
    }

    public void attach(TimeLineEventView timeLineEventView) {
        timeLineEventViewWR = new WeakReference<>(timeLineEventView);
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

    public void draw(Canvas canvas) {
        EventNode eventNode = buildEventTree();
        if (eventNode.childNodes.size() == 0) {
            return;
        }
        drawEvents(eventNode.childNodes, canvas);
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
        rectF.left += (node.level - 1) * eventLevelWidth;
        if (node.sameNodes.isEmpty()) {
            drawEventOnNormal(canvas, node.event, rectF);
        } else {
            float rectWidth = rectF.width() / (node.sameNodes.size() + 1);
            RectF sameNodeRect = new RectF(rectF.left, rectF.top, rectF.left + rectWidth, rectF.bottom);
            drawEventOnNormal(canvas, node.event, sameNodeRect);
            for (int i = 0; i < node.sameNodes.size(); i++) {
                sameNodeRect.offset(rectWidth, 0);
                drawEventOnNormal(canvas, node.sameNodes.get(0).event, sameNodeRect);
            }
        }
    }

    private void drawEventOnNormal(Canvas canvas, Event event, RectF rectF) {
        canvas.drawRect(rectF, eventSolidP);
//        canvas.drawRect(rectF.left, rectF.top, rectF.left + ViewUtil.dpToPx(3), rectF.bottom, eventEditP);
        drawContent(canvas, event, rectF);
    }


    private void drawContent(Canvas canvas, Event event, RectF rectF) {
        int save = canvas.save();
        canvas.translate(rectF.left + eventPadding, rectF.top + eventPadding);
        int width = (int) (rectF.width() - 2 * eventPadding);
        StaticLayout myStaticLayout = new StaticLayout(event.text, 0, event.text.length(),
                eventContentP, width,
                Layout.Alignment.ALIGN_NORMAL,
                1.0f, 0.0f,
                false,
                null, width);
        myStaticLayout.draw(canvas);
        canvas.restoreToCount(save);
    }


    public TimeLineEventView getV() {
        return timeLineEventViewWR.get();
    }
//
//    public Event createAndAddNewEvent(float y) {
//        long timeStart = getCurTimeLineEventView().getTimeByOffsetY(touchY);
//        return new Event(timeStart, DEFAULT_EVENT_TIME_TAKEN);
//    }

    public void dettach() {
        if (timeLineEventViewWR != null) {
            timeLineEventViewWR.clear();
        }
    }

    private Comparator<Event> eventComparator = new Comparator<Event>() {
        @Override
        public int compare(Event o1, Event o2) {
            return (int) (o1.timeStart - o2.timeStart);
        }
    };
}
