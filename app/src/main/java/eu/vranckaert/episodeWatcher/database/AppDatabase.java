package eu.vranckaert.episodeWatcher.database;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;

import eu.vranckaert.episodeWatcher.service.EpisodeRuntime;


@Database(entities = {EpisodeRuntime.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SeriesDAO getSeriesDAO();

}


