package io.crest.engine.utils;

import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineDateUtilsTest {

    @Test
    void getDateFormatKeepsLegacyFallbacks() {
        assertEquals("yyyy-MM-dd HH:mm:ss", DateUtils.get_date_format("2026-06-30 12:30:01"));
        assertEquals("yyyy/MM/dd", DateUtils.get_date_format("2026/06/30"));
        assertEquals("yyyy-MM-dd HH:mm:ss", DateUtils.get_date_format(null));
    }

    @Test
    void allDateFormatReturnsZeroForInvalidValue() {
        assertEquals(0L, Utils.allDateFormat2Long(null));
        assertEquals(0L, Utils.allDateFormat2Long("not-a-date"));
        assertTrue(Utils.allDateFormat2Long("2026-06-30") > 0);
    }

    @Test
    void parseDateTimeValueBuildsMonthRange() throws Exception {
        Map<String, Long> range = Utils.parseDateTimeValue("2026-06");

        long expectedStart = new SimpleDateFormat("yyyy-MM").parse("2026-06").getTime();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(expectedStart);
        calendar.add(Calendar.MONTH, 1);

        assertEquals(expectedStart, range.get("startTime"));
        assertEquals(calendar.getTimeInMillis() - 1, range.get("endTime"));
    }
}
