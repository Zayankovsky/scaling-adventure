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

import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

/**
 * This class wraps up completing some arbitrary long running work when loading a bitmap to an
 * ImageView. It handles things like using a memory and disk cache, running the work in a background
 * thread and setting a placeholder image.
 */
public class ImageWorker {

    private static Resources mResources;
    private static ConnectivityManager mConnMgr;
    private static ContentResolver mContentResolver;
    private static int mScreenDensity;
    private static int mColumnCount;
    private static int mImageWidth;
    private static int mThumbnailWidth;

    private static Bitmap emptyPhoto;
    private static Bitmap thumbnail;

    private static final int[] imageIds = {
            R.drawable.image_1, R.drawable.image_2, R.drawable.image_3,
            R.drawable.image_4, R.drawable.image_5, R.drawable.image_6,
    };

    private static List<Integer> randomizer = new ArrayList<>(720);
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

    private static List<SortedMap<Integer, String>> fotki = new ArrayList<>();
    private static List<Uri> gallery = new ArrayList<>();

    static {
        permute(0);
        Collections.shuffle(randomizer);
    }

    public static void init(Resources resources, ConnectivityManager connMgr, ContentResolver contentResolver,
                            int screenWidth, int screenDensity, int columnCount) {
        mResources = resources;
        mConnMgr = connMgr;
        mContentResolver = contentResolver;
        mScreenDensity = screenDensity;
        mColumnCount = columnCount;
        mImageWidth = screenWidth;
        mThumbnailWidth = screenWidth / mColumnCount - 10;

        emptyPhoto = BitmapFactory.decodeResource(mResources, R.drawable.empty_photo);
        emptyPhoto = Bitmap.createScaledBitmap(
                emptyPhoto, mThumbnailWidth, emptyPhoto.getHeight() * mThumbnailWidth / emptyPhoto.getWidth(), false
            );
    }

    public static void init(Bitmap thumbnail) {
        ImageWorker.thumbnail = thumbnail;
    }

    public static void init(List<SortedMap<Integer, String>> fotki) {
        ImageWorker.fotki = fotki;
    }

    public static void addToGallery(Uri fileUri) {
        gallery.add(fileUri);
    }

    public static int getFotkiSize() {
        return fotki.size();
    }

    public static int getGallerySize() {
        return gallery.size();
    }

    public static void loadThumbnail(int position, ImageView imageView) {
        getFromCacheOrResources("thumbnails/" + mThumbnailWidth + "/", position, imageView, true);
    }

    public static void loadImage(int position, ImageView imageView) {
        getFromCacheOrResources("images/", position, imageView, false);
    }

    public static void loadFotkiThumbnail(int position, ImageView imageView) {
        getFromCacheOrDownload("fotki/thumbnails/" + mThumbnailWidth + "/", position, imageView, true);
    }

    public static void loadFotkiImage(int position, ImageView imageView) {
        getFromCacheOrDownload("fotki/images/", position, imageView, false);
    }

    public static void loadGalleryThumbnail(int position, ImageView imageView) {
        getFromCacheOrGallery("gallery/thumbnails/" + mThumbnailWidth + "/", position, imageView, true);
    }

    public static void loadGalleryImage(int position, ImageView imageView) {
        getFromCacheOrGallery("gallery/images/", position, imageView, false);
    }

    /**
     * Return an image specified by the position parameter.
     * If the image is found in the memory cache, it is returned immediately, otherwise
     * {@link BitmapFactory::decodeResource} will be called to load the bitmap.
     *
     * @param position The position of the ImageView to bind the image to.
     */
    private static void getFromCacheOrResources(String prefix, int position, ImageView imageView, boolean isThumbnail) {
        int permutation = randomizer.get(position / mColumnCount % 720);
        for (int i = 0; i < position % mColumnCount; ++i) {
            permutation /= 6;
        }
        int index = permutation % 6;
        String data = prefix + index;
        Bitmap value = ImageCache.getBitmapFromMemoryCache(data);

        if (value == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDensity = mScreenDensity;
            options.inScaled = false;
            value = BitmapFactory.decodeResource(mResources, imageIds[index], options);
            if (isThumbnail) {
                value = Bitmap.createScaledBitmap(
                        value, mThumbnailWidth, value.getHeight() * mThumbnailWidth / value.getWidth(), false
                );
            }
            ImageCache.addBitmapToMemoryCache(data, value);
        }

        imageView.setImageBitmap(value);
    }

