package nz.mentalinc.watcher.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import nz.mentalinc.watcher.R;
import nz.mentalinc.watcher.controllers.EpisodesController;
import nz.mentalinc.watcher.database.AppDatabase;
import nz.mentalinc.watcher.database.SeriesDAO;
import nz.mentalinc.watcher.enums.ShowType;
import nz.mentalinc.watcher.service.EpisodeRuntime;
import nz.mentalinc.watcher.constants.ActivityConstants;
import nz.mentalinc.watcher.utils.TaskRunner;

/**
 * @author Ivo Janssen, maintained and updated by mentalinc
 */
public class ShowManagementPortalActivity extends Activity {

    private static final String LOG_TAG = EpisodesController.class.getSimpleName();

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
            default: //added for code quality
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_management_portal);
        findViewById(R.id.appBarLayoutPortal).setZ(100f);

        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {
            Intent home = new Intent(ShowManagementPortalActivity.this, HomeActivity.class);
            home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(home);
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigationManage);
        bottomNav.setOnItemSelectedListener(navigationItemSelectedListener);
    }

    private void loadButtons() {
        com.google.android.material.button.MaterialButton favShowsButton = findViewById(R.id.selectionPanelFavoShows);
        com.google.android.material.button.MaterialButton ignoredShowsButton = findViewById(R.id.selectionPanelIgnoredShows);
        com.google.android.material.button.MaterialButton addShowsButton = findViewById(R.id.selectionPanelAddShows);
        com.google.android.material.button.MaterialButton ShowsRuntimeButton = findViewById(R.id.selectionPanelShowsRuntime);
        com.google.android.material.button.MaterialButton nullRuntimeButton = findViewById(R.id.addNullRuntime);
        favShowsButton.setOnClickListener(view -> openFavouriteOrIgnoredShows(ShowType.FAVOURITE_SHOWS));
        ignoredShowsButton.setOnClickListener(view -> openFavouriteOrIgnoredShows(ShowType.IGNORED_SHOWS));
        addShowsButton.setOnClickListener(view -> openSearchActivity());
        ShowsRuntimeButton.setOnClickListener(view -> openRunTimeActivity());
        nullRuntimeButton.setOnClickListener(view -> nullRuntimeFixer());
    }

    private final NavigationBarView.OnItemSelectedListener navigationItemSelectedListener =
            new NavigationBarView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    final int itemId = item.getItemId();
                    if (itemId == R.id.barFavouriteShows) {
                        openFavouriteOrIgnoredShows(ShowType.FAVOURITE_SHOWS);
                        return true;
                    } else if (itemId == R.id.barIgnoredShows) {
                        openFavouriteOrIgnoredShows(ShowType.IGNORED_SHOWS);
                        return true;
                    } else if (itemId == R.id.barAddShows) {
                        openSearchActivity();
                        return true;
                    } else if (itemId == R.id.barShowRuntime) {
                        openRunTimeActivity();
                        return true;
                    }
                    return false;
                }
            };

    private void openSearchActivity() {
        Intent searchIntent = new Intent(this.getApplicationContext(), ShowManagementAddActivity.class);
        searchIntent.putExtra(ActivityConstants.EXTRA_TITLE, getString(R.string.addShow));
        startActivity(searchIntent);
    }

    private void openRunTimeActivity() {
        Intent runTimeIntent = new Intent(this.getApplicationContext(), ShowManagementRunTimeActivity.class);
        runTimeIntent.putExtra(ActivityConstants.EXTRA_TITLE, getString(R.string.ShowRuntime));
        startActivity(runTimeIntent);
    }


    private void nullRuntimeFixer() {
        TaskRunner.getExecutor().execute(() -> {
            AppDatabase database = AppDatabase.getInstance(HomeActivity.getContext().getApplicationContext());
            SeriesDAO seriesDAO = database.getSeriesDAO();
            List<EpisodeRuntime> runtimeList = seriesDAO.getEpisodeRuntime();

            for (int i = 0; i < runtimeList.size(); i++) {
                EpisodeRuntime showRuntime = runtimeList.get(i);

                if (showRuntime.getShowRuntime() == null || showRuntime.getShowRuntime().equals("null")) {
                    HashMap<String, String> showSummaryHashMap = new HashMap<>();
                    String tvmazeId = showRuntime.getShowTVMazeID();
                    runOnUiThread(() -> downloadShowSummary(showSummaryHashMap, tvmazeId));
                }
            }
        });
    }

    private void openFavouriteOrIgnoredShows(ShowType showType) {
        Intent intent = new Intent(this.getApplicationContext(), ShowManagementActivity.class);
        intent.putExtra(ShowType.class.getSimpleName(), showType);
        Log.e("Fav or Ignore show type", showType.toString());
        if (showType.toString().equals("FAVOURITE_SHOWS"))
            intent.putExtra(ActivityConstants.EXTRA_TITLE, getString(R.string.favouriteShows));
        else if (showType.toString().equals("IGNORED_SHOWS"))
            intent.putExtra(ActivityConstants.EXTRA_TITLE, getString(R.string.ignoredShows));
        startActivity(intent);
    }

    public void onHomeClick(View v) {
        finish();
    }


    private void downloadShowSummary(HashMap<String, String> showSummaryHash, String... params) {
        TaskRunner.getExecutor().execute(() -> {
            AppDatabase database = AppDatabase.getInstance(nz.mentalinc.watcher.activities.HomeActivity.getContext().getApplicationContext());

            SeriesDAO seriesDAO = database.getSeriesDAO();
            EpisodeRuntime showInfo = seriesDAO.getEpisodeRuntimeWithTVMazeId(params[0]);
            String showRuntime = showInfo.getShowRuntime();

            HttpsURLConnection connection = null;
            BufferedReader reader = null;
            String episodeSummaryAPIURL = "https://api.tvmaze.com/shows/" + params[0];

            try {
                URL url = new URL(episodeSummaryAPIURL);
                connection = (HttpsURLConnection) url.openConnection();
                connection.connect();
                int code = connection.getResponseCode();
                Log.d(LOG_TAG, "API HTTP Status Code: " + code);

                if (code == 429) {
                    Thread.sleep(10000);
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.connect();
                }

                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuilder buffer = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                    buffer.append("\n");
                }

                String jsonString = buffer.toString();
                JSONObject jObj;

                try {
                    jObj = new JSONObject(jsonString);

                    showRuntime = jObj.getString("runtime");

                    if (showRuntime.equals("null") || showRuntime == null) {
                        showRuntime = jObj.getString("averageRuntime");
                    }

                    showSummaryHash.put("showRuntime", showRuntime);
                    EpisodeRuntime showSummaryInfo = seriesDAO.getEpisodeRuntimeWithMyEpsId(showInfo.getShowMyEpsID());

                    showSummaryInfo.setShowRuntime(showSummaryHash.get("showRuntime"));

                    seriesDAO.update(showSummaryInfo);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}