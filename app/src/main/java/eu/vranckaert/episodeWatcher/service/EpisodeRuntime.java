package eu.vranckaert.episodeWatcher.service;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;


@Entity(tableName = "EpisodeRuntime")
public class EpisodeRuntime {
    public String showName;
    public String showRuntime;
    public String showTVMazeID;

    @PrimaryKey
    @NonNull
    public String showMyEpsID;

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

}