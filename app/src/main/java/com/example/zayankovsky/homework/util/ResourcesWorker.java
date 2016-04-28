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

package com.example.zayankovsky.homework.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.example.zayankovsky.homework.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResourcesWorker extends ImageWorker {

    private static final int[] imageIds = {
            R.drawable.image_1, R.drawable.image_2, R.drawable.image_3,
            R.drawable.image_4, R.drawable.image_5, R.drawable.image_6,
    };

    private static final List<Integer> randomizer = new ArrayList<>(720);
    private static final int[] indexes = {0, 1, 2, 3, 4, 5};

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
        permute(0);
        Collections.shuffle(randomizer);
    }

    public static void loadThumbnail(int position, ImageView imageView) {
        getFromCacheOrResources("thumbnails/" + mThumbnailWidth + "/", position, imageView, true);
    }

    public static void loadImage(int position, ImageView imageView) {
        getFromCacheOrResources("images/", position, imageView, false);
    }

    /**
     * Load an image specified by the position parameter.
     * If the image is found in the memory cache, it is returned immediately, otherwise
     * {@link BitmapFactory::decodeResource} will be called to load the bitmap.
     *
     * @param position The position of the ImageView to bind the image to.
     * @param imageView The ImageView itself.
     */
    private static void getFromCacheOrResources(String prefix, int position, ImageView imageView, boolean isThumbnail) {
        Resources resources = imageView.getResources();

        int permutation = randomizer.get(position / mColumnCount % 720);
        for (int i = 0; i < position % mColumnCount; ++i) {
            permutation /= 6;
        }
        int index = permutation % 6;
        String data = prefix + index;
        Bitmap value = ImageCache.getBitmapFromMemoryCache(data);

        if (value == null) {
            value = BitmapFactory.decodeResource(resources, imageIds[index]);
            if (isThumbnail) {
                value = Bitmap.createScaledBitmap(
                        value, mThumbnailWidth, value.getHeight() * mThumbnailWidth / value.getWidth(), false
                );
            }
            ImageCache.addBitmapToMemoryCache(data, value);
        }

        imageView.setImageBitmap(value);
        title = resources.getResourceEntryName(imageIds[index]);
    }
}
