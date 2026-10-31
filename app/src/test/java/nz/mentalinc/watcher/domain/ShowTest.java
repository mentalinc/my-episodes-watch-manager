package nz.mentalinc.watcher.domain;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ShowTest {
    private Show show;

    @Before
    public void setUp() {
        show = new Show("Test Show", "runtime123");
    }

    @Test
    public void testConstructor_nameAndId() {
        Show s = new Show("My Show", "eps42");
        assertEquals("My Show", s.getShowName());
        assertEquals("eps42", s.getMyEpisodeID());
        assertNull(s.getRunTime());
    }

    @Test
    public void testConstructor_nameOnly() {
        Show s = new Show("Just Name");
        assertEquals("Just Name", s.getShowName());
        assertNull(s.getMyEpisodeID());
    }

    @Test
    public void testConstructor_nameRuntimeAndId() {
        Show s = new Show("Full Show", "30 mins", "eps99");
        assertEquals("Full Show", s.getShowName());
        assertEquals("30 mins", s.getRunTime());
        assertEquals("eps99", s.getMyEpisodeID());
    }

    @Test
    public void testAddEpisode() {
        Episode ep = new Episode();
        ep.setShowName("Test Show");
        ep.setName("Ep 1");
        show.addEpisode(ep);
        assertEquals(1, show.getNumberEpisodes());
        assertSame(ep, show.getFirstEpisode());
    }

    @Test
    public void testGetEpisodes_listIsMutable() {
        Episode ep1 = new Episode();
        ep1.setName("Ep 1");
        Episode ep2 = new Episode();
        ep2.setName("Ep 2");
        show.addEpisode(ep1);
        show.addEpisode(ep2);

        assertEquals(2, show.getEpisodes().size());
        assertTrue(show.getEpisodes().contains(ep1));
        assertTrue(show.getEpisodes().contains(ep2));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetFirstEpisode_emptyList_throws() {
        new Show("Empty").getFirstEpisode();
    }

    @Test
    public void testSetShowName() {
        show.setShowName("Updated Name");
        assertEquals("Updated Name", show.getShowName());
    }

    @Test
    public void testSetMyEpisodeID() {
        show.setMyEpisodeID("newID");
        assertEquals("newID", show.getMyEpisodeID());
    }

    @Test
    public void testRunTime() {
        show.setRunTime("45 mins");
        assertEquals("45 mins", show.getRunTime());
    }

    @Test
    public void testTVMazeWebSite() {
        show.setTVMazeWebSite("https://tvmaze.com/shows/1");
        assertEquals("https://tvmaze.com/shows/1", show.getTVMazeWebSite());
    }

    @Test
    public void testToString() {
        show.setRunTime("30");
        assertEquals("30 mins - Test Show", show.toString());
    }

    @Test
    public void testToString_nullRuntime() {
        Show s = new Show("No Runtime");
        assertEquals("null mins - No Runtime", s.toString());
    }
}
