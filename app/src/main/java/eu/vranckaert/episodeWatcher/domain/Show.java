package eu.vranckaert.episodeWatcher.domain;

import java.util.ArrayList;
import java.util.List;

public class Show {
    private final List<Episode> episodes = new ArrayList<>(0);
    private String showName;
    private String myEpisodeID;
    private String runTime;
    private String showTVMazeID;
    private String TVMazeWebSite;

    public Show(String showName) {
        this.showName = showName;
    }

    public Show(String showName, String myEpisodeID) {
        this.showName = showName;
        this.myEpisodeID = myEpisodeID;
    }

    public Show(String showName, String showRuntime, String myEpisodeID) {
        this.showName = showName;
        this.runTime = showRuntime;
        this.myEpisodeID = myEpisodeID;
    }

    /**
     * Gets the show name.
     *
     * @return The show name.
     */
    public String getShowName() {
        return showName;
    }

    /**
     * Set the show name.
     *
     * @param showName The show name.
     */
    public void setShowName(String showName) {
        this.showName = showName;
    }

    /**
     * Gets the show runTime.
     *
     * @return The show runTime.
     */
    public String getRunTime() {
        return runTime;
    }

    /**
     * Sets the show runTime.
     *
     * @param runTime The show runTime.
     */
    public void setRunTime(String runTime) {
        this.runTime = runTime;
    }

    public void addEpisode(Episode episode) {
        episodes.add(episode);
    }

    public List<Episode> getEpisodes() {
        return episodes;
    }

    public Episode getFirstEpisode() {
        return episodes.get(0);
    }

    public int getNumberEpisodes() {
        return episodes.size();
    }

    public String getMyEpisodeID() {
        return myEpisodeID;
    }

    public void setMyEpisodeID(String myEpisodeID) {
        this.myEpisodeID = myEpisodeID;
    }


    public String getTVMazeWebSite() {
        return TVMazeWebSite;
    }

    public void setTVMazeWebSite(String tVMazeWebSite) {
        TVMazeWebSite = tVMazeWebSite;
    }

    @Override
    public String toString() {
        return this.runTime + " mins - " + this.showName;
    }
}
