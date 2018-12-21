package eu.vranckaert.episodeWatcher.service;

import java.net.URL;

import eu.vranckaert.episodeWatcher.domain.Feed;
import eu.vranckaert.episodeWatcher.enums.EpisodeType;


interface RssFeedParser {
    Feed parseFeed(EpisodeType episodesType, final URL url) throws Exception;

    Feed parseFeed(final URL url) throws Exception;
}
