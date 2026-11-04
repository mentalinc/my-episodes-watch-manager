package nz.mentalinc.watcher.domain;

import java.util.Comparator;

/**
 * @author Dirk Vranckaert , maintained and updated by mentalinc
 */
public class EpisodeDescendingComparator implements Comparator<Episode> {
    @Override
    public int compare(Episode o1, Episode o2) {
        long a1 = o1.getAirDate();
        long a2 = o2.getAirDate();
        if (a1 != a2) {
            return Long.compare(a2, a1);
        }
        int s1 = o1.getSeason();
        int e1 = o1.getEpisode();
        int s2 = o2.getSeason();
        int e2 = o2.getEpisode();
        if (s1 != s2) {
            return Integer.compare(s2, s1);
        }
        return Integer.compare(e2, e1);
    }
}
