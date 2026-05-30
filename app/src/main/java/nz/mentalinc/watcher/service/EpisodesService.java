package nz.mentalinc.watcher.service;

import static nz.mentalinc.watcher.constants.MyEpisodeConstants.TV_MAZE_SHOWS_URL;

import android.util.Log;
import android.util.Xml;

import org.json.JSONException;
import org.json.JSONObject;
import org.pojava.datetime.DateTime;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import nz.mentalinc.watcher.constants.MyEpisodeConstants;
import nz.mentalinc.watcher.controllers.EpisodesController;
import nz.mentalinc.watcher.utils.CacheUtils;
import nz.mentalinc.watcher.database.AppDatabase;
import nz.mentalinc.watcher.database.SeriesDAO;
import nz.mentalinc.watcher.domain.Episode;
import nz.mentalinc.watcher.domain.Feed;
import nz.mentalinc.watcher.domain.FeedItem;
import nz.mentalinc.watcher.domain.User;
import nz.mentalinc.watcher.enums.EpisodeType;
import nz.mentalinc.watcher.exception.FeedUrlBuildingFaildException;
import nz.mentalinc.watcher.exception.InternetConnectivityException;
import nz.mentalinc.watcher.exception.LoginFailedException;
import nz.mentalinc.watcher.exception.ShowUpdateFailedException;
import nz.mentalinc.watcher.http.HttpClientProvider;
import nz.mentalinc.watcher.utils.DateUtil;
import okhttp3.Response;


public class EpisodesService {
    private static final String LOG_TAG = EpisodesService.class.getSimpleName();
    private static final String CHECKED = "checked";
    private static final String DESCRIPTION = "description";
    private static final String CHANNEL = "channel";
    private final UserService userService;
    private boolean fullUnwatchedDownloaded;


    public EpisodesService() {
        userService = new UserService();
    }

