package io.git.zjoker.timelinerecyclerview.util;

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import io.git.zjoker.timelinerecyclerview.App;

public class ViewUtil {
    public static float dpToPx(int dp) {
        DisplayMetrics dm = App.appContext.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, dm);
    }

    public static float spToPx(int sp, Resources resources) {
        DisplayMetrics dm = App.appContext.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, dm);
    }
}
