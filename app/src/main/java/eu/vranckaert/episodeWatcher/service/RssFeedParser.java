package eu.vranckaert.episodeWatcher.service;

import java.net.URL;

import eu.vranckaert.episodeWatcher.domain.Feed;
import eu.vranckaert.episodeWatcher.enums.EpisodeType;
import eu.vranckaert.episodeWatcher.exception.FeedUrlParsingException;
import eu.vranckaert.episodeWatcher.exception.InternetConnectivityException;
import eu.vranckaert.episodeWatcher.exception.RssFeedParserException;


interface RssFeedParser {
    Feed parseFeed(EpisodeType episodesType, final URL url) throws Exception;
   Feed parseFeed(final URL url) throws Exception;
}
