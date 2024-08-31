package nz.mentalinc.episodeWatcher.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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

/**
 * @author Ivo Janssen, maintained and updated by mentalinc
 */
public class EpisodeDetailsActivity extends Activity {
    private Episode episode = null;
    private List<Episode> episodesRaw = new ArrayList<Episode>();
    private EpisodeType episodesType;
    private String title;
    private static final String LOG_TAG = EpisodeDetailsActivity.class.getSimpleName();
    private BottomNavigationView bottomNavigationView;
    Bundle data;
    private String showMyEpisodeID;
    List<Show> shows = new ArrayList<Show>();
    ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
 /*      SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
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
        }*/
        super.onCreate(savedInstanceState);


        //TODO ADD in a check so that only show this view when clicking on an episode to get more info (updatedepisodelistingactivity). if in the other views and not an actual episode, should only no new episode to watch.

        setContentView(R.layout.episode_details);

        Bundle data = this.getIntent().getExtras();
        title = (String) data.getSerializable("Title");
        TextView showNameText = findViewById(R.id.episodeDetShowName);
        TextView episodeNameText = findViewById(R.id.episodeDetName);
        TextView seasonText = findViewById(R.id.episodeDetSeason);
        TextView episodeText = findViewById(R.id.episodeDetEpisode);
        TextView airdateText = findViewById(R.id.episodeDetAirdate);

