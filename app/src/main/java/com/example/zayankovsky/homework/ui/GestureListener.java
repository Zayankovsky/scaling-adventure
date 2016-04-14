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

package com.example.zayankovsky.homework.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

class GestureListener extends GestureDetector.SimpleOnGestureListener {

    private static Activity mActivity;
    private static ImageView mImageView;
    private static float mMidX, mMidY, mLeft, mRight, mTop, mBottom;

    private static void updateImageCoordinates() {
        mMidX = (mImageView.getLeft() + mImageView.getRight()) / 2.f + mImageView.getTranslationX();
        mMidY = (mImageView.getTop() + mImageView.getBottom()) / 2.f + mImageView.getTranslationY();

        float halfWidth = (mImageView.getRight() - mImageView.getLeft()) / 2.f * mImageView.getScaleX();
        float halfHeight = (mImageView.getBottom() - mImageView.getTop()) / 2.f * mImageView.getScaleY();
        float realHalfHeight;

        BitmapDrawable drawable = (BitmapDrawable) mImageView.getDrawable();
        if (drawable != null) {
            Bitmap bitmap = drawable.getBitmap();
            if ((realHalfHeight = halfWidth * bitmap.getHeight() / bitmap.getWidth()) > halfHeight) {
                halfWidth = halfHeight * bitmap.getWidth() / bitmap.getHeight();
            } else {
                halfHeight = realHalfHeight;
            }
        } else {
            halfHeight = 0;
            halfWidth = 0;
        }

        mLeft = mMidX - halfWidth;
        mRight = mMidX + halfWidth;

        mTop = mMidY - halfHeight;
        mBottom = mMidY + halfHeight;
    }

    public static void init(Activity activity, ImageView imageView) {
        mActivity = activity;
        mImageView = imageView;
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

    @Override
    public void onLongPress(MotionEvent e) {
        // Touch has been long enough to indicate a long press.
        // Does not indicate motion is complete yet (no up event necessarily)
        float x = e.getX();
        float y = e.getY();
        updateImageCoordinates();

        if (mLeft <= x && x <= mRight && mTop <= y && y <= mBottom) {
            mActivity.openContextMenu(mImageView);
        }
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        // User attempted to scroll
        mImageView.setTranslationX(mImageView.getTranslationX() - distanceX);
        mImageView.setTranslationY(mImageView.getTranslationY() - distanceY);
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
            if ((mImageView.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
                mImageView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            } else {
                mImageView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
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
