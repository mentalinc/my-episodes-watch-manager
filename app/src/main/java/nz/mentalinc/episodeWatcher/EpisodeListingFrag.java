package nz.mentalinc.episodeWatcher;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import nz.mentalinc.episodeWatcher.activities.EpisodeAdapter;
import nz.mentalinc.episodeWatcher.activities.EpisodeDetailsActivity;
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


public class EpisodeListingFrag extends Fragment {
    List<Show> shows;
    private static final String LOG_TAG = EpisodeListingFrag.class.getSimpleName();
    private List<Episode> episodesRaw = new ArrayList<>();
    private List<Episode> episodes = new ArrayList<>();
    private static EpisodeType episodesType;
    private String showMyEpisodeID;
    RecyclerView rvEpisode;

    public EpisodeListingFrag() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recycle_view_episodes, container, false);
        return rootView;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        episodesRaw = new ArrayList<>();
        episodes = new ArrayList<>();

        episodesType = (EpisodeType) getArguments().getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE);
        showMyEpisodeID = getArguments().getString(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID);
        String ShowTitleHeader = getArguments().getString("Title");

        /*tabLayout = (TabLayout) getActivity().findViewById(R.id.tabLayoutShowHome);


        Log.w(LOG_TAG, "Tab selected position: " + tabLayout.getSelectedTabPosition());
   /*     TabLayout.Tab tab = tabLayout.getTabAt(1);
        Log.w(LOG_TAG, "Tab selected - " + tab.getText());
        tab.select();*/


        ShowTitleHeader = ShowTitleHeader + " (" + episodes.size() + ")";
        com.google.android.material.appbar.MaterialToolbar ShowNameTitle = (com.google.android.material.appbar.MaterialToolbar) view.findViewById(R.id.topAppBarEpisodesView);
        ShowNameTitle.setTitle(ShowTitleHeader);

        rvEpisode = (RecyclerView) view.findViewById(R.id.recyclerViewListItemsEps);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        rvEpisode.setLayoutManager(layoutManager);
        EpisodeAdapter adapter = new EpisodeAdapter(episodes);
        adapter.submitList(episodes);

        //this doesn't seem to be called each time the tab is clicked on.
        adapter.notifyItemInserted(0);
        //adapter.notifyDataSetChanged();
        //rvEpisode.setLayoutManager(new LinearLayoutManager(this.getContext()));
        // Attach the adapter to the recyclerview to populate items
        rvEpisode.setAdapter(adapter);


        // Leveraging ItemClickSupport decorator to handle clicks on items in our recyclerView
        ItemClickSupport.addTo(rvEpisode).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
                                                                     @Override
                                                                     public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                                                                         // do stuff

                                                                         Log.w(LOG_TAG, "Recycle view area clicked.");
                                                                         Episode episodeSelected = episodes.get(position);
                                                                         //Show Show = episodeSelected.getShowName();

                                                                         openEpisodeDetails(episodeSelected, episodesType);

                                                                         //Show testEpisode = (Show) adapter.getItemId(position);
                                                                         // Snackbar snackbar = Snackbar.make(findViewById(R.id.recyclerViewListItems),"Postition: " + position +" Show: " + showSelected.getShowName(),Snackbar.LENGTH_LONG);
                                                                         // snackbar.show();
                                                                     }
                                                                 }
        );

        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = getActivity().findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {
            Log.w(LOG_TAG, "Home button clicked.");
            //not sure how to stop the view yet.
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

    }

    private void openEpisodeDetails(Episode episode, EpisodeType episodeType) {
        Intent episodeDetailsSubActivity = new Intent(super.getActivity(), EpisodeDetailsActivity.class);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode)
                .putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra("Title", episode.getShowName());
        startActivity(episodeDetailsSubActivity);
    }


    private void returnEpisodes() {

        //ideally this just grabs the data from the show somehow, loop is slow with lots of data
        episodesRaw = EpisodesController.getInstance().getEpisodes(episodesType);
        shows = new ArrayList<>();

        String ShowTitle = "";
        if (episodesRaw != null && episodesRaw.size() > 0) {
            for (Episode ep : episodesRaw) {
                if (ep.getMyEpisodeID().equals(showMyEpisodeID)) {
                    //add to the episode List to show next
                    episodes.add(ep);
                    ShowTitle = ep.getShowName();
                }
            }
        } else {
            Log.d(LOG_TAG, "Episode can't be added to show.");
        }

        Log.d(LOG_TAG, "Episodes type being added: " + episodesType);

        sortShows(shows);
        sortEpisodesOfShows(shows);

   /*     com.google.android.material.appbar.MaterialToolbar ShowNameTitle = (com.google.android.material.appbar.MaterialToolbar)  super.getActivity().findViewById(R.id.toolbarShowHome);
        ShowNameTitle.setTitle(ShowTitle);*/

    }

    private void sortEpisodesOfShows(List<Show> showList) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(super.getActivity());
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


    private void sortShows(List<Show> showList) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(super.getContext());
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
