package io.git.zjoker.timelineeventview.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import io.git.zjoker.timelineeventview.ui.event.util.EventAdjustHelper;
import io.git.zjoker.timelineeventview.ui.event.util.EventHelper;
import io.git.zjoker.timelineeventview.util.DateUtil;

public class TimeLineEventViewPager extends ViewPager implements EventHelper.Callback {
    private int dayCount;
    private PagerAdapter adapter;
    private EventAdjustHelper adjustHelper;

    public TimeLineEventViewPager(@NonNull Context context) {
        super(context);
        init();
    }

    public TimeLineEventViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        dayCount = DateUtil.getCurrentMonthDay();
        adapter = new PagerAdapter() {
            @Override
            public int getCount() {
                return dayCount;
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                return view == object;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                TimeLineEventView timeLineEventView = new TimeLineEventView(getContext());
                container.addView(timeLineEventView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                return timeLineEventView;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View) object);
            }
        };
        setAdapter(adapter);
        adjustHelper = new EventAdjustHelper();
        adjustHelper.setEventAdjustListener(this);
        adjustHelper.attach(this);
    }

    public TimeLineEventView getCurrentView() {
        return null;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (adjustHelper.onTouchEvent(ev)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onEventAdjusting(long timeAdjust) {
        getCurrentView().onEventAdjusting(timeAdjust);
    }

    @Override
    public void onEventAdjustEnd() {
        getCurrentView().onEventAdjustEnd();
    }

    @Override
    public void onEventAdjustWithScroll(int scrollTo) {
        getCurrentView().onEventAdjustWithScroll(scrollTo);
    }
}
