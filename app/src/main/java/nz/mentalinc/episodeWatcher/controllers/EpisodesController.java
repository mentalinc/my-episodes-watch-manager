package nz.mentalinc.episodeWatcher.controllers;

import android.util.Log;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nz.mentalinc.episodeWatcher.database.AppDatabase;
import nz.mentalinc.episodeWatcher.domain.Episode;
import nz.mentalinc.episodeWatcher.domain.Show;
import nz.mentalinc.episodeWatcher.enums.EpisodeType;

public class EpisodesController {
    private List<Episode> watchEpisodes = new ArrayList<>();
    private List<Episode> acquireEpisodes = new ArrayList<>();
    private List<Episode> comingEpisodes = new ArrayList<>();
    private ArrayList<Show> shows;
    private final HashMap<String, Show> watchShows = new HashMap<>();
    private final HashMap<String, Show> acquireShows = new HashMap<>();
    private final HashMap<String, Show> comingShows = new HashMap<>();
    private static EpisodesController Instance;
    private static final String LOG_TAG = EpisodesController.class.getSimpleName();

    public List<Episode> getEpisodes(EpisodeType episodesType) {
        switch (episodesType) {
            case EPISODES_TO_WATCH:
                return watchEpisodes;
            case EPISODES_TO_ACQUIRE:
            case EPISODES_TO_YESTERDAY1:
            case EPISODES_TO_YESTERDAY2:
                return acquireEpisodes;
            case EPISODES_COMING:
                return comingEpisodes;

            default:
                return null;
        }
    }


    public HashMap<String, Show> getEpisodesShows(EpisodeType episodesType) {
        switch (episodesType) {
            case WATCH_BY_SHOW:
                return watchShows;
            case ACQUIRE_BY_SHOW:
                return acquireShows;
            case COMING_BY_SHOW:
                return comingShows;

            default:
                return null;
        }
    }

    public List<Episode> getShowTypeEpisodes(EpisodeType episodesType, String myEpisodeID) {
        switch (episodesType) {
            case WATCH_BY_SHOW: {
                Show show = watchShows.get(myEpisodeID);
                return show != null ? show.getEpisodes() : null;
            }
            case ACQUIRE_BY_SHOW: {
                Show show = acquireShows.get(myEpisodeID);
                return show != null ? show.getEpisodes() : null;
            }
            case COMING_BY_SHOW: {
                Show show = comingShows.get(myEpisodeID);
                return show != null ? show.getEpisodes() : null;
            }
            default:
                return null;
        }
    }

    public int getEpisodesCount(EpisodeType episodesType) {
        switch (episodesType) {
            case EPISODES_TO_WATCH:
                return watchEpisodes.size();
            case EPISODES_TO_ACQUIRE:
            case EPISODES_TO_YESTERDAY1:
            case EPISODES_TO_YESTERDAY2:
                return acquireEpisodes.size();
            case EPISODES_COMING:
                return comingEpisodes.size();
            default:
                return 0;
        }
    }

    public int getShowsCount() {
        if (shows != null)
            return shows.size();

        return 0;
    }

    public int getShowCountType(EpisodeType episodesType) {
        switch (episodesType) {
            case EPISODES_TO_WATCH:
                return watchShows.size();
            case EPISODES_TO_ACQUIRE:
                return acquireShows.size();
            case EPISODES_COMING:
                return comingShows.size();

            default:
                return 0;
        }
    }


    public int getEpisodesCount(EpisodeType episodesType, String myEpisodeID) {
        switch (episodesType) {
            case WATCH_BY_SHOW:
                if (watchShows.get(myEpisodeID) != null)
                    return watchShows.get(myEpisodeID).getEpisodes().size();
                else
                    return 0;
            case ACQUIRE_BY_SHOW:
                if (acquireShows.get(myEpisodeID) != null)
                    return acquireShows.get(myEpisodeID).getEpisodes().size();
                else
                    return 0;
            case COMING_BY_SHOW:
                if (comingShows.get(myEpisodeID) != null)
                    return comingShows.get(myEpisodeID).getEpisodes().size();
                else
                    return 0;
            default:
                return 0;
        }
    }

    public void setEpisodes(EpisodeType episodesType, List<Episode> episodes) {
        switch (episodesType) {
            case EPISODES_TO_WATCH:
                watchEpisodes = episodes;
                break;
            case EPISODES_TO_ACQUIRE:
            case EPISODES_TO_YESTERDAY1:
            case EPISODES_TO_YESTERDAY2:
                acquireEpisodes = episodes;
                break;
            case EPISODES_COMING:
                comingEpisodes = episodes;
                break;
            default:
                break;
        }
    }

