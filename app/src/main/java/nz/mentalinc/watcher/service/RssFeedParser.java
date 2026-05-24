package nz.mentalinc.watcher.service;

import java.net.URL;

import nz.mentalinc.watcher.domain.Feed;
import nz.mentalinc.watcher.enums.EpisodeType;


interface RssFeedParser {
    Feed parseFeed(EpisodeType episodesType, final URL url) throws Exception;

    Feed parseFeed(final URL url) throws Exception;
}
