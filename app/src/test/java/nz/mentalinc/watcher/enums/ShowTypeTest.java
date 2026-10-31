package nz.mentalinc.watcher.enums;

import org.junit.Test;

import static org.junit.Assert.*;

public class ShowTypeTest {
    @Test
    public void testValues() {
        assertEquals(2, ShowType.values().length);
    }

    @Test
    public void testConstants() {
        assertEquals(ShowType.FAVOURITE_SHOWS, ShowType.valueOf("FAVOURITE_SHOWS"));
        assertEquals(ShowType.IGNORED_SHOWS, ShowType.valueOf("IGNORED_SHOWS"));
    }
}
