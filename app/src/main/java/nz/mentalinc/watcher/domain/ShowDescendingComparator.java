package nz.mentalinc.watcher.domain;

import java.util.Comparator;

/**
 * @author Dirk Vranckaert, maintained and updated by mentalinc
 */
public class ShowDescendingComparator implements Comparator<Show> {
    @Override
    public int compare(Show o1, Show o2) {
        return o2.getShowName().compareTo(o1.getShowName());
    }
}
