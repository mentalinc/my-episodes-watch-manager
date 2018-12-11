package eu.vranckaert.episodeWatcher.domain;

import java.util.ArrayList;
import java.util.List;

public class Feed {
    private final List<FeedItem> items = new ArrayList<>(0);

    public List<FeedItem> getItems() {
        return items;
    }

    public void addItem(FeedItem item) {
        items.add(item);
    }
}