    public void addEpisodes(EpisodeType episodesType, List<Episode> episodes) {
        switch (episodesType) {
            case EPISODES_TO_WATCH:
                watchEpisodes.addAll(episodes);
                break;
            case EPISODES_TO_ACQUIRE:
            case EPISODES_TO_YESTERDAY1:
            case EPISODES_TO_YESTERDAY2:
                acquireEpisodes.addAll(episodes);
                break;
            case EPISODES_COMING:
                comingEpisodes.addAll(episodes);
                break;
            default:
                break;
        }
    }

    public void deleteEpisode(EpisodeType episodesType, Episode episode) {
        switch (episodesType) {
            case EPISODES_TO_WATCH:
                for (int i = watchEpisodes.size() - 1; i >= 0; i--) {
                    if (watchEpisodes.get(i).toString().equals(episode.toString())) {
                        watchEpisodes.remove(i);
                        break;
                    }
                }
                break;
            case EPISODES_TO_ACQUIRE:
            case EPISODES_TO_YESTERDAY1:
            case EPISODES_TO_YESTERDAY2:
                for (int i = acquireEpisodes.size() - 1; i >= 0; i--) {
                    if (acquireEpisodes.get(i).toString().equals(episode.toString())) {
                        acquireEpisodes.remove(i);
                        break;
                    }
                }
                break;
            case EPISODES_COMING:
                for (int i = comingEpisodes.size() - 1; i >= 0; i--) {
                    if (comingEpisodes.get(i).toString().equals(episode.toString())) {
                        comingEpisodes.remove(i);
                        break;
                    }
                }
                break;
            default:
                break;
        }
    }

