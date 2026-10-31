package nz.mentalinc.watcher.controllers;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class RowControllerTest {
    private RowController controller;

    @Before
    public void setUp() {
        controller = RowController.getInstance();
    }

    @Test
    public void testSingleton() {
        assertSame(RowController.getInstance(), controller);
    }

    @Test
    public void testOpenWatchRows_defaultEmpty() {
        assertTrue(controller.getOpenWatchRows().isEmpty());
    }

    @Test
    public void testSetOpenWatchRows() {
        List<String> rows = Arrays.asList("show1", "show2");
        controller.setOpenWatchRows(rows);
        assertEquals(rows, controller.getOpenWatchRows());
    }

    @Test
    public void testOpenAcquireRows_defaultEmpty() {
        assertTrue(controller.getOpenAcquireRows().isEmpty());
    }

    @Test
    public void testSetOpenAcquireRows() {
        List<String> rows = Arrays.asList("show3");
        controller.setOpenAcquireRows(rows);
        assertEquals(rows, controller.getOpenAcquireRows());
    }

    @Test
    public void testOpenComingRows_defaultEmpty() {
        assertTrue(controller.getOpenComingRows().isEmpty());
    }

    @Test
    public void testSetOpenComingRows() {
        List<String> rows = new ArrayList<>();
        controller.setOpenComingRows(rows);
        assertSame(rows, controller.getOpenComingRows());
    }
}
