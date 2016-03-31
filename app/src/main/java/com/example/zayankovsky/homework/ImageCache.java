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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.LruCache;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class handles disk and memory caching of bitmaps in conjunction with the
 * {@link ImageWorker} class.
 */
public class ImageCache {
    // Maximum memory cache size in kilobytes
    private static final int MAX_MEMORY_CACHE_SIZE = Math.round(0.8f * Runtime.getRuntime().maxMemory() / 1024);

    // Disk cache size in bytes
    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 200; // 200MB

    private static LruCache<String, Bitmap> mMemoryCache;
    private static DiskLruCache mDiskLruCache;
    private static final Object mDiskCacheLock = new Object();
    private static boolean mDiskCacheStarting = true;

    /**
     * Initialize the cache, providing all parameters.
     *
     * @param context A context to use.
     */
    public static void init(Context context, int memoryCacheSize, boolean clearDiskCache) {
        // Set up memory cache
        mMemoryCache = new LruCache<String, Bitmap>(Math.min(memoryCacheSize, MAX_MEMORY_CACHE_SIZE)) {

            /**
             * Measure item size in kilobytes rather than units
             * which is more practical for a bitmap cache
             */
            @Override
            protected int sizeOf(String key, Bitmap value) {
                final int bitmapSize = value.getByteCount() / 1024;
                return bitmapSize == 0 ? 1 : bitmapSize;
            }
        };

        // Set up disk cache
        new InitDiskCacheTask(context, clearDiskCache).execute();
    }

    /**
     * Adds a bitmap to memory cache.
     * @param data Unique identifier for the bitmap to store
     * @param value The bitmap drawable to store
     */
    public static void addBitmapToMemoryCache(String data, Bitmap value) {
        if (data == null || value == null) {
            return;
        }

        if (mMemoryCache != null) {
            mMemoryCache.put(data, value);
        }
    }

    /**
     * Adds a bitmap to disk cache.
     * @param data Unique identifier for the bitmap to store
     * @param value The bitmap drawable to store
     */
    public static void addBitmapToDiskCache(String data, Bitmap value) {
        if (data == null || value == null) {
            return;
        }

        synchronized (mDiskCacheLock) {
            if (mDiskLruCache != null) {
                final String key = hashKeyForDisk(data);
                OutputStream out = null;
                try {
                    DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
                    if (snapshot == null) {
                        final DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                        if (editor != null) {
                            out = editor.newOutputStream();
                            value.compress(Bitmap.CompressFormat.PNG, 100, out);
                            editor.commit();
                            out.close();
                        }
                    } else {
                        snapshot.getInputStream().close();
                    }
                } catch (final IOException ignored) {
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException ignored) {}
                }
            }
        }
    }

    /**
     * Get from memory cache.
     *
     * @param data Unique identifier for which item to get
     * @return The bitmap drawable if found in cache, null otherwise
     */
    public static Bitmap getBitmapFromMemoryCache(String data) {
        if (mMemoryCache != null) {
            return mMemoryCache.get(data);
        }

        return null;
    }

    /**
     * Get from disk cache.
     *
     * @param data Unique identifier for which item to get
     * @return The bitmap if found in cache, null otherwise
     */
    public static Bitmap getBitmapFromDiskCache(String data) {
        final String key = hashKeyForDisk(data);
        Bitmap bitmap = null;

        synchronized (mDiskCacheLock) {
            while (mDiskCacheStarting) {
                try {
                    mDiskCacheLock.wait();
                } catch (InterruptedException ignored) {}
            }
            if (mDiskLruCache != null) {
                InputStream inputStream = null;
                try {
                    final DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
                    if (snapshot != null) {
                        inputStream = snapshot.getInputStream();
                        if (inputStream != null) {
                            FileDescriptor fd = ((FileInputStream) inputStream).getFD();
                            bitmap = BitmapFactory.decodeFileDescriptor(fd);
                        }
                    }
                } catch (final IOException ignored) {
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (IOException ignored) {}
                }
            }
            return bitmap;
        }
    }

    /**
     * Get a usable cache directory (external if available, internal otherwise).
     *
     * @param context The context to use
     * @param uniqueName A unique directory name to append to the cache dir
     * @return The cache dir
     */
    private static File getDiskCacheDir(Context context, String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        final String cachePath =
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable() ?
                        context.getExternalCacheDir().getPath() : context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    /**
     * A hashing method that changes a string (like a URL) into a hash suitable for using as a
     * disk filename.
     */
    private static String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * Initializes the disk cache. Note that this includes disk access
     * so this is executed on a background thread.
     */
    private static class InitDiskCacheTask extends AsyncTask<Void, Void, Void> {
        private final Context context;
        private final boolean clearDiskCache;

        public InitDiskCacheTask(Context context, boolean clearDiskCache) {
            this.context = context;
            this.clearDiskCache = clearDiskCache;
        }

        @Override
        protected Void doInBackground(Void... params) {
            synchronized (mDiskCacheLock) {
                mDiskCacheStarting = true;
                if (clearDiskCache && mDiskLruCache != null && !mDiskLruCache.isClosed()) {
                    try {
                        mDiskLruCache.delete();
                    } catch (IOException ignored) {}
                    mDiskLruCache = null;
                }

                File diskCacheDir = getDiskCacheDir(context, "cache");
                if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
                    if (diskCacheDir != null) {
                        if (!diskCacheDir.exists()) {
                            diskCacheDir.mkdirs();
                        }
                        try {
                            mDiskLruCache = DiskLruCache.open(
                                    diskCacheDir, Math.min(DISK_CACHE_SIZE, Math.round(0.8 * diskCacheDir.getUsableSpace()))
                            );
                        } catch (final IOException ignored) {}
                    }
                }
                mDiskCacheStarting = false;
                mDiskCacheLock.notifyAll();
            }
            return null;
        }
    }
}
