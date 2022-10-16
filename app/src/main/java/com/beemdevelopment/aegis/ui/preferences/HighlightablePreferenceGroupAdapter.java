/*
 * Based on: https://android.googlesource.com/platform/packages/apps/Settings/+/master/src/com/android/settings/widget/HighlightablePreferenceGroupAdapter.java
 *
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beemdevelopment.aegis.ui.preferences;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.ThemeHelper;

@SuppressLint("RestrictedApi")
public class HighlightablePreferenceGroupAdapter extends PreferenceGroupAdapter {
    @VisibleForTesting
    static final long DELAY_COLLAPSE_DURATION_MILLIS = 300L;

    @VisibleForTesting
    static final long DELAY_HIGHLIGHT_DURATION_MILLIS = 600L;
    private static final long HIGHLIGHT_DURATION = 2000L;
    private static final long HIGHLIGHT_FADE_OUT_DURATION = 500L;
    private static final long HIGHLIGHT_FADE_IN_DURATION = 200L;

    @VisibleForTesting
    final int mHighlightColor;
    @VisibleForTesting
    boolean mFadeInAnimated;

    private final int mNormalBackgroundRes;
    private final String mHighlightKey;
    private boolean mHighlightRequested;
    private int mHighlightPosition = RecyclerView.NO_POSITION;

    public HighlightablePreferenceGroupAdapter(PreferenceGroup preferenceGroup, String key,
                                               boolean highlightRequested) {
        super(preferenceGroup);
        mHighlightKey = key;
        mHighlightRequested = highlightRequested;
        final Context context = preferenceGroup.getContext();
        final TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground,
                outValue, true /* resolveRefs */);
        mNormalBackgroundRes = outValue.resourceId;
        mHighlightColor = context.getColor(R.color.card_background_focused_dark);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        updateBackground(holder, position);
    }

    @VisibleForTesting
    void updateBackground(PreferenceViewHolder holder, int position) {
        View v = holder.itemView;
        if (position == mHighlightPosition
                && (mHighlightKey != null
                && TextUtils.equals(mHighlightKey, getItem(position).getKey()))) {
            // This position should be highlighted. If it's highlighted before - skip animation.
            addHighlightBackground(holder, !mFadeInAnimated);
        } else if (Boolean.TRUE.equals(v.getTag(R.id.preference_highlighted))) {
            // View with highlight is reused for a view that should not have highlight
            removeHighlightBackground(holder, false /* animate */);
        }
    }

    public void requestHighlight(View root, RecyclerView recyclerView) {
        if (mHighlightRequested || recyclerView == null || TextUtils.isEmpty(mHighlightKey)) {
            return;
        }
        final int position = getPreferenceAdapterPosition(mHighlightKey);
        if (position < 0) {
            return;
        }

        // Remove the animator as early as possible to avoid a RecyclerView crash.
        recyclerView.setItemAnimator(null);
        // Scroll to correct position after 600 milliseconds.
        root.postDelayed(() -> {
            mHighlightRequested = true;
            recyclerView.smoothScrollToPosition(position);
            mHighlightPosition = position;
        }, DELAY_HIGHLIGHT_DURATION_MILLIS);

        // Highlight preference after 900 milliseconds.
        root.postDelayed(() -> {
            notifyItemChanged(position);
        }, DELAY_COLLAPSE_DURATION_MILLIS + DELAY_HIGHLIGHT_DURATION_MILLIS);
    }

    @VisibleForTesting
    void requestRemoveHighlightDelayed(PreferenceViewHolder holder) {
        final View v = holder.itemView;
        v.postDelayed(() -> {
            mHighlightPosition = RecyclerView.NO_POSITION;
            removeHighlightBackground(holder, true /* animate */);
        }, HIGHLIGHT_DURATION);
    }

    private void addHighlightBackground(PreferenceViewHolder holder, boolean animate) {
        final View v = holder.itemView;
        v.setTag(R.id.preference_highlighted, true);
        if (!animate) {
            v.setBackgroundColor(mHighlightColor);
            requestRemoveHighlightDelayed(holder);
            return;
        }
        mFadeInAnimated = true;
        final int colorFrom = mNormalBackgroundRes;
        final int colorTo = mHighlightColor;
        final ValueAnimator fadeInLoop = ValueAnimator.ofObject(
                new ArgbEvaluator(), colorFrom, colorTo);
        fadeInLoop.setDuration(HIGHLIGHT_FADE_IN_DURATION);
        fadeInLoop.addUpdateListener(
                animator -> v.setBackgroundColor((int) animator.getAnimatedValue()));
        fadeInLoop.setRepeatMode(ValueAnimator.REVERSE);
        fadeInLoop.setRepeatCount(4);
        fadeInLoop.start();
        holder.setIsRecyclable(false);
        requestRemoveHighlightDelayed(holder);
    }

    private void removeHighlightBackground(PreferenceViewHolder holder, boolean animate) {
        final View v = holder.itemView;
        if (!animate) {
            v.setTag(R.id.preference_highlighted, false);
            v.setBackgroundResource(mNormalBackgroundRes);
            return;
        }

        if (!Boolean.TRUE.equals(v.getTag(R.id.preference_highlighted))) {
            return;
        }

        v.setTag(R.id.preference_highlighted, false);
        final ValueAnimator colorAnimation = ValueAnimator.ofObject(
                new ArgbEvaluator(), mHighlightColor, mNormalBackgroundRes);
        colorAnimation.setDuration(HIGHLIGHT_FADE_OUT_DURATION);
        colorAnimation.addUpdateListener(
                animator -> v.setBackgroundColor((int) animator.getAnimatedValue()));
        colorAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Animation complete - the background is now white. Change to mNormalBackgroundRes
                // so it is white and has ripple on touch.
                v.setBackgroundResource(mNormalBackgroundRes);
                holder.setIsRecyclable(true);
            }
        });
        colorAnimation.start();
    }
}