    public void deleteShowEpisode(EpisodeType episodesType, Episode episode) {
        switch (episodesType) {
            case EPISODES_TO_WATCH: {
                Show tempShow = watchShows.get(episode.getMyEpisodeID());
                if (tempShow != null) {
                    List<Episode> episodesListing = tempShow.getEpisodes();
                    for (int i = episodesListing.size() - 1; i >= 0; i--) {
                        if (episodesListing.get(i).toString().equals(episode.toString())) {
                            episodesListing.remove(i);
                            break;
                        }
                    }
                }
                break;
            }
            case EPISODES_TO_ACQUIRE:
            case EPISODES_TO_YESTERDAY1:
            case EPISODES_TO_YESTERDAY2: {
                Show tempShow = acquireShows.get(episode.getMyEpisodeID());
                if (tempShow != null) {
                    List<Episode> episodesListing = tempShow.getEpisodes();
                    for (int i = episodesListing.size() - 1; i >= 0; i--) {
                        if (episodesListing.get(i).toString().equals(episode.toString())) {
                            episodesListing.remove(i);
                            break;
                        }
                    }
                }
                break;
            }
            case EPISODES_COMING: {
                Show tempShow = comingShows.get(episode.getMyEpisodeID());
                if (tempShow != null) {
                    List<Episode> episodesListing = tempShow.getEpisodes();
                    for (int i = episodesListing.size() - 1; i >= 0; i--) {
                        if (episodesListing.get(i).toString().equals(episode.toString())) {
                            episodesListing.remove(i);
                            break;
                        }
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    public boolean areListsEmpty() {
        if (watchEpisodes.isEmpty()) {
            if (acquireEpisodes.isEmpty()) {
                return comingEpisodes.isEmpty();
            }
        }
        return false;
    }

    public List<Show> getRandomWatchEpisodeShowList() {
        shows = new ArrayList<>();
        if (watchEpisodes != null && watchEpisodes.size() > 0) {
            for (Episode ep : watchEpisodes) {
                AddEpisodeToShow(ep);
            }
        }
        SecureRandom r = new SecureRandom();
        int randint = r.nextInt(shows.size());
        List<Show> randomShow = new ArrayList<>();
        randomShow.add(shows.get(randint));
        return randomShow;

    }

    public Episode getRandomWatchEpisode() {
        shows = new ArrayList<>();
        if (watchEpisodes != null && watchEpisodes.size() > 0) {
            for (Episode ep : watchEpisodes) {
                AddEpisodeToShow(ep);
            }
        }
        SecureRandom r = new SecureRandom();
        int randint = r.nextInt(shows.size());
        return shows.get(randint).getFirstEpisode();
    }

    public void deleteAll() {
        List<Episode> tempList = new ArrayList<>();
        watchEpisodes = tempList;
        acquireEpisodes = tempList;
        comingEpisodes = tempList;
        //shows.clear();
        watchShows.clear();
        acquireShows.clear();
        comingShows.clear();
        AppDatabase database = AppDatabase.getInstance(nz.mentalinc.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext());
        database.clearAllTables();
    }

    public void addEpisode(EpisodeType episodesType, Episode episode) {
        switch (episodesType) {
            case EPISODES_TO_WATCH:
                watchEpisodes.add(episode);
                break;
            case EPISODES_TO_ACQUIRE:
            case EPISODES_TO_YESTERDAY1:
            case EPISODES_TO_YESTERDAY2:
                acquireEpisodes.add(episode);
                break;
            case EPISODES_COMING:
                comingEpisodes.add(episode);
                break;
            default:
                break;
        }
    }

    public static EpisodesController getInstance() {
        if (Instance == null)
            Instance = new EpisodesController();
        return Instance;
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

    public void AddToWatchShow(List<Episode> episodeList) {
        //pass the full episode type list to then split into all the shows.
        //need to get the array list and loop the episodes putting each episode in the array into its own show, then once have all the shows created, then add them finally to the ArrayMap using the myepisodeid as the key

        for (Episode eps : episodeList) {

            //check if show is in the array map if yes return from the ArrayMap, add the episode then update the ArrayMap with new.
            String myEpID = eps.getMyEpisodeID();
            //current show for the if statement below maybe?

            Show showTemp = watchShows.get(myEpID); //may need to test for null to determine if in the arraymayp
            if (showTemp != null) { //use code below
                //Add episode to the show then add back to the the array,/update
                showTemp.addEpisode(eps);
                watchShows.replace(myEpID, showTemp);
            } else {
                Show showNew = new Show(eps.getShowName(), eps.getMyEpisodeID());
                showNew.addEpisode(eps);
                watchShows.put(myEpID, showNew);
            }
        } //end for loop

        Log.w(LOG_TAG, "AddToWatchShow method completed.");
    }


    public void AddToAcquireShow(List<Episode> episodeList) { //pass the full episode type list to then split into all the shows.

        // TODO Consider passing episode type and making same method work for all types but get working for watch first
        //need to get the array list and loop the episodes putting each episode in the array into its own show, then once have all the shows created, then add them finally to the ArrayMap using the myepisodeid as the key

        for (Episode eps : episodeList) {

            //check if show is in the array map if yes return from the ArrayMap, add the episode then update the ArrayMap with new.
            String myEpID = eps.getMyEpisodeID();
            //current show for the if statement below maybe?

            Show showTemp = acquireShows.get(myEpID); //may need to test for null to determine if in the arraymayp
            if (showTemp != null) { //use code below
                //Add episode to the show then add back to the the array,/update
                showTemp.addEpisode(eps);
                acquireShows.replace(myEpID, showTemp);
            } else {
                Show showNew = new Show(eps.getShowName(), eps.getMyEpisodeID());
                showNew.addEpisode(eps);
                acquireShows.put(myEpID, showNew);

            }
        } //end for loop

        Log.w(LOG_TAG, "AddToWatchShow method completed.");
    }


    public void AddToComingShow(List<Episode> episodeList) { //pass the full episode type list to then split into all the shows.

        // TODO Consider passing episode type and making same method work for all types but get working for watch first
        //need to get the array list and loop the episodes putting each episode in the array into its own show, then once have all the shows created, then add them finally to the ArrayMap using the myepisodeid as the key

        for (Episode eps : episodeList) {

            //check if show is in the array map if yes return from the ArrayMap, add the episode then update the ArrayMap with new.
            String myEpID = eps.getMyEpisodeID();
            //current show for the if statement below maybe?

            Show showTemp = comingShows.get(myEpID); //may need to test for null to determine if in the arraymayp
            if (showTemp != null) { //use code below
                //Add episode to the show then add back to the the array,/update
                showTemp.addEpisode(eps);
                comingShows.replace(myEpID, showTemp);
            } else {
                Show showNew = new Show(eps.getShowName(), eps.getMyEpisodeID());
                showNew.addEpisode(eps);
                comingShows.put(myEpID, showNew);

            }
        } //end for loop

        Log.w(LOG_TAG, "AddToWatchShow method completed.");
    }

}
