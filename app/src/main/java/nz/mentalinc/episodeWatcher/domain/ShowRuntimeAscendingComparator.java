package nz.mentalinc.episodeWatcher.domain;

import java.util.Comparator;

public class ShowRuntimeAscendingComparator implements Comparator<Show> {
    @Override
    public int compare(Show o1, Show o2) {

      /*  Episode o1ep = o1.getFirstEpisode();
        Episode o2Ep = o1.getFirstEpisode();
        o1ep.getMyEpisodeID();
        o2Ep.getMyEpisodeID();

       */

        //todo add in the work to get runtime from room database


        return o1.toString().compareTo(o2.toString());
    }
}


