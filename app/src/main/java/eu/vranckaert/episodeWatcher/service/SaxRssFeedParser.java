package eu.vranckaert.episodeWatcher.service;

import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import eu.vranckaert.episodeWatcher.constants.MyEpisodeConstants;
import eu.vranckaert.episodeWatcher.domain.Feed;
import eu.vranckaert.episodeWatcher.domain.FeedItem;
import eu.vranckaert.episodeWatcher.enums.EpisodeType;
import eu.vranckaert.episodeWatcher.exception.FeedUrlParsingException;
import eu.vranckaert.episodeWatcher.exception.InternetConnectivityException;
import eu.vranckaert.episodeWatcher.exception.RssFeedParserException;

public class SaxRssFeedParser extends DefaultHandler implements RssFeedParser {
    private static final String LOG_TAG = SaxRssFeedParser.class.getSimpleName();

    private boolean inItem = false;
    private boolean inDescription = true;
    private boolean recordTitleString = false;
    private StringBuilder tempTitle = new StringBuilder();
    private StringBuilder nodeValue = null;
    private final Feed feed = new Feed();
    private FeedItem item = null;

    public Feed parseFeed(final URL url) {

        //replaced by parseFeed(EpisodeType episodesType, final URL url)
        Log.e(LOG_TAG, "This will no longer be called. Replaced by parseFeed(EpisodeType episodesType, final URL url)");
       /*
        InputStream inputStream;
		try {
			
			//Log.e(LOG_TAG, "URL???: " + url.toString().substring(0, 16));
			if(url.toString().substring(0, 16).equalsIgnoreCase("http://127.0.0.1")){
				inputStream = new ByteArrayInputStream(MyEpisodeConstants.EXTENDED_EPISODES_XML.getBytes("UTF-8"));
			}else{
				inputStream = url.openConnection().getInputStream();
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
        SAXParser parser = saxFactory.newSAXParser();
        
        try {
			parser.parse(inputStream, this);
		} catch (SAXException e) {
			String message = "Exception occured during the RSS parsing process.";
			Log.e(LOG_TAG, message, e);
			throw new RssFeedParserException(message, e);
		} catch (IOException e) {
			String message = "Exception occured during the RSS parsing process.";
			Log.e(LOG_TAG, message, e);
			throw new RssFeedParserException(message, e);
		}

        //Log.d(LOG_TAG, " Feed size: " + feed.getItems().size());
        */
        return feed;
    }


