package nz.mentalinc.episodeWatcher.activities;


import android.annotation.SuppressLint;
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
import androidx.room.Room;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import nz.mentalinc.episodeWatcher.service.ItemClickSupport;

public class ShowListingActivity extends Activity {

    private static final String LOG_TAG = ShowListingActivity.class.getSimpleName();
    List<Show> shows = new ArrayList<>();
    private List<Episode> episodes = new ArrayList<>();
    private static EpisodeType episodesType;


    private BottomNavigationView bottomNavigationView;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycle_view_shows);

        Bundle data = this.getIntent().getExtras();
        //episodeType is set based on the button on the home page that is press.
        episodesType = (EpisodeType) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE);

        com.google.android.material.appbar.MaterialToolbar episodeTypeTitle = findViewById(R.id.topAppBarShowsView);
        if (episodesType.toString().equals("EPISODES_TO_WATCH")) {
            episodeTypeTitle.setTitle("Watch");
        }
        if (episodesType.toString().equals("EPISODES_TO_ACQUIRE")) {
            episodeTypeTitle.setTitle("Acquire");
        }
        if (episodesType.toString().equals("EPISODES_COMING")) {
            episodeTypeTitle.setTitle("Coming");
        }

        //todo - this is commented out when using the newer show hash approach.
        episodes = EpisodesController.getInstance().getEpisodes(episodesType);

        int countEpisodes = EpisodesController.getInstance().getEpisodesCount(episodesType);

        bottomNavigationView = findViewById(R.id.bottom_navigationRecyclerShow);
        bottomNavigationView.setOnItemSelectedListener(navigationItemSelectedListener);

        if (countEpisodes == 200) {

            @SuppressLint("CutPasteId") // this is to stop and error - the below is meant to be used so can pop up the snackbar when 200 shows are found.
            Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarShowsView), R.string.watchListFull, Snackbar.LENGTH_LONG);
            snackbar.setAnchorView(bottomNavigationView);
            snackbar.show();
        }



        if (episodesType.equals(EpisodeType.EPISODES_TO_ACQUIRE)) {
            bottomNavigationView.getMenu().getItem(2).setChecked(true);
         //   returnEpisodesShowHash(EpisodesController.getInstance().getEpisodesShows(EpisodeType.ACQUIRE_BY_SHOW));
        } else if (episodesType.equals(EpisodeType.EPISODES_TO_WATCH)) {
            bottomNavigationView.getMenu().getItem(1).setChecked(true);
          //  returnEpisodesShowHash(EpisodesController.getInstance().getEpisodesShows(EpisodeType.WATCH_BY_SHOW));

        } else if (episodesType.equals(EpisodeType.EPISODES_COMING)) {
            bottomNavigationView.getMenu().getItem(3).setChecked(true);
          //  returnEpisodesShowHash(EpisodesController.getInstance().getEpisodesShows(EpisodeType.COMING_BY_SHOW));
        }

        //DISABLED TO TEST NEW HASHMAP which is in the returnEpisodesShowHash in the if else if else if above. Hashmap has lots of issues removing for now
        returnEpisodes();



        ShowAdapter adapter = new ShowAdapter(shows);
        adapter.submitList(shows);
        adapter.notifyItemInserted(0);

        // Attach the adapter to the recyclerview to populate items
        RecyclerView rvShows = findViewById(R.id.recyclerViewListItemsShows);
        rvShows.setAdapter(adapter);
        // Set layout manager to position the items
        rvShows.setLayoutManager(new LinearLayoutManager(this));
        rvShows.setHasFixedSize(true);

        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {
            Log.w(LOG_TAG, "Home button clicked.");
            finish();
        });

        androidx.appcompat.view.menu.ActionMenuItemView appBarRefresh = findViewById(R.id.btn_title_refresh);
        appBarRefresh.setOnClickListener(v -> {
            Log.w(LOG_TAG, "Refresh button clicked.");
            //finish();
            //TODO Need to move all the code to download the info gain
        });


        //todo add LONG click listener to popup the options to either mark as watched or acquired, details etc (per existing screnshopws.

        // Leveraging ItemClickSupport decorator to handle clicks on items in our recyclerView

        ItemClickSupport.addTo(rvShows).setOnItemClickListener((recyclerView, position, v) -> {
                    // do stuff
                    Show showSelected = shows.get(position);
                    //to go straight to details if there is only 1 episode - saves clicking through the list when it would only show one anyway.
                    Episode nextEpisodeToWatch = showSelected.getFirstEpisode();

                    if (showSelected.getNumberEpisodes() == 1) {
                        openEpisodeDetails(nextEpisodeToWatch, episodesType);
                    } else {
                        ShowSummaryActivity(showSelected, episodesType);
                    }
                }
        );
    }

    /* This is only here to test the old view of showing all the episodes for an app instead grouping them via the show overview view now.
    private void openEpisodeListing(Show show, EpisodeType episodeType) {
        finish();
        Intent updatedEpisodeListActivity = new Intent(this.getApplicationContext(), UpdatedEpisodeListingActivity.class);

        Episode nextEpisodeToWatch = show.getFirstEpisode();
        String myepisodeID = nextEpisodeToWatch.getMyEpisodeID();


        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, myepisodeID);
        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        updatedEpisodeListActivity.putExtra("Title", show.getShowName());
        startActivity(updatedEpisodeListActivity);
    }
    */

    private BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            switch (item.getItemId()) {

                case R.id.barHome:
                    finish();
                    bottomNavigationView.getMenu().getItem(0).setChecked(true);
                    Intent HomeActivity = new Intent(getApplicationContext(), HomeActivity.class);
                    HomeActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    HomeActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_WATCH);
                    startActivity(HomeActivity);
                    return true;

                case R.id.barWatch:
                    finish();
                    Log.w(LOG_TAG, "barWatch selected");
                    Intent newWatchShowListing = new Intent(getApplicationContext(), ShowListingActivity.class);
                    newWatchShowListing.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    newWatchShowListing.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_WATCH);
                    startActivity(newWatchShowListing);

                    return true;
                case R.id.barAcquire:
                    finish();
                    Log.w(LOG_TAG, "barAcquire selected");
                    Intent newAcquireShowListing = new Intent(getApplicationContext(), ShowListingActivity.class);
                    newAcquireShowListing.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    newAcquireShowListing.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_ACQUIRE);
                    startActivity(newAcquireShowListing);

                    return true;
                case R.id.barComing:
                    finish();
                    Log.w(LOG_TAG, "barComing selected");
                    Intent newComingShowListing = new Intent(getApplicationContext(), ShowListingActivity.class);
                    newComingShowListing.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    newComingShowListing.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_COMING);
                    startActivity(newComingShowListing);

                    return true;
            }
            return false;
        }


    };



    //this is the new view.
    private void ShowSummaryActivity(Show show, EpisodeType episodeType) {
        // todo decide if i want home to be the very first load page, or the show button. I think it want to leave it as is so returns to the episode type last looked at
        //finish();

        Intent ShowSummaryActivity = new Intent(this.getApplicationContext(), ShowSummaryActivity.class);

        Episode nextEpisodeToWatch = show.getFirstEpisode();
        String myEpisodeID = nextEpisodeToWatch.getMyEpisodeID();
        ShowSummaryActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, nextEpisodeToWatch);
        ShowSummaryActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, myEpisodeID);
        ShowSummaryActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        ShowSummaryActivity.putExtra("Title", show.getShowName());
        Log.w(LOG_TAG, "openShowHomePage method called");
        startActivity(ShowSummaryActivity);
    }

    private void openEpisodeDetails(Episode episode, EpisodeType episodeType) {

        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), EpisodeDetailsActivity.class);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode)
                .putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra("Title", episode.getShowName());
        startActivity(episodeDetailsSubActivity);
    }

    private void returnEpisodes() {
        //shows = new ArrayList<>();
        if (episodes != null && episodes.size() > 0) {
            for (Episode ep : episodes) {
                AddEpisodeToShow(ep);
            }
        } else {
            Log.d(LOG_TAG, "Episode can't be added to show.");
        }

        //shows don't have runtime added yet?
        sortShows(shows);
        sortEpisodesOfShows(shows);

    }


    private void returnEpisodesShowHash(HashMap<String, Show> hashMap) {

        //TODO - try to add the runtime to the show here? so show has runime early (OR add when first created in the episode service?)
        hashMap.size();
        hashMap.forEach( (k,v) -> shows.add(v));

        //shows don't have runtime added yet?
        sortShows(shows);
        sortEpisodesOfShows(shows);

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

            AppDatabase database = Room.databaseBuilder(nz.mentalinc.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                    .allowMainThreadQueries()   //Allows room to do operation on main thread
                    .fallbackToDestructiveMigration()
                    .build();

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
            database.close();

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

    private Show CheckShowDuplicate(String episodeName) {
        for (Show show : shows) {
            if (show.getShowName().equals(episodeName)) {
                return show;
            }
        }
        return null;
    }

}
