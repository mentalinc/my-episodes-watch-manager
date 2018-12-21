package eu.vranckaert.episodeWatcher.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import eu.vranckaert.episodeWatcher.service.EpisodeRuntime;


@Database(entities = {EpisodeRuntime.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SeriesDAO getSeriesDAO();
}
