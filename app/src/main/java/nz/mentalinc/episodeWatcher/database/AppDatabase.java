package nz.mentalinc.episodeWatcher.database;


import androidx.room.Database;
import androidx.room.RoomDatabase;

import nz.mentalinc.episodeWatcher.service.EpisodeRuntime;


@Database(entities = {EpisodeRuntime.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SeriesDAO getSeriesDAO();

}


