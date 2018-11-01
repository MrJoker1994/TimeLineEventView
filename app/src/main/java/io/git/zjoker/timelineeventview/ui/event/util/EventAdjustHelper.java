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

import io.git.zjoker.timelineeventview.ui.event.model.Event;
import io.git.zjoker.timelineeventview.ui.event.model.EventCache;
import io.git.zjoker.timelineeventview.ui.event.model.EventNode;
import io.git.zjoker.timelineeventview.ui.widget.TimeLineEventView;
import io.git.zjoker.timelineeventview.ui.widget.TimeLineEventViewPager;
import io.git.zjoker.timelineeventview.util.ViewUtil;

import static io.git.zjoker.timelineeventview.ui.event.model.EventCache.STATUS_EDITING;
import static io.git.zjoker.timelineeventview.ui.event.model.EventCache.STATUS_SCALING_TOP;

public class EventAdjustHelper {
    public static final long DEFAULT_EVENT_TIME_TAKEN = 50 * 60 * 1000;
    private WeakReference<TimeLineEventViewPager> timeLineEventViewWR;

    private Paint eventSolidP;
    private Paint eventEditP;
    private Paint eventDragHandlerP;
    private TextPaint eventContentP;
    private GestureDetector gestureDetector;

    private float scallerRadius;
    private float scallerPadding;
    private EventHelper.Callback eventAdjustListener;

    private float moveDistanceY;
    private float lastTouchY;
    private float lastTouchX;

    private float eventPadding;
    private float eventLevelWidth;//每一级缩进的宽度

    private EventCache eventEditingCache;

    private ValueAnimator scrollAnimator;

    @ColorInt
    public static final int NORMAL_SOLID_COLOR = Color.parseColor("#AA9999AA");
    @ColorInt
    public static final int EDIT_SOLID_COLOR = Color.parseColor("#FFAAAAFF");
    @ColorInt
    public static final int SCALLER_SOLID_COLOR = Color.WHITE;
    @ColorInt
    public static final int SCALLER_STROKE_COLOR = Color.parseColor("#FFAAAAFF");

    public EventAdjustHelper() {
        eventSolidP = new Paint();
        eventSolidP.setStyle(Paint.Style.FILL);
        eventSolidP.setColor(NORMAL_SOLID_COLOR);

        eventEditP = new Paint();
        eventEditP.setStyle(Paint.Style.FILL);
        eventEditP.setColor(EDIT_SOLID_COLOR);

        eventDragHandlerP = new Paint();
        eventDragHandlerP.setStyle(Paint.Style.FILL);
        eventDragHandlerP.setStrokeWidth(ViewUtil.dpToPx(2));
        eventDragHandlerP.setColor(SCALLER_SOLID_COLOR);


        eventContentP = new TextPaint();
        eventContentP.setColor(Color.WHITE);
        eventContentP.setTextSize(ViewUtil.spToPx(15));
        eventContentP.setStyle(Paint.Style.FILL);

        scallerRadius = ViewUtil.dpToPx(5);
        scallerPadding = ViewUtil.dpToPx(10);

        eventPadding = ViewUtil.dpToPx(8);
        eventLevelWidth = ViewUtil.dpToPx(5);
    }

    private boolean hasEventUnderTouch;

    public void setEventAdjustListener(EventHelper.Callback eventAdjustListener) {
        this.eventAdjustListener = eventAdjustListener;
    }


