package nz.mentalinc.episodeWatcher.activities;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.SettingScreenFrag;

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
        findViewById(R.id.appBarSettingsLayout).setZ(100f);

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

