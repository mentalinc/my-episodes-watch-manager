package nz.mentalinc.watcher.database;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import nz.mentalinc.watcher.service.EpisodeRuntime;


@Dao
public interface SeriesDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EpisodeRuntime... contacts);

    @Update
    void update(EpisodeRuntime... contacts);

    @Delete
    void delete(EpisodeRuntime contact);

    @Query("SELECT * FROM EpisodeRuntime")
    List<EpisodeRuntime> getEpisodeRuntime();

    @Query("SELECT * FROM EpisodeRuntime WHERE showMyEpsID = :number")
    EpisodeRuntime getEpisodeRuntimeWithMyEpsId(String number);

    @Query("SELECT * FROM EpisodeRuntime WHERE showTVMazeID = :number")
    EpisodeRuntime getEpisodeRuntimeWithTVMazeId(String number);

    @Query("SELECT * FROM EpisodeRuntime WHERE showMyEpsID IN (:ids)")
    List<EpisodeRuntime> getEpisodeRuntimeWithMyEpsIds(List<String> ids);

    @Query("SELECT showMyEpsID FROM EpisodeRuntime WHERE showRuntime IS NOT NULL AND showRuntime != 'null' AND CAST(showRuntime AS INTEGER) <= :maxRuntime")
    List<String> getShowIdsWithRuntimeUnder(int maxRuntime);

    @Query("SELECT showMyEpsID FROM EpisodeRuntime WHERE showRuntime IS NOT NULL AND showRuntime != 'null' AND CAST(showRuntime AS INTEGER) > :maxLower AND CAST(showRuntime AS INTEGER) <= :maxRuntime")
    List<String> getShowIdsWithRuntimeBetween(int maxLower, int maxRuntime);

    @Query("SELECT showMyEpsID FROM EpisodeRuntime WHERE showRuntime IS NOT NULL AND showRuntime != 'null' AND CAST(showRuntime AS INTEGER) > :minRuntime")
    List<String> getShowIdsWithRuntimeAtLeast(int minRuntime);
}