    public void attach(TimeLineEventViewPager viewPager) {
        timeLineEventViewWR = new WeakReference<>(viewPager);
        gestureDetector = new GestureDetector(viewPager.getContext(), new GestureDetector.SimpleOnGestureListener() {

            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
                float touchX = e.getX();
                float touchY = getYWithScroll(e.getY());

                if (eventEditingCache == null) {
                    Event eventUnderTouch = getCurTimeLineEventView().getEventUnderTouch(touchX, touchY);
                    if (eventUnderTouch == null) {
                        eventUnderTouch = createEvent(e.getY());
//                        events.add(eventUnderTouch);
                    }
                    float xOffset = getCurTimeLineEventView().getRectOnTimeLine(eventUnderTouch.timeStart, eventUnderTouch.timeTaken).left;
                    eventEditingCache = EventCache.build(eventUnderTouch, xOffset);

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
                if (eventEditingCache != null) {
                    if (isTopScalerUnderTouch(eventEditingCache.newEvent, touchX, touchY)) {
                        eventEditingCache.changeToScaleTop();
                        hasEventUnderTouch = true;
                    } else if (isBottomScalerUnderTouch(eventEditingCache.newEvent, touchX, touchY)) {
                        eventEditingCache.changeToScaleBottom();
                        hasEventUnderTouch = true;
                    } else if (isEventUnderTouch(eventEditingCache.newEvent, touchX, touchY)) {
                        eventEditingCache.changeToEdit();
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
                boolean hasEditingEvent = eventEditingCache != null;
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
        RectF rectF = getCurTimeLineEventView().getRectOnTimeLine(eventModel.timeStart, eventModel.timeTaken);
        return rectF.contains(touchX, touchY);
    }

    private void invalidate() {
        getV().invalidate();
    }


    private boolean isTopScalerUnderTouch(Event editingEvent, float touchX, float touchY) {
        RectF rectF = getCurTimeLineEventView().getRectOnTimeLine(editingEvent.timeStart, editingEvent.timeTaken);

        PointF topDragHandlerPoint = getTopScallerPoint(rectF);
        return getScalerRectF(topDragHandlerPoint.x, topDragHandlerPoint.y).contains(touchX, touchY);
    }

    private boolean isBottomScalerUnderTouch(Event editingEvent, float touchX, float touchY) {
        RectF rectF = getCurTimeLineEventView().getRectOnTimeLine(editingEvent.timeStart, editingEvent.timeTaken);
        PointF topDragHandlerPoint = getBottomScallerPoint(rectF);
        return getScalerRectF(topDragHandlerPoint.x, topDragHandlerPoint.y).contains(touchX, touchY);
    }

    private boolean isInRect(RectF rectF, float x, float y) {
        return rectF.contains(x, getYWithScroll(y));
    }

    private RectF getScalerRectF(float x, float y) {
        float halfSize = scallerRadius + scallerPadding;
        return new RectF(x - halfSize, y - halfSize, x + halfSize, y + halfSize);
    }


    private Event createEvent(float touchY) {
        long timeStart = getCurTimeLineEventView().getTimeByOffsetY(touchY);
        return new Event(timeStart, DEFAULT_EVENT_TIME_TAKEN);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        gestureDetector.onTouchEvent(motionEvent);
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchY = motionEvent.getY();
                lastTouchX = motionEvent.getX();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                moveDistanceY = 0;
                stopScroll();
                if (hasEventUnderTouch && eventAdjustListener != null) {
                    eventAdjustListener.onEventAdjustEnd();
                }
                if (eventEditingCache != null) {
                    eventEditingCache.reset();
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                moveDistanceY = motionEvent.getY() - lastTouchY;
                float moveDistanceX = motionEvent.getX() - lastTouchX;
                lastTouchY = motionEvent.getY();
                lastTouchX = motionEvent.getX();
                if (hasEventUnderTouch) {
                    checkScroll(lastTouchY);
                    checkEditEvent(moveDistanceX, moveDistanceY);
                    return true;
                }
                break;
        }
        return hasEventUnderTouch;
    }

    private boolean checkEditEvent(float moveDistanceX, float moveDistanceY) {
        if ((moveDistanceY != 0 || moveDistanceX != 0) && eventEditingCache != null) {
            long timeAdjust = getCurTimeLineEventView().getTimeByDistance(moveDistanceY);
            Log.d("checkEditEvent", moveDistanceX + "--");
            long timeAdjustBound;
            if (eventEditingCache.status == STATUS_EDITING) {
                eventEditingCache.moveBy(moveDistanceX, timeAdjust);
                timeAdjustBound = eventEditingCache.newEvent.timeStart;
            } else if (eventEditingCache.status == STATUS_SCALING_TOP) {
                eventEditingCache.scaleTopBy(timeAdjust);
                if (DEFAULT_EVENT_TIME_TAKEN > eventEditingCache.newEvent.timeTaken) {
                    eventEditingCache.scaleTopBy(eventEditingCache.newEvent.timeTaken - DEFAULT_EVENT_TIME_TAKEN);
                }
                timeAdjustBound = eventEditingCache.newEvent.timeStart;
            } else {
                eventEditingCache.scaleBottomBy(timeAdjust);
                if (DEFAULT_EVENT_TIME_TAKEN > eventEditingCache.newEvent.timeTaken) {
                    eventEditingCache.scaleBottomBy(DEFAULT_EVENT_TIME_TAKEN - eventEditingCache.newEvent.timeTaken);
                }
                timeAdjustBound = eventEditingCache.newEvent.getTimeEnd();
            }

            invalidate();
            if (eventAdjustListener != null) {
                eventAdjustListener.onEventAdjusting(timeAdjustBound);
            }
            return true;
        }
        return false;
    }

    private boolean checkScroll(float touchY) {
        int adjustSpace = getV().getHeight() / 8;
        if (touchY < adjustSpace && moveDistanceY <= 0 && getCurTimeLineEventView().canScroll(false)) {
            startScroll(false);
            return true;
        } else if (touchY > getV().getHeight() - adjustSpace && moveDistanceY >= 0 && getCurTimeLineEventView().canScroll(true)) {
            startScroll(true);
            return true;
        } else {
            stopScroll();
        }
        return false;
    }

    private void startScroll(final boolean isScrollUp) {
        if (scrollAnimator != null && scrollAnimator.isRunning()) {
            return;
        }

        final int from;
        int target;
        if (isScrollUp) {
            from = getV().getScrollY();
            target = getCurTimeLineEventView().getTotalHeight() - getV().getHeight();
        } else {
            from = getV().getScrollY();
            target = 0;
        }
        scrollAnimator = ObjectAnimator.ofInt(from, target).setDuration(Math.abs(target - from));
        scrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            private int lastScrollBy = from;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int scrollTo = (int) animation.getAnimatedValue();
                if (eventAdjustListener != null) {
                    eventAdjustListener.onEventAdjustWithScroll(scrollTo);
                }
                checkEditEvent(0, scrollTo - lastScrollBy);//滚动距离+滚动时的move距离
                lastScrollBy = scrollTo;
            }
        });
        scrollAnimator.start();
    }

    private void stopScroll() {
        if (scrollAnimator != null) {
            scrollAnimator.cancel();
        }
    }

    public void draw(Canvas canvas) {
        if (eventEditingCache != null) {
            drawEventOnEdit(canvas, eventEditingCache);
        }
    }

    private void resetEventStatus() {
        eventEditingCache.refreshOrigin();
        eventEditingCache = null;
    }

    private void drawEventOnEdit(Canvas canvas, EventCache eventCache) {
        RectF rectF = getCurTimeLineEventView().getRectOnTimeLine(eventCache.newEvent.timeStart, eventCache.newEvent.timeTaken);
        rectF.offsetTo(eventCache.newX, rectF.top);
        canvas.drawRect(rectF, eventEditP);
        drawContent(canvas, eventCache.newEvent, rectF);

        PointF topDragHandlerPoint = getTopScallerPoint(rectF);
        PointF bottomDragHandlerPoint = getBottomScallerPoint(rectF);
        drawScaller(canvas, topDragHandlerPoint, bottomDragHandlerPoint);
        eventDragHandlerP.setStyle(Paint.Style.STROKE);
        eventDragHandlerP.setColor(SCALLER_STROKE_COLOR);
        drawScaller(canvas, topDragHandlerPoint, bottomDragHandlerPoint);
        eventDragHandlerP.setColor(SCALLER_SOLID_COLOR);
        eventDragHandlerP.setStyle(Paint.Style.FILL);
    }

    private void drawScaller(Canvas canvas, PointF topScaller, PointF bottomScaller) {
        canvas.drawCircle(topScaller.x, topScaller.y, scallerRadius, eventDragHandlerP);
        canvas.drawCircle(bottomScaller.x, bottomScaller.y, scallerRadius, eventDragHandlerP);
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

    private PointF getTopScallerPoint(RectF eventRectF) {
        float size = 2 * scallerRadius;
        float topXOffset = eventRectF.right - size;
        float topYOffset = eventRectF.top;
        return new PointF(topXOffset, topYOffset);
    }


    private PointF getBottomScallerPoint(RectF eventRectF) {
        float size = 2 * scallerRadius;
        float bottomXOffset = eventRectF.left + size;
        float bottomYOffset = eventRectF.bottom;
        return new PointF(bottomXOffset, bottomYOffset);
    }

    public TimeLineEventViewPager getV() {
        return timeLineEventViewWR.get();
    }

    private TimeLineEventView getCurTimeLineEventView() {
        return getV().getCurrentView();
    }

    public interface Callback {
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

    private Comparator<Event> eventComparator = new Comparator<Event>() {
        @Override
        public int compare(Event o1, Event o2) {
            return (int) (o1.timeStart - o2.timeStart);
        }
    };
}
