package nz.mentalinc.watcher.activities;

import android.app.ActivityOptions;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

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
import nz.mentalinc.watcher.service.ShowService;
import nz.mentalinc.watcher.utils.DateUtil;
import nz.mentalinc.watcher.utils.TaskRunner;

/**
 * @author Ivo Janssen, maintained and updated by mentalinc
 */
public class EpisodeDetailsActivity extends AppCompatActivity {
    private Episode episode = null;
    private List<Episode> episodesForShow = new ArrayList<>();
    private EpisodeType episodesType;
    private String title;
    private static final String LOG_TAG = EpisodeDetailsActivity.class.getSimpleName();
    private BottomNavigationView bottomNavigationView;
    Bundle data;
    private String showMyEpisodeID;
    List<Show> shows = new ArrayList<>();
    private GestureDetector gestureDetector;
    private int currentEpisodeIndex = 0;
    private List<Episode> episodesRaw = new ArrayList<>();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        setContentView(R.layout.episode_details);

        findViewById(R.id.appBarEpDetailOverviewLayout2).setZ(100f);

        Bundle data = this.getIntent().getExtras();
        title = data.getSerializable(ActivityConstants.EXTRA_TITLE, String.class);

        episode = Objects.requireNonNull(data).getParcelable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, Episode.class);
        episodesType = data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.class);
        showMyEpisodeID = data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, String.class);

        returnEpisodes();
        episodesForShow = !shows.isEmpty() ? shows.get(0).getEpisodes() : new ArrayList<>();
        currentEpisodeIndex = findEpisodeIndex(episode);

        displayEpisode(episode);

        String episodeFullNumbering = "S" + episode.getSeasonString() + "E" + episode.getEpisodeString();
        com.google.android.material.appbar.MaterialToolbar ShowNameTitle = findViewById(R.id.topAppBarEpisodeDetails);
        ShowNameTitle.setTitle(title + " - " + episodeFullNumbering);

        bottomNavigationView = findViewById(R.id.bottom_navigationEpisodeDetail);
        bottomNavigationView.getMenu().getItem(1).setChecked(true);
        bottomNavigationView.setOnItemSelectedListener(navigationItemSelectedListener);

        ScrollView scrollView = findViewById(R.id.ScrollView01);
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD && Math.abs(diffX) > Math.abs(e2.getY() - e1.getY())) {
                    if (diffX < 0) {
                        navigateToEpisode(1);
                    } else {
                        navigateToEpisode(-1);
                    }
                    return true;
                }
                return false;
            }
        });
        scrollView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });

        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {
            Log.w(LOG_TAG, "Home button clicked.");
            Intent home = new Intent(EpisodeDetailsActivity.this, HomeActivity.class);
            home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(home);
        });

        androidx.appcompat.view.menu.ActionMenuItemView appBarmarkAsSeen = findViewById(R.id.markAsSeen);
        appBarmarkAsSeen.setOnClickListener(v -> {
            Log.w(LOG_TAG, "markAsSeen button clicked.");
            closeAndMarkWatched(episode);
        });

        androidx.appcompat.view.menu.ActionMenuItemView appBarmarkAsAquired = findViewById(R.id.markAsAquired);
        appBarmarkAsAquired.setOnClickListener(v -> {
            Log.w(LOG_TAG, "markAsAquired button clicked.");
            closeAndAcquireEpisode(episode);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                exit();
            }
        });
    }

    private void displayEpisode(Episode ep) {
        episode = ep;

        TextView showNameText = findViewById(R.id.episodeDetShowName);
        TextView episodeNameText = findViewById(R.id.episodeDetName);
        TextView seasonText = findViewById(R.id.episodeDetSeason);
        TextView episodeText = findViewById(R.id.episodeDetEpisode);
        TextView airdateText = findViewById(R.id.episodeDetAirdate);

        showNameText.setText(ep.getShowName());
        episodeNameText.setText(ep.getName());
        seasonText.setText(ep.getSeasonString());
        episodeText.setText(ep.getEpisodeString());

        Date airdate = new Date(ep.getAirDate());
        String formattedAirDate;
        if (airdate != null) {
            formattedAirDate = DateUtil.formatDateLong(airdate);
        } else {
            formattedAirDate = getString(R.string.episodeDetailsAirDateLabelDateNotFound);
        }
        airdateText.setText(formattedAirDate);

        Button markAsAcquiredButton = findViewById(R.id.markAsAcquiredButton);
        Button markAsSeenButton = findViewById(R.id.markAsSeenButton);

        switch (episodesType) {
            case EPISODES_TO_WATCH:
                markAsAcquiredButton.setVisibility(View.GONE);
                markAsSeenButton.setVisibility(View.VISIBLE);
                break;
            case EPISODES_TO_YESTERDAY1:
            case EPISODES_TO_YESTERDAY2:
            case EPISODES_TO_ACQUIRE:
                markAsAcquiredButton.setVisibility(View.VISIBLE);
                markAsSeenButton.setVisibility(View.VISIBLE);
                break;
            case EPISODES_COMING:
                markAsAcquiredButton.setVisibility(View.GONE);
                markAsSeenButton.setVisibility(View.GONE);
                break;
            default: //added for code quality
        }
 
        markAsAcquiredButton.setOnClickListener(v -> closeAndAcquireEpisode(ep));
        markAsSeenButton.setOnClickListener(v -> closeAndMarkWatched(ep));

        com.google.android.material.appbar.MaterialToolbar ShowNameTitle = findViewById(R.id.topAppBarEpisodeDetails);
        ShowNameTitle.setTitle(title + " - S" + ep.getSeasonString() + "E" + ep.getEpisodeString());

        TextView aboutWebsite = findViewById(R.id.tvMazeWebsite);
        if (!TextUtils.isEmpty(ep.getTVMazeWebSite())) {
            TaskRunner.getExecutor().execute(() -> {
                AppDatabase database = AppDatabase.getInstance(HomeActivity.getContext().getApplicationContext());
                EpisodeRuntime showRuntime = database.getSeriesDAO().getEpisodeRuntimeWithMyEpsId(ep.getMyEpisodeID());
                String showTVMazeID = showRuntime.getShowTVMazeID();
                runOnUiThread(() -> {
                    HashMap<String, String> episodeSummaryHashMap = new HashMap<>();
                    episodeSummaryHashMap.put("a", "b");
                    HashMap<String, String> showSummaryHashMap = new HashMap<>();
                    showSummaryHashMap.put("a", "b");

                    downloadShowSummary(showSummaryHashMap, showTVMazeID);
                    downloadEpisodeSummary(episodeSummaryHashMap, showTVMazeID, ep.getSeasonString(), ep.getEpisodeString());
                });
            });
        } else {
            aboutWebsite.setVisibility(View.GONE);
        }
    }

    private int findEpisodeIndex(Episode target) {
        if (episodesForShow == null || episodesForShow.isEmpty()) return 0;
        for (int i = 0; i < episodesForShow.size(); i++) {
            Episode ep = episodesForShow.get(i);
            if (ep.getMyEpisodeID().equals(target.getMyEpisodeID()) &&
                    ep.getSeasonString().equals(target.getSeasonString()) &&
                    ep.getEpisodeString().equals(target.getEpisodeString())) {
                return i;
            }
        }
        return 0;
    }

    private void navigateToEpisode(int direction) {
        int newIndex = currentEpisodeIndex + direction;
        if (newIndex < 0 || newIndex >= episodesForShow.size()) return;
        currentEpisodeIndex = newIndex;
        displayEpisode(episodesForShow.get(newIndex));
    }

    private final NavigationBarView.OnItemSelectedListener navigationItemSelectedListener = new NavigationBarView.OnItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            final int previousItem = bottomNavigationView.getSelectedItemId();
            final int nextItem = item.getItemId();
            if (previousItem != nextItem) {
               // switch (nextItem) {
                    if( R.id.barShowDetail == nextItem) {
                        Log.w(LOG_TAG, "barShowDetail selected");
                        if (!shows.isEmpty()) {
                            openShowSummary(shows.get(0).getFirstEpisode(), episodesType);
                        } else {
                            //openEpisodeListing(episodesType);

                        }
                        return true;
                    } else if (R.id.barEpisodeOverview == nextItem) {
                        Log.w(LOG_TAG, "barEpisodeOverview selected");

                        if (shows.size() > 0) {
                            //   episodesType = EpisodeType.EPISODES_TO_WATCH;
                            //   returnEpisodes();
                            openEpisodeDetails(shows.get(0).getFirstEpisode(), episodesType);
                        } else {


                        }
                        return true;
                    }else if( R.id.barWatch == nextItem){
                    Log.w(LOG_TAG, "barWatch selected");
                    if (shows.size() > 0) {
                        openEpisodeListing(shows.get(0), EpisodeType.EPISODES_TO_WATCH);
                    } else {
                        openEpisodeListing(episodesType);
                    }
                    return true;
                }else if ( R.id.barAcquire == nextItem) {
                        Log.w(LOG_TAG, "barAcquire selected");
                        if (shows.size() > 0) {
                            openEpisodeListing(shows.get(0), EpisodeType.EPISODES_TO_ACQUIRE);
                        } else {
                            openEpisodeListing(episodesType);
                        }
                        return true;
                    }else if( R.id.barComing == nextItem) {
                        Log.w(LOG_TAG, "barComing selected");
                        if (shows.size() > 0) {
                            openEpisodeListing(shows.get(0), EpisodeType.EPISODES_COMING);
                        } else {
                           /* episodesType = EpisodeType.EPISODES_COMING;
                            returnEpisodes();
                            if (shows.size() > 0) {
                                openEpisodeListing(shows.get(0), EpisodeType.EPISODES_COMING);
                            }*/
                            openEpisodeListing(episodesType);
                        }
                        return true;
                }
                else {
                        return false;
                    }
            }
            return false;
        }

    };


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

                    Toast.makeText(EpisodeDetailsActivity.this, getString(R.string.episodeNotFoundViaAPI), Toast.LENGTH_LONG).show();
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
                String episodeSummary = "";
                String episodeURL = "";
                String episodeImageURL = "";

                try {
                    jObj = new JSONObject(jsonString);
                    episodeSummary = jObj.getString("summary");
                    episodeURL = jObj.getString("url");
                    if (!"null".equals(jObj.getString(MyEpisodeConstants.TVMAZE_IMAGE_KEY))) {
                        episodeImageURL = jObj.getJSONObject(MyEpisodeConstants.TVMAZE_IMAGE_KEY).getString(MyEpisodeConstants.TVMAZE_IMAGE_SIZE_MEDIUM);
                    }

                    episodeURL = episodeURL.replace("http://", "https://");
                    episodeSummary = episodeSummary.replace("<p>", "");
                    episodeSummary = episodeSummary.replace("</p>", "");

                    episodeSummaryHash.put("episodeURL", episodeURL);
                    episodeSummaryHash.put("episodeSummary", episodeSummary);
                    episodeSummaryHash.put("episodeImageURL", episodeImageURL);

                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error parsing episode JSON", e);
                }

            } catch (IOException | InterruptedException e) {
                Log.e(LOG_TAG, "Error fetching episode from API", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error closing reader", e);
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
                    if (!"null".equals(episodeSummaryStr)) {
                        tvMazeEpisodeSummary.setText(episodeSummaryStr);
                    } else {
                        tvMazeEpisodeSummary.setVisibility(View.GONE);
                    }
                }

                String episodeImageURLStr = result.get("episodeImageURL");
                ImageView episodeImage = findViewById(R.id.episodeImage);
                if (!episodeImageURLStr.isEmpty()) {
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
            String showStatus = showInfo.getShowStatus();

            if (!ShowName.isEmpty() && !showURL.isEmpty() && !officialSite.isEmpty() && !showSummary.isEmpty() && !showImageURL.isEmpty() && !ShowRuntime.isEmpty() && showStatus != null) {

                showSummaryHash.put("ShowName", ShowName);
                showSummaryHash.put(MyEpisodeConstants.SHOW_URL, showURL);
                showSummaryHash.put(MyEpisodeConstants.OFFICIAL_SITE, officialSite);
                showSummaryHash.put(MyEpisodeConstants.SHOW_SUMMARY, showSummary);
                showSummaryHash.put(MyEpisodeConstants.SHOW_IMAGE_URL, showImageURL);
                showSummaryHash.put(MyEpisodeConstants.SHOW_RUNTIME, ShowRuntime);
                showSummaryHash.put(MyEpisodeConstants.SHOW_STATUS, showStatus);

            } else {
                try {
                    JSONObject jObj = ShowService.fetchTvMazeShowJson(params[0]);
                    showSummary = jObj.getString("summary");
                    ShowName = jObj.getString("name");
                    showURL = jObj.getString("url");
                    ShowRuntime = jObj.getString("runtime");

                    if (ShowRuntime == null || "null".equals(ShowRuntime)) {
                        ShowRuntime = jObj.getString("averageRuntime");
                    }

                    officialSite = jObj.getString(MyEpisodeConstants.OFFICIAL_SITE);
                    showStatus = jObj.optString("status", null);
                    if (!"null".equals(jObj.getString(MyEpisodeConstants.TVMAZE_IMAGE_KEY))) {
                        showImageURL = jObj.getJSONObject(MyEpisodeConstants.TVMAZE_IMAGE_KEY).getString(MyEpisodeConstants.TVMAZE_IMAGE_SIZE_MEDIUM);
                    }

                    showImageURL = showImageURL.replace("http://", "https://");
                    showSummary = ShowService.stripHtml(showSummary);

                    showSummaryHash.put(MyEpisodeConstants.SHOW_URL, showURL);
                    showSummaryHash.put(MyEpisodeConstants.SHOW_SUMMARY, showSummary);
                    showSummaryHash.put(MyEpisodeConstants.SHOW_IMAGE_URL, showImageURL);
                    showSummaryHash.put(MyEpisodeConstants.OFFICIAL_SITE, officialSite);
                    showSummaryHash.put("ShowName", ShowName);
                    showSummaryHash.put(MyEpisodeConstants.SHOW_RUNTIME, ShowRuntime);
                    showSummaryHash.put(MyEpisodeConstants.SHOW_STATUS, showStatus);

                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error downloading show summary", e);
                }
            }

            HashMap<String, String> result = showSummaryHash;

            showInfo.setShowSummary(result.get(MyEpisodeConstants.SHOW_SUMMARY));
            showInfo.setShowURL(result.get(MyEpisodeConstants.SHOW_URL));
            showInfo.setOfficialSite(result.get(MyEpisodeConstants.OFFICIAL_SITE));
            showInfo.setShowRuntime(result.get(MyEpisodeConstants.SHOW_RUNTIME));
            showInfo.setShowImageURL(result.get(MyEpisodeConstants.SHOW_IMAGE_URL));
            showInfo.setShowStatus(result.get(MyEpisodeConstants.SHOW_STATUS));
            seriesDAO.update(showInfo);

            runOnUiThread(() -> {
                TextView ShowRuntimeTV = findViewById(R.id.episodeRuntime);
                ShowRuntimeTV.setText(result.get(MyEpisodeConstants.SHOW_RUNTIME) + " mins");
            });
        });
    }

    /*
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.episode_details_menu, menu);
            if (episodesType.equals(EpisodeType.EPISODES_TO_WATCH)) {
                menu.removeItem(R.id.markAsAquired);
            } else if (episodesType.equals(EpisodeType.EPISODES_COMING)) {
                menu.removeItem(R.id.markAsAquired);
            }
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.markAsSeen:
                    closeAndMarkWatched(episode);
                    return true;
                case R.id.markAsAquired:
                    closeAndAcquireEpisode(episode);
                    return true;
                case R.id.btn_title_share:
                    tweetThis();
                    return true;
                case R.id.home:
                    exit();
                    return true;
            }
            return false;
        }
    */
    private void closeAndAcquireEpisode(Episode episode) {
        finish();

        openListingActivity(episode, ActivityConstants.EXTRA_BUNDLE_VALUE_ACQUIRE);
    }

    private void closeAndMarkWatched(Episode episode) {
        finish();

        openListingActivity(episode, ActivityConstants.EXTRA_BUNDLE_VALUE_WATCH);
    }

    private void openListingActivity(Episode episode, String type) {
        String[] showOrderOptions = getResources().getStringArray(R.array.showOrderOptionsValues);

        Intent episodeListingActivity = new Intent(getApplicationContext(), UpdatedEpisodeListingActivity.class);

        episodeListingActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode)
                .putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_MARK_EPISODE, type)
                .putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodesType)
                .putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, showMyEpisodeID)
                .putExtra(ActivityConstants.EXTRA_TITLE, title);


        String sorting = "";
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        switch (episodesType) {
            case EPISODES_TO_WATCH:
                // sorting = Preferences.getPreference(this, PreferencesKeys.WATCH_SHOW_SORTING_KEY);
                sorting = sharedPref.getString("showWatchOrder", MyEpisodeConstants.SHOW_MYEPISODES_DEFAULT_SORT);
                break;
            case EPISODES_TO_YESTERDAY1:
            case EPISODES_TO_YESTERDAY2:
            case EPISODES_TO_ACQUIRE:
                //sorting = Preferences.getPreference(this, PreferencesKeys.ACQUIRE_SHOW_SORTING_KEY);
                sorting = sharedPref.getString("showAcquireOrder", MyEpisodeConstants.SHOW_MYEPISODES_DEFAULT_SORT);
                break;
            case EPISODES_COMING:
                //sorting = Preferences.getPreference(this, PreferencesKeys.COMING_SHOW_SORTING_KEY);
                sorting = sharedPref.getString("showComingOrder", MyEpisodeConstants.SHOW_MYEPISODES_DEFAULT_SORT);
                break;
            default: //added for code quality
        }
 
        if (sorting.equals(showOrderOptions[3])) {
            episodeListingActivity.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_DATE);
        } else {
            episodeListingActivity.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_SHOW);
        }

        startActivity(episodeListingActivity, ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_left, R.anim.slide_out_right).toBundle());
    }

    private void tweetThis() {
        String tweet = episode.getShowName() + " S" + episode.getSeasonString() + "E" + episode.getEpisodeString() + " - " + episode.getName();
        Intent i = new Intent(android.content.Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, getString(R.string.Tweet, tweet));
        startActivity(Intent.createChooser(i, getString(R.string.TweetTitle)));
    }

    public void onHomeClick(View v) {
        exit();
    }

    private void exit() {
        Intent home = new Intent(this, HomeActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(home);
    }


    private void openEpisodeListing(EpisodeType episodeType) {
        finish();
        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), UpdatedEpisodeListingActivity.class);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, showMyEpisodeID);
        // episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_TITLE, title);
        startActivity(episodeDetailsSubActivity, ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_left, R.anim.slide_out_right).toBundle());
    }

    private void openEpisodeListing(Show show, EpisodeType episodeType) {
        //finish();
        Intent updatedEpisodeListActivity = new Intent(this.getApplicationContext(), UpdatedEpisodeListingActivity.class);

        Episode nextEpisodeToWatch = show.getFirstEpisode();
        String myepisodeID = nextEpisodeToWatch.getMyEpisodeID();

        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, myepisodeID);
        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_TITLE, show.getShowName());
        startActivity(updatedEpisodeListActivity, ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_left, R.anim.slide_out_right).toBundle());
    }

    private void openShowSummary(Episode episode, EpisodeType episodeType) {
        //finish();
        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), ShowSummaryActivity.class);
        String myEpisodeID = episode.getMyEpisodeID();

        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, myEpisodeID);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_TITLE, episode.getShowName());
        startActivity(episodeDetailsSubActivity, ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_left, R.anim.slide_out_right).toBundle());
    }

    private void openEpisodeDetails(Episode episode, EpisodeType episodeType) {
        //finish();
        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), EpisodeDetailsActivity.class);
        String myEpisodeID = episode.getMyEpisodeID();

        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, myEpisodeID);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_TITLE, episode.getShowName());
        startActivity(episodeDetailsSubActivity, ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_left, R.anim.slide_out_right).toBundle());
    }

    private void returnEpisodes() {
        //ideally this just grabs the data from the show somehow, loop is slow with lots of data
        episodesRaw = EpisodesController.getInstance().getEpisodes(episodesType);
        //shows = new ArrayList<Show>();

        if (episodesRaw != null && episodesRaw.size() > 0) {
            for (Episode ep : episodesRaw) {
                if (ep.getMyEpisodeID().equals(showMyEpisodeID)) {
                    AddEpisodeToShow(ep);
                }
            }
        } else {
            Log.d(LOG_TAG, "Episode can't be added to show.");
        }
        Log.d(LOG_TAG, "Episodes type being added: " + episodesType);

        sortEpisodesOfShows(shows);
    }

    private void AddEpisodeToShow(Episode episode) {

        Show currentShow = CheckShowDuplicate(episode.getShowName());

        if (currentShow == null) {
            Show tempShow = new Show(episode.getShowName(), episode.getMyEpisodeID());
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

    private void sortEpisodesOfShows(List<Show> showList) {
        EpisodesController.sortEpisodesOfShows(this, showList);
    }

}
