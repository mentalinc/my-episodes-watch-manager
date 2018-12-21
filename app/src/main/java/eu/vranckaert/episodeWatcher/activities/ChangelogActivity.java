package eu.vranckaert.episodeWatcher.activities;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import eu.vranckaert.episodeWatcher.R;
import eu.vranckaert.episodeWatcher.utils.ApplicationUtil;
import roboguice.activity.GuiceActivity;

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
            text.addView(textView);
        }

        TextView version = findViewById(R.id.whatsNewVersion);
        version.setText(ApplicationUtil.getCurrentApplicationVersion(getApplicationContext()));
    }

    private void init(Bundle savedInstanceState) {
        //setTheme(Preferences.getPreferenceInt(this, PreferencesKeys.THEME_KEY) == 0 ? android.R.style.Theme_Light_NoTitleBar : android.R.style.Theme_NoTitleBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.changelog);

        ((TextView) findViewById(R.id.title_text)).setText(R.string.whatsNew);
    }

    public void onHomeClick(View v) {
        finish();
    }
}
