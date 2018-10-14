package io.git.zjoker.timelinerecyclerview.ui.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.ViewTreeObserver;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.git.zjoker.timelinerecyclerview.ui.model.TimeLineModel;
import io.git.zjoker.timelinerecyclerview.ui.widget.TimeLineRecyclerView;

public class TimeLineHelper {
    private WeakReference<TimeLineRecyclerView> timeLineRVWR;
    private int heightRV;
    private List<TimeLineModel> timeLineModels;
    public static int TIME_LINE_TOTAL_COUNT = 24;
    private Paint timeTextP;
    private Paint timeLineP;

    public TimeLineHelper() {
        timeLineModels = new ArrayList<>();
        timeTextP = new Paint();
        timeTextP.setStrokeWidth(2);
        timeTextP.setTextSize(50);
        timeTextP.setColor(Color.GRAY);

        timeLineP = new Paint();
        timeLineP.setStrokeWidth(2);
        timeLineP.setColor(Color.GRAY);
    }

    private TimeLineRecyclerView getRV() {
        return timeLineRVWR.get();
    }

    public void attach(final TimeLineRecyclerView timeLineRecyclerView) {
        this.timeLineRVWR = new WeakReference<>(timeLineRecyclerView);
        timeLineModels = new ArrayList<>();
        getRV().getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                heightRV = getRV().getHeight();
                calculateTimeLinesArray();
                getRV().getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        });
        invalidate();
    }

    private void calculateTimeLinesArray() {
        int availableHeight = getAvailableHeight();
        float perHeight = availableHeight * 1f / TIME_LINE_TOTAL_COUNT;

        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.HOUR_OF_DAY, 0);
        int topPadding = getRV().getPaddingTop() + 100;
        for (int i = 0; i < TIME_LINE_TOTAL_COUNT; i++) {
            timeLineModels.add(new TimeLineModel(i * perHeight + topPadding, i + "时"));
            instance.add(Calendar.HOUR_OF_DAY, 1);
        }
    }

    private int getAvailableHeight() {
        return heightRV - getRV().getPaddingTop() - getRV().getPaddingBottom();
    }

    private void invalidate() {
        getRV().invalidate();
    }

    public void drawTime(Canvas canvas) {
        int save = canvas.save();
        int paddingLeft = getRV().getPaddingLeft();
        int right = getRV().getWidth() - getRV().getPaddingRight();
        for (int i = 0; i < timeLineModels.size(); i++) {
            TimeLineModel timeLineModel = timeLineModels.get(i);
            canvas.drawText(timeLineModel.timeTip, paddingLeft, timeLineModel.yOffset, timeTextP);
            canvas.drawLine(paddingLeft, timeLineModel.yOffset, right, timeLineModel.yOffset, timeLineP);
        }
        canvas.restoreToCount(save);
    }
}
