package nz.mentalinc.watcher.controllers;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import nz.mentalinc.watcher.domain.Episode;
import nz.mentalinc.watcher.enums.EpisodeType;

import static org.junit.Assert.*;

public class EpisodesControllerTest {
    private EpisodesController controller;

    @Before
    public void setUp() {
        controller = EpisodesController.getInstance();
    }

    @Test
    public void testSingleton() {
        assertSame(EpisodesController.getInstance(), controller);
    }

    @Test
    public void testSetAndGetEpisodes_watch() {
        Episode ep = new Episode();
        ep.setMyEpisodeID("1");
        List<Episode> list = Arrays.asList(ep);
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH, list);
        assertSame(list, controller.getEpisodes(EpisodeType.EPISODES_TO_WATCH));
    }

    @Test
    public void testSetAndGetEpisodes_acquire() {
        Episode ep = new Episode();
        ep.setMyEpisodeID("2");
        List<Episode> list = Arrays.asList(ep);
        controller.setEpisodes(EpisodeType.EPISODES_TO_ACQUIRE, list);
        assertSame(list, controller.getEpisodes(EpisodeType.EPISODES_TO_ACQUIRE));
    }

    @Test
    public void testSetAndGetEpisodes_coming() {
        Episode ep = new Episode();
        ep.setMyEpisodeID("3");
        List<Episode> list = Arrays.asList(ep);
        controller.setEpisodes(EpisodeType.EPISODES_COMING, list);
        assertSame(list, controller.getEpisodes(EpisodeType.EPISODES_COMING));
    }

    @Test
    public void testAddEpisodes() {
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH, new ArrayList<Episode>());
        Episode ep1 = new Episode();
        ep1.setMyEpisodeID("1");
        Episode ep2 = new Episode();
        ep2.setMyEpisodeID("2");
        controller.addEpisodes(EpisodeType.EPISODES_TO_WATCH, Arrays.asList(ep1, ep2));
        assertEquals(2, controller.getEpisodes(EpisodeType.EPISODES_TO_WATCH).size());
    }

    @Test
    public void testAddEpisode() {
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH, new ArrayList<Episode>());
        Episode ep = new Episode();
        ep.setMyEpisodeID("1");
        controller.addEpisode(EpisodeType.EPISODES_TO_WATCH, ep);
        assertEquals(1, controller.getEpisodes(EpisodeType.EPISODES_TO_WATCH).size());
    }

    @Test
    public void testGetEpisodesCount() {
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH, new ArrayList<Episode>());
        assertEquals(0, controller.getEpisodesCount(EpisodeType.EPISODES_TO_WATCH));

        Episode ep = new Episode();
        ep.setMyEpisodeID("1");
        controller.addEpisode(EpisodeType.EPISODES_TO_WATCH, ep);
        assertEquals(1, controller.getEpisodesCount(EpisodeType.EPISODES_TO_WATCH));
    }

    @Test
    public void testDeleteEpisode() {
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH, new ArrayList<Episode>());
        Episode ep = new Episode();
        ep.setShowName("Show");
        ep.setSeason(1);
        ep.setEpisode(1);
        ep.setName("Ep");
        ep.setMyEpisodeID("1");
        controller.addEpisode(EpisodeType.EPISODES_TO_WATCH, ep);
        assertEquals(1, controller.getEpisodesCount(EpisodeType.EPISODES_TO_WATCH));

        controller.deleteEpisode(EpisodeType.EPISODES_TO_WATCH, ep);
        assertEquals(0, controller.getEpisodesCount(EpisodeType.EPISODES_TO_WATCH));
    }

    @Test
    public void testAreListsEmpty_allEmpty() {
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH, new ArrayList<Episode>());
        controller.setEpisodes(EpisodeType.EPISODES_TO_ACQUIRE, new ArrayList<Episode>());
        controller.setEpisodes(EpisodeType.EPISODES_COMING, new ArrayList<Episode>());
        assertTrue(controller.areListsEmpty());
    }

    @Test
    public void testAreListsEmpty_nonEmpty() {
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH, new ArrayList<Episode>());
        controller.setEpisodes(EpisodeType.EPISODES_TO_ACQUIRE, new ArrayList<Episode>());
        controller.setEpisodes(EpisodeType.EPISODES_COMING, new ArrayList<Episode>());

        Episode ep = new Episode();
        ep.setMyEpisodeID("1");
        controller.addEpisode(EpisodeType.EPISODES_TO_WATCH, ep);
        assertFalse(controller.areListsEmpty());
    }

    @Test
    public void testGetEpisodesShows_watch() {
        Episode ep = new Episode();
        ep.setMyEpisodeID("sid1");
        ep.setShowName("Show1");
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH, Arrays.asList(ep));
        controller.AddToWatchShow(controller.getEpisodes(EpisodeType.EPISODES_TO_WATCH));

        HashMap<String, nz.mentalinc.watcher.domain.Show> shows =
                controller.getEpisodesShows(EpisodeType.WATCH_BY_SHOW);
        assertNotNull(shows);
        assertTrue(shows.containsKey("sid1"));
    }

    @Test
    public void testGetEpisodesShows_acquire() {
        Episode ep = new Episode();
        ep.setMyEpisodeID("sid2");
        ep.setShowName("Show2");
        controller.setEpisodes(EpisodeType.EPISODES_TO_ACQUIRE, Arrays.asList(ep));
        controller.AddToAcquireShow(controller.getEpisodes(EpisodeType.EPISODES_TO_ACQUIRE));

        HashMap<String, nz.mentalinc.watcher.domain.Show> shows =
                controller.getEpisodesShows(EpisodeType.ACQUIRE_BY_SHOW);
        assertNotNull(shows);
        assertTrue(shows.containsKey("sid2"));
    }

    @Test
    public void testGetEpisodesShows_coming() {
        Episode ep = new Episode();
        ep.setMyEpisodeID("sid3");
        ep.setShowName("Show3");
        controller.setEpisodes(EpisodeType.EPISODES_COMING, Arrays.asList(ep));
        controller.AddToComingShow(controller.getEpisodes(EpisodeType.EPISODES_COMING));

        HashMap<String, nz.mentalinc.watcher.domain.Show> shows =
                controller.getEpisodesShows(EpisodeType.COMING_BY_SHOW);
        assertNotNull(shows);
        assertTrue(shows.containsKey("sid3"));
    }
}