    public Feed parseFeed(EpisodeType episodesType, final URL url) throws ParserConfigurationException, SAXException, FeedUrlParsingException, RssFeedParserException, InternetConnectivityException {
        InputStream inputStream;

        Log.d(LOG_TAG, "episodesType being Parsed: " + episodesType);

        try {

            if (MyEpisodeConstants.CACHE_EPISODES_ENABLED) {
                Log.d(LOG_TAG, "Cache is enabled, read from disk");
                String FILENAME;
                String FileContents;
                switch (episodesType) {
                    case EPISODES_TO_WATCH:
                        if (MyEpisodeConstants.DAYS_BACK_ENABLED) {
                            Log.d(LOG_TAG, "MyEpisodeConstants.EXTENDED_EPISODES_XML:  " + MyEpisodeConstants.EXTENDED_EPISODES_XML);
                            inputStream = new ByteArrayInputStream(MyEpisodeConstants.EXTENDED_EPISODES_XML.getBytes("UTF-8"));
                        } else {
                            inputStream = url.openConnection().getInputStream();
                            FILENAME = "Watch.xml";
                            FileContents = ReadFile(FILENAME);
                            inputStream = new ByteArrayInputStream(FileContents.getBytes("UTF-8"));


                            //xml file not found
                            if (FileContents.equalsIgnoreCase("FileNotFound")) {
                                Log.d(LOG_TAG, "No cached " + FILENAME + " file found. Downloading...");
                                inputStream = url.openConnection().getInputStream();

                                //write the to disk for future use
                                FileOutputStream fos = MyEpisodeConstants.CONTEXT.openFileOutput(FILENAME, 0); //Mode_PRIVATE
                                String stringInputStream = convertStreamToString(inputStream, "UTF-8");
                                fos.write(stringInputStream.getBytes());
                                fos.close();
                                Log.d(LOG_TAG, FILENAME + " saved to disk");

                                //inputStream is closed
                                inputStream = new ByteArrayInputStream(stringInputStream.getBytes("UTF-8"));
                            }
                        }
                        break;
                    case EPISODES_TO_YESTERDAY1:
                        inputStream = url.openConnection().getInputStream();
                    case EPISODES_TO_YESTERDAY2:
                        inputStream = url.openConnection().getInputStream();
                    case EPISODES_TO_ACQUIRE:
                        FILENAME = "Acquire.xml";
                        FileContents = ReadFile(FILENAME);
                        inputStream = new ByteArrayInputStream(FileContents.getBytes("UTF-8"));

                        //xml file not found
                        if (FileContents.equalsIgnoreCase("FileNotFound")) {
                            Log.d(LOG_TAG, "No cached " + FILENAME + " file found. Downloading...");
                            inputStream = url.openConnection().getInputStream();

                            //write the to disk for future use
                            FileOutputStream fos = MyEpisodeConstants.CONTEXT.openFileOutput(FILENAME, 0); //Mode_PRIVATE
                            String stringInputStream = convertStreamToString(inputStream, "UTF-8");
                            fos.write(stringInputStream.getBytes());
                            fos.close();
                            Log.d(LOG_TAG, FILENAME + " saved to disk");

                            //inputStream is closed
                            inputStream = new ByteArrayInputStream(stringInputStream.getBytes("UTF-8"));
                        }

                        break;
                    case EPISODES_COMING:
                        FILENAME = "Coming.xml";
                        FileContents = ReadFile(FILENAME);
                        inputStream = new ByteArrayInputStream(FileContents.getBytes("UTF-8"));

                        //xml file not found
                        if (FileContents.equalsIgnoreCase("FileNotFound")) {
                            Log.d(LOG_TAG, "No cached " + FILENAME + " file found. Downloading...");
                            inputStream = url.openConnection().getInputStream();

                            //write the to disk for future use
                            FileOutputStream fos = MyEpisodeConstants.CONTEXT.openFileOutput(FILENAME, 0); //Mode_PRIVATE
                            String stringInputStream = convertStreamToString(inputStream, "UTF-8");
                            fos.write(stringInputStream.getBytes());
                            fos.close();
                            Log.d(LOG_TAG, FILENAME + " saved to disk");

                            //inputStream is closed
                            inputStream = new ByteArrayInputStream(stringInputStream.getBytes("UTF-8"));
                        }

                        break;

                    default:
                        //this should not be used
                        Log.d(LOG_TAG, "Default switch statement being called for ep type: " + episodesType);
                        inputStream = url.openConnection().getInputStream();
                }


            } else {
                //Cache is not enabled using standard RSS feeds
                if (url.toString().substring(0, 16).equalsIgnoreCase("http://127.0.0.1")) {
                    inputStream = new ByteArrayInputStream(MyEpisodeConstants.EXTENDED_EPISODES_XML.getBytes("UTF-8"));
                } else {
                    Log.d(LOG_TAG, "Cache is disabled, download from Internet RSS Feeds");
                    inputStream = url.openConnection().getInputStream();
                }
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

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
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

        if (inItem && localName.equals("guid")) {
            item.setGuid(nodeValue.toString());
        }
        if (inItem && localName.equals("title")) {
            item.setTitle(tempTitle.toString());
            tempTitle = new StringBuilder();
        }
        if (inItem && localName.equals("link")) {
            if (nodeValue != null) {
                item.setLink(nodeValue.toString());
            }
        }
        if (inItem && localName.equals("description")) {
            item.setDescription("");
        }
        nodeValue = null;
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        //Fix for issue 96: If we ignore the description tag the issue is solved!
        if (!inDescription) {
            nodeValue = new StringBuilder(new String(ch, start, length));
            if (recordTitleString) {
                if (nodeValue.length() == 1 && nodeValue.toString().equals("]")) {
                    tempTitle.append(nodeValue);
                    recordTitleString = false;
                } else if (nodeValue.length() > 1 && nodeValue.substring(nodeValue.length() - 2, nodeValue.length()).equals(" ]")) {
                    tempTitle.append(nodeValue);
                    recordTitleString = false;
                } else {
                    tempTitle.append(nodeValue);
                }
            } else {
                if (nodeValue.length() > 1 && nodeValue.substring(0, 2).equals("[ ")) {
                    if (nodeValue.length() > 1 && nodeValue.substring(nodeValue.length() - 2, nodeValue.length()).equals(" ]")) {
                        tempTitle.append(nodeValue);
                    } else {
                        tempTitle.append(nodeValue);
                        recordTitleString = true;
                    }
                }
            }
        }
    }


    private String ReadFile(String FILENAME) {
        StringBuilder EpisodeXML = new StringBuilder();
        try {
            //String FILENAME = "Watch.xml";
            FileInputStream instream = MyEpisodeConstants.CONTEXT.openFileInput(FILENAME);

            File file = new File(MyEpisodeConstants.CONTEXT.getFilesDir(), FILENAME);
            if (isOnline()) {
                deleteOldCacheFiles(file);
            } else {
                Log.d(LOG_TAG, "Offline, Cache files not checked for aging");
            }

            //add a check in the future to prompt user if they want to refresh the cache due to age. Add a preference to set time options....

            // if file the available for reading
            if (instream.available() > 1) {
                // prepare the file for reading
                InputStreamReader inputreader = new InputStreamReader(instream);
                BufferedReader buffreader = new BufferedReader(inputreader);

                String line;

                // read every line of the file into the line-variable, on line at the time
                while ((line = buffreader.readLine()) != null) {
                    EpisodeXML.append(line);
                    EpisodeXML.append('\n');
                }
            }

            // close the file again
            instream.close();
        } catch (FileNotFoundException e) {
            String message = "File doesn't exist: " + FILENAME;
            Log.e(LOG_TAG, message);
            return "FileNotFound";

        } catch (IOException e) {
            String message = "Problem reading file: " + FILENAME;
            Log.e(LOG_TAG, message);

        }

        return EpisodeXML.toString();
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
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, encoding));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";

        }
    }

    private void deleteOldCacheFiles(File filetoDelete) {
        if (!MyEpisodeConstants.CACHE_EPISODES_ENABLED || MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE.equalsIgnoreCase("0")) {
            Log.d(LOG_TAG, "Cache aging is disabled. Cache files not deleted");
        } else {
            Date lastModDate = new Date(filetoDelete.lastModified());
            Date Now = new Date();
            Calendar ModDate = Calendar.getInstance();
            Calendar NowDate = Calendar.getInstance();
            ModDate.setTime(lastModDate);
            NowDate.setTime(Now);
            long milliseconds1 = ModDate.getTimeInMillis();
            long milliseconds2 = NowDate.getTimeInMillis();
            long diff = milliseconds2 - milliseconds1;
            long diffHours = diff / (60 * 60 * 1000);
            long diffDays = diff / (24 * 60 * 60 * 1000);
            Log.d(LOG_TAG, "Time in hours: " + diffHours + " hours.");
            Log.d(LOG_TAG, "Time in days: " + diffDays + " days.");
            Log.d(LOG_TAG, "Cache age setting: " + Double.parseDouble(MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE) + " days " + MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE);
            Log.d(LOG_TAG, "Filename: " + filetoDelete.getName() + " Diff: " + diffDays + " last modified @ : " + lastModDate.toString());
            if (diffDays >= Double.parseDouble(MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE)) {
                Log.d(LOG_TAG, "Delete File too many DAYS old...");
                deleteFile(filetoDelete);
            } else if (Double.parseDouble(MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE) < 1) {
                if (diffHours >= 6 && MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE.equalsIgnoreCase("0.25")) {
                    Log.d(LOG_TAG, "Delete File too many HOURS old, Greater than 6...");
                    deleteFile(filetoDelete);
                }
                if (diffHours >= 12 && MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE.equalsIgnoreCase("0.5")) {
                    Log.d(LOG_TAG, "Delete File too many HOURS old, Greater than 12...");
                    deleteFile(filetoDelete);
                }
            } else {
                Log.d(LOG_TAG, filetoDelete.getName() + " cache not deleted. Cache still current.");

            }
        }
    }

    private void deleteFile(File filetoDelete) {
        if (filetoDelete.exists()) {
            if (filetoDelete.delete()) {
                Log.d(LOG_TAG, filetoDelete.getName() + " deleted");
            } else {
                Log.e(LOG_TAG, "ERROR deleting " + filetoDelete.getName());
            }
        }
    }

    private static boolean isOnline() {
        try {
            URL url = new URL("https://www.myepisodes.com/favicon.ico");
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "yourAgent");
            connection.setRequestProperty("Connection", "close");
            connection.setConnectTimeout(1000);
            connection.connect();

            if (connection.getResponseCode() == 200) {
                connection.disconnect();
                Log.d(LOG_TAG, "Online.");
                return true;
            } else {
                connection.disconnect();
                Log.d(LOG_TAG, "Offline!!");
                return false;
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, e.toString());
            return false;
        }
    }
}
