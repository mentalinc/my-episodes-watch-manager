package nz.mentalinc.watcher.activities;


import android.app.ActivityOptions;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import javax.net.ssl.HttpsURLConnection;

import nz.mentalinc.watcher.R;
import nz.mentalinc.watcher.constants.ActivityConstants;
import nz.mentalinc.watcher.constants.MyEpisodeConstants;
import nz.mentalinc.watcher.controllers.EpisodesController;
import nz.mentalinc.watcher.controllers.RowController;
import nz.mentalinc.watcher.domain.Episode;
import nz.mentalinc.watcher.domain.EpisodeAscendingComparator;
import nz.mentalinc.watcher.domain.EpisodeDescendingComparator;
import nz.mentalinc.watcher.domain.Show;
import nz.mentalinc.watcher.domain.ShowAscendingComparator;
import nz.mentalinc.watcher.domain.ShowDescendingComparator;
import nz.mentalinc.watcher.domain.User;
import nz.mentalinc.watcher.enums.EpisodeType;
import nz.mentalinc.watcher.enums.ListMode;
import nz.mentalinc.watcher.exception.FeedUrlParsingException;
import nz.mentalinc.watcher.exception.InternetConnectivityException;
import nz.mentalinc.watcher.exception.LoginFailedException;
import nz.mentalinc.watcher.exception.ShowUpdateFailedException;
import nz.mentalinc.watcher.service.CredentialStore;
import nz.mentalinc.watcher.service.EpisodesService;
import nz.mentalinc.watcher.service.UserService;
import nz.mentalinc.watcher.utils.DateUtil;
import nz.mentalinc.watcher.utils.TaskRunner;


/**
 * @author Ivo Janssen, maintained and updated by mentalinc
 */

public class EpisodeListingActivity extends AppCompatActivity {
    private static final int EXCEPTION_DIALOG = 2;
    private static final int SETTINGS_RESULT = 6;
    private static final String LOG_TAG = EpisodeListingActivity.class.getSimpleName();
    private static final String DIALOG_LOADING_TAG = "LOADING";
    private static final String DIALOG_ONLINE_TAG = "ONLINE";
    private static final String EPISODE_ROW_TITLE = "episodeRowTitle";
    private static final String EPISODE_ROW_CHILD_TITLE = "episodeRowChildTitle";
    private static final String EPISODE_ROW_CHILD_DETAIL = "episodeRowChildDetail";
    private long settingsTimestamp;

    private User user;
    private final EpisodesService service;
    private List<Episode> episodes = new ArrayList<>();
    private List<Show> shows = new ArrayList<>();
    //private TextView Title;
    private TextView subTitle;
    private SimpleExpandableListAdapter episodeAdapter;
    private Integer exceptionMessageResId = null;
    private static EpisodeType episodesType;
    private ListMode listMode;
    private String title;
    //private Resources res; // Resource object to get Drawables
    // private android.content.res.Configuration conf;
    private Map<Date, List<Episode>> listedAirDates = null;
    private boolean collapsed = true;
    //private boolean isOnelineCheck;
    private ExpandableListView expandableListView;

    private final UserService userService;

    public EpisodeListingActivity() {
        super();
        userService = new UserService();
        this.service = new EpisodesService();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
        String languageCode = prefs.getString("language", "en");
        Configuration config = new Configuration();
        config.setLocale(Locale.forLanguageTag(languageCode));
        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SETTINGS_RESULT && resultCode == RESULT_OK) {
            EpisodesController.getInstance().deleteAll();
            finish();

            startActivity(new Intent(getApplicationContext(), HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);

        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            int groupid = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            int childid = ExpandableListView.getPackedPositionChild(info.packedPosition);

            Episode selectedEpisode = determineEpisode(groupid, childid);

            menu.setHeaderTitle(Objects.requireNonNull(selectedEpisode).getShowName() +
                    " S" + selectedEpisode.getSeasonString() +
                    "E" + selectedEpisode.getEpisodeString() + "\n" +
                    selectedEpisode.getName());
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.episode_listing_tab_child_list_menu, menu);

            if (episodesType.equals(EpisodeType.EPISODES_COMING)) {
                menu.removeItem(R.id.episodeTweet);
                menu.removeItem(R.id.episodeMenuAcquired);
            } else if (episodesType.equals(EpisodeType.EPISODES_TO_WATCH)) {
                menu.removeItem(R.id.episodeMenuAcquired);
            }
        } else {
            int groupid = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            switch (listMode) {
                case EPISODES_BY_SHOW:
                    menu.setHeaderTitle(shows.get(groupid).getShowName());
                    break;
                case EPISODES_BY_DATE:
                    menu.setHeaderTitle(DateUtil.formatDateLong(determineDate(groupid)));
                    break;
                default: //added for code quality
            }
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.episode_listing_tab_group_list_menu, menu);

