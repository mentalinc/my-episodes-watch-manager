package nz.mentalinc.watcher.utils;

import org.junit.Test;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import static org.junit.Assert.*;

public class DateUtilTest {
    @Test
    public void testFormatDateFull() {
        Calendar cal = new GregorianCalendar(2024, Calendar.MARCH, 15);
        Date date = cal.getTime();
        DateFormat fullFormat = DateFormat.getDateInstance(DateFormat.FULL);
        assertEquals(fullFormat.format(date), DateUtil.formatDateFull(date));
    }

    @Test
    public void testFormatDateLong() {
        Calendar cal = new GregorianCalendar(2024, Calendar.MARCH, 15);
        Date date = cal.getTime();
        DateFormat longFormat = DateFormat.getDateInstance(DateFormat.LONG);
        assertEquals(longFormat.format(date), DateUtil.formatDateLong(date));
    }

    @Test
    public void testConvertToDate_dMMMy() {
        Date result = DateUtil.convertToDate("15-Mar-2024");
        assertNotNull(result);
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTime(result);
        assertEquals(2024, cal.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH));
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testConvertToDate_dMColonY() {
        Date result = DateUtil.convertToDate("15/3/2024");
        assertNotNull(result);
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTime(result);
        assertEquals(2024, cal.get(Calendar.YEAR));
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testConvertToDate_dMy() {
        Date result = DateUtil.convertToDate("15-3-2024");
        assertNotNull(result);
    }

    @Test
    public void testConvertToDate_dMMy() {
        Date result = DateUtil.convertToDate("15-03-2024");
        assertNotNull(result);
    }

    @Test
    public void testConvertToDate_stripsTrailingPeriod() {
        Date result = DateUtil.convertToDate("15-Mar-2024.");
        assertNotNull(result);
    }

    @Test
    public void testConvertToDate_stripsTrailingSemicolon() {
        Date result = DateUtil.convertToDate("15-Mar-2024;");
        assertNotNull(result);
    }

    @Test
    public void testConvertToDate_stripsTrailingColon() {
        Date result = DateUtil.convertToDate("15-Mar-2024:");
        assertNotNull(result);
    }

    @Test
    public void testConvertToDate_stripsTrailingComma() {
        Date result = DateUtil.convertToDate("15-Mar-2024,");
        assertNotNull(result);
    }

    @Test
    public void testConvertToDate_stripsTrailingDash() {
        Date result = DateUtil.convertToDate("15-Mar-2024-");
        assertNotNull(result);
    }

    @Test
    public void testConvertToDate_invalidFormat_returnsNull() {
        assertNull(DateUtil.convertToDate("not-a-date"));
    }

    @Test
    public void testConvertToDate_emptyString_returnsNull() {
        assertNull(DateUtil.convertToDate(""));
    }

    @Test
    public void testConvertToDate_nullInput_throws() {
        assertThrows(NullPointerException.class, () -> DateUtil.convertToDate(null));
    }
}
