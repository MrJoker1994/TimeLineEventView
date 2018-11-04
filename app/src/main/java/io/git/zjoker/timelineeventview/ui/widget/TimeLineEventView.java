package io.git.zjoker.timelineeventview.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.git.zjoker.timelineeventview.ui.event.model.Event;
import io.git.zjoker.timelineeventview.ui.event.util.EventAdjustHelper;
import io.git.zjoker.timelineeventview.ui.event.util.EventsHelper;
import io.git.zjoker.timelineeventview.ui.timeline.util.TimeLineHelper;

import static io.git.zjoker.timelineeventview.ui.event.util.EventAdjustHelper.DEFAULT_EVENT_TIME_TAKEN;
import static io.git.zjoker.timelineeventview.ui.event.util.EventAdjustHelper.getTimeIgnoreDay;

public class TimeLineEventView extends ScrollView implements TimeLineHelper.TimeLineEventWatcher {
    private TimeLineHelper timeLineHelper;
    private EventsHelper eventHelper;
    private SpaceView spaceView;
    public Set<Event> eventSet;

    public TimeLineEventView(@NonNull Context context) {
        super(context);
        init();
    }

    public TimeLineEventView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TimeLineEventView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        eventSet = new HashSet<>();
        spaceView = new SpaceView(getContext());
        addView(spaceView);

        timeLineHelper = new TimeLineHelper(this);
        timeLineHelper.attach(this);

        setSpaceViewHeight((int) timeLineHelper.getTotalHeight());

        eventHelper = new EventsHelper();
        eventHelper.attach(this);
    }

    public RectF getRectOnTimeLine(long timeStampStart, long timeTaken) {
        return timeLineHelper.getRectOnTimeLine(getTimeIgnoreDay(timeStampStart), timeTaken);
    }

    public long getTimeByOffsetY(float offSetY) {
        return timeLineHelper.getTimeByOffsetY(getScrollY() + offSetY);
    }

    public long getTimeByDistance(float distance) {
        return timeLineHelper.getTimeByDistance(distance);
    }

    private void setSpaceViewHeight(int height) {
        spaceView.setHeight(height);
        spaceView.requestLayout();
    }

    public int getTotalHeight() {
        return spaceView.getHeight();
    }


    public boolean canScroll(boolean isScrollToUp) {
        if (isScrollToUp) {
            return getScrollY() < getTotalHeight() - getHeight();
        } else {
            return getScrollY() > 0;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        if (timeLineHelper.onTouchEvent(ev)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        timeLineHelper.draw(canvas);
        eventHelper.draw(canvas);
    }

    public void onEventAdjusting(long timeAdjust) {
        timeLineHelper.onEventAdjusting(timeAdjust);
    }

    public void onEventAdjustEnd() {
        timeLineHelper.onEventAdjustEnd();
    }

    public void onEventAdjustWithScroll(int scrollTo) {
        scrollTo(0, scrollTo);
    }

    @Override
    public void onScale() {
        setSpaceViewHeight((int) timeLineHelper.getTotalHeight());
    }

    public Set<Event> getEvents() {
        return eventSet;
    }

    public void notifyEvents(Set<Event> events) {
        this.eventSet = events;
        invalidate();
    }

    private static class SpaceView extends View {
        private int realHeight;

        public SpaceView(Context context) {
            super(context);
        }

        private void setHeight(int height) {
            this.realHeight = height;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), realHeight);
        }
    }

}
