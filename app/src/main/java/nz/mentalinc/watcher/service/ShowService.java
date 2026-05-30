package nz.mentalinc.watcher.service;

import android.net.Uri;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import nz.mentalinc.watcher.constants.MyEpisodeConstants;
import nz.mentalinc.watcher.database.AppDatabase;
import nz.mentalinc.watcher.database.SeriesDAO;
import nz.mentalinc.watcher.domain.Show;
import nz.mentalinc.watcher.domain.User;
import nz.mentalinc.watcher.enums.ShowAction;
import nz.mentalinc.watcher.enums.ShowType;
import nz.mentalinc.watcher.exception.InternetConnectivityException;
import nz.mentalinc.watcher.exception.LoginFailedException;
import nz.mentalinc.watcher.exception.ShowAddFailedException;
import nz.mentalinc.watcher.exception.ShowUpdateFailedException;
import nz.mentalinc.watcher.http.HttpClientProvider;
import nz.mentalinc.watcher.utils.StringUtils;
import okhttp3.Response;


public class ShowService {
    private static final String LOG_TAG = ShowService.class.getSimpleName();
    private static final String COULD_NOT_CONNECT_TO_HOST = "Could not connect to host.";
    private static final String SEARCH_ON_MYEPISODES_FAILED = "Search on MyEpisodes failed.";

    private final UserService userService;


    public ShowService() {
        userService = new UserService();
    }

    public interface OnRuntimeProgressListener {
        void onProgress(int processed, int total, String showName);
    }

