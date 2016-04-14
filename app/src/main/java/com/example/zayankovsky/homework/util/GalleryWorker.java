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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.ImageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GalleryWorker extends ImageWorker {

    private static final List<GalleryImage> gallery = new ArrayList<>();

    public static void add(String title, Uri uri) {
        gallery.add(new GalleryImage(title, uri));
    }

    public static void clear() {
        gallery.clear();
    }

    public static int size() {
        return gallery.size();
    }

    public static void loadThumbnail(int position, ImageView imageView) {
        getFromCacheOrGallery("gallery/thumbnails/" + mThumbnailWidth + "/", position, imageView, true);
    }

    public static void loadImage(int position, ImageView imageView) {
        getFromCacheOrGallery("gallery/images/", position, imageView, false);
    }

    /**
     * Load an image specified by the position parameter. If the image is found in the memory cache,
     * it is returned immediately, otherwise {@link MediaStore.Images.Media::getBitmap} or
     * {@link MediaStore.Images.Thumbnails::getThumbnail} will be called to load the bitmap.
     *
     * @param position The position of the ImageView to bind the image to.
     * @param imageView The ImageView itself.
     */
    private static void getFromCacheOrGallery(String prefix, int position, ImageView imageView, boolean isThumbnail) {
        ContentResolver contentResolver = imageView.getContext().getContentResolver();

        GalleryImage image = gallery.get(position);
        Uri uri = image.getUri();
        String data = prefix + uri;
        Bitmap value = ImageCache.getBitmapFromMemoryCache(data);

        if (value == null) {
            if (isThumbnail) {
                value = MediaStore.Images.Thumbnails.getThumbnail(
                        contentResolver, ContentUris.parseId(uri),
                        mThumbnailWidth > 96 ? MediaStore.Images.Thumbnails.MINI_KIND : MediaStore.Images.Thumbnails.MICRO_KIND,
                        null
                );
                value = Bitmap.createScaledBitmap(
                        value, mThumbnailWidth, value.getHeight() * mThumbnailWidth / value.getWidth(), false
                );
            } else {
                try {
                    value = MediaStore.Images.Media.getBitmap(contentResolver, uri);
                } catch (IOException ignored) {}
            }
            ImageCache.addBitmapToMemoryCache(data, value);
        }

        imageView.setImageBitmap(value);
        title = image.getTitle();
    }

    private static class GalleryImage {
        private final String title;
        private final Uri uri;

        public GalleryImage(String title, Uri uri) {
            this.title = title;
            this.uri = uri;
        }

        public String getTitle() {
            return title;
        }

        public Uri getUri() {
            return uri;
        }
    }
}