            if (episodesType.equals(EpisodeType.EPISODES_COMING)) {
                menu.removeItem(R.id.showMenuAcquired);
                menu.removeItem(R.id.showMenuWatched);
            } else if (episodesType.equals(EpisodeType.EPISODES_TO_WATCH)) {
                menu.removeItem(R.id.showMenuAcquired);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        long ts = PreferenceManager.getDefaultSharedPreferences(this)
                .getLong("settings_changed_timestamp", 0);
        if (ts != settingsTimestamp && ts != 0) {
            settingsTimestamp = ts;
            recreate();
        }
    }

    public void onPause() {
        super.onPause();
        saveListRows();
    }

    private Episode determineEpisode(int listGroupId, int listChildId) {
        Episode episode = null;

        if (listGroupId < 0 || listChildId < 0) {
            return null;
        }

        switch (listMode) {
            case EPISODES_BY_SHOW:
                episode = shows.get(listGroupId).getEpisodes().get(listChildId);
                break;
            case EPISODES_BY_DATE:
                Iterator<Map.Entry<Date, List<Episode>>> iter = listedAirDates.entrySet().iterator();
                int i = 0;
                while (iter.hasNext()) {
                    Map.Entry entry = iter.next();
                    if (i == listGroupId) {
                        episode = listedAirDates.get(entry.getKey()).get(listChildId);
                        break;
                    } else {
                        i++;
                    }
                }
                break;
            default: //added for code quality
        }
        return episode;
    }

    private List<Episode> determineGroup(int listGroupId) {
        List<Episode> episodes = null;

        if (listGroupId < 0) {
            return null;
        }

        switch (listMode) {
            case EPISODES_BY_SHOW:
                episodes = shows.get(listGroupId).getEpisodes();
                break;
            case EPISODES_BY_DATE:
                Iterator<Map.Entry<Date, List<Episode>>> iter = listedAirDates.entrySet().iterator();
                int i = 0;
                while (iter.hasNext()) {
                    Map.Entry entry = iter.next();
                    if (i == listGroupId) {
                        episodes = listedAirDates.get(entry.getKey());
                        break;
                    } else {
                        i++;
                    }
                }
                break;
            default: //added for code quality
        }
        return episodes;
    }

    private Date determineDate(int listGroupId) {

        if (listGroupId < 0) {
            return null;
        }

        switch (listMode) {
            case EPISODES_BY_SHOW:
                return new Date(shows.get(listGroupId).getEpisodes().get(0).getAirDate());
            case EPISODES_BY_DATE:
                Iterator<Map.Entry<Date, List<Episode>>> iter = listedAirDates.entrySet().iterator();
                int i = 0;
                while (iter.hasNext()) {
                    Map.Entry entry = iter.next();
                    if (i == listGroupId) {
                        return (Date) entry.getKey();
                    } else {
                        i++;
                    }
                }
                break;
            default: //added for code quality
        }
        return null;
    }




