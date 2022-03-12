package nz.mentalinc.episodeWatcher;



import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import nz.mentalinc.episodeWatcher.activities.ShowDetailAdapter;
import nz.mentalinc.episodeWatcher.activities.ShowHomeTabActivity;
import nz.mentalinc.episodeWatcher.constants.ActivityConstants;
import nz.mentalinc.episodeWatcher.controllers.EpisodesController;
import nz.mentalinc.episodeWatcher.domain.Episode;
import nz.mentalinc.episodeWatcher.domain.EpisodeAscendingComparator;
import nz.mentalinc.episodeWatcher.domain.EpisodeDescendingComparator;
import nz.mentalinc.episodeWatcher.domain.Show;
import nz.mentalinc.episodeWatcher.domain.ShowAscendingComparator;
import nz.mentalinc.episodeWatcher.domain.ShowDescendingComparator;
import nz.mentalinc.episodeWatcher.enums.EpisodeType;


public class ShowDetailFrag extends Fragment {

    List<Show> shows;
    private static final String LOG_TAG = ShowDetailFrag.class.getSimpleName();
    private List<Episode> episodesRaw = new ArrayList<>();
    private List<Episode> episodes = new ArrayList<>();

    private static EpisodeType episodesType;
    private String showMyEpisodeID;


    public ShowDetailFrag() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recycle_view_show_detail, container, false);

        episodesType = (EpisodeType) getArguments().getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE);
        showMyEpisodeID = getArguments().getString(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID);

        String ShowTitleHeader = getArguments().getString("Title");
        com.google.android.material.appbar.MaterialToolbar ShowNameTitle = (com.google.android.material.appbar.MaterialToolbar)  super.getActivity().findViewById(R.id.toolbarShowHome);
        ShowNameTitle.setTitle(ShowTitleHeader);

        returnEpisodes();

        RecyclerView rvShowDetail = this.getActivity().findViewById(R.id.recyclerViewListItemsEps);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        rvShowDetail.setLayoutManager(layoutManager);
        ShowDetailAdapter adapter= new ShowDetailAdapter(shows);
        adapter.submitList(shows);

        //this doesn't seem to be called each time the tab is clicked on.
        //adapter.notifyItemInserted(0);
        adapter.notifyDataSetChanged();
        //rvEpisode.setLayoutManager(new LinearLayoutManager(this.getContext()));
        // Attach the adapter to the recyclerview to populate items
        rvShowDetail.setAdapter(adapter);




        return rootView;
    }

    public static ShowDetailFrag newInstance(String param1, String param2) {
        ShowDetailFrag fragment = new ShowDetailFrag();
        /*Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);*/
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
     /*   if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }*/
    }


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
    //public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment

        super.onViewCreated(view, savedInstanceState);

       // return inflater.inflate(R.layout.fragment_show_detail, container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

    }

    private void returnEpisodes() {

        //ideally this just grabs the data from the show somehow, loop is slow with lots of data
        episodesRaw = EpisodesController.getInstance().getEpisodes(episodesType);
        shows = new ArrayList<>();

      //  String ShowTitle = "";
        if (episodesRaw != null && episodesRaw.size() > 0) {
            for (Episode ep : episodesRaw) {
                if (ep.getMyEpisodeID().equals(showMyEpisodeID)) {
                    //add to the episode List to show next
                  //  episodes.add(ep);
     //               ShowTitle = ep.getShowName();
                    AddEpisodeToShow(ep);
                }
            }
        } else {
            Log.d(LOG_TAG, "Episode can't be added to show.");
        }

        Log.d(LOG_TAG, "Episodes type being added: " + episodesType);

        //no point sorting shows when this is only for the one show - unless i change the data model latter.
      /*  sortShows(shows);
        sortEpisodesOfShows(shows);*/

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

    private Show CheckShowDublicate(String episodename) {
        for (Show show : shows) {
            if (show.getShowName().equals(episodename)) {
                return show;
            }
        }
        return null;
    }
}