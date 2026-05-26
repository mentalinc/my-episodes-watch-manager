package nz.mentalinc.watcher.domain;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class FeedItemTest {
    private FeedItem item;

    @Before
    public void setUp() {
        item = new FeedItem();
        item.setGuid("guid-123");
        item.setTitle("Test Title");
        item.setLink("https://example.com");
        item.setDescription("Test Description");
    }

    @Test
    public void testGetters() {
        assertEquals("guid-123", item.getGuid());
        assertEquals("Test Title", item.getTitle());
        assertEquals("https://example.com", item.getLink());
        assertEquals("Test Description", item.getDescription());
    }

    @Test
    public void testSetters() {
        item.setGuid("new-guid");
        item.setTitle("New Title");
        item.setLink("https://new.com");
        item.setDescription("New Description");

        assertEquals("new-guid", item.getGuid());
        assertEquals("New Title", item.getTitle());
        assertEquals("https://new.com", item.getLink());
        assertEquals("New Description", item.getDescription());
    }

    @Test
    public void testToString() {
        String expected = "GUID: guid-123 | Title: Test Title | Link: https://example.com | Description: Test Description";
        assertEquals(expected, item.toString());
    }

    @Test
    public void testDefaultValues() {
        FeedItem empty = new FeedItem();
        assertNull(empty.getGuid());
        assertNull(empty.getTitle());
        assertNull(empty.getLink());
        assertNull(empty.getDescription());
    }
}
