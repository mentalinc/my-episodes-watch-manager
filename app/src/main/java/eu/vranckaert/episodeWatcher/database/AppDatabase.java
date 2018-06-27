package eu.vranckaert.episodeWatcher.database;

import eu.vranckaert.episodeWatcher.service.EpisodeRuntime;


import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

@Database(entities = {EpisodeRuntime.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SeriesDAO getSeriesDAO();
}
