package nz.mentalinc.episodeWatcher.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import nz.mentalinc.episodeWatcher.service.EpisodeRuntime;

@Database(entities = {EpisodeRuntime.class}, version = 4)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SeriesDAO getSeriesDAO();

    private static volatile AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "EpisodeRuntime")
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }

    public static void closeInstance() {
        if (instance != null && instance.isOpen()) {
            instance.close();
        }
        instance = null;
    }
}
