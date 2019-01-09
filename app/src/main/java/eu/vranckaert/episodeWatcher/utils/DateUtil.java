package eu.vranckaert.episodeWatcher.utils;

import android.content.Context;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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


    /**
     * Formats a given date in the {@link java.text.DateFormat#FULL} format.
     *
     * @param date   The date to format.
     * @return The date representation in a string.
     */
    public static String formatDateFull(Date date) {
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL);
        return dateFormat.format(date);
    }

    /**
     * Formats a given date in the {@link java.text.DateFormat#LONG} format.
     *
     * @param date   The date to format.
     *
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
}