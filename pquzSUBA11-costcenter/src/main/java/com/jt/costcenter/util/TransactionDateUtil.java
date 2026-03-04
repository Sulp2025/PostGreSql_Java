package com.jt.costcenter.util;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TransactionDateUtil {

/**
 * Parse TransactionDate in flexible formats:
 * - 2015-08-17
 * - 2015-8-17
 * - 2015/08/17
 * - 2015/8/17
 *
 */

  private TransactionDateUtil() {}

    private static final Pattern DATE_PATTERN = Pattern.compile("^(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})$");

    public static LocalDate parseFlexible(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("TransactionDate is blank");
        }

        String s = raw.trim();
        Matcher m = DATE_PATTERN.matcher(s);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Invalid TransactionDate: " + raw + " (expected yyyy-MM-dd or yyyy/M/d etc.)");
        }

        int year;
        int month;
        int day;
        try {
            year = Integer.parseInt(m.group(1));
            month = Integer.parseInt(m.group(2));
            day = Integer.parseInt(m.group(3));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid TransactionDate: " + raw, e);
        }

        try {
            return LocalDate.of(year, month, day);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid TransactionDate: " + raw, e);
        }
    }
}