    public void exceptionDialog(Context context) {

        if (exceptionMessageResId == null) {
            exceptionMessageResId = R.string.defaultExceptionMessage;
        }

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
        dialog.setTitle(R.string.exceptionDialogTitle);
        dialog.setMessage(exceptionMessageResId);
        dialog.setPositiveButton(R.string.dialogOK, (dialog1, which) -> {
            exceptionMessageResId = null;
            dialog1.dismiss();
        });

        dialog.setCancelable(false);
        dialog.create();
        dialog.show();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        settingsTimestamp = sharedPref.getLong("settings_changed_timestamp", 0);
        String themeSetting = sharedPref.getString("ThemeSetting", "0");


        switch (themeSetting) {
            case "0":
                setTheme(R.style.ThemeDayNight);
                break;
            case "1":
                setTheme(R.style.ThemeLight);
                break;
            case "2":
                setTheme(R.style.ThemeDark);
                break;
            default: //added for code quality
        }
        super.onCreate(savedInstanceState);

        Bundle data = this.getIntent().getExtras();
        episodesType = Objects.requireNonNull(data).getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.class);
        listMode = data.getSerializable(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.class);
        title = data.getSerializable(ActivityConstants.EXTRA_TITLE, String.class);
        init();
        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {

            Log.w(LOG_TAG, "Home button clicked.");
            exit();
        });


        androidx.appcompat.view.menu.ActionMenuItemView appBarbtn_title_collapse = findViewById(R.id.btn_title_collapse);
        appBarbtn_title_collapse.setOnClickListener(v -> {

            Log.w(LOG_TAG, "Collapse button clicked.");
            onCollapseClick();
        });

        androidx.appcompat.view.menu.ActionMenuItemView appBarbtn_title_refresh = findViewById(R.id.btn_title_refresh);
        appBarbtn_title_refresh.setOnClickListener(v -> {

            Log.w(LOG_TAG, "Refresh button clicked.");
            onRefreshClick();
        });


    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
        int groupid = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        int childid = ExpandableListView.getPackedPositionChild(info.packedPosition);
        Episode selectedEpisode = determineEpisode(groupid, childid);
        List<Episode> selectedGroup;
        selectedGroup = determineGroup(groupid);
        final int nextItem = item.getItemId();
        //switch (item.getItemId()) {
            if( R.id.episodeMenuWatched == nextItem) {
                markEpisodes(0, selectedEpisode);
                return true;
            }else if (R.id.episodeMenuAcquired == nextItem) {
                markEpisodes(1, selectedEpisode);
                return true;
            }else if( R.id.episodeTweet == nextItem) {
                String tweet = Objects.requireNonNull(selectedEpisode).getShowName() + " S" + selectedEpisode.getSeasonString() + "E" + selectedEpisode.getEpisodeString() + " - " + selectedEpisode.getName();
                Intent i = new Intent(android.content.Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, getString(R.string.Tweet, tweet));
                startActivity(Intent.createChooser(i, getString(R.string.TweetTitle)));
                return true;
            }else if( R.id.episodeMenuDetails == nextItem){
                openEpisodeDetails(selectedEpisode, episodesType);
                return true;
             }else if( R.id.showMenuWatched == nextItem) {
            markEpisodes(0, selectedGroup);
            return true;
            }else if (R.id.showMenuAcquired == nextItem) {
                markEpisodes(1, selectedGroup);
                return true;
            }else {
                return false;
            }
        }


    //@Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        openEpisodeDetails(determineEpisode(groupPosition, childPosition), episodesType);
        return true;
    }

    private void init() {
        setContentView(R.layout.episode_listing_tab);
        expandableListView = findViewById(android.R.id.list);
        episodes = new ArrayList<>();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        user = new User(
                sharedPref.getString(MyEpisodeConstants.PREF_USERNAME, null),
                CredentialStore.getPassword(EpisodeListingActivity.this)
        );

        TextView Title = findViewById(R.id.watchListTitle);
        Title.setText(getString(R.string.watchListTitle));
        subTitle = findViewById(R.id.watchListSubTitle);
        subTitle.setText("");

        Bundle data = this.getIntent().getExtras();
        String markEpisode = Objects.requireNonNull(data).getString(ActivityConstants.EXTRA_BUNDLE_VAR_MARK_EPISODE);

        if (markEpisode != null && !Objects.equals(markEpisode, "")) {
            Episode episode = data.getParcelable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, Episode.class);

            if (markEpisode.equals(ActivityConstants.EXTRA_BUNDLE_VALUE_WATCH)) {
                markEpisodes(0, episode);
            } else if (markEpisode.equals(ActivityConstants.EXTRA_BUNDLE_VALUE_ACQUIRE)) {
                markEpisodes(1, episode);
            }
        }
        getEpisodes();
        returnEpisodes();
        //set actionbar  title
        Toolbar toolbar = findViewById(R.id.topAppBarEpListing);
        toolbar.setTitle(title);
    }

    private void initExendableList() {
        episodeAdapter = new SimpleExpandableListAdapter(
                this,
                createGroups(),
                R.layout.episode_listing_tab_row_group,
                new String[]{EPISODE_ROW_TITLE},
                new int[]{R.id.episodeRowTitle},
                createChilds(),
                R.layout.episode_listing_tab_row_child,
                new String[]{EPISODE_ROW_CHILD_TITLE, EPISODE_ROW_CHILD_DETAIL},
                new int[]{R.id.episodeRowChildTitle, R.id.episodeRowChildDetail}
        );
        expandableListView.setAdapter(episodeAdapter);
        episodeAdapter.notifyDataSetChanged();
        registerForContextMenu(expandableListView);

        int countEpisodes = EpisodesController.getInstance().getEpisodesCount(episodesType);

        if (countEpisodes == 200) {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarEpListing), R.string.watchListFull, Snackbar.LENGTH_LONG);
            snackbar.show();
        }
        if (countEpisodes == 1) {
            switch (episodesType) {
                case EPISODES_TO_WATCH:
                    subTitle.setText(getString(R.string.watchListSubTitleWatch, countEpisodes));
                    this.setTitle(R.string.watch);

                    openListRows(RowController.getInstance().getOpenWatchRows());
                    break;
                case EPISODES_TO_YESTERDAY1:
                case EPISODES_TO_YESTERDAY2:
                case EPISODES_TO_ACQUIRE:
                    subTitle.setText(getString(R.string.watchListSubTitleAcquire, countEpisodes));
                    this.setTitle(R.string.acquire);
                    openListRows(RowController.getInstance().getOpenAcquireRows());
                    break;
                case EPISODES_COMING:
                    subTitle.setText(getString(R.string.watchListSubTitleComing, countEpisodes));
                    this.setTitle(R.string.coming);
                    openListRows(RowController.getInstance().getOpenComingRows());
                    break;
            default: //added for code quality
            }
        } else {
            switch (episodesType) {
                case EPISODES_TO_WATCH:
                    subTitle.setText(getString(R.string.watchListSubTitleWatchPlural, countEpisodes));
                    this.setTitle(R.string.watch);
                    openListRows(RowController.getInstance().getOpenWatchRows());
                    break;
                case EPISODES_TO_YESTERDAY1:
                case EPISODES_TO_YESTERDAY2:
                case EPISODES_TO_ACQUIRE:
                    subTitle.setText(getString(R.string.watchListSubTitleAcquirePlural, countEpisodes));
                    this.setTitle(R.string.acquire);
                    openListRows(RowController.getInstance().getOpenAcquireRows());
                    break;
                case EPISODES_COMING:
                    subTitle.setText(getString(R.string.watchListSubTitleComingPlural, countEpisodes));
                    this.setTitle(R.string.coming);
                    openListRows(RowController.getInstance().getOpenComingRows());
                    break;
            default: //added for code quality
            }
        }
    }

    private void openListRows(List<String> openRows) {
        for (String rowName : openRows) {
            for (int i = 0; i < episodeAdapter.getGroupCount(); i++) {
                if (rowName.equals((episodeAdapter.getGroup(i).toString().split("\\["))[0]))
                    expandableListView.expandGroup(i);
            }
        }
    }

    private void saveListRows() {
        List<String> rows = new ArrayList<>();

        for (int i = 0; i < episodeAdapter.getGroupCount(); i++) {
            if (expandableListView.collapseGroup(i)) {
                rows.add((episodeAdapter.getGroup(i).toString().split("\\["))[0]);
            }

        }

        switch (episodesType) {
            case EPISODES_TO_WATCH:
                RowController.getInstance().setOpenWatchRows(rows);
                break;
            case EPISODES_TO_YESTERDAY1:
            case EPISODES_TO_YESTERDAY2:
            case EPISODES_TO_ACQUIRE:
                RowController.getInstance().setOpenAcquireRows(rows);
                break;
            case EPISODES_COMING:
                RowController.getInstance().setOpenComingRows(rows);
                break;
            default: //added for code quality
        }
    }

    private List<? extends Map<String, ?>> createGroups() {
        List<Map<String, String>> headerList = new ArrayList<>();

        switch (listMode) {
            case EPISODES_BY_DATE: {
                listedAirDates = new LinkedHashMap<>();
                Map<Date, Integer> workingMap = new TreeMap<>();
                for (Show show : shows) {
                    for (Episode episode : show.getEpisodes()) {
                        Date airDate = new Date(episode.getAirDate());
                        if (!workingMap.containsKey(airDate)) {
                            workingMap.put(airDate, 1);
                        } else {
                            int count = workingMap.get(airDate);
                            workingMap.put(airDate, ++count);
                        }
                    }
                }

                for (Date key : workingMap.keySet()) {
                    Map<String, String> map = new HashMap<>();
                    int countEp = workingMap.get(key);
                    map.put(EPISODE_ROW_TITLE, DateUtil.formatDateFull(key) + " [ " + countEp + " ]");
                    headerList.add(map);
                    listedAirDates.put(key, null);
                }
                break;
            }
            case EPISODES_BY_SHOW: {
                for (Show show : shows) {
                    Map<String, String> map = new HashMap<>();
                    map.put(EPISODE_ROW_TITLE, show.getShowName() + " [ " + show.getNumberEpisodes() + " ]");
                    headerList.add(map);
                }
                break;
            }
            default: //added for code quality
        }
 
        return headerList;
    }

    private List<? extends List<? extends Map<String, ?>>> createChilds() {
        List<List<Map<String, String>>> childList = new ArrayList<>();

        switch (listMode) {
            case EPISODES_BY_DATE: {
                for (Map.Entry<Date, List<Episode>> dateListEntry : listedAirDates.entrySet()) {
                    Date listedAirDate = dateListEntry.getKey();
                    List<Episode> episodeList = new ArrayList<>();

                    List<Map<String, String>> subListSecondLvl = new ArrayList<>();
                    for (Show show : shows) {
                        for (Episode episode : show.getEpisodes()) {
                            if (listedAirDate.getTime() == episode.getAirDate()) {
                                HashMap<String, String> map = new HashMap<>();
                                map.put(EPISODE_ROW_CHILD_TITLE, episode.getShowName());
                                map.put(EPISODE_ROW_CHILD_DETAIL, "S" + episode.getSeasonString() + "E" + episode.getEpisodeString() + " - " + episode.getName());
                                subListSecondLvl.add(map);
                                episodeList.add(episode);
                            }
                        }
                    }
                    dateListEntry.setValue(episodeList);
                    childList.add(subListSecondLvl);
                }
                break;
            }
            case EPISODES_BY_SHOW: {
                for (Show show : shows) {
                    List<Map<String, String>> subListSecondLvl = new ArrayList<>();
                    for (Episode episode : show.getEpisodes()) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put(EPISODE_ROW_CHILD_TITLE, episode.getShowName());
                        map.put(EPISODE_ROW_CHILD_DETAIL, "S" + episode.getSeasonString() + "E" + episode.getEpisodeString() + " - " + episode.getName());
                        subListSecondLvl.add(map);
                    }
                    childList.add(subListSecondLvl);
                }
                break;
            }
            default: //added for code quality
        }
        return childList;
    }

    private void openEpisodeDetails(Episode episode, EpisodeType episodeType) {
        finish();

        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), EpisodeDetailsActivity.class);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode)
                .putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_TITLE, title);
        startActivity(episodeDetailsSubActivity, ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_left, R.anim.slide_out_right).toBundle());
    }

    private void reloadEpisodes() {
        saveListRows();

        if (MyEpisodeConstants.CACHE_EPISODES_ENABLED) {
            showLoadingDialog(R.string.progressLoadingTitleCache);
        } else {
            showLoadingDialog(R.string.progressLoadingTitle);
        }

        TaskRunner.getExecutor().execute(() -> {
            getEpisodesMyEpisodes();

            runOnUiThread(() -> {
                returnEpisodes();
                dismissLoadingDialog();
            });
        });
    }


    private void resetPageFilters(User user) {

        try {
            userService.login(user.getUsername(), user.getPassword());
            //this should read from preferences in time but manual building for now
            //unaquired 1
            //Unwatched 2
            //Ignored 4
            //Pilots 2048
            //Localized Airdate 4096
            String urlParameters = "";//"eps_filters%5B%5D=1&eps_filters%5B%5D=2&eps_filters%5B%5D=4096";


            if (MyEpisodeConstants.SHOW_LISTING_UNACQUIRED_ENABLED) {
                //unaquired 1
                if (urlParameters.length() < 1)
                    urlParameters += "eps_filters%5B%5D=1";
                else {
                    urlParameters += "&eps_filters%5B%5D=1";
                }

                Log.d(LOG_TAG, "SHOW_LISTING_UNACQUIRED_ENABLED" + " " + urlParameters);
            }
            if (MyEpisodeConstants.SHOW_LISTING_UNWATCHED_ENABLED) {
                //Unwatched 2
                if (urlParameters.length() < 1)
                    urlParameters += "eps_filters%5B%5D=2";
                else {
                    urlParameters += "&eps_filters%5B%5D=2";
                }
                Log.d(LOG_TAG, "SHOW_LISTING_UNWATCHED_ENABLED" + " " + urlParameters);

            }

            if (MyEpisodeConstants.SHOW_LISTING_IGNORED_ENABLED) {
                //Ignored 4
                if (urlParameters.length() < 1)
                    urlParameters += "eps_filters%5B%5D=4";
                else {
                    urlParameters += "&eps_filters%5B%5D=4";
                }
                Log.d(LOG_TAG, "SHOW_LISTING_IGNORED_ENABLED" + " " + urlParameters);
            }

            if (MyEpisodeConstants.SHOW_LISTING_PILOTS_ENABLED) {
                //Pilots 2048
                if (urlParameters.length() < 1)
                    urlParameters += "eps_filters%5B%5D=2048";
                else {
                    urlParameters += "&eps_filters%5B%5D=2048";
                }
                Log.d(LOG_TAG, "SHOW_LISTING_PILOTS_ENABLED" + " " + urlParameters);

            }


            if (MyEpisodeConstants.SHOW_LISTING_LOCALIZED_AIRDATES__ENABLED) {
                //Localized Airdate 4096
                if (urlParameters.length() < 1)
                    urlParameters += "eps_filters%5B%5D=4096";
                else {
                    urlParameters += "&eps_filters%5B%5D=4096";
                }
                Log.d(LOG_TAG, "SHOW_LISTING_LOCALIZED_AIRDATES__ENABLED" + " " + urlParameters);
            }


            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
            int postDataLength = postData.length;
            String request = MyEpisodeConstants.MYEPISODES_FULL_UNWATCHED_LISTING_TABLE;
            URL url = new URL(request);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");
            conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
            conn.setUseCaches(false);
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.write(postData);
                wr.flush();
            }

            InputStream stream = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8), 8);
            String result = reader.readLine();

        } catch (Exception e) {
            String message = "Error resetting episode filter";
            Log.e(LOG_TAG, message, e);
        }
    }


    private void getEpisodes() {
        episodes = EpisodesController.getInstance().getEpisodes(episodesType);
    }

    private void getEpisodesMyEpisodes() {
        try {
            if (episodesType == EpisodeType.EPISODES_TO_ACQUIRE) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                String acquire = sharedPref.getString("ACQUIRE_KEY", "0");
                if (acquire != null && acquire.equals("1")) {
                    EpisodesController.getInstance().setEpisodes(EpisodeType.EPISODES_TO_YESTERDAY1, service.retrieveEpisodes(EpisodeType.EPISODES_TO_YESTERDAY1, user));
                    EpisodesController.getInstance().addEpisodes(EpisodeType.EPISODES_TO_YESTERDAY2, service.retrieveEpisodes(EpisodeType.EPISODES_TO_YESTERDAY2, user));
                } else {
                    EpisodesController.getInstance().setEpisodes(EpisodeType.EPISODES_TO_ACQUIRE, service.retrieveEpisodes(EpisodeType.EPISODES_TO_ACQUIRE, user));
                }
            } else {
                EpisodesController.getInstance().setEpisodes(episodesType, service.retrieveEpisodes(episodesType, user));
            }
        } catch (InternetConnectivityException e) {
            String message = MyEpisodeConstants.CONNECT_ERROR;
            Log.e(LOG_TAG, message, e);
            exceptionMessageResId = R.string.internetConnectionFailureReload;
        } catch (FeedUrlParsingException e) {
            String message = "Exception occured:";
            Log.e(LOG_TAG, message, e);
            exceptionMessageResId = R.string.watchListUnableToReadFeed;
        } catch (Exception e) {
            String message = "Exception occured:";
            Log.e(LOG_TAG, message, e);
            exceptionMessageResId = R.string.defaultExceptionMessage;
        }

        getEpisodes();
        resetPageFilters(user);
    }

    private void returnEpisodes() {
        shows = new ArrayList<>();
        if (episodes != null && episodes.size() > 0) {
            for (Episode ep : episodes) {
                AddEpisodeToShow(ep);
            }
        } else {
            Log.d(LOG_TAG, "Episode can't be added to show.");
        }

        sortShows(shows);
        sortEpisodesOfShows(shows);

        initExendableList();

        if (exceptionMessageResId != null) {
            //showDialog(EXCEPTION_DIALOG);
            exceptionDialog(EpisodeListingActivity.this);
            exceptionMessageResId = null;
        }
    }

    private void AddEpisodeToShow(Episode episode) {

        Show currentShow = CheckShowDublicate(episode.getShowName());

        if (currentShow == null) {
            Show tempShow = new Show(episode.getShowName());
            tempShow.addEpisode(episode);
            shows.add(tempShow);
        } else {
            currentShow.addEpisode(episode);
        }
    }

    private Show CheckShowDublicate(String episodename) {
        for (Show show : shows) {
            if (show.getShowName().equals(episodename)) {
                return show;
            }
        }
        return null;
    }

    private void sortShows(List<Show> showList) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String sorting = "";
        switch (episodesType) {
            case EPISODES_TO_WATCH:
                sorting = sharedPref.getString("showWatchOrder", MyEpisodeConstants.SHOW_MYEPISODES_DEFAULT_SORT);//Preferences.getPreference(this, PreferencesKeys.WATCH_SHOW_SORTING_KEY);
                break;
            case EPISODES_TO_YESTERDAY1:
            case EPISODES_TO_YESTERDAY2:
            case EPISODES_TO_ACQUIRE:
                sorting = sharedPref.getString("showAcquireOrder", MyEpisodeConstants.SHOW_MYEPISODES_DEFAULT_SORT); //Preferences.getPreference(this, PreferencesKeys.ACQUIRE_SHOW_SORTING_KEY);
                break;
            case EPISODES_COMING:
                sorting = sharedPref.getString("showComingOrder", MyEpisodeConstants.SHOW_MYEPISODES_DEFAULT_SORT); //Preferences.getPreference(this, PreferencesKeys.COMING_SHOW_SORTING_KEY);
                break;
            default: //added for code quality
        }
        String[] showOrderOptions = getResources().getStringArray(R.array.showOrderOptionsValues);

        if (sorting.equals(showOrderOptions[1])) {
            Log.d(LOG_TAG, "Sorting episodes ascending");
            showList.sort(new ShowAscendingComparator());
        } else if (sorting.equals(showOrderOptions[2])) {
            Log.d(LOG_TAG, "Sorting episodes descending");
            showList.sort(new ShowDescendingComparator());
        } else if (sorting.equals(showOrderOptions[0])) {
            Log.d(LOG_TAG, "Default my episodes show sorting, nothing to do!");
        }
    }

    private void sortEpisodesOfShows(List<Show> showList) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        // String sorting = Preferences.getPreference(this, PreferencesKeys.EPISODE_SORTING_KEY);
        String sorting = sharedPref.getString("episodeOrder", "oldest_on_top");

        String[] episodeOrderOptions = getResources().getStringArray(R.array.episodeOrderOptionsValues);

        for (Show show : showList) {
            if (sorting.equals(episodeOrderOptions[0])) {
                show.getEpisodes().sort(new EpisodeAscendingComparator());
            } else if (sorting.equals(episodeOrderOptions[1])) {
                show.getEpisodes().sort(new EpisodeDescendingComparator());
            }
        }
    }

    private void markEpisodes(final int EpisodeStatus, final Episode episode) {
        showLoadingDialog(R.string.progressLoadingTitle);

        TaskRunner.getExecutor().execute(() -> {
            markEpisode(EpisodeStatus, episode);
            if (exceptionMessageResId == null || exceptionMessageResId.equals("")) {
                getEpisodes();
            }

            runOnUiThread(() -> {
                dismissLoadingDialog();
                if (exceptionMessageResId != null && !exceptionMessageResId.equals("")) {
                            exceptionDialog(EpisodeListingActivity.this);
                    exceptionMessageResId = null;
                } else {
                    EpisodesController.getInstance().deleteEpisode(episode.getType(), episode);
                    returnEpisodes();
                }
            });
        });
    }



    private void markEpisodes(final int episodeStatus, final List<Episode> episodes) {
        showLoadingDialog(R.string.progressLoadingTitle);

        TaskRunner.getExecutor().execute(() -> {
            markAllEpisodes(episodeStatus, episodes);
            if (exceptionMessageResId == null || exceptionMessageResId.equals("")) {
                getEpisodes();
            }

            runOnUiThread(() -> {
                if (exceptionMessageResId != null && !exceptionMessageResId.equals("")) {
                    dismissLoadingDialog();
                            exceptionDialog(EpisodeListingActivity.this);
                    exceptionMessageResId = null;
                } else {
                    dismissLoadingDialog();
                    returnEpisodes();
                }
            });
        });
    }

    private void markEpisode(int EpisodeStatus, Episode episode) {
        try {
            switch (EpisodeStatus) {
                case 0:
                    service.watchedEpisode(episode, user);
                    break;
                case 1:
                    service.acquireEpisode(episode, user);
                    break;
            default: //added for code quality
            }
        } catch (InternetConnectivityException e) {
            String message = MyEpisodeConstants.CONNECT_ERROR;
            Log.e(LOG_TAG, message, e);
            exceptionMessageResId = R.string.networkIssues;
        } catch (LoginFailedException e) {
            String message = "Login failure";
            Log.e(LOG_TAG, message, e);
            exceptionMessageResId = R.string.networkIssues;
        } catch (ShowUpdateFailedException e) {
            String message = "Marking the show watched failed (" + episode + ")";
            Log.e(LOG_TAG, message, e);
            exceptionMessageResId = R.string.watchListUnableToMarkWatched;
        } catch (Exception e) {
            String message = "Unknown exception occured";
            Log.e(LOG_TAG, message, e);
            exceptionMessageResId = R.string.defaultExceptionMessage;
        }
    }

    private void markAllEpisodes(int EpisodeStatus, List<Episode> episodes) {
        try {
            switch (EpisodeStatus) {
                case 0:
                    service.watchedEpisodes(episodes, user);
                    break;
                case 1:
                    service.acquireEpisodes(episodes, user);
                    break;
            default: //added for code quality
            }
        } catch (InternetConnectivityException e) {
            String message = MyEpisodeConstants.CONNECT_ERROR;
            Log.e(LOG_TAG, message, e);
            exceptionMessageResId = R.string.networkIssues;
        } catch (LoginFailedException e) {
            String message = "Login failure";
            Log.e(LOG_TAG, message, e);
            exceptionMessageResId = R.string.networkIssues;
        } catch (ShowUpdateFailedException e) {
            String message = "Marking shows watched failed";
            Log.e(LOG_TAG, message, e);
            exceptionMessageResId = R.string.watchListUnableToMarkWatched;
        } catch (Exception e) {
            String message = "Unknown exception occured";
            Log.e(LOG_TAG, message, e);
            exceptionMessageResId = R.string.defaultExceptionMessage;
        }
    }

    public void onHomeClick(View v) {
        exit();
    }

    public void onRefreshClick() {
        Log.v(LOG_TAG, "Show online dialog.");
        showOnlineDialog();
        boolean onlineCheck = isOnline();
        Log.v(LOG_TAG, "Hide online dialog.");
        dismissOnlineDialog();

        Log.d(LOG_TAG, "Check if online: " + onlineCheck);


        Log.d(LOG_TAG, "Episode type: " + episodesType + " Episodes Refreshing...");
        Log.d(LOG_TAG, "Cache Age: " + MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE);

        File file;
        //delete the current cache file to force a new download
        switch (episodesType) {
            case EPISODES_TO_WATCH:
                file = new File(MyEpisodeConstants.CONTEXT.getFilesDir(), "Watch.xml");
                if (file.exists() && onlineCheck) {
                    if (file.delete()) {
                        Log.d(LOG_TAG, "Watch.xml deleted");
                    } else {
                        Log.e(LOG_TAG, "ERROR deleting Watch.xml");
                    }
                }
                break;
            case EPISODES_TO_YESTERDAY1:
            case EPISODES_TO_YESTERDAY2:
            case EPISODES_TO_ACQUIRE:
                file = new File(MyEpisodeConstants.CONTEXT.getFilesDir(), "Acquire.xml");
                if (file.exists() && onlineCheck) {
                    if (file.delete()) {
                        Log.d(LOG_TAG, "Acquire.xml deleted");
                    } else {
                        Log.e(LOG_TAG, "ERROR deleting Acquire.xml");
                    }
                }
                break;
            case EPISODES_COMING:
                file = new File(MyEpisodeConstants.CONTEXT.getFilesDir(), "Coming.xml");
                if (file.exists() && onlineCheck) {
                    if (file.delete()) {
                        Log.d(LOG_TAG, "Coming.xml deleted");
                    } else {
                        Log.e(LOG_TAG, "ERROR deleting Coming.xml");
                    }
                }
                break;
            default: //added for code quality
        }
        reloadEpisodes();


    }

    public void onCollapseClick() {
        for (int i = 0; i < episodeAdapter.getGroupCount(); i++) {
            if (collapsed) {
                expandableListView.expandGroup(i);
            } else {
                expandableListView.collapseGroup(i);
            }
        }

        collapsed = !collapsed;
    }

    private void exit() {
        Intent home = new Intent(this, HomeActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(home);
    }

    private Boolean isOnline() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            //Thread.sleep(3000);
            URL url = new URL("https://www.myepisodes.com/favicon.ico");
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "yourAgent");
            connection.setRequestProperty("Connection", "close");
            connection.setConnectTimeout(1000);
            connection.connect();

            if (connection.getResponseCode() == 200) {
                connection.disconnect();
                Log.v(LOG_TAG, "Online.");
                return true;
            } else {
                connection.disconnect();
                return false;
            }
        } catch (UnknownHostException e) {
            Log.e(LOG_TAG, e.toString());
            Log.v(LOG_TAG, "Offline!!");
            return false;
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
            return false;
        }
    }

    public static class LoadingDialogFragment extends DialogFragment {
        private static final String ARG_MESSAGE = "message";

        public static LoadingDialogFragment newInstance(int messageResId) {
            LoadingDialogFragment frag = new LoadingDialogFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_MESSAGE, messageResId);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int messageResId = getArguments().getInt(ARG_MESSAGE);
            View view = getLayoutInflater().inflate(R.layout.progress_dialog, null);
            ((TextView) view.findViewById(R.id.message)).setText(getString(messageResId));
            return new MaterialAlertDialogBuilder(requireActivity())
                    .setView(view)
                    .setCancelable(false)
                    .create();
        }
    }

    private void showLoadingDialog(int messageResId) {
        if (getSupportFragmentManager().findFragmentByTag(DIALOG_LOADING_TAG) == null) {
            LoadingDialogFragment.newInstance(messageResId).show(getSupportFragmentManager(), DIALOG_LOADING_TAG);
        }
    }

    private void dismissLoadingDialog() {
        Fragment prev = getSupportFragmentManager().findFragmentByTag(DIALOG_LOADING_TAG);
        if (prev != null) ((DialogFragment) prev).dismiss();
    }

    private void showOnlineDialog() {
        if (getSupportFragmentManager().findFragmentByTag(DIALOG_ONLINE_TAG) == null) {
            LoadingDialogFragment.newInstance(R.string.progressLoadingOnlineCheck).show(getSupportFragmentManager(), DIALOG_ONLINE_TAG);
            getSupportFragmentManager().executePendingTransactions();
        }
    }

    private void dismissOnlineDialog() {
        Fragment prev = getSupportFragmentManager().findFragmentByTag(DIALOG_ONLINE_TAG);
        if (prev != null) ((DialogFragment) prev).dismiss();
    }
}