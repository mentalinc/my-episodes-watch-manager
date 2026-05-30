package nz.mentalinc.watcher.activities;

import android.app.Activity;
import android.app.AlertDialog;
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
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import nz.mentalinc.watcher.R;
import nz.mentalinc.watcher.constants.ActivityConstants;
import nz.mentalinc.watcher.constants.MyEpisodeConstants;
import nz.mentalinc.watcher.controllers.EpisodesController;
import nz.mentalinc.watcher.database.AppDatabase;
import nz.mentalinc.watcher.database.SeriesDAO;
import nz.mentalinc.watcher.domain.Episode;
import nz.mentalinc.watcher.domain.EpisodeAscendingComparator;
import nz.mentalinc.watcher.domain.EpisodeDescendingComparator;
import nz.mentalinc.watcher.domain.Show;
import nz.mentalinc.watcher.enums.EpisodeType;
import nz.mentalinc.watcher.enums.ListMode;
import nz.mentalinc.watcher.service.EpisodeRuntime;
import nz.mentalinc.watcher.utils.DateUtil;
import nz.mentalinc.watcher.utils.TaskRunner;

public class RandomEpPickerActivity extends Activity {
    private Episode random;
    private BottomNavigationView bottomNavigationView;
    private static final String LOG_TAG = RandomEpPickerActivity.class.getSimpleName();
    private static EpisodeType episodesType;
    private String showMyEpisodeID;
    List<Show> shows = new ArrayList<>();
    private List<Episode> episodes = new ArrayList<>();
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
            default: //added for code quality
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.randompicker);

        TextView seasonText = findViewById(R.id.episodeDetSeason);
        TextView episodeText = findViewById(R.id.episodeDetEpisode);
        TextView airdateText = findViewById(R.id.episodeDetAirdate);


        bottomNavigationView = findViewById(R.id.bottom_navigationRandomEpisode);
        bottomNavigationView.setOnItemSelectedListener(navigationItemSelectedListener);
        bottomNavigationView.getMenu().getItem(1).setChecked(true);

        if (EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_TO_WATCH) > 0) {
            showRuntimePickerDialog();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Random Episode")
                    .setMessage("Nothing new to watch")
                    .setPositiveButton("OK", (dialog, which) -> exit())
                    .setCancelable(false)
                    .show();
        }

        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {
            Log.w(LOG_TAG, "Home button clicked.");
            exit();
        });
    }

    private void displayShow(Show show) {
        shows.clear();
        shows.add(show);
        showMyEpisodeID = show.getMyEpisodeID();
        random = show.getFirstEpisode();

        TextView showNameText = findViewById(R.id.episodeDetShowName);
        TextView episodeNameText = findViewById(R.id.episodeDetName);
        TextView seasonText = findViewById(R.id.episodeDetSeason);
        TextView episodeText = findViewById(R.id.episodeDetEpisode);
        TextView airdateText = findViewById(R.id.episodeDetAirdate);
        Button markAsSeenButton = findViewById(R.id.markAsSeenButton);

        String seasonString = " " + random.getSeasonString();
        String episodeString = " " + random.getEpisodeString();

        showNameText.setText(random.getShowName());
        episodeNameText.setText(random.getName());
        seasonText.setText(seasonString);
        episodeText.setText(episodeString);

        Date airdate = new Date(random.getAirDate());
        String formattedAirDate;
        if (airdate != null) {
            formattedAirDate = DateUtil.formatDateLong(airdate);
        } else {
            formattedAirDate = getString(R.string.episodeDetailsAirDateLabelDateNotFound);
        }

        String airdateString = " " + formattedAirDate;
        airdateText.setText(airdateString);
        TextView aboutWebsite = findViewById(R.id.tvMazeWebsite);
        if (!TextUtils.isEmpty(random.getTVMazeWebSite())) {
            TaskRunner.getExecutor().execute(() -> {
                AppDatabase database = AppDatabase.getInstance(HomeActivity.getContext().getApplicationContext());
                EpisodeRuntime showRuntime = database.getSeriesDAO().getEpisodeRuntimeWithMyEpsId(random.getMyEpisodeID());
                String showTVMazeID = showRuntime.getShowTVMazeID();
                runOnUiThread(() -> {
                    HashMap<String, String> episodeSummaryHashMap = new HashMap<>();
                    HashMap<String, String> showSummaryHashMap = new HashMap<>();

                    downloadShowSummary(showSummaryHashMap, showTVMazeID);
                    downloadEpisodeSummary(episodeSummaryHashMap, showTVMazeID, random.getSeasonString(), random.getEpisodeString());
                });
            });
        } else {
            aboutWebsite.setVisibility(View.GONE);
        }

        markAsSeenButton.setOnClickListener(v -> closeAndMarkWatched(random));
    }

    private void showRuntimePickerDialog() {
        String[] options = {
                getString(R.string.filterUnder15),
                getString(R.string.filterUnder30),
                getString(R.string.filterUnder45),
                getString(R.string.filterUnder60),
                getString(R.string.filterOver60),
                getString(R.string.randompickerBar)
        };

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int lastSelection = prefs.getInt("runtime_filter_selection", -1);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select runtime");
        builder.setSingleChoiceItems(options, lastSelection, (dialog, which) -> {
            prefs.edit().putInt("runtime_filter_selection", which).apply();
            switch (which) {
                case 0: pickRandomByRuntime(0, 15); break;
                case 1: pickRandomByRuntime(15, 30); break;
                case 2: pickRandomByRuntime(30, 45); break;
                case 3: pickRandomByRuntime(45, 60); break;
                case 4: pickRandomByRuntime(60, -1); break;
                default:
                    shows = EpisodesController.getInstance().getRandomWatchEpisodeShowList();
                    displayShow(shows.get(0));
                    break;
            }
            dialog.dismiss();
        });
        builder.setOnCancelListener(dialog -> {
            shows = EpisodesController.getInstance().getRandomWatchEpisodeShowList();
            if (!shows.isEmpty()) displayShow(shows.get(0));
        });
        builder.show();
    }

    private void pickRandomByRuntime(int minRuntime, int maxRuntime) {
        TaskRunner.getExecutor().execute(() -> {
            AppDatabase database = AppDatabase.getInstance(HomeActivity.getContext().getApplicationContext());
            SeriesDAO seriesDAO = database.getSeriesDAO();
            List<String> filteredShowIds;

            if (maxRuntime == -1) {
                filteredShowIds = seriesDAO.getShowIdsWithRuntimeAtLeast(minRuntime);
            } else if (minRuntime == 0) {
                filteredShowIds = seriesDAO.getShowIdsWithRuntimeUnder(maxRuntime);
            } else {
                filteredShowIds = seriesDAO.getShowIdsWithRuntimeBetween(minRuntime, maxRuntime);
            }

            List<String> finalFilteredShowIds = filteredShowIds;
            runOnUiThread(() -> {
                if (finalFilteredShowIds == null || finalFilteredShowIds.isEmpty()) {
                    Snackbar snackbar = Snackbar.make(findViewById(R.id.ScrollView01), "No shows found with this runtime", Snackbar.LENGTH_SHORT);
                    snackbar.setAnchorView(bottomNavigationView);
                    snackbar.show();
                    return;
                }

                List<Episode> watchEpisodes = EpisodesController.getInstance().getEpisodes(EpisodeType.EPISODES_TO_WATCH);
                if (watchEpisodes == null || watchEpisodes.isEmpty()) return;

                Map<String, List<Episode>> showMap = new HashMap<>();
                for (Episode ep : watchEpisodes) {
                    if (finalFilteredShowIds.contains(ep.getMyEpisodeID())) {
                        showMap.computeIfAbsent(ep.getMyEpisodeID(), k -> new ArrayList<>()).add(ep);
                    }
                }

                if (showMap.isEmpty()) {
                    Snackbar snackbar = Snackbar.make(findViewById(R.id.ScrollView01), "No watch episodes with this runtime", Snackbar.LENGTH_SHORT);
                    snackbar.setAnchorView(bottomNavigationView);
                    snackbar.show();
                    return;
                }

                List<String> showIds = new ArrayList<>(showMap.keySet());
                String randomShowId = showIds.get(new SecureRandom().nextInt(showIds.size()));
                List<Episode> episodesForShow = showMap.get(randomShowId);

                if (episodesForShow == null || episodesForShow.isEmpty()) return;

                String showId = episodesForShow.get(0).getMyEpisodeID();
                Show show = new Show(episodesForShow.get(0).getShowName(), showId);
                for (Episode ep : episodesForShow) {
                    show.addEpisode(ep);
                }

                displayShow(show);
            });
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
                .putExtra(ActivityConstants.EXTRA_TITLE, episode.getShowName())
                .putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_SHOW);

        startActivity(episodeListingActivity);

    }


    private final NavigationBarView.OnItemSelectedListener navigationItemSelectedListener = new NavigationBarView.OnItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            //episodesType = (EpisodeType) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE);
            //showMyEpisodeID = (String) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID);
            //String Title = data.getString(ActivityConstants.EXTRA_TITLE);

            Bundle BundleInfoShowDetail = new Bundle();
            BundleInfoShowDetail.putSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodesType);
            BundleInfoShowDetail.putString(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, showMyEpisodeID);
            BundleInfoShowDetail.putString(ActivityConstants.EXTRA_TITLE, "Random Episode");

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
                            ShowNameTitle.setTitle("No episodes to watch");
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
        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_TITLE, show.getShowName());
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
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_TITLE, episode.getShowName());
        startActivity(episodeDetailsSubActivity);
    }


    private void openShowSummary(EpisodeType episodeType) {
        finish();
        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), ShowSummaryActivity.class);
        episodeDetailsSubActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, showMyEpisodeID);
        // episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_TITLE, "");
        startActivity(episodeDetailsSubActivity);
    }

    private void openEpisodeDetails(Episode episode, EpisodeType episodeType) {
        finish();
        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), EpisodeDetailsActivity.class);
        episodeDetailsSubActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, episode.getMyEpisodeID());
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_TITLE, episode.getShowName());
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
                    if (!jObj.getString(MyEpisodeConstants.TVMAZE_IMAGE_KEY).equals("null")) {
                        episodeImageURL = jObj.getJSONObject(MyEpisodeConstants.TVMAZE_IMAGE_KEY).getString(MyEpisodeConstants.TVMAZE_IMAGE_SIZE_MEDIUM);
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

                String episodeImageURLStr = result.get("episodeImageURL");
                ImageView episodeImage = findViewById(R.id.episodeImage);
                if (episodeImageURLStr != null && !episodeImageURLStr.equals("")) {
                    RequestOptions requestOptions = new RequestOptions();
                    requestOptions.placeholder(R.drawable.placeholder);
                    requestOptions.error(R.drawable.error);

                    Glide.with(findViewById(R.id.episodeImage))
                            .load(episodeImageURLStr)
                            .apply(requestOptions)
                            .into(episodeImage);
                } else {
                    episodeImage.setVisibility(View.GONE);
                }
            });
        });
    }


    private void downloadShowSummary(HashMap<String, String> showSummaryHash, String... params) {
        TaskRunner.getExecutor().execute(() -> {
            AppDatabase database = AppDatabase.getInstance(nz.mentalinc.watcher.activities.HomeActivity.getContext().getApplicationContext());

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
                showSummaryHash.put(MyEpisodeConstants.SHOW_URL, showURL);
                showSummaryHash.put(MyEpisodeConstants.OFFICIAL_SITE, officialSite);
                showSummaryHash.put(MyEpisodeConstants.SHOW_SUMMARY, showSummary);
                showSummaryHash.put(MyEpisodeConstants.SHOW_IMAGE_URL, showImageURL);
                showSummaryHash.put(MyEpisodeConstants.SHOW_RUNTIME, ShowRuntime);
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
                        officialSite = jObj.getString(MyEpisodeConstants.OFFICIAL_SITE);
                        if (!jObj.getString(MyEpisodeConstants.TVMAZE_IMAGE_KEY).equals("null")) {
                            showImageURL = jObj.getJSONObject(MyEpisodeConstants.TVMAZE_IMAGE_KEY).getString(MyEpisodeConstants.TVMAZE_IMAGE_SIZE_MEDIUM);
                        }

                        showImageURL = showImageURL.replace("http://", "https://");

                        showSummary = showSummary.replace("<p>", "");
                        showSummary = showSummary.replace("</p>", "");
                        showSummary = showSummary.replace("<b>", "");
                        showSummary = showSummary.replace("</b>", "");
                        showSummary = showSummary.replace("<i>", "");
                        showSummary = showSummary.replace("</i>", "");

                        showSummaryHash.put(MyEpisodeConstants.SHOW_URL, showURL);
                        showSummaryHash.put(MyEpisodeConstants.SHOW_SUMMARY, showSummary);
                        showSummaryHash.put(MyEpisodeConstants.SHOW_IMAGE_URL, showImageURL);
                        showSummaryHash.put(MyEpisodeConstants.OFFICIAL_SITE, officialSite);
                        showSummaryHash.put("ShowName", ShowName);
                        showSummaryHash.put(MyEpisodeConstants.SHOW_RUNTIME, ShowRuntime);

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

            AppDatabase db = AppDatabase.getInstance(nz.mentalinc.watcher.activities.HomeActivity.getContext().getApplicationContext());
            SeriesDAO sDAO = db.getSeriesDAO();
            EpisodeRuntime showSummaryInfo = sDAO.getEpisodeRuntimeWithMyEpsId(random.getMyEpisodeID());
            showSummaryInfo.setShowSummary(result.get(MyEpisodeConstants.SHOW_SUMMARY));
            showSummaryInfo.setShowURL(result.get(MyEpisodeConstants.SHOW_URL));
            showSummaryInfo.setOfficialSite(result.get(MyEpisodeConstants.OFFICIAL_SITE));
            showSummaryInfo.setShowImageURL(result.get(MyEpisodeConstants.SHOW_IMAGE_URL));
            sDAO.update(showSummaryInfo);

            runOnUiThread(() -> {
                TextView ShowRuntimeTV = findViewById(R.id.episodeRuntime);
                String showruntimeText = " " + result.get(MyEpisodeConstants.SHOW_RUNTIME) + " mins";
                ShowRuntimeTV.setText(showruntimeText);
            });
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
