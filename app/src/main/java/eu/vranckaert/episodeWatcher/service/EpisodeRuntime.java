package eu.vranckaert.episodeWatcher.service;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;


@Entity(tableName = "EpisodeRuntime")
public class EpisodeRuntime {
    private String showName;
    private String showRuntime;
    private String showTVMazeID;
    private String showSummary;
    private String showImageURL;
    private String officialSite;
    private String showURL;

    @PrimaryKey
    @NonNull
    public String showMyEpsID = "";

    public String getShowName() {
        return showName;
    }

    public void setShowName(String showName) {
        this.showName = showName;
    }

    @NonNull
    public String getShowMyEpsID() {
        return showMyEpsID;
    }

    public void setshowMyepsID(@NonNull String showMyEpsID) {
        this.showMyEpsID = showMyEpsID;
    }

    public String getShowTVMazeID() {
        return showTVMazeID;
    }

    public void setShowTVMazeID(String showTVMazeID) {
        this.showTVMazeID = showTVMazeID;
    }

    public String getShowRuntime() {
        return showRuntime;
    }

    public void setShowRuntime(String showRuntime) {
        this.showRuntime = showRuntime;
    }


    public String getShowSummary() {
        return showSummary;
    }

    public void setShowSummary(String showSummary) {
        this.showSummary = showSummary;
    }

    public String getShowImageURL() {
        return showImageURL;
    }

    public void setShowImageURL(String showImageURL) {
        this.showImageURL = showImageURL;
    }

    public String getOfficialSite() {
        return officialSite;
    }

    public void setOfficialSite(String officialSite) {
        this.officialSite = officialSite;
    }

    public String getShowURL() {
        return showURL;
    }

    public void setShowURL(String showURL) {
        this.showURL = showURL;
    }




}