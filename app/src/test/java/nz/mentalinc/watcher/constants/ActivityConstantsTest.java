package nz.mentalinc.watcher.constants;

import org.junit.Test;

import static org.junit.Assert.*;

public class ActivityConstantsTest {
    @Test
    public void testBundleKeys() {
        assertEquals("Type", ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE);
        assertEquals("episode", ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE);
        assertEquals("showID", ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID);
        assertEquals("markEpisode", ActivityConstants.EXTRA_BUNDLE_VAR_MARK_EPISODE);
        assertEquals("listMode", ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE);
    }

    @Test
    public void testBundleValues() {
        assertEquals("watch", ActivityConstants.EXTRA_BUNDLE_VALUE_WATCH);
        assertEquals("acquire", ActivityConstants.EXTRA_BUNDLE_VALUE_ACQUIRE);
    }
}
