package io.git.zjoker.timelineeventview.util;

import java.util.Calendar;

public class DateUtil {
    /**
     *      * 获取当月的 天数
     *     
     */

    public static int getCurrentMonthDay() {
        Calendar a = Calendar.getInstance();
        a.set(Calendar.DATE, 1);
        a.roll(Calendar.DATE, -1);
        int maxDate = a.get(Calendar.DATE);
        return maxDate;
    }
}
