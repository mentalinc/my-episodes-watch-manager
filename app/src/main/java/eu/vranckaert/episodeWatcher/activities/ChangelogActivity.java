package eu.vranckaert.episodeWatcher.activities;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import eu.vranckaert.episodeWatcher.R;
import eu.vranckaert.episodeWatcher.preferences.Preferences;
import eu.vranckaert.episodeWatcher.preferences.PreferencesKeys;
import eu.vranckaert.episodeWatcher.utils.ApplicationUtil;
import roboguice.activity.GuiceActivity;

import static eu.vranckaert.episodeWatcher.activities.HomeActivity.getContext;

public class ChangelogActivity extends GuiceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(savedInstanceState);

        LinearLayout text = findViewById(R.id.whatsNewTextEntries);

        Resources res = getResources();
        String[] textValues = res.getStringArray(R.array.whatsNewValues);
        String textValuesPrefix = getString(R.string.whatsNewListPrefix);

        for (String textValue : textValues) {
            TextView textView = new TextView(getApplicationContext());
            textView.setText(textValuesPrefix + textValue);
            textView.setTextColor(getContext().getTheme().getResources().getColor(R.color.session_foreground_past));

           // textView.setTextColor(ContextCompat.getColor(this,));


            text.addView(textView);
        }

        TextView version = findViewById(R.id.whatsNewVersion);
        version.setText(ApplicationUtil.getCurrentApplicationVersion(getApplicationContext()));
    }

    private void init(Bundle savedInstanceState) {
        setTheme(Preferences.getPreferenceInt(this, PreferencesKeys.THEME_KEY) == 0 ? android.R.style.Theme_Material_Light : android.R.style.Theme_Material);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.changelog);
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
