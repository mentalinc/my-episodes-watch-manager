package nz.mentalinc.watcher.domain;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import nz.mentalinc.watcher.enums.EpisodeType;

import static org.junit.Assert.*;

public class EpisodeTest {
    private Episode episode;

    @Before
    public void setUp() {
        episode = new Episode();
        episode.setShowName("Test Show");
        episode.setName("Test Episode");
        episode.setSeason(3);
        episode.setEpisode(7);
        episode.setAirDate(new Date(1700000000000L));
        episode.setMyEpisodeID("12345");
        episode.setType(EpisodeType.EPISODES_TO_WATCH);
        episode.setTVMazeWebSite("https://tvmaze.com/episodes/1");
    }

    @Test
    public void testGetters() {
        assertEquals("Test Show", episode.getShowName());
        assertEquals("Test Episode", episode.getName());
        assertEquals(3, episode.getSeason());
        assertEquals(7, episode.getEpisode());
        assertEquals("12345", episode.getMyEpisodeID());
        assertEquals(EpisodeType.EPISODES_TO_WATCH, episode.getType());
        assertEquals("https://tvmaze.com/episodes/1", episode.getTVMazeWebSite());
    }

    @Test
    public void testSeasonString_under10() {
        episode.setSeason(5);
        assertEquals("05", episode.getSeasonString());
    }

    @Test
    public void testSeasonString_10orMore() {
        episode.setSeason(12);
        assertEquals("12", episode.getSeasonString());
    }

    @Test
    public void testSeasonString_zero() {
        episode.setSeason(0);
        assertEquals("00", episode.getSeasonString());
    }

    @Test
    public void testEpisodeString_under10() {
        episode.setEpisode(3);
        assertEquals("03", episode.getEpisodeString());
    }

    @Test
    public void testEpisodeString_10orMore() {
        episode.setEpisode(15);
        assertEquals("15", episode.getEpisodeString());
    }

    @Test
    public void testEpisodeString_zero() {
        episode.setEpisode(0);
        assertEquals("00", episode.getEpisodeString());
    }

    @Test
    public void testToString() {
        Date date = new Date(1700000000000L);
        episode.setAirDate(date);
        String expected = "Test Show S03E07 - Test Episode (12345) (" + date + ")";
        assertEquals(expected, episode.toString());
    }

    @Test
    public void testSettersMutateState() {
        episode.setShowName("New Show");
        episode.setName("New Episode");
        episode.setSeason(5);
        episode.setEpisode(10);
        episode.setMyEpisodeID("67890");
        episode.setType(EpisodeType.EPISODES_TO_ACQUIRE);
        episode.setTVMazeWebSite("https://tvmaze.com/episodes/2");

        assertEquals("New Show", episode.getShowName());
        assertEquals("New Episode", episode.getName());
        assertEquals(5, episode.getSeason());
        assertEquals(10, episode.getEpisode());
        assertEquals("10", episode.getEpisodeString());
        assertEquals("67890", episode.getMyEpisodeID());
        assertEquals(EpisodeType.EPISODES_TO_ACQUIRE, episode.getType());
        assertEquals("https://tvmaze.com/episodes/2", episode.getTVMazeWebSite());
    }

    @Test
    public void testAirDate() {
        Date now = new Date();
        episode.setAirDate(now);
        assertSame(now, episode.getAirDate());
    }
}
