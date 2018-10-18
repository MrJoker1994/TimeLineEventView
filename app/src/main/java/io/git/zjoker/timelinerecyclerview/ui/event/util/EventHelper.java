package io.git.zjoker.timelinerecyclerview.ui.event.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.git.zjoker.timelinerecyclerview.ui.event.model.EventModel;
import io.git.zjoker.timelinerecyclerview.ui.widget.TimeLineEventView;
import io.git.zjoker.timelinerecyclerview.util.ViewUtil;

import static io.git.zjoker.timelinerecyclerview.ui.event.model.EventModel.STATUS_EDITING;
import static io.git.zjoker.timelinerecyclerview.ui.event.model.EventModel.STATUS_NORMAL;

public class EventHelper {
    private List<EventModel> eventModels;
    private WeakReference<TimeLineEventView> timeLineEventViewWR;
    private Paint eventSolidP;
    private Paint eventEditStrokeP;
    private GestureDetector gestureDetector;
    public static final long DEFAULT_EVENT_SPAN = 30 * 60;

    private int editingPostion = -1;

    public EventHelper() {
        this.eventModels = new ArrayList<>();
        eventSolidP = new Paint();
        eventSolidP.setStyle(Paint.Style.FILL);
        eventSolidP.setColor(Color.parseColor("#AAAAAAFF"));

        eventEditStrokeP = new Paint();
        eventEditStrokeP.setStyle(Paint.Style.STROKE);
        eventEditStrokeP.setStrokeWidth(ViewUtil.dpToPx(2));
        eventEditStrokeP.setColor(Color.parseColor("#FFAAAAFF"));
    }

    public void attach(TimeLineEventView timeLineEventView) {
        timeLineEventViewWR = new WeakReference<>(timeLineEventView);
        gestureDetector = new GestureDetector(timeLineEventView.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
                eventModels.add(createEvent(e.getY()));
                invalidate();
            }
        });
    }

    private void invalidate() {
        getV().invalidate();
    }

    private EventModel createEvent(float touchY) {
        long timeStart = getV().getTimeByOffset(touchY);
        long timeEnd = timeStart + DEFAULT_EVENT_SPAN;
        return new EventModel(timeStart, timeEnd, STATUS_EDITING);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        gestureDetector.onTouchEvent(motionEvent);
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                resetEventStatus();
                invalidate();
                break;
        }
        return getEditingEvent() != null;
    }

    private EventModel getEditingEvent() {
        for (int i = 0; i < eventModels.size(); i++) {
            EventModel eventModel = eventModels.get(i);
            if(eventModel.status != STATUS_NORMAL) {
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
        RectF rectF = getV().getRectYByTime(eventModel.timeStart, eventModel.timeEnd);
        canvas.drawRect(rectF, eventSolidP);
        if (eventModel.status == STATUS_EDITING) {
            canvas.drawRect(rectF, eventEditStrokeP);
        }
    }

    public TimeLineEventView getV() {
        return timeLineEventViewWR.get();
    }
}
