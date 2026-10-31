package nz.mentalinc.watcher.domain;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class EpisodeDescendingComparatorTest {
    private final EpisodeDescendingComparator comparator = new EpisodeDescendingComparator();

    private Episode makeEpisode(int season, int episode) {
        Episode e = new Episode();
        e.setSeason(season);
        e.setEpisode(episode);
        return e;
    }

    @Test
    public void testSortsBySeasonDescendingThenEpisodeDescending() {
        Episode s1e1 = makeEpisode(1, 1);
        Episode s1e2 = makeEpisode(1, 2);
        Episode s2e1 = makeEpisode(2, 1);

        List<Episode> eps = new ArrayList<>();
        eps.add(s1e1);
        eps.add(s2e1);
        eps.add(s1e2);
        Collections.sort(eps, comparator);

        assertEquals(2, eps.get(0).getSeason());
        assertEquals(2, eps.get(1).getEpisode());
        assertEquals(1, eps.get(2).getEpisode());
    }

    @Test
    public void testHigherSeasonFirst() {
        Episode s1 = makeEpisode(1, 1);
        Episode s2 = makeEpisode(2, 1);
        assertTrue(comparator.compare(s2, s1) < 0);
        assertTrue(comparator.compare(s1, s2) > 0);
    }

    @Test
    public void testSameSeasonHigherEpisodeFirst() {
        Episode e1 = makeEpisode(3, 5);
        Episode e2 = makeEpisode(3, 10);
        assertTrue(comparator.compare(e2, e1) < 0);
    }

    @Test
    public void testEqual_returnsZero() {
        Episode e1 = makeEpisode(2, 5);
        Episode e2 = makeEpisode(2, 5);
        assertEquals(0, comparator.compare(e1, e2));
    }
}