    public List<Episode> retrieveEpisodes(EpisodeType episodesType, final User user) throws Exception {
        String encryptedPassword = userService.encryptPassword(user.getPassword());

        URL feedUrl;

        //downloadFullUnwatched list 
        if (Objects.equals(episodesType.toString(), "EPISODES_TO_WATCH")
                || Objects.equals(episodesType.toString(), "EPISODES_TO_ACQUIRE")) {

            Log.d(LOG_TAG, "MyEpisodeConstants.DAYS_BACK_ENABLED: " + MyEpisodeConstants.DAYS_BACK_ENABLED);
            Log.d(LOG_TAG, "MyEpisodeConstants.DAYS_BACK_CP: " + MyEpisodeConstants.DAYS_BACK_CP);
            Log.d(LOG_TAG, "MyEpisodeConstants.CACHE_EPISODES_ENABLED: " + MyEpisodeConstants.CACHE_EPISODES_ENABLED);

            boolean isWatch = Objects.equals(episodesType.toString(), "EPISODES_TO_WATCH");
            String cacheFile = isWatch ? "Watch.xml" : "Acquire.xml";

            if (MyEpisodeConstants.DAYS_BACK_ENABLED) {

                if (MyEpisodeConstants.CACHE_EPISODES_ENABLED) {
                    Log.d(LOG_TAG, "Cache is enabled, read from disk");
                    String cachedXml = ReadFile(cacheFile);

                    //xml file not found
                    if (cachedXml.equalsIgnoreCase("FileNotFound")) {
                        Log.d(LOG_TAG, "No cached file found. Downloading...");
                        downloadFullUnwatched(user);

                        //write the xml to disk for future use
                        String xmlToCache = isWatch ? MyEpisodeConstants.EXTENDED_EPISODES_XML : MyEpisodeConstants.EXTENDED_EPISODES_XML_ACQUIRE;
                        FileOutputStream fos = MyEpisodeConstants.CONTEXT.openFileOutput(cacheFile, 0); //Mode_PRIVATE
                        fos.write(xmlToCache.getBytes());
                        fos.close();
                        Log.d(LOG_TAG, cacheFile + " saved to disk");
                    } else {
                        if (isWatch) {
                            MyEpisodeConstants.EXTENDED_EPISODES_XML = cachedXml;
                        } else {
                            MyEpisodeConstants.EXTENDED_EPISODES_XML_ACQUIRE = cachedXml;
                        }
                    }
                } else {
                    Log.d(LOG_TAG, "Cache is disabled, download from Internet");
                    downloadFullUnwatched(user);
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

        List<Episode> episodes = new ArrayList<>(rssFeed.getItems().size());

        AppDatabase database = AppDatabase.getInstance(nz.mentalinc.watcher.activities.HomeActivity.getContext().getApplicationContext());
        SeriesDAO seriesDAO = database.getSeriesDAO();

        // Batch load episode runtimes into a map (avoids per-episode queries)
        Map<String, EpisodeRuntime> runtimeMap = new HashMap<>();
        if (MyEpisodeConstants.SHOW_RUNTIME_ENABLED) {
            List<String> allIds = new ArrayList<>();
            for (FeedItem item : rssFeed.getItems()) {
                allIds.add(item.getGuid().split("-")[0].trim());
            }
            for (EpisodeRuntime rt : seriesDAO.getEpisodeRuntimeWithMyEpsIds(allIds)) {
                runtimeMap.put(rt.showMyEpsID, rt);
            }
        }

        for (FeedItem item : rssFeed.getItems()) {
            Episode episode = new Episode();

            String guid = item.getGuid();
            String myEpisodeID = guid.split("-")[0].trim();


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

                    try {
                        airDate = parseDate(airDateString);
                    } catch (Exception e) {
                        airDate = DateUtil.convertToDate(airDateString);
                    }

                    episode.setAirDate(airDate != null ? airDate.getTime() : 0);
                    episode.setMyEpisodeID(myEpisodeID);
                    //episode.setTVMazeWebSite(item.getLink());


                    //episode.setShowName(episode.getShowName() +" - "+ showRuntime.showRuntime + " mins");
                    try {

                        //RunTimeEnable = true = show runtime
                        if (MyEpisodeConstants.SHOW_RUNTIME_ENABLED) {
                            EpisodeRuntime showRuntime = runtimeMap.get(episode.getMyEpisodeID());
                            //TODO this line is what sets the title to have the runtime. This was only added to make sure the sort worked on episodes with runtime enabled
                            //episode.setShowName(showRuntime.getShowRuntime() + " mins" + " - " + episode.getShowName());
                            episode.setShowName(episode.getShowName());
                            if (showRuntime != null) {
                                episode.setTVMazeWebSite(TV_MAZE_SHOWS_URL + showRuntime.getShowTVMazeID());
                            }

                        } else {
                            episode.setShowName(episode.getShowName());

                        }

                    } catch (NullPointerException e) {
                        episode.setShowName(episode.getShowName());
                        String message = "Problem reading runtime for " + episode.getName();
                        Log.e(LOG_TAG, message);
                    }

                    //   Log.d(LOG_TAG,"Episode RunTime: " + episode.getShowName() + "  " + showRuntime.showRuntime);

//                    Log.d(LOG_TAG, "Episode from feed: ID=" + episode.getMyEpisodeID() + " " + episode.getShowName() + " - S" + episode.getSeasonString() + "E" + episode.getEpisodeString());
                } else if (episodeInfo.length == MyEpisodeConstants.FEED_TITLE_EPISODE_FIELDS - 1) {
                    //Solves problem mentioned in Issue 20
                    episode.setName(episodeInfo[2].trim() + "...");
                    episode.setMyEpisodeID(myEpisodeID);
                    //episode.setTVMazeWebSite(item.getLink());

                    EpisodeRuntime showRuntime = runtimeMap.get(episode.getMyEpisodeID());
                    //TODO this line is what sets the title to have the runtime. This was only added to make sure the sort worked on episodes with runtime enabled
                    //episode.setShowName(showRuntime.getShowRuntime() + " mins" + " - " + episode.getShowName());
                    episode.setShowName(episode.getShowName());
                    if (showRuntime != null) {
                        episode.setTVMazeWebSite(TV_MAZE_SHOWS_URL + showRuntime.getShowTVMazeID());
                    }

                    //episode.setTVMazeWebSite("Link to episode description coming soon");
                    //episode.setTVMazeWebSite(ShowsEpisodeLink(seriesDAO.getTvmazeShowID(episode.getMyEpisodeID()).getShowTVMazeID(), episode.getSeason(), episode.getEpisode()));

                } else {
                    String message = "Problem parsing a feed item. Feed details: " + item;
                    Log.e(LOG_TAG, message);
                }


                if (episodesType.equals(EpisodeType.EPISODES_COMING)) {
                    Calendar rightNow = Calendar.getInstance();
                    rightNow.add(Calendar.DATE, -1);
                    Date yesterday = rightNow.getTime();
                    if (airDate != null && airDate.after(yesterday)) {
                        episodes.add(episode);
                    }
                } else if (episodesType.equals(EpisodeType.EPISODES_TO_ACQUIRE)) {
                    Calendar rightNow = Calendar.getInstance();
                    if (airDate == null || !airDate.after(rightNow.getTime())) {
                        episodes.add(episode);
                    }
                } else {
                    episodes.add(episode);
                }
            }
        }
        return episodes;
    }

    private String ReadFile(String FILENAME) {
        StringBuilder EpisodeXML = new StringBuilder();
        try {
            //String FILENAME = "Watch.xml";
            FileInputStream instream = MyEpisodeConstants.CONTEXT.openFileInput(FILENAME);

            File file = new File(MyEpisodeConstants.CONTEXT.getFilesDir(), FILENAME);

            Log.d(LOG_TAG, "Save file Path" + MyEpisodeConstants.CONTEXT.getFilesDir().toString());
            if (MyEpisodeConstants.isOnline()) {
                CacheUtils.deleteOldCacheFiles(file);
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

    public void watchedEpisode(Episode episode, User user) throws LoginFailedException, ShowUpdateFailedException, InternetConnectivityException {
        List<Episode> episodes = new ArrayList<>();
        episodes.add(episode);
        watchedEpisodes(episodes, user);
    }

    public void watchedEpisodes(List<Episode> episodes, User user) throws LoginFailedException, ShowUpdateFailedException, InternetConnectivityException {


        userService.login(user.getUsername(), user.getPassword());

        for (Episode episode : episodes) {
            markAnEpisode(0, episode);
            EpisodesController.getInstance().deleteEpisode(EpisodeType.EPISODES_COMING, episode);
            EpisodesController.getInstance().deleteEpisode(EpisodeType.EPISODES_TO_ACQUIRE, episode);
            EpisodesController.getInstance().deleteEpisode(EpisodeType.EPISODES_TO_WATCH, episode);
            //todo - remove episodes from the hashmap as well
            EpisodesController.getInstance().deleteShowEpisode(EpisodeType.EPISODES_TO_WATCH, episode);
            EpisodesController.getInstance().deleteShowEpisode(EpisodeType.EPISODES_TO_ACQUIRE, episode);
            EpisodesController.getInstance().deleteShowEpisode(EpisodeType.EPISODES_COMING, episode);
        }
    }

    public void acquireEpisode(Episode episode, User user) throws LoginFailedException, ShowUpdateFailedException, InternetConnectivityException {
        List<Episode> episodes = new ArrayList<>();
        episodes.add(episode);
        acquireEpisodes(episodes, user);
    }

    public void acquireEpisodes(List<Episode> episodes, User user) throws LoginFailedException, ShowUpdateFailedException, InternetConnectivityException {

        userService.login(user.getUsername(), user.getPassword());

        for (Episode episode : episodes) {
            markAnEpisode(1, episode);
            EpisodesController.getInstance().deleteEpisode(EpisodeType.EPISODES_TO_ACQUIRE, episode);
            EpisodesController.getInstance().addEpisode(EpisodeType.EPISODES_TO_WATCH, episode);
            EpisodesController.getInstance().deleteShowEpisode(EpisodeType.EPISODES_TO_ACQUIRE, episode);
            List<Episode> tempList = EpisodesController.getInstance().getShowTypeEpisodes(EpisodeType.WATCH_BY_SHOW, episode.getMyEpisodeID());
            if (tempList == null) {
                tempList = new ArrayList<>();
                tempList.add(episode);
            }else{
                tempList.add(episode);
            }
            EpisodesController.getInstance().AddToWatchShow(tempList);
        }
    }

    private void markAnEpisode(int EpisodeStatus, Episode episode) throws ShowUpdateFailedException, InternetConnectivityException {
        String urlRep = (EpisodeStatus == 0 ? MyEpisodeConstants.MYEPISODES_UPDATE_WATCH :
                MyEpisodeConstants.MYEPISODES_UPDATE_ACQUIRE)
                .replace(MyEpisodeConstants.MYEPISODES_UPDATE_PAGE_EPISODE_REPLACEMENT, String.valueOf(episode.getEpisode()))
                .replace(MyEpisodeConstants.MYEPISODES_UPDATE_PAGE_SEASON_REPLACEMENT, String.valueOf(episode.getSeason()))
                .replace(MyEpisodeConstants.MYEPISODES_UPDATE_PAGE_SHOWID_REPLACEMENT, episode.getMyEpisodeID());

        try (Response response = HttpClientProvider.getInstance().get(urlRep)) {
            if (!response.isSuccessful()) {
                String message = "Updating the show status failed with HTTP " + response.code() + " for URL " + urlRep;
                Log.w(LOG_TAG, message);
                throw new ShowUpdateFailedException(message);
            }
            Log.i(LOG_TAG, "Successfully updated the show from url " + urlRep + " (" + episode + ")");
        } catch (IOException e) {
            String message = "Updating the show status failed for URL " + urlRep;
            Log.w(LOG_TAG, message, e);
            throw new ShowUpdateFailedException(message, e);
        }
    }


    /*
     * parse the views.php page to show a full list of unwatched apps
     */

    private synchronized StringWriter downloadFullUnwatched(User user) throws LoginFailedException, ShowUpdateFailedException, InternetConnectivityException {
        String urlRep = MyEpisodeConstants.MYEPISODES_FULL_UNWATCHED_LISTING_TABLE;
        userService.login(user.getUsername(), user.getPassword());

        StringWriter sw = new StringWriter();

        try {
            String[] controlPanelSettings = getDaysBack();
            setDaysBack(controlPanelSettings, false);

            Log.d(LOG_TAG, "DOWNLOADING FULL LIST");
            String htmlContent = HttpClientProvider.getInstance().getBody(urlRep);

            StringBuilder HTML = new StringBuilder(htmlContent);

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
                StringWriter swAcquire = new StringWriter();
                XmlSerializer xsAcquire = Xml.newSerializer();

                xs.setOutput(sw);
                xs.startDocument(null, null);
                xs.startTag(null, CHANNEL);

                xsAcquire.setOutput(swAcquire);
                xsAcquire.startDocument(null, null);
                xsAcquire.startTag(null, CHANNEL);

                for (String a : EpisodeTable) {
                    //split each column into a array
                    if (a.isEmpty()) {
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

                       // This row spams logcat when processing the html file to create the RSS files.
                        // Log.d("Serier EP", "|" + rowProcess[3]);

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
                        episodeName = episodeName.substring(1);
                        //	Log.d(LOG_TAG, "EpisodeName: " + rowProcess[4].toString());

                        //Get episode link - doesn't work yet.
                        int indexEpLink = rowProcess[4].indexOf("a href=") + 8;
                        String episodeLink = rowProcess[4].substring(indexEpLink);
                        int indexEpLink1 = episodeLink.indexOf("\"");

                        //get air date
                        int indexAirDate = rowProcess[0].length() - 15;
                        String airDate = rowProcess[0].substring(indexAirDate, indexAirDate + 11);

                        //Get guid
                        int indexGUID = rowProcess[5].indexOf("name=") + 7;
                        String guid = rowProcess[5].substring(indexGUID);
                        int indexGUID1 = guid.indexOf("\"");
                        guid = guid.substring(0, indexGUID1);
                        if (rowProcess[5].contains(CHECKED) && !rowProcess[6].contains(CHECKED)) {

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

                            xs.startTag(null, DESCRIPTION);
                            xs.endTag(null, DESCRIPTION);

                            xs.endTag(null, "item");
                        } else {
                            //Log.d(LOG_TAG, "Already watched or Not Acquired not adding to rss: [ " + Show + " ]" + "[ " + SeriesEp + " ]" + "[ " + EpisodeName + " ]" + "[ " + AirDate + " ]");
                        }
                        if (!rowProcess[5].contains(CHECKED)) {

                            String headerRow = "[ " + show + " ]" + "[ " + seriesEp + " ]" + "[ " + episodeName + " ]" + "[ " + airDate + " ]";

                            xsAcquire.startTag(null, "item");
                            xsAcquire.startTag(null, "guid");
                            xsAcquire.text(guid);
                            xsAcquire.endTag(null, "guid");

                            xsAcquire.startTag(null, "title");
                            xsAcquire.text(headerRow);
                            xsAcquire.endTag(null, "title");

                            xsAcquire.startTag(null, "link");
                            xsAcquire.text(episodeLink);
                            xsAcquire.endTag(null, "link");

                            xsAcquire.startTag(null, DESCRIPTION);
                            xsAcquire.endTag(null, DESCRIPTION);

                            xsAcquire.endTag(null, "item");

                           // useful for debugging, but mainly just spams logcat
                            // Log.d(LOG_TAG, "Not acquired adding to rss: [ " + show + " ]" + "[ " + seriesEp + " ]" + "[ " + episodeName + " ]" + "[ " + airDate + " ]");
                        }
                    }
                }

                xs.endTag(null, CHANNEL);
                xs.endDocument();

                xsAcquire.endTag(null, CHANNEL);
                xsAcquire.endDocument();

                MyEpisodeConstants.EXTENDED_EPISODES_XML = sw.toString();
                MyEpisodeConstants.EXTENDED_EPISODES_XML_ACQUIRE = swAcquire.toString();

                Log.d(LOG_TAG, "Finished Download and RSS built");
                Log.d(LOG_TAG, "Resetting  control panel settings");
                setDaysBack(controlPanelSettings, true);
            
        } catch (Exception e) {
            String message = "Error downloading and processing " + urlRep;
            Log.w(LOG_TAG, message, e);
            throw new ShowUpdateFailedException(message, e);
        }

        fullUnwatchedDownloaded = true;
        return sw;
    }

    //get the users web browser settings to keep them the same
    private String[] getDaysBack() {
        String[] controlPanelSettings = new String[20];

        String action = "Save";

        try {
            String response = HttpClientProvider.getInstance().getBody(MyEpisodeConstants.MYEPISODES_CONTROL_PANEL);
            String html = response;
            int cursor = 0;
            int idx;

            idx = html.indexOf("name=\"ce_dback\" value=\"", cursor);
            cursor = idx + 23;
            String ce_dback = html.substring(cursor, html.indexOf("\"", cursor));
            cursor = html.indexOf("\"", cursor) + 1;

            idx = html.indexOf("name=\"eps_time_offset\" value=\"", cursor);
            cursor = idx + 30;
            String eps_time_offset = html.substring(cursor, html.indexOf("\"", cursor));
            cursor = html.indexOf("\"", cursor) + 1;

            idx = html.indexOf("name=\"dateformat\" value=\"", cursor);
            cursor = idx + 25;
            String dateformat = html.substring(cursor, html.indexOf("\"", cursor));
            cursor = html.indexOf("\"", cursor) + 1;

            idx = html.indexOf("name=\"timeformat\" value=\"", cursor);
            cursor = idx + 25;
            String timeformat = html.substring(cursor, html.indexOf("\"", cursor));
            cursor = html.indexOf("\"", cursor) + 1;

            idx = html.indexOf("name=\"eps_number_format\" value=\"", cursor);
            cursor = idx + 32;
            String eps_number_format = html.substring(cursor, html.indexOf("\"", cursor));
            cursor = html.indexOf("\"", cursor) + 1;

            idx = html.indexOf("name=\"ce_dforward\" value=\"", cursor);
            cursor = idx + 26;
            String ce_dforward = html.substring(cursor, html.indexOf("\"", cursor));
            cursor = html.indexOf("\"", cursor) + 1;

            idx = html.indexOf("'link1');\" type=\"text\" value=\"", cursor);
            cursor = idx + 30;
            String colorpast1 = html.substring(cursor, html.indexOf("\"", cursor));
            cursor = html.indexOf("\"", cursor) + 1;

            idx = html.indexOf("'link2');\" type=\"text\" value=\"", cursor);
            cursor = idx + 30;
            String colorpast2 = html.substring(cursor, html.indexOf("\"", cursor));
            cursor = html.indexOf("\"", cursor) + 1;

            idx = html.indexOf("'link3');\" type=\"text\" value=\"", cursor);
            cursor = idx + 30;
            String colortoday = html.substring(cursor, html.indexOf("\"", cursor));
            cursor = html.indexOf("\"", cursor) + 1;

            idx = html.indexOf("'link4');\" type=\"text\" value=\"", cursor);
            cursor = idx + 30;
            String color1 = html.substring(cursor, html.indexOf("\"", cursor));
            cursor = html.indexOf("\"", cursor) + 1;

            idx = html.indexOf("'link5');\" type=\"text\" value=\"", cursor);
            cursor = idx + 30;
            String color2 = html.substring(cursor, html.indexOf("\"", cursor));
            cursor = html.indexOf("\"", cursor) + 1;

            idx = html.indexOf("'link6');\" type=\"text\" value=\"", cursor);
            cursor = idx + 30;
            String colorhover = html.substring(cursor, html.indexOf("\"", cursor));
            cursor = html.indexOf("\"", cursor) + 1;

            idx = html.indexOf("name=\"sw_acquire_delay\" value=\"", cursor);
            cursor = idx + 31;
            String sw_acquire_delay = html.substring(cursor, html.indexOf("\"", cursor));
            cursor = html.indexOf("\"", cursor) + 1;

            idx = html.indexOf("name=\"cal_firstday\"", cursor);
            cursor = idx;
            String cal_firstday = html.substring(html.indexOf("selected", cursor) - 3, html.indexOf("selected", cursor) - 2);

            String eps_timezone = null;
            idx = html.indexOf("name=\"eps_timezone\"", cursor);
            if (idx != -1) {
                eps_timezone = html.substring(idx);
                int timeZoneSelectedIndex = eps_timezone.indexOf("</select>");
                if (timeZoneSelectedIndex != -1) {
                    String timezoneRange = html.substring(idx, idx + timeZoneSelectedIndex);
                    String[] splitTimeZones = timezoneRange.split("</option>");
                    for (String a : splitTimeZones) {
                        int selectedIndex = a.indexOf("selected");
                        if (selectedIndex > 1) {
                            eps_timezone = a.substring(a.indexOf(">") + 1);
                        }
                    }
                    cursor = idx + timeZoneSelectedIndex + "</select>".length();
                } else {
                    cursor = html.length();
                }
            } else {
                cursor = html.length();
            }

            idx = html.indexOf("name=\"loginpage\"", cursor);
            cursor = idx + 17;
            int loginpageSelectedIndex = html.indexOf("</select>", cursor);
            String loginpageRange = html.substring(cursor, loginpageSelectedIndex);
            String loginpage = "";
            String[] splitLoginpage = loginpageRange.split("</option>");
            for (String a : splitLoginpage) {
                int selectedIndex = a.indexOf("selected");
                if (selectedIndex > 1) {
                    loginpage = a.substring(a.indexOf("=") + 2);
                    loginpage = loginpage.substring(0, loginpage.indexOf("\""));
                }
            }
            cursor = loginpageSelectedIndex + "</select>".length();

            idx = html.indexOf("name=\"sw_hidefuture\"", cursor);
            String sw_hidefuture = html.substring(idx + 21, idx + 28).trim();
            sw_hidefuture = sw_hidefuture.equals(CHECKED) ? "on" : null;
            cursor = idx + 28;

            idx = html.indexOf("name=\"sw_presentonly\"", cursor);
            String sw_presentonly = html.substring(idx + 22, idx + 29).trim();
            sw_presentonly = sw_presentonly.equals(CHECKED) ? "on" : null;
            cursor = idx + 29;

            idx = html.indexOf("name=\"sw_currentseasononly\"", cursor);
            String sw_currentseasononly = html.substring(idx + 28, idx + 35).trim();
            sw_currentseasononly = sw_currentseasononly.equals(CHECKED) ? "on" : null;
            cursor = idx + 35;

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
        } catch (Exception e) {
            String message = "Error reading control panel settings";
            Log.e(LOG_TAG, message, e);
        }
        return controlPanelSettings;
    }

    private void setDaysBack(String[] controlPanelSettings, Boolean restore) {
        Log.d(LOG_TAG, "Setting number of days back");
        String[] controlPanelOrder = {
                "eps_timezone", "eps_time_offset", "dateformat", "timeformat",
                "eps_number_format", "ce_dback", "ce_dforward", "colorpast1",
                "colorpast2", "colortoday", "color1", "color2", "colorhover",
                "sw_acquire_delay", "cal_firstday", "action", "loginpage",
                "sw_hidefuture", "sw_presentonly", "sw_currentseasononly"
        };

        try {
            HashMap<String, String> postDataParams = new HashMap<>();
            for (int i = 0; i < controlPanelOrder.length; i++) {
                if (i == 5 && !restore && controlPanelSettings[i] != null) {
                    postDataParams.put(controlPanelOrder[i], MyEpisodeConstants.DAYS_BACK_CP);
                } else if (i == 6 && !restore && controlPanelSettings[i] != null) {
                    postDataParams.put(controlPanelOrder[i], MyEpisodeConstants.DAYS_FORWARD_CP);
                } else {
                    if (controlPanelSettings[i] != null) {
                        postDataParams.put(controlPanelOrder[i], controlPanelSettings[i]);
                    }
                }
            }
            HttpClientProvider.getInstance().postFormBody(MyEpisodeConstants.MYEPISODES_CONTROL_PANEL, postDataParams);
        } catch (Exception e) {
            String message = "Error setting days back";
            Log.e(LOG_TAG, message, e);
        }
    }


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
            default: //added for code quality
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


}
