package nz.mentalinc.watcher.domain;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ShowRuntimeAscendingComparatorTest {
    private final ShowRuntimeAscendingComparator comparator = new ShowRuntimeAscendingComparator();

    @Test
    public void testErrorMinsFirst() {
        Show error = new Show("Error Show", "Error mins", "1");
        Show normal = new Show("Normal Show", "30", "2");

        assertTrue(comparator.compare(error, normal) < 0);
        assertTrue(comparator.compare(normal, error) > 0);
    }

    @Test
    public void testNullRuntimeBeforeNumeric() {
        Show nullShow = new Show("Null Show", "null", "1");
        Show normal = new Show("Normal Show", "30", "2");

        assertTrue(comparator.compare(nullShow, normal) < 0);
        assertTrue(comparator.compare(normal, nullShow) > 0);
    }

    @Test
    public void testErrorMinsBeforeNull() {
        Show error = new Show("Error Show", "Error mins", "1");
        Show nullShow = new Show("Null Show", "null", "2");

        assertTrue(comparator.compare(error, nullShow) < 0);
    }

    @Test
    public void testNumericAscending() {
        Show low = new Show("Low", "15", "1");
        Show mid = new Show("Mid", "30", "2");
        Show high = new Show("High", "60", "3");

        List<Show> shows = new ArrayList<>();
        shows.add(high);
        shows.add(low);
        shows.add(mid);
        Collections.sort(shows, comparator);

        assertEquals("15", shows.get(0).getRunTime());
        assertEquals("30", shows.get(1).getRunTime());
        assertEquals("60", shows.get(2).getRunTime());
    }

    @Test
    public void testSameRuntime_sortsByName() {
        Show apple = new Show("Apple", "30", "1");
        Show banana = new Show("Banana", "30", "2");

        assertTrue(comparator.compare(apple, banana) < 0);
        assertTrue(comparator.compare(banana, apple) > 0);
    }

    @Test
    public void testFullSortingOrder() {
        Show error = new Show("Error", "Error mins", "1");
        Show nullShow = new Show("Null", "null", "2");
        Show ten = new Show("Ten Min", "10", "3");
        Show thirtyA = new Show("Alpha Thirty", "30", "4");
        Show thirtyZ = new Show("Zeta Thirty", "30", "5");
        Show sixty = new Show("Sixty", "60", "6");

        List<Show> shows = new ArrayList<>();
        shows.add(thirtyZ);
        shows.add(sixty);
        shows.add(error);
        shows.add(ten);
        shows.add(nullShow);
        shows.add(thirtyA);
        Collections.sort(shows, comparator);

        assertEquals("Error mins", shows.get(0).getRunTime());
        assertEquals("null", shows.get(1).getRunTime());
        assertEquals("10", shows.get(2).getRunTime());
        assertEquals("30", shows.get(3).getRunTime());
        assertEquals("Alpha Thirty", shows.get(3).getShowName());
        assertEquals("30", shows.get(4).getRunTime());
        assertEquals("Zeta Thirty", shows.get(4).getShowName());
        assertEquals("60", shows.get(5).getRunTime());
    }

    @Test(expected = NumberFormatException.class)
    public void testNonNumericRuntime_throws() {
        Show invalid = new Show("Bad", "abc", "1");
        Show normal = new Show("Good", "30", "2");
        comparator.compare(normal, invalid);
    }
}
