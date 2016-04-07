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

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ImageDetailActivity extends AppCompatActivity {
    public static final String SECTION_NUMBER = "section_number";
    public static final String POSITION = "position";
    private ImageView mImageView;

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 200;

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

        // Locate the main ImageView and TextView
        mImageView = (ImageView) findViewById(R.id.imageView);
        TextView mTextView = (TextView) findViewById(R.id.textView);

        // Enable some additional newer visibility and ActionBar features
        // to create a more immersive photo viewing experience
        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            // Set home as up
            actionBar.setDisplayHomeAsUpEnabled(true);

            // Hide and show the ActionBar as the visibility changes
            mImageView.setOnSystemUiVisibilityChangeListener(
                    new View.OnSystemUiVisibilityChangeListener() {
                        @Override
                        public void onSystemUiVisibilityChange(int vis) {
                            if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
                                actionBar.hide();
                            } else {
                                actionBar.show();
                            }
                        }
                    });

            // Start low profile mode and hide ActionBar
            mImageView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            actionBar.hide();
        }

        GestureListener.init(this, mImageView);
        ScaleGestureListener.init(mImageView);

        // Use the ImageWorker to load the image into the ImageView
        // (so a single cache can be used over all pages in the ViewPager)
        // based on the extra passed in to this activity
        int position = getIntent().getIntExtra(POSITION, 0);
        switch (getIntent().getIntExtra(SECTION_NUMBER, 0)) {
            case 0:
                ImageWorker.loadGalleryImage(position, mImageView);
                break;
            case 1:
                ImageWorker.loadFotkiImage(position, mImageView);
                registerForContextMenu(mImageView);
                mImageView.setLongClickable(false);
                mTextView.setText("Author: " + ImageWorker.getAuthor() + "\n" + "Published: " +
                        new SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(ImageWorker.getPublished()));
                break;
            case 2:
                ImageWorker.loadImage(position, mImageView);
                break;
        }
        setTitle(ImageWorker.getTitle());

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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
                    );
                } else {
                    saveImageToGallery();
                }
                return true;
            case R.id.open:
                Uri webpage = Uri.parse(ImageWorker.getUrl());
                Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay!
                saveImageToGallery();
            }
        }
    }

    private void saveImageToGallery() {
        BitmapDrawable drawable = (BitmapDrawable) mImageView.getDrawable();
        if (drawable != null) {
            String title = ImageWorker.getTitle();
            String url = MediaStore.Images.Media.insertImage(
                    getContentResolver(), drawable.getBitmap(), title, null
            );
            ImageWorker.addToGallery(title, Uri.parse(url));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mImageView != null) {
            mImageView.setImageDrawable(null);
        }
    }
}
