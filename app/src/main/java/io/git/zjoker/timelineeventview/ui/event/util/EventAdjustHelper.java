package io.git.zjoker.timelineeventview.ui.event.util;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.annotation.Size;
import android.support.v4.view.ViewPager;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import io.git.zjoker.timelineeventview.ui.event.model.Event;
import io.git.zjoker.timelineeventview.ui.event.model.EventCache;
import io.git.zjoker.timelineeventview.ui.event.model.EventNode;
import io.git.zjoker.timelineeventview.ui.widget.TimeLineEventView;
import io.git.zjoker.timelineeventview.ui.widget.TimeLineEventViewPager;
import io.git.zjoker.timelineeventview.util.ViewUtil;

import static io.git.zjoker.timelineeventview.ui.event.model.EventCache.STATUS_EDITING;
import static io.git.zjoker.timelineeventview.ui.event.model.EventCache.STATUS_SCALING_TOP;
import static io.git.zjoker.timelineeventview.ui.timeline.util.TimeLineHelper.getTotalMilliSecond;

public class EventAdjustHelper {
    public static final long DEFAULT_EVENT_TIME_TAKEN = 50 * 60 * 1000;
    private final Handler crossDayHandler;
    private WeakReference<TimeLineEventViewPager> timeLineEventViewWR;

    private Paint eventSolidP;
    private Paint eventEditP;
    private Paint eventDragHandlerP;
    private TextPaint eventContentP;
    private GestureDetector gestureDetector;

    private float scallerRadius;
    private float scallerPadding;
    private EventAdjustHelper.Callback eventAdjustListener;

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
        crossDayHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                @Size(2) int[] days = (int[]) msg.obj;
                if (eventAdjustListener != null) {
                    eventEditingCache.changeDayTo(days[1], (days[1] - days[0]) * getV().getWidth());
                    eventEditingCache.refreshOrigin();
                    eventAdjustListener.onEventCrossDay(eventEditingCache.originEvent, days[0], days[1]);
                }
                return true;
            }
        });

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

    public void setEventAdjustListener(Callback eventAdjustListener) {
        this.eventAdjustListener = eventAdjustListener;
    }


    public void attach(TimeLineEventViewPager viewPager) {
        timeLineEventViewWR = new WeakReference<>(viewPager);
        gestureDetector = new GestureDetector(viewPager.getContext(), new GestureDetector.SimpleOnGestureListener() {

            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
                float touchX = e.getX();
                float touchY = e.getY();

                if (eventEditingCache == null) {
                    Event eventUnderTouch = getEventUnderTouch(touchX, e.getY(), getCurTimeLineEventView());
                    if (eventUnderTouch == null) {
                        eventUnderTouch = createNewEvent(touchY, getCurTimeLineEventView());
                        if (eventAdjustListener != null) {
                            eventAdjustListener.onEventCreated(eventUnderTouch);
                        }
                    }
                    float xOffset = getCurTimeLineEventView().getRectOnTimeLine(eventUnderTouch.timeStart, eventUnderTouch.timeTaken).left;
                    eventEditingCache = EventCache.build(eventUnderTouch, xOffset + getV().getCurrentItem() * getV().getWidth());

                    hasEventUnderTouch = true;
                    getCurTimeLineEventView().onEventAdjusting(getTimeIgnoreDay(eventUnderTouch.timeStart));
                    invalidate();
                }
            }

            @Override
            public boolean onDown(MotionEvent e) {
                float touchX = e.getX();
                float touchY = getYWithScroll(e.getY());
                if (eventEditingCache != null) {
                    if (isTopScalerUnderTouch(eventEditingCache.newEvent, touchX, touchY, getCurTimeLineEventView())) {
                        eventEditingCache.changeToScaleTop();
                        hasEventUnderTouch = true;
                    } else if (isBottomScalerUnderTouch(eventEditingCache.newEvent, touchX, touchY, getCurTimeLineEventView())) {
                        eventEditingCache.changeToScaleBottom();
                        hasEventUnderTouch = true;
                    } else if (isEventUnderTouch(eventEditingCache.newEvent, touchX, touchY, getCurTimeLineEventView())) {
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
                    eventEditingCache.refreshOrigin();
                    getCurTimeLineEventView().onEventAdjustEnd();
                    if (eventAdjustListener != null) {
                        eventAdjustListener.onEventAdjustEnd(eventEditingCache.newEvent);
                    }
                    eventEditingCache = null;

                    invalidate();
                }
                return super.onSingleTapUp(e);
            }
        });
    }

    private Event createNewEvent(float touchY, TimeLineEventView timeLineEventView) {
        long timeStart = timeLineEventView.getTimeByOffsetY(touchY);
        int hour = (int) (timeStart / (60 * 60 * 1000));
        timeStart -= (hour * 60 * 60 * 1000);
        int minute = (int) (timeStart / (60 * 1000));
        timeStart -= (minute * 60 * 1000);
        int second = (int) (timeStart / 1000);
        timeStart -= (second * 1000);
        int millisSecond = (int) timeStart;
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, getV().getCurrentPosition() + 1);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, millisSecond);
        Event event = new Event(calendar.getTimeInMillis(), DEFAULT_EVENT_TIME_TAKEN);
