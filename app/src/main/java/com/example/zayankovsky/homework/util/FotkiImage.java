package com.example.zayankovsky.homework.util;

import java.util.Date;
import java.util.SortedMap;

public class FotkiImage {
    private final String author;
    private final String title;
    private final Date published;
    private final SortedMap<Integer, String> urls;
    private final Date podDate;
    private final boolean divider;

    public FotkiImage(String author, String title, Date published,
                      SortedMap<Integer, String> urls, Date podDate, boolean divider) {
        this.author = author;
        this.title = title;
        this.published = published;
        this.urls = urls;
        this.podDate = podDate;
        this.divider = divider;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public Date getPublished() {
        return published;
    }

    public SortedMap<Integer, String> getUrls() {
        return urls;
    }

    public Date getPODDate() {
        return podDate;
    }

    public boolean isDivider() {
        return divider;
    }
}
