package nz.mentalinc.episodeWatcher.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.utils.ApplicationUtil;


public class AboutActivity extends Activity {
    private static final String LOG_TAG = AboutActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String themeSetting = sharedPref.getString("ThemeSetting", "0");
        switch (themeSetting) {
            case "0":
                Log.d(LOG_TAG, "Theme Preference set as ThemeDayNight");
                setTheme(R.style.ThemeDayNight);
                break;
            case "1":
                Log.d(LOG_TAG, "Theme Preference set as ThemeLight");
                setTheme(R.style.ThemeLight);
                break;
            case "2":
                Log.d(LOG_TAG, "Theme Preference set as ThemeDark");
                setTheme(R.style.ThemeDark);
                break;
        }


        super.onCreate(savedInstanceState);
        //init(savedInstanceState);
        setContentView(R.layout.about);

        //Application version
        String version = ApplicationUtil.getCurrentApplicationVersion(this);

        Log.d(LOG_TAG, "Current version of the application: " + version);

        TextView textVersion = findViewById(R.id.aboutVersion);
        textVersion.setText(version);

        TextView aboutEmail = findViewById(R.id.aboutEmail);
        Linkify.addLinks(aboutEmail, Linkify.EMAIL_ADDRESSES);

        TextView aboutWebsite = findViewById(R.id.aboutWebsite);
        Linkify.addLinks(aboutWebsite, Linkify.WEB_URLS);

        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(LOG_TAG, "Home button clicked.");
                exit();
            }
        });
    }


    public void onHomeClick(View v) {
        exit();
    }

    private void exit() {
        Intent home = new Intent(this, HomeActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(home);
    }
}
