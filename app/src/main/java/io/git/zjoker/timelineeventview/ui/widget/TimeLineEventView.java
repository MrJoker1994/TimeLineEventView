package io.git.zjoker.timelineeventview.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

import io.git.zjoker.timelineeventview.ui.event.util.EventHelper;
import io.git.zjoker.timelineeventview.ui.timeline.util.TimeLineHelper;

public class TimeLineEventView extends ScrollView implements EventHelper.EventAdjustListener{
    private TimeLineHelper timeLineHelper;
    private EventHelper eventHelper;
    private SpaceView spaceView;

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
        spaceView = new SpaceView(getContext());
        addView(spaceView);

        timeLineHelper = new TimeLineHelper();
        timeLineHelper.attach(this);

        setSpaceViewHeight((int) timeLineHelper.getTotalHeight());

        eventHelper = new EventHelper();
        eventHelper.attach(this);

        eventHelper.setEventAdjustListener(this);
    }

    public RectF getRectOnTimeLine(long timeStampStart, long timeTaken) {
        return timeLineHelper.getRectOnTimeLine(timeStampStart, timeTaken);
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
        if(isScrollToUp) {
            return getScrollY() < getTotalHeight() - getHeight();
        } else {
            return getScrollY() > 0;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (eventHelper.onTouchEvent(ev)) {
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

    @Override
    public void onEventAdjusting(long timeAdjust) {
        timeLineHelper.onEventAdjusting(timeAdjust);
    }

    @Override
    public void onEventAdjustEnd() {
        timeLineHelper.onEventAdjustEnd();
    }

    @Override
    public void onEventAdjustWithScroll(int scrollTo) {
        scrollTo(0,scrollTo);
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
