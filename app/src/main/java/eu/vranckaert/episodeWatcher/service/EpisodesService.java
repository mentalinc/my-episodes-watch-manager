package eu.vranckaert.episodeWatcher.service;

import android.util.Log;
import android.util.Xml;

import org.json.JSONException;
import org.json.JSONObject;
import org.pojava.datetime.DateTime;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

import androidx.room.Room;
import eu.vranckaert.episodeWatcher.constants.MyEpisodeConstants;
import eu.vranckaert.episodeWatcher.controllers.EpisodesController;
import eu.vranckaert.episodeWatcher.database.AppDatabase;
import eu.vranckaert.episodeWatcher.database.SeriesDAO;
import eu.vranckaert.episodeWatcher.domain.Episode;
import eu.vranckaert.episodeWatcher.domain.Feed;
import eu.vranckaert.episodeWatcher.domain.FeedItem;
import eu.vranckaert.episodeWatcher.domain.User;
import eu.vranckaert.episodeWatcher.enums.EpisodeType;
import eu.vranckaert.episodeWatcher.exception.FeedUrlBuildingFaildException;
import eu.vranckaert.episodeWatcher.exception.InternetConnectivityException;
import eu.vranckaert.episodeWatcher.exception.LoginFailedException;
import eu.vranckaert.episodeWatcher.exception.ShowUpdateFailedException;
import eu.vranckaert.episodeWatcher.exception.UnsupportedHttpPostEncodingException;
import eu.vranckaert.episodeWatcher.utils.DateUtil;


public class EpisodesService {
    private static final String LOG_TAG = EpisodesService.class.getSimpleName();
    private final UserService userService;


    public EpisodesService() {
        userService = new UserService();
    }

