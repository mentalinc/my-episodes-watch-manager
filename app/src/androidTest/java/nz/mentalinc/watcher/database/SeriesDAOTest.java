package nz.mentalinc.watcher.database;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import nz.mentalinc.watcher.service.EpisodeRuntime;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SeriesDAOTest {
    private AppDatabase database;
    private SeriesDAO dao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = database.getSeriesDAO();
    }

    @After
    public void tearDown() {
        database.close();
    }

    private EpisodeRuntime createRuntime(String id, String runtime, String tvmazeId) {
        EpisodeRuntime er = new EpisodeRuntime();
        er.showMyEpsID = id;
        er.setShowRuntime(runtime);
        er.setShowTVMazeID(tvmazeId);
        er.setShowName("Show " + id);
        return er;
    }

    @Test
    public void testInsertAndGetAll() {
        EpisodeRuntime er = createRuntime("1", "30", "tv1");
        dao.insert(er);
        List<EpisodeRuntime> all = dao.getEpisodeRuntime();
        assertEquals(1, all.size());
        assertEquals("1", all.get(0).showMyEpsID);
    }

    @Test
    public void testInsertMultiple() {
        dao.insert(createRuntime("1", "30", "tv1"));
        dao.insert(createRuntime("2", "45", "tv2"));
        assertEquals(2, dao.getEpisodeRuntime().size());
    }

    @Test
    public void testGetEpisodeRuntimeWithMyEpsId() {
        dao.insert(createRuntime("show42", "60", "tv99"));
        EpisodeRuntime result = dao.getEpisodeRuntimeWithMyEpsId("show42");
        assertNotNull(result);
        assertEquals("show42", result.showMyEpsID);
        assertEquals("60", result.getShowRuntime());
    }

    @Test
    public void testGetEpisodeRuntimeWithMyEpsId_notFound() {
        assertNull(dao.getEpisodeRuntimeWithMyEpsId("nonexistent"));
    }

    @Test
    public void testGetEpisodeRuntimeWithTVMazeId() {
        dao.insert(createRuntime("1", "30", "tvmaze_abc"));
        EpisodeRuntime result = dao.getEpisodeRuntimeWithTVMazeId("tvmaze_abc");
        assertNotNull(result);
        assertEquals("1", result.showMyEpsID);
    }

    @Test
    public void testGetEpisodeRuntimeWithMyEpsIds() {
        dao.insert(createRuntime("a", "30", "tv1"));
        dao.insert(createRuntime("b", "45", "tv2"));
        dao.insert(createRuntime("c", "60", "tv3"));

        List<EpisodeRuntime> result = dao.getEpisodeRuntimeWithMyEpsIds(Arrays.asList("a", "c"));
        assertEquals(2, result.size());
    }

    @Test
    public void testUpdate() {
        dao.insert(createRuntime("1", "30", "tv1"));
        EpisodeRuntime loaded = dao.getEpisodeRuntimeWithMyEpsId("1");
        loaded.setShowRuntime("60");
        dao.update(loaded);

        EpisodeRuntime updated = dao.getEpisodeRuntimeWithMyEpsId("1");
        assertEquals("60", updated.getShowRuntime());
    }

    @Test
    public void testDelete() {
        EpisodeRuntime er = createRuntime("toDelete", "30", "tv1");
        dao.insert(er);
        assertEquals(1, dao.getEpisodeRuntime().size());
        dao.delete(er);
        assertEquals(0, dao.getEpisodeRuntime().size());
    }

    @Test
    public void testGetShowIdsWithRuntimeUnder() {
        dao.insert(createRuntime("id15", "15", "tv1"));
        dao.insert(createRuntime("id30", "30", "tv2"));
        dao.insert(createRuntime("id45", "45", "tv3"));

        List<String> under30 = dao.getShowIdsWithRuntimeUnder(30);
        assertTrue(under30.contains("id15"));
        assertTrue(under30.contains("id30"));
        assertFalse(under30.contains("id45"));
    }

    @Test
    public void testGetShowIdsWithRuntimeBetween() {
        dao.insert(createRuntime("id15", "15", "tv1"));
        dao.insert(createRuntime("id30", "30", "tv2"));
        dao.insert(createRuntime("id45", "45", "tv3"));
        dao.insert(createRuntime("id60", "60", "tv4"));

        List<String> between30and45 = dao.getShowIdsWithRuntimeBetween(30, 45);
        assertFalse(between30and45.contains("id15"));
        assertFalse(between30and45.contains("id30"));
        assertTrue(between30and45.contains("id45"));
        assertFalse(between30and45.contains("id60"));
    }

    @Test
    public void testGetShowIdsWithRuntimeAtLeast() {
        dao.insert(createRuntime("id30", "30", "tv1"));
        dao.insert(createRuntime("id45", "45", "tv2"));
        dao.insert(createRuntime("id60", "60", "tv3"));

        List<String> atLeast45 = dao.getShowIdsWithRuntimeAtLeast(45);
        assertFalse(atLeast45.contains("id30"));
        assertTrue(atLeast45.contains("id45"));
        assertTrue(atLeast45.contains("id60"));
    }

    @Test
    public void testNullRuntime_excludedFromQueries() {
        EpisodeRuntime er = new EpisodeRuntime();
        er.showMyEpsID = "nullRutime";
        er.setShowRuntime("null");
        dao.insert(er);

        List<String> under60 = dao.getShowIdsWithRuntimeUnder(60);
        assertFalse(under60.contains("nullRutime"));

        List<String> atLeast0 = dao.getShowIdsWithRuntimeAtLeast(0);
        assertFalse(atLeast0.contains("nullRutime"));
    }
}
