package com.example.zayankovsky.homework;

import java.util.SortedMap;

public class FotkiImage {
    private final String title;
    private final SortedMap<Integer, String> urls;

    public FotkiImage(String title, SortedMap<Integer, String> urls) {
        this.title = title;
        this.urls = urls;
    }

    public String getTitle() {
        return title;
    }

    public SortedMap<Integer, String> getUrls() {
        return urls;
    }
}