    private static void getFromCacheOrDownload(String prefix, int position, ImageView imageView, boolean isThumbnail) {
        int width = isThumbnail ? mThumbnailWidth : mImageWidth;
        SortedMap<Integer, String> image = fotki.get(position % fotki.size());
        String url = image.get(image.lastKey());

        for (SortedMap.Entry<Integer, String> entry : image.entrySet()) {
            if (entry.getKey() > width) {
                url = entry.getValue();
                break;
            }
        }

        String data = prefix + url;
        Bitmap value = ImageCache.getBitmapFromMemoryCache(data);

        if (value != null) {
            // Bitmap found in memory cache
            imageView.setImageBitmap(value);
        } else {
            imageView.setImageBitmap(isThumbnail ? emptyPhoto : thumbnail);
            NetworkInfo networkInfo = mConnMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                new DownloadBitmapTask(imageView, isThumbnail).execute(data, url);
            }
        }
    }

    private static void getFromCacheOrGallery(String prefix, int position, ImageView imageView, boolean isThumbnail) {
        Uri uri = gallery.get(position % gallery.size());
        String data = prefix + uri;
        Bitmap value = ImageCache.getBitmapFromMemoryCache(data);

        if (value == null) {
            try {
                value = MediaStore.Images.Media.getBitmap(mContentResolver, gallery.get(position % gallery.size()));
                if (isThumbnail) {
                    value = Bitmap.createScaledBitmap(
                            value, mThumbnailWidth, value.getHeight() * mThumbnailWidth / value.getWidth(), false
                    );
                }
                ImageCache.addBitmapToMemoryCache(data, value);
            } catch (IOException ignored) {}
        }

        imageView.setImageBitmap(value);
    }

    /**
     * The actual AsyncTask that will asynchronously download the image.
     */
    private static class DownloadBitmapTask extends AsyncTask<String, Void, Bitmap> {
        private final ImageView imageView;
        private final boolean isThumbnail;

        public DownloadBitmapTask(ImageView imageView, boolean isThumbnail) {
            this.imageView = imageView;
            this.isThumbnail = isThumbnail;
        }

        /**
         * Background processing.
         */
        @Override
        protected Bitmap doInBackground(String... params) {
            try {
                // Try and fetch the bitmap from the cache
                Bitmap bitmap = ImageCache.getBitmapFromDiskCache(params[0]);

                // If the bitmap was not found in the cache, then call the downloadBitmap method
                if (bitmap == null) {
                    bitmap = downloadBitmap(params[1]);
                }

                // If the bitmap was downloaded, then add the downloaded
                // bitmap to the cache for future use.
                if (bitmap != null) {
                    if (isThumbnail) {
                        bitmap = Bitmap.createScaledBitmap(
                                bitmap, mThumbnailWidth, bitmap.getHeight() * mThumbnailWidth / bitmap.getWidth(), false
                        );
                    }
                    ImageCache.addBitmapToMemoryCache(params[0], bitmap);
                    ImageCache.addBitmapToDiskCache(params[0], bitmap);
                }

                return bitmap;
            } catch (IOException e) {
                return null;
            }
        }

        /**
         * Once the image is processed, associates it to the imageView
         */
        @Override
        protected void onPostExecute(Bitmap value) {
            imageView.setImageBitmap(value);
        }

        // Given a URL, establishes an HttpUrlConnection and retrieves
        // an image as a InputStream, which it returns as a bitmap.
        private Bitmap downloadBitmap(String url) throws IOException {
            InputStream is = null;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                is = conn.getInputStream();

                // Convert the InputStream into a bitmap
                return BitmapFactory.decodeStream(is);

                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }
    }
}
