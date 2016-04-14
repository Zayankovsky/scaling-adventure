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

/**
 * This class and its subclasses wraps up completing some arbitrary long running work
 * when loading a bitmap to an ImageView. It handles things like using a memory and disk cache,
 * running the work in a background thread and setting a placeholder image.
 */
public class ImageWorker {

    static int mColumnCount;
    static int mImageWidth;
    static int mThumbnailWidth;

    static String title;

    public static void init(int screenWidth, int columnCount) {
        mColumnCount = columnCount;
        mImageWidth = screenWidth;
        mThumbnailWidth = screenWidth / mColumnCount - 10;
    }

    public static String getTitle() {
        return title;
    }
}
