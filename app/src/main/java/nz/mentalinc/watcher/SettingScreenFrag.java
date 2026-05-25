package nz.mentalinc.watcher;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import java.io.File;

import nz.mentalinc.watcher.BuildConfig;
import nz.mentalinc.watcher.constants.MyEpisodeConstants;

public class SettingScreenFrag extends PreferenceFragmentCompat {

    private static final String LOG_TAG = SettingScreenFrag.class.getSimpleName();
    private boolean refreshDialog;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_screen, rootKey);

        SwitchPreference daysBackwardEnable = findPreference("daysBackwardEnable");
        SwitchPreference runTimeEnable = findPreference("RunTime");
        SwitchPreference cacheEpisodesEnable = findPreference("CacheEpisodes");
        SwitchPreference disableAcquirePref = findPreference("disableAcquire");
        SwitchPreference disableComingPref = findPreference("disableComing");

        EditTextPreference daysBackCP = findPreference("daysBack");
        EditTextPreference daysForwardCP = findPreference("daysForward");

        ListPreference cacheAgingPref = findPreference("CacheFileAge");
        ListPreference showAcquireOrderingPref = findPreference("showAcquireOrder");
        ListPreference showComingOrderingPref = findPreference("showComingOrder");
        ListPreference showRuntimeOrderingPref = findPreference("showRuntimeOrder");

        if (daysBackwardEnable != null) {
            if (daysBackCP != null) {
                daysBackCP.setEnabled(daysBackwardEnable.isChecked());
            }
            daysBackwardEnable.setOnPreferenceChangeListener((preference, newValue) -> {
                refreshDialog = true;
                if (daysBackCP != null) {
                    daysBackCP.setEnabled((Boolean) newValue);
                }
                deleteCacheFiles();
                return true;
            });
        }

        if (runTimeEnable != null) {
            if (showRuntimeOrderingPref != null) {
                showRuntimeOrderingPref.setEnabled(runTimeEnable.isChecked());
            }
            runTimeEnable.setOnPreferenceChangeListener((preference, newValue) -> {
                refreshDialog = true;
                if (showRuntimeOrderingPref != null) {
                    showRuntimeOrderingPref.setEnabled((Boolean) newValue);
                }
                deleteCacheFiles();
                return true;
            });
        }

        if (daysBackCP != null) {
            daysBackCP.setOnPreferenceChangeListener((preference, newValue) -> {
                refreshDialog = true;
                if (MyEpisodeConstants.CACHE_EPISODES_ENABLED) {
                    deleteCacheFiles();
                }
                return true;
            });
        }

        if (daysForwardCP != null) {
            daysForwardCP.setOnPreferenceChangeListener((preference, newValue) -> {
                refreshDialog = true;
                if (MyEpisodeConstants.CACHE_EPISODES_ENABLED) {
                    deleteCacheFiles();
                }
                return true;
            });
        }

        if (cacheEpisodesEnable != null) {
            if (cacheAgingPref != null) {
                cacheAgingPref.setEnabled(cacheEpisodesEnable.isChecked());
            }
            cacheEpisodesEnable.setOnPreferenceChangeListener((preference, newValue) -> {
                refreshDialog = true;
                if (cacheAgingPref != null) {
                    cacheAgingPref.setEnabled((Boolean) newValue);
                }
                deleteCacheFiles();
                return true;
            });
        }

        if (cacheAgingPref != null) {
            cacheAgingPref.setOnPreferenceChangeListener((preference, newValue) -> {
                refreshDialog = true;
                deleteCacheFiles();
                return true;
            });
        }

        if (disableAcquirePref != null) {
            if (showAcquireOrderingPref != null) {
                showAcquireOrderingPref.setEnabled(!disableAcquirePref.isChecked());
            }
            disableAcquirePref.setOnPreferenceChangeListener((preference, newValue) -> {
                refreshDialog = true;
                if (showAcquireOrderingPref != null) {
                    showAcquireOrderingPref.setEnabled(!(Boolean) newValue);
                }
                return true;
            });
        }

        if (disableComingPref != null) {
            if (showComingOrderingPref != null) {
                showComingOrderingPref.setEnabled(!disableComingPref.isChecked());
            }
            disableComingPref.setOnPreferenceChangeListener((preference, newValue) -> {
                refreshDialog = true;
                if (showComingOrderingPref != null) {
                    showComingOrderingPref.setEnabled(!(Boolean) newValue);
                }
                return true;
            });
        }

        Preference[] reloadListeners = {
                findPreference("showWatchOrder"),
                findPreference("showAcquireOrder"),
                findPreference("showComingOrder"),
                findPreference("showRuntimeOrder"),
                findPreference("episodeOrder"),
                findPreference("AquireSettings"),
                findPreference("listingUnacquiredFilter"),
                findPreference("listingUnwatchedFilter"),
                findPreference("listingIgnoredFilter"),
                findPreference("listingPilotsFilter"),
                findPreference("listingLocalizedAirdatesFilter"),
                findPreference("ThemeSetting"),
                findPreference("language")
        };

        for (Preference pref : reloadListeners) {
            if (pref != null) {
                pref.setOnPreferenceChangeListener((preference, newValue) -> {
                    refreshDialog = true;
                    return true;
                });
            }
        }

        Preference aboutVersion = findPreference("aboutVersion");
        if (aboutVersion != null) {
            aboutVersion.setSummary("You're using Version " + BuildConfig.VERSION_NAME);
        }
    }

    public boolean isRefreshDialog() {
        return refreshDialog;
    }

    private void deleteCacheFiles() {
        deleteFile("Watch.xml");
        deleteFile("Acquire.xml");
        deleteFile("Coming.xml");
    }

    private boolean deleteFile(String fileName) {
        Context context = getContext();
        if (context != null) {
            File fileToDelete = new File(context.getFilesDir(), fileName);
            if (fileToDelete.exists()) {
                if (fileToDelete.delete()) {
                    Log.d(LOG_TAG, fileName + " deleted");
                    return true;
                } else {
                    Log.e(LOG_TAG, "ERROR deleting " + fileName);
                    return false;
                }
            }
        }
        return false;
    }
}
