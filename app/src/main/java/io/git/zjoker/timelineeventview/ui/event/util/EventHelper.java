package io.git.zjoker.timelineeventview.ui.event.util;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.git.zjoker.timelineeventview.ui.event.model.EventModel;
import io.git.zjoker.timelineeventview.ui.widget.TimeLineEventView;
import io.git.zjoker.timelineeventview.util.ViewUtil;

import static io.git.zjoker.timelineeventview.ui.event.model.EventModel.STATUS_EDITING;
import static io.git.zjoker.timelineeventview.ui.event.model.EventModel.STATUS_NORMAL;
import static io.git.zjoker.timelineeventview.ui.event.model.EventModel.STATUS_SCALING_TOP;

public class EventHelper {
    private List<EventModel> eventModels;
    private WeakReference<TimeLineEventView> timeLineEventViewWR;
    private Paint eventSolidP;
    private Paint eventEditP;
    private Paint eventDragHandlerP;
    private GestureDetector gestureDetector;
    public static final long DEFAULT_EVENT_TIME_TAKEN = 50 * 60;

    private int editingPostion = -1;
    private float dragHandlerRadius;
    private EventAdjustListener eventAdjustListener;
    private boolean hasEventUnderTouch;

    private float touchMoveDistance;
    private float lastTouchY;

    private ValueAnimator scrollAnimator;

    public EventHelper() {
        this.eventModels = new ArrayList<>();
        eventSolidP = new Paint();
        eventSolidP.setStyle(Paint.Style.FILL);
        eventSolidP.setColor(Color.parseColor("#AAAAAAFF"));

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


    public void attach(TimeLineEventView timeLineEventView) {
        timeLineEventViewWR = new WeakReference<>(timeLineEventView);
        gestureDetector = new GestureDetector(timeLineEventView.getContext(), new GestureDetector.SimpleOnGestureListener() {

            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
                float touchX = e.getX();
                float touchY = getYWithScroll(e.getY());

                if (getEventEditing() == null) {
                    EventModel eventUnderTouch = getEventUnderTouch(touchX, touchY);
                    if (eventUnderTouch == null) {
                        eventUnderTouch = createEvent(e.getY());
                        eventUnderTouch.changeToEdit();
                        eventModels.add(eventUnderTouch);
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
                EventModel eventEditing = getEventEditing();
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

    private boolean isEventUnderTouch(EventModel eventModel, float touchX, float touchY) {
        RectF rectF = getV().getRectOnTimeLine(eventModel.timeStart, eventModel.timeTaken);
        return rectF.contains(touchX, touchY);
    }

    private void invalidate() {
        getV().invalidate();
    }

    private EventModel getEventUnderTouch(float touchX, float touchY) {
        for (int i = 0; i < eventModels.size(); i++) {
            EventModel eventModel = eventModels.get(i);
            if (isEventUnderTouch(eventModel, touchX, touchY)) {
                return eventModel;
            }
        }
        return null;
    }

    private boolean isTopScalerUnderTouch(EventModel editingEvent, float touchX, float touchY) {
        RectF rectF = getV().getRectOnTimeLine(editingEvent.timeStart, editingEvent.timeTaken);

        PointF topDragHandlerPoint = getTopScallerPoint(rectF);
        return getScalerRectF(topDragHandlerPoint.x, topDragHandlerPoint.y).contains(touchX, touchY);
    }

    private boolean isBottomScalerUnderTouch(EventModel editingEvent, float touchX, float touchY) {
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


    private EventModel createEvent(float touchY) {
        long timeStart = getV().getTimeByOffsetY(touchY);
        return new EventModel(timeStart, DEFAULT_EVENT_TIME_TAKEN, STATUS_EDITING);
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
                        checkEditEvent(motionEvent.getX(), lastTouchY);
                    }
                    return true;
                }
                break;
        }
        return hasEventUnderTouch;
    }

    private boolean checkEditEvent(float touchX, float touchY) {
        EventModel eventEditing = getEventEditing();
        if (eventEditing != null) {
            long timeAdjust;
            if (eventEditing.status == STATUS_EDITING) {
                timeAdjust = getV().getTimeByOffsetY(touchY);
                eventEditing.moveTo(timeAdjust);
            } else if (eventEditing.status == STATUS_SCALING_TOP) {
                timeAdjust = getV().getTimeByOffsetY(touchY);
                eventEditing.scaleTopTo(timeAdjust);
            } else {
                timeAdjust = getV().getTimeByOffsetY(touchY);
                eventEditing.scaleBottomTo(timeAdjust);
            }
            invalidate();
            if (eventAdjustListener != null) {
                eventAdjustListener.onEventAdjusting(timeAdjust);
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
                checkEditEvent(0, lastTouchY);
            }
        });
        scrollAnimator.start();
    }

    private void stopScroll() {
        if (scrollAnimator != null) {
            scrollAnimator.cancel();
        }
    }

    private EventModel getEventEditing() {
        for (int i = 0; i < eventModels.size(); i++) {
            EventModel eventModel = eventModels.get(i);
            if (eventModel.status != STATUS_NORMAL) {
                return eventModel;
            }
        }
        return null;
    }

    public void draw(Canvas canvas) {
        for (int i = 0; i < eventModels.size(); i++) {
            drawEvent(i, eventModels.get(i), canvas);
        }
    }

    private void resetEventStatus() {
        for (int i = 0; i < eventModels.size(); i++) {
            eventModels.get(i).changeToNormal();
        }
    }

    private void drawEvent(int index, EventModel eventModel, Canvas canvas) {
        RectF rectF = getV().getRectOnTimeLine(eventModel.timeStart, eventModel.timeTaken);
        if (eventModel.status != STATUS_NORMAL) {
            drawEventOnEdit(canvas, eventModel, rectF);
        } else {
            drawEventOnNormal(canvas, rectF);
        }
    }

    private void drawEventOnNormal(Canvas canvas, RectF rectF) {
        canvas.drawRect(rectF, eventSolidP);
        canvas.drawRect(rectF.left, rectF.top, rectF.left + ViewUtil.dpToPx(3), rectF.bottom, eventEditP);
    }

    private void drawEventOnEdit(Canvas canvas, EventModel eventModel, RectF rectF) {
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
        if(timeLineEventViewWR != null) {
            timeLineEventViewWR.clear();
        }
    }
}
