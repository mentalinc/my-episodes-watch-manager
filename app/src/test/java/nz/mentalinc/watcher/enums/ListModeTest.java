package nz.mentalinc.watcher.enums;

import org.junit.Test;

import static org.junit.Assert.*;

public class ListModeTest {
    @Test
    public void testValues() {
        assertEquals(2, ListMode.values().length);
    }

    @Test
    public void testConstants() {
        assertEquals(ListMode.EPISODES_BY_SHOW, ListMode.valueOf("EPISODES_BY_SHOW"));
        assertEquals(ListMode.EPISODES_BY_DATE, ListMode.valueOf("EPISODES_BY_DATE"));
    }
}
