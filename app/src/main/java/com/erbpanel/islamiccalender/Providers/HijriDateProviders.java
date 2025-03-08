package com.erbpanel.islamiccalender.Providers;

import java.time.LocalDate;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

public class HijriDateProviders {

    public static class DatePair {
        private String hijriDate;
        private String gregorianDate;
        private boolean isCurrentDate;
        private String seheri;
        private String iftar;

        public DatePair(String hijriDate, String gregorianDate, boolean isCurrentDate, String seheri, String iftar) {
            this.hijriDate = hijriDate;
            this.gregorianDate = gregorianDate;
            this.isCurrentDate = isCurrentDate;
            this.seheri = seheri;
            this.iftar = iftar;
        }

        public String getHijriDate() { return hijriDate; }
        public String getGregorianDate() { return gregorianDate; }
        public boolean isCurrentDate() { return isCurrentDate; }
        public String getIftar() {return iftar;}

        public void setIftar(String iftar) {this.iftar = iftar;}
    }

    public static List<DatePair> getHijriDates() {
        List<DatePair> datePairs = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Get current dates
        HijrahDate todayHijri = HijrahChronology.INSTANCE.dateNow();
        LocalDate todayGregorian = LocalDate.now();

        // Check if current month is Ramadan (month 9)
        int currentHijriMonth = todayHijri.get(ChronoField.MONTH_OF_YEAR);
        boolean isRamadan = (currentHijriMonth == 9);

        HijrahDate startDate;
        int daysInMonth;

        if (isRamadan) {
            // For Ramadan, get the full month
            startDate = todayHijri.with(ChronoField.DAY_OF_MONTH, 1);
            daysInMonth = todayHijri.lengthOfMonth();
        } else {
            // For non-Ramadan months, use current month
            startDate = todayHijri.with(ChronoField.DAY_OF_MONTH, 1);
            daysInMonth = todayHijri.lengthOfMonth();
        }

        try {
            // Generate dates for the entire month
            for (int day = 1; day <= daysInMonth; day++) {
                HijrahDate currentHijri = startDate.with(ChronoField.DAY_OF_MONTH, day);
                LocalDate currentGregorian = LocalDate.ofEpochDay(currentHijri.toEpochDay());
                boolean isToday = currentHijri.equals(todayHijri);
                String hijriFormatted = currentHijri.format(formatter);
                String gregorianFormatted = currentGregorian.format(formatter);

                datePairs.add(
                        new DatePair(
                                hijriFormatted,
                                gregorianFormatted,
                                isToday,
                                "",
                                ""
                                ));
            }
        } catch (Exception e) {
            System.err.println("Error generating dates: " + e.getMessage());
            // Return what we have so far in case of error
            return datePairs;
        }

        return datePairs;
    }
}