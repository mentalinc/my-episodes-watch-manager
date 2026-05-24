package nz.mentalinc.episodeWatcher.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.room.Room;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.constants.ActivityConstants;
import nz.mentalinc.episodeWatcher.controllers.EpisodesController;
import nz.mentalinc.episodeWatcher.database.AppDatabase;
import nz.mentalinc.episodeWatcher.database.SeriesDAO;
import nz.mentalinc.episodeWatcher.domain.Episode;
import nz.mentalinc.episodeWatcher.domain.EpisodeAscendingComparator;
import nz.mentalinc.episodeWatcher.domain.EpisodeDescendingComparator;
import nz.mentalinc.episodeWatcher.domain.Show;
import nz.mentalinc.episodeWatcher.enums.EpisodeType;
import nz.mentalinc.episodeWatcher.enums.ListMode;
import nz.mentalinc.episodeWatcher.service.EpisodeRuntime;
import nz.mentalinc.episodeWatcher.utils.DateUtil;
import nz.mentalinc.episodeWatcher.utils.TaskRunner;

public class RandomEpPickerActivity extends Activity {
    private Episode random;
    private BottomNavigationView bottomNavigationView;
    private static final String LOG_TAG = RandomEpPickerActivity.class.getSimpleName();
    private static EpisodeType episodesType;
    private String showMyEpisodeID;
    List<Show> shows = new ArrayList<>();
    private List<Episode> episodes = new ArrayList<>();
    Bundle data;

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
        setContentView(R.layout.randompicker);

        TextView showNameText = findViewById(R.id.episodeDetShowName);
        TextView episodeNameText = findViewById(R.id.episodeDetName);
        TextView seasonText = findViewById(R.id.episodeDetSeason);
        TextView episodeText = findViewById(R.id.episodeDetEpisode);
        TextView airdateText = findViewById(R.id.episodeDetAirdate);
        Button markAsSeenButton = findViewById(R.id.markAsSeenButton);


        bottomNavigationView = findViewById(R.id.bottom_navigationRandomEpisode);
        bottomNavigationView.setOnItemSelectedListener(navigationItemSelectedListener);
        bottomNavigationView.getMenu().getItem(1).setChecked(true);

