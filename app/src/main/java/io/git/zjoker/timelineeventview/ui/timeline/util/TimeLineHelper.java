package io.git.zjoker.timelineeventview.ui.timeline.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.git.zjoker.timelineeventview.ui.timeline.model.TimeLineModel;
import io.git.zjoker.timelineeventview.util.ViewUtil;

public class TimeLineHelper {
    private WeakReference<ScrollView> timeLineRVWR;
    public static final String TIME_TEXT_FORMAT_HOUR = "%s:00";
    public static final float MIN_SCALE = 1;
    public static final float MAX_SCALE = 3;
    private static float UNIT_HEIGHT = ViewUtil.dpToPx(80);
    private int vWidth;
    private List<TimeLineModel> timeLineModels;
    public static int TIME_LINE_TOTAL_COUNT = 24;
    private TextPaint timeTextP;
    private Paint timeLineP;
    private Paint curTimeP;
    //    private int timeTextWidth;
    private int originPaddingLeft;
    private int lineStartX;
    private int lineEndX;
    private float topSpace;
    private float bottomSpace;

    private boolean eventAdjusting;
    private long timeAdjust;

    private float timeUnitHeight = UNIT_HEIGHT;

    private ScaleGestureDetector gestureDetector;
    private TimeLineEventWatcher timeLineEventWatcher;

    public TimeLineHelper(TimeLineEventWatcher timeLineEventWatcher) {
        this.timeLineEventWatcher = timeLineEventWatcher;
        topSpace = ViewUtil.dpToPx(30);
        bottomSpace = topSpace;
        timeLineModels = new ArrayList<>();
        timeTextP = new TextPaint();
        timeTextP.setTextSize(ViewUtil.spToPx(18));
        timeTextP.setColor(Color.GRAY);

        timeLineP = new Paint();
        timeLineP.setStrokeWidth(ViewUtil.dpToPx(1));
        timeLineP.setColor(Color.GRAY);

        curTimeP = new Paint();
        curTimeP.setStrokeWidth(ViewUtil.dpToPx(1));
        curTimeP.setColor(Color.RED);
        reset();

    }

    private ScrollView getRV() {
        return timeLineRVWR.get();
    }

