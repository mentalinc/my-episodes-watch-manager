package nz.mentalinc.episodeWatcher.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
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
import nz.mentalinc.episodeWatcher.domain.ShowAscendingComparator;
import nz.mentalinc.episodeWatcher.domain.ShowDescendingComparator;
import nz.mentalinc.episodeWatcher.domain.ShowRuntimeAscendingComparator;
import nz.mentalinc.episodeWatcher.enums.EpisodeType;
import nz.mentalinc.episodeWatcher.service.EpisodeRuntime;
import nz.mentalinc.episodeWatcher.service.ShowService;
import nz.mentalinc.episodeWatcher.utils.TaskRunner;

public class ShowSummaryActivity extends Activity {

    List<Show> shows = new ArrayList<>();

    private List<Episode> episodesRaw = new ArrayList<>();
    private List<Episode> episodes = new ArrayList<>();
    private static EpisodeType episodesType;
    private String showMyEpisodeID;
    private Episode episode = null;

    private static final String LOG_TAG = EpisodeDetailsActivity.class.getSimpleName();
    private BottomNavigationView bottomNavigationView;
    Bundle data;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.show_overview);

        findViewById(R.id.appBarShowOverviewLayout2).setZ(100f);

        data = this.getIntent().getExtras();
        episodesType = (EpisodeType) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE);
        showMyEpisodeID = (String) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID);
        episode = (Episode) Objects.requireNonNull(data).getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE);
        String title = data.getString("Title");

        //showDetail = new Show(title, showMyEpisodeID);

        com.google.android.material.appbar.MaterialToolbar ShowNameTitle = findViewById(R.id.topAppBarShowOverview);
        ShowNameTitle.setTitle(title);

        bottomNavigationView = findViewById(R.id.bottom_navigationShowOverview);
        bottomNavigationView.setOnItemSelectedListener(navigationItemSelectedListener);


        //TextView seriesnameView = (TextView) findViewById(R.id.ShowName);
        //seriesnameView.setText("testing the view works");

        returnEpisodes();

        //todo Need to work out the downloadSHowSummary below (from the episodedetails activity, to make it show the info needed.

        AppDatabase database = AppDatabase.getInstance(nz.mentalinc.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext());

        SeriesDAO seriesDAO = database.getSeriesDAO();
        EpisodeRuntime showRuntime = seriesDAO.getEpisodeRuntimeWithMyEpsId(showMyEpisodeID);

        TextView textViewWatchShowsRemaining = findViewById(R.id.episodesToWatch);
        TextView textViewAcquireShowsRemaining = findViewById(R.id.episodesToAcquire);
        TextView textViewComingShowsRemaining = findViewById(R.id.episodesComing);
        //String watchCount = "Episodes to watch: " + EpisodesController.getInstance().getEpisodesCount(EpisodeType.WATCH_BY_SHOW, showMyEpisodeID);
        String watchCount = "" +EpisodesController.getInstance().getEpisodesCount(EpisodeType.WATCH_BY_SHOW, showMyEpisodeID);
        textViewWatchShowsRemaining.setText(watchCount);
        //String acquireCount = "Episodes to acquire: " + EpisodesController.getInstance().getEpisodesCount(EpisodeType.ACQUIRE_BY_SHOW, showMyEpisodeID);
        String acquireCount = "" + EpisodesController.getInstance().getEpisodesCount(EpisodeType.ACQUIRE_BY_SHOW, showMyEpisodeID);
        textViewAcquireShowsRemaining.setText(acquireCount);
        //String comingCount = "Episodes coming: " + EpisodesController.getInstance().getEpisodesCount(EpisodeType.COMING_BY_SHOW, showMyEpisodeID);
        String comingCount = "" + EpisodesController.getInstance().getEpisodesCount(EpisodeType.COMING_BY_SHOW, showMyEpisodeID);
        textViewComingShowsRemaining.setText(comingCount);


        //create hashmap's to prevent build fails, they get replaced
        HashMap<String, String> episodeSummaryHashMap = new HashMap<>() {{
            put("a", "b");
        }};
        HashMap<String, String> showSummaryHashMap = new HashMap<>() {{
            put("a", "b");
        }};


        if (showRuntime != null && showRuntime.getShowTVMazeID() != null) {
            downloadShowSummary(showSummaryHashMap, showRuntime.getShowTVMazeID());
        } else if (showRuntime != null) {
            Log.w(LOG_TAG, "showRuntime getShowTVMazeID is null");
        } else {
            populateShowRuntimeAndSummary(showSummaryHashMap);
        }


        //       episodeSummaryHashMap.get("episodeURL");
        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {
            Log.w(LOG_TAG, "Home button clicked.");
            Intent home = new Intent(ShowSummaryActivity.this, HomeActivity.class);
            home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(home);
        });

        returnEpisodes();
        Log.w(LOG_TAG, "showSummaryActivity created");
    }


    private final NavigationBarView.OnItemSelectedListener navigationItemSelectedListener = new NavigationBarView.OnItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            episodesType = (EpisodeType) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE);
            showMyEpisodeID = (String) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID);
            String Title = data.getString("Title");

            Bundle BundleInfoShowDetail = new Bundle();
            BundleInfoShowDetail.putSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodesType);
            BundleInfoShowDetail.putString(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, showMyEpisodeID);
            BundleInfoShowDetail.putString("Title", Title);

            final int previousItem = bottomNavigationView.getSelectedItemId();
            final int nextItem = item.getItemId();
            if (previousItem != nextItem) {
                //switch (nextItem) {
                    if( R.id.barShowDetail == nextItem) {
                        Log.w(LOG_TAG, "barShowDetail selected");
                        //TODO need to build an activity to use the showDetail content.
                        if (episodes.size() > 0) {
                            Log.w(LOG_TAG, "barShowDetail Executing openShowSummary");
                            openShowSummary(episodes.get(0), episodesType);
                        }
                        return true;
                    }else if( R.id.barEpisodeOverview == nextItem) {
                        Log.w(LOG_TAG, "barEpisodeOverview selected");
                        //if (shows.size() > 0) {
                        //make sure it opens the next WATCH episode details.
                        episodesType = EpisodeType.EPISODES_TO_WATCH;
                        returnEpisodes();
                        if (shows.size() > 0) {
                            openEpisodeDetails(shows.get(0).getFirstEpisode(), episodesType);


                        /*    } else {

                                Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarShowOverview), "No episodes to watch", Snackbar.LENGTH_LONG);
                                snackbar.setAnchorView(bottomNavigationView);
                                snackbar.show();
                                //show the snackbar and stay on the episode summary
                                bottomNavigationView.getMenu().getItem(0).setChecked(true);
                            }*/
                        } else {

                            Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarShowOverview), "No episodes to watch", Snackbar.LENGTH_LONG);
                            snackbar.setAnchorView(bottomNavigationView);
                            snackbar.show();
                            //show the snackbar and stay on the episode summary
                            bottomNavigationView.getMenu().getItem(0).setChecked(true);
                        }
                        return true;
                    }else if( R.id.barWatch == nextItem) {
                        Log.w(LOG_TAG, "barWatch selected");

                        if (shows.size() > 0) {
                            openEpisodeListing(shows.get(0), EpisodeType.EPISODES_TO_WATCH);
                        } else {
                            openEpisodeListing(EpisodeType.EPISODES_TO_WATCH);
                        }
                        return true;
                    }else if( R.id.barAcquire == nextItem) {
                        Log.w(LOG_TAG, "barAcquire selected");
                        if (shows.size() > 0) {
                            openEpisodeListing(shows.get(0), EpisodeType.EPISODES_TO_ACQUIRE);
                        } else {
                            openEpisodeListing(EpisodeType.EPISODES_TO_ACQUIRE);
                        }
                        return true;
                    }else if( R.id.barComing == nextItem) {
                        Log.w(LOG_TAG, "barComing selected");
                        if (shows.size() > 0) {
                            openEpisodeListing(shows.get(0), EpisodeType.EPISODES_COMING);
                        } else {
                            openEpisodeListing(EpisodeType.EPISODES_COMING);
                        }
                        return true;
                }
                return false;
            }
            return false;
        }
    };


    private void openEpisodeListing(Show show, EpisodeType episodeType) {
        finish();
        Intent updatedEpisodeListActivity = new Intent(this.getApplicationContext(), UpdatedEpisodeListingActivity.class);
        //todo make the animation slide between the different tabs like this:
        //https://stackoverflow.com/questions/10243557/how-to-apply-slide-animation-between-two-activities-in-android
        updatedEpisodeListActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        Episode nextEpisodeToWatch = show.getFirstEpisode();
        String myepisodeID = nextEpisodeToWatch.getMyEpisodeID();

        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, myepisodeID);
        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        updatedEpisodeListActivity.putExtra("Title", show.getShowName());
        startActivity(updatedEpisodeListActivity);
    }


    private void openEpisodeListing(EpisodeType episodeType) {
        finish();
        Intent updatedEpisodeListActivity = new Intent(this.getApplicationContext(), UpdatedEpisodeListingActivity.class);
        //todo make the animation slide between the different tabs like this:
        //https://stackoverflow.com/questions/10243557/how-to-apply-slide-animation-between-two-activities-in-android
        updatedEpisodeListActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, showMyEpisodeID);
        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        updatedEpisodeListActivity.putExtra("Title", data.getString("Title"));
        startActivity(updatedEpisodeListActivity);
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

    private void returnEpisodes() {

        //ideally this just grabs the data from the show somehow, loop is slow with lots of data
        episodesRaw = EpisodesController.getInstance().getEpisodes(episodesType);
        // shows = new ArrayList<>();
        episodes = new ArrayList<>();

        if (episodesRaw != null && episodesRaw.size() > 0) {
            for (Episode ep : episodesRaw) {
                if (ep.getMyEpisodeID().equals(showMyEpisodeID)) {
                    //add to the episode List to show next
                    episodes.add(ep);
                    AddEpisodeToShow(ep);
                }
            }
        } else {
            Log.d(LOG_TAG, "Episode can't be added to show.");
        }
        Log.w(LOG_TAG, "returnEpisodes() completed");

        sortEpisodesOfShows(shows);
    }


    private Show CheckShowDuplicate(String episodename) {
        for (Show show : shows) {
            if (show.getShowName().equals(episodename)) {
                return show;
            }
        }
        return null;
    }


    private void downloadShowSummary(HashMap<String, String> showSummaryHash, String... params) {
        TaskRunner.getExecutor().execute(() -> {
            AppDatabase database = AppDatabase.getInstance(nz.mentalinc.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext());

            SeriesDAO seriesDAO = database.getSeriesDAO();
            EpisodeRuntime showInfo = seriesDAO.getEpisodeRuntimeWithTVMazeId(params[0]);

            String showName = showInfo.getShowName();
            String showURL = showInfo.getShowURL();
            String officialSite = showInfo.getOfficialSite();
            String showSummary = showInfo.getShowSummary();
            String showImageURL = showInfo.getShowImageURL();
            String showRuntime = showInfo.getShowRuntime();
            String showStatus = showInfo.getShowStatus();

            if (showName != null && showURL != null && officialSite != null && showSummary != null && showStatus != null && showImageURL != null && !showRuntime.equals("null")) {
                showSummaryHash.put("showName", showName);
                showSummaryHash.put("showURL", showURL);
                showSummaryHash.put("officialSite", officialSite);
                showSummaryHash.put("showSummary", showSummary);
                showSummaryHash.put("showImageURL", showImageURL);
                showSummaryHash.put("showRuntime", showRuntime);
                showSummaryHash.put("showStatus", showStatus);
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
                        showName = jObj.getString("name");
                        showURL = jObj.getString("url");
                        showRuntime = jObj.getString("runtime");

                        if (showRuntime.equals("null") || showRuntime == null) {
                            showRuntime = jObj.getString("averageRuntime");
                        }

                        officialSite = jObj.getString("officialSite");
                        showStatus = jObj.getString("status");
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
                        showSummaryHash.put("showName", showName);
                        showSummaryHash.put("showRuntime", showRuntime);
                        showSummaryHash.put("showStatus", showStatus);

                        EpisodeRuntime showSummaryInfo = seriesDAO.getEpisodeRuntimeWithMyEpsId(showMyEpisodeID);

                        showSummaryInfo.setShowSummary(showSummaryHash.get("showSummary"));
                        showSummaryInfo.setShowURL(showSummaryHash.get("showURL"));
                        showSummaryInfo.setOfficialSite(showSummaryHash.get("officialSite"));
                        showSummaryInfo.setShowImageURL(showSummaryHash.get("showImageURL"));
                        showSummaryInfo.setShowRuntime(showSummaryHash.get("showRuntime"));
                        showSummaryInfo.setShowStatus(showSummaryHash.get("showStatus"));

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
            }

            HashMap<String, String> result = showSummaryHash;
            runOnUiThread(() -> {
                TextView ShowRuntimeTV = findViewById(R.id.ShowRuntime);
                String runtimeStr = "Episode runtime: " + result.get("showRuntime") + " mins";
                ShowRuntimeTV.setText(runtimeStr);

                TextView ShowStatusTV = findViewById(R.id.ShowStatus);
                String showStatusStr = "Show status: " + result.get("showStatus");
                ShowStatusTV.setText(showStatusStr);

                TextView tvMazeShowWebsite = findViewById(R.id.tvMazeShowWebsite);
                tvMazeShowWebsite.setText(result.get("showURL"));
                Linkify.addLinks(tvMazeShowWebsite, Linkify.WEB_URLS);

                TextView officialShowDetailWebsite = findViewById(R.id.officialShowWebsite);
                String showOfficialWebsite = result.get("officialSite");
                if (!showOfficialWebsite.equals("null")) {
                    officialShowDetailWebsite.setText(showOfficialWebsite);
                    Linkify.addLinks(officialShowDetailWebsite, Linkify.WEB_URLS);
                } else {
                    officialShowDetailWebsite.setVisibility(View.GONE);
                }

                TextView tvMazeShowDetailSummary = findViewById(R.id.tvMazeShowSummary);
                String episodeSummary = result.get("showSummary");

                if (!episodeSummary.equals("null")) {
                    tvMazeShowDetailSummary.setText(episodeSummary);
                } else {
                    tvMazeShowDetailSummary.setVisibility(View.GONE);
                }

                ImageView showImage = findViewById(R.id.showImage);
                String showImageURLStr = result.get("showImageURL");

                if (!showImageURLStr.equals("")) {
                    RequestOptions requestOptions = new RequestOptions();
                    requestOptions.placeholder(R.drawable.placeholder);
                    requestOptions.error(R.drawable.error);

                    Glide.with(findViewById(R.id.showImage))
                            .load(showImageURLStr)
                            .apply(requestOptions)
                            .into(showImage);
                } else {
                    showImage.setVisibility(View.GONE);
                }
            });
        });
    }

    private void populateShowRuntimeAndSummary(HashMap<String, String> showSummaryHashMap) {
        if (shows.isEmpty()) {
            Log.w(LOG_TAG, "No shows available to populate runtime");
            return;
        }
        String showName = shows.get(0).getShowName();
        if (showName == null) {
            Log.w(LOG_TAG, "Show name is null, cannot populate runtime");
            return;
        }
        TaskRunner.getExecutor().execute(() -> {
            AppDatabase database = AppDatabase.getInstance(HomeActivity.getContext().getApplicationContext());
            try {
                ShowService showService = new ShowService();
                showService.ShowsRuntime(showName, showMyEpisodeID, database);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to populate show runtime", e);
            }
            runOnUiThread(() -> {
                AppDatabase db = AppDatabase.getInstance(HomeActivity.getContext().getApplicationContext());
                SeriesDAO seriesDAO = db.getSeriesDAO();
                EpisodeRuntime updatedRuntime = seriesDAO.getEpisodeRuntimeWithMyEpsId(showMyEpisodeID);
                if (updatedRuntime != null && updatedRuntime.getShowTVMazeID() != null) {
                    downloadShowSummary(showSummaryHashMap, updatedRuntime.getShowTVMazeID());
                } else {
                    Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarShowOverview), R.string.showDetailsNotAvailable, Snackbar.LENGTH_LONG);
                    snackbar.setAnchorView(bottomNavigationView);
                    snackbar.show();
                }
            });
        });
    }

    private void sortEpisodesOfShows(List<Show> showList) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        // String sorting = Preferences.getPreference(this, PreferencesKeys.EPISODE_SORTING_KEY);
        String sorting = sharedPref.getString("episodeOrder", "oldest_on_top");

        //TODO add a sort by runtime might need to be on the below somehow EpisodeAscendingComparator()??

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

            AppDatabase database = AppDatabase.getInstance(nz.mentalinc.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext());

            SeriesDAO seriesDAO = database.getSeriesDAO();
            EpisodeRuntime Runtime = seriesDAO.getEpisodeRuntimeWithMyEpsId(episode.getMyEpisodeID());

            String RuntimeMins;
            if (Runtime == null) {
                RuntimeMins = "Error mins";
            } else {
                RuntimeMins = Runtime.getShowRuntime();
            }

            Show tempShow = new Show(episode.getShowName(), RuntimeMins, episode.getMyEpisodeID());
            tempShow.addEpisode(episode);
            shows.add(tempShow);

        } else {
            currentShow.addEpisode(episode);
        }

    }

    private void sortShows(List<Show> showList) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String sorting = "";
        switch (episodesType) {
            case EPISODES_TO_WATCH:
                sorting = sharedPref.getString("showWatchOrder", "show_myepisodes_default_sort");//Preferences.getPreference(this, PreferencesKeys.WATCH_SHOW_SORTING_KEY);
                break;
            case EPISODES_TO_YESTERDAY1:
            case EPISODES_TO_YESTERDAY2:
            case EPISODES_TO_ACQUIRE:
                sorting = sharedPref.getString("showAcquireOrder", "show_myepisodes_default_sort"); //Preferences.getPreference(this, PreferencesKeys.ACQUIRE_SHOW_SORTING_KEY);
                break;
            case EPISODES_COMING:
                sorting = sharedPref.getString("showComingOrder", "show_myepisodes_default_sort"); //Preferences.getPreference(this, PreferencesKeys.COMING_SHOW_SORTING_KEY);
                break;
        }

        String[] showOrderOptions = getResources().getStringArray(R.array.showOrderOptionsValues);
        if (sorting.equals(showOrderOptions[1])) {
            Log.d(LOG_TAG, "Sorting episodes ascending");
            showList.sort(new ShowAscendingComparator());
        } else if (sorting.equals(showOrderOptions[2])) {
            Log.d(LOG_TAG, "Sorting episodes descending");
            showList.sort(new ShowDescendingComparator());
        } else if (sorting.equals(showOrderOptions[0])) {
            Log.d(LOG_TAG, "Default my episodes show sorting, nothing to do!");
        } else if (sorting.equals(showOrderOptions[4])) {
            Log.d(LOG_TAG, "Sort by Runtime!");
            showList.sort(new ShowRuntimeAscendingComparator());
        }
    }


}