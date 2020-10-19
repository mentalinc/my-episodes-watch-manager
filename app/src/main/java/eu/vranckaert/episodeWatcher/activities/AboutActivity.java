package eu.vranckaert.episodeWatcher.activities;

import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import eu.vranckaert.episodeWatcher.R;
import eu.vranckaert.episodeWatcher.preferences.Preferences;
import eu.vranckaert.episodeWatcher.preferences.PreferencesKeys;
import eu.vranckaert.episodeWatcher.utils.ApplicationUtil;
import roboguice.activity.GuiceActivity;


public class AboutActivity extends GuiceActivity {
    private static final String LOG_TAG = AboutActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(savedInstanceState);

        //Application version
        String version = ApplicationUtil.getCurrentApplicationVersion(this);

        Log.d(LOG_TAG, "Current version of the application: " + version);

        TextView textVersion = findViewById(R.id.aboutVersion);
        textVersion.setText(version);

        TextView aboutEmail = findViewById(R.id.aboutEmail);
        Linkify.addLinks(aboutEmail, Linkify.EMAIL_ADDRESSES);

        TextView aboutWebsite = findViewById(R.id.aboutWebsite);
        Linkify.addLinks(aboutWebsite, Linkify.WEB_URLS);
    }

    private void init(Bundle savedInstanceState) {
        setTheme(Preferences.getPreferenceInt(this, PreferencesKeys.THEME_KEY) == 0 ? android.R.style.Theme_Material_Light : android.R.style.Theme_Material);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.show_management_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.closePreferences:
                finish();
                return true;
            case R.id.home:
                finish();
                return true;

        }
        return false;
    }


    public void onHomeClick(View v) {
        finish();
    }
}
