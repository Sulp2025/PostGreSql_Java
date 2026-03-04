package com.jt.summary.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class DateParsers {
  private DateParsers() {}

  /**
   * @param s raw date string from request
   * @return parsed {@link LocalDate}, or null if blank
   * @throws IllegalArgumentException if format is not supported
   */
  private static final List<DateTimeFormatter> FMTS =
      List.of(
          DateTimeFormatter.ISO_LOCAL_DATE, // 2026-01-13
          DateTimeFormatter.ofPattern("yyyy/M/d"), // 2026/1/13
          DateTimeFormatter.ofPattern("yyyy/MM/dd"));

  public static LocalDate parseFlexible(String s) {
    if (s == null || s.isBlank()) return null;

    String raw = s.trim();

    String datePart = raw.split("[T\\s]", 2)[0];

    // Try each formatter in order; return immediately on the first success.
    for (DateTimeFormatter f : FMTS) {
      try {
        return LocalDate.parse(datePart, f);
      } catch (Exception ignored) {
      }
    }

    // All formats failed -> throw a semantic exception
    throw new IllegalArgumentException("TransactionDate format not supported: " + s);
  }
}
