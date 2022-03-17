package nz.mentalinc.episodeWatcher.activities;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.constants.ActivityConstants;
import nz.mentalinc.episodeWatcher.controllers.EpisodesController;
import nz.mentalinc.episodeWatcher.domain.Episode;
import nz.mentalinc.episodeWatcher.domain.EpisodeAscendingComparator;
import nz.mentalinc.episodeWatcher.domain.EpisodeDescendingComparator;
import nz.mentalinc.episodeWatcher.domain.Show;
import nz.mentalinc.episodeWatcher.enums.EpisodeType;

public class ShowHomeTabActivity extends AppCompatActivity {

    private static final String LOG_TAG = ShowHomeTabActivity.class.getSimpleName();

    Bundle data;

    List<Show> shows;
    private List<Episode> episodesRaw = new ArrayList<>();
    private List<Episode> episodes = new ArrayList<>();

    private static EpisodeType episodesType;
    private String showMyEpisodeID;

    // String tabNames[] = {"Show Overview","Episode Summary","Episodes to Watch","Episodes to Acquire","Episodes Coming"};
    String menus[] = {"Show Overview", "Episodes to Watch", "Episodes to Acquire", "Episodes Coming"};


    // String[] tabNames = {"Episodes to Watch","Episodes to Acquire","Episodes Coming"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_home_tab);
        data = this.getIntent().getExtras();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigationview);
        bottomNavigationView.setOnItemSelectedListener(navigationItemSelectedListener);

        episodesType = (EpisodeType) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE);
        showMyEpisodeID = (String) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID);
        String title = data.getString("Title");
        returnEpisodes();

        //  Title = Title + " (" + episodesRaw.size() + ")";
        com.google.android.material.appbar.MaterialToolbar ShowNameTitle = findViewById(R.id.topAppBarShowHomeTab);
        title = title + " (" + episodes.size()+ ")";
        ShowNameTitle.setTitle(title);

        Bundle BundleInfoShowDetail = new Bundle();
        BundleInfoShowDetail.putSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodesType);
        BundleInfoShowDetail.putString(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, showMyEpisodeID);
        BundleInfoShowDetail.putString("Title", title);

        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {
            Log.w(LOG_TAG, "Home button clicked.");
            finish();
        });
    }

    private void openEpisodeListing(Show show, EpisodeType episodeType) {

        Intent updatedEpisodeListActivity = new Intent(this.getApplicationContext(), UpdatedEpisodeListingActivity.class);
        updatedEpisodeListActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        Episode nextEpisodeToWatch = show.getFirstEpisode();
        String myepisodeID = nextEpisodeToWatch.getMyEpisodeID();

        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, myepisodeID);
        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        updatedEpisodeListActivity.putExtra("Title", show.getShowName());
        startActivity(updatedEpisodeListActivity);
    }


    private void returnEpisodes() {
        //ideally this just grabs the data from the show somehow, loop is slow with lots of data
        episodesRaw = EpisodesController.getInstance().getEpisodes(episodesType);
        shows = new ArrayList<>();

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


    private BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            episodesType = (EpisodeType) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE);
            showMyEpisodeID = (String) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID);
            String Title = data.getString("Title");

            Bundle BundleInfoShowDetail = new Bundle();
            BundleInfoShowDetail.putSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodesType);
            BundleInfoShowDetail.putString(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, showMyEpisodeID);
            BundleInfoShowDetail.putString("Title", Title);


            switch (item.getItemId()) {
                case R.id.barShowDetail:
                    Log.w(LOG_TAG, "barShowDetail selected");
                    //TODO need to build an activity to use the showDetail content.

                    return true;
                case R.id.barEpisodeOverview:
                    Log.w(LOG_TAG, "barEpisodeOverview selected");
                    if (shows.size() > 0) {
                        openEpisodeDetails(shows.get(0).getFirstEpisode(), episodesType);
                    }
                    return true;
                case R.id.barWatch:
                    Log.w(LOG_TAG, "barWatch selected");
                    if (shows.size() > 0) {
                        openEpisodeListing(shows.get(0), EpisodeType.EPISODES_TO_WATCH);
                    }
                    return true;
                case R.id.barAcquire:
                    Log.w(LOG_TAG, "barAcquire selected");
                    if (shows.size() > 0) {
                        openEpisodeListing(shows.get(0), EpisodeType.EPISODES_TO_ACQUIRE);

                    }
                    return true;
                case R.id.barComing:
                    Log.w(LOG_TAG, "barComing selected");
                    if (shows.size() > 0) {
                        openEpisodeListing(shows.get(0), EpisodeType.EPISODES_COMING);
                    }
                    return true;
            }
            return false;

        }

        ;
    };

    private void openEpisodeDetails(Episode episode, EpisodeType episodeType) {
        finish();
        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), EpisodeDetailsActivity.class);
        episodeDetailsSubActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode)
                .putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra("Title", episode.getShowName());
        startActivity(episodeDetailsSubActivity);
    }

    private void AddEpisodeToShow(Episode episode) {

        Show currentShow = CheckShowDuplicate(episode.getShowName());

        if (currentShow == null) {
            Show tempShow = new Show(episode.getShowName(),episode.getMyEpisodeID());
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