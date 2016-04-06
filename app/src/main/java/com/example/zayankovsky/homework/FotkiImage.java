package com.example.zayankovsky.homework;

import java.util.Date;
import java.util.SortedMap;

public class FotkiImage {
    private final String title;
    private final SortedMap<Integer, String> urls;
    private final Date date;

    public FotkiImage(String title, SortedMap<Integer, String> urls, Date date) {
        this.title = title;
        this.urls = urls;
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public SortedMap<Integer, String> getUrls() {
        return urls;
    }

    public Date getDate() {
        return date;
    }
}
