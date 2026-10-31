package nz.mentalinc.watcher.domain;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class FeedTest {
    private Feed feed;

    @Before
    public void setUp() {
        feed = new Feed();
    }

    @Test
    public void testNewFeed_hasEmptyItems() {
        assertTrue(feed.getItems().isEmpty());
    }

    @Test
    public void testAddItem() {
        FeedItem item = new FeedItem();
        feed.addItem(item);
        assertEquals(1, feed.getItems().size());
        assertSame(item, feed.getItems().get(0));
    }

    @Test
    public void testAddMultipleItems() {
        FeedItem item1 = new FeedItem();
        FeedItem item2 = new FeedItem();
        feed.addItem(item1);
        feed.addItem(item2);
        assertEquals(2, feed.getItems().size());
    }

    @Test
    public void testGetItems_returnsSameList() {
        FeedItem item = new FeedItem();
        feed.addItem(item);
        assertSame(feed.getItems(), feed.getItems());
    }
}
