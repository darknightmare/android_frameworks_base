/*
 * Copyright (c) 2012-2013 The Linux Foundation. All rights reserved.
 * Not a Contribution.
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.BarBackgroundUpdater;
import com.android.systemui.statusbar.policy.NetworkController;

import java.util.ArrayList;

// Intimately tied to the design of res/layout/signal_cluster_view.xml
public class SignalClusterView extends LinearLayout
        implements NetworkController.SignalCluster {

    static final boolean DEBUG = false;
    static final String TAG = "SignalClusterView";

    NetworkController mNC;

    private boolean mWifiVisible = false;
    private int mWifiStrengthId = 0;
    private boolean mMobileVisible = false;
    private int mMobileStrengthId = 0, mMobileActivityId = 0;
    private int mMobileTypeId = 0, mNoSimIconId = 0;
    private boolean mIsAirplaneMode = false;
    private int mAirplaneIconId = 0;
    private String mWifiDescription, mMobileDescription, mMobileTypeDescription;

    ViewGroup mWifiGroup, mMobileGroup;
    ImageView mWifi, mMobile, mMobileType, mAirplane, mNoSimSlot;
    View mSpacer;

    private final Handler mHandler;
    private final int mDSBDuration;
    private int mPreviousOverrideIconColor = 0;
    private int mOverrideIconColor = 0;

    public SignalClusterView(Context context) {
        this(context, null);
    }

    public SignalClusterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignalClusterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mHandler = new Handler();
        mDSBDuration = context.getResources().getInteger(R.integer.dsb_transition_duration);
        BarBackgroundUpdater.addListener(new BarBackgroundUpdater.UpdateListener(this) {

            @Override
            public AnimatorSet onUpdateStatusBarIconColor(final int previousIconColor,
                    final int iconColor) {
                mPreviousOverrideIconColor = previousIconColor;
                mOverrideIconColor = iconColor;

                if (mOverrideIconColor == 0) {
                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            if (mWifi != null) {
                                mWifi.setColorFilter(null);
                            }
                            if (mMobile != null) {
                                mMobile.setColorFilter(null);
                            }
                            if (mMobileType != null) {
                                mMobileType.setColorFilter(null);
                            }
                            if (mAirplane != null) {
                                mAirplane.setColorFilter(null);
                            }
                        }

                    });

                    return null;
                } else {
                    final ArrayList<Animator> anims = new ArrayList<Animator>();

                    if (mWifi != null) {
                        anims.add(buildAnimator(mWifi));
                    }
                    if (mMobile != null) {
                        anims.add(buildAnimator(mMobile));
                    }
                    if (mMobileType != null) {
                        anims.add(buildAnimator(mMobileType));
                    }
                    if (mAirplane != null) {
                        anims.add(buildAnimator(mAirplane));
                    }

                    if (anims.isEmpty()) {
                        return null;
                    } else {
                        final AnimatorSet animSet = new AnimatorSet();
                        animSet.playTogether(anims);
                        return animSet;
                    }
                }
            }

        });
    }

    private ObjectAnimator buildAnimator(final ImageView target) {
        final ObjectAnimator animator = ObjectAnimator.ofObject(target, "colorFilter",
                new ArgbEvaluator(), mPreviousOverrideIconColor, mOverrideIconColor);
        animator.setDuration(mDSBDuration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(final ValueAnimator anim) {
                target.invalidate();
            }

        });
        return animator;
    }

    public void setNetworkController(NetworkController nc) {
        if (DEBUG) Log.d(TAG, "NetworkController=" + nc);
        mNC = nc;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mWifiGroup      = (ViewGroup) findViewById(R.id.wifi_combo);
        mWifi           = (ImageView) findViewById(R.id.wifi_signal);
        mMobileGroup    = (ViewGroup) findViewById(R.id.mobile_combo);
        mMobile         = (ImageView) findViewById(R.id.mobile_signal);
        mMobileType     = (ImageView) findViewById(R.id.mobile_type);
        mNoSimSlot      = (ImageView) findViewById(R.id.no_sim);
        mSpacer         =             findViewById(R.id.spacer);
        mAirplane       = (ImageView) findViewById(R.id.airplane);

        apply();
    }

    @Override
    protected void onDetachedFromWindow() {
        mWifiGroup      = null;
        mWifi           = null;
        mMobileGroup    = null;
        mMobile         = null;
        mMobileType     = null;
        mNoSimSlot      = null;
        mSpacer         = null;
        mAirplane       = null;

        super.onDetachedFromWindow();
    }

    @Override
    public void setWifiIndicators(boolean visible, int strengthIcon, String contentDescription) {
        mWifiVisible = visible;
        mWifiStrengthId = strengthIcon;
        mWifiDescription = contentDescription;

        apply();
    }

    @Override
    public void setMobileDataIndicators(boolean visible, int strengthIcon,
            int typeIcon, String contentDescription, String typeContentDescription,
            int noSimIcon) {
        mMobileVisible = visible;
        mMobileStrengthId = strengthIcon;
        mMobileTypeId = typeIcon;
        mMobileDescription = contentDescription;
        mMobileTypeDescription = typeContentDescription;
        mNoSimIconId = noSimIcon;

        apply();
    }

    @Override
    public void setIsAirplaneMode(boolean is, int airplaneIconId) {
        mIsAirplaneMode = is;
        mAirplaneIconId = airplaneIconId;

        apply();
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        // Standard group layout onPopulateAccessibilityEvent() implementations
        // ignore content description, so populate manually
        if (mWifiVisible && mWifiGroup != null && mWifiGroup.getContentDescription() != null)
            event.getText().add(mWifiGroup.getContentDescription());
        if (mMobileVisible && mMobileGroup != null && mMobileGroup.getContentDescription() != null)
            event.getText().add(mMobileGroup.getContentDescription());
        return super.dispatchPopulateAccessibilityEvent(event);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);

        if (mWifi != null) {
            mWifi.setImageDrawable(null);
        }

        if (mMobile != null) {
            mMobile.setImageDrawable(null);
        }

        if (mMobileType != null) {
            mMobileType.setImageDrawable(null);
        }

        if(mAirplane != null) {
            mAirplane.setImageDrawable(null);
        }

        apply();
    }

    // Run after each indicator change.
    private void apply() {
        if (mWifiGroup == null) return;

        if (mWifiVisible) {
            mWifi.setImageResource(mWifiStrengthId);

            mWifiGroup.setContentDescription(mWifiDescription);
            mWifiGroup.setVisibility(View.VISIBLE);
        } else {
            mWifiGroup.setVisibility(View.GONE);
        }

        if (DEBUG) Log.d(TAG,
                String.format("wifi: %s sig=%d",
                    (mWifiVisible ? "VISIBLE" : "GONE"),
                    mWifiStrengthId));

        if (mMobileVisible && !mIsAirplaneMode) {
            mMobile.setImageResource(mMobileStrengthId);
            mMobileType.setImageResource(mMobileTypeId);

            mMobileGroup.setContentDescription(mMobileTypeDescription + " " + mMobileDescription);
            mMobileGroup.setVisibility(View.VISIBLE);
            mNoSimSlot.setImageResource(mNoSimIconId);
        } else {
            mMobileGroup.setVisibility(View.GONE);
        }

        if (mIsAirplaneMode) {
            mAirplane.setImageResource(mAirplaneIconId);
            mAirplane.setVisibility(View.VISIBLE);
        } else {
            mAirplane.setVisibility(View.GONE);
        }

        if (mMobileVisible && mWifiVisible &&
                ((mIsAirplaneMode) || (mNoSimIconId != 0))) {
            mSpacer.setVisibility(View.INVISIBLE);
        } else {
            mSpacer.setVisibility(View.GONE);
        }

        if (DEBUG) Log.d(TAG,
                String.format("mobile: %s sig=%d typ=%d",
                    (mMobileVisible ? "VISIBLE" : "GONE"),
                    mMobileStrengthId, mMobileTypeId));

        mMobileType.setVisibility(
                !mWifiVisible ? View.VISIBLE : View.GONE);
    }

}
