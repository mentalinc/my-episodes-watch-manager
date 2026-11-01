package nz.mentalinc.watcher.activities;


import android.app.ActivityOptions;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

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
import nz.mentalinc.watcher.service.ShowService;
import nz.mentalinc.watcher.service.UserService;
import nz.mentalinc.watcher.utils.DateUtil;
import nz.mentalinc.watcher.utils.NetworkUtils;
import nz.mentalinc.watcher.utils.TaskRunner;


/**
 * @author Ivo Janssen, maintained and updated by mentalinc
 */

public class EpisodeListingActivity extends AppCompatActivity {
    private static final int EXCEPTION_DIALOG = 2;
    private static final int SETTINGS_RESULT = 6;
    private static final String LOG_TAG = EpisodeListingActivity.class.getSimpleName();
    private static final String DIALOG_LOADING_TAG = "LOADING";
    private long settingsTimestamp;

    private User user;
    private final EpisodesService service;
    private List<Episode> episodes = new ArrayList<>();
    private List<Show> shows = new ArrayList<>();
    //private TextView Title;
    private TextView subTitle;
    private EpisodeExpandableAdapter episodeAdapter;
    private Integer exceptionMessageResId = null;
    private static EpisodeType episodesType;
    private ListMode listMode;
    private String title;
    //private Resources res; // Resource object to get Drawables
    // private android.content.res.Configuration conf;
    private Map<LocalDate, List<Episode>> listedAirDates = null;
    private List<Map.Entry<LocalDate, List<Episode>>> listedAirDatesList = null;
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
                    menu.setHeaderTitle(DateUtil.formatAirDate(determineDate(groupid),
                            getString(R.string.unknownAirDate)));
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
                if (listedAirDatesList != null && listGroupId < listedAirDatesList.size()) {
                    episode = listedAirDatesList.get(listGroupId).getValue().get(listChildId);
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
                if (listedAirDatesList != null && listGroupId < listedAirDatesList.size()) {
                    episodes = listedAirDatesList.get(listGroupId).getValue();
                }
                break;
            default: //added for code quality
        }
        return episodes;
    }

    private LocalDate determineDate(int listGroupId) {

        if (listGroupId < 0) {
            return null;
        }

        switch (listMode) {
            case EPISODES_BY_SHOW:
                return DateUtil.toLocalDate(shows.get(listGroupId).getEpisodes().get(0).getAirDate());
            case EPISODES_BY_DATE:
                if (listedAirDatesList != null && listGroupId < listedAirDatesList.size()) {
                    return listedAirDatesList.get(listGroupId).getKey();
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
        List<EpisodeExpandableAdapter.Group> groups = createGroups();
        if (episodeAdapter == null) {
            episodeAdapter = new EpisodeExpandableAdapter(this, groups);
            expandableListView.setAdapter(episodeAdapter);
            registerForContextMenu(expandableListView);
        } else {
            episodeAdapter.submitList(groups);
        }

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
                if (rowName.equals(episodeAdapter.getGroupKey(i))) {
                    expandableListView.expandGroup(i);
                }
            }
        }
    }

    private void saveListRows() {
        List<String> rows = new ArrayList<>();

        for (int i = 0; i < episodeAdapter.getGroupCount(); i++) {
            if (expandableListView.collapseGroup(i)) {
                rows.add(episodeAdapter.getGroupKey(i));
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

    private List<EpisodeExpandableAdapter.Group> createGroups() {
        List<EpisodeExpandableAdapter.Group> groups = new ArrayList<>();
        String unknownAirDate = getString(R.string.unknownAirDate);

        switch (listMode) {
            case EPISODES_BY_DATE: {
                listedAirDates = new LinkedHashMap<>();
                Map<LocalDate, Integer> workingMap = new TreeMap<>();
                Map<LocalDate, List<Episode>> dateEpisodesMap = new HashMap<>();
                List<Episode> unknownDateEpisodes = new ArrayList<>();
                for (Show show : shows) {
                    for (Episode episode : show.getEpisodes()) {
                        LocalDate airDate = DateUtil.toLocalDate(episode.getAirDate());
                        if (airDate == null) {
                            unknownDateEpisodes.add(episode);
                            continue;
                        }
                        workingMap.merge(airDate, 1, Integer::sum);
                        dateEpisodesMap.computeIfAbsent(airDate, key -> new ArrayList<>())
                                .add(episode);
                    }
                }

                for (Map.Entry<LocalDate, Integer> entry : workingMap.entrySet()) {
                    LocalDate key = entry.getKey();
                    String title = DateUtil.formatAirDateFull(key, unknownAirDate);
                    List<Episode> groupEpisodes = dateEpisodesMap.get(key);
                    groups.add(new EpisodeExpandableAdapter.Group(title,
                            title + " [ " + entry.getValue() + " ]", groupEpisodes));
                    listedAirDates.put(key, groupEpisodes);
                }
                if (!unknownDateEpisodes.isEmpty()) {
                    groups.add(new EpisodeExpandableAdapter.Group(unknownAirDate,
                            unknownAirDate + " [ " + unknownDateEpisodes.size() + " ]",
                            unknownDateEpisodes));
                    listedAirDates.put(null, unknownDateEpisodes);
                }
                listedAirDatesList = new ArrayList<>(listedAirDates.entrySet());
                break;
            }
            case EPISODES_BY_SHOW: {
                for (Show show : shows) {
                    groups.add(new EpisodeExpandableAdapter.Group(show.getShowName(),
                            show.getShowName() + " [ " + show.getNumberEpisodes() + " ]",
                            show.getEpisodes()));
                }
                break;
            }
            default: //added for code quality
        }

        return groups;
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
        ShowService.resetPageFilters(user);
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
        EpisodesController.sortEpisodesOfShows(this, showList);
    }

    private void markEpisodes(final int EpisodeStatus, final Episode episode) {
        showLoadingDialog(R.string.progressLoadingTitle);

        TaskRunner.getExecutor().execute(() -> {
            markEpisode(EpisodeStatus, episode);
            if (exceptionMessageResId == null) {
                getEpisodes();
            }

            runOnUiThread(() -> {
                dismissLoadingDialog();
                if (exceptionMessageResId != null) {
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
            if (exceptionMessageResId == null) {
                getEpisodes();
            }

            runOnUiThread(() -> {
                if (exceptionMessageResId != null) {
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
        boolean onlineCheck = NetworkUtils.isOnline(this);

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
}