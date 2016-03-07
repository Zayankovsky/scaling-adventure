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

import java.util.ArrayList;
import java.util.Collections;

/**
 * This class wraps up completing some arbitrary work when loading a bitmap to an ImageView.
 * It handles things like using a memory cache, scaling bitmaps and loading them randomly.
 */
public class ImageWorker {

    private static Resources mResources;
    private static int mScreenWidth;
    private static int mScreenDensity;
    private static int mColumnCount;

    private static int[] imageIds = {
            R.drawable.image_1, R.drawable.image_2, R.drawable.image_3,
            R.drawable.image_4, R.drawable.image_5, R.drawable.image_6,
    };

    private static ArrayList<SparseArray<Bitmap>> thumbnailCaches = new ArrayList<>(5);
    private static SparseArray<Bitmap> imageCache = new SparseArray<>();
    private static ArrayList<Integer> randomizer = new ArrayList<>(720);

    private static int[] indexes = {0, 1, 2, 3, 4, 5};

    private static void permute(int start) {
        if (start == 5) {
            randomizer.add(
                    indexes[0] + 6 * indexes[1] + 36 * indexes[2] + 216 * indexes[3] + 1296 * indexes[4] + 7776 * indexes[5]
            );
        } else {
            permute(start + 1);
            for (int i = start + 1; i < 6; ++i) {
                indexes[start] ^= indexes[i];
                indexes[i] ^= indexes[start];
                indexes[start] ^= indexes[i];

                permute(start + 1);

                indexes[start] ^= indexes[i];
                indexes[i] ^= indexes[start];
                indexes[start] ^= indexes[i];
            }
        }
    }

    static {
        for (int i = 0; i < 5; ++i) {
            thumbnailCaches.add(new SparseArray<Bitmap>());
        }

        permute(0);
        Collections.shuffle(randomizer);
    }

    public static void init(Resources resources, int screenWidth, int screenDensity, int columnCount) {
        mResources = resources;
        mScreenWidth = screenWidth;
        mScreenDensity = screenDensity;
        mColumnCount = columnCount;
    }

    public static void loadThumbnail(int position, ImageView imageView) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2 + (mColumnCount < 4 ? 0 : 2);

        getFromCacheOrResources(
                thumbnailCaches.get(mColumnCount - 2), imageView, options, position, mScreenWidth / mColumnCount - 10
        );
    }

    public static void loadImage(int position, ImageView imageView) {
        getFromCacheOrResources(imageCache, imageView, new BitmapFactory.Options(), position, 0);
    }

    /**
     * Return an image specified by the position parameter.
     * If the image is found in the memory cache, it is returned immediately, otherwise
     * {@link BitmapFactory::decodeResource} will be called to load the bitmap.
     *
     * @param position The position of the ImageView to bind the image to.
     */
    private static void getFromCacheOrResources(SparseArray<Bitmap> cache, ImageView imageView,
                                                BitmapFactory.Options options, int position, int size) {
        int permutation = randomizer.get(position / mColumnCount % 720);
        for (int i = 0; i < position % mColumnCount; ++i) {
            permutation /= 6;
        }
        int imageId = imageIds[permutation % 6];
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
