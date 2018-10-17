package io.git.zjoker.timelinerecyclerview.ui.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.git.zjoker.timelinerecyclerview.ui.model.TimeLineModel;
import io.git.zjoker.timelinerecyclerview.ui.widget.TimeLineRecyclerView;
import io.git.zjoker.timelinerecyclerview.util.ViewUtil;

public class TimeLineHelper {
    private WeakReference<ScrollView> timeLineRVWR;
    public static final String TIME_TEXT_FORMAT_HOUR = "%s:00";
    private static float UNIT_HEIGHT = ViewUtil.dpToPx(80);
    private int vWidth;
    private List<TimeLineModel> timeLineModels;
    public static int TIME_LINE_TOTAL_COUNT = 24;
    private Paint timeTextP;
    private Paint timeLineP;
    private Paint curTimeP;
    //    private int timeTextWidth;
    private int originPaddingLeft;
    private int lineStartX;
    private int lineEndX;
    private float topSpace;
    private float bottomSpace;

    public TimeLineHelper() {
        topSpace = ViewUtil.dpToPx(30);
        bottomSpace = topSpace;
        timeLineModels = new ArrayList<>();
        timeTextP = new Paint();
        timeTextP.setStrokeWidth(2);
        timeTextP.setTextSize(50);
        timeTextP.setColor(Color.GRAY);

        timeLineP = new Paint();
        timeLineP.setStrokeWidth(2);
        timeLineP.setColor(Color.GRAY);

        curTimeP = new Paint();
        curTimeP.setStrokeWidth(2);
        curTimeP.setColor(Color.RED);
        reset();
    }

    private ScrollView getRV() {
        return timeLineRVWR.get();
    }

    public void attach(final ScrollView timeLineRecyclerView) {
        reset();
        this.timeLineRVWR = new WeakReference<>(timeLineRecyclerView);
        originPaddingLeft = getRV().getPaddingLeft();

        getRV().getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                vWidth = getRV().getWidth();
                adjustPadding();
                getRV().getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        });
        calculateTimeLinesArray();

        invalidate();
    }

    private void adjustPadding() {
        int timeTextWidth = (int) timeTextP.measureText(String.format(TIME_TEXT_FORMAT_HOUR, "00"));
        lineStartX = originPaddingLeft + timeTextWidth;
        lineEndX = getRV().getWidth() - getRV().getPaddingRight();
    }

    public float getTotalHeight() {
        return getAllLineHeight() + topSpace + bottomSpace;
    }

    private float getAllLineHeight() {
        return (TIME_LINE_TOTAL_COUNT - 1) * UNIT_HEIGHT;
    }

    private void calculateTimeLinesArray() {
        String tip;
        float topOffset = getTopOffset();
        for (int i = 0; i < TIME_LINE_TOTAL_COUNT; i++) {
            if (i < 10) {
                tip = String.format(TIME_TEXT_FORMAT_HOUR, ("0" + i));
            } else {
                tip = String.format(TIME_TEXT_FORMAT_HOUR, i);
            }
            timeLineModels.add(new TimeLineModel(i * UNIT_HEIGHT + topOffset, tip));
        }
    }

    public void reset() {
        timeLineModels.clear();
        originPaddingLeft = 0;
    }

    private void invalidate() {
        getRV().invalidate();
    }

    public void draw(Canvas canvas) {
        int save = canvas.save();
        drawTimeUnits(canvas);
        drawCurTimeLine(canvas);
        canvas.restoreToCount(save);
    }

    private void drawTimeUnits(Canvas canvas) {
        int textLeft = getRV().getPaddingLeft();
        for (int i = 0; i < timeLineModels.size(); i++) {
            TimeLineModel timeLineModel = timeLineModels.get(i);
            canvas.drawText(timeLineModel.timeTip, textLeft, timeLineModel.yOffset, timeTextP);
            canvas.drawLine(lineStartX, timeLineModel.yOffset, lineEndX, timeLineModel.yOffset, timeLineP);
        }
    }

    private float getTopOffset() {
        return getRV().getPaddingTop() + topSpace;
    }

    private int getTotalSecond() {
        return (TIME_LINE_TOTAL_COUNT - 1) * 60 * 60;
    }

    private void drawCurTimeLine(Canvas canvas) {
        float totalSecond = getTotalSecond() * 1f;
        Calendar instance = Calendar.getInstance();
        long curSecond = instance.get(Calendar.HOUR_OF_DAY) * 60 * 60 + instance.get(Calendar.MINUTE) * 60 + instance.get(Calendar.SECOND);

        float yOffset = curSecond / totalSecond * getAllLineHeight() + getTopOffset();
        canvas.drawLine(lineStartX, yOffset, lineEndX, yOffset, curTimeP);
    }

    public RectF getRectYByTime(long timeStempStart, long timeStempEnd) {
        Calendar instance = Calendar.getInstance();
        instance.setTimeInMillis(timeStempStart*1000);

        int totalSecond = getTotalSecond();

        float topOffset = timeStempStart * 1f / totalSecond * getAllLineHeight() + getTopOffset();
        float bottomOffset = timeStempEnd* 1f / totalSecond * getAllLineHeight() + getTopOffset();

        return new RectF(lineStartX, topOffset, lineEndX, bottomOffset);
    }

    public long getTimeByOffset(float offSetY) {
        float radio = (offSetY - getTopOffset()) / getAllLineHeight();
        return (long) (radio * getTotalSecond());
    }
}
