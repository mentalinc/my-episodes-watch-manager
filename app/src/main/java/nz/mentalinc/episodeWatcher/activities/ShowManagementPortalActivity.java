package nz.mentalinc.episodeWatcher.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.room.Room;

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

import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.controllers.EpisodesController;
import nz.mentalinc.episodeWatcher.database.AppDatabase;
import nz.mentalinc.episodeWatcher.database.SeriesDAO;
import nz.mentalinc.episodeWatcher.enums.ShowType;
import nz.mentalinc.episodeWatcher.service.EpisodeRuntime;

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
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_management_portal);
        findViewById(R.id.appBarLayoutPortal).setZ(100f);

        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> finish());

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
        searchIntent.putExtra("Title", getString(R.string.addShow));
        startActivity(searchIntent);
    }

    private void openRunTimeActivity() {
        Intent runTimeIntent = new Intent(this.getApplicationContext(), ShowManagementRunTimeActivity.class);
        runTimeIntent.putExtra("Title", getString(R.string.ShowRuntime));
        startActivity(runTimeIntent);
    }


    private void nullRuntimeFixer() {

        //open the database and find shows that are null runtime and get get it...
        AppDatabase database = Room.databaseBuilder(nz.mentalinc.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                .allowMainThreadQueries()   //Allows room to do operation on main thread
                .fallbackToDestructiveMigration()
                .build();

        SeriesDAO seriesDAO = database.getSeriesDAO();
        List<EpisodeRuntime> runtimeList = seriesDAO.getEpisodeRuntime();


        for (int i = 0; i < runtimeList.size(); i++) {
            EpisodeRuntime showRuntime = runtimeList.get(i);

            if (showRuntime.getShowRuntime() == null || showRuntime.getShowRuntime().equals("null")) {

                //get the runtime for the null from TVMaze
                HashMap<String, String> showSummaryHashMap = new HashMap<>() {{
                    put("a", "b");
                }};

                new ShowManagementPortalActivity.downloadShowSummary(showSummaryHashMap).execute(showRuntime.getShowTVMazeID());
            }
        }
        database.close();
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


    //https://stackoverflow.com/questions/29555909/asynctask-how-to-return-a-hashmap-from-doinbackground
    private class downloadShowSummary extends AsyncTask<String, String, HashMap<String, String>> {
        HashMap<String, String> showSummaryHash;

        downloadShowSummary(HashMap<String, String> showSummaryHash) {
            this.showSummaryHash = showSummaryHash;
        }

        /*
            doInBackground(Params... params)
                Override this method to perform a computation on a background thread.
         */
        protected HashMap<String, String> doInBackground(String... params) {


            //check if there are values in the database first. if there are use those, if not use the API

            AppDatabase database = Room.databaseBuilder(nz.mentalinc.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                    //.allowMainThreadQueries()   //Allows room to do operation on main thread
                    .fallbackToDestructiveMigration()
                    .build();

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
                    //wait 10 seconds then try again
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
                    //Inserting episodeRuntime adding the info that was not collected during the runtime. addition

                    showSummaryInfo.setShowRuntime(showSummaryHash.get("showRuntime"));

                    //  Log.d("epsRunTime: ", epsRunTime.toString());
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

            database.close();
            return showSummaryHash;
        }
    }
}