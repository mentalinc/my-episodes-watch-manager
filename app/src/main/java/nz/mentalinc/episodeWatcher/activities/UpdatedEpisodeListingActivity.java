package nz.mentalinc.episodeWatcher.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.constants.ActivityConstants;
import nz.mentalinc.episodeWatcher.controllers.EpisodesController;
import nz.mentalinc.episodeWatcher.domain.Episode;
import nz.mentalinc.episodeWatcher.domain.EpisodeAscendingComparator;
import nz.mentalinc.episodeWatcher.domain.EpisodeDescendingComparator;
import nz.mentalinc.episodeWatcher.domain.Show;
import nz.mentalinc.episodeWatcher.enums.EpisodeType;
import nz.mentalinc.episodeWatcher.service.ItemClickSupport;

public class UpdatedEpisodeListingActivity extends Activity {
    private static final String LOG_TAG = UpdatedEpisodeListingActivity.class.getSimpleName();

    List<Show> shows = new ArrayList<>();

    private List<Episode> episodesRaw = new ArrayList<>();
    private List<Episode> episodes = new ArrayList<>();
    private static EpisodeType episodesType;
    private String showMyEpisodeID;
    private BottomNavigationView bottomNavigationView;
    Bundle data;
    EpisodeAdapter adapter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //todo add the refesh icon to the screen and then the methods to do that
        data = this.getIntent().getExtras();
        episodesType = (EpisodeType) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE);
        showMyEpisodeID = (String) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID);
        String title = data.getString("Title");

        setContentView(R.layout.recycle_view_episodes);

        bottomNavigationView = findViewById(R.id.bottom_navigationviewShowHome);
        bottomNavigationView.setOnItemSelectedListener(navigationItemSelectedListener);

        if (episodesType.equals(EpisodeType.EPISODES_TO_ACQUIRE)) {
            bottomNavigationView.getMenu().getItem(3).setChecked(true);
            // episodes = EpisodesController.getInstance().getShowTypeEpisodes(EpisodeType.ACQUIRE_BY_SHOW,showMyEpisodeID);
        } else if (episodesType.equals(EpisodeType.EPISODES_TO_WATCH)) {
            bottomNavigationView.getMenu().getItem(2).setChecked(true);
            //  episodes = EpisodesController.getInstance().getShowTypeEpisodes(EpisodeType.WATCH_BY_SHOW,showMyEpisodeID);

        } else if (episodesType.equals(EpisodeType.EPISODES_COMING)) {
            bottomNavigationView.getMenu().getItem(4).setChecked(true);
            //  episodes = EpisodesController.getInstance().getShowTypeEpisodes(EpisodeType.COMING_BY_SHOW,showMyEpisodeID);
        }

        returnEpisodes();

       /* if (shows.size() < 1) {
            Show holderShow = new Show(title, showMyEpisodeID);
            shows.add(holderShow);
        }*/

        RecyclerView rvEpisode = findViewById(R.id.recyclerViewListItems);
        adapter = new EpisodeAdapter(episodes);
        adapter.submitList(episodes);
        adapter.notifyItemInserted(0);
        // Attach the adapter to the recyclerview to populate items
        rvEpisode.setAdapter(adapter);
        // Set layout manager to position the items
        //rvEpisode.setLayoutManager(new LinearLayoutManager(this));
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rvEpisode.setLayoutManager(linearLayoutManager);
        rvEpisode.setHasFixedSize(true);

        com.google.android.material.appbar.MaterialToolbar ShowNameTitle = findViewById(R.id.topAppBarEpisodesView);
        title = title + " (" + episodes.size() + ")";
        ShowNameTitle.setTitle(title);


        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {
            Log.w(LOG_TAG, "Home button clicked.");
            finish();

        });

        // Leveraging ItemClickSupport decorator to handle clicks on items in our recyclerView
        ItemClickSupport.addTo(rvEpisode).setOnItemClickListener((recyclerView, position, v) -> {
                    Episode episodeSelected = episodes.get(position);
                    //Show Show = episodeSelected.getShowName();
                    openEpisodeDetails(episodeSelected, episodesType);
                }
        );


        ItemClickSupport.addTo(rvEpisode).setOnItemLongClickListener((recyclerView, position, v) -> {
                    //Episode episodeSelected = episodes.get(position);
                    //openEpisodeDetails(episodeSelected, episodesType);

                    Log.w(LOG_TAG, "Long clicked on an episode - pop up the marked watch material box.");

                    return true;
                }
        );
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

            final int previousItem = bottomNavigationView.getSelectedItemId();
            final int nextItem = item.getItemId();

            //TODO need to do something to open a blank list instead of failing back when clicking on a button that has no shows to watch.
            if (previousItem != nextItem) {
                switch (nextItem) {
                    case R.id.barShowDetail:
                        Log.w(LOG_TAG, "barShowDetail selected");
                        if (shows.size() > 0) {
                            //issue where is the epiosdetype doesn't have any episode then this will fail as show.size=0.
                            //need to figure out how to keep the show value current?
                            openShowSummary(shows.get(0).getFirstEpisode(), episodesType);
                        }

                        //TODO need to build an activity to use the showDetail content.
                        if (episodes.size() > 0) {
                            Log.w(LOG_TAG, "barShowDetail Executing openShowSummary");
                            openShowSummary(episodes.get(0), episodesType);
                        }
                        return true;
                    case R.id.barEpisodeOverview:
                        Log.w(LOG_TAG, "barEpisodeOverview selected");
                        if (shows.size() > 0) {
                            //make sure it opens the next WATCH episode details.
                            episodesType = EpisodeType.EPISODES_TO_WATCH;
                            returnEpisodes();
                            //returnEpisodesShowHash(EpisodesController.getInstance().getEpisodesShows(EpisodeType.WATCH_BY_SHOW));
                            if (shows.size() > 0) {
                                openEpisodeDetails(shows.get(0).getFirstEpisode(), episodesType);
                            }
                        }
                        return true;
                    case R.id.barWatch:
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
                    case R.id.barAcquire:
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
                    case R.id.barComing:
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
                }
                return false;
            }
            return false;
        }
    };

    private void returnEpisodesShowHash(HashMap<String, Show> hashMap) {

        //TODO - try to add the runtime to the show here? so show has runime early (OR add when first created in the episode service?)
        hashMap.size();
        hashMap.forEach((k, v) -> shows.add(v));

        //shows don't have runtime added yet?
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
}
