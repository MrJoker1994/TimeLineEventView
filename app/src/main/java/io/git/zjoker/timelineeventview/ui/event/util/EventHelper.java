package io.git.zjoker.timelineeventview.ui.event.util;

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
    private Paint eventEditStrokeP;
    private Paint eventDragHandlerP;
    private GestureDetector gestureDetector;
    public static final long DEFAULT_EVENT_TIME_TAKEN = 30 * 60;

    private int editingPostion = -1;
    private float dragHandlerRadius;
    private EventAdjustListener eventAdjustListener;
    private boolean hasEventUnderTouch;

    public EventHelper() {
        this.eventModels = new ArrayList<>();
        eventSolidP = new Paint();
        eventSolidP.setStyle(Paint.Style.FILL);
        eventSolidP.setColor(Color.parseColor("#AAAAAAFF"));

        eventEditStrokeP = new Paint();
        eventEditStrokeP.setStyle(Paint.Style.STROKE);
        eventEditStrokeP.setStrokeWidth(ViewUtil.dpToPx(2));
        eventEditStrokeP.setColor(Color.parseColor("#FFAAAAFF"));

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
                EventModel eventWillEdit;
                if ((eventWillEdit = getEventUnderTouch(e.getX(), e.getY() + getV().getScrollY())) != null) {
                    eventWillEdit.changeToEdit();
                } else {
                    eventWillEdit = createEvent(e.getY());
                    eventModels.add(eventWillEdit);
                }
                hasEventUnderTouch = true;
                if (eventAdjustListener != null) {
                    eventAdjustListener.onEventAdjusting(eventWillEdit.timeStart);
                }
                invalidate();
            }

            @Override
            public boolean onDown(MotionEvent e) {
                EventModel eventUnderTouch = getEventUnderTouch(e.getX(), e.getY() + getV().getScrollY());
                EventModel eventEditing = getEditingEvent();
                if (eventEditing != null) {
                    if (isUnderTopScaler(eventEditing, e.getX(), e.getY() + getV().getScrollY())) {
                        eventEditing.changeToScaleTop();
                        hasEventUnderTouch = true;
                    } else if (isUnderBottomScaler(eventEditing, e.getX(), e.getY() + getV().getScrollY())) {
                        eventEditing.changeToScaleBottom();
                        hasEventUnderTouch = true;
                    } else if (eventUnderTouch == eventEditing) {
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
                boolean hasEditingEvent = getEditingEvent() != null;
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

    private void invalidate() {
        getV().invalidate();
    }

    private EventModel getEventUnderTouch(float touchX, float touchY) {
        for (int i = 0; i < eventModels.size(); i++) {
            EventModel eventModel = eventModels.get(i);
            RectF rectF = getV().getRectOnTimeLine(eventModel.timeStart, eventModel.timeTaken);
            if (rectF.contains(touchX, touchY)) {
                return eventModel;
            }
        }
        return null;
    }

    private boolean isUnderTopScaler(EventModel editingEvent, float touchX, float touchY) {
        RectF rectF = getV().getRectOnTimeLine(editingEvent.timeStart, editingEvent.timeTaken);

        PointF topDragHandlerPoint = getTopDragHandlerPoint(rectF);
        return getScalerRectF(topDragHandlerPoint.x, topDragHandlerPoint.y).contains(touchX, touchY);
    }

    private boolean isUnderBottomScaler(EventModel editingEvent, float touchX, float touchY) {
        RectF rectF = getV().getRectOnTimeLine(editingEvent.timeStart, editingEvent.timeTaken);
        PointF topDragHandlerPoint = getBottomDragHandlerPoint(rectF);
        return getScalerRectF(topDragHandlerPoint.x, topDragHandlerPoint.y).contains(touchX, touchY);
    }

    private boolean isInRect(RectF rectF, float x, float y) {
        return rectF.contains(x, y + getV().getScrollY());
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
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (hasEventUnderTouch && eventAdjustListener != null) {
                    eventAdjustListener.onEventAdjustEnd();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (hasEventUnderTouch) {
                    checkEditEvent(motionEvent.getX(), motionEvent.getY());
                    return true;
                }
                break;
        }
        return hasEventUnderTouch;
    }

    private boolean checkEditEvent(float touchX, float touchY) {
        EventModel event = getEditingEvent();
        if (event != null) {
            long timeAdjust;
            if (event.status == STATUS_EDITING) {
                timeAdjust = getV().getTimeByOffsetY(touchY);
                event.moveTo(timeAdjust);
            } else if (event.status == STATUS_SCALING_TOP) {
                timeAdjust = getV().getTimeByOffsetY(touchY);
                event.scaleTopTo(timeAdjust);
            } else {
                timeAdjust = getV().getTimeByOffsetY(touchY);
                event.scaleBottomTo(timeAdjust);
            }
            invalidate();
            if (eventAdjustListener != null) {
                eventAdjustListener.onEventAdjusting(timeAdjust);
            }
            return true;
        }
        return false;
    }

    private EventModel getEditingEvent() {
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
        canvas.drawRect(rectF, eventSolidP);
        if (eventModel.status != STATUS_NORMAL) {
            drawEdit(canvas, eventModel, rectF);
        }
    }

    private void drawEdit(Canvas canvas, EventModel eventModel, RectF rectF) {
        canvas.drawRect(rectF, eventEditStrokeP);
        PointF topDragHandlerPoint = getTopDragHandlerPoint(rectF);
        canvas.drawCircle(topDragHandlerPoint.x, topDragHandlerPoint.y, dragHandlerRadius, eventDragHandlerP);

        PointF bottomDragHandlerPoint = getBottomDragHandlerPoint(rectF);
        canvas.drawCircle(bottomDragHandlerPoint.x, bottomDragHandlerPoint.y, dragHandlerRadius, eventDragHandlerP);
    }

    private PointF getTopDragHandlerPoint(RectF eventRectF) {
        float size = 2 * dragHandlerRadius;
        float topXOffset = eventRectF.right - size;
        float topYOffset = eventRectF.top;
        return new PointF(topXOffset, topYOffset);
    }

    private PointF getBottomDragHandlerPoint(RectF eventRectF) {
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
    }
}
