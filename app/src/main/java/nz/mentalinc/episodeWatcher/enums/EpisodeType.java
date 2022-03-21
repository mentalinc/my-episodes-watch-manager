package nz.mentalinc.episodeWatcher.enums;

import java.util.ArrayList;
import java.util.List;

/**
 * User: DIRK VRANCKAERT
 * Date: Dec 2, 2010
 * Time: 7:09:15 PM
 */
public enum EpisodeType {
    EPISODES_TO_WATCH(0),
    EPISODES_TO_ACQUIRE(1),
    EPISODES_COMING(2),
    EPISODES_TO_YESTERDAY1(3),
    EPISODES_TO_YESTERDAY2(4),
    WATCH_BY_SHOW(5),
    ACQUIRE_BY_SHOW(6),
    COMING_BY_SHOW(7);


    EpisodeType(int type) {
        this.episodeListingType = type;
    }

    private final int episodeListingType;

    private int getEpisodeListingType() {
        return episodeListingType;
    }

    public static List<Integer> getEpisodeListingTypeList() {
        List<Integer> episodeTypes = new ArrayList<>();
        episodeTypes.add(EPISODES_TO_WATCH.getEpisodeListingType());
        episodeTypes.add(EPISODES_TO_ACQUIRE.getEpisodeListingType());
        episodeTypes.add(EPISODES_COMING.getEpisodeListingType());
        episodeTypes.add(WATCH_BY_SHOW.getEpisodeListingType());
        episodeTypes.add(ACQUIRE_BY_SHOW.getEpisodeListingType());
        episodeTypes.add(COMING_BY_SHOW.getEpisodeListingType());
        return episodeTypes;
    }

    public static CharSequence[] getEpisodeListingTypeArray() {
        return new CharSequence[]{
                String.valueOf(EPISODES_TO_WATCH.getEpisodeListingType()),
                String.valueOf(EPISODES_TO_ACQUIRE.getEpisodeListingType()),
                String.valueOf(EPISODES_COMING.getEpisodeListingType()),
                String.valueOf(WATCH_BY_SHOW.getEpisodeListingType()),
                String.valueOf(ACQUIRE_BY_SHOW.getEpisodeListingType()),
                String.valueOf(COMING_BY_SHOW.getEpisodeListingType())
        };
    }
}
