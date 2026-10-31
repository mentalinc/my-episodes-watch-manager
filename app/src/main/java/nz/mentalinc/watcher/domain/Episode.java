package nz.mentalinc.watcher.domain;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

import nz.mentalinc.watcher.enums.EpisodeType;

public class Episode implements Parcelable {
    private String showName;
    private String name;
    private int season;
    private int episode;
    private long airDateMillis;
    private String myEpisodeID;
    private String TVMazeWebSite;
    private EpisodeType type;

    public Episode() {
    }

    protected Episode(Parcel in) {
        showName = in.readString();
        name = in.readString();
        season = in.readInt();
        episode = in.readInt();
        airDateMillis = in.readLong();
        myEpisodeID = in.readString();
        TVMazeWebSite = in.readString();
        type = EpisodeType.valueOf(in.readString());
    }

    public static final Creator<Episode> CREATOR = new Creator<Episode>() {
        @Override
        public Episode createFromParcel(Parcel in) {
            return new Episode(in);
        }

        @Override
        public Episode[] newArray(int size) {
            return new Episode[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(showName);
        dest.writeString(name);
        dest.writeInt(season);
        dest.writeInt(episode);
        dest.writeLong(airDateMillis);
        dest.writeString(myEpisodeID);
        dest.writeString(TVMazeWebSite);
        dest.writeString(type.name());
    }

    public String getShowName() {
        return showName;
    }

    public void setShowName(String showName) {
        this.showName = showName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSeason() {
        return season;
    }

    public String getSeasonString() {
        if (season < 10) {
            return "0" + season;
        } else {
            return "" + season;
        }
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public int getEpisode() {
        return episode;
    }

    public String getEpisodeString() {
        if (episode < 10) {
            return "0" + episode;
        } else {
            return "" + episode;
        }
    }

    public void setEpisode(int episode) {
        this.episode = episode;
    }

    public long getAirDate() {
        return airDateMillis;
    }

    public void setAirDate(long airDateMillis) {
        this.airDateMillis = airDateMillis;
    }

    public String getMyEpisodeID() {
        return myEpisodeID;
    }

    public void setMyEpisodeID(String myEpisodeID) {
        this.myEpisodeID = myEpisodeID;
    }

    public EpisodeType getType() {
        return type;
    }

    public void setType(EpisodeType type) {
        this.type = type;
    }

    public String getTVMazeWebSite() {
        return TVMazeWebSite;
    }

    public void setTVMazeWebSite(String tVMazeWebSite) {
        TVMazeWebSite = tVMazeWebSite;
    }

    @Override
    public String toString() {
        return showName + " S" + getSeasonString() + "E" + getEpisodeString() + " - " + name + " (" + myEpisodeID + ") (" + new Date(airDateMillis) + ")";
    }
}
