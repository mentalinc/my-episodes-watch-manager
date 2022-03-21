package nz.mentalinc.episodeWatcher.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.preference.PreferenceManager;

import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.enums.ShowType;

/**
 * @author Ivo Janssen, maintained and updated by mentalinc
 */
public class ShowManagementPortalActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(savedInstanceState);
        loadButtons();
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
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_management_portal);


        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {
            finish();
        });
    }

    private void loadButtons() {
        Button favShowsButton = findViewById(R.id.selectionPanelFavoShows);
        Button ignoredShowsButton = findViewById(R.id.selectionPanelIgnoredShows);
        Button addShowsButton = findViewById(R.id.selectionPanelAddShows);
        Button ShowsRuntimeButton = findViewById(R.id.selectionPanelShowsRuntime);
        favShowsButton.setOnClickListener(view -> openFavouriteOrIgnoredShows(ShowType.FAVOURITE_SHOWS));
        ignoredShowsButton.setOnClickListener(view -> openFavouriteOrIgnoredShows(ShowType.IGNORED_SHOWS));
        addShowsButton.setOnClickListener(view -> openSearchActivity());
        ShowsRuntimeButton.setOnClickListener(view -> openRunTimeActivity());
    }

    private void openSearchActivity() {
        Intent searchIntent = new Intent(this.getApplicationContext(), ShowManagementAddActivity.class);
        searchIntent.putExtra("Title", getString(R.string.addShow));
        startActivity(searchIntent);
    }

    private void openRunTimeActivity() {
        Intent runTimeIntent = new Intent(this.getApplicationContext(), ShowManagementRunTimeActivity.class);
        runTimeIntent.putExtra("Title", getString(R.string.ShowRuntime));
        startActivity(runTimeIntent);
    }

    private void openFavouriteOrIgnoredShows(ShowType showType) {
        Intent intent = new Intent(this.getApplicationContext(), ShowManagementActivity.class);
        intent.putExtra(ShowType.class.getSimpleName(), showType);
        Log.e("Fav or Ignore show type", showType.toString());
        if (showType.toString().equals("FAVOURITE_SHOWS"))
            intent.putExtra("Title", getString(R.string.favouriteShows));
        else if (showType.toString().equals("IGNORED_SHOWS"))
            intent.putExtra("Title", getString(R.string.ignoredShows));
        startActivity(intent);
    }

    public void onHomeClick(View v) {
        finish();
    }
}
