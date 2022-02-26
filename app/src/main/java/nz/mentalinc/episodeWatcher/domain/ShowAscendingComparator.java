package nz.mentalinc.episodeWatcher.domain;

import java.util.Comparator;

/**
 * @author Dirk Vranckaert, maintained and updated by mentalinc
 *
 */
public class ShowAscendingComparator implements Comparator<Show> {
    @Override
    public int compare(Show o1, Show o2) {
        return o1.getShowName().compareTo(o2.getShowName());
    }
}
