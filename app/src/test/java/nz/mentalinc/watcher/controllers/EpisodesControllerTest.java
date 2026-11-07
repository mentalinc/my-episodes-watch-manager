package nz.mentalinc.watcher.controllers;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import nz.mentalinc.watcher.domain.Episode;
import nz.mentalinc.watcher.domain.Show;
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
        List<Episode> list = List.of(ep);
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH, list);
        assertSame(list, controller.getEpisodes(EpisodeType.EPISODES_TO_WATCH));
    }

    @Test
    public void testSetAndGetEpisodes_acquire() {
        Episode ep = new Episode();
        ep.setMyEpisodeID("2");
        List<Episode> list = List.of(ep);
        controller.setEpisodes(EpisodeType.EPISODES_TO_ACQUIRE, list);
        assertSame(list, controller.getEpisodes(EpisodeType.EPISODES_TO_ACQUIRE));
    }

    @Test
    public void testSetAndGetEpisodes_coming() {
        Episode ep = new Episode();
        ep.setMyEpisodeID("3");
        List<Episode> list = List.of(ep);
        controller.setEpisodes(EpisodeType.EPISODES_COMING, list);
        assertSame(list, controller.getEpisodes(EpisodeType.EPISODES_COMING));
    }

    @Test
    public void testAddEpisodes() {
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH, new ArrayList<>());
        Episode ep1 = new Episode();
        ep1.setMyEpisodeID("1");
        Episode ep2 = new Episode();
        ep2.setMyEpisodeID("2");
        controller.addEpisodes(EpisodeType.EPISODES_TO_WATCH, Arrays.asList(ep1, ep2));
        assertEquals(2, controller.getEpisodes(EpisodeType.EPISODES_TO_WATCH).size());
    }

    @Test
    public void testAddEpisode() {
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH, new ArrayList<>());
        Episode ep = new Episode();
        ep.setMyEpisodeID("1");
        controller.addEpisode(EpisodeType.EPISODES_TO_WATCH, ep);
        assertEquals(1, controller.getEpisodes(EpisodeType.EPISODES_TO_WATCH).size());
    }

    @Test
    public void testGetEpisodesCount() {
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH, new ArrayList<>());
        assertEquals(0, controller.getEpisodesCount(EpisodeType.EPISODES_TO_WATCH));

        Episode ep = new Episode();
        ep.setMyEpisodeID("1");
        controller.addEpisode(EpisodeType.EPISODES_TO_WATCH, ep);
        assertEquals(1, controller.getEpisodesCount(EpisodeType.EPISODES_TO_WATCH));
    }

    @Test
    public void testDeleteEpisode() {
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH, new ArrayList<>());
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
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH, new ArrayList<>());
        controller.setEpisodes(EpisodeType.EPISODES_TO_ACQUIRE, new ArrayList<>());
        controller.setEpisodes(EpisodeType.EPISODES_COMING, new ArrayList<>());
        assertTrue(controller.areListsEmpty());
    }

    @Test
    public void testAreListsEmpty_nonEmpty() {
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH, new ArrayList<>());
        controller.setEpisodes(EpisodeType.EPISODES_TO_ACQUIRE, new ArrayList<>());
        controller.setEpisodes(EpisodeType.EPISODES_COMING, new ArrayList<>());

        Episode ep = new Episode();
        ep.setMyEpisodeID("1");
        controller.addEpisode(EpisodeType.EPISODES_TO_WATCH, ep);
        assertFalse(controller.areListsEmpty());
    }

    @Test
    public void testGetRandomWatchEpisodeShowList_canBeCalledTwice() {
        Episode ep = new Episode();
        ep.setMyEpisodeID("random-show");
        ep.setShowName("Random Show");
        ep.setSeason(1);
        ep.setEpisode(1);
        ep.setName("Pilot");
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH, List.of(ep));

        List<Show> first = controller.getRandomWatchEpisodeShowList();
        List<Show> second = controller.getRandomWatchEpisodeShowList();

        assertEquals(1, first.size());
        assertEquals(1, second.size());
        assertEquals("Random Show", second.get(0).getShowName());
    }

    @Test
    public void testGetRandomWatchEpisodeShowList_emptyWhenNothingToWatch() {
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH, new ArrayList<>());

        assertTrue(controller.getRandomWatchEpisodeShowList().isEmpty());
    }

    @Test
    public void testGetEpisodesShows_watch() {
        Episode ep = new Episode();
        ep.setMyEpisodeID("sid1");
        ep.setShowName("Show1");
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH, List.of(ep));
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
        controller.setEpisodes(EpisodeType.EPISODES_TO_ACQUIRE, List.of(ep));
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
        controller.setEpisodes(EpisodeType.EPISODES_COMING, List.of(ep));
        controller.AddToComingShow(controller.getEpisodes(EpisodeType.EPISODES_COMING));

        HashMap<String, nz.mentalinc.watcher.domain.Show> shows =
                controller.getEpisodesShows(EpisodeType.COMING_BY_SHOW);
        assertNotNull(shows);
        assertTrue(shows.containsKey("sid3"));
    }

    @Test
    public void testAddToWatchShow_acceptsShowEpisodeList() {
        Episode first = new Episode();
        first.setMyEpisodeID("cme-show");
        first.setShowName("CME Show");
        first.setSeason(1);
        first.setEpisode(1);
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH,
                new ArrayList<>(Collections.singletonList(first)));
        controller.AddToWatchShow(controller.getEpisodes(EpisodeType.EPISODES_TO_WATCH));

        List<Episode> showEpisodes =
                controller.getShowTypeEpisodes(EpisodeType.WATCH_BY_SHOW, "cme-show");
        assertNotNull(showEpisodes);

        Episode acquired = new Episode();
        acquired.setMyEpisodeID("cme-show");
        acquired.setShowName("CME Show");
        acquired.setSeason(1);
        acquired.setEpisode(2);
        showEpisodes.add(acquired);

        // Regression: this used to throw ConcurrentModificationException because the method
        // appended to the very list it was iterating over. Any exception fails the test.
        controller.AddToWatchShow(showEpisodes);
    }

    @Test
    public void testAddToWatchShow_addsSingleEpisodeToExistingShow() {
        Episode first = new Episode();
        first.setMyEpisodeID("single-show");
        first.setShowName("Single Show");
        first.setSeason(1);
        first.setEpisode(1);
        controller.setEpisodes(EpisodeType.EPISODES_TO_WATCH,
                new ArrayList<>(Collections.singletonList(first)));
        controller.AddToWatchShow(controller.getEpisodes(EpisodeType.EPISODES_TO_WATCH));

        Episode acquired = new Episode();
        acquired.setMyEpisodeID("single-show");
        acquired.setShowName("Single Show");
        acquired.setSeason(1);
        acquired.setEpisode(2);
        controller.AddToWatchShow(Collections.singletonList(acquired));

        List<Episode> showEpisodes =
                controller.getShowTypeEpisodes(EpisodeType.WATCH_BY_SHOW, "single-show");
        assertNotNull(showEpisodes);
        assertEquals(2, showEpisodes.size());
        assertTrue(showEpisodes.contains(acquired));
    }
}
