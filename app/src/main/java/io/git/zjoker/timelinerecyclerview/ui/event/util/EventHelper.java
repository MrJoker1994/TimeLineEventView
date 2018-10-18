package io.git.zjoker.timelinerecyclerview.ui.event.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.git.zjoker.timelinerecyclerview.ui.event.model.EventModel;
import io.git.zjoker.timelinerecyclerview.ui.widget.TimeLineEventView;

public class EventHelper {
    private List<EventModel> eventModels;
    private WeakReference<TimeLineEventView> timeLineEventViewWR;
    private Paint eventSolidP;
    private GestureDetector gestureDetector;
    public static final long DEFAUL_EVENT_SPAN = 30 * 60;

    public EventHelper() {
        this.eventModels = new ArrayList<>();
        eventSolidP = new Paint();
        eventSolidP.setStyle(Paint.Style.FILL);
        eventSolidP.setColor(Color.parseColor("#AAAAAAFF"));

    }

    public void attach(TimeLineEventView timeLineEventView) {
        timeLineEventViewWR = new WeakReference<>(timeLineEventView);
        gestureDetector = new GestureDetector(timeLineEventView.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
                long timeStart = getV().getTimeByOffset(e.getY());
                long timeEnd = timeStart + DEFAUL_EVENT_SPAN;
                eventModels.add(new EventModel(timeStart, timeEnd));
                getV().invalidate();
            }
        });

    }

    public boolean dispathTouchEvent(MotionEvent motionEvent) {
        return gestureDetector.onTouchEvent(motionEvent);
    }

    public void draw(Canvas canvas) {
        for (int i = 0; i < eventModels.size(); i++) {
            drawEvent(eventModels.get(i), canvas);
        }
    }

    private void drawEvent(EventModel eventModel, Canvas canvas) {
        canvas.drawRect(getV().getRectYByTime(eventModel.timeStart, eventModel.timeEnd), eventSolidP);
    }

    public TimeLineEventView getV() {
        return timeLineEventViewWR.get();
    }
}
