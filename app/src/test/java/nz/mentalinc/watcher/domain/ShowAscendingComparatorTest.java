package nz.mentalinc.watcher.domain;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ShowAscendingComparatorTest {
    private final ShowAscendingComparator comparator = new ShowAscendingComparator();

    @Test
    public void testSortsAlphabetically() {
        Show a = new Show("Alpha");
        Show b = new Show("Bravo");
        Show c = new Show("Charlie");

        List<Show> shows = new ArrayList<>();
        shows.add(b);
        shows.add(c);
        shows.add(a);
        Collections.sort(shows, comparator);

        assertEquals("Alpha", shows.get(0).getShowName());
        assertEquals("Bravo", shows.get(1).getShowName());
        assertEquals("Charlie", shows.get(2).getShowName());
    }

    @Test
    public void testEqualNames_returnsZero() {
        Show a = new Show("Same");
        Show b = new Show("Same");
        assertEquals(0, comparator.compare(a, b));
    }

    @Test
    public void testCaseSensitive() {
        Show a = new Show("apple");
        Show z = new Show("Zebra");
        assertTrue(comparator.compare(a, z) > 0);
    }
}
