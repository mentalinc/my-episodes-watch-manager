package nz.mentalinc.watcher.utils;

import android.util.Log;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import nz.mentalinc.watcher.constants.MyEpisodeConstants;

public final class CacheUtils {
    private static final String LOG_TAG = CacheUtils.class.getSimpleName();

    private CacheUtils() {
    }

    public static void deleteFile(File filetoDelete) {
        if (filetoDelete.exists()) {
            if (filetoDelete.delete()) {
                Log.d(LOG_TAG, filetoDelete.getName() + " deleted");
            } else {
                Log.e(LOG_TAG, "ERROR deleting " + filetoDelete.getName());
            }
        }
    }

    public static void deleteOldCacheFiles(File filetoDelete) {
        if (!MyEpisodeConstants.CACHE_EPISODES_ENABLED || MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE.equalsIgnoreCase("0")) {
            Log.d(LOG_TAG, "Cache aging is disabled. Cache files not deleted");
        } else {
            Date lastModDate = new Date(filetoDelete.lastModified());
            Date Now = new Date();
            Calendar ModDate = Calendar.getInstance();
            Calendar NowDate = Calendar.getInstance();
            ModDate.setTime(lastModDate);
            NowDate.setTime(Now);
            long milliseconds1 = ModDate.getTimeInMillis();
            long milliseconds2 = NowDate.getTimeInMillis();
            long diff = milliseconds2 - milliseconds1;
            long diffHours = diff / (60 * 60 * 1000);
            long diffDays = diff / (24 * 60 * 60 * 1000);
            Log.d(LOG_TAG, "Time in hours: " + diffHours + " hours.");
            Log.d(LOG_TAG, "Time in days: " + diffDays + " days.");
            Log.d(LOG_TAG, "Cache age setting: " + Double.parseDouble(MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE) + " days " + MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE);
            Log.d(LOG_TAG, "Filename: " + filetoDelete.getName() + " Diff: " + diffDays + " last modified @ : " + lastModDate);
            if (diffDays >= Double.parseDouble(MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE)) {
                Log.d(LOG_TAG, "Delete File too many DAYS old...");
                deleteFile(filetoDelete);
            } else if (Double.parseDouble(MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE) < 1) {
                if (diffHours >= 6 && MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE.equalsIgnoreCase("0.25")) {
                    Log.d(LOG_TAG, "Delete File too many HOURS old, Greater than 6...");
                    deleteFile(filetoDelete);
                }
                if (diffHours >= 12 && MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE.equalsIgnoreCase("0.5")) {
                    Log.d(LOG_TAG, "Delete File too many HOURS old, Greater than 12...");
                    deleteFile(filetoDelete);
                }
            } else {
                Log.d(LOG_TAG, filetoDelete.getName() + " cache not deleted. Cache still current.");
            }
        }
    }
}
