package io.crest.engine.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class DateUtils {
    private static final String format = "yyyy-MM-dd HH:mm:ss";
    private static final String minuteFormat = "yyyy-MM-dd HH:mm";
    private static final String hourFormat = "yyyy-MM-dd HH";
    private static final String dateOnly = "yyyy-MM-dd";
    private static final String monthOnly = "yyyy-MM";
    private static final String yearOnly = "yyyy";
    private static final String timeOnly = "HH:mm:ss";

    public static String get_date_format(String date) {
        String format1 = format;
        String minuteFormat1 = minuteFormat;
        String hourFormat1 = hourFormat;
        String timeOnly1 = timeOnly;
        String dateOnly1 = dateOnly;
        String monthOnly1 = monthOnly;
        String yearOnly1 = yearOnly;
        if (date != null && date.contains("/")) {
            format1 = format1.replace("-", "/");
            minuteFormat1 = minuteFormat1.replace("-", "/");
            hourFormat1 = hourFormat1.replace("-", "/");
            timeOnly1 = timeOnly1.replace("-", "/");
            dateOnly1 = dateOnly1.replace("-", "/");
            monthOnly1 = monthOnly1.replace("-", "/");
            yearOnly1 = yearOnly1.replace("-", "/");
        }
        for (String pattern : new String[]{format1, minuteFormat1, hourFormat1, timeOnly1, dateOnly1, monthOnly1, yearOnly1}) {
            if (canParse(date, pattern)) {
                return pattern;
            }
        }
        return format1;
    }

    private static boolean canParse(String date, String pattern) {
        if (date == null) {
            return false;
        }
        try {
            new SimpleDateFormat(pattern).parse(date);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
}
