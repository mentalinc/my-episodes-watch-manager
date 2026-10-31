package nz.mentalinc.watcher.domain;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class EpisodeAscendingComparatorTest {
    private final EpisodeAscendingComparator comparator = new EpisodeAscendingComparator();

    private Episode makeEpisode(int season, int episode) {
        Episode e = new Episode();
        e.setSeason(season);
        e.setEpisode(episode);
        return e;
    }

    @Test
    public void testSortsBySeasonThenEpisode() {
        Episode s1e2 = makeEpisode(1, 2);
        Episode s1e1 = makeEpisode(1, 1);
        Episode s2e1 = makeEpisode(2, 1);
        Episode s1e3 = makeEpisode(1, 3);
        Episode s2e3 = makeEpisode(2, 3);

        List<Episode> eps = new ArrayList<>();
        eps.add(s2e3);
        eps.add(s1e2);
        eps.add(s2e1);
        eps.add(s1e3);
        eps.add(s1e1);
        Collections.sort(eps, comparator);

        assertEquals(1, eps.get(0).getEpisode());
        assertEquals(2, eps.get(1).getEpisode());
        assertEquals(3, eps.get(2).getEpisode());
        assertEquals(1, eps.get(3).getSeason());
        assertEquals(1, eps.get(4).getEpisode());
    }

    @Test
    public void testSameSeason() {
        Episode e1 = makeEpisode(3, 5);
        Episode e2 = makeEpisode(3, 10);
        assertTrue(comparator.compare(e1, e2) < 0);
        assertTrue(comparator.compare(e2, e1) > 0);
    }

    @Test
    public void testSameEpisode() {
        Episode e1 = makeEpisode(2, 5);
        Episode e2 = makeEpisode(2, 5);
        assertEquals(0, comparator.compare(e1, e2));
    }
}
