package eu.vranckaert.episodeWatcher.service;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import androidx.room.Room;
import eu.vranckaert.episodeWatcher.constants.MyEpisodeConstants;
import eu.vranckaert.episodeWatcher.database.AppDatabase;
import eu.vranckaert.episodeWatcher.database.SeriesDAO;
import eu.vranckaert.episodeWatcher.domain.Show;
import eu.vranckaert.episodeWatcher.domain.User;
import eu.vranckaert.episodeWatcher.enums.ShowAction;
import eu.vranckaert.episodeWatcher.enums.ShowType;
import eu.vranckaert.episodeWatcher.exception.InternetConnectivityException;
import eu.vranckaert.episodeWatcher.exception.LoginFailedException;
import eu.vranckaert.episodeWatcher.exception.ShowAddFailedException;
import eu.vranckaert.episodeWatcher.utils.StringUtils;


public class ShowService {
    private static final String LOG_TAG = ShowService.class.getSimpleName();

    private final UserService userService;


    public ShowService() {
        userService = new UserService();
    }

    public List<Show> searchShows(String search, User user) throws InternetConnectivityException, LoginFailedException {


        userService.login(user.getUsername(), user.getPassword());


        URL url;
        String response = "";
        java.net.CookieManager msCookieManager = new java.net.CookieManager();
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        try {
            url = new URL(MyEpisodeConstants.MYEPISODES_SEARCH_PAGE);

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

            HashMap postDataParams = new HashMap<String, String>() {{
                put(MyEpisodeConstants.MYEPISODES_SEARCH_PAGE_PARAM_SHOW, search);
                put(MyEpisodeConstants.MYEPISODES_FORM_PARAM_ACTION, MyEpisodeConstants.MYEPISODES_SEARCH_PAGE_PARAM_ACTION_VALUE);
            }};

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


        } catch (UnknownHostException e) {
            String message = "Could not connect to host.";
            Log.e(LOG_TAG, message, e);
            throw new InternetConnectivityException(message, e);
        } catch (IOException e) {
            String message = "Search on MyEpisodes failed.";
            Log.w(LOG_TAG, message, e);
            throw new LoginFailedException(message, e);

        } catch (Exception e) {
            String message = "Error";
            Log.e(LOG_TAG, message, e);
        }

        List<Show> shows = extractSearchResults(response);

        Log.d(LOG_TAG, shows.size() + " shows found for search value " + search);

        return shows;
    }


    /**
     * Extract a list of shows from the MyEpisodes.com HTML output!
     *
     * @param html The MyEpisodes.com HTML output
     * @return A List of {@link eu.vranckaert.episodeWatcher.domain.Show} instances.
     */
    private List<Show> extractSearchResults(String html) {
        List<Show> shows = new ArrayList<>();
        if (html.contains("No results found.")) {
            return shows;
        }
        String[] split = html.split(MyEpisodeConstants.MYEPISODES_SEARCH_RESULT_PAGE_SPLITTER_SEARCH_RESULTS);
        if (split.length == 2) {
            split = split[1].split(MyEpisodeConstants.MYEPISODES_SEARCH_RESULT_PAGE_SPLITTER_TABLE_END_TAG);
            if (split.length > 0) {
                split = split[0].split(MyEpisodeConstants.MYEPISODES_SEARCH_RESULT_PAGE_SPLITTER_TD_START_TAG);
                for (int i = 0; i < split.length; i++) {
                    if (i > 0) {
                        String showName;
                        String showId;

                        String htmlPart = split[i];
                        htmlPart = htmlPart.replace("href=\"views.php?type=epsbyshow&showid=", "");

                        //Get the showid
                        htmlPart = htmlPart.replace("href=\"/epsbyshow/", "");
                        int indexOfSlash = htmlPart.indexOf("/");
                        showId = htmlPart.substring(0, indexOfSlash);
                        int closingIndex = htmlPart.indexOf("\">");
                        htmlPart = htmlPart.substring(closingIndex + 2);

//                        //Get the showid
//                        String showSeperator = "\">";
//                        int showIdSeperatorIndex = StringUtils.indexOf(htmlPart, showSeperator);
//                        showId = htmlPart.substring(0, showIdSeperatorIndex);
//                        htmlPart = htmlPart.replace(showId + showSeperator, "");
                        //Get the showName
                        showName = htmlPart.substring(0, StringUtils.indexOf(htmlPart, "</a></td>"));

                        shows.add(new Show(showName, showId));
                    }
                }
            }
        }
        return shows;
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


    public void addShow(String myEpsidodesShowId, User user) throws InternetConnectivityException, LoginFailedException, ShowAddFailedException {

        userService.login(user.getUsername(), user.getPassword());
        String responsePage = "";
        int status;
        String URLString = MyEpisodeConstants.MYEPISODES_ADD_SHOW_PAGE + myEpsidodesShowId;

        try {
            URL url = new URL(URLString);

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            status = conn.getResponseCode();

            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = br.readLine()) != null) {
                responsePage += line;
            }

        } catch (UnknownHostException e) {
            String message = "Could not connect to host.";
            Log.e(LOG_TAG, message, e);
            throw new InternetConnectivityException(message, e);
        } catch (IOException e) {
            String message = "Adding the show status failed for URL " + URLString;
            Log.w(LOG_TAG, message, e);
            throw new ShowAddFailedException(message, e);
        }

