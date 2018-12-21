package eu.vranckaert.episodeWatcher.domain;

import java.util.Comparator;

public class ShowRuntimeAscendingComparator implements Comparator<Show>{
    @Override
    public int compare(Show o1, Show o2) {
        return o1.toString().compareTo(o2.toString());
    }
}


