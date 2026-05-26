package nz.mentalinc.watcher.domain;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ShowDescendingComparatorTest {
    private final ShowDescendingComparator comparator = new ShowDescendingComparator();

    @Test
    public void testSortsReverseAlphabetically() {
        Show a = new Show("Alpha");
        Show b = new Show("Bravo");
        Show c = new Show("Charlie");

        List<Show> shows = new ArrayList<>();
        shows.add(a);
        shows.add(b);
        shows.add(c);
        Collections.sort(shows, comparator);

        assertEquals("Charlie", shows.get(0).getShowName());
        assertEquals("Bravo", shows.get(1).getShowName());
        assertEquals("Alpha", shows.get(2).getShowName());
    }

    @Test
    public void testEqualNames_returnsZero() {
        Show a = new Show("Same");
        Show b = new Show("Same");
        assertEquals(0, comparator.compare(a, b));
    }
}
