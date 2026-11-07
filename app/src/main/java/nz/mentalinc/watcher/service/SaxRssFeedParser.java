package nz.mentalinc.watcher.service;

import android.text.Html;
import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import nz.mentalinc.watcher.constants.MyEpisodeConstants;
import nz.mentalinc.watcher.domain.Feed;
import nz.mentalinc.watcher.utils.CacheUtils;
import nz.mentalinc.watcher.domain.FeedItem;
import nz.mentalinc.watcher.enums.EpisodeType;
import nz.mentalinc.watcher.exception.FeedUrlParsingException;
import nz.mentalinc.watcher.exception.InternetConnectivityException;
import nz.mentalinc.watcher.exception.RssFeedParserException;
import nz.mentalinc.watcher.http.HttpClientProvider;

public class SaxRssFeedParser extends DefaultHandler implements RssFeedParser {
    private static final String LOG_TAG = SaxRssFeedParser.class.getSimpleName();
    private static final String UTF_8 = "UTF-8";

    private boolean inItem = false;
    private boolean inDescription = true;
    private StringBuilder nodeValue = null;
    private final Feed feed = new Feed();
    private FeedItem item = null;

    public Feed parseFeed(final URL url) {
        //replaced by parseFeed(EpisodeType episodesType, final URL)
        Log.e(LOG_TAG, "This will no longer be called. Replaced by parseFeed(EpisodeType episodesType, final URL url)");
        return feed;
    }


    public Feed parseFeed(EpisodeType episodesType, final URL url) throws ParserConfigurationException, SAXException, FeedUrlParsingException, RssFeedParserException, InternetConnectivityException {
        InputStream inputStream;

        Log.d(LOG_TAG, "episodesType being Parsed: " + episodesType);

        try {

            if (MyEpisodeConstants.CACHE_EPISODES_ENABLED) {
                Log.d(LOG_TAG, "Cache is enabled, read from disk");
                switch (episodesType) {
                    case EPISODES_TO_WATCH:
                        if (MyEpisodeConstants.DAYS_BACK_ENABLED) {
                            Log.d(LOG_TAG, "MyEpisodeConstants.EXTENDED_EPISODES_XML:  " + MyEpisodeConstants.EXTENDED_EPISODES_XML);
                            inputStream = new ByteArrayInputStream(xmlBytes(MyEpisodeConstants.EXTENDED_EPISODES_XML));
                        } else {
                            inputStream = openCacheFile("Watch.xml");
                            if (inputStream == null) {
                                Log.d(LOG_TAG, "No cached Watch.xml file found. Downloading...");
                                inputStream = openFeed(url);
                                String content = convertStreamToString(inputStream, UTF_8);
                                FileOutputStream fos = MyEpisodeConstants.CONTEXT.openFileOutput("Watch.xml", 0);
                                fos.write(content.getBytes(StandardCharsets.UTF_8));
                                fos.close();
                                Log.d(LOG_TAG, "Watch.xml saved to disk");
                                inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
                            }
                        }
                        break;
                    case EPISODES_TO_YESTERDAY1:
                        inputStream = openFeed(url);
                        break;
                    case EPISODES_TO_YESTERDAY2:
                        inputStream = openFeed(url);
                        break;
                    case EPISODES_TO_ACQUIRE:
                        if (MyEpisodeConstants.DAYS_BACK_ENABLED) {
                            Log.d(LOG_TAG, "MyEpisodeConstants.EXTENDED_EPISODES_XML_ACQUIRE:  " + MyEpisodeConstants.EXTENDED_EPISODES_XML_ACQUIRE);
                            inputStream = new ByteArrayInputStream(xmlBytes(MyEpisodeConstants.EXTENDED_EPISODES_XML_ACQUIRE));
                        } else {
                            inputStream = openCacheFile("Acquire.xml");
                            if (inputStream == null) {
                                Log.d(LOG_TAG, "No cached Acquire.xml file found. Downloading...");
                                InputStream downloadStream = openFeed(url);
                                String content = convertStreamToString(downloadStream, UTF_8);
                                FileOutputStream fos = MyEpisodeConstants.CONTEXT.openFileOutput("Acquire.xml", 0);
                                fos.write(content.getBytes(StandardCharsets.UTF_8));
                                fos.close();
                                Log.d(LOG_TAG, "Acquire.xml saved to disk");
                                inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
                            }
                        }
                        break;
                    case EPISODES_COMING:
                        inputStream = openCacheFile("Coming.xml");
                        if (inputStream == null) {
                            Log.d(LOG_TAG, "No cached Coming.xml file found. Downloading...");
                            InputStream downloadStream = openFeed(url);
                            String content = convertStreamToString(downloadStream, UTF_8);
                            FileOutputStream fos = MyEpisodeConstants.CONTEXT.openFileOutput("Coming.xml", 0);
                            fos.write(content.getBytes(StandardCharsets.UTF_8));
                            fos.close();
                            Log.d(LOG_TAG, "Coming.xml saved to disk");
                            inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
                        }
                        break;

                    default:
                        //this should not be used
                        Log.d(LOG_TAG, "Default switch statement being called for ep type: " + episodesType);
                        inputStream = openFeed(url);
                }


            } else {
                //Cache is not enabled using standard RSS feeds
                if (url.toString().substring(0, 16).equalsIgnoreCase("http://127.0.0.1")) {
                    inputStream = switch (episodesType) {
                        case EPISODES_TO_WATCH ->
                                new ByteArrayInputStream(xmlBytes(MyEpisodeConstants.EXTENDED_EPISODES_XML));
                        case EPISODES_TO_ACQUIRE ->
                                new ByteArrayInputStream(xmlBytes(MyEpisodeConstants.EXTENDED_EPISODES_XML_ACQUIRE));
                        default ->
                                new ByteArrayInputStream(xmlBytes(MyEpisodeConstants.EXTENDED_EPISODES_XML));
                    };
                } else {
                    Log.d(LOG_TAG, "Cache is disabled, download from Internet RSS Feeds");
                    inputStream = openFeed(url);
                }

                Log.w(LOG_TAG, "check inputStream");
            }

        } catch (UnknownHostException e) {
            String message = "Could not connect to host.";
            Log.e(LOG_TAG, message, e);
            throw new InternetConnectivityException(message, e);
        } catch (IOException e) {
            String message = "Could not parse the URL for the feed.";
            Log.e(LOG_TAG, message, e);
            throw new FeedUrlParsingException(message, e);
        }

        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        // Harden the parser against XXE (external entity) attacks. Feeds are untrusted input.
        // Android's built-in Expat parser only honours features in the http://xml.org/sax/features/
        // namespace; the Apache-specific features and XInclude are not supported and only produced
        // noisy "feature not supported" log lines, so they are intentionally omitted. External
        // entities are disabled by the parser by default.
        setFeatureQuietly(saxFactory, "http://xml.org/sax/features/external-general-entities", false);
        setFeatureQuietly(saxFactory, "http://xml.org/sax/features/external-parameter-entities", false);
        SAXParser parser = saxFactory.newSAXParser();

        try {
            parser.parse(inputStream, this);
        } catch (SAXException | IOException e) {
            String message = "Exception occured during the RSS parsing process.";
            Log.e(LOG_TAG, message, e);
            throw new RssFeedParserException(message, e);
        }

        //Log.d(LOG_TAG, " Feed size: " + feed.getItems().size());

        return feed;
    }

