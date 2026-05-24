package nz.mentalinc.episodeWatcher.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.SettingScreenFrag;
import nz.mentalinc.episodeWatcher.constants.ActivityConstants;
import nz.mentalinc.episodeWatcher.enums.EpisodeType;

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

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigationSettings);
        bottomNavigationView.getMenu().getItem(0).setChecked(true);
        bottomNavigationView.setOnItemSelectedListener(navigationItemSelectedListener);

    }

    private final NavigationBarView.OnItemSelectedListener navigationItemSelectedListener = new NavigationBarView.OnItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            final int nextItem = item.getItemId();
            if (R.id.barHome == nextItem) {
                finish();
                return true;
            } else if (R.id.barWatch == nextItem) {
                Intent watchIntent = new Intent(getApplicationContext(), ShowListingActivity.class);
                watchIntent.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_WATCH);
                watchIntent.putExtra("Title", getString(R.string.watch));
                startActivity(watchIntent);
                return true;
            } else if (R.id.barAcquire == nextItem) {
                Intent acquireIntent = new Intent(getApplicationContext(), ShowListingActivity.class);
                acquireIntent.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_ACQUIRE);
                acquireIntent.putExtra("Title", getString(R.string.acquire));
                startActivity(acquireIntent);
                return true;
            } else if (R.id.barComing == nextItem) {
                Intent comingIntent = new Intent(getApplicationContext(), ShowListingActivity.class);
                comingIntent.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_COMING);
                comingIntent.putExtra("Title", getString(R.string.coming));
                startActivity(comingIntent);
                return true;
            }
            return false;
        }
    };
}
