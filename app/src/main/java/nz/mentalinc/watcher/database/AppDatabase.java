package nz.mentalinc.watcher.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import nz.mentalinc.watcher.service.EpisodeRuntime;

@Database(entities = {EpisodeRuntime.class}, version = 4)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SeriesDAO getSeriesDAO();

    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "EpisodeRuntime")
                    .fallbackToDestructiveMigration(false)
                    .build();
        }
        return instance;
    }

    public static void closeInstance() {
        synchronized (AppDatabase.class) {
            if (instance != null) {
                if (instance.isOpen()) {
                    instance.close();
                }
                instance = null;
            }
        }
    }
}
