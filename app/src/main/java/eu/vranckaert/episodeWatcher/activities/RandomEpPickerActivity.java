package eu.vranckaert.episodeWatcher.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.room.Room;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

import eu.vranckaert.episodeWatcher.R;
import eu.vranckaert.episodeWatcher.constants.ActivityConstants;
import eu.vranckaert.episodeWatcher.controllers.EpisodesController;
import eu.vranckaert.episodeWatcher.database.AppDatabase;
import eu.vranckaert.episodeWatcher.database.SeriesDAO;
import eu.vranckaert.episodeWatcher.domain.Episode;
import eu.vranckaert.episodeWatcher.enums.EpisodeType;
import eu.vranckaert.episodeWatcher.enums.ListMode;
import eu.vranckaert.episodeWatcher.preferences.Preferences;
import eu.vranckaert.episodeWatcher.preferences.PreferencesKeys;
import eu.vranckaert.episodeWatcher.service.EpisodeRuntime;
import eu.vranckaert.episodeWatcher.utils.DateUtil;
import roboguice.activity.GuiceActivity;

//import android.widget.ImageButton;

public class RandomEpPickerActivity extends GuiceActivity {
    private Episode random;
    private static final String LOG_TAG = RandomEpPickerActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Preferences.getPreferenceInt(this, PreferencesKeys.THEME_KEY) == 0 ? android.R.style.Theme_Material_Light : android.R.style.Theme_Material);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.randompicker);

        TextView showNameText = findViewById(R.id.episodeDetShowName);
        TextView episodeNameText = findViewById(R.id.episodeDetName);
        TextView seasonText = findViewById(R.id.episodeDetSeason);
        TextView episodeText = findViewById(R.id.episodeDetEpisode);
        TextView airdateText = findViewById(R.id.episodeDetAirdate);
        Button markAsSeenButton = findViewById(R.id.markAsSeenButton);

        if (EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_TO_WATCH) > 0) {
            random = EpisodesController.getInstance().getRandomWatchEpisode();

            showNameText.setText(random.getShowName());
            episodeNameText.setText(random.getName());
            seasonText.setText(" " + random.getSeasonString());
            episodeText.setText(" " + random.getEpisodeString());
            //runtimeText.setText(random.get);

            //Air date in specifc format
            Date airdate = random.getAirDate();
            String formattedAirDate;
            if (airdate != null) {
                formattedAirDate = DateUtil.formatDateLong(airdate);
            } else {
                formattedAirDate = getText(R.string.episodeDetailsAirDateLabelDateNotFound).toString();
            }

            airdateText.setText(" " + formattedAirDate);
            TextView aboutWebsite = findViewById(R.id.tvMazeWebsite);
            if (!TextUtils.isEmpty(random.getTVMazeWebSite())) {
                //aboutWebsite.setText(episode.getTVMazeWebSite());
                AppDatabase database = Room.databaseBuilder(eu.vranckaert.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                        .allowMainThreadQueries()   //Allows room to do operation on main thread
                        .fallbackToDestructiveMigration()
                        .build();

                SeriesDAO seriesDAO = database.getSeriesDAO();
                EpisodeRuntime showRuntime = seriesDAO.getEpisodeRuntimeWithMyEpsId(random.getMyEpisodeID());


                //create hashmap's to prevent build fails, they get replaced
                HashMap episodeSummaryHashMap = new HashMap<String, String>() {{
                    put("a", "b");
                }};
                HashMap showSummaryHashMap = new HashMap<String, String>() {{
                    put("a", "b");
                }};


                new RandomEpPickerActivity.downloadShowSummary(showSummaryHashMap).execute(showRuntime.getShowTVMazeID());
               new RandomEpPickerActivity.downloadEpisodeSummary(episodeSummaryHashMap).execute(showRuntime.getShowTVMazeID(), random.getSeasonString(), random.getEpisodeString());


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



        markAsSeenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeAndMarkWatched(random);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.show_management_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.closePreferences:
                finish();
                return true;
            case R.id.home:
                finish();
                return true;

        }
        return false;
    }


    private void closeAndMarkWatched(Episode episode) {
        finish();

        Intent episodeListingActivity = new Intent(this.getApplicationContext(), EpisodeListingActivity.class);
        episodeListingActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode)
                .putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_MARK_EPISODE, ActivityConstants.EXTRA_BUNDLE_VALUE_WATCH)
                .putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episode.getType())
                .putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_SHOW);
        startActivity(episodeListingActivity);
    }


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

                    Toast.makeText(RandomEpPickerActivity.this, "episode not found via API", Toast.LENGTH_LONG).show();
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

            AppDatabase database = Room.databaseBuilder(eu.vranckaert.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
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

            if (!ShowName.equals("") && !showURL.equals("") && !officialSite.equals("") && !showSummary.equals("") && !showImageURL.equals("")) {

                showSummaryHash.put("ShowName", ShowName);
                showSummaryHash.put("showURL", showURL);
                showSummaryHash.put("officialSite", officialSite);
                showSummaryHash.put("showSummary", showSummary);
                showSummaryHash.put("showImageURL", showImageURL);
                showSummaryHash.put("ShowRuntime", ShowRuntime);

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
            return showSummaryHash;
        }

        /*
            onPostExecute(Result result)
                Runs on the UI thread after doInBackground(Params...).
         */
        protected void onPostExecute(HashMap<String, String> result) {
            //episodeSummaryHash = result;

            TextView ShowName = findViewById(R.id.ShowName);
            ShowName.setText(showSummaryHash.get("ShowName"));

            TextView ShowRuntime = findViewById(R.id.episodeRuntime);
            ShowRuntime.setText(showSummaryHash.get("ShowRuntime") + " mins");

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


            //add the info into the show database to limit the need to api call the info all the time for static show info
            AppDatabase database = Room.databaseBuilder(eu.vranckaert.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                    .allowMainThreadQueries()   //Allows room to do operation on main thread
                    .fallbackToDestructiveMigration()
                    .build();

            SeriesDAO seriesDAO = database.getSeriesDAO();

            EpisodeRuntime showSummaryInfo = seriesDAO.getEpisodeRuntimeWithMyEpsId(random.getMyEpisodeID());


            //Inserting an episodeRuntime adding the info that was not collected during the runtime. addition

            showSummaryInfo.setShowSummary(showSummaryHash.get("showSummary"));
            showSummaryInfo.setShowURL(showSummaryHash.get("showURL"));
            showSummaryInfo.setOfficialSite(showSummaryHash.get("officialSite"));
            showSummaryInfo.setShowImageURL(showSummaryHash.get("showImageURL"));

            //  Log.d("epsRunTime: ", epsRunTime.toString());

            seriesDAO.update(showSummaryInfo);

        }
    }





    public void onHomeClick(View v) {
        exit();
    }

    private void exit() {
        finish();
    }
}
