package nz.mentalinc.episodeWatcher;



import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import nz.mentalinc.episodeWatcher.activities.ShowDetailAdapter;
import nz.mentalinc.episodeWatcher.constants.ActivityConstants;
import nz.mentalinc.episodeWatcher.controllers.EpisodesController;
import nz.mentalinc.episodeWatcher.domain.Episode;
import nz.mentalinc.episodeWatcher.domain.Show;
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
        Log.d(LOG_TAG, "onCreateView called" );
        return rootView;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        episodesType = (EpisodeType) getArguments().getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE);
        showMyEpisodeID = getArguments().getString(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID);

        String ShowTitleHeader = getArguments().getString("Title");
        com.google.android.material.appbar.MaterialToolbar ShowNameTitle = (com.google.android.material.appbar.MaterialToolbar)  view.findViewById(R.id.topAppBarShowsDetailView);
        ShowNameTitle.setTitle(ShowTitleHeader);

        returnEpisodes();

        RecyclerView rvShowDetail = (RecyclerView) view.findViewById(R.id.recyclerViewListShowItems);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        rvShowDetail.setLayoutManager(layoutManager);
        ShowDetailAdapter adapter= new ShowDetailAdapter(shows);
        adapter.submitList(shows);

        //this doesn't seem to be called each time the tab is clicked on.
        adapter.notifyItemInserted(0);
        //adapter.notifyDataSetChanged();
        //rvEpisode.setLayoutManager(new LinearLayoutManager(this.getContext()));
        // Attach the adapter to the recyclerview to populate items
        rvShowDetail.setAdapter(adapter);

        Log.d(LOG_TAG, "onViewCreated called" );

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