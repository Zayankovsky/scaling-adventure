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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * This fragment will populate the children of the ViewPager from {@link ImageDetailActivity}.
 */
public class ImageDetailFragment extends Fragment {
    private static final String POSITION = "position";
    private int position;
    private ImageView mImageView;

    /**
     * Factory method to generate a new instance of the fragment given an image number.
     *
     * @param position The position of the ImageView resource to load the image resource into
     * @return A new instance of ImageDetailFragment
     */
    public static ImageDetailFragment newInstance(int position) {
        final ImageDetailFragment f = new ImageDetailFragment();

        final Bundle args = new Bundle();
        args.putInt(POSITION, position);
        f.setArguments(args);

        return f;
    }

    /**
     * Empty constructor as per the Fragment documentation
     */
    public ImageDetailFragment() {}

    /**
     * Populate image using an ImageView position, use the convenience factory method
     * {@link ImageDetailFragment#newInstance(int)} to create this fragment.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            position = getArguments().getInt(POSITION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate and locate the main ImageView
        final View v = inflater.inflate(R.layout.image_detail_fragment, container, false);
        mImageView = (ImageView) v.findViewById(R.id.imageView);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Use the ImageWorker to load the image into the ImageView
        // (so a single cache can be used over all pages in the ViewPager)
        if (ImageDetailActivity.class.isInstance(getActivity())) {
            ImageWorker.loadImage(position, mImageView);
        }

        // First we create the GestureListener that will include all our callbacks.
        // Then we create the GestureDetector, which takes that listener as an argument.
        GestureDetector.SimpleOnGestureListener gestureListener = new GestureListener();
        final GestureDetector gd = new GestureDetector(getActivity(), gestureListener);

        ScaleGestureDetector.SimpleOnScaleGestureListener scaleGestureListener = new ScaleGestureListener();
        final ScaleGestureDetector sgd = new ScaleGestureDetector(getActivity(), scaleGestureListener);

        /* For the view where gestures will occur, we create an onTouchListener that sends
         * all motion events to the gesture detectors.  When the gesture detectors
         * actually detects an event, it will use the callbacks we created in the
         * SimpleOnGestureListener and SimpleOnScaleGestureListener to alert our application.
        */

        mImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                GestureListener.init(mImageView);
                gd.onTouchEvent(motionEvent);

                ScaleGestureListener.init(mImageView);
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
