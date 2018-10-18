package io.git.zjoker.timelinerecyclerview.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import io.git.zjoker.timelinerecyclerview.ui.event.util.EventHelper;
import io.git.zjoker.timelinerecyclerview.ui.util.TimeLineHelper;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class TimeLineEventView extends ScrollView {
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
    }

    public RectF getRectYByTime(long timeStempStart, long timeStempEnd) {
        return timeLineHelper.getRectYByTime(timeStempStart, timeStempEnd);
    }

    public long getTimeByOffset(float offSetY) {
        return timeLineHelper.getTimeByOffset(getScrollY() + offSetY);
    }

    private void setSpaceViewHeight(int height) {
        spaceView.setHeight(height);
        spaceView.requestLayout();
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        eventHelper.dispathTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        timeLineHelper.draw(canvas);
        eventHelper.draw(canvas);
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
