package nz.mentalinc.episodeWatcher.activities;

import static org.acra.ACRA.LOG_TAG;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.constants.ActivityConstants;
import nz.mentalinc.episodeWatcher.controllers.EpisodesController;
import nz.mentalinc.episodeWatcher.domain.Episode;
import nz.mentalinc.episodeWatcher.domain.EpisodeAscendingComparator;
import nz.mentalinc.episodeWatcher.domain.EpisodeDescendingComparator;
import nz.mentalinc.episodeWatcher.domain.Show;
import nz.mentalinc.episodeWatcher.domain.ShowAscendingComparator;
import nz.mentalinc.episodeWatcher.domain.ShowDescendingComparator;
import nz.mentalinc.episodeWatcher.enums.EpisodeType;
import nz.mentalinc.episodeWatcher.service.ItemClickSupport;

public class ShowListingActivity extends Activity {

    List<Show> shows;
    private List<Episode> episodes = new ArrayList<>();
    private static EpisodeType episodesType;


    protected void onCreate(Bundle savedInstanceState) {

        //TODO get the episodes to watch add some sort of if statement or something here depending episodes to show
        episodesType = EpisodeType.EPISODES_TO_WATCH;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycle_view_shows);

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

        RecyclerView rvShows = (RecyclerView) findViewById(R.id.recyclerViewListItems);

        // Initialize Show fake data
        //TODO this is where where the ACTUAL data needs to be added from the .xml files.

        episodes = EpisodesController.getInstance().getEpisodes(episodesType);
        returnEpisodes();

        ShowAdapter adapter = new ShowAdapter(shows);
        adapter.submitList(shows);
        adapter.notifyItemInserted(0);
        // Attach the adapter to the recyclerview to populate items
        rvShows.setAdapter(adapter);
        // Set layout manager to position the items
        rvShows.setLayoutManager(new LinearLayoutManager(this));
        rvShows.setHasFixedSize(true);

        androidx.appcompat.view.menu.ActionMenuItemView appBarHome =  findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {
            Log.w(LOG_TAG, "Home button clicked.");
            finish();
        });



        // Leveraging ItemClickSupport decorator to handle clicks on items in our recyclerView
        ItemClickSupport.addTo(rvShows).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                        // do stuff


                        Show showSelected =  shows.get(position);
                        Episode nextEpisodeToWatch = showSelected.getFirstEpisode();


                        openEpisodeDetails(nextEpisodeToWatch, episodesType );

                        //Show testEpisode = (Show) adapter.getItemId(position);
                       // Snackbar snackbar = Snackbar.make(findViewById(R.id.recyclerViewListItems),"Postition: " + position +" Show: " + showSelected.getShowName(),Snackbar.LENGTH_LONG);
                       // snackbar.show();
                    }
                }
        );


    }

    private void openEpisodeDetails(Episode episode, EpisodeType episodeType) {
        finish();

        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), EpisodeDetailsActivity.class);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode)
                .putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra("Title", "New Show click");
        startActivity(episodeDetailsSubActivity);
    }

    private void returnEpisodes() {
        shows = new ArrayList<>();
        if (episodes != null && episodes.size() > 0) {
            for (Episode ep : episodes) {
                AddEpisodeToShow(ep);
            }
        } else {
            Log.d(LOG_TAG, "Episode can't be added to show.");
        }

        sortShows(shows);
        sortEpisodesOfShows(shows);

    }


    private void sortEpisodesOfShows(List<Show> showList) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        // String sorting = Preferences.getPreference(this, PreferencesKeys.EPISODE_SORTING_KEY);
        String sorting = sharedPref.getString("episodeOrder","oldest_on_top");

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

        Show currentShow = CheckShowDublicate(episode.getShowName());

        if (currentShow == null) {
            Show tempShow = new Show(episode.getShowName());
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
                sorting =  sharedPref.getString("showWatchOrder","show_myepisodes_default_sort");//Preferences.getPreference(this, PreferencesKeys.WATCH_SHOW_SORTING_KEY);
                break;
            case EPISODES_TO_YESTERDAY1:
            case EPISODES_TO_YESTERDAY2:
            case EPISODES_TO_ACQUIRE:
                sorting = sharedPref.getString("showAcquireOrder","show_myepisodes_default_sort"); //Preferences.getPreference(this, PreferencesKeys.ACQUIRE_SHOW_SORTING_KEY);
                break;
            case EPISODES_COMING:
                sorting = sharedPref.getString("showComingOrder","show_myepisodes_default_sort"); //Preferences.getPreference(this, PreferencesKeys.COMING_SHOW_SORTING_KEY);
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
        }
    }

    private Show CheckShowDublicate(String episodename) {
        for (Show show : shows) {
            if (show.getShowName().equals(episodename)) {
                return show;
            }
        }
        return null;
    }

}
