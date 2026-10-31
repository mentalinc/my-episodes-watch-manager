package nz.mentalinc.watcher.enums;

import org.junit.Test;

import static org.junit.Assert.*;

public class ShowActionTest {
    @Test
    public void testValues() {
        assertEquals(3, ShowAction.values().length);
    }

    @Test
    public void testConstants() {
        assertEquals(ShowAction.IGNORE, ShowAction.valueOf("IGNORE"));
        assertEquals(ShowAction.UNIGNORE, ShowAction.valueOf("UNIGNORE"));
        assertEquals(ShowAction.DELETE, ShowAction.valueOf("DELETE"));
    }
}