    /**
     * Decodes HTML/XML entities such as {@code &#039;} or {@code &amp;} that can appear
     * double-encoded in MyEpisodes feed titles.
     */
    private String decodeHtmlEntities(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString();
    }

    /**
     * Downloads a feed with the shared HTTP client so the MyEpisodes session cookie is sent.
     */
    private InputStream openFeed(URL url) throws IOException {
        return new ByteArrayInputStream(HttpClientProvider.getInstance().getBytes(url.toString()));
    }

    private static byte[] xmlBytes(String xml) {
        return xml == null ? new byte[0] : xml.getBytes(StandardCharsets.UTF_8);
    }

    private void setFeatureQuietly(SAXParserFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception e) {
            // Not every platform parser implements every feature; keep the ones it does support.
            Log.w(LOG_TAG, "Parser feature not supported: " + feature, e);
        }
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        // Start a fresh buffer for every element. characters() appends to it because SAX is
        // free to split the text of a single element across multiple callbacks.
        nodeValue = new StringBuilder();
        switch (localName) {
            case "item":
                item = new FeedItem();
                item.setTitle("");
                inItem = true;
                inDescription = false;
                break;
            case "description":
                //Fix for issue 96: If we ignore the description tag the issue is solved!
                inDescription = true;
                nodeValue = null;
                break;
            default:
                inDescription = false;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (localName.equals("item")) {
            feed.addItem(item);
            item = null;
            inItem = false;
        }

        if (inItem && nodeValue != null) {
            switch (localName) {
                case "guid":
                    item.setGuid(nodeValue.toString().trim());
                    break;
                case "title":
                    // Sample title: [ Reaper ][ 01x14 ][ Rebellion ][ 23-Apr-2008 ]
                    // Keep the whole title, including when the parser splits it into chunks.
                    String rawTitle = nodeValue.toString().trim();
                    if (rawTitle.startsWith("[ ")) {
                        item.setTitle(decodeHtmlEntities(rawTitle));
                    }
                    break;
                case "link":
                    item.setLink(nodeValue.toString().trim());
                    break;
                default:
                    break;
            }
        }
        // The description buffer is deliberately not populated (see issue 96), but keep the
        // empty description on the item so callers still see a non-null value.
        if (inItem && localName.equals("description")) {
            item.setDescription("");
        }
        nodeValue = null;
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        //Fix for issue 96: If we ignore the description tag the issue is solved!
        if (!inDescription && nodeValue != null) {
            nodeValue.append(ch, start, length);
        }
    }


    private InputStream openCacheFile(String filename) throws IOException {
        File file = new File(MyEpisodeConstants.CONTEXT.getFilesDir(), filename);
        if (!file.exists()) {
            return null;
        }
        if (MyEpisodeConstants.isOnline()) {
            CacheUtils.deleteOldCacheFiles(file);
        } else {
            Log.d(LOG_TAG, "Offline, Cache files not checked for aging");
        }
        if (file.exists()) {
            return new FileInputStream(file);
        }
        return null;
    }

    private String convertStreamToString(InputStream is, String encoding) throws IOException {
        /*
         * To convert the InputStream to String we use the
         * Reader.read(char[] buffer) method. We iterate until the
         * Reader return -1 which means there's no more data to
         * read. We use the StringWriter class to produce the string.
         */
        if (is != null) {
            StringWriter writer = new StringWriter();

            char[] buffer = new char[1024];
            try (is) {
                Reader reader = new BufferedReader(new InputStreamReader(is, encoding));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            }
            return writer.toString();
        } else {
            return "";
        }
    }
}
