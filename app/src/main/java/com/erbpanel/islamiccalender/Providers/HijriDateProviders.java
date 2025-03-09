package com.erbpanel.islamiccalender.Providers;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

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

        public String getHijriDate() {
            return convertHijriDate(hijriDate);
        }
        public String getGregorianDate() { return gregorianDate; }
        public boolean isCurrentDate() { return isCurrentDate; }
        public String getIftar() { return iftar; }

        public void setIftar(String iftar) { this.iftar = iftar; }
    }

    public static String getOrdinalSuffix(int number) {
        if (number >= 11 && number <= 13) {
            return "th";
        }
        switch (number % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }

    private static String convertHijriDate(String hijriDate) {
        ZoneId deviceZone = TimeZone.getDefault().toZoneId();
        // Hijri month names
        String[] hijriMonths = {
                "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani", "Jumada al-Awwal",
                "Jumada al-Thani", "Rajab", "Sha'ban", "Ramadhan", "Shawwal", "Dhul Qa'dah", "Dhul Hijjah"
        };

        try {
            // Split the Hijri date (Format: DD/MM/YYYY)
            String[] parts = hijriDate.split("/");
            if (parts.length != 3) return "Invalid Date";

            int day = Integer.parseInt(parts[0]);
            int monthIndex = Integer.parseInt(parts[1]) - 1; // Adjust index (0-based)

            if (monthIndex < 0 || monthIndex >= hijriMonths.length) return "Invalid Date";

            // Get the correct ordinal suffix (st, nd, rd, th)
            String ordinalDay = day + getOrdinalSuffix(day);

            // Return formatted date
            return ordinalDay + " " + hijriMonths[monthIndex];

        } catch (Exception e) {
            return "Invalid Date";
        }
    }

    public static List<DatePair> getHijriDates() {
        List<DatePair> datePairs = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Get current Hijri and Gregorian dates
        // Add adjustment of -1 day (or adjust as needed for your region)
        HijrahDate todayHijri = HijrahChronology.INSTANCE.dateNow().minus(1, java.time.temporal.ChronoUnit.DAYS);
        LocalDate todayGregorian = LocalDate.now();

        // Check if current month is Ramadan (month 9 in Hijri calendar)
        int currentHijriMonth = todayHijri.get(ChronoField.MONTH_OF_YEAR);
        boolean isRamadan = (currentHijriMonth == 9);

        HijrahDate startDate = todayHijri.with(ChronoField.DAY_OF_MONTH, 1);
        int daysInMonth = startDate.lengthOfMonth();

        try {
            // Generate dates for the entire month
            for (int day = 1; day <= daysInMonth; day++) {
                HijrahDate currentHijri = startDate.with(ChronoField.DAY_OF_MONTH, day);

                // Correctly convert HijriDate to GregorianDate
                LocalDate currentGregorian = LocalDate.ofEpochDay(currentHijri.toEpochDay()+1);

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
