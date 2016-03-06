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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.SparseArray;
import android.widget.ImageView;

import java.util.HashMap;

/**
 * This class wraps up completing some arbitrary work when loading a bitmap to an ImageView.
 * It handles things like using a memory cache and scaling bitmaps.
 */
public class ImageWorker {

    private static Resources mResources;
    private static int mScreenWidth;
    private static int mScreenDensity;

    private static int [] imageIds = {
            R.drawable.image_1, R.drawable.image_2, R.drawable.image_3,
            R.drawable.image_4, R.drawable.image_5, R.drawable.image_6,
    };

    private static HashMap<Integer, SparseArray<Bitmap>> thumbnailCaches = new HashMap<>();
    private static SparseArray<Bitmap> imageCache = new SparseArray<>();

    public static void init(Resources resources, int screenWidth, int screenDensity) {
        mResources = resources;
        mScreenWidth = screenWidth;
        mScreenDensity = screenDensity;
    }

    public static void loadThumbnail(int columnCount, int position, ImageView imageView) {
        SparseArray<Bitmap> thumbnailCache = thumbnailCaches.get(columnCount);

        if (thumbnailCache == null) {
            thumbnailCache = new SparseArray<>();
            thumbnailCaches.put(columnCount, thumbnailCache);
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2 + (columnCount < 4 ? 0 : 2);
        getFromCacheOrResources(thumbnailCache, imageView, position, options, mScreenWidth / columnCount);
    }

    public static void loadImage(int position, ImageView imageView) {
        getFromCacheOrResources(imageCache, imageView, position, new BitmapFactory.Options(), 0);
    }

    /**
     * Return an image specified by the position parameter.
     * If the image is found in the memory cache, it is returned immediately, otherwise
     * {@link BitmapFactory::decodeResource} will be called to load the bitmap.
     *
     * @param position The position of the ImageView to bind the image to.
     */
    private static void getFromCacheOrResources(SparseArray<Bitmap> cache, ImageView imageView,
                                                int position, BitmapFactory.Options options, int size) {
        int imageId = imageIds[position % imageIds.length];
        Bitmap value = cache.get(imageId);

        if (value == null) {
            options.inDensity = mScreenDensity;
            options.inScaled = false;
            value = BitmapFactory.decodeResource(mResources, imageId, options);
            if (size != 0) {
                value = Bitmap.createScaledBitmap(value, size, size, false);
            }
            cache.put(imageId, value);
        }

        imageView.setImageBitmap(value);
    }
}
