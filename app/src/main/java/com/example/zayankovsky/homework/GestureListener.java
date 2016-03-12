/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.zayankovsky.homework;

import android.app.Activity;
import android.support.v4.view.ViewPager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class GestureListener extends GestureDetector.SimpleOnGestureListener {

    private static Activity mActivity;
    private static ViewPager mPager;
    private static ImageView mImageView;
    private static float mMidX, mMidY, mLeft, mRight, mTop, mBottom;

    private static void updateImageCoordinates() {
        mMidX = mImageView.getWidth() / 2.f;
        mMidY = mImageView.getHeight() / 2.f;
        float halfSize = Math.min(mMidX, mMidY);

        mLeft = mMidX - halfSize;
        mRight = mMidX + halfSize;

        mTop = mMidY - halfSize;
        mBottom = mMidY + halfSize;
    }

    public static void init(Activity activity, ViewPager pager) {
        mActivity = activity;
        mPager = pager;
    }

    public static void init(ImageView imageView) {
        mImageView = imageView;
    }

    public static void reset() {
        if (mImageView != null) {
            mImageView.setScaleX(1);
            mImageView.setScaleY(1);

            mImageView.setTranslationX(0);
            mImageView.setTranslationY(0);
        }
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        // Up motion completing a single tap occurred.
        float x = e.getX();
        float y = e.getY();
        updateImageCoordinates();

        if (x < mLeft || mRight < x || y < mTop || mBottom < y) {
            mActivity.finish();
        }
        return true;
    }

    /**
     * Set on the ImageView in the ViewPager children fragments,
     * to enable/disable low profile mode when the ImageView is touched.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        // A confirmed single-tap event has occurred.  Only called when the detector has
        // determined that the first tap stands alone, and is not part of a double tap.
        float x = e.getX();
        float y = e.getY();
        updateImageCoordinates();

        if (mLeft <= x && x <= mRight && mTop <= y && y <= mBottom) {
            if ((mPager.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
                mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            } else {
                mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            }
        }
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        // User tapped the screen twice.
        float x = e.getX();
        float y = e.getY();
        updateImageCoordinates();

        if (mLeft <= x && x <= mRight && mTop <= y && y <= mBottom) {
            if (mImageView.getScaleX() == 1 && mImageView.getScaleY() == 1) {
                mImageView.setScaleX(2);
                mImageView.setScaleY(2);

                mImageView.setTranslationX(mImageView.getTranslationX() + mMidX - x);
                mImageView.setTranslationY(mImageView.getTranslationY() + mMidY - y);
            } else {
                mImageView.setScaleX(1);
                mImageView.setScaleY(1);

                mImageView.setTranslationX(0);
                mImageView.setTranslationY(0);
            }
        }

        return true;
    }

}
