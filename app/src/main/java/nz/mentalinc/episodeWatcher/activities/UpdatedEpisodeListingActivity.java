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

    List<Show> shows;

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
        } else if (episodesType.equals(EpisodeType.EPISODES_TO_WATCH)) {
            bottomNavigationView.getMenu().getItem(2).setChecked(true);

        } else if (episodesType.equals(EpisodeType.EPISODES_COMING)) {
            bottomNavigationView.getMenu().getItem(4).setChecked(true);
        }

        returnEpisodes();

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
        title = title + " (" + episodes.size()+ ")";
        ShowNameTitle.setTitle(title);


        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {
            Log.w(LOG_TAG, "Home button clicked.");
            finish();

        });

        // Leveraging ItemClickSupport decorator to handle clicks on items in our recyclerView
        ItemClickSupport.addTo(rvEpisode).setOnItemClickListener((recyclerView, position, v) -> {
                    // do stuff

                    Episode episodeSelected = episodes.get(position);
                    //Show Show = episodeSelected.getShowName();

                    //TODO in here link to the to be built SHOW screen. So it opens on the next episode detail to watch
                    // then gives tabs to look at what needs to be acquire, and coming to the right, and left of the detail
                    //it gives a show overview.
                    // which also means episode details tab can have all the show info removed as will be to the right
                    //also need to create a view just like the show one for episodes.


                    openEpisodeDetails(episodeSelected, episodesType);

                    //Show testEpisode = (Show) adapter.getItemId(position);
                    // Snackbar snackbar = Snackbar.make(findViewById(R.id.recyclerViewListItems),"Postition: " + position +" Show: " + showSelected.getShowName(),Snackbar.LENGTH_LONG);
                    // snackbar.show();
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

          //  episodesRaw.clear();
          //  episodes.clear();

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
    };


    private void openEpisodeListing(Show show, EpisodeType episodeType) {
        finish();
        Intent updatedEpisodeListActivity = new Intent(this.getApplicationContext(), UpdatedEpisodeListingActivity.class);
        updatedEpisodeListActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        Episode nextEpisodeToWatch = show.getFirstEpisode();
        String myepisodeID = nextEpisodeToWatch.getMyEpisodeID();

        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, myepisodeID);
        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        updatedEpisodeListActivity.putExtra("Title", show.getShowName());
        startActivity(updatedEpisodeListActivity);
    }


    private void openEpisodeDetails(Episode episode, EpisodeType episodeType) {

        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), EpisodeDetailsActivity.class);
        episodeDetailsSubActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode)
                .putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra("Title", episode.getShowName());
        startActivity(episodeDetailsSubActivity);
    }

    private void returnEpisodes() {

        //ideally this just grabs the data from the show somehow, loop is slow with lots of data
        episodesRaw = EpisodesController.getInstance().getEpisodes(episodesType);
        shows = new ArrayList<>();
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
