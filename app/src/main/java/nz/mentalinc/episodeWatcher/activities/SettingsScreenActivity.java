package nz.mentalinc.episodeWatcher.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.SettingScreenFrag;
import nz.mentalinc.episodeWatcher.utils.ApplicationUtil;

public class SettingsScreenActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String themeSetting = sharedPref.getString("ThemeSetting", "0");
        switch (themeSetting) {
            case "0":
                setTheme(R.style.ThemeDayNight);
                break;
            case "1":
                setTheme(R.style.ThemeLight);
                break;
            case "2":
                setTheme(R.style.ThemeDark);
                break;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_Layout, new SettingScreenFrag())
                .commit();


        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {

            //Log.w(LOG_TAG, "logout button clicked.");
            finish();
        });

    }
}

