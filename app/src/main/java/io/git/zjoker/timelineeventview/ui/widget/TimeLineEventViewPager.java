package io.git.zjoker.timelineeventview.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import io.git.zjoker.timelineeventview.ui.event.model.Event;
import io.git.zjoker.timelineeventview.ui.event.util.EventAdjustHelper;
import io.git.zjoker.timelineeventview.util.DateUtil;

public class TimeLineEventViewPager extends ViewPager implements EventAdjustHelper.Callback {
    private PagerAdapter adapter;
    private EventAdjustHelper adjustHelper;
    private TimeLineEventView curView;
    private SparseArray<Set<Event>> allEvents;
    public TimeLineEventViewPager(@NonNull Context context) {
        super(context);
        init();
    }

    public TimeLineEventViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void buildEventMap() {
        int dayCount = DateUtil.getCurrentMonthDay();
        allEvents = new SparseArray<>();
        for (int i = 0; i < dayCount; i++) {
            allEvents.put(i + 1, new HashSet<Event>());
        }
    }

    private void init() {
        buildEventMap();

        adapter = new PagerAdapter() {
            @Override
            public int getCount() {
                return allEvents.size();
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
                timeLineEventView.notifyEvents(allEvents.get(position + 1));
                return timeLineEventView;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View) object);
            }

            @Override
            public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                super.setPrimaryItem(container, position, object);
                curView = (TimeLineEventView) object;
                curView.notifyEvents(allEvents.get(position + 1));
            }
        };

        setAdapter(adapter);
        adjustHelper = new EventAdjustHelper();
        adjustHelper.setEventAdjustListener(this);

        adjustHelper.attach(this);
    }

    public TimeLineEventView getCurrentView() {
        return curView;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (adjustHelper.onTouchEvent(ev)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        adjustHelper.draw(canvas);
    }

    public int getCurrentPosition() {
        return getCurrentItem();
    }

    @Override
    public void onEventCreated(Event newEvent) {
        int day = getCurrentPosition() + 1;
        Set<Event> events = allEvents.get(day);
        events.add(newEvent);
        curView.notifyEvents(events);
    }

    @Override
    public void onEventAdjustEnd(Event newEvent) {
        int day = getCurrentPosition() + 1;
        Set<Event> events = allEvents.get(day);
        events.remove(newEvent);
        events.add(newEvent);
        curView.notifyEvents(events);
    }

    @Override
    public void onEventCrossDay(Event event, int fromPosition, int toPosition) {
        if (toPosition < 0 || toPosition > allEvents.size() - 1) {
            return;
        }
        int fromDay = fromPosition + 1;
        int toDay = toPosition + 1;
        allEvents.get(fromDay).remove(event);
        allEvents.get(toDay).add(event);
        curView.notifyEvents(allEvents.get(fromDay));
        setCurrentItem(toPosition);
    }
}
