package nz.mentalinc.episodeWatcher.service;

import java.net.URL;

import nz.mentalinc.episodeWatcher.domain.Feed;
import nz.mentalinc.episodeWatcher.enums.EpisodeType;


interface RssFeedParser {
    Feed parseFeed(EpisodeType episodesType, final URL url) throws Exception;

    Feed parseFeed(final URL url) throws Exception;
}
