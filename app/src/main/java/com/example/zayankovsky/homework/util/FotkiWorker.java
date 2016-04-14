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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.example.zayankovsky.homework.R;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;

public class FotkiWorker extends ImageWorker {

    private static Bitmap emptyPhoto;
    private static Bitmap thumbnail;

    private static final List<FotkiImage> fotki = new ArrayList<>();

    private static String author;
    private static Date published;
    private static String url;
    private static Date podDate;

    public static void init(Resources resources) {
        emptyPhoto = BitmapFactory.decodeResource(resources, R.drawable.empty_photo);
        emptyPhoto = Bitmap.createScaledBitmap(
                emptyPhoto, mThumbnailWidth, emptyPhoto.getHeight() * mThumbnailWidth / emptyPhoto.getWidth(), false
        );
    }

    public static void init(Bitmap thumbnail) {
        FotkiWorker.thumbnail = thumbnail;
    }

    public static void add(List<FotkiImage> fotki) {
        FotkiWorker.fotki.addAll(fotki);
    }

    public static void clear() {
        fotki.clear();
    }

    public static void save(FileOutputStream fileOutputStream) throws IOException {
        DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(fileOutputStream));
        outputStream.writeInt(fotki.size());

        for (FotkiImage image : fotki) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            outputStream.writeUTF(dateFormat.format(image.getPODDate()));

            outputStream.writeInt(image.getUrls().size());
            for (SortedMap.Entry<Integer, String> entry : image.getUrls().entrySet()) {
                outputStream.writeInt(entry.getKey());
                outputStream.writeUTF(entry.getValue());
            }

            outputStream.writeUTF(dateFormat.format(image.getPublished()));
            outputStream.writeUTF(image.getAuthor());
            outputStream.writeUTF(image.getTitle());
        }

        outputStream.close();
    }

    public static int size() {
        return fotki.size();
    }

    public static String getAuthor() {
        return author;
    }

    public static Date getPublished() {
        return published;
    }

    public static String getUrl() {
        return url;
    }

    public static Date getPODDate() {
        return podDate;
    }

    public static Date getLastPODDate() {
        return fotki.get(fotki.size() - 1).getPODDate();
    }

    public static void loadThumbnail(int position, ImageView imageView) {
        getFromCacheOrNetwork("fotki/thumbnails/" + mThumbnailWidth + "/", position, imageView, true);
    }

    public static void loadImage(int position, ImageView imageView) {
        getFromCacheOrNetwork("fotki/images/" + mImageWidth + "/", position, imageView, false);
    }

    /**
     * Load an image specified by the position parameter.
     * If the image is found in the memory cache, it is returned immediately, otherwise
     * an {@link AsyncTask} will be created to asynchronously load the bitmap.
     *
     * @param position The position of the ImageView to bind the image to.
     * @param imageView The ImageView itself.
     */
    private static void getFromCacheOrNetwork(String prefix, int position, ImageView imageView, boolean isThumbnail) {
        FotkiImage image = fotki.get(position);
        SortedMap<Integer, String> urls = image.getUrls();
        int reqWidth = isThumbnail ? mThumbnailWidth : mImageWidth;
        int width = urls.lastKey();
        String url = urls.get(width);

        for (SortedMap.Entry<Integer, String> entry : urls.entrySet()) {
            if (entry.getKey() >= reqWidth) {
                width = entry.getKey();
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
            ConnectivityManager connMgr =
                    (ConnectivityManager) imageView.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                new DownloadBitmapTask(imageView, isThumbnail, width).execute(data, url);
            }
        }

        author = image.getAuthor();
        title = image.getTitle();
        published = image.getPublished();
        FotkiWorker.url = url;
        podDate = image.getPODDate();
    }

    /**
     * The actual AsyncTask that will asynchronously download the image.
     */
    private static class DownloadBitmapTask extends AsyncTask<String, Void, Bitmap> {
        private final ImageView imageView;
        private final boolean isThumbnail;
        private final int width;

        public DownloadBitmapTask(ImageView imageView, boolean isThumbnail, int width) {
            this.imageView = imageView;
            this.isThumbnail = isThumbnail;
            this.width = width;
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
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = calculateInSampleSize(width, isThumbnail ? mThumbnailWidth : mImageWidth);
                return BitmapFactory.decodeStream(is, null, options);

                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }

        private int calculateInSampleSize(int width, int reqWidth) {
            int inSampleSize = 1;

            if (width > reqWidth) {

                final int halfWidth = width / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps
                // width larger than the requested height and width.
                while ((halfWidth / inSampleSize) > reqWidth) {
                    inSampleSize *= 2;
                }
            }

            return inSampleSize;
        }
    }
}
