package nz.mentalinc.watcher.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilsTest {
    @Test
    public void testStartsWith_returnsZero() {
        assertEquals(0, StringUtils.indexOf("hello world", "hello"));
    }

    @Test
    public void testSearchInMiddle() {
        assertEquals(6, StringUtils.indexOf("hello world", "world"));
    }

    @Test
    public void testSearchNotFound_returnsMinusOne() {
        assertEquals(-1, StringUtils.indexOf("hello world", "xyz"));
    }

    @Test
    public void testEmptySource_returnsMinusOne() {
        assertEquals(-1, StringUtils.indexOf("", "test"));
    }

    @Test
    public void testEmptySearch_startsWith() {
        assertEquals(0, StringUtils.indexOf("hello", ""));
    }

    @Test
    public void testExactMatch_returnsZero() {
        assertEquals(0, StringUtils.indexOf("exact", "exact"));
    }

    @Test
    public void testSearchAtEnd() {
        assertEquals(5, StringUtils.indexOf("hello!", "!"));
    }

    @Test
    public void testMultipleOccurrences_returnsFirst() {
        assertEquals(0, StringUtils.indexOf("abc abc", "abc"));
    }

    @Test
    public void testEmptySourceAndSearch() {
        assertEquals(0, StringUtils.indexOf("", ""));
    }
}