    public void attach(final ScrollView TimeLineEventView) {
        reset();
        this.timeLineRVWR = new WeakReference<>(TimeLineEventView);
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

        gestureDetector = new ScaleGestureDetector(getRV().getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private float centerY;

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                centerY = detector.getFocusY();
                return super.onScaleBegin(detector);
            }

            @Override
            public boolean onScale(final ScaleGestureDetector detector) {
                float realTimeUnitHeight = timeUnitHeight * detector.getScaleFactor();
                if (realTimeUnitHeight < MIN_SCALE * UNIT_HEIGHT) {
                    realTimeUnitHeight = MIN_SCALE * UNIT_HEIGHT;
                } else if (realTimeUnitHeight > MAX_SCALE * UNIT_HEIGHT) {
                    realTimeUnitHeight = MAX_SCALE * UNIT_HEIGHT;
                }
                float realScaleFactor = realTimeUnitHeight / timeUnitHeight;
                timeUnitHeight = realTimeUnitHeight;
                calculateTimeLinesArray();
                if (timeLineEventWatcher != null) {
                    timeLineEventWatcher.onScale();

                }

                Log.d("onScale", "--" + centerY + "--" + realScaleFactor * centerY);

                getRV().scrollBy(0, (int) (realScaleFactor * centerY - centerY));
                centerY = realScaleFactor * centerY;
                return true;
            }
        });

        invalidate();
    }

    public boolean onTouchEvent(MotionEvent me) {
        gestureDetector.onTouchEvent(me);
        return gestureDetector.isInProgress();
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
        return (TIME_LINE_TOTAL_COUNT - 1) * timeUnitHeight;
    }

    private void calculateTimeLinesArray() {
        timeLineModels.clear();
        String tip;
        float topOffset = getTopOffset();
        for (int i = 0; i < TIME_LINE_TOTAL_COUNT; i++) {
            if (i < 10) {
                tip = String.format(TIME_TEXT_FORMAT_HOUR, ("0" + i));
            } else {
                tip = String.format(TIME_TEXT_FORMAT_HOUR, i);
            }
            timeLineModels.add(new TimeLineModel(i * timeUnitHeight + topOffset, tip));
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
        checkDrawTimeWhenEventAdjusting(canvas);
        canvas.restoreToCount(save);
    }

    private void checkDrawTimeWhenEventAdjusting(Canvas canvas) {
        if (eventAdjusting) {
            long timeOnFloor = getTimeOnFloor(timeAdjust);
            String adjustingUnit = getAdjustingUnit(timeOnFloor);
            if (!TextUtils.isEmpty(adjustingUnit)) {
                float textWidth = timeTextP.measureText(adjustingUnit);
                float offsetY = getOffsetYByTime(timeOnFloor);
                canvas.drawText(adjustingUnit, lineStartX - textWidth, offsetY, timeTextP);
            }
        }
    }

    private long getTimeOnFloor(long timeAdjust) {//向下取每个单位(类似Math.floor())
        Calendar instance = Calendar.getInstance();
        instance.setTimeInMillis(timeAdjust);
        instance.clear(Calendar.SECOND);
        instance.clear(Calendar.MILLISECOND);
        int minute = instance.get(Calendar.MINUTE);
        if (15 < minute && minute < 30) {
            minute = 15;
        } else if (30 <= minute && minute < 45) {
            minute = 30;
        } else if (45 <= minute && minute < 60) {
            minute = 45;
        } else {
            minute = 0;
        }
        instance.set(Calendar.MINUTE, minute);
        return instance.getTimeInMillis();
    }

    private String getAdjustingUnit(long timeAdjust) {
        Calendar instance = Calendar.getInstance();
        instance.setTimeInMillis(timeAdjust);
        int minute = instance.get(Calendar.MINUTE);
        return minute != 0 ? String.format(":%s", minute) : null;
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

    private int getTotalMilliSecond() {
        return (TIME_LINE_TOTAL_COUNT - 1) * 60 * 60 * 1000;
    }

    private void drawCurTimeLine(Canvas canvas) {
        float totalSecond = getTotalMilliSecond() * 1f;
        Calendar instance = Calendar.getInstance();
        long curSecond = instance.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000 + instance.get(Calendar.MINUTE) * 60 * 1000 + instance.get(Calendar.SECOND) * 1000 + instance.get(Calendar.MILLISECOND);

        float yOffset = curSecond / totalSecond * getAllLineHeight() + getTopOffset();
        canvas.drawLine(lineStartX, yOffset, lineEndX, yOffset, curTimeP);
    }

    public RectF getRectOnTimeLine(long timeStampStart, long timeTaken) {
        float topOffset = getOffsetYByTime(timeStampStart);

        long timeStampEnd = timeStampStart + timeTaken;
        float bottomOffset = getOffsetYByTime(timeStampEnd);

        return new RectF(lineStartX, topOffset, lineEndX, bottomOffset);
    }

    private float getOffsetYByTime(long timeStamp) {
        return timeStamp * 1f / getTotalMilliSecond() * getAllLineHeight() + getTopOffset();
    }

    public long getTimeByOffsetY(float offSetY) {
        float radio = (offSetY - getTopOffset()) / getAllLineHeight();
        return (long) (radio * getTotalMilliSecond());
    }

    public long getTimeByDistance(float distance) {
        float radio = distance / getAllLineHeight();
        return (long) (radio * getTotalMilliSecond());
    }

    public void onEventAdjusting(long timeAdjust) {
        this.eventAdjusting = true;
        this.timeAdjust = timeAdjust;
    }

    public void onEventAdjustEnd() {
        this.eventAdjusting = false;
    }

    public interface TimeLineEventWatcher {
        void onScale();
    }
}
