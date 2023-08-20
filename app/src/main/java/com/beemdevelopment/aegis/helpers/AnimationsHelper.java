package com.beemdevelopment.aegis.helpers;

import android.content.Context;
import android.provider.Settings;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

public class AnimationsHelper {

    private static float getAnimationScale(Context context) {
        try {
            return Settings.Global.getFloat(context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE);
        } catch (Settings.SettingNotFoundException e) {
            return 1.0f;
        }
    }

    public static Animation loadScaledAnimation(Context context, int animationResId) {
        Animation animation = AnimationUtils.loadAnimation(context, animationResId);
        long newDuration = (long) (animation.getDuration() * getAnimationScale(context));
        animation.setDuration(newDuration);

        return animation;
    }

    public static LayoutAnimationController loadScaledLayoutAnimation(Context context, int animationResId) {
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(context, animationResId);
        animation.getAnimation().setDuration((long)(animation.getAnimation().getDuration() * getAnimationScale(context)));

        return animation;
    }
}