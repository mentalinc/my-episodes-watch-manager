package eu.vranckaert.episodeWatcher.constants;

import android.content.Context;

public class MyEpisodeConstants {
    public static final String UID_REPLACEMENT_STRING = "[UID]";
    public static final String PWD_REPLACEMENT_STRING = "[PWD]";
    private static final String FEED = "unwatched";
    private static final String SHOW_IGNORED = "0";

    public static String DAYS_BACK_CP = "365";
    public static Boolean DAYS_BACK_ENABLED = false;
    public static Boolean CACHE_EPISODES_ENABLED = false;
    public static Boolean SHOW_RUNTIME_ENABLED = false;
    public static String CACHE_EPISODES_CACHE_AGE = "Disabled";

    public static Boolean SHOW_LISTING_UNACQUIRED_ENABLED = false;
    public static Boolean SHOW_LISTING_UNWATCHED_ENABLED = false;
    public static Boolean SHOW_LISTING_IGNORED_ENABLED = false;
    public static Boolean SHOW_LISTING_PILOTS_ENABLED = false;
    public static Boolean SHOW_LISTING_LOCALIZED_AIRDATES__ENABLED = false;



    public static Context CONTEXT = null;

    public static String EXTENDED_EPISODES_XML;

    public  static final String TV_MAZE_SHOWS_URL = "https://www.tvmaze.com/shows/";

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



}
