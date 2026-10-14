/*
 * Copyright (C) 2026 Shinkai Project / WowoOS
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

package com.android.settings.display;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Interactive preview card demonstrating the smoothness of display refresh rates (e.g. 60Hz, 90Hz, 120Hz).
 * Features smooth continuous animated sliding pucks for each refresh tier and dynamically highlights
 * the currently selected refresh rate.
 */
public class RefreshRatePreviewView extends LinearLayout {

    public interface OnRefreshRateSelectedListener {
        void onRefreshRateSelected(int refreshRate);
    }

    private static class RowHolder {
        int refreshRate;
        LinearLayout rowContainer;
        TextView labelView;
        FrameLayout trackLayout;
        View puckView;
        ValueAnimator animator;
    }

    private final List<Integer> mRefreshRates = new ArrayList<>();
    private int mSelectedRefreshRate = 60;
    private OnRefreshRateSelectedListener mListener;

    private final List<RowHolder> mRowHolders = new ArrayList<>();
    private final List<View> mDividers = new ArrayList<>();
    private boolean mIsAnimating = false;

    public RefreshRatePreviewView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public RefreshRatePreviewView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RefreshRatePreviewView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        // Default refresh rate tiers matching common high-refresh displays
        mRefreshRates.addAll(Arrays.asList(60, 90, 120));
        mSelectedRefreshRate = 120;
        rebuildRows();
    }

    public void setOnRefreshRateSelectedListener(OnRefreshRateSelectedListener listener) {
        mListener = listener;
    }

    public void setRefreshRates(List<Integer> refreshRates) {
        if (refreshRates == null || refreshRates.isEmpty()) {
            return;
        }
        if (!mRefreshRates.equals(refreshRates)) {
            mRefreshRates.clear();
            mRefreshRates.addAll(refreshRates);
            rebuildRows();
        }
    }

    public void setSelectedRefreshRate(int refreshRate) {
        if (mSelectedRefreshRate != refreshRate) {
            mSelectedRefreshRate = refreshRate;
            updateRowStyles();
        }
    }

    private void rebuildRows() {
        stopAnimations();
        removeAllViews();
        mRowHolders.clear();
        mDividers.clear();

        final Context context = getContext();
        final int rowHeight = dpToPx(48);
        final int puckSize = dpToPx(20);

        for (int i = 0; i < mRefreshRates.size(); i++) {
            final int rate = mRefreshRates.get(i);

            final LinearLayout row = new LinearLayout(context);
            row.setOrientation(HORIZONTAL);
            row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, rowHeight));
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dpToPx(14), 0, dpToPx(14), 0);
            row.setClickable(true);
            row.setFocusable(true);

            final TextView label = new TextView(context);
            label.setText(String.format("%d Hz", rate));
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
            label.setSingleLine(true);
            final LayoutParams lpLabel = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            label.setMinimumWidth(dpToPx(64));
            label.setLayoutParams(lpLabel);

            final FrameLayout track = new FrameLayout(context);
            final LayoutParams lpTrack = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);
            track.setLayoutParams(lpTrack);
            track.setPadding(dpToPx(8), 0, dpToPx(8), 0);

            final View puck = new View(context);
            final FrameLayout.LayoutParams lpPuck = new FrameLayout.LayoutParams(puckSize, puckSize);
            lpPuck.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            puck.setLayoutParams(lpPuck);
            track.addView(puck);

            row.addView(label);
            row.addView(track);

            row.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                setSelectedRefreshRate(rate);
                if (mListener != null) {
                    mListener.onRefreshRateSelected(rate);
                }
            });

            addView(row);

            final RowHolder holder = new RowHolder();
            holder.refreshRate = rate;
            holder.rowContainer = row;
            holder.labelView = label;
            holder.trackLayout = track;
            holder.puckView = puck;
            mRowHolders.add(holder);

            if (i < mRefreshRates.size() - 1) {
                final View divider = new View(context);
                final LayoutParams lpDiv = new LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(1));
                lpDiv.setMargins(dpToPx(14), 0, dpToPx(14), 0);
                divider.setLayoutParams(lpDiv);
                divider.setBackgroundColor(Color.parseColor("#1E2420"));
                addView(divider);
                mDividers.add(divider);
            }
        }

        updateRowStyles();
        setupAnimators();

        if (isAttachedToWindow()) {
            startAnimations();
        }
    }

    private void updateRowStyles() {
        int selectedIdx = -1;
        for (int i = 0; i < mRowHolders.size(); i++) {
            final RowHolder holder = mRowHolders.get(i);
            final boolean isSelected = (holder.refreshRate == mSelectedRefreshRate);
            if (isSelected) {
                selectedIdx = i;
                holder.rowContainer.setBackgroundResource(R.drawable.refresh_rate_preview_selected_pill_bg);
                holder.labelView.setTextColor(Color.parseColor("#6BD598"));
                holder.labelView.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
                holder.puckView.setBackgroundResource(R.drawable.refresh_rate_preview_puck_green);
            } else {
                holder.rowContainer.setBackground(null);
                holder.labelView.setTextColor(Color.parseColor("#EEEEEE"));
                holder.labelView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                holder.puckView.setBackgroundResource(R.drawable.refresh_rate_preview_puck_grey);
            }
        }

        // Hide divider adjacent to the selected row container for a clean floating pill effect
        for (int i = 0; i < mDividers.size(); i++) {
            final View divider = mDividers.get(i);
            if (i == selectedIdx - 1 || i == selectedIdx) {
                divider.setVisibility(View.INVISIBLE);
            } else {
                divider.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupAnimators() {
        for (RowHolder holder : mRowHolders) {
            final long duration;
            if (holder.refreshRate == 60) {
                duration = 2200L;
            } else if (holder.refreshRate == 90) {
                duration = 1500L;
            } else if (holder.refreshRate == 120) {
                duration = 1100L;
            } else {
                duration = Math.max(800L, Math.min(3000L, Math.round(2200f * 60f / holder.refreshRate)));
            }

            final ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(duration);
            anim.setRepeatMode(ValueAnimator.REVERSE);
            anim.setRepeatCount(ValueAnimator.INFINITE);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());

            final RowHolder rowHolder = holder;
            anim.addUpdateListener(animation -> {
                final float fraction = (float) animation.getAnimatedValue();
                final int trackWidth = rowHolder.trackLayout.getWidth();
                final int puckWidth = rowHolder.puckView.getWidth();
                final int padStart = rowHolder.trackLayout.getPaddingStart();
                final int padEnd = rowHolder.trackLayout.getPaddingEnd();
                final int maxTravel = trackWidth - puckWidth - padStart - padEnd;
                if (maxTravel > 0) {
                    rowHolder.puckView.setTranslationX(fraction * maxTravel);
                }
            });

            holder.animator = anim;

            holder.trackLayout.addOnLayoutChangeListener((v, left, top, right, bottom, oldL, oldT, oldR, oldB) -> {
                if (right - left > 0 && mIsAnimating && !anim.isStarted()) {
                    anim.start();
                }
            });
        }
    }

    public void startAnimations() {
        mIsAnimating = true;
        for (RowHolder holder : mRowHolders) {
            if (holder.animator != null && holder.trackLayout.getWidth() > 0) {
                if (!holder.animator.isStarted()) {
                    holder.animator.start();
                } else if (holder.animator.isPaused()) {
                    holder.animator.resume();
                }
            }
        }
    }

    public void stopAnimations() {
        mIsAnimating = false;
        for (RowHolder holder : mRowHolders) {
            if (holder.animator != null && holder.animator.isStarted()) {
                holder.animator.pause();
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimations();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimations();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == View.VISIBLE && isAttachedToWindow()) {
            startAnimations();
        } else {
            stopAnimations();
        }
    }

    @Override
    public void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == View.VISIBLE && isAttachedToWindow()) {
            startAnimations();
        } else {
            stopAnimations();
        }
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }
}