    public List<Show> searchShows(String search, User user) throws InternetConnectivityException, LoginFailedException {

        userService.login(user.getUsername(), user.getPassword());

        String response = "";
        try {
            HashMap<String, String> postDataParams = new HashMap<>();
            postDataParams.put(MyEpisodeConstants.MYEPISODES_SEARCH_PAGE_PARAM_SHOW, search);
            postDataParams.put(MyEpisodeConstants.MYEPISODES_FORM_PARAM_ACTION, MyEpisodeConstants.MYEPISODES_SEARCH_PAGE_PARAM_ACTION_VALUE);

            try (Response resp = HttpClientProvider.getInstance().postForm(MyEpisodeConstants.MYEPISODES_SEARCH_PAGE, postDataParams)) {
                response = resp.isSuccessful() && resp.body() != null ? resp.body().string() : "";
            }

        } catch (UnknownHostException e) {
            String message = COULD_NOT_CONNECT_TO_HOST;
            Log.e(LOG_TAG, message, e);
            throw new InternetConnectivityException(message, e);
        } catch (IOException e) {
            String message = SEARCH_ON_MYEPISODES_FAILED;
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
     * @return A List of {@link nz.mentalinc.watcher.domain.Show} instances.
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

    public static void resetPageFilters(User user) {
        try {
            new UserService().login(user.getUsername(), user.getPassword());
            StringBuilder urlParameters = new StringBuilder();

            if (MyEpisodeConstants.SHOW_LISTING_UNACQUIRED_ENABLED) {
                if (urlParameters.length() > 0) urlParameters.append("&");
                urlParameters.append("eps_filters%5B%5D=1");
            }
            if (MyEpisodeConstants.SHOW_LISTING_UNWATCHED_ENABLED) {
                if (urlParameters.length() > 0) urlParameters.append("&");
                urlParameters.append("eps_filters%5B%5D=2");
            }
            if (MyEpisodeConstants.SHOW_LISTING_IGNORED_ENABLED) {
                if (urlParameters.length() > 0) urlParameters.append("&");
                urlParameters.append("eps_filters%5B%5D=4");
            }
            if (MyEpisodeConstants.SHOW_LISTING_PILOTS_ENABLED) {
                if (urlParameters.length() > 0) urlParameters.append("&");
                urlParameters.append("eps_filters%5B%5D=2048");
            }
            if (MyEpisodeConstants.SHOW_LISTING_LOCALIZED_AIRDATES__ENABLED) {
                if (urlParameters.length() > 0) urlParameters.append("&");
                urlParameters.append("eps_filters%5B%5D=4096");
            }

            byte[] postData = urlParameters.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            URL url = new URL(MyEpisodeConstants.MYEPISODES_FULL_UNWATCHED_LISTING_TABLE);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            try {
                conn.setDoOutput(true);
                conn.setInstanceFollowRedirects(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("charset", "utf-8");
                conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
                conn.setUseCaches(false);
                try (java.io.DataOutputStream wr = new java.io.DataOutputStream(conn.getOutputStream())) {
                    wr.write(postData);
                    wr.flush();
                }
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8), 8)) {
                    reader.readLine();
                }
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error resetting episode filter", e);
        }
    }

    public void addShow(String myEpsidodesShowId, User user) throws InternetConnectivityException, LoginFailedException, ShowAddFailedException {

        userService.login(user.getUsername(), user.getPassword());
        int status;
        String URLString = MyEpisodeConstants.MYEPISODES_ADD_SHOW_PAGE + myEpsidodesShowId;

        try {
            Response resp = HttpClientProvider.getInstance().get(URLString);
            status = resp.code();
            resp.close();

        } catch (UnknownHostException e) {
            String message = COULD_NOT_CONNECT_TO_HOST;
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
        String responsePage;

        try {
            responsePage = HttpClientProvider.getInstance().getBody(MyEpisodeConstants.MYEPISODES_FAVO_IGNORE_PAGE);
        } catch (UnknownHostException e) {
            String message = COULD_NOT_CONNECT_TO_HOST;
            Log.e(LOG_TAG, message, e);
            throw new InternetConnectivityException(message, e);
        } catch (IOException | ShowUpdateFailedException e) {
            String message = SEARCH_ON_MYEPISODES_FAILED;
            Log.w(LOG_TAG, message, e);
            throw new LoginFailedException(message, e);
        }

        List<Show> shows = parseShowsHtml(responsePage, showType);

        Log.d(LOG_TAG, shows.size() + " show(s) found!");

        return shows;
    }

    public List<Show> getFavoriteOrIgnoredShows(User user, ShowType showType, OnRuntimeProgressListener listener) throws InternetConnectivityException, LoginFailedException {

        userService.login(user.getUsername(), user.getPassword());
        String responsePage;

        try {
            responsePage = HttpClientProvider.getInstance().getBody(MyEpisodeConstants.MYEPISODES_FAVO_IGNORE_PAGE);
        } catch (UnknownHostException e) {
            String message = COULD_NOT_CONNECT_TO_HOST;
            Log.e(LOG_TAG, message, e);
            throw new InternetConnectivityException(message, e);
        } catch (IOException | ShowUpdateFailedException e) {
            String message = SEARCH_ON_MYEPISODES_FAILED;
            Log.w(LOG_TAG, message, e);
            throw new LoginFailedException(message, e);
        }

        List<Show> shows = parseShowsHtml(responsePage, showType, listener);

        Log.d(LOG_TAG, shows.size() + " show(s) found from " + responsePage.length() + " byte response");

        return shows;
    }

    private List<Show> parseShowsHtml(String html, ShowType showType) {
        return parseShowsHtml(html, showType, null);
    }

    private List<Show> parseShowsHtml(String html, ShowType showType, OnRuntimeProgressListener listener) {
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
            default:
        }
        int startPosition = html.indexOf(startTag);

        if (startPosition == -1) {
            return shows;
        }

        String selectTag = html.substring(startPosition);
        int endPosition = selectTag.indexOf(endTag);
        selectTag = selectTag.substring(0, endPosition);

        List<String[]> allOptions = new ArrayList<>();
        int searchIdx = 0;
        while (true) {
            int optStart = selectTag.indexOf(optionStartTag, searchIdx);
            if (optStart == -1) break;
            int optEnd = selectTag.indexOf(optionEndTag, optStart);
            if (optEnd == -1) break;

            String content = selectTag.substring(optStart + optionStartTag.length(), optEnd);
            String[] parts = content.split("\">");
            if (parts.length == 2) {
                allOptions.add(new String[]{parts[0].trim(), parts[1].trim()});
            }
            searchIdx = optEnd + optionEndTag.length();
        }

        int total = allOptions.size();
        Log.d(LOG_TAG, "Found " + total + " show options in the select list");

        AppDatabase database = AppDatabase.getInstance(nz.mentalinc.watcher.activities.HomeActivity.getContext().getApplicationContext());

        List<String> allShowIds = new ArrayList<>();
        for (String[] option : allOptions) {
            allShowIds.add(option[0]);
        }

        Map<String, EpisodeRuntime> runtimeMap = new HashMap<>();
        if (MyEpisodeConstants.SHOW_RUNTIME_ENABLED && !allShowIds.isEmpty()) {
            for (EpisodeRuntime rt : database.getSeriesDAO().getEpisodeRuntimeWithMyEpsIds(allShowIds)) {
                runtimeMap.put(rt.showMyEpsID, rt);
            }
        }

        int processed = 0;
        for (String[] option : allOptions) {
            if (option[0].isEmpty()) {
                continue;
            }

            Show show = new Show(option[1], option[0]);
            shows.add(show);
            Log.d(LOG_TAG, "Show found: " + show.getShowName() + " (" + show.getMyEpisodeID() + ")");

            processed++;
            if (listener != null) {
                listener.onProgress(processed, total, show.getShowName());
            }

            if (MyEpisodeConstants.SHOW_RUNTIME_ENABLED) {
                EpisodeRuntime showRuntime = runtimeMap.get(show.getMyEpisodeID());

                if (showRuntime == null) {
                    Log.d(LOG_TAG, "Show NOT found in Database");
                    try {
                        ShowsRuntime(show.getShowName(), show.getMyEpisodeID(), database);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return shows;
    }


    public void ShowsRuntime(String show, String myEpsID, AppDatabase database) {

        HttpsURLConnection connection = null;
        BufferedReader reader = null;

        String tvMazeAPIURL = "https://api.tvmaze.com/singlesearch/shows?q=";

        Log.d(LOG_TAG, "Show getting searched: " + show);
        show = show.replace("#", "");
        if (show.startsWith("Error mins - ")) {
            show = show.substring("Error mins - ".length());
        }

        if (show.equals("Top Gear (UK)")) {
            show = "Top Gear";
        }
        show = Uri.encode(show, "utf-8");
        tvMazeAPIURL += show;
        System.out.println(show);
        try {
            URL url = new URL(tvMazeAPIURL);
            connection = (HttpsURLConnection) url.openConnection();
            connection.connect();
            int code = connection.getResponseCode();
            Log.d(LOG_TAG, "API HTTP Status Code: " + code);

            int retries = 0;
            while (code == 429 && retries < 3) {
                retries++;
                long backoff = (long) Math.pow(2, retries) * 1000;
                Thread.sleep(backoff);
                connection = (HttpsURLConnection) url.openConnection();
                connection.connect();
                code = connection.getResponseCode();
            }

            InputStream stream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));

            StringBuilder buffer = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                buffer.append(line);
                buffer.append("\n");
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
                if (showRuntimeString == null || showRuntimeString.equals("null") || showRuntimeString.isEmpty()) {
                    try {
                        showRuntimeString = jObj.getString("averageRuntime");
                        if (showRuntimeString == null || showRuntimeString.equals("null") || showRuntimeString.isEmpty()) {
                            showRuntimeString = "";
                        }
                    } catch (JSONException e) {
                        showRuntimeString = "";
                    }
                }
                tvmazeShowID = jObj.getString("id");
                showSummary = jObj.getString("summary");
                showURL = jObj.getString("url");
                officialSite = jObj.getString(MyEpisodeConstants.OFFICIAL_SITE);
                if (!jObj.getString(MyEpisodeConstants.TVMAZE_IMAGE_KEY).equals("null")) {
                    showImageURL = jObj.getJSONObject(MyEpisodeConstants.TVMAZE_IMAGE_KEY).getString(MyEpisodeConstants.TVMAZE_IMAGE_SIZE_MEDIUM);
                }

                showImageURL = showImageURL.replace("http://", "https://");
                showSummary = showSummary.replaceAll("<[^>]+>", "");

            } catch (JSONException e) {
                e.printStackTrace();
            }

            SeriesDAO seriesDAO = database.getSeriesDAO();

            EpisodeRuntime epsRunTime = new EpisodeRuntime();
            epsRunTime.setshowMyepsID(myEpsID);
            epsRunTime.setShowName(showNameString);
            epsRunTime.setShowTVMazeID(tvmazeShowID);
            epsRunTime.setShowRuntime(showRuntimeString);
            epsRunTime.setShowSummary(showSummary);
            epsRunTime.setShowURL(showURL);
            epsRunTime.setOfficialSite(officialSite);
            epsRunTime.setShowImageURL(showImageURL);

            seriesDAO.insert(epsRunTime);

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            Log.e(LOG_TAG, "Error fetching show data", e);
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
                default: //added for code quality
            }

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            try {
                conn.getResponseCode();
            } finally {
                conn.disconnect();
            }

        } catch (UnknownHostException e) {
            String message = COULD_NOT_CONNECT_TO_HOST;
            Log.e(LOG_TAG, message, e);
            throw new InternetConnectivityException(message, e);
        } catch (IOException e) {
            String message = "Marking shows on MyEpisodes failed.";
            Log.w(LOG_TAG, message, e);
            throw new LoginFailedException(message, e);
        }

        return getFavoriteOrIgnoredShows(user, showType);
    }

    public static JSONObject fetchTvMazeShowJson(String tvmazeId) throws IOException, InterruptedException, JSONException {
        String response = fetchUrl("https://api.tvmaze.com/shows/" + tvmazeId);
        return new JSONObject(response);
    }

    private static String fetchUrl(String urlString) throws IOException, InterruptedException {
        HttpsURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpsURLConnection) url.openConnection();
            connection.connect();
            int code = connection.getResponseCode();
            int retries = 0;
            while (code == 429 && retries < 3) {
                retries++;
                long backoff = (long) Math.pow(2, retries) * 1000;
                Thread.sleep(backoff);
                connection = (HttpsURLConnection) url.openConnection();
                connection.connect();
                code = connection.getResponseCode();
            }
            InputStream stream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
                buffer.append("\n");
            }
            return buffer.toString();
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (IOException ignored) { }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static String stripHtml(String input) {
        if (input == null) return null;
        return input.replace("<p>", "").replace("</p>", "")
                    .replace("<b>", "").replace("</b>", "")
                    .replace("<i>", "").replace("</i>", "");
    }
}
