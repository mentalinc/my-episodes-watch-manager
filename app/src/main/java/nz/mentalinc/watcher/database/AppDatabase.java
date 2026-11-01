package nz.mentalinc.watcher.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import nz.mentalinc.watcher.service.EpisodeRuntime;

@Database(entities = {EpisodeRuntime.class}, version = 4)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SeriesDAO getSeriesDAO();

    private static AppDatabase instance;

    /**
     * v1 -> v4: adds the summary/site columns.
     */
    static final Migration MIGRATION_1_4 = new Migration(1, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE EpisodeRuntime ADD COLUMN showSummary TEXT");
            db.execSQL("ALTER TABLE EpisodeRuntime ADD COLUMN showImageURL TEXT");
            db.execSQL("ALTER TABLE EpisodeRuntime ADD COLUMN officialSite TEXT");
            db.execSQL("ALTER TABLE EpisodeRuntime ADD COLUMN showURL TEXT");
            db.execSQL("ALTER TABLE EpisodeRuntime ADD COLUMN ShowStatus TEXT");
        }
    };

    /**
     * v2/v3 -> v4: adds the show status column.
     */
    static final Migration MIGRATION_2_4 = new Migration(2, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE EpisodeRuntime ADD COLUMN ShowStatus TEXT");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE EpisodeRuntime ADD COLUMN ShowStatus TEXT");
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "EpisodeRuntime")
                    .addMigrations(MIGRATION_1_4, MIGRATION_2_4, MIGRATION_3_4)
                    // Runtime data is a cache that is re-fetched from TVMaze, so falling back to
                    // a destructive migration for unknown schema versions is safe. Without this,
                    // an upgrade from an unhandled version throws and the app cannot open the DB.
                    .fallbackToDestructiveMigration(true)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
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
