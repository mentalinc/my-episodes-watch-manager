package eu.vranckaert.episodeWatcher.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.File;

import eu.vranckaert.episodeWatcher.R;
import eu.vranckaert.episodeWatcher.constants.MyEpisodeConstants;
import eu.vranckaert.episodeWatcher.preferences.Preferences;
import eu.vranckaert.episodeWatcher.preferences.PreferencesKeys;
import roboguice.activity.GuicePreferenceActivity;

/**
 * @author Ivo Janssen
 */

public class PreferencesActivity extends GuicePreferenceActivity {
    private static final int RELOAD_DIALOG = 0;
    private boolean refreshDialog;
    private EditTextPreference daysBackCP;
    private ListPreference cacheAgingPref;
    private ListPreference RunTimeEnablePref;
    private ListPreference showAcquireOrderingPref;
    private ListPreference showComingOrderingPref;
    private ListPreference showRuntimeOrderingPref;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.preferences_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.closePreferences:
                finish();
                return true;
        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        //setTheme(Preferences.getPreferenceInt(this, PreferencesKeys.THEME_KEY) == 0 ? android.R.style.Theme_Light_NoTitleBar : android.R.style.Theme_NoTitleBar);
        super.onCreate(savedInstance);

        setContentView(R.layout.preferences);

        ((TextView) findViewById(R.id.title_text)).setText(R.string.preferences);

        getPreferenceManager().setSharedPreferencesName(Preferences.PREF_NAME);
        PreferenceScreen screen = createPreferenceScreen();