        if (status != 200) {
            String message = "Adding the show status failed with status code " + status + " for URL " + URLString;
            Log.w(LOG_TAG, message);
            throw new ShowAddFailedException(message);
        } else {
            Log.i(LOG_TAG, "Successfully added the show from url " + URLString);
        }
    }

    public List<Show> getFavoriteOrIgnoredShows(User user, ShowType showType) throws InternetConnectivityException, LoginFailedException {

        userService.login(user.getUsername(), user.getPassword());
        String responsePage = "";

        try {
            URL url = new URL(MyEpisodeConstants.MYEPISODES_FAVO_IGNORE_PAGE);

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.getResponseCode();

            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = br.readLine()) != null) {
                responsePage += line;
            }
        } catch (UnknownHostException e) {
            String message = "Could not connect to host.";
            Log.e(LOG_TAG, message, e);
            throw new InternetConnectivityException(message, e);
        } catch (IOException e) {
            String message = "Search on MyEpisodes failed.";
            Log.w(LOG_TAG, message, e);
            throw new LoginFailedException(message, e);
        }

        List<Show> shows = parseShowsHtml(responsePage, showType);

        Log.d(LOG_TAG, shows.size() + " show(s) found!");

        return shows;
    }


    private List<Show> parseShowsHtml(String html, ShowType showType) {
        List<Show> shows = new ArrayList<>();

        String startTag = "<select id=\"";
        String endTag = "</select>";

        String optionStartTag = "<option value=\"";
        String optionEndTag = "</option>";

        switch (showType) {
            case FAVOURITE_SHOWS:
                startTag += "shows\"";
                break;
            case IGNORED_SHOWS:
                startTag += "ignored_shows\"";
                break;
        }
        int startPosition = html.indexOf(startTag);

        if (startPosition == -1) {
            return shows;
        }

        String selectTag = html.substring(startPosition, html.length());
        int endPosition = selectTag.indexOf(endTag);
        selectTag = selectTag.substring(0, endPosition);

        AppDatabase database = Room.databaseBuilder(eu.vranckaert.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                .allowMainThreadQueries()   //Allows room to do operation on main thread
                .fallbackToDestructiveMigration()
                .build();

        int ep = 1;

        while (selectTag.length() > 0) {
            int startPositionOption = selectTag.indexOf(optionStartTag);
            int endPositionOption = selectTag.indexOf(optionEndTag);

            if (startPositionOption == -1 || endPositionOption == -1 || endPositionOption < startPositionOption) {
                break;
            }

            String optionTag = selectTag.substring(startPositionOption + optionStartTag.length(), endPositionOption);
            selectTag = selectTag.replace(optionStartTag + optionTag + optionEndTag, "");

            String[] values = optionTag.split("\">");
            if (values.length != 2) {
                break;
            }

            Show show = new Show(values[1].trim(), values[0].trim());
            shows.add(show);
            Log.d(LOG_TAG, "Show found: " + show.getShowName() + " (" + show.getMyEpisodeID() + ")");

            //if the show has no runtime, AND runtime is enabled
            if (MyEpisodeConstants.SHOW_RUNTIME_ENABLED) {
                //check if the show runtime is already in the database
                // if not then go and get the runtime
                SeriesDAO seriesDAO = database.getSeriesDAO();
                EpisodeRuntime showRuntime = seriesDAO.getEpisodeRuntimeWithMyEpsId(show.getMyEpisodeID());

                if (showRuntime == null) {
                    Log.d(LOG_TAG, "Show NOT found in Database");
                    try {
                        ShowsRuntime(show.getShowName(), show.getMyEpisodeID(), database);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    //do nothing as already exists no need to add
                    // Log.d(LOG_TAG, "Show ID found in Database: " + showRuntime.showName + "(" + showRuntime.showMyEpsID + ")");
                }
            }
        }
        return shows;
    }


    private void ShowsRuntime(String show, String myEpsID, AppDatabase database) {

        HttpsURLConnection connection = null;
        BufferedReader reader = null;

        String tvMazeAPIURL = "https://api.tvmaze.com/singlesearch/shows?q=";
        //Add the show to the end of the URL
        //need to pull the show out of the list one at a time may?

        // for (int i = 0; i < show.size(); i++) {
        //for now just work with the first item will build to work with all in time.

        System.out.println(show);
        // Log.d("Response: ", "> " + show);
        show = show.replace("#", "");
        tvMazeAPIURL += show;

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
            String showNameString = "";
            String showRuntimeString = "";
            String tvmazeShowID = "";
            String showSummary = "";
            String showURL = "";
            String showImageURL = "";
            String officialSite = "";


            try {
                jObj = new JSONObject(jsonString);
                showNameString = jObj.getString("name");
                showRuntimeString = jObj.getString("runtime");
                tvmazeShowID = jObj.getString("id");
                showSummary = jObj.getString("summary");
                showURL = jObj.getString("url");
                officialSite = jObj.getString("officialSite");
                if (!jObj.getString("image").equals("null")) {
                    showImageURL = jObj.getJSONObject("image").getString("medium");
                }

                //change the http:// to https://
                showImageURL = showImageURL.replace("http://", "https://");

                showSummary = showSummary.replace("<p>", "");
                showSummary = showSummary.replace("</p>", "");
                showSummary = showSummary.replace("<b>", "");
                showSummary = showSummary.replace("</b>", "");
                showSummary = showSummary.replace("<i>", "");
                showSummary = showSummary.replace("</i>", "");


            } catch (JSONException e) {
                e.printStackTrace();
            }

            //now put the tv Show values into a database....
            SeriesDAO seriesDAO = database.getSeriesDAO();

            //Inserting an episodeRuntime
            EpisodeRuntime epsRunTime = new EpisodeRuntime();
            epsRunTime.setshowMyepsID(myEpsID);
            epsRunTime.setShowName(showNameString);
            epsRunTime.setShowTVMazeID(tvmazeShowID);
            epsRunTime.setShowRuntime(showRuntimeString);
            epsRunTime.setShowSummary(showSummary);
            epsRunTime.setShowURL(showURL);
            epsRunTime.setOfficialSite(officialSite);
            epsRunTime.setShowImageURL(showImageURL);

            //  Log.d("epsRunTime: ", epsRunTime.toString());

            seriesDAO.insert(epsRunTime);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            //database.close();
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
    }


    public List<Show> markShow(User user, Show show, ShowAction showAction, ShowType showType) throws LoginFailedException, InternetConnectivityException {

        userService.login(user.getUsername(), user.getPassword());

        try {
            URL url = new URL(MyEpisodeConstants.MYEPISODES_FAVO_REMOVE_ULR + show.getMyEpisodeID());

            switch (showAction) {
                case IGNORE:
                    Log.d(LOG_TAG, "IGNORING SHOWS");
                    url = new URL(MyEpisodeConstants.MYEPISODES_FAVO_IGNORE_URL + show.getMyEpisodeID());
                    break;
                case UNIGNORE:
                    Log.d(LOG_TAG, "UNIGNORING SHOWS");
                    url = new URL(MyEpisodeConstants.MYEPISODES_FAVO_UNIGNORE_URL + show.getMyEpisodeID());
                    break;
                case DELETE:
                    Log.d(LOG_TAG, "DELETING SHOWS");
                    url = new URL(MyEpisodeConstants.MYEPISODES_FAVO_REMOVE_ULR + show.getMyEpisodeID());
                    break;
            }

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.getResponseCode();


        } catch (UnknownHostException e) {
            String message = "Could not connect to host.";
            Log.e(LOG_TAG, message, e);
            throw new InternetConnectivityException(message, e);
        } catch (IOException e) {
            String message = "Marking shows on MyEpisodes failed.";
            Log.w(LOG_TAG, message, e);
            throw new LoginFailedException(message, e);
        }

        return getFavoriteOrIgnoredShows(user, showType);
    }
}