    public List<Episode> retrieveEpisodes(EpisodeType episodesType, final User user) throws Exception {
        String encryptedPassword = userService.encryptPassword(user.getPassword());

        URL feedUrl;

        //downloadFullUnwatched list 
        if (Objects.equals(episodesType.toString(), "EPISODES_TO_WATCH")) {
            //override with download from http://myepisodes.com/views.php

            Log.d(LOG_TAG, "MyEpisodeConstants.DAYS_BACK_ENABLED: " + MyEpisodeConstants.DAYS_BACK_ENABLED);
            Log.d(LOG_TAG, "MyEpisodeConstants.DAYS_BACK_CP: " + MyEpisodeConstants.DAYS_BACK_CP);
            Log.d(LOG_TAG, "MyEpisodeConstants.CACHE_EPISODES_ENABLED: " + MyEpisodeConstants.CACHE_EPISODES_ENABLED);


            if (MyEpisodeConstants.DAYS_BACK_ENABLED) {

                if (MyEpisodeConstants.CACHE_EPISODES_ENABLED) {
                    Log.d(LOG_TAG, "Cache is enabled, read from disk");
                    MyEpisodeConstants.EXTENDED_EPISODES_XML = ReadFile("Watch.xml");

                    //xml file not found
                    if (MyEpisodeConstants.EXTENDED_EPISODES_XML.equalsIgnoreCase("FileNotFound")) {
                        Log.d(LOG_TAG, "No cached file found. Downloading...");
                        MyEpisodeConstants.EXTENDED_EPISODES_XML = downloadFullUnwatched(user).toString();

                        //write the xml to disk for future use
                        String FILENAME = "Watch.xml";
                        FileOutputStream fos = MyEpisodeConstants.CONTEXT.openFileOutput(FILENAME, 0); //Mode_PRIVATE
                        fos.write(MyEpisodeConstants.EXTENDED_EPISODES_XML.getBytes());
                        fos.close();
                        Log.d(LOG_TAG, FILENAME + " saved to disk");
                    }
                } else {
                    Log.d(LOG_TAG, "Cache is disabled, download from Internet");
                    MyEpisodeConstants.EXTENDED_EPISODES_XML = downloadFullUnwatched(user).toString();
                }

                feedUrl = new URL("http://127.0.0.1"); //this is used in the parse to confirm that this has been run.
            } else {
                //if not enabling the extended functions
                feedUrl = buildEpisodesUrl(episodesType, user.getUsername().replace(" ", "%20"), encryptedPassword);
            }
        } else {
            //if 200+ episodes not enabled
            feedUrl = buildEpisodesUrl(episodesType, user.getUsername().replace(" ", "%20"), encryptedPassword);
        }

        RssFeedParser rssFeedParser = new SaxRssFeedParser();
        Feed rssFeed;
        rssFeed = rssFeedParser.parseFeed(episodesType, feedUrl);

        List<Episode> episodes = new ArrayList<>(0);

        AppDatabase database = Room.databaseBuilder(eu.vranckaert.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                .allowMainThreadQueries()   //Allows room to do operation on main thread
                .fallbackToDestructiveMigration()
                .build();

        SeriesDAO seriesDAO = database.getSeriesDAO();
        //remove any shows that have a null in them
        seriesDAO.deleteNullShow();

        for (FeedItem item : rssFeed.getItems()) {
            Episode episode = new Episode();

            StringBuilder title = new StringBuilder(item.getTitle());

            if (title.length() > 0) {
                //Sample title: [ Reaper ][ 01x14 ][ Rebellion ][ 23-Apr-2008 ]
                title = title.replace(0, 2, ""); //Strip off first bracket [
                title = title.replace(title.length() - 2, title.length(), ""); //Strip off last bracket ]
                String[] episodeInfo = title.toString().split(MyEpisodeConstants.FEED_TITLE_SEPERATOR);
                episode.setShowName(episodeInfo[0].trim());
                getSeasonAndEpisodeNumber(episodeInfo[1], episode);
                Date airDate = null;
                if (episodeInfo.length == MyEpisodeConstants.FEED_TITLE_EPISODE_FIELDS) {
                    episode.setName(episodeInfo[2].trim());
                    String airDateString = episodeInfo[3].trim();
                    episode.setType(episodesType);

                    //Log.d(LOG_TAG, "airDateString: " + airDateString);

                    // airDateString = airDateString.replace("-","/");


                    try {
                        airDate = parseDate(airDateString);
                    } catch (Exception e) {
                        airDate = DateUtil.convertToDate(airDateString);
                    }

                    // Log.d(LOG_TAG, "airDateString: " + airDateString);

                    episode.setAirDate(airDate);
                    episode.setMyEpisodeID(item.getGuid().split("-")[0].trim());
                    //episode.setTVMazeWebSite(item.getLink());


                    //episode.setShowName(episode.getShowName() +" - "+ showRuntime.showRuntime + " mins");
                    try {

                        //RunTimeEnable = true = show runtime
                        if (MyEpisodeConstants.SHOW_RUNTIME_ENABLED) {
                            //Add runtime to the Show Name
                            seriesDAO = database.getSeriesDAO();

                            EpisodeRuntime showRuntime = seriesDAO.getEpisodeRuntimeWithMyEpsId(episode.getMyEpisodeID());
                            episode.setShowName(showRuntime.getShowRuntime() + " mins" + " - " + episode.getShowName());
                            episode.setTVMazeWebSite("https://www.tvmaze.com/shows/" + showRuntime.getShowTVMazeID());

                        } else {
                            episode.setShowName(episode.getShowName());

                        }

                    } catch (NullPointerException e) {
                        if (MyEpisodeConstants.SHOW_RUNTIME_ENABLED) {
                            episode.setShowName("Error mins" + " - " + episode.getShowName());
                        } else {
                            episode.setShowName(episode.getShowName());
                        }
                        String message = "Problem reading runtime for " + episode.getName();
                        Log.e(LOG_TAG, message);

                    }

                    //   Log.d(LOG_TAG,"Episode RunTime: " + episode.getShowName() + "  " + showRuntime.showRuntime);

                    Log.d(LOG_TAG, "Episode from feed: " + episode.getShowName() + " - S" + episode.getSeasonString() + "E" + episode.getEpisodeString());
                } else if (episodeInfo.length == MyEpisodeConstants.FEED_TITLE_EPISODE_FIELDS - 1) {
                    //Solves problem mentioned in Issue 20
                    episode.setName(episodeInfo[2].trim() + "...");
                    episode.setMyEpisodeID(item.getGuid().split("-")[0].trim());
                    //episode.setTVMazeWebSite(item.getLink());

                    seriesDAO = database.getSeriesDAO();

                    EpisodeRuntime showRuntime = seriesDAO.getEpisodeRuntimeWithMyEpsId(episode.getMyEpisodeID());
                    episode.setShowName(showRuntime.getShowRuntime() + " mins" + " - " + episode.getShowName());
                    episode.setTVMazeWebSite("https://www.tvmaze.com/shows/" + showRuntime.getShowTVMazeID());

                    //episode.setTVMazeWebSite("Link to episode description coming soon");
                    //episode.setTVMazeWebSite(ShowsEpisodeLink(seriesDAO.getTvmazeShowID(episode.getMyEpisodeID()).showTVMazeID, episode.getSeason(), episode.getEpisode()));

                } else {
                    String message = "Problem parsing a feed item. Feed details: " + item.toString();
                    Log.e(LOG_TAG, message);
                }


                if (!episodesType.equals(EpisodeType.EPISODES_COMING)) {
                    episodes.add(episode);
                } else {
                    Calendar rightNow = Calendar.getInstance();
                    rightNow.add(Calendar.DATE, -1);
                    Date yesterday = rightNow.getTime();
                    if (airDate != null) {
                        if (airDate.after(yesterday)) {
                            episodes.add(episode);
                        }
                    }
                }
            }
        }
        database.close();
        return episodes;
    }

    private String ReadFile(String FILENAME) {
        StringBuilder EpisodeXML = new StringBuilder();
        try {
            //String FILENAME = "Watch.xml";
            FileInputStream instream = MyEpisodeConstants.CONTEXT.openFileInput(FILENAME);

            File file = new File(MyEpisodeConstants.CONTEXT.getFilesDir(), FILENAME);

            Log.d(LOG_TAG, "Save file Path" + MyEpisodeConstants.CONTEXT.getFilesDir().toString());
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

    private void deleteFile(File filetoDelete) {
        if (filetoDelete.exists()) {
            if (filetoDelete.delete()) {
                Log.d(LOG_TAG, filetoDelete.getName() + " deleted");
            } else {
                Log.e(LOG_TAG, "ERROR deleting " + filetoDelete.getName());
            }
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

    public void watchedEpisode(Episode episode, User user) throws LoginFailedException, ShowUpdateFailedException, UnsupportedHttpPostEncodingException, InternetConnectivityException {
        List<Episode> episodes = new ArrayList<>();
        episodes.add(episode);
        watchedEpisodes(episodes, user);
    }

    public void watchedEpisodes(List<Episode> episodes, User user) throws LoginFailedException
            , ShowUpdateFailedException, UnsupportedHttpPostEncodingException, InternetConnectivityException {


        userService.login(user.getUsername(), user.getPassword());

        for (Episode episode : episodes) {
            markAnEpisode(0, episode);
            EpisodesController.getInstance().deleteEpisode(EpisodeType.EPISODES_COMING, episode);
            EpisodesController.getInstance().deleteEpisode(EpisodeType.EPISODES_TO_ACQUIRE, episode);
            EpisodesController.getInstance().deleteEpisode(EpisodeType.EPISODES_TO_WATCH, episode);
        }
    }

    public void acquireEpisode(Episode episode, User user) throws LoginFailedException, ShowUpdateFailedException, UnsupportedHttpPostEncodingException, InternetConnectivityException {
        List<Episode> episodes = new ArrayList<>();
        episodes.add(episode);
        acquireEpisodes(episodes, user);
    }

    public void acquireEpisodes(List<Episode> episodes, User user) throws LoginFailedException, ShowUpdateFailedException, UnsupportedHttpPostEncodingException, InternetConnectivityException {

        userService.login(user.getUsername(), user.getPassword());

        for (Episode episode : episodes) {
            markAnEpisode(1, episode);
            EpisodesController.getInstance().deleteEpisode(EpisodeType.EPISODES_TO_ACQUIRE, episode);
            EpisodesController.getInstance().addEpisode(EpisodeType.EPISODES_TO_WATCH, episode);
        }
    }

    private void markAnEpisode(int EpisodeStatus, Episode episode) throws ShowUpdateFailedException, InternetConnectivityException {
        String urlRep;

        urlRep = EpisodeStatus == 0 ? MyEpisodeConstants.MYEPISODES_UPDATE_WATCH :
                MyEpisodeConstants.MYEPISODES_UPDATE_ACQUIRE;

        urlRep = urlRep.replace(MyEpisodeConstants.MYEPISODES_UPDATE_PAGE_EPISODE_REPLACEMENT,
                String.valueOf(episode.getEpisode()));
        urlRep = urlRep.replace(MyEpisodeConstants.MYEPISODES_UPDATE_PAGE_SEASON_REPLACEMENT,
                String.valueOf(episode.getSeason()));
        urlRep = urlRep.replace(MyEpisodeConstants.MYEPISODES_UPDATE_PAGE_SHOWID_REPLACEMENT, episode.getMyEpisodeID());

        URL url;
        int responseCode;
        try {
            url = new URL(urlRep);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            responseCode = conn.getResponseCode();

        } catch (UnknownHostException e) {
            String message = "Could not connect to host.";
            Log.e(LOG_TAG, message, e);
            throw new InternetConnectivityException(message, e);
        } catch (IOException e) {
            String message = "Updating the show status failed for URL " + urlRep;
            Log.w(LOG_TAG, message, e);
            throw new ShowUpdateFailedException(message, e);
        }

        if (responseCode != HttpsURLConnection.HTTP_OK) {
            String message = "Updating the show status failed with status code " + responseCode + " for URL " + urlRep;
            Log.w(LOG_TAG, message);
            throw new ShowUpdateFailedException(message);
        } else {
            Log.i(LOG_TAG, "Successfully updated the show from url " + urlRep + " (" + episode + ")");
        }
    }


    /*
     * parse the views.php page to show a full list of unwatched apps
     */

    private StringWriter downloadFullUnwatched(User user) throws LoginFailedException, ShowUpdateFailedException, InternetConnectivityException {
        String urlRep = MyEpisodeConstants.MYEPISODES_FULL_UNWATCHED_LISTING_TABLE;
        URL url;
        //login to myepisodes
        java.net.CookieManager msCookieManager = userService.login(user.getUsername(), user.getPassword());

        int status;

        StringWriter sw = new StringWriter();

        try {
            // Get current days back so users view is not broken.
            String[] controlPanelSettings = getDaysBack(msCookieManager);

            // Set the days back to retrieve unwatched eps.
            setDaysBack(controlPanelSettings, msCookieManager, false);

            // set the filter to only show eps that have not yet been watched
            //setViewFilters(true, isWatched, httpClient);


            url = new URL(urlRep);
            String response = "";
            Log.d(LOG_TAG, "DOWNLOADING FULL LIST");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            //String cookieList = "";
/*            if (msCookieManager.getCookieStore().getCookies().size() > 0) {
                // While joining the Cookies, use ',' or ';' as needed. Most of the servers are using ';'
                conn.setRequestProperty("Cookie", TextUtils.join(";", msCookieManager.getCookieStore().getCookies()));
                //cookieList = TextUtils.join(";", msCookieManager.getCookieStore().getCookies());
            }
*/
            // Log.d(LOG_TAG, "cookieList: " + cookieList);

            status = conn.getResponseCode();

            InputStreamReader inStreamReader = null;

            if (status == HttpsURLConnection.HTTP_OK) {

                inStreamReader = new InputStreamReader(conn.getInputStream());

                BufferedReader reader = new BufferedReader(inStreamReader);

                StringBuilder HTML = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    HTML.append(line);
                }

                String HTMLtoDecode = HTML.toString();

                //read html file.
                int startTable = HTMLtoDecode.indexOf(
                        "<table class=\"mylist\" width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">") +
                        78;
                HTMLtoDecode = HTMLtoDecode.substring(startTable);

                int endTable = HTMLtoDecode.indexOf("</table>") - 8;

                if (endTable < 1) {
                    //prevent index out of bounds exception below when defining HTMLtoDecode.
                    endTable = 1;
                    Log.d(LOG_TAG, "No episodes to display");
                }

                HTMLtoDecode = HTMLtoDecode.substring(1, endTable);

                //split each table row into an array for processing
                String[] EpisodeTable = HTMLtoDecode.split("</tr>");

                Log.d(LOG_TAG, "Number of eps found: " + EpisodeTable.length);

                //Download complete, now start rebuilding the RSS feed file.
                XmlSerializer xs = Xml.newSerializer();

                xs.setOutput(sw);
                xs.startDocument(null, null);
                xs.startTag(null, "channel");

                for (String a : EpisodeTable) {
                    //split each column into a array
                    if (a.equals("")) {
                        Log.d(LOG_TAG, "No Episodes found.");
                    }
                    if (a.contains("class=\"header\"")) {
                        //		Log.d(LOG_TAG,"Header row processed");
                    } else {
                        String[] rowProcess = a.split("</td>");

                        //name of show
                        int indexName = rowProcess[2].indexOf("show/id-");
                        String showPart = rowProcess[2].substring(indexName + 9);
                        indexName = showPart.indexOf("\">") + 2;
                        int indexNameEndTag = showPart.indexOf("</a>");
                        String show = showPart.substring(indexName, indexNameEndTag);

                        Log.d("Serier EP", "|" + rowProcess[3]);

                        // get Series and Episode
                        String seriesEp;
                        int seriesEpID;
                        if (rowProcess[3].contains("longnumber firstep")) {
                            seriesEpID = rowProcess[3].indexOf(">") + 1;
                            seriesEp = rowProcess[3].substring(seriesEpID);
                            //			Log.d(LOG_TAG,"S0xE0 First: " + rowProcess[3].toString());
                        } else {
                            seriesEpID = rowProcess[3].indexOf(">") + 1;
                            seriesEp = rowProcess[3].substring(seriesEpID);
                            //			Log.d(LOG_TAG,"S0xE0 Second: " + rowProcess[3].toString());
                        }
                        //		Log.d(LOG_TAG," SeriesEp: " + SeriesEp);

                        //Get episode name
                        int indexEp;
                        if (rowProcess[4].contains("epname firstep")) {
                            indexEp = rowProcess[4].indexOf(">"); //epname
                        } else {
                            indexEp = rowProcess[4].indexOf(">"); //epname firstep
                        }
                        String episodeName = rowProcess[4].substring(indexEp);
                        episodeName = episodeName.substring(1, episodeName.length());
                        //	Log.d(LOG_TAG, "EpisodeName: " + rowProcess[4].toString());

                        //Get episode link - doesn't work yet.
                        int indexEpLink = rowProcess[4].indexOf("a href=") + 8;
                        String episodeLink = rowProcess[4].substring(indexEpLink);
                        int indexEpLink1 = episodeLink.indexOf("\"");
                        //TODO check why setting to ""
                        //  episodeLink = "";

                        //get air date
                        int indexAirDate = rowProcess[0].length() - 15;
                        String airDate = rowProcess[0].substring(indexAirDate, indexAirDate + 11);

                        //Get guid
                        int indexGUID = rowProcess[5].indexOf("name=") + 7;
                        String guid = rowProcess[5].substring(indexGUID);
                        int indexGUID1 = guid.indexOf("\"");
                        guid = guid.substring(0, indexGUID1);
                        if (rowProcess[5].contains("checked") && !rowProcess[6].contains("checked")) {

                            String headerRow = "[ " + show + " ]" + "[ " + seriesEp + " ]" + "[ " + episodeName + " ]" + "[ " + airDate + " ]";

                            xs.startTag(null, "item");
                            xs.startTag(null, "guid");
                            xs.text(guid);
                            xs.endTag(null, "guid");

                            xs.startTag(null, "title");
                            xs.text(headerRow);
                            xs.endTag(null, "title");

                            xs.startTag(null, "link");
                            xs.text(episodeLink);
                            xs.endTag(null, "link");

                            xs.startTag(null, "description");
                            xs.endTag(null, "description");

                            xs.endTag(null, "item");
                        } else {
                            //Log.d(LOG_TAG, "Already watched or Not Acquired not adding to rss: [ " + Show + " ]" + "[ " + SeriesEp + " ]" + "[ " + EpisodeName + " ]" + "[ " + AirDate + " ]");
                        }
                        if (false) {
                            //  Log.d(LOG_TAG, "Setting 200+ acquire list");
                            // Log.d(LOG_TAG,"5: " + rowProcess[5].toString());
                            //  Log.d(LOG_TAG,"6: " + rowProcess[6].toString());

                            if (!rowProcess[5].contains("checked")) {

                                String headerRow = "[ " + show + " ]" + "[ " + seriesEp + " ]" + "[ " + episodeName + " ]" + "[ " + airDate + " ]";

                                xs.startTag(null, "item");
                                xs.startTag(null, "guid");
                                xs.text(guid);
                                xs.endTag(null, "guid");

                                xs.startTag(null, "title");
                                xs.text(headerRow);
                                xs.endTag(null, "title");

                                xs.startTag(null, "link");
                                xs.text(episodeLink);
                                xs.endTag(null, "link");

                                xs.startTag(null, "description");
                                xs.endTag(null, "description");

                                xs.endTag(null, "item");

                                Log.d(LOG_TAG, "Not acquired adding to rss: [ " + show + " ]" + "[ " + seriesEp + " ]" + "[ " + episodeName + " ]" + "[ " + airDate + " ]");
                            }
                        }
                    }
                }

                xs.endTag(null, "channel");
                xs.endDocument();
                Log.d(LOG_TAG, "Finished Download and RSS built");
                Log.d(LOG_TAG, "Resetting  control panel settings");
                //set the days back to what they are in the settigns
                setDaysBack(controlPanelSettings, msCookieManager, true);
                // setViewFilters(false, false, httpClient);
            }
        } catch (UnknownHostException e) {
            String message = "Could not connect to host.";
            Log.e(LOG_TAG, message, e);
            throw new InternetConnectivityException(message, e);
        } catch (IOException e) {
            String message = "Error downloading and processing " + urlRep;
            Log.w(LOG_TAG, message, e);
            throw new ShowUpdateFailedException(message, e);
        }

        if (status != 200) {
            String message =
                    "Error downloading and processing, failed with status code " + status + " for URL " + urlRep;
            Log.w(LOG_TAG, message);
            throw new ShowUpdateFailedException(message);
        } else {
            Log.i(LOG_TAG, "Successfully downloaded full episode list from url " + urlRep + " (" + ")");
        }
        return sw;
    }

    //get the users web browser settings to keep them the same
    private String[] getDaysBack(java.net.CookieManager msCookieManager) {
        String[] controlPanelSettings = new String[20];


        //control panel settings to read.
        String eps_timezone;
        String eps_time_offset;
        String dateformat;
        String timeformat;
        String eps_number_format;
        String ce_dback;
        String ce_dforward;
        String colorpast1;
        String colorpast2;
        String colortoday;
        String color1;
        String color2;
        String colorhover;
        String sw_acquire_delay;
        String cal_firstday;
        String action;
        String loginpage;
        String sw_hidefuture;
        String sw_presentonly;
        String sw_currentseasononly;

        //default values should be same for all users.
        action = "Save";

        try {

            URL url;
            String response = "";
            try {
                url = new URL(MyEpisodeConstants.MYEPISODES_CONTROL_PANEL);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
/*                if (msCookieManager.getCookieStore().getCookies().size() > 0) {
                    // While joining the Cookies, use ',' or ';' as needed. Most of the servers are using ';'
                    conn.setRequestProperty("Cookie", TextUtils.join(";", msCookieManager.getCookieStore().getCookies()));
                }
*/
                int responseCode = conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    StringBuilder HTMLcp = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        HTMLcp.append(line);
                    }

                    //need to store and send all the settings sending just the daysback setting doesn't work.
                    String settingsHTML = HTMLcp.toString();

                    ce_dback = settingsHTML.substring(settingsHTML.indexOf("name=\"ce_dback\" value=\"") + 23);
                    ce_dback = ce_dback.substring(0, ce_dback.indexOf("\""));

                    eps_time_offset = settingsHTML.substring(settingsHTML.indexOf("name=\"eps_time_offset\" value=\"") + 30);
                    eps_time_offset = eps_time_offset.substring(0, eps_time_offset.indexOf("\""));

                    dateformat = settingsHTML.substring(settingsHTML.indexOf("name=\"dateformat\" value=\"") + 25);
                    dateformat = dateformat.substring(0, dateformat.indexOf("\""));

                    timeformat = settingsHTML.substring(settingsHTML.indexOf("name=\"timeformat\" value=\"") + 25);
                    timeformat = timeformat.substring(0, timeformat.indexOf("\""));

                    eps_number_format = settingsHTML.substring(settingsHTML.indexOf("name=\"eps_number_format\" value=\"") + 32);
                    eps_number_format = eps_number_format.substring(0, eps_number_format.indexOf("\""));

                    ce_dforward = settingsHTML.substring(settingsHTML.indexOf("name=\"ce_dforward\" value=\"") + 26);
                    ce_dforward = ce_dforward.substring(0, ce_dforward.indexOf("\""));

                    colorpast1 = settingsHTML.substring(settingsHTML.indexOf("'link1');\" type=\"text\" value=\"") + 30);
                    colorpast1 = colorpast1.substring(0, colorpast1.indexOf("\""));

                    colorpast2 = settingsHTML.substring(settingsHTML.indexOf("'link2');\" type=\"text\" value=\"") + 30);
                    colorpast2 = colorpast2.substring(0, colorpast2.indexOf("\""));

                    colortoday = settingsHTML.substring(settingsHTML.indexOf("'link3');\" type=\"text\" value=\"") + 30);
                    colortoday = colortoday.substring(0, colortoday.indexOf("\""));

                    color1 = settingsHTML.substring(settingsHTML.indexOf("'link4');\" type=\"text\" value=\"") + 30);
                    color1 = color1.substring(0, color1.indexOf("\""));

                    color2 = settingsHTML.substring(settingsHTML.indexOf("'link5');\" type=\"text\" value=\"") + 30);
                    color2 = color2.substring(0, color2.indexOf("\""));

                    colorhover = settingsHTML.substring(settingsHTML.indexOf("'link6');\" type=\"text\" value=\"") + 30);
                    colorhover = colorhover.substring(0, colorhover.indexOf("\""));

                    sw_acquire_delay = settingsHTML.substring(settingsHTML.indexOf("name=\"sw_acquire_delay\" value=\"") + 31);
                    sw_acquire_delay = sw_acquire_delay.substring(0, sw_acquire_delay.indexOf("\""));

                    cal_firstday = settingsHTML.substring(settingsHTML.indexOf("name=\"cal_firstday\""));
                    cal_firstday = cal_firstday.substring(cal_firstday.indexOf("selected") - 3, cal_firstday.indexOf("selected") - 2);

                    /*
                    ERROR HERE SO IT ALL FAILS NEED TO CHECK WHATS IS HAPPENNING

                    Not logged in so fails as there is only 1 default option

                     */

                    int timeZoneIndex = settingsHTML.indexOf("name=\"eps_timezone\"");
                    eps_timezone = settingsHTML.substring(timeZoneIndex);
                    int timeZoneSelectedIndex = eps_timezone.indexOf("</select>");
                    String TimezoneRange = settingsHTML.substring(timeZoneIndex, timeZoneIndex + timeZoneSelectedIndex);
                    String[] SplitTimeZones = TimezoneRange.split("</option>");

                    for (String a : SplitTimeZones) {
                        int selectedIndex = a.indexOf("selected");
                        if (selectedIndex > 1) {
                            eps_timezone = a.substring(a.indexOf(">") + 1);
                        }
                    }


                    // eps_timezone = "US/Eastern";
                    int loginpageIndex = settingsHTML.indexOf("name=\"loginpage\"") + 17;
                    loginpage = settingsHTML.substring(loginpageIndex);
                    int loginpageSelectedIndex = loginpage.indexOf("</select>");
                    String loginpageRange = settingsHTML.substring(loginpageIndex, loginpageIndex + loginpageSelectedIndex);
                    String[] Splitloginpage = loginpageRange.split("</option>");

                    for (String a : Splitloginpage) {
                        int selectedIndex = a.indexOf("selected");
                        if (selectedIndex > 1) {
                            loginpage = a.substring(a.indexOf("=") + 2);
                            loginpage = loginpage.substring(0, loginpage.indexOf("\""));
                        }
                    }

                    sw_hidefuture = settingsHTML.substring(settingsHTML.indexOf("name=\"sw_hidefuture\""));
                    sw_hidefuture = sw_hidefuture.substring(21, 28);
                    if (sw_hidefuture.equals("checked")) {
                        sw_hidefuture = "on";
                    } else {
                        sw_hidefuture = null;
                    }

                    sw_presentonly = settingsHTML.substring(settingsHTML.indexOf("name=\"sw_presentonly\""));
                    sw_presentonly = sw_presentonly.substring(22, 29);
                    if (sw_presentonly.equals("checked")) {
                        sw_presentonly = "on";
                    } else {
                        sw_presentonly = null;
                    }

                    sw_currentseasononly = settingsHTML.substring(settingsHTML.indexOf("name=\"sw_currentseasononly\""));
                    sw_currentseasononly = sw_currentseasononly.substring(28, 35);
                    if (sw_currentseasononly.equals("checked")) {
                        sw_currentseasononly = "on";
                    } else {
                        sw_currentseasononly = null;
                    }

                    controlPanelSettings[0] = eps_timezone;
                    controlPanelSettings[1] = eps_time_offset;
                    controlPanelSettings[2] = dateformat;
                    controlPanelSettings[3] = timeformat;
                    controlPanelSettings[4] = eps_number_format;
                    controlPanelSettings[5] = ce_dback;
                    controlPanelSettings[6] = ce_dforward;
                    controlPanelSettings[7] = colorpast1;
                    controlPanelSettings[8] = colorpast2;
                    controlPanelSettings[9] = colortoday;
                    controlPanelSettings[10] = color1;
                    controlPanelSettings[11] = color2;
                    controlPanelSettings[12] = colorhover;
                    controlPanelSettings[13] = sw_acquire_delay;
                    controlPanelSettings[14] = cal_firstday;
                    controlPanelSettings[15] = action;
                    controlPanelSettings[16] = loginpage;
                    controlPanelSettings[17] = sw_hidefuture;
                    controlPanelSettings[18] = sw_presentonly;
                    controlPanelSettings[19] = sw_currentseasononly;

                    //display all settings which will be set for cp.php
                /*StringBuilder builder = new StringBuilder();
                for (String value : controlPanelSettings) {
                    builder.append("   ");
                    builder.append(value);
                }*/
                    //display all settings which will be set for cp.php
                    //Log.d(LOG_TAG, builder.toString());
                }
            } catch (IOException e) {
                String message = "Error setting days back";
                Log.e(LOG_TAG, message, e);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return controlPanelSettings;
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    private void setDaysBack(String[] controlPanelSettings, java.net.CookieManager msCookieManager, Boolean restore) {
        //send POST to set just the number of days in the past to show..
        Log.d(LOG_TAG, "Setting number of days back");
        String[] controlPanelOrder = new String[20];
        controlPanelOrder[0] = "eps_timezone";
        controlPanelOrder[1] = "eps_time_offset";
        controlPanelOrder[2] = "dateformat";
        controlPanelOrder[3] = "timeformat";
        controlPanelOrder[4] = "eps_number_format";
        controlPanelOrder[5] = "ce_dback";
        controlPanelOrder[6] = "ce_dforward";
        controlPanelOrder[7] = "colorpast1";
        controlPanelOrder[8] = "colorpast2";
        controlPanelOrder[9] = "colortoday";
        controlPanelOrder[10] = "color1";
        controlPanelOrder[11] = "color2";
        controlPanelOrder[12] = "colorhover";
        controlPanelOrder[13] = "sw_acquire_delay";
        controlPanelOrder[14] = "cal_firstday";
        controlPanelOrder[15] = "action";
        controlPanelOrder[16] = "loginpage";

        //if not commented sets the value in settings need to increase the array size above as well
        controlPanelOrder[17] = "sw_hidefuture";
        controlPanelOrder[18] = "sw_presentonly";
        controlPanelOrder[19] = "sw_currentseasononly";


        try {


            URL url;
            String response = "";

            url = new URL(MyEpisodeConstants.MYEPISODES_CONTROL_PANEL);

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
/*            if (msCookieManager.getCookieStore().getCookies().size() > 0) {
                // While joining the Cookies, use ',' or ';' as needed. Most of the servers are using ';'
                conn.setRequestProperty("Cookie", TextUtils.join(";", msCookieManager.getCookieStore().getCookies()));
            }
*/

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));


            HashMap postDataParams = new HashMap<String, String>() {{
                put("A", "B");
            }};

            for (int i = 0; i < controlPanelOrder.length; i++) {
                //when setting the value for the download
                if (i == 5 && !restore && controlPanelSettings[i] != null) {
                    postDataParams.put(controlPanelOrder[i], MyEpisodeConstants.DAYS_BACK_CP);
                } else if (i == 6 && !restore && controlPanelSettings[i] != null) {
                    postDataParams.put(controlPanelOrder[i], "1");
                } else {
                    if (controlPanelSettings[i] != null) {
                        postDataParams.put(controlPanelOrder[i], controlPanelSettings[i]);
                    }
                }
            }

            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
            } else {
                response = "";

            }
        } catch (Exception e) {
            String message = "Error setting days back";
            Log.e(LOG_TAG, message, e);
        }
    }
/*
    //todo - make this actually work
    private void setViewFilters(Boolean setForDownload, Boolean isWatched) {        HttpPost httppost = new HttpPost(MyEpisodeConstants.MYEPISODES_FULL_UNWATCHED_LISTING);
        Log.e(LOG_TAG, "setViewFilters has been called");
        try {
            if (setForDownload) {
                //send POST request to only show episodes with the filter Watch
                //eps_filters[]=2&action=Filter
                if (isWatched) {
                    List<NameValuePair> nameValuePairs = new ArrayList<>(2);
                    nameValuePairs.add(new BasicNameValuePair("eps_filters[]", "2"));
                    nameValuePairs.add(new BasicNameValuePair("action", "Filter"));
                    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                } else {
                    List<NameValuePair> nameValuePairs = new ArrayList<>(2);
                    nameValuePairs.add(new BasicNameValuePair("eps_filters[]", "1"));
                    nameValuePairs.add(new BasicNameValuePair("action", "Filter"));
                    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                }
            } else {
                //set back to user default settings for the view filters
                // need to work on detecting the settings to use.
                List<NameValuePair> nameValuePairs2 = new ArrayList<>(2);
                nameValuePairs2.add(new BasicNameValuePair("eps_filters[]", "1"));
                nameValuePairs2.add(new BasicNameValuePair("eps_filters[]", "2"));
                nameValuePairs2.add(new BasicNameValuePair("action", "Filter"));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs2));
            }
            // 	Execute HTTP Post Request
            HttpResponse responsePost = httpClient.execute(httppost);
            responsePost.getStatusLine();

            EntityUtils.toString(responsePost.getEntity());
            // Get hold of the response entity - need to read to the end to prevent "Invalid use of SingleClient...."
            /*HttpEntity entity = responsePost.getEntity();
            if (entity != null) {
    			InputStream instream = entity.getContent();    			
    			BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
    			StringBuilder HTMLcp = new StringBuilder();
    			String line;
    			while ((line = reader.readLine()) != null) {
    				HTMLcp.append(line);
    			}
    		}*/
/*
        } catch (IOException e) {
            String message = "Error setting days back";
            Log.e(LOG_TAG, message, e);
        }
    }
*/


    private Date parseDate(String date) {
        if (date.endsWith(".") || date.endsWith(";") || date.endsWith(":") || date.endsWith(",") || date.endsWith("-")) {
            date = date.substring(0, date.length() - 1);
        }

        DateTime parsedDate = new DateTime(date);
        return parsedDate.toDate();
    }

    private void getSeasonAndEpisodeNumber(String seasonEpisodeNumber, Episode episode) {
        if (seasonEpisodeNumber.startsWith("S")) {
            String[] episodeInfoNumber = seasonEpisodeNumber.split("E");
            episode.setSeason(Integer.parseInt(episodeInfoNumber[0].replace("S", "").trim()));
            episode.setEpisode(Integer.parseInt(episodeInfoNumber[1].trim()));
        } else {
            String[] episodeInfoNumber = seasonEpisodeNumber.split(MyEpisodeConstants.SEASON_EPISODE_NUMBER_SEPERATOR);
            episode.setSeason(Integer.parseInt(episodeInfoNumber[0].trim()));
            episode.setEpisode(Integer.parseInt(episodeInfoNumber[1].trim()));
        }
    }

    private URL buildEpisodesUrl(EpisodeType episodesType, final String username, final String encryptedPassword)
            throws FeedUrlBuildingFaildException {
        String urlRep = "";
        switch (episodesType) {
            case EPISODES_TO_WATCH:
                urlRep = MyEpisodeConstants.UNWATCHED_EPISODES_URL;
                break;
            case EPISODES_TO_ACQUIRE:
                urlRep = MyEpisodeConstants.UNAQUIRED_EPISODES_URL;
                break;
            case EPISODES_TO_YESTERDAY1:
                urlRep = MyEpisodeConstants.YESTERDAY_EPISODES_URL;
                break;
            case EPISODES_TO_YESTERDAY2:
                urlRep = MyEpisodeConstants.YESTERDAY2_EPISODES_URL;
                break;
            case EPISODES_COMING:
                urlRep = MyEpisodeConstants.COMING_EPISODES_URL;
                break;
        }

        urlRep = urlRep.replace(MyEpisodeConstants.UID_REPLACEMENT_STRING, username);
        urlRep = urlRep.replace(MyEpisodeConstants.PWD_REPLACEMENT_STRING, encryptedPassword);

        URL url;
        try {
            url = new URL(urlRep);
        } catch (MalformedURLException e) {
            String message = "The feed URL could not be build";
            Log.e(LOG_TAG, message, e);
            throw new FeedUrlBuildingFaildException(message, e);
        }

        Log.d(LOG_TAG, "FEED URL: " + url);

        return url;
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


    public String ShowsEpisodeLink(String showTVMazeID, int seasonNumber, int episodeNumber) {

        HttpsURLConnection connection = null;
        BufferedReader reader = null;
        //https://api.tvmaze.com/shows/1/episodebynumber?season=1&number=1
        String tvMazeAPIURL = "https://api.tvmaze.com/shows/";
        String episodeURLandSummary = "";
        //Add the show to the end of the URL
        //need to pull the show out of the list one at a time may?

        // for (int i = 0; i < show.size(); i++) {
        //for now just work with the first item will build to work with all in time.


        // Log.d("Response: ", "> " + show);
        showTVMazeID = showTVMazeID.replace("#", "");
        tvMazeAPIURL += showTVMazeID + "/episodebynumber?season=" + seasonNumber + "&number=" + episodeNumber;


        Log.d(LOG_TAG, "epsiode info URL: " + tvMazeAPIURL);

        try {
            URL url = new URL(tvMazeAPIURL);
            connection = (HttpsURLConnection) url.openConnection();
            connection.connect();
            int code = connection.getResponseCode();
            Log.d(LOG_TAG, "API HTTP Status Code: " + code);

            if (code == 429) {
                Thread.sleep(10000);
                //wait 10 seconds then try again
                connection = (HttpsURLConnection) url.openConnection();
                connection.connect();
            }
            if (code == 404) {
                return "Episode detail not found";
            }


            InputStream stream = connection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(stream));

            StringBuilder buffer = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                buffer.append(line);
                buffer.append("\n");
                //    Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)

            }

            String jsonString = buffer.toString();
            JSONObject jObj;
            String episodeUrl = "";
            String episodeSummary = "";

            try {
                jObj = new JSONObject(jsonString);
                episodeUrl = jObj.getString("url");
                episodeSummary = jObj.getString("summary");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            episodeURLandSummary = episodeUrl + "\n\n" + episodeSummary;

            Log.d("episodeUrl: ", "> " + episodeUrl);
            Log.d("episodeSummary: ", "> " + episodeSummary);

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        } finally {

            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return episodeURLandSummary;
    }
}