        if (EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_TO_WATCH) > 0) {

            //return an array of the shows
            shows = EpisodesController.getInstance().getRandomWatchEpisodeShowList();

            showMyEpisodeID = shows.get(0).getMyEpisodeID();
            random = shows.get(0).getFirstEpisode();

            String seasonString = " " + random.getSeasonString();
            String episodeString = " " + random.getEpisodeString();


            showNameText.setText(random.getShowName());
            episodeNameText.setText(random.getName());
            seasonText.setText(seasonString);
            episodeText.setText(episodeString);
            //runtimeText.setText(random.get);

            //Air date in specifc format
            Date airdate = random.getAirDate();
            String formattedAirDate;
            if (airdate != null) {
                formattedAirDate = DateUtil.formatDateLong(airdate);
            } else {
                formattedAirDate = getText(R.string.episodeDetailsAirDateLabelDateNotFound).toString();
            }

            String airdateString = " " + formattedAirDate;
            airdateText.setText(airdateString);
            TextView aboutWebsite = findViewById(R.id.tvMazeWebsite);
            if (!TextUtils.isEmpty(random.getTVMazeWebSite())) {
                //aboutWebsite.setText(episode.getTVMazeWebSite());
                AppDatabase database = Room.databaseBuilder(nz.mentalinc.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                        .allowMainThreadQueries()   //Allows room to do operation on main thread
                        .fallbackToDestructiveMigration()
                        .build();

                SeriesDAO seriesDAO = database.getSeriesDAO();
                EpisodeRuntime showRuntime = seriesDAO.getEpisodeRuntimeWithMyEpsId(random.getMyEpisodeID());


                //create hashmap's to prevent build fails, they get replaced
                HashMap<String, String> episodeSummaryHashMap = new HashMap<>() {{
                    put("a", "b");
                }};
                HashMap<String, String> showSummaryHashMap = new HashMap<>() {{
                    put("a", "b");
                }};

                downloadShowSummary(showSummaryHashMap, showRuntime.getShowTVMazeID());
                downloadEpisodeSummary(episodeSummaryHashMap, showRuntime.getShowTVMazeID(), random.getSeasonString(), random.getEpisodeString());

                database.close();
                //       episodeSummaryHashMap.get("episodeURL");

            } else {
                aboutWebsite.setVisibility(View.GONE);
            }

            markAsSeenButton.setOnClickListener(v -> closeAndMarkWatched(random));

        } else {
            seasonText.setText("-");
            episodeText.setText("-");
            airdateText.setText("-");

            markAsSeenButton.setVisibility(View.GONE);
        }

        markAsSeenButton.setOnClickListener(v -> closeAndMarkWatched(random));

        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {
            Log.w(LOG_TAG, "Home button clicked.");
            exit();
        });
    }


    private void closeAndMarkWatched(Episode episode) {
        finish();

        //Intent episodeListingActivity = new Intent(this.getApplicationContext(), EpisodeListingActivity.class);
        Intent episodeListingActivity = new Intent(this.getApplicationContext(), UpdatedEpisodeListingActivity.class);
        episodeListingActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        episodeListingActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode)
                .putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_MARK_EPISODE, ActivityConstants.EXTRA_BUNDLE_VALUE_WATCH)
                .putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episode.getType())
                .putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID,episode.getMyEpisodeID())
                .putExtra("Title", episode.getShowName())
                .putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_SHOW);

        startActivity(episodeListingActivity);

    }


    private final NavigationBarView.OnItemSelectedListener navigationItemSelectedListener = new NavigationBarView.OnItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            //episodesType = (EpisodeType) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE);
            //showMyEpisodeID = (String) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID);
            //String Title = data.getString("Title");

            Bundle BundleInfoShowDetail = new Bundle();
            BundleInfoShowDetail.putSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodesType);
            BundleInfoShowDetail.putString(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, showMyEpisodeID);
            BundleInfoShowDetail.putString("Title", "Random Episode");

            final int previousItem = bottomNavigationView.getSelectedItemId();
            final int nextItem = item.getItemId();

            //TODO need to do something to open a blank list instead of failing back when clicking on a button that has no shows to watch.
            if (previousItem != nextItem) {
               // switch (nextItem) {
                    if(R.id.barShowDetail == nextItem) {
                        Log.w(LOG_TAG, "barShowDetail selected");
                        if (shows.size() > 0) {
                            //episodeType is basically watch when clicking on a random episode a s onlt shows EPISODES_TO_WATCH
                            episodesType = EpisodeType.EPISODES_TO_WATCH;
                            openShowSummary(shows.get(0).getFirstEpisode(), episodesType);
                        } else if (episodes.size() > 0) {
                            Log.w(LOG_TAG, "barShowDetail Executing openShowSummary");
                            openShowSummary(episodes.get(0), episodesType);
                        } else {

                            openShowSummary(episodesType);
                        }
                        return true;
                    }else if( R.id.barEpisodeOverview == nextItem) {
                        Log.w(LOG_TAG, "barEpisodeOverview selected");
                        //   if (shows.size() > 0) {
                        //make sure it opens the next WATCH episode details.
                        episodesType = EpisodeType.EPISODES_TO_WATCH;
                        returnEpisodes();
                        //returnEpisodesShowHash(EpisodesController.getInstance().getEpisodesShows(EpisodeType.WATCH_BY_SHOW));
                        if (shows.size() > 0) {
                            openEpisodeDetails(shows.get(0).getFirstEpisode(), episodesType);
                            //   }
                        } else {
                            Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarRandomPicker), "No episodes to watch", Snackbar.LENGTH_LONG);
                            snackbar.setAnchorView(bottomNavigationView);
                            snackbar.show();
                            com.google.android.material.appbar.MaterialToolbar ShowNameTitle = findViewById(R.id.topAppBarRandomPicker);
                            String title = data.getString("Title");
                            title = title + " - No episodes to watch";
                            ShowNameTitle.setTitle(title);
                            //  openShowSummary(episodesType);

                        }
                        return true;
                    }else if( R.id.barWatch== nextItem) {
                        Log.w(LOG_TAG, "barWatch selected");

                        if (shows.size() > 0) {
                            openEpisodeListing(shows.get(0), EpisodeType.EPISODES_TO_WATCH);
                        } else {
                            episodesType = EpisodeType.EPISODES_TO_WATCH;
                            //changed to the new hash feature
                            returnEpisodes();
                            //returnEpisodesShowHash(EpisodesController.getInstance().getEpisodesShows(EpisodeType.WATCH_BY_SHOW));
                            if (shows.size() > 0) {
                                openEpisodeListing(shows.get(0), EpisodeType.EPISODES_TO_WATCH);

                            } else {
                                //   bottomNavigationView.getMenu().getItem(2).setChecked(true);
                            }
                        }
                        return true;
                    }else if( R.id.barAcquire== nextItem) {
                        Log.w(LOG_TAG, "barAcquire selected");

                        if (shows.size() > 0) {
                            openEpisodeListing(shows.get(0), EpisodeType.EPISODES_TO_ACQUIRE);
                        } else {
                            episodesType = EpisodeType.EPISODES_TO_ACQUIRE;
                            //changed to the new hash feature
                            returnEpisodes();
                            //returnEpisodesShowHash(EpisodesController.getInstance().getEpisodesShows(EpisodeType.ACQUIRE_BY_SHOW));

                            if (shows.size() > 0) {
                                openEpisodeListing(shows.get(0), EpisodeType.EPISODES_TO_ACQUIRE);
                            }
                        }
                        return true;
                    }else if( R.id.barComing== nextItem) {
                        Log.w(LOG_TAG, "barComing selected");

                        if (shows.size() > 0) {
                            openEpisodeListing(shows.get(0), EpisodeType.EPISODES_COMING);
                        } else {
                            episodesType = EpisodeType.EPISODES_COMING;
                            //changed to the new hash feature
                            returnEpisodes();
                            //returnEpisodesShowHash(EpisodesController.getInstance().getEpisodesShows(EpisodeType.COMING_BY_SHOW));
                            if (shows.size() > 0) {
                                openEpisodeListing(shows.get(0), EpisodeType.EPISODES_COMING);
                            }
                        }
                        return true;

                }else {
                        return false;
                    }
            }
            return false;
        }
    };

    private void returnEpisodes() {
        List<Episode> episodesRaw;
        //ideally this just grabs the data from the show somehow, loop is slow with lots of data
        episodesRaw = EpisodesController.getInstance().getEpisodes(episodesType);
        //setting to a new list when this is called risks there being now show left to work with if there are no episodes of a particualr type left.
        //shows = new ArrayList<>();
        episodes = new ArrayList<>();

        String ShowTitle = "";
        if (episodesRaw != null && episodesRaw.size() > 0) {
            for (Episode ep : episodesRaw) {
                if (ep.getMyEpisodeID().equals(showMyEpisodeID)) {
                    //add to the episode List to show next
                    episodes.add(ep);
                    ShowTitle = ep.getShowName();
                    AddEpisodeToShow(ep);
                }
            }
        } else {
            Log.d(LOG_TAG, "Episode can't be added to show.");
        }
        Log.w(LOG_TAG, "returnEpisodes() completed");

        sortEpisodesOfShows(shows);
    }

    private void openEpisodeListing(Show show, EpisodeType episodeType) {
        finish();
        Intent updatedEpisodeListActivity = new Intent(this.getApplicationContext(), UpdatedEpisodeListingActivity.class);
        updatedEpisodeListActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        Episode nextEpisodeToWatch = show.getFirstEpisode();
        String myEpisodeID = nextEpisodeToWatch.getMyEpisodeID();

        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, myEpisodeID);
        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        updatedEpisodeListActivity.putExtra("Title", show.getShowName());
        startActivity(updatedEpisodeListActivity);
    }

    private void sortEpisodesOfShows(List<Show> showList) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        // String sorting = Preferences.getPreference(this, PreferencesKeys.EPISODE_SORTING_KEY);
        String sorting = sharedPref.getString("episodeOrder", "oldest_on_top");

        String[] episodeOrderOptions = getResources().getStringArray(R.array.episodeOrderOptionsValues);

        for (Show show : showList) {
            if (sorting.equals(episodeOrderOptions[0])) {
                show.getEpisodes().sort(new EpisodeAscendingComparator());
            } else if (sorting.equals(episodeOrderOptions[1])) {
                show.getEpisodes().sort(new EpisodeDescendingComparator());
            }
        }
    }

    private void AddEpisodeToShow(Episode episode) {

        Show currentShow = CheckShowDuplicate(episode.getShowName());

        if (currentShow == null) {
            Show tempShow = new Show(episode.getShowName());
            tempShow.addEpisode(episode);
            shows.add(tempShow);
        } else {
            currentShow.addEpisode(episode);
        }
    }


    private Show CheckShowDuplicate(String episodename) {
        for (Show show : shows) {
            if (show.getShowName().equals(episodename)) {
                return show;
            }
        }
        return null;
    }

    private void openShowSummary(Episode episode, EpisodeType episodeType) {
        finish();
        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), ShowSummaryActivity.class);
        episodeDetailsSubActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, episode.getMyEpisodeID());
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra("Title", episode.getShowName());
        startActivity(episodeDetailsSubActivity);
    }


    private void openShowSummary(EpisodeType episodeType) {
        finish();
        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), ShowSummaryActivity.class);
        episodeDetailsSubActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, showMyEpisodeID);
        // episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra("Title", data.getString("Title"));
        startActivity(episodeDetailsSubActivity);
    }

    private void openEpisodeDetails(Episode episode, EpisodeType episodeType) {
        finish();
        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), EpisodeDetailsActivity.class);
        episodeDetailsSubActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, episode.getMyEpisodeID());
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra("Title", episode.getShowName());
        startActivity(episodeDetailsSubActivity);
    }

    private void downloadEpisodeSummary(HashMap<String, String> episodeSummaryHash, String... params) {
        TaskRunner.getExecutor().execute(() -> {
            HttpsURLConnection connection = null;
            BufferedReader reader = null;
            String episodeSummaryAPIURL = "https://api.tvmaze.com/shows/" + params[0] + "/episodebynumber?season=" + params[1] + "&number=" + params[2];

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

                String jsonString;
                if (code == 404) {
                    jsonString = "{\"id\":0,\"url\":\"Unknown Episode\",\"name\":\"Unknown Episode\",\"image\":null,\"summary\":\"Unknown Episode\"}";

                    Toast.makeText(RandomEpPickerActivity.this, RandomEpPickerActivity.this.getString(R.string.episodeNotFoundViaAPI), Toast.LENGTH_LONG).show();
                } else {
                    InputStream stream = connection.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(stream));

                    StringBuilder buffer = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                        buffer.append("\n");
                    }
                    jsonString = buffer.toString();
                }

                JSONObject jObj;
                String episodeSummary;
                String episodeURL;
                String episodeImageURL = "";

                try {
                    jObj = new JSONObject(jsonString);

                    episodeSummary = jObj.getString("summary");
                    episodeURL = jObj.getString("url");
                    if (!jObj.getString("image").equals("null")) {
                        episodeImageURL = jObj.getJSONObject("image").getString("medium");
                    }

                    episodeURL = episodeURL.replace("http://", "https://");
                    episodeSummary = episodeSummary.replace("<p>", "");
                    episodeSummary = episodeSummary.replace("</p>", "");

                    episodeSummaryHash.put("episodeURL", episodeURL);
                    episodeSummaryHash.put("episodeSummary", episodeSummary);
                    episodeSummaryHash.put("episodeImageURL", episodeImageURL);

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

            HashMap<String, String> result = episodeSummaryHash;
            runOnUiThread(() -> {
                TextView aboutWebsite = findViewById(R.id.tvMazeWebsite);
                aboutWebsite.setText(result.get("episodeURL"));
                Linkify.addLinks(aboutWebsite, Linkify.WEB_URLS);

                TextView tvMazeEpisodeSummary = findViewById(R.id.tvMazeEpisodeSummary);

                String episodeSummaryStr = result.get("episodeSummary");

                if (!(episodeSummaryStr == null)) {
                    if (!episodeSummaryStr.equals("null")) {
                        tvMazeEpisodeSummary.setText(episodeSummaryStr);
                    } else {
                        tvMazeEpisodeSummary.setVisibility(View.GONE);
                    }
                }
            });
        });
    }


    private void downloadShowSummary(HashMap<String, String> showSummaryHash, String... params) {
        TaskRunner.getExecutor().execute(() -> {
            AppDatabase database = Room.databaseBuilder(nz.mentalinc.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();

            SeriesDAO seriesDAO = database.getSeriesDAO();
            EpisodeRuntime showInfo = seriesDAO.getEpisodeRuntimeWithTVMazeId(params[0]);

            String ShowName = showInfo.getShowName();
            String showURL = showInfo.getShowURL();
            String officialSite = showInfo.getOfficialSite();
            String showSummary = showInfo.getShowSummary();
            String showImageURL = showInfo.getShowImageURL();
            String ShowRuntime = showInfo.getShowRuntime();

            if (!ShowName.equals("") && !showURL.equals("") && !officialSite.equals("") && !showSummary.equals("") && !showImageURL.equals("")) {

                showSummaryHash.put("ShowName", ShowName);
                showSummaryHash.put("showURL", showURL);
                showSummaryHash.put("officialSite", officialSite);
                showSummaryHash.put("showSummary", showSummary);
                showSummaryHash.put("showImageURL", showImageURL);
                showSummaryHash.put("ShowRuntime", ShowRuntime);

                database.close();
            } else {
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

                        showSummary = jObj.getString("summary");
                        ShowName = jObj.getString("name");
                        showURL = jObj.getString("url");
                        ShowRuntime = jObj.getString("runtime");
                        officialSite = jObj.getString("officialSite");
                        if (!jObj.getString("image").equals("null")) {
                            showImageURL = jObj.getJSONObject("image").getString("medium");
                        }

                        showImageURL = showImageURL.replace("http://", "https://");

                        showSummary = showSummary.replace("<p>", "");
                        showSummary = showSummary.replace("</p>", "");
                        showSummary = showSummary.replace("<b>", "");
                        showSummary = showSummary.replace("</b>", "");
                        showSummary = showSummary.replace("<i>", "");
                        showSummary = showSummary.replace("</i>", "");

                        showSummaryHash.put("showURL", showURL);
                        showSummaryHash.put("showSummary", showSummary);
                        showSummaryHash.put("showImageURL", showImageURL);
                        showSummaryHash.put("officialSite", officialSite);
                        showSummaryHash.put("ShowName", ShowName);
                        showSummaryHash.put("ShowRuntime", ShowRuntime);

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
            }

            HashMap<String, String> result = showSummaryHash;
            runOnUiThread(() -> {
                TextView ShowNameTV = findViewById(R.id.ShowName);
                ShowNameTV.setText(result.get("ShowName"));

                TextView ShowRuntimeTV = findViewById(R.id.episodeRuntime);
                String showruntimeText = " " + result.get("ShowRuntime") + " mins";
                ShowRuntimeTV.setText(showruntimeText);

                TextView aboutShowWebsite = findViewById(R.id.tvMazeShowWebsite);
                aboutShowWebsite.setText(result.get("showURL"));
                Linkify.addLinks(aboutShowWebsite, Linkify.WEB_URLS);

                TextView aboutShowOfficialWebsite = findViewById(R.id.officialShowWebsite);
                String showOfficialWebsite = result.get("officialSite");
                if (!Objects.requireNonNull(showOfficialWebsite).equals("null")) {
                    aboutShowOfficialWebsite.setText(showOfficialWebsite);
                    Linkify.addLinks(aboutShowOfficialWebsite, Linkify.WEB_URLS);
                } else {
                    aboutShowOfficialWebsite.setVisibility(View.GONE);
                }

                TextView tvMazeShowSummary = findViewById(R.id.tvMazeShowSummary);
                String episodeSummaryStr = result.get("showSummary");

                if (!Objects.requireNonNull(episodeSummaryStr).equals("null")) {
                    tvMazeShowSummary.setText(episodeSummaryStr);
                } else {
                    tvMazeShowSummary.setVisibility(View.GONE);
                }

                ImageView showImage = findViewById(R.id.showImage);
                String showImageURLStr = result.get("showImageURL");

                if (!Objects.requireNonNull(showImageURLStr).equals("")) {
                    RequestOptions requestOptions = new RequestOptions();
                    requestOptions.placeholder(R.drawable.placeholder);
                    requestOptions.error(R.drawable.error);

                    Glide.with(findViewById(R.id.showImage))
                            .load(showImageURLStr)
                            .placeholder(R.drawable.placeholder)
                            .into(showImage);
                } else {
                    showImage.setVisibility(View.GONE);
                }

                AppDatabase db = Room.databaseBuilder(nz.mentalinc.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                        .allowMainThreadQueries()
                        .fallbackToDestructiveMigration()
                        .build();

                SeriesDAO sDAO = db.getSeriesDAO();

                EpisodeRuntime showSummaryInfo = sDAO.getEpisodeRuntimeWithMyEpsId(random.getMyEpisodeID());

                showSummaryInfo.setShowSummary(result.get("showSummary"));
                showSummaryInfo.setShowURL(result.get("showURL"));
                showSummaryInfo.setOfficialSite(result.get("officialSite"));
                showSummaryInfo.setShowImageURL(result.get("showImageURL"));

                sDAO.update(showSummaryInfo);
                db.close();
            });
        });
    }


    public void onHomeClick(View v) {
        exit();
    }

    private void exit() {
        finish();
    }
}
