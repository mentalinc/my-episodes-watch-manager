package nz.mentalinc.watcher.service;

import android.app.Activity;
import android.content.Intent;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.text.util.Linkify;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private static final ExecutorService RUNTIME_EXECUTOR = Executors.newFixedThreadPool(10, r -> {
        Thread t = new Thread(r, "ShowRuntimeEnricher");
        t.setDaemon(true);
        return t;
    });
    private static final Set<String> IN_FLIGHT_RUNTIME_IDS = ConcurrentHashMap.newKeySet();

    private static volatile Activity currentActivity;

    public static void setCurrentActivity(Activity activity) {
        currentActivity = activity;
    }

    public static Activity getCurrentActivity() {
        return currentActivity;
    }

    public ShowService() {
        userService = new UserService();
    }

    public interface OnRuntimeProgressListener {
        void onProgress(int processed, int total, String showName);
    }

    private List<Show> searchShowsFromApi(String apiKey, String search) throws IOException, JSONException {
        List<Show> shows = new ArrayList<>();
        int offset = 0;
        boolean hasMore = true;
        while (hasMore) {
            String url = MyEpisodeConstants.MYEPISODES_API_BASE_URL + "/v1/shows"
                    + "?search=" + Uri.encode(search, "utf-8")
                    + "&limit=1000&offset=" + offset;
            Map<String, String> headers = new HashMap<>();
            headers.put(MyEpisodeConstants.MYEPISODES_API_AUTH_HEADER,
                    MyEpisodeConstants.MYEPISODES_API_AUTH_SCHEME + apiKey);

            JSONObject page;
            try (Response response = HttpClientProvider.getInstance().get(url, headers)) {
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP " + response.code() + " for GET " + url);
                }
                String body = response.body() != null ? response.body().string() : "";
                page = new JSONObject(body);
            }

            JSONArray data = page.optJSONArray("data");
            if (data != null) {
                for (int i = 0; i < data.length(); i++) {
                    JSONObject show = data.optJSONObject(i);
                    if (show == null) continue;
                    String showId = String.valueOf(show.optInt("showid", 0));
                    String showName = show.optString("showname", "").trim();
                    if (!showId.equals("0") && !showName.isEmpty()) {
                        shows.add(new Show(showName, showId));
                    }
                }
            }

            JSONObject meta = page.optJSONObject("meta");
            int returned = data != null ? data.length() : 0;
            hasMore = meta != null && meta.optBoolean("has_more", false);
            int nextOffset = meta != null ? meta.optInt("offset", offset) + returned : offset + returned;
            if (returned == 0 || nextOffset <= offset) {
                break;
            }
            offset = nextOffset;
        }
        return shows;
    }

    public List<Show> searchShows(String search, User user) throws InternetConnectivityException, LoginFailedException {
 
        String apiKey = getApiKey();
        if (!apiKey.isEmpty()) {
            try {
                return searchShowsFromApi(apiKey, search);
            } catch (Exception e) {
                if (!MyEpisodeConstants.LEGACY_PARSING_ENABLED) {
                    notifyLegacyDisabled();
                    throw e instanceof LoginFailedException ? (LoginFailedException) e
                            : new LoginFailedException("Legacy RSS/HTML parsing is disabled", e);
                }
                Log.w(LOG_TAG, "MyEpisodes API search failed; falling back to the HTML search page", e);
            }
        } else if (!MyEpisodeConstants.LEGACY_PARSING_ENABLED) {
            notifyLegacyDisabled();
            throw new LoginFailedException("Legacy RSS/HTML parsing is disabled");
        }

        if (!MyEpisodeConstants.LEGACY_PARSING_ENABLED) {
            notifyLegacyDisabled();
            throw new LoginFailedException("Legacy RSS/HTML parsing is disabled");
        }

        userService.login(user.getUsername(), user.getPassword());

        String response = "";
        try {
            HashMap<String, String> postDataParams = new HashMap<>();
            postDataParams.put(MyEpisodeConstants.MYEPISODES_SEARCH_PAGE_PARAM_SHOW, search);
            postDataParams.put(MyEpisodeConstants.MYEPISODES_FORM_PARAM_ACTION, MyEpisodeConstants.MYEPISODES_SEARCH_PAGE_PARAM_ACTION_VALUE);
            HttpClientProvider.getInstance().addCsrfToken(MyEpisodeConstants.MYEPISODES_SEARCH_PAGE, postDataParams);

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
        if (!MyEpisodeConstants.LEGACY_PARSING_ENABLED) {
            return;
        }
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

            HttpClientProvider http = HttpClientProvider.getInstance();
            try (Response response = http.postFormEncoded(
                    MyEpisodeConstants.MYEPISODES_FULL_UNWATCHED_LISTING_TABLE,
                    urlParameters.toString(),
                    http.getCsrfHeaders())) {
                Log.d(LOG_TAG, "Reset episode filter, HTTP " + response.code());
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error resetting episode filter", e);
        }
    }

    public void addShow(String myEpsidodesShowId, User user) throws InternetConnectivityException, LoginFailedException, ShowAddFailedException {
 
        String apiKey = getApiKey();
        if (!apiKey.isEmpty()) {
            try {
                String url = MyEpisodeConstants.MYEPISODES_API_BASE_URL + "/v1/me/shows/" + myEpsidodesShowId;
                Map<String, String> headers = new HashMap<>();
                headers.put(MyEpisodeConstants.MYEPISODES_API_AUTH_HEADER,
                        MyEpisodeConstants.MYEPISODES_API_AUTH_SCHEME + apiKey);
                try (Response response = HttpClientProvider.getInstance().put(url, headers)) {
                    if (!response.isSuccessful()) {
                        throw new ShowAddFailedException("HTTP " + response.code() + " for PUT " + url);
                    }
                }
                return;
            } catch (ShowAddFailedException e) {
                if (!MyEpisodeConstants.LEGACY_PARSING_ENABLED) {
                    notifyLegacyDisabled();
                    throw e;
                }
                Log.w(LOG_TAG, "MyEpisodes API add show failed; falling back to the HTML page", e);
            } catch (IOException e) {
                if (!MyEpisodeConstants.LEGACY_PARSING_ENABLED) {
                    notifyLegacyDisabled();
                    throw new ShowAddFailedException("Legacy RSS/HTML parsing is disabled", e);
                }
                Log.w(LOG_TAG, "MyEpisodes API add show failed; falling back to the HTML page", e);
            }
        } else if (!MyEpisodeConstants.LEGACY_PARSING_ENABLED) {
            notifyLegacyDisabled();
            throw new LoginFailedException("Legacy RSS/HTML parsing is disabled");
        }

        if (!MyEpisodeConstants.LEGACY_PARSING_ENABLED) {
            notifyLegacyDisabled();
            throw new LoginFailedException("Legacy RSS/HTML parsing is disabled");
        }

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
        return getFavoriteOrIgnoredShows(user, showType, null);
    }

    public List<Show> getFavoriteOrIgnoredShows(User user, ShowType showType, OnRuntimeProgressListener listener) throws InternetConnectivityException, LoginFailedException {

        // The API is preferred because it returns the show id and name directly, but it is only
        // available when the user has saved an API key. Any failure falls back to the HTML page.
        String apiKey = getApiKey();
        List<String[]> allOptions = null;
        if (!apiKey.isEmpty()) {
            try {
                allOptions = getFavoriteOrIgnoredShowsOptionsFromApi(apiKey, showType);
                Log.d(LOG_TAG, allOptions.size() + " show(s) found from the MyEpisodes API");
            } catch (Exception e) {
                if (!MyEpisodeConstants.LEGACY_PARSING_ENABLED) {
                    notifyLegacyDisabled();
                    throw e instanceof LoginFailedException ? (LoginFailedException) e
                            : new LoginFailedException("Legacy RSS/HTML parsing is disabled", e);
                }
                Log.w(LOG_TAG, "MyEpisodes API request failed; falling back to the HTML manage page", e);
                allOptions = null;
            }
        } else if (!MyEpisodeConstants.LEGACY_PARSING_ENABLED) {
            notifyLegacyDisabled();
            throw new LoginFailedException("Legacy RSS/HTML parsing is disabled");
        }

        if (allOptions == null) {
            if (!MyEpisodeConstants.LEGACY_PARSING_ENABLED) {
                notifyLegacyDisabled();
                throw new LoginFailedException("Legacy RSS/HTML parsing is disabled");
            }
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

            allOptions = parseShowsHtml(responsePage, showType);
            Log.d(LOG_TAG, allOptions.size() + " show(s) found from " + responsePage.length() + " byte response");
        }

        List<Show> shows = loadShows(allOptions);

        if (listener != null) {
            // Synchronous enrichment for callers that track progress (e.g. HomeActivity).
            enrichRuntimes(shows, listener);
        } else if (MyEpisodeConstants.SHOW_RUNTIME_ENABLED) {
            // Decoupled background enrichment for callers that do not need progress.
            enrichRuntimesAsync(shows);
        }

        return shows;
    }

    private String getApiKey() {
        return MyEpisodeConstants.API_KEY;
    }

    /**
     * Maps the app's show list type to the API's {@code ignored} query value.
     */
    static String apiIgnoredValue(ShowType showType) {
        return showType == ShowType.IGNORED_SHOWS
                ? MyEpisodeConstants.MYEPISODES_API_IGNORED_DISMISSED
                : MyEpisodeConstants.MYEPISODES_API_IGNORED_FOLLOWED;
    }

    /**
     * Loads followed or dismissed shows from {@code GET /v1/me/shows}. The endpoint pages its
     * results, so this keeps requesting pages until {@code meta.has_more} is false.
     */
    private List<String[]> getFavoriteOrIgnoredShowsOptionsFromApi(String apiKey, ShowType showType) throws IOException, JSONException {
        String ignored = apiIgnoredValue(showType);

        Log.d(LOG_TAG,"Using API for shows list");

        List<String[]> allOptions = new ArrayList<>();
        int offset = 0;
        boolean hasMore = true;

        while (hasMore) {
            String url = MyEpisodeConstants.MYEPISODES_ME_SHOWS_API_URL
                    + "?" + MyEpisodeConstants.MYEPISODES_API_IGNORED_PARAM + "=" + ignored
                    + "&limit=" + MyEpisodeConstants.MYEPISODES_API_PAGE_LIMIT
                    + "&offset=" + offset;

            Map<String, String> headers = new HashMap<>();
            headers.put(MyEpisodeConstants.MYEPISODES_API_AUTH_HEADER,
                    MyEpisodeConstants.MYEPISODES_API_AUTH_SCHEME + apiKey);

            JSONObject page;
            try (Response response = HttpClientProvider.getInstance().get(url, headers)) {
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP " + response.code() + " for GET " + url);
                }
                String body = response.body() != null ? response.body().string() : "";
                page = new JSONObject(body);
            }

            JSONArray data = page.optJSONArray("data");
            int returned = data != null ? data.length() : 0;
            if (data != null) {
                for (int i = 0; i < data.length(); i++) {
                    JSONObject show = data.optJSONObject(i);
                    if (show == null) {
                        continue;
                    }

                    String showId = show.optString("showid", "").trim();
                    String showName = show.optString("showname", "").trim();
                    if (!showId.isEmpty()) {
                        allOptions.add(new String[]{showId, showName});
                    }
                }
            }

            JSONObject meta = page.optJSONObject("meta");
            hasMore = meta != null && meta.optBoolean("has_more", false);
            int nextOffset = meta != null ? meta.optInt("offset", offset) + returned : offset + returned;
            if (returned == 0 || nextOffset <= offset) {
                break;
            }
            offset = nextOffset;
        }

        return allOptions;
    }

    private List<String[]> parseShowsHtml(String html, ShowType showType) {
        List<String[]> allOptions = new ArrayList<>();

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
            return allOptions;
        }

        String selectTag = html.substring(startPosition);
        int endPosition = selectTag.indexOf(endTag);
        selectTag = selectTag.substring(0, endPosition);

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

        return allOptions;
    }

    /**
     * Loads the show list from {@code [showId, showName]} pairs collected from either the HTML
     * page or the JSON API. This is a pure mapping step; runtime enrichment is performed
     * separately by {@link #enrichRuntimes} / {@link #enrichRuntimesAsync}.
     */
    public List<Show> loadShows(List<String[]> allOptions) {
        return buildShows(allOptions);
    }

    /**
     * Pure mapping from {@code [showId, showName]} pairs to {@link Show} objects.
     */
    public List<Show> buildShows(List<String[]> allOptions) {
        List<Show> shows = new ArrayList<>();
        if (allOptions == null) {
            return shows;
        }
        for (String[] option : allOptions) {
            if (option == null || option.length < 2 || option[0] == null || option[0].isEmpty()) {
                continue;
            }
            shows.add(new Show(option[1], option[0]));
        }
        Log.d(LOG_TAG, "Found " + shows.size() + " show(s) to process");
        return shows;
    }

    /**
     * Backwards-compatible overload retaining the original listener parameter; enrichment is now
     * decoupled, so the listener is ignored here.
     */
    public List<Show> buildShows(List<String[]> allOptions, OnRuntimeProgressListener listener) {
        return buildShows(allOptions);
    }

    /**
     * Enriches the cached runtime data for the supplied shows synchronously.
     */
    public void enrichRuntimes(List<Show> shows) {
        enrichRuntimes(shows, null);
    }

    /**
     * Enriches the cached runtime data for the supplied shows synchronously. Rows that already
     * hold a usable runtime are left untouched; missing or unusable rows are fetched from
     * TVMaze and stored. Does not modify the supplied {@link Show} objects.
     */
    public void enrichRuntimes(List<Show> shows, OnRuntimeProgressListener listener) {
        if (!MyEpisodeConstants.SHOW_RUNTIME_ENABLED || shows == null || shows.isEmpty()) {
            return;
        }
        AppDatabase database = getDatabase();
        if (database == null) {
            return;
        }

        List<String> allShowIds = new ArrayList<>();
        for (Show show : shows) {
            allShowIds.add(show.getMyEpisodeID());
        }
        Map<String, EpisodeRuntime> runtimeMap = loadCachedRuntimes(allShowIds);

        int total = shows.size();
        java.util.concurrent.atomic.AtomicInteger processed = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(shows.size());

        for (Show show : shows) {
            RUNTIME_EXECUTOR.execute(() -> {
                try {
                    EpisodeRuntime cached = runtimeMap.get(show.getMyEpisodeID());
                    if (!hasUsableRuntime(cached)) {
                        if (IN_FLIGHT_RUNTIME_IDS.add(show.getMyEpisodeID())) {
                            try {
                                ShowsRuntime(show.getShowName(), show.getMyEpisodeID(), database);
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "Error fetching runtime for show", e);
                            } finally {
                                IN_FLIGHT_RUNTIME_IDS.remove(show.getMyEpisodeID());
                            }
                        }
                    }
                } finally {
                    int p = processed.incrementAndGet();
                    if (listener != null) {
                        listener.onProgress(p, total, show.getShowName());
                    }
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Schedules {@link #enrichRuntimes} on the dedicated daemon executor.
     */
    public void enrichRuntimesAsync(List<Show> shows) {
        if (!MyEpisodeConstants.SHOW_RUNTIME_ENABLED || shows == null || shows.isEmpty()) {
            return;
        }
        RUNTIME_EXECUTOR.execute(() -> enrichRuntimes(shows, null));
    }

    /**
     * Returns true when the cached runtime row actually contains usable runtime data.
     */
    public static boolean hasUsableRuntime(EpisodeRuntime runtime) {
        if (runtime == null) {
            return false;
        }
        // A show that TVMaze reported as missing (404) is cached as "NOT_FOUND"; treat that
        // as a usable (final) result so we do not keep retrying the lookup.
        if ("NOT_FOUND".equals(runtime.getShowTVMazeID())) {
            return true;
        }
        String value = runtime.getShowRuntime();
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.isEmpty() && !"null".equalsIgnoreCase(trimmed);
    }

    public AppDatabase getDatabase() {
        if (MyEpisodeConstants.CONTEXT == null) {
            return null;
        }
        return AppDatabase.getInstance(MyEpisodeConstants.CONTEXT);
    }

    public Map<String, EpisodeRuntime> loadCachedRuntimes(List<String> showIds) {
        Map<String, EpisodeRuntime> runtimeMap = new HashMap<>();
        if (!MyEpisodeConstants.SHOW_RUNTIME_ENABLED || showIds == null || showIds.isEmpty()) {
            return runtimeMap;
        }
        AppDatabase database = getDatabase();
        if (database == null) {
            return runtimeMap;
        }
        SeriesDAO dao = database.getSeriesDAO();
        final int chunkSize = 500;
        for (int i = 0; i < showIds.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, showIds.size());
            List<String> chunk = new ArrayList<>(showIds.subList(i, end));
            for (EpisodeRuntime rt : dao.getEpisodeRuntimeWithMyEpsIds(chunk)) {
                runtimeMap.put(rt.showMyEpsID, rt);
            }
        }
        return runtimeMap;
    }


    public void ShowsRuntime(String show, String myEpsID, AppDatabase database) {
        if (database == null) {
            return;
        }

        String tvMazeAPIURL = "https://api.tvmaze.com/singlesearch/shows?q=";

        Log.d(LOG_TAG, "Show getting searched: " + show);
        show = show.replace("#", "");
        if (show.startsWith("Error mins - ")) {
            show = show.substring("Error mins - ".length());
        }

        if (show.equals("Top Gear (UK)")) {
            show = "Top Gear";
        }
        tvMazeAPIURL += Uri.encode(show, "utf-8");

        try {
            String jsonString = fetchUrl(tvMazeAPIURL);

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
                if (showRuntimeString == null || "null".equals(showRuntimeString) || showRuntimeString.isEmpty()) {
                    try {
                        showRuntimeString = jObj.getString("averageRuntime");
                        if (showRuntimeString == null || "null".equals(showRuntimeString) || showRuntimeString.isEmpty()) {
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
                if (!"null".equals(jObj.getString(MyEpisodeConstants.TVMAZE_IMAGE_KEY))) {
                    showImageURL = jObj.getJSONObject(MyEpisodeConstants.TVMAZE_IMAGE_KEY).getString(MyEpisodeConstants.TVMAZE_IMAGE_SIZE_MEDIUM);
                }

                showImageURL = showImageURL.replace("http://", "https://");
                showSummary = stripHtmlTags(showSummary);

            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error parsing TVMaze JSON", e);
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
                Log.e(LOG_TAG, "Error fetching show data", e);
                return;
            }
            // A 404 from TVMaze means the single-search endpoint could not match the title.
            // Fall back to the broader /search/shows endpoint, honouring a (YYYY) suffix as a
            // year filter against the result's "premiered" field, and let the user pick if needed.
            String message = e.getMessage();
            if (message != null && message.contains("HTTP 404")) {
                Log.d(LOG_TAG, "Single show search returned 404 for: " + show + "; trying generic search");
                tryGenericTvMazeSearch(show, myEpsID, database);
            } else {
                Log.e(LOG_TAG, "Error fetching show data", e);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error fetching show data", e);
        }
    }


    /**
     * Fallback used when the exact single-search endpoint returns a 404. Queries the broader
     * {@code /search/shows} endpoint, optionally filtering by a trailing {@code (YYYY)} year
     * against the result's {@code premiered} field, and lets the user choose when ambiguous.
     */
    private void tryGenericTvMazeSearch(String originalShow, String myEpsID, AppDatabase database) {
        if (database == null) {
            return;
        }

        String year = null;
        Matcher yearMatcher = Pattern.compile("\\((\\d{4})\\)").matcher(originalShow);
        if (yearMatcher.find()) {
            year = yearMatcher.group(1);
        }
        String cleaned = originalShow;
        if (year != null) {
            cleaned = originalShow.replace(yearMatcher.group(0), "").trim();
        }
        if (cleaned.isEmpty()) {
            cleaned = originalShow;
        }

        String searchUrl = "https://api.tvmaze.com/search/shows?q=" + Uri.encode(cleaned, "utf-8");
        JSONArray results;
        try {
            results = new JSONArray(fetchUrl(searchUrl));
        } catch (Exception e) {
            Log.w(LOG_TAG, "Generic TVMaze search failed for: " + cleaned, e);
            cacheNotFound(myEpsID, originalShow, database);
            return;
        }

        List<JSONObject> candidates = new ArrayList<>();
        for (int i = 0; i < results.length(); i++) {
            try {
                JSONObject item = results.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                JSONObject showObj = item.optJSONObject("show");
                if (showObj == null) {
                    continue;
                }
                if (year != null) {
                    String premiered = showObj.optString("premiered", "");
                    String pYear = premiered.length() >= 4 ? premiered.substring(0, 4) : "";
                    if (!year.equals(pYear)) {
                        continue;
                    }
                }
                candidates.add(showObj);
            } catch (Exception ignored) {
                // Skip malformed entries.
            }
        }

        // If the year filter removed everything, fall back to the unfiltered list.
        if (candidates.isEmpty() && year != null) {
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.optJSONObject(i);
                if (item != null) {
                    JSONObject showObj = item.optJSONObject("show");
                    if (showObj != null) {
                        candidates.add(showObj);
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            Log.d(LOG_TAG, "No generic TVMaze matches for: " + originalShow);
            cacheNotFound(myEpsID, originalShow, database);
            return;
        }

        JSONObject selected = null;
        if (candidates.size() == 1) {
            selected = candidates.get(0);
        } else {
            selected = promptForShowSelection(candidates, originalShow);
        }

        if (selected == null) {
            cacheNotFound(myEpsID, originalShow, database);
            return;
        }

        storeTvMazeRuntime(myEpsID, originalShow, selected, database);
    }

    private void cacheNotFound(String myEpsID, String showName, AppDatabase database) {
        try {
            EpisodeRuntime notFound = new EpisodeRuntime();
            notFound.setshowMyepsID(myEpsID);
            notFound.setShowName(showName);
            notFound.setShowTVMazeID("NOT_FOUND");
            notFound.setShowRuntime("");
            database.getSeriesDAO().insert(notFound);
        } catch (Exception insertError) {
            Log.w(LOG_TAG, "Could not cache 404 result for " + showName, insertError);
        }
    }

    private JSONObject promptForShowSelection(List<JSONObject> candidates, String originalShow) {
        Activity activity = getCurrentActivity();
        if (activity == null) {
            // No foreground UI available; fall back to the first candidate.
            return candidates.isEmpty() ? null : candidates.get(0);
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<JSONObject> selected = new AtomicReference<>();

        activity.runOnUiThread(() -> {
            String[] items = new String[candidates.size()];
            for (int i = 0; i < candidates.size(); i++) {
                JSONObject s = candidates.get(i);
                String name = s.optString("name", "");
                String premiered = s.optString("premiered", "");
                String pYear = premiered.length() >= 4 ? premiered.substring(0, 4) : "";
                items[i] = name + (pYear.isEmpty() ? "" : " (" + pYear + ")")
                        + "  -  " + s.optString("url", "");
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Select the correct show for: " + originalShow);
            // Clickable link to MyEpisodes to support matching.
            android.text.SpannableString link = new android.text.SpannableString(
                    "MyEpisodes: https://www.myepisodes.com/search.php?tvshow=" + Uri.encode(originalShow));
            Linkify.addLinks(link, Linkify.WEB_URLS);
            builder.setMessage(link);
            builder.setItems(items, (dialog, which) -> {
                selected.set(candidates.get(which));
                dialog.dismiss();
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.setOnDismissListener(d -> latch.countDown());
            dialog.show();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return selected.get();
    }

    private void storeTvMazeRuntime(String myEpsID, String originalShow, JSONObject tvmazeShow, AppDatabase database) {
        try {
            String tvdbId = "";
            if (tvmazeShow.has("externals")) {
                JSONObject externals = tvmazeShow.optJSONObject("externals");
                if (externals != null) {
                    tvdbId = externals.optString("thetvdb", "");
                }
            }

            JSONObject details;
            if (!tvdbId.isEmpty() && !"null".equals(tvdbId)) {
                details = new JSONObject(fetchUrl("https://api.tvmaze.com/lookup/shows?thetvdb=" + tvdbId));
            } else {
                details = new JSONObject(fetchUrl("https://api.tvmaze.com/shows/" + tvmazeShow.optString("id")));
            }

            String showNameString = details.optString("name", originalShow);
            String showRuntimeString = details.optString("runtime", "");
            if (showRuntimeString == null || "null".equals(showRuntimeString) || showRuntimeString.isEmpty()) {
                showRuntimeString = details.optString("averageRuntime", "");
                if (showRuntimeString == null || "null".equals(showRuntimeString) || showRuntimeString.isEmpty()) {
                    showRuntimeString = "";
                }
            }
            String tvmazeShowID = details.optString("id", tvmazeShow.optString("id", ""));
            String showSummary = stripHtmlTags(details.optString("summary", ""));
            String showURL = details.optString("url", "");
            String officialSite = details.optString(MyEpisodeConstants.OFFICIAL_SITE, "");
            String showImageURL = "";
            if (details.has(MyEpisodeConstants.TVMAZE_IMAGE_KEY) && !details.isNull(MyEpisodeConstants.TVMAZE_IMAGE_KEY)) {
                showImageURL = details.optJSONObject(MyEpisodeConstants.TVMAZE_IMAGE_KEY).optString(MyEpisodeConstants.TVMAZE_IMAGE_SIZE_MEDIUM, "");
            }
            showImageURL = showImageURL.replace("http://", "https://");

            EpisodeRuntime epsRunTime = new EpisodeRuntime();
            epsRunTime.setshowMyepsID(myEpsID);
            epsRunTime.setShowName(showNameString);
            epsRunTime.setShowTVMazeID(tvmazeShowID);
            epsRunTime.setShowRuntime(showRuntimeString);
            epsRunTime.setShowSummary(showSummary);
            epsRunTime.setShowURL(showURL);
            epsRunTime.setOfficialSite(officialSite);
            epsRunTime.setShowImageURL(showImageURL);

            database.getSeriesDAO().insert(epsRunTime);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error fetching runtime after generic search", e);
            cacheNotFound(myEpsID, originalShow, database);
        }
    }

    public List<Show> markShow(User user, Show show, ShowAction showAction, ShowType showType) throws LoginFailedException, InternetConnectivityException {

        // Prefer the API when a key is saved. It needs a write-scoped key, so a failure here
        // (for example a read-only key) falls through to the existing HTML form below.
        String apiKey = getApiKey();
        if (!apiKey.isEmpty() && markShowViaApi(apiKey, show, showAction)) {
            Log.d(LOG_TAG, "Show " + show.getMyEpisodeID() + " updated via the MyEpisodes API");
            return getFavoriteOrIgnoredShows(user, showType);
        }

        if (!MyEpisodeConstants.LEGACY_PARSING_ENABLED) {
            notifyLegacyDisabled();
            throw new LoginFailedException("Legacy RSS/HTML parsing is disabled");
        }

        userService.login(user.getUsername(), user.getPassword());

        try {
            // These actions change state, so they must be sent as a POST with a CSRF token
            // rather than as a GET link that any page can trigger. The parameters are kept in
            // the query string as well as the body for compatibility with the site's handler.
            String url = MyEpisodeConstants.MYEPISODES_FAVO_REMOVE_ULR + show.getMyEpisodeID();
            HashMap<String, String> params = new HashMap<>();
            params.put("showid", show.getMyEpisodeID());

            switch (showAction) {
                case IGNORE:
                    Log.d(LOG_TAG, "IGNORING SHOWS");
                    url = MyEpisodeConstants.MYEPISODES_FAVO_IGNORE_URL + show.getMyEpisodeID();
                    params.put("action", "Ignore");
                    params.put("Ignore", "1");
                    break;
                case UNIGNORE:
                    Log.d(LOG_TAG, "UNIGNORING SHOWS");
                    url = MyEpisodeConstants.MYEPISODES_FAVO_UNIGNORE_URL + show.getMyEpisodeID();
                    params.put("action", "Ignore");
                    params.put("Ignore", "0");
                    break;
                case DELETE:
                    Log.d(LOG_TAG, "DELETING SHOWS");
                    url = MyEpisodeConstants.MYEPISODES_FAVO_REMOVE_ULR + show.getMyEpisodeID();
                    params.put("action", "Remove");
                    params.put("Remove", "1");
                    break;
                default: //added for code quality
            }

            // Use the shared client so the session cookie (including myeps_sso) is sent.
            HttpClientProvider http = HttpClientProvider.getInstance();
            http.addCsrfToken(MyEpisodeConstants.MYEPISODES_FAVO_IGNORE_PAGE, params);
            try (Response response = http.postForm(url, params)) {
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP " + response.code() + " for " + url);
                }
                Log.d(LOG_TAG, "Mark show response: HTTP " + response.code());
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

    /**
     * Applies an ignore, unignore or delete action through the JSON API.
     *
     * @return true when the API accepted the change, false when the caller should fall back to
     * the HTML form.
     */
    private boolean markShowViaApi(String apiKey, Show show, ShowAction showAction) {
        String url = MyEpisodeConstants.MYEPISODES_ME_SHOW_API_URL + show.getMyEpisodeID();
        Map<String, String> headers = new HashMap<>();
        headers.put(MyEpisodeConstants.MYEPISODES_API_AUTH_HEADER,
                MyEpisodeConstants.MYEPISODES_API_AUTH_SCHEME + apiKey);

        HttpClientProvider http = HttpClientProvider.getInstance();
        try {
            if (showAction == ShowAction.DELETE) {
                try (Response response = http.delete(url, headers)) {
                    return isApiResponseSuccessful(response, "DELETE " + url);
                }
            }

            if (showAction == ShowAction.IGNORE || showAction == ShowAction.UNIGNORE) {
                String body = showAction == ShowAction.UNIGNORE
                        ? "{\"ignored\":false}"
                        : "{\"ignored\":true}";
                try (Response response = http.patchJson(url, body, headers)) {
                    return isApiResponseSuccessful(response, "PATCH " + url);
                }
            }
        } catch (IOException e) {
            Log.w(LOG_TAG, "MyEpisodes API show update failed; falling back to the HTML page", e);
        }

        return false;
    }

    private boolean isApiResponseSuccessful(Response response, String request) throws IOException {
        if (!response.isSuccessful()) {
            throw new IOException("HTTP " + response.code() + " for " + request);
        }
        return true;
    }

    public static JSONObject fetchTvMazeShowJson(String tvmazeId) throws IOException, InterruptedException, JSONException {
        String response = fetchUrl("https://api.tvmaze.com/shows/" + tvmazeId);
        return new JSONObject(response);
    }

    private static String fetchUrl(String urlString) throws IOException, InterruptedException {
        HttpClientProvider http = HttpClientProvider.getInstance();
        int retries = 0;
        while (true) {
            try (Response response = http.get(urlString)) {
                int code = response.code();
                if (code == 429 && retries < 3) {
                    retries++;
                    long backoff = parseRetryAfter(response.header("Retry-After"));
                    if (backoff <= 0) {
                        backoff = (long) Math.pow(2, retries) * 1000;
                    }
                    Thread.sleep(backoff);
                    continue;
                }
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP " + code + " for GET " + urlString);
                }
                return response.body() != null ? response.body().string() : "";
            }
        }
    }

    private static long parseRetryAfter(String header) {
        if (header == null) {
            return -1;
        }
        try {
            return Long.parseLong(header.trim()) * 1000L;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void notifyLegacyDisabled() {
        if (MyEpisodeConstants.CONTEXT == null) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(MyEpisodeConstants.CONTEXT, "Legacy RSS/HTML parsing is disabled", Toast.LENGTH_SHORT).show());
    }

    public static String stripHtml(String input) {
        if (input == null) return null;
        return input.replace("<p>", "").replace("</p>", "")
                    .replace("<b>", "").replace("</b>", "")
                    .replace("<i>", "").replace("</i>", "");
    }

    /**
     * Removes HTML tags from network-supplied text without using a regular expression.
     *
     * <p>The previous implementation used {@code replaceAll("<[^>]+>", "")}. Running a
     * regular expression over uncontrolled input is flagged by static analysis as a
     * potential super-linear (polynomial) match, so this scans the input once instead.
     * It keeps the same behaviour as the old expression: anything between a {@code '<'}
     * and the next {@code '>'} is removed, while an unclosed {@code '<'} leaves the rest
     * of the text untouched.
     */
    static String stripHtmlTags(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder(input.length());
        int index = 0;
        while (index < input.length()) {
            int tagStart = input.indexOf('<', index);
            if (tagStart < 0) {
                result.append(input, index, input.length());
                break;
            }
            int tagEnd = input.indexOf('>', tagStart + 1);
            if (tagEnd < 0) {
                result.append(input, index, input.length());
                break;
            }
            result.append(input, index, tagStart);
            index = tagEnd + 1;
        }
        return result.toString();
    }
}
