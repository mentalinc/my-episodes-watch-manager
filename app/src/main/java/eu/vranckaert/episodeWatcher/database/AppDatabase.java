package eu.vranckaert.episodeWatcher.database;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;

import eu.vranckaert.episodeWatcher.service.EpisodeRuntime;


@Database(entities = {EpisodeRuntime.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SeriesDAO getSeriesDAO();

    static final Migration MIGRATION_1_2 = new Migration(1, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE EpisodeRuntime ADD COLUMN showSummary TEXT");
            database.execSQL("ALTER TABLE EpisodeRuntime ADD COLUMN showImageURL TEXT");
            database.execSQL("ALTER TABLE EpisodeRuntime ADD COLUMN officialSite TEXT");
            database.execSQL("ALTER TABLE EpisodeRuntime ADD COLUMN showURL TEXT");
        }
    };
}


