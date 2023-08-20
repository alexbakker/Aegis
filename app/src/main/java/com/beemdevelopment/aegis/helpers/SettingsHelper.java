package com.beemdevelopment.aegis.helpers;

import android.content.Context;
import android.provider.Settings;

public class SettingsHelper {
    private SettingsHelper() {

    }

    public static double getAnimatorDurationScale(Context context) {
        try {
            return Settings.Global.getFloat(context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE);
        } catch (Settings.SettingNotFoundException e) {
            return 1.0f;
        }
    }
}
