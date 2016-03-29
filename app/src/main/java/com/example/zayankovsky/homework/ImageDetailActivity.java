/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.example.zayankovsky.homework;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

public class ImageDetailActivity extends FragmentActivity {
    public static final String SECTION_NUMBER = "section_number";
    public static final String POSITION = "position";
    public static final String THUMBNAIL_BITMAP = "thumbnail_bitmap";
    private ImageView mImageView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_detail);
        getWindow().getDecorView().setBackgroundResource(
                PreferenceManager.getDefaultSharedPreferences(this).getString("theme", "light").equals("dark") ?
                        R.color.darkColorTransparent : R.color.lightColorTransparent
        );

        // Set up activity to go full screen
        getWindow().addFlags(LayoutParams.FLAG_FULLSCREEN);

        // Locate the main ImageView
        mImageView = (ImageView) findViewById(R.id.imageView);

        GestureListener.init(this, mImageView);
        ScaleGestureListener.init(mImageView);

        // Use the ImageWorker to load the image into the ImageView
        // (so a single cache can be used over all pages in the ViewPager)
        // based on the extra passed in to this activity
        int position = getIntent().getIntExtra(POSITION, 0);
        if (getIntent().getIntExtra(SECTION_NUMBER, 0) == 1 && ImageWorker.getNumberOfUrls() > 0) {
            mImageView.setImageBitmap((Bitmap) getIntent().getParcelableExtra(THUMBNAIL_BITMAP));
            ImageWorker.loadYandexImage(position, mImageView);
        } else {
            ImageWorker.loadImage(position, mImageView);
        }

        // First we create the GestureListener that will include all our callbacks.
        // Then we create the GestureDetector, which takes that listener as an argument.
        GestureDetector.SimpleOnGestureListener gestureListener = new GestureListener();
        final GestureDetector gd = new GestureDetector(this, gestureListener);

        ScaleGestureDetector.SimpleOnScaleGestureListener scaleGestureListener = new ScaleGestureListener();
        final ScaleGestureDetector sgd = new ScaleGestureDetector(this, scaleGestureListener);

        /* For the view where gestures will occur, we create an onTouchListener that sends
         * all motion events to the gesture detectors.  When the gesture detectors
         * actually detects an event, it will use the callbacks we created in the
         * SimpleOnGestureListener and SimpleOnScaleGestureListener to alert our application.
        */

        findViewById(R.id.frameLayout).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gd.onTouchEvent(motionEvent);
                sgd.onTouchEvent(motionEvent);
                return true;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mImageView != null) {
            mImageView.setImageDrawable(null);
        }
    }
}
