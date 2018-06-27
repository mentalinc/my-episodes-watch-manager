package eu.vranckaert.episodeWatcher.database;


import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import eu.vranckaert.episodeWatcher.service.EpisodeRuntime;
import java.util.List;



@Dao
public interface SeriesDAO {
    @Insert
    public void insert(EpisodeRuntime... contacts);

    @Update
    public void update(EpisodeRuntime... contacts);

    @Delete
    public void delete(EpisodeRuntime contact);

    @Query("SELECT * FROM EpisodeRuntime")
    public List<EpisodeRuntime> getEpisodeRuntime();

    @Query("SELECT * FROM EpisodeRuntime WHERE showMyEpsID = :number")
    public EpisodeRuntime getEpisodeRuntimeWithMyEpsId(String number);

    @Query("SELECT * FROM EpisodeRuntime WHERE showTVMazeID = :number")
    public EpisodeRuntime getEpisodeRuntimeWithTVMazeId(String number);
}