        setPreferenceScreen(screen);
    }

    private PreferenceScreen createPreferenceScreen() {
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

        PreferenceCategory general = new PreferenceCategory(this);
        general.setTitle(R.string.generalPreferences);
        root.addPreference(general);

        CheckBoxPreference passwordPref = new CheckBoxPreference(this);
        passwordPref.setDefaultValue(true);
        passwordPref.setKey(PreferencesKeys.STORE_PASSWORD_KEY);
        passwordPref.setTitle(R.string.storePasswordPrompt);
        root.addPreference(passwordPref);

        ListPreference openThemePref = new ListPreference(this);
        openThemePref.setOnPreferenceChangeListener((preference, newValue) -> {
            refreshDialog = true;
            return true;
        });
        openThemePref.setKey(PreferencesKeys.THEME_KEY);
        openThemePref.setTitle(R.string.ThemePrompt);
        openThemePref.setSummary(R.string.ThemePromptExtra);
        openThemePref.setEntries(R.array.ThemeOptions);
        openThemePref.setEntryValues(R.array.ThemeValues);
        root.addPreference(openThemePref);

        PreferenceCategory showSettings = new PreferenceCategory(this);
        showSettings.setTitle(R.string.showPreferences);
        root.addPreference(showSettings);

        final CheckBoxPreference daysBackwardEnable = new CheckBoxPreference(this);
        daysBackwardEnable.setDefaultValue(false);
        daysBackwardEnable.setKey(PreferencesKeys.DAYS_BACKWARD_ENABLED_KEY);
        daysBackwardEnable.setTitle(R.string.daysBackwardEnable);
        daysBackwardEnable.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                refreshDialog = true;
                if (daysBackwardEnable.isChecked()) {
                    daysBackCP.setEnabled(false);
                } else {
                    daysBackCP.setEnabled(true);
                }

                deleteFile("Watch.xml");
                deleteFile("Acquire.xml");
                deleteFile("Coming.xml");
                return true;
            }
        });
        root.addPreference(daysBackwardEnable);


        final CheckBoxPreference RunTimeEnable = new CheckBoxPreference(this);
        RunTimeEnable.setDefaultValue(false);
        RunTimeEnable.setKey(PreferencesKeys.RUNTIME_ENABLED_KEY);
        RunTimeEnable.setTitle(R.string.RunTime);
        RunTimeEnable.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                refreshDialog = true;

                if (RunTimeEnable.isChecked()) {
                    showRuntimeOrderingPref.setEnabled(false);
                } else {
                    showRuntimeOrderingPref.setEnabled(true);
                }

                deleteFile("Watch.xml");
                deleteFile("Acquire.xml");
                deleteFile("Coming.xml");

                return true;
            }
        });
        root.addPreference(RunTimeEnable);
        Log.d("RunTimeEnable: value", RunTimeEnable.getKey());

        daysBackCP = new EditTextPreference(this);
        daysBackCP.setTitle(R.string.daysBackwardCP);
        daysBackCP.setKey(PreferencesKeys.DAYS_BACKWARDCP);
        daysBackCP.setSummary(R.string.daysBackwardCPExtra);
        daysBackCP.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);

        daysBackCP.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                refreshDialog = true;

                if (MyEpisodeConstants.CACHE_EPISODES_ENABLED) {
                    //delete the current Cache file so reload downloads ne days back
                    deleteFile("Watch");
                    deleteFile("Acquire.xml");
                    deleteFile("Coming.xml");
                }

                return true;
            }
        });

        if (!daysBackwardEnable.isChecked()) {
            daysBackCP.setEnabled(false);
        }

        root.addPreference(daysBackCP);

        //Issue 109
        final CheckBoxPreference CacheEpisodesEnable = new CheckBoxPreference(this);
        CacheEpisodesEnable.setDefaultValue(false);
        CacheEpisodesEnable.setKey(PreferencesKeys.CACHE_EPISODES_ENABLED_KEY);
        CacheEpisodesEnable.setTitle(R.string.CacheEpisodes);
        CacheEpisodesEnable.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                refreshDialog = true;

                if (CacheEpisodesEnable.isChecked()) {
                    cacheAgingPref.setEnabled(false);
                } else {
                    cacheAgingPref.setEnabled(true);
                }

                deleteFile("Watch.xml");
                deleteFile("Acquire.xml");
                deleteFile("Coming.xml");

                return true;
            }
        });

        root.addPreference(CacheEpisodesEnable);
        //Issue 109 - Ends


        cacheAgingPref = new ListPreference(this);
        cacheAgingPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                refreshDialog = true;

                deleteFile("Watch.xml");
                deleteFile("Acquire.xml");
                deleteFile("Coming.xml");

                return true;
            }
        });
        cacheAgingPref.setKey(PreferencesKeys.CACHE_EPISODES_CACHE_AGE);
        cacheAgingPref.setDefaultValue("Disabled");
        cacheAgingPref.setTitle(R.string.CacheFileAge);
        cacheAgingPref.setEntries(R.array.CacheFileAgeArray);
        cacheAgingPref.setEntryValues(R.array.CacheFileAgeValues);


        if (!CacheEpisodesEnable.isChecked()) {
            cacheAgingPref.setEnabled(false);
        }

        root.addPreference(cacheAgingPref);


        ListPreference openAcquirePref = new ListPreference(this);
        openAcquirePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                refreshDialog = true;
                return true;
            }
        });
        openAcquirePref.setKey(PreferencesKeys.ACQUIRE_KEY);
        openAcquirePref.setTitle(R.string.openAcquirePrompt);
        openAcquirePref.setSummary(R.string.openAcquirePromptExtra);
        openAcquirePref.setEntries(R.array.openAcquireOptions);
        openAcquirePref.setEntryValues(R.array.AcquireValues);
        root.addPreference(openAcquirePref);

        PreferenceCategory disableSettings = new PreferenceCategory(this);
        disableSettings.setTitle(R.string.disablePreferences);
        root.addPreference(disableSettings);

        final CheckBoxPreference disableAcquirePref = new CheckBoxPreference(this);
        disableAcquirePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                refreshDialog = true;
                if (disableAcquirePref.isChecked()) {
                    showAcquireOrderingPref.setEnabled(true);
                } else {
                    showAcquireOrderingPref.setEnabled(false);
                }
                return true;
            }
        });
        disableAcquirePref.setDefaultValue(false);
        disableAcquirePref.setKey(PreferencesKeys.DISABLE_ACQUIRE);
        disableAcquirePref.setTitle(R.string.disableAcquire);
        root.addPreference(disableAcquirePref);

        final CheckBoxPreference disableComingPref = new CheckBoxPreference(this);
        disableComingPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                refreshDialog = true;
                if (disableComingPref.isChecked()) {
                    showComingOrderingPref.setEnabled(true);
                } else {
                    showComingOrderingPref.setEnabled(false);
                }
                return true;
            }
        });
        disableComingPref.setDefaultValue(false);
        disableComingPref.setKey(PreferencesKeys.DISABLE_COMING);
        disableComingPref.setTitle(R.string.disableComing);
        root.addPreference(disableComingPref);


        PreferenceCategory orderSettings = new PreferenceCategory(this);
        orderSettings.setTitle(R.string.orderPreferences);
        root.addPreference(orderSettings);

        ListPreference showWatchOrderingPref = new ListPreference(this);
        showWatchOrderingPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                refreshDialog = true;
                return true;
            }
        });
        showWatchOrderingPref.setKey(PreferencesKeys.WATCH_SHOW_SORTING_KEY);
        showWatchOrderingPref.setTitle(R.string.showWatchOrderPrompt);
        showWatchOrderingPref.setEntries(R.array.showOrderOptions);
        showWatchOrderingPref.setEntryValues(R.array.showOrderOptionsValues);
        root.addPreference(showWatchOrderingPref);

        showAcquireOrderingPref = new ListPreference(this);
        showAcquireOrderingPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                refreshDialog = true;
                return true;
            }
        });
        showAcquireOrderingPref.setKey(PreferencesKeys.ACQUIRE_SHOW_SORTING_KEY);
        showAcquireOrderingPref.setTitle(R.string.showAcquireOrderPrompt);
        showAcquireOrderingPref.setEntries(R.array.showOrderOptions);
        showAcquireOrderingPref.setEntryValues(R.array.showOrderOptionsValues);
        root.addPreference(showAcquireOrderingPref);

        if (disableAcquirePref.isChecked()) {
            showAcquireOrderingPref.setEnabled(false);
        }

        showComingOrderingPref = new ListPreference(this);
        showComingOrderingPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                refreshDialog = true;
                return true;
            }
        });
        showComingOrderingPref.setKey(PreferencesKeys.COMING_SHOW_SORTING_KEY);
        showComingOrderingPref.setTitle(R.string.showComingOrderPrompt);
        showComingOrderingPref.setEntries(R.array.showOrderOptions);
        showComingOrderingPref.setEntryValues(R.array.showOrderOptionsValues);
        root.addPreference(showComingOrderingPref);

        if (disableComingPref.isChecked()) {
            showComingOrderingPref.setEnabled(false);
        }


        showRuntimeOrderingPref = new ListPreference(this);
        showRuntimeOrderingPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                refreshDialog = true;
                return true;
            }
        });
        showRuntimeOrderingPref.setKey(PreferencesKeys.RUNTIME_SHOW_SORTING_KEY);
        showRuntimeOrderingPref.setTitle(R.string.showRuntimeOrderPrompt);
        showRuntimeOrderingPref.setEntries(R.array.showOrderOptions);
        showRuntimeOrderingPref.setEntryValues(R.array.showOrderOptionsValues);
        root.addPreference(showRuntimeOrderingPref);

        if (RunTimeEnable.isChecked()) {
            showRuntimeOrderingPref.setEnabled(true);
        }


        ListPreference episodeOrderingPref = new ListPreference(this);
        episodeOrderingPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                refreshDialog = true;
                return true;
            }
        });
        episodeOrderingPref.setKey(PreferencesKeys.EPISODE_SORTING_KEY);
        episodeOrderingPref.setTitle(R.string.episodeOrderPrompt);
        episodeOrderingPref.setSummary(R.string.episodeOrderPromptExtra);
        episodeOrderingPref.setEntries(R.array.episodeOrderOptions);
        episodeOrderingPref.setEntryValues(R.array.episodeOrderOptionsValues);
        root.addPreference(episodeOrderingPref);

        PreferenceCategory languageSettings = new PreferenceCategory(this);
        languageSettings.setTitle(R.string.languagePreferences);
        root.addPreference(languageSettings);

        ListPreference showLanguagePref = new ListPreference(this);
        showLanguagePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                refreshDialog = true;
                return true;
            }
        });
        showLanguagePref.setKey(PreferencesKeys.LANGUAGE_KEY);
        showLanguagePref.setTitle(R.string.showLanguagePrompt);
        showLanguagePref.setEntries(R.array.showLanguageOptions);
        showLanguagePref.setEntryValues(R.array.showLanguageOptionsValues);
        root.addPreference(showLanguagePref);

        return root;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch (id) {
            case RELOAD_DIALOG:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.attention)
                        .setMessage(R.string.applyPreferences)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialogOK, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                startTabMain();
                            }
                        });
                dialog = builder.create();
                break;
            default:
                dialog = super.onCreateDialog(id);
                break;
        }
        return dialog;
    }

    @Override
    public void finish() {
        Log.d("PreferencesActivity", "Closing the preferences screen!");

        if (refreshDialog) {
            showDialog(RELOAD_DIALOG);
        } else {
            super.finish();
        }
    }

    public void onHomeClick(View v) {
        finish();
    }

    private void startTabMain() {
        setResult(RESULT_OK);
        super.finish();
    }


    public boolean deleteFile(String fileNametoDelete) {

        File filetoDelete = new File(MyEpisodeConstants.CONTEXT.getFilesDir(), fileNametoDelete);
        if (filetoDelete.exists()) {
            String LOG_TAG = "PreferencesActivity";
            if (filetoDelete.delete()) {
                Log.d(LOG_TAG, filetoDelete.getName() + " deleted");
                return true;
            } else {
                Log.e(LOG_TAG, "ERROR deleting " + filetoDelete.getName());
                return false;
            }
        }
        return false;
    }
}