        episode = (Episode) Objects.requireNonNull(data).getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE);
        episodesType = (EpisodeType) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE);
        showMyEpisodeID = (String) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID);

        AddEpisodeToShow(episode);

        //returnEpisodes();

        String seasonNumber = episode.getSeasonString();
        String episodeNumber = episode.getEpisodeString();
        String episodeFullNumbering = "S" + seasonNumber + "E" + episodeNumber;

        com.google.android.material.appbar.MaterialToolbar ShowNameTitle = findViewById(R.id.topAppBarEpisodeDetails);
        title = title + " - " + episodeFullNumbering;
        ShowNameTitle.setTitle(title);

        bottomNavigationView = findViewById(R.id.bottom_navigationEpisodeDetail);
        bottomNavigationView.getMenu().getItem(1).setChecked(true);
        bottomNavigationView.setOnItemSelectedListener(navigationItemSelectedListener);

        showNameText.setText(episode.getShowName());
        episodeNameText.setText(episode.getName());
        seasonText.setText(episode.getSeasonString());
        episodeText.setText(episode.getEpisodeString());


        //Air date in specific format
        Date airdate = episode.getAirDate();
        String formattedAirDate;
        if (airdate != null) {
            formattedAirDate = DateUtil.formatDateLong(airdate);
        } else {
            formattedAirDate = getText(R.string.episodeDetailsAirDateLabelDateNotFound).toString();
        }

        airdateText.setText(formattedAirDate);

        TextView aboutWebsite = findViewById(R.id.tvMazeWebsite);
        if (!TextUtils.isEmpty(episode.getTVMazeWebSite())) {
            //aboutWebsite.setText(episode.getTVMazeWebSite());
            AppDatabase database = Room.databaseBuilder(nz.mentalinc.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                    .allowMainThreadQueries()   //Allows room to do operation on main thread
                    .fallbackToDestructiveMigration()
                    .build();

            SeriesDAO seriesDAO = database.getSeriesDAO();
            EpisodeRuntime showRuntime = seriesDAO.getEpisodeRuntimeWithMyEpsId(episode.getMyEpisodeID());


            //create hashmap's to prevent build fails, they get replaced
            HashMap episodeSummaryHashMap = new HashMap<String, String>() {{
                put("a", "b");
            }};
            HashMap showSummaryHashMap = new HashMap<String, String>() {{
                put("a", "b");
            }};


            new downloadShowSummary(showSummaryHashMap).execute(showRuntime.getShowTVMazeID());
            new downloadEpisodeSummary(episodeSummaryHashMap).execute(showRuntime.getShowTVMazeID(), episode.getSeasonString(), episode.getEpisodeString());


            //       episodeSummaryHashMap.get("episodeURL");
            database.close();
        } else {
            aboutWebsite.setVisibility(View.GONE);
        }

        Button markAsAcquiredButton = findViewById(R.id.markAsAcquiredButton);
        Button markAsSeenButton = findViewById(R.id.markAsSeenButton);

        switch (episodesType) {
            case EPISODES_TO_WATCH:
                markAsAcquiredButton.setVisibility(View.GONE);
                break;
            case EPISODES_TO_YESTERDAY1:
            case EPISODES_TO_YESTERDAY2:
            case EPISODES_TO_ACQUIRE:
                break;
            case EPISODES_COMING:
                // Cant see or acquire future episdodes (unless same day?), just remove the buttons.
                markAsAcquiredButton.setVisibility(View.GONE);
                markAsSeenButton.setVisibility(View.GONE);
                break;
        }

        markAsAcquiredButton.setOnClickListener(v -> closeAndAcquireEpisode(episode));

        markAsSeenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeAndMarkWatched(episode);
            }
        });


        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(LOG_TAG, "Home button clicked.");
                //exit();
                finish();
            }
        });

        androidx.appcompat.view.menu.ActionMenuItemView appBarmarkAsSeen = findViewById(R.id.markAsSeen);
        appBarmarkAsSeen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.w(LOG_TAG, "markAsSeen button clicked.");
                closeAndMarkWatched(episode);
            }
        });

        androidx.appcompat.view.menu.ActionMenuItemView appBarmarkAsAquired = findViewById(R.id.markAsAquired);
        appBarmarkAsAquired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.w(LOG_TAG, "markAsAquired button clicked.");
                closeAndAcquireEpisode(episode);
            }
        });


    }

    private BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            final int previousItem = bottomNavigationView.getSelectedItemId();
            final int nextItem = item.getItemId();
            if (previousItem != nextItem) {
                switch (nextItem) {
                    case R.id.barShowDetail:
                        Log.w(LOG_TAG, "barShowDetail selected");
                        //TODO need to build an activity to use the showDetail content.
                        if (shows.size() > 0) {
                            openShowSummary(shows.get(0).getFirstEpisode(), episodesType);
                        } else {
                            //openEpisodeListing(episodesType);

                        }
                        return true;
                    case R.id.barEpisodeOverview:
                        Log.w(LOG_TAG, "barEpisodeOverview selected");

                        if (shows.size() > 0) {
                            //   episodesType = EpisodeType.EPISODES_TO_WATCH;
                            //   returnEpisodes();
                            openEpisodeDetails(shows.get(0).getFirstEpisode(), episodesType);
                        } else {


                        }
                        return true;
                    case R.id.barWatch:
                        Log.w(LOG_TAG, "barWatch selected");
                        if (shows.size() > 0) {
                            openEpisodeListing(shows.get(0), EpisodeType.EPISODES_TO_WATCH);
                        } else {
                            openEpisodeListing(episodesType);
                        }
                        return true;
                    case R.id.barAcquire:
                        Log.w(LOG_TAG, "barAcquire selected");
                        if (shows.size() > 0) {
                            openEpisodeListing(shows.get(0), EpisodeType.EPISODES_TO_ACQUIRE);
                        } else {
                            openEpisodeListing(episodesType);
                        }
                        return true;
                    case R.id.barComing:
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
                return false;
            }
            return false;
        }

    };


    private class downloadEpisodeSummary extends AsyncTask<String, String, HashMap<String, String>> {
        final HashMap<String, String> episodeSummaryHash;

        downloadEpisodeSummary(HashMap<String, String> episodeSummaryHash) {
            this.episodeSummaryHash = episodeSummaryHash;
        }

        /*
            doInBackground(Params... params)
                Override this method to perform a computation on a background thread.
         */
        protected HashMap<String, String> doInBackground(String... params) {

            HttpsURLConnection connection = null;
            BufferedReader reader = null;
            //String episodeSummaryAPIURL = "https://api.tvmaze.com/shows/" + ShowTVMazeID + "/episodebynumber?season=" + SeasonString + "&number=" + EpisodeString;
            String episodeSummaryAPIURL = "https://api.tvmaze.com/shows/" + params[0] + "/episodebynumber?season=" + params[1] + "&number=" + params[2];


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

                //episode not found so stop and display to user...
                String jsonString;
                if (code == 404) {
                    jsonString = "{\"id\":0,\"url\":\"Unknown Episode\",\"name\":\"Unknown Episode\",\"image\":null,\"summary\":\"Unknown Episode\"}";

                    Toast.makeText(EpisodeDetailsActivity.this, "episode not found via API", Toast.LENGTH_LONG).show();
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
                    //      showNameString = jObj.getString("name");
                    //     showRuntimeString = jObj.getString("runtime");
                    episodeSummary = jObj.getString("summary");
                    episodeURL = jObj.getString("url");
                    if (!jObj.getString("image").equals("null")) {
                        episodeImageURL = jObj.getJSONObject("image").getString("medium");
                    }

                    //change the http:// to https://
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
            return episodeSummaryHash;
        }

        /*
            onPostExecute(Result result)
                Runs on the UI thread after doInBackground(Params...).
         */
        protected void onPostExecute(HashMap<String, String> result) {
            //episodeSummaryHash = result;
            TextView aboutWebsite = findViewById(R.id.tvMazeWebsite);
            aboutWebsite.setText(episodeSummaryHash.get("episodeURL"));
            Linkify.addLinks(aboutWebsite, Linkify.WEB_URLS);

            TextView tvMazeEpisodeSummary = findViewById(R.id.tvMazeEpisodeSummary);

            String episodeSummary = episodeSummaryHash.get("episodeSummary");

            //null check is to before NPE when there is no network (i.e. flight mode).
            if (!(episodeSummary == null)) {
                if (!episodeSummary.equals("null")) { //not a type want to check for the string null not null no object
                    tvMazeEpisodeSummary.setText(episodeSummary);
                } else {
                    tvMazeEpisodeSummary.setVisibility(View.GONE);
                }
            }


            //set the Episode Image
            String episodeImageURL = episodeSummaryHash.get("episodeImageURL");
            ImageView episodeImage = findViewById(R.id.episodeImage);
            if (!episodeImageURL.equals("")) {

                RequestOptions requestOptions = new RequestOptions();
                requestOptions.placeholder(R.drawable.placeholder);
                requestOptions.error(R.drawable.error);


              //  if (!a.isFinishing()){
                    Glide.with(findViewById(R.id.episodeImage))
                            .load(episodeImageURL)
                            .apply(requestOptions)
                            .into(episodeImage);
           // }

            } else {
                episodeImage.setVisibility(View.GONE);
            }

        }
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
                    .allowMainThreadQueries()   //Allows room to do operation on main thread
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
            String showStatus = showInfo.getShowStatus();

            if(!ShowRuntime.equals("")){
                TextView ShowRuntimeTV = findViewById(R.id.episodeRuntime);
                ShowRuntimeTV.setText(ShowRuntime + " mins");

            }

            if (!ShowName.equals("") && !showURL.equals("") && !officialSite.equals("") && !showSummary.equals("") && !showImageURL.equals("") && !ShowRuntime.equals("")  && showStatus != null) {

                showSummaryHash.put("ShowName", ShowName);
                showSummaryHash.put("showURL", showURL);
                showSummaryHash.put("officialSite", officialSite);
                showSummaryHash.put("showSummary", showSummary);
                showSummaryHash.put("showImageURL", showImageURL);
                showSummaryHash.put("ShowRuntime", ShowRuntime);
                showSummaryHash.put("showStatus", showStatus);


                //not in database so download from API
            } else {


                HttpsURLConnection connection = null;
                BufferedReader reader = null;
                //String episodeSummaryAPIURL = "https://api.tvmaze.com/shows/" + ShowTVMazeID + "/episodebynumber?season=" + SeasonString + "&number=" + EpisodeString;
                String episodeSummaryAPIURL = "https://api.tvmaze.com/shows/" + params[0];
                //  HashMap<String, String> hashMap = new HashMap<String, String>();

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
                        //      showNameString = jObj.getString("name");
                        //     showRuntimeString = jObj.getString("runtime");
                        showSummary = jObj.getString("summary");
                        ShowName = jObj.getString("name");
                        showURL = jObj.getString("url");
                        ShowRuntime = jObj.getString("runtime");
                        if (ShowRuntime.equals("null") || ShowRuntime == null) {
                            ShowRuntime = jObj.getString("averageRuntime");
                        }
                        officialSite = jObj.getString("officialSite");
                        if (!jObj.getString("image").equals("null")) {
                            showImageURL = jObj.getJSONObject("image").getString("medium");
                        }

                        //change the http:// to https://
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
                        showSummaryHash.put("showStatus", showStatus);

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
            database.close();
            return showSummaryHash;
        }

        /*
            onPostExecute(Result result)
                Runs on the UI thread after doInBackground(Params...).
         */
        protected void onPostExecute(HashMap<String, String> result) {
            //episodeSummaryHash = result;
/*
            TextView ShowName = findViewById(R.id.ShowName);
            ShowName.setText(showSummaryHash.get("ShowName"));
*/
            TextView ShowRuntime = findViewById(R.id.episodeRuntime);
            ShowRuntime.setText(showSummaryHash.get("ShowRuntime") + " mins");
/*
            TextView aboutShowWebsite = findViewById(R.id.tvMazeShowWebsite);
            aboutShowWebsite.setText(showSummaryHash.get("showURL"));
            Linkify.addLinks(aboutShowWebsite, Linkify.WEB_URLS);

            TextView aboutShowOfficialWebsite = findViewById(R.id.officialShowWebsite);
            String showOfficialWebsite = showSummaryHash.get("officialSite");
            if (!showOfficialWebsite.equals("null")) { //not a type want to check for the string null not null no object
                aboutShowOfficialWebsite.setText(showOfficialWebsite);
                Linkify.addLinks(aboutShowOfficialWebsite, Linkify.WEB_URLS);
            } else {
                aboutShowOfficialWebsite.setVisibility(View.GONE);
            }

            TextView tvMazeShowSummary = findViewById(R.id.tvMazeShowSummary);

            String episodeSummary = showSummaryHash.get("showSummary");

            if (!episodeSummary.equals("null")) { //not a type want to check for the string null not null no object
                tvMazeShowSummary.setText(episodeSummary);
            } else {
                tvMazeShowSummary.setVisibility(View.GONE);
            }

            ImageView showImage = findViewById(R.id.showImage);

            String showImageURL = showSummaryHash.get("showImageURL");
            // add in here to download tv series info...and the show level info to a database!

            if (!showImageURL.equals("")) {

                RequestOptions requestOptions = new RequestOptions();
                requestOptions.placeholder(R.drawable.placeholder);
                requestOptions.error(R.drawable.error);

                Glide.with(findViewById(R.id.showImage))
                        .load(showImageURL)
                        .apply(requestOptions)
                        .into(showImage);

            } else {
                showImage.setVisibility(View.GONE);
            }

*/
            //add the info into the show database to limit the need to api call the info all the time for static show info
            AppDatabase database = Room.databaseBuilder(nz.mentalinc.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                    .allowMainThreadQueries()   //Allows room to do operation on main thread
                    .fallbackToDestructiveMigration()
                    .build();

            SeriesDAO seriesDAO = database.getSeriesDAO();


            EpisodeRuntime showSummaryInfo = seriesDAO.getEpisodeRuntimeWithMyEpsId(episode.getMyEpisodeID());


            //Inserting an episodeRuntime adding the info that was not collected during the runtime. addition


            showSummaryInfo.setShowSummary(showSummaryHash.get("showSummary"));
            showSummaryInfo.setShowURL(showSummaryHash.get("showURL"));
            showSummaryInfo.setOfficialSite(showSummaryHash.get("officialSite"));
            showSummaryInfo.setShowRuntime(showSummaryHash.get("ShowRuntime"));
            showSummaryInfo.setShowImageURL(showSummaryHash.get("showImageURL"));
            showSummaryInfo.setShowStatus(showSummaryHash.get("showStatus"));
            //  Log.d("epsRunTime: ", epsRunTime.toString());

            seriesDAO.update(showSummaryInfo);
            database.close();

        }
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

        OpenListingActivity(episode, ActivityConstants.EXTRA_BUNDLE_VALUE_ACQUIRE);
    }

    private void closeAndMarkWatched(Episode episode) {
        finish();

        OpenListingActivity(episode, ActivityConstants.EXTRA_BUNDLE_VALUE_WATCH);
    }

    private void OpenListingActivity(Episode episode, String type) {
        String[] showOrderOptions = getResources().getStringArray(R.array.showOrderOptionsValues);

        //todo need to have it call the new show home tab, but the buttons fail to work when clicking acquire.
        //Intent episodeListingActivity = new Intent(this.getApplicationContext(), ShowHomeTabActivity.class);
        //Intent episodeListingActivity = new Intent(this.getApplicationContext(), EpisodeListingActivity.class);
        Intent episodeListingActivity = new Intent(getApplicationContext(), UpdatedEpisodeListingActivity.class);

        episodeListingActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode)
                .putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_MARK_EPISODE, type)
                .putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodesType)
                .putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, showMyEpisodeID)
                .putExtra("Title", title);


        String sorting = "";
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        switch (episodesType) {
            case EPISODES_TO_WATCH:
                // sorting = Preferences.getPreference(this, PreferencesKeys.WATCH_SHOW_SORTING_KEY);
                sorting = sharedPref.getString("showWatchOrder", "show_myepisodes_default_sort");
                break;
            case EPISODES_TO_YESTERDAY1:
            case EPISODES_TO_YESTERDAY2:
            case EPISODES_TO_ACQUIRE:
                //sorting = Preferences.getPreference(this, PreferencesKeys.ACQUIRE_SHOW_SORTING_KEY);
                sorting = sharedPref.getString("showAcquireOrder", "show_myepisodes_default_sort");
                break;
            case EPISODES_COMING:
                //sorting = Preferences.getPreference(this, PreferencesKeys.COMING_SHOW_SORTING_KEY);
                sorting = sharedPref.getString("showComingOrder", "show_myepisodes_default_sort");
                break;
        }

        if (sorting.equals(showOrderOptions[3])) {
            episodeListingActivity.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_DATE);
        } else {
            episodeListingActivity.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_SHOW);
        }

        startActivity(episodeListingActivity);
    }

    private void OpenListingActivity() {
        String[] showOrderOptions = getResources().getStringArray(R.array.showOrderOptionsValues);

        Intent episodeListingActivity = new Intent(this.getApplicationContext(), EpisodeListingActivity.class);
        episodeListingActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodesType);

        String sorting = "";
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        switch (episodesType) {
            case EPISODES_TO_WATCH:
                //sorting = Preferences.getPreference(this, PreferencesKeys.WATCH_SHOW_SORTING_KEY);                
                sorting = sharedPref.getString("showWatchOrder", "show_myepisodes_default_sort");
                break;
            case EPISODES_TO_YESTERDAY1:
            case EPISODES_TO_YESTERDAY2:
            case EPISODES_TO_ACQUIRE:
                //sorting = Preferences.getPreference(this, PreferencesKeys.ACQUIRE_SHOW_SORTING_KEY);
                sorting = sharedPref.getString("showAcquireOrder", "show_myepisodes_default_sort");
                break;
            case EPISODES_COMING:
                //sorting = Preferences.getPreference(this, PreferencesKeys.COMING_SHOW_SORTING_KEY);
                sorting = sharedPref.getString("showComingOrder", "show_myepisodes_default_sort");
                break;
        }

        if (sorting.equals(showOrderOptions[3])) {
            episodeListingActivity.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_DATE);
        } else {
            episodeListingActivity.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_SHOW);
        }
        episodeListingActivity.putExtra("Title", title);
        // startActivity(episodeListingActivity);
    }

    private void tweetThis() {
        String tweet = episode.getShowName() + " S" + episode.getSeasonString() + "E" + episode.getEpisodeString() + " - " + episode.getName();
        Intent i = new Intent(android.content.Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, getString(R.string.Tweet, tweet));
        startActivity(Intent.createChooser(i, getString(R.string.TweetTitle)));
    }

    public void onTweetClick(View v) {
        tweetThis();
    }

    @Override
    public final void onBackPressed() {
        exit();
    }

    public void onHomeClick(View v) {
        exit();
    }

    private void exit() {
        finish();

        OpenListingActivity();
    }


    private void openEpisodeListing(EpisodeType episodeType) {
        finish();
        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), UpdatedEpisodeListingActivity.class);
        episodeDetailsSubActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, showMyEpisodeID);
        // episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra("Title", data.getString("Title"));
        startActivity(episodeDetailsSubActivity);
    }

    private void openEpisodeListing(Show show, EpisodeType episodeType) {
        //finish();
        Intent updatedEpisodeListActivity = new Intent(this.getApplicationContext(), UpdatedEpisodeListingActivity.class);

        Episode nextEpisodeToWatch = show.getFirstEpisode();
        String myepisodeID = nextEpisodeToWatch.getMyEpisodeID();

        updatedEpisodeListActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, myepisodeID);
        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        updatedEpisodeListActivity.putExtra("Title", show.getShowName());
        startActivity(updatedEpisodeListActivity);
    }

    private void openShowSummary(Episode episode, EpisodeType episodeType) {
        //finish();
        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), ShowSummaryActivity.class);
        String myEpisodeID = episode.getMyEpisodeID();
        episodeDetailsSubActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, myEpisodeID);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra("Title", episode.getShowName());
        startActivity(episodeDetailsSubActivity);
    }

    private void openEpisodeDetails(Episode episode, EpisodeType episodeType) {
        //finish();
        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), EpisodeDetailsActivity.class);
        String myEpisodeID = episode.getMyEpisodeID();

        episodeDetailsSubActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, myEpisodeID);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra("Title", episode.getShowName());
        startActivity(episodeDetailsSubActivity);
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

}
