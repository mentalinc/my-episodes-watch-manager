package nz.mentalinc.watcher.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import nz.mentalinc.watcher.R;
import nz.mentalinc.watcher.utils.ApplicationUtil;


//import static nz.mentalinc.episodeWatcher.activities.HomeActivity.getContext;

public class ChangelogActivity extends Activity {
    private static final String LOG_TAG = AboutActivity.class.getSimpleName();

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
            String featureToAdd = textValuesPrefix + textValue;
            textView.setText(featureToAdd);
            textView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.on_text_white));
            text.addView(textView);
        }

        TextView version = findViewById(R.id.whatsNewVersion);
        version.setText(ApplicationUtil.getCurrentApplicationVersion(getApplicationContext()));

        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(LOG_TAG, "Home button clicked.");
                exit();
            }
        });
    }

    private void init(Bundle savedInstanceState) {
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
            default: //added for code quality
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.changelog);
    }


    public void onHomeClick(View v) {
        Intent home = new Intent(this, HomeActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(home);
    }

    private void exit() {
    }
}
