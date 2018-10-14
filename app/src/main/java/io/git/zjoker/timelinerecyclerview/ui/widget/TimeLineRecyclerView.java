package io.git.zjoker.timelinerecyclerview.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import io.git.zjoker.timelinerecyclerview.ui.util.TimeLineHelper;

public class TimeLineRecyclerView extends RecyclerView {
    private TimeLineHelper timeLineHelper;

    public TimeLineRecyclerView(Context context) {
        super(context);
        init();
    }

    public TimeLineRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TimeLineRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        timeLineHelper = new TimeLineHelper();
        timeLineHelper.attach(this);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        timeLineHelper.drawTimeLine(canvas);
    }
}
