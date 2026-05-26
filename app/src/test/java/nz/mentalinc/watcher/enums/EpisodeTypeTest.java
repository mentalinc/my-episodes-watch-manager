package nz.mentalinc.watcher.enums;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class EpisodeTypeTest {
    @Test
    public void testEnumValues() {
        assertEquals(8, EpisodeType.values().length);
    }

    @Test
    public void testGetEpisodeListingTypeList() {
        List<Integer> types = EpisodeType.getEpisodeListingTypeList();
        assertEquals(6, types.size());
        assertTrue(types.contains(0));
        assertTrue(types.contains(1));
        assertTrue(types.contains(2));
        assertTrue(types.contains(5));
        assertTrue(types.contains(6));
        assertTrue(types.contains(7));
    }

    @Test
    public void testGetEpisodeListingTypeList_excludesYesterdayTypes() {
        List<Integer> types = EpisodeType.getEpisodeListingTypeList();
        assertFalse(types.contains(3));
        assertFalse(types.contains(4));
    }

    @Test
    public void testGetEpisodeListingTypeArray() {
        CharSequence[] arr = EpisodeType.getEpisodeListingTypeArray();
        assertEquals(6, arr.length);
        assertEquals("0", arr[0].toString());
        assertEquals("1", arr[1].toString());
        assertEquals("2", arr[2].toString());
        assertEquals("5", arr[3].toString());
        assertEquals("6", arr[4].toString());
        assertEquals("7", arr[5].toString());
    }

    @Test
    public void testEnumConstants() {
        assertEquals(0, EpisodeType.EPISODES_TO_WATCH.ordinal());
        assertEquals(1, EpisodeType.EPISODES_TO_ACQUIRE.ordinal());
        assertEquals(2, EpisodeType.EPISODES_COMING.ordinal());
        assertEquals(3, EpisodeType.EPISODES_TO_YESTERDAY1.ordinal());
        assertEquals(4, EpisodeType.EPISODES_TO_YESTERDAY2.ordinal());
        assertEquals(5, EpisodeType.WATCH_BY_SHOW.ordinal());
        assertEquals(6, EpisodeType.ACQUIRE_BY_SHOW.ordinal());
        assertEquals(7, EpisodeType.COMING_BY_SHOW.ordinal());
    }
}
