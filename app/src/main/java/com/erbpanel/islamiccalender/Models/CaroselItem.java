package com.erbpanel.islamiccalender.Models;
public class CaroselItem {
    private final String hijriDate;
    private final String sehriTime;
    private final String iftarTime;

    public CaroselItem(String hijriDate, String sehriTime, String iftarTime) {
        this.hijriDate = hijriDate;
        this.sehriTime = sehriTime;
        this.iftarTime = iftarTime;
    }

    public String getHijriDate() { return hijriDate; }
    public String getSehriTime() { return sehriTime; }
    public String getIftarTime() { return iftarTime; }
}