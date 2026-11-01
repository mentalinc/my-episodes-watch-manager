package nz.mentalinc.watcher.utils;

import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Locale;

/**
 * Author Dirk Vranckaert
 * Date: 24-apr-2010
 * Time: 0:41:33
 */
public final class DateUtil {
    private static final String LOG_TAG = DateUtil.class.getSimpleName();
    //http://download.oracle.com/javase/1.4.2/docs/api/java/text/SimpleDateFormat.html#rfc822timezone
    private static final String[] dateFormats = {"d-MMM-y", "d/M/y", "d-M-y", "d-MM-y"};
    private static final DateTimeFormatter AIR_DATE_FULL =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL);
    private static final DateTimeFormatter AIR_DATE_LONG =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG);
    private static final DateTimeFormatter AIR_DATE_MEDIUM =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);


    /**
     * Formats a given date in the {@link java.text.DateFormat#FULL} format.
     *
     * @param date The date to format.
     * @return The date representation in a string.
     */
    public static String formatDateFull(Date date) {
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL);
        return dateFormat.format(date);
    }

    /**
     * Formats a given date in the {@link java.text.DateFormat#LONG} format.
     *
     * @param date The date to format.
     * @return The date representation in a string.
     */
    public static String formatDateLong(Date date) {
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
        return dateFormat.format(date);
    }

    /**
     * Tries to convert a string to a date intance.
     *
     * @param dateString The string to convert.
     * @return A date instance. Null if the date pattern could not be determined!
     */
    public static Date convertToDate(String dateString) {
        if (dateString.endsWith(".") || dateString.endsWith(";") || dateString.endsWith(":") || dateString.endsWith(",") || dateString.endsWith("-")) {
            dateString = dateString.substring(0, dateString.length() - 1);
        }

        for (String dateFormat : dateFormats) {
            DateFormat format = new SimpleDateFormat(dateFormat, Locale.ENGLISH);
            format.setLenient(false);
            try {
                return format.parse(dateString);
            } catch (ParseException e) {
                Log.d(LOG_TAG, "Dateformat " + dateFormat + " not valid for dateString " + dateString);
            }
        }
        return null;
    }

    /**
     * Converts a millisecond timestamp to a {@link LocalDate}, treating non-positive values as an
     * unknown air date.
     *
     * @param epochMillis The timestamp in milliseconds since the epoch.
     * @return The date, or {@code null} when the air date is unknown.
     */
    public static LocalDate toLocalDate(long epochMillis) {
        if (epochMillis <= 0L) {
            return null;
        }
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * Formats an air date in the {@link java.text.DateFormat#LONG} format, returning the supplied
     * fallback when the date is unknown.
     */
    public static String formatAirDate(long epochMillis, String unknownValue) {
        return formatAirDate(toLocalDate(epochMillis), AIR_DATE_LONG, unknownValue);
    }

    /**
     * Formats an air date in the {@link java.text.DateFormat#FULL} format, returning the supplied
     * fallback when the date is unknown.
     */
    public static String formatAirDateFull(long epochMillis, String unknownValue) {
        return formatAirDate(toLocalDate(epochMillis), AIR_DATE_FULL, unknownValue);
    }

    /**
     * Formats a nullable air date in the {@link java.text.DateFormat#FULL} format, returning the
     * supplied fallback when the date is unknown.
     */
    public static String formatAirDateFull(LocalDate date, String unknownValue) {
        return formatAirDate(date, AIR_DATE_FULL, unknownValue);
    }

    /**
     * Formats an air date in the {@link java.text.DateFormat#MEDIUM} format, returning the supplied
     * fallback when the date is unknown.
     */
    public static String formatAirDateMedium(long epochMillis, String unknownValue) {
        return formatAirDate(toLocalDate(epochMillis), AIR_DATE_MEDIUM, unknownValue);
    }

    /**
     * Formats a nullable air date in the {@link java.text.DateFormat#LONG} format, returning the
     * supplied fallback when the date is unknown.
     */
    public static String formatAirDate(LocalDate date, String unknownValue) {
        return formatAirDate(date, AIR_DATE_LONG, unknownValue);
    }

    private static String formatAirDate(LocalDate date, DateTimeFormatter formatter,
                                        String unknownValue) {
        return date == null ? unknownValue : formatter.format(date);
    }
}
