package io.git.zjoker.timelinerecyclerview.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import io.git.zjoker.timelinerecyclerview.ui.util.TimeLineHelper;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class TimeLineEventView extends ScrollView {
    private TimeLineHelper timeLineHelper;
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
    }


    private void setSpaceViewHeight(int height) {
        spaceView.setHeight(height);
        spaceView.requestLayout();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        timeLineHelper.draw(canvas);
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
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),realHeight);
        }
    }
}
