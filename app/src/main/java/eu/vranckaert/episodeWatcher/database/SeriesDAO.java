package eu.vranckaert.episodeWatcher.database;


import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import eu.vranckaert.episodeWatcher.service.EpisodeRuntime;


@Dao
public interface SeriesDAO {
    @Insert
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

    @Query("SELECT * FROM EpisodeRuntime WHERE showMyEpsID = :number")
    EpisodeRuntime getTvmazeShowID(String number);

    @Query("DELETE FROM EpisodeRuntime WHERE showTVMazeID IS NULL")
    int deleteNullShow();

}

