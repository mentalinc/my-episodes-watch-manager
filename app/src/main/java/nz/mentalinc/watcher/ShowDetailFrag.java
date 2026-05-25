package nz.mentalinc.watcher;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nz.mentalinc.watcher.activities.ShowDetailAdapter;
import nz.mentalinc.watcher.constants.ActivityConstants;
import nz.mentalinc.watcher.controllers.EpisodesController;
import nz.mentalinc.watcher.database.AppDatabase;
import nz.mentalinc.watcher.domain.Episode;
import nz.mentalinc.watcher.domain.Show;
import nz.mentalinc.watcher.enums.EpisodeType;
import nz.mentalinc.watcher.service.EpisodeRuntime;
import nz.mentalinc.watcher.utils.TaskRunner;


public class ShowDetailFrag extends Fragment {

    List<Show> shows;
    private static final String LOG_TAG = ShowDetailFrag.class.getSimpleName();
    private List<Episode> episodesRaw = new ArrayList<>();
    private final List<Episode> episodes = new ArrayList<>();

    private static EpisodeType episodesType;
    private String showMyEpisodeID;


    public ShowDetailFrag() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recycle_view_show_detail, container, false);
        Log.d(LOG_TAG, "onCreateView called");
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
        com.google.android.material.appbar.MaterialToolbar ShowNameTitle = view.findViewById(R.id.topAppBarShowsDetailView);
        ShowNameTitle.setTitle(ShowTitleHeader);

        returnEpisodes();

        RecyclerView rvShowDetail = view.findViewById(R.id.recyclerViewListShowItems);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        rvShowDetail.setLayoutManager(layoutManager);
        ShowDetailAdapter adapter = new ShowDetailAdapter(shows, new HashMap<>());
        adapter.submitList(shows);
        rvShowDetail.setAdapter(adapter);

        TaskRunner.getExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            Map<String, EpisodeRuntime> map = new HashMap<>();
            for (Show show : shows) {
                if (show.getFirstEpisode() != null) {
                    EpisodeRuntime rt = db.getSeriesDAO().getEpisodeRuntimeWithMyEpsId(show.getFirstEpisode().getMyEpisodeID());
                    if (rt != null) {
                        map.put(rt.getShowMyEpsID(), rt);
                    }
                }
            }
            getActivity().runOnUiThread(() -> {
                ShowDetailAdapter newAdapter = new ShowDetailAdapter(shows, map);
                rvShowDetail.setAdapter(newAdapter);
                newAdapter.submitList(shows);
            });
        });

        Log.d(LOG_TAG, "onViewCreated called");

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