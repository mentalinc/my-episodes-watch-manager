package nz.mentalinc.watcher.constants;

import android.content.Context;

import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MyEpisodeConstants {
    public static final String UID_REPLACEMENT_STRING = "[UID]";
    public static final String PWD_REPLACEMENT_STRING = "[PWD]";
    private static final String FEED = "unwatched";
    private static final String SHOW_IGNORED = "0";

    public static String DAYS_BACK_CP = "365";
    public static String DAYS_FORWARD_CP = "20";
    public static boolean DAYS_BACK_ENABLED = false;
    public static boolean CACHE_EPISODES_ENABLED = false;
    public static boolean SHOW_RUNTIME_ENABLED = false;
    public static String CACHE_EPISODES_CACHE_AGE = "Disabled";

    public static boolean SHOW_LISTING_UNACQUIRED_ENABLED = false;
    public static boolean SHOW_LISTING_UNWATCHED_ENABLED = false;
    public static boolean SHOW_LISTING_IGNORED_ENABLED = false;
    public static boolean SHOW_LISTING_PILOTS_ENABLED = false;
    public static boolean SHOW_LISTING_LOCALIZED_AIRDATES__ENABLED = false;

    public static final String SHOW_URL = "showURL";
    public static final String OFFICIAL_SITE = "officialSite";
    public static final String SHOW_SUMMARY = "showSummary";
    public static final String SHOW_IMAGE_URL = "showImageURL";
    public static final String SHOW_RUNTIME = "ShowRuntime";
    public static final String SHOW_STATUS = "showStatus";
    public static final String SHOW_MYEPISODES_DEFAULT_SORT = "show_myepisodes_default_sort";
    public static final String CONNECT_ERROR = "Could not connect to host";
    public static final String PREF_USERNAME = "username";
    public static final String TVMAZE_IMAGE_KEY = "image";
    public static final String TVMAZE_IMAGE_SIZE_MEDIUM = "medium";


    public static Context CONTEXT = null;

    public static String EXTENDED_EPISODES_XML;
    public static String EXTENDED_EPISODES_XML_ACQUIRE;

    public static final String TV_MAZE_SHOWS_URL = "https://www.tvmaze.com/shows/";

    public static final String UNWATCHED_EPISODES_URL = "https://www.myepisodes.com/rss.php" +
            "?feed=" + FEED +
            "&showignored=" + SHOW_IGNORED +
            "&uid=" + UID_REPLACEMENT_STRING +
            "&pwdmd5=" + PWD_REPLACEMENT_STRING;

    public static final String UNAQUIRED_EPISODES_URL = "https://www.myepisodes.com/rss.php?feed=unacquired" +
            "&showignored=" + SHOW_IGNORED +
            "&uid=" + UID_REPLACEMENT_STRING +
            "&pwdmd5=" + PWD_REPLACEMENT_STRING;
    public static final String YESTERDAY_EPISODES_URL = "https://www.myepisodes.com/rss.php?feed=yesterday" +
            "&showignored=" + SHOW_IGNORED +
            "&onlyunacquired=1" +
            "&uid=" + UID_REPLACEMENT_STRING +
            "&pwdmd5=" + PWD_REPLACEMENT_STRING;
    public static final String YESTERDAY2_EPISODES_URL = "https://www.myepisodes.com/rss.php?feed=today" +
            "&showignored=" + SHOW_IGNORED +
            "&onlyunacquired=1" +
            "&uid=" + UID_REPLACEMENT_STRING +
            "&pwdmd5=" + PWD_REPLACEMENT_STRING;
    public static final String COMING_EPISODES_URL = "https://www.myepisodes.com/rss.php?feed=mylist" +
            "&showignored=" + SHOW_IGNORED +
            "&onlyunacquired=1" +
            "&uid=" + UID_REPLACEMENT_STRING +
            "&pwdmd5=" + PWD_REPLACEMENT_STRING;
    public static final String PASSWORD_ENCRYPTION_TYPE = "MD5";
    public static final String FEED_TITLE_SEPERATOR = " \\]\\[ ";
    public static final String SEASON_EPISODE_NUMBER_SEPERATOR = "x";
    public static final int FEED_TITLE_EPISODE_FIELDS = 4;
    public static final String MYEPISODES_FORM_PARAM_ACTION = "action";
    public static final String MYEPISODES_REGISTER_PAGE = "https://www.myepisodes.com/register.php";
    public static final String MYEPISODES_REGISTER_PAGE_PARAM_USERNAME = "username";
    public static final String MYEPISODES_REGISTER_PAGE_PARAM_PASSWORD = "password";
    public static final String MYEPISODES_REGISTER_PAGE_PARAM_EMAIL = "user_email";
    public static final String MYEPISODES_REGISTER_PAGE_PARAM_ACTION_VALUE = "Register";
    public static final String MYEPISODES_LOGIN_PAGE = "https://www.myepisodes.com/login.php?action=login";
    public static final String MYEPISODES_LOGIN_PAGE_PARAM_USERNAME = "username";
    public static final String MYEPISODES_LOGIN_PAGE_PARAM_PASSWORD = "password";
    public static final String MYEPISODES_LOGIN_PAGE_PARAM_ACTION_VALUE = "Login";
    public static final String MYEPISODES_UPDATE_PAGE_SHOWID_REPLACEMENT = "[ID]";
    public static final String MYEPISODES_UPDATE_PAGE_SEASON_REPLACEMENT = "[S]";
    public static final String MYEPISODES_UPDATE_PAGE_EPISODE_REPLACEMENT = "[E]";

    public static final String MYEPISODES_FULL_UNWATCHED_LISTING = "https://www.myepisodes.com/views.php";
    public static final String MYEPISODES_FULL_UNWATCHED_LISTING_TABLE = "https://www.myepisodes.com/ajax/service.php?mode=view_privatelist";
    public static final String MYEPISODES_CONTROL_PANEL = "https://www.myepisodes.com/cp.php";


    private static final int MYEPISODES_UPDATE_PAGE_SEEN = 1;
    public static final String MYEPISODES_UPDATE_WATCH = "https://www.myepisodes.com/allinone/?action=Update" +
            "&showid=" + MYEPISODES_UPDATE_PAGE_SHOWID_REPLACEMENT +
            "&season=" + MYEPISODES_UPDATE_PAGE_SEASON_REPLACEMENT +
            "&episode=" + MYEPISODES_UPDATE_PAGE_EPISODE_REPLACEMENT +
            "&seen=" + MYEPISODES_UPDATE_PAGE_SEEN;
    private static final int MYEPISODES_UPDATE_PAGE_UNSEEN = 0;
    public static final String MYEPISODES_UPDATE_ACQUIRE = "https://www.myepisodes.com/allinone/?action=Update" +
            "&showid=" + MYEPISODES_UPDATE_PAGE_SHOWID_REPLACEMENT +
            "&season=" + MYEPISODES_UPDATE_PAGE_SEASON_REPLACEMENT +
            "&episode=" + MYEPISODES_UPDATE_PAGE_EPISODE_REPLACEMENT +
            "&seen=" + MYEPISODES_UPDATE_PAGE_UNSEEN;

    public static final String MYEPISODES_SEARCH_PAGE = "https://www.myepisodes.com/search.php";
    public static final String MYEPISODES_SEARCH_PAGE_PARAM_SHOW = "tvshow";
    public static final String MYEPISODES_SEARCH_PAGE_PARAM_ACTION_VALUE = "Search myepisodes.com";
    public static final String MYEPISODES_SEARCH_RESULT_PAGE_SPLITTER_SEARCH_RESULTS = "Search results:";
    public static final String MYEPISODES_SEARCH_RESULT_PAGE_SPLITTER_TABLE_END_TAG = "</table>";
    public static final String MYEPISODES_SEARCH_RESULT_PAGE_SPLITTER_TD_START_TAG = "<td width=\"50%\"><a ";
    public static final String MYEPISODES_ADD_SHOW_PAGE = "https://www.myepisodes.com/views.php?type=manageshow&mode=add&showid=";
    public static final String MYEPISODES_FAVO_IGNORE_PAGE = "https://www.myepisodes.com/shows.php?type=manage";

    public static final String MYEPISODES_FAVO_IGNORE_URL = "https://www.myepisodes.com/myshows.php?action=Ignore&Ignore=1&showid=";
    public static final String MYEPISODES_FAVO_UNIGNORE_URL = "https://www.myepisodes.com/myshows.php?action=Ignore&Ignore=0&showid=";
    public static final String MYEPISODES_FAVO_REMOVE_ULR = "https://www.myepisodes.com/myshows.php?action=Remove&Remove=1&showid=";

    private static Boolean sIsOnline = null;

    public static boolean isOnline() {
        if (sIsOnline != null) {
            return sIsOnline;
        }
        try {
            URL url = new URL("https://www.myepisodes.com/favicon.ico");
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "yourAgent");
            connection.setRequestProperty("Connection", "close");
            connection.setConnectTimeout(1000);
            connection.connect();
            sIsOnline = connection.getResponseCode() == 200;
            connection.disconnect();
        } catch (IOException e) {
            sIsOnline = false;
        }
        return sIsOnline;
    }

    public static void resetOnlineCache() {
        sIsOnline = null;
    }
}