//        timeLineEventView.addEvent(event);
        return event;
    }

    private float getYWithScroll(float touchY) {
        return touchY + getCurTimeLineEventView().getScrollY();
    }

    public Event getEventUnderTouch(float touchX, float touchY, TimeLineEventView curTimeLineEventView) {
        Set<Event> events = curTimeLineEventView.getEvents();
        for (Event event : events) {
            if (isEventUnderTouch(event, touchX, touchY, getCurTimeLineEventView())) {
                return event;
            }
        }
        return null;
    }

    private boolean isEventUnderTouch(Event eventModel, float touchX, float touchY, TimeLineEventView timeLineEventView) {
        RectF rectF = timeLineEventView.getRectOnTimeLine(eventModel.timeStart, eventModel.timeTaken);
        return rectF.contains(touchX, getYWithScroll(touchY));
    }

    private void invalidate() {
        getV().invalidate();
    }

    private boolean isTopScalerUnderTouch(Event editingEvent, float touchX, float touchY, TimeLineEventView timeLineEventView) {
        RectF rectF = timeLineEventView.getRectOnTimeLine(editingEvent.timeStart, editingEvent.timeTaken);

        PointF topDragHandlerPoint = getTopScallerPoint(rectF);
        return getScalerRectF(topDragHandlerPoint.x, topDragHandlerPoint.y).contains(touchX, touchY);
    }

    private boolean isBottomScalerUnderTouch(Event editingEvent, float touchX, float touchY, TimeLineEventView timeLineEventView) {
        RectF rectF = timeLineEventView.getRectOnTimeLine(editingEvent.timeStart, editingEvent.timeTaken);
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

    private boolean checkCrossDay(float touchX, float moveDistanceX) {
        int fromDay = getV().getCurrentPosition();
        int toDay = fromDay;
        if (touchX < getCurTimeLineEventView().getWidth() / 6 && moveDistanceX <= 0) {
            toDay = fromDay - 1;
        } else if (touchX > getCurTimeLineEventView().getWidth() * 5 / 6 && moveDistanceX >= 0) {
            toDay = fromDay + 1;
        }
        if (fromDay == toDay) {
            crossDayHandler.removeMessages(1);
            return false;
        }
        stopScroll();
        if (!crossDayHandler.hasMessages(1)) {
            Message message = Message.obtain();
            message.what = 1;
            message.obj = new int[]{fromDay, toDay};
            crossDayHandler.sendMessageDelayed(message, 700);
        }

        return true;
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
                crossDayHandler.removeMessages(1);
                if (hasEventUnderTouch) {
                    getCurTimeLineEventView().onEventAdjustEnd();
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
                    if (!checkCrossDay(lastTouchX, moveDistanceX)) {
                        checkScroll(lastTouchY, getCurTimeLineEventView());
                    }
                    checkEditEvent(moveDistanceX, moveDistanceY, getCurTimeLineEventView());
                    return true;
                }
                break;
        }
        return hasEventUnderTouch;
    }

    private boolean checkEditEvent(float moveDistanceX, float moveDistanceY, TimeLineEventView timeLineEventView) {
        if ((moveDistanceY != 0 || moveDistanceX != 0) && eventEditingCache != null) {
            long timeAdjust = timeLineEventView.getTimeByDistance(moveDistanceY);
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
            getCurTimeLineEventView().onEventAdjusting(getTimeIgnoreDay(timeAdjustBound));

            return true;
        }
        return false;
    }

    private boolean checkScroll(float touchY, TimeLineEventView timeLineEventView) {
        int adjustSpace = getV().getHeight() / 8;
        if (touchY < adjustSpace && moveDistanceY <= 0 && timeLineEventView.canScroll(false)) {
            startScroll(false, timeLineEventView);
            return true;
        } else if (touchY > getV().getHeight() - adjustSpace && moveDistanceY >= 0 && timeLineEventView.canScroll(true)) {
            startScroll(true, timeLineEventView);
            return true;
        } else {
            stopScroll();
        }
        return false;
    }

    private void startScroll(final boolean isScrollUp, final TimeLineEventView timeLineEventView) {
        if (scrollAnimator != null && scrollAnimator.isRunning()) {
            return;
        }

        final int from;
        int target;
        if (isScrollUp) {
            from = getV().getScrollY();
            target = timeLineEventView.getTotalHeight() - getV().getHeight();
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
                getCurTimeLineEventView().onEventAdjustWithScroll(scrollTo);
                checkEditEvent(0, scrollTo - lastScrollBy, timeLineEventView);//滚动距离+滚动时的move距离
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
            drawEventOnEdit(canvas, eventEditingCache, getCurTimeLineEventView());
        }
    }

    private void resetEventStatus() {
    }

    private void drawEventOnEdit(Canvas canvas, EventCache eventCache, TimeLineEventView timeLineEventView) {
        RectF rectF = timeLineEventView.getRectOnTimeLine(eventCache.newEvent.timeStart, eventCache.newEvent.timeTaken);
        rectF.offsetTo(eventCache.newX, rectF.top - getCurTimeLineEventView().getScrollY());
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

    public static long getTimeIgnoreDay(long originTime) {
        Calendar instance = Calendar.getInstance();
        instance.setTimeInMillis(originTime);
        return instance.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000 + instance.get(Calendar.MINUTE) * 60 * 1000 + instance.get(Calendar.SECOND) * 1000 + instance.get(Calendar.MILLISECOND);
    }

    public interface Callback {
        void onEventCreated(Event newEvent);

        void onEventAdjustEnd(Event newEvent);

        void onEventCrossDay(Event event, int fromDay, int toDay);
    }

    public void dettach() {
        if (scrollAnimator != null) {
            scrollAnimator.cancel();
        }
        if (timeLineEventViewWR != null) {
            timeLineEventViewWR.clear();
        }

        crossDayHandler.removeMessages(1);
    }
}
