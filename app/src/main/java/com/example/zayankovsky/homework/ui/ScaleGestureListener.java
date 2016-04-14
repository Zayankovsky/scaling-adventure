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

import android.view.ScaleGestureDetector;
import android.widget.ImageView;

class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

    private static ImageView mImageView;

    static void init(ImageView imageView) {
        mImageView = imageView;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scaleFactor = detector.getScaleFactor();

        if (scaleFactor == 1) {
            return true;
        }

        float scaleX = mImageView.getScaleX() * scaleFactor;
        float scaleY = mImageView.getScaleY() * scaleFactor;

        // Don't let the object get too large or too small.
        if (scaleFactor > 1 && Math.max(scaleX, scaleY) > 5 || scaleFactor < 1 && Math.min(scaleX, scaleY) < .2) {
            return true;
        }

        mImageView.setScaleX(scaleX);
        mImageView.setScaleY(scaleY);

        mImageView.setTranslationX(mImageView.getTranslationX() * scaleFactor);
        mImageView.setTranslationY(mImageView.getTranslationY() * scaleFactor);

        return true;
    }

}
