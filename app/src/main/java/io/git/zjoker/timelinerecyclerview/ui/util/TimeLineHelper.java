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
import io.git.zjoker.timelinerecyclerview.util.ViewUtil;

public class TimeLineHelper {
    private WeakReference<TimeLineRecyclerView> timeLineRVWR;
    public static final String TIME_TEXT_FORMAT_HOUR = "%s:00";
    private static float UNIT_HEIGHT = ViewUtil.dpToPx(80);
    private int heightRV;
    private List<TimeLineModel> timeLineModels;
    public static int TIME_LINE_TOTAL_COUNT = 24;
    private Paint timeTextP;
    private Paint timeLineP;
    //    private int timeTextWidth;
    private int originPaddingLeft;

    public TimeLineHelper() {
        timeLineModels = new ArrayList<>();
        timeTextP = new Paint();
        timeTextP.setStrokeWidth(2);
        timeTextP.setTextSize(50);
        timeTextP.setColor(Color.GRAY);

        timeLineP = new Paint();
        timeLineP.setStrokeWidth(2);
        timeLineP.setColor(Color.GRAY);
        reset();
    }

    private TimeLineRecyclerView getRV() {
        return timeLineRVWR.get();
    }

    public void attach(final TimeLineRecyclerView timeLineRecyclerView) {
        reset();
        this.timeLineRVWR = new WeakReference<>(timeLineRecyclerView);
        originPaddingLeft = getRV().getPaddingLeft();

        adjustPadding();
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

    private void adjustPadding() {
        int timeTextWidth = (int) timeTextP.measureText(String.format(TIME_TEXT_FORMAT_HOUR, "00"));
        getRV().setPadding(originPaddingLeft + timeTextWidth, getRV().getPaddingTop(), getRV().getPaddingRight(), getRV().getPaddingBottom());
    }

    private void calculateTimeLinesArray() {
        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.HOUR_OF_DAY, 0);
        int topPadding = getRV().getPaddingTop() + 80;

        String tip;
        for (int i = 0; i < TIME_LINE_TOTAL_COUNT; i++) {
            if (i < 10) {
                tip = String.format(TIME_TEXT_FORMAT_HOUR, ("0" + i));
            } else {
                tip = String.format(TIME_TEXT_FORMAT_HOUR, i);
            }
            timeLineModels.add(new TimeLineModel(i * UNIT_HEIGHT + topPadding, tip));
            instance.add(Calendar.HOUR_OF_DAY, 1);
        }
    }

    public void reset() {
        timeLineModels.clear();
        originPaddingLeft = 0;
    }

    private int getAvailableHeight() {
        return heightRV - getRV().getPaddingTop() - getRV().getPaddingBottom();
    }

    private void invalidate() {
        getRV().invalidate();
    }

    public void drawTimeLine(Canvas canvas) {
        int save = canvas.save();
        int lineStartX = getRV().getPaddingLeft();
        int lineEndX = getRV().getWidth() - getRV().getPaddingRight();
        int textLeft = originPaddingLeft;
        for (int i = 0; i < timeLineModels.size(); i++) {
            TimeLineModel timeLineModel = timeLineModels.get(i);
            canvas.drawText(timeLineModel.timeTip, textLeft, timeLineModel.yOffset, timeTextP);
            canvas.drawLine(lineStartX, timeLineModel.yOffset, lineEndX, timeLineModel.yOffset, timeLineP);
        }
        canvas.restoreToCount(save);
    }
}
