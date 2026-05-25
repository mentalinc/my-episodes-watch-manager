package nz.mentalinc.watcher.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import nz.mentalinc.watcher.R;
import nz.mentalinc.watcher.constants.ActivityConstants;
import nz.mentalinc.watcher.controllers.EpisodesController;
import nz.mentalinc.watcher.database.AppDatabase;
import nz.mentalinc.watcher.domain.Episode;
import nz.mentalinc.watcher.domain.EpisodeAscendingComparator;
import nz.mentalinc.watcher.domain.EpisodeDescendingComparator;
import nz.mentalinc.watcher.domain.Show;
import nz.mentalinc.watcher.domain.User;
import nz.mentalinc.watcher.enums.EpisodeType;
import nz.mentalinc.watcher.exception.InternetConnectivityException;
import nz.mentalinc.watcher.exception.LoginFailedException;
import nz.mentalinc.watcher.exception.ShowUpdateFailedException;
import nz.mentalinc.watcher.service.EpisodeRuntime;
import nz.mentalinc.watcher.service.EpisodesService;
import nz.mentalinc.watcher.service.ItemClickSupport;
import nz.mentalinc.watcher.service.UserService;
import nz.mentalinc.watcher.utils.TaskRunner;

public class UpdatedEpisodeListingActivity extends Activity {
    private static final String LOG_TAG = UpdatedEpisodeListingActivity.class.getSimpleName();

    List<Show> shows = new ArrayList<>();
    private final EpisodesService service;

    private List<Episode> episodes = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshEps;
    private LinearLayoutManager layoutManager;
    private SharedPreferences sharedPref;
    private static final String SCROLL_POS_EPS = "scroll_pos_eps";
    private Integer exceptionMessageResId = null;
    private static EpisodeType episodesType;
    private String showMyEpisodeID;
    private BottomNavigationView bottomNavigationView;
    Bundle data;
    EpisodeAdapter adapter;
    private User user;

    private static final int EPISODE_LOADING_DIALOG = 0;
    private static final int ONLINE_CHECK_DIALOG = 5;
    private static final int EPISODE_LOADING_DIALOG_CACHE = 7;
    private final UserService userService;

    public UpdatedEpisodeListingActivity() {
        super();
        userService = new UserService();
        this.service = new EpisodesService();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String themeSetting = prefs.getString("ThemeSetting", "0");
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
        }

        sharedPref = prefs;
        user = new User(
                sharedPref.getString("username", null),
                sharedPref.getString("UserPassword", null)
        );


        //todo add the refresh icon to the screen and then the methods to do that
        data = this.getIntent().getExtras();
        episodesType = (EpisodeType) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE);
        showMyEpisodeID = (String) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID);
        String title = data.getString("Title");

        setContentView(R.layout.recycle_view_episodes);

        findViewById(R.id.appBarEpLayout2).setZ(100f);

        bottomNavigationView = findViewById(R.id.bottom_navigationviewEpisodeHome);
        bottomNavigationView.setOnItemSelectedListener(navigationItemSelectedListener);

        String markEpisode = Objects.requireNonNull(data).getString(ActivityConstants.EXTRA_BUNDLE_VAR_MARK_EPISODE);

        if (markEpisode != null && !Objects.equals(markEpisode, "")) {
            Episode episode = (Episode) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE);

            if (markEpisode.equals(ActivityConstants.EXTRA_BUNDLE_VALUE_WATCH)) {
                markEpisodes(0, episode);
            } else if (markEpisode.equals(ActivityConstants.EXTRA_BUNDLE_VALUE_ACQUIRE)) {
                markEpisodes(1, episode);
            }
        }

        returnEpisodes();

       /* if (shows.size() < 1) {
            Show holderShow = new Show(title, showMyEpisodeID);
            shows.add(holderShow);
        }*/

        if (episodesType.equals(EpisodeType.EPISODES_TO_WATCH)) {
            bottomNavigationView.getMenu().getItem(2).setChecked(true);
            //   returnEpisodesShowHash(EpisodesController.getInstance().getEpisodesShows(EpisodeType.ACQUIRE_BY_SHOW));
        } else if (episodesType.equals(EpisodeType.EPISODES_TO_ACQUIRE)) {
            bottomNavigationView.getMenu().getItem(3).setChecked(true);
            //  returnEpisodesShowHash(EpisodesController.getInstance().getEpisodesShows(EpisodeType.WATCH_BY_SHOW));

        } else if (episodesType.equals(EpisodeType.EPISODES_COMING)) {
            bottomNavigationView.getMenu().getItem(4).setChecked(true);
            //  returnEpisodesShowHash(EpisodesController.getInstance().getEpisodesShows(EpisodeType.COMING_BY_SHOW));
        }

        RecyclerView rvEpisode = findViewById(R.id.recyclerViewListItemsEps);
        adapter = new EpisodeAdapter(episodes, new HashMap<>());
        adapter.submitList(episodes);
        rvEpisode.setAdapter(adapter);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rvEpisode.setLayoutManager(layoutManager);
        rvEpisode.setHasFixedSize(true);
        loadRuntimeMapForEpisodes(episodes, adapter);

        swipeRefreshEps = findViewById(R.id.swipe_refresh_eps);
        swipeRefreshEps.setOnRefreshListener(this::onRefreshClick);
        swipeRefreshEps.setColorSchemeResources(R.color.colorAccent, android.R.color.holo_green_dark, android.R.color.holo_orange_dark);

        int savedPos = sharedPref.getInt(SCROLL_POS_EPS + episodesType + showMyEpisodeID, 0);
        if (savedPos > 0) {
            layoutManager.scrollToPosition(savedPos);
        }


        com.google.android.material.appbar.MaterialToolbar ShowNameTitle = findViewById(R.id.topAppBarEpisodesView);
        title = title + " (" + episodes.size() + ")";
        ShowNameTitle.setTitle(title);

        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {
            Log.w(LOG_TAG, "Home button clicked.");
            Intent home = new Intent(UpdatedEpisodeListingActivity.this, HomeActivity.class);
            home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(home);
        });

        // Leveraging ItemClickSupport decorator to handle clicks on items in our recyclerView
        ItemClickSupport.addTo(rvEpisode).setOnItemClickListener((recyclerView, position, v) -> {
                    //todo something wrong here AFTer clicking on marked watched - the gui shows the correct view, but the episodes array is the whole watched list.
                        //need to figure out why not being filtered down correctly to just the show myepisodeID list before being selected.
                    Episode episodeSelected = episodes.get(position);
                    //Show Show = episodeSelected.getShowName();
                    openEpisodeDetails(episodeSelected, episodesType);
                }
        );


        ItemClickSupport.addTo(rvEpisode).setOnItemLongClickListener((recyclerView, position, v) -> {
                    //Episode episodeSelected = episodes.get(position);
                    //openEpisodeDetails(episodeSelected, episodesType);

                    Log.w(LOG_TAG, "Long clicked on an episode - pop up the marked watch material box.");

                    return true;
                }
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (layoutManager != null) {
            int pos = layoutManager.findFirstVisibleItemPosition();
            sharedPref.edit().putInt(SCROLL_POS_EPS + episodesType + showMyEpisodeID, pos).apply();
        }
    }

    public void onRefreshClick() {
        Log.d(LOG_TAG, "Refreshing episodes for type: " + episodesType);
        EpisodesController controller = EpisodesController.getInstance();

        controller.setEpisodes(episodesType, new ArrayList<>());
        controller.getEpisodesShows(getByShowType(episodesType)).clear();

        TaskRunner.getExecutor().execute(() -> {
            try {
                List<Episode> freshEpisodes = service.retrieveEpisodes(episodesType, user);
                controller.setEpisodes(episodesType, freshEpisodes);
                switch (episodesType) {
                    case EPISODES_TO_WATCH:
                        controller.AddToWatchShow(freshEpisodes);
                        break;
                    case EPISODES_TO_ACQUIRE:
                    case EPISODES_TO_YESTERDAY1:
                    case EPISODES_TO_YESTERDAY2:
                        controller.AddToAcquireShow(freshEpisodes);
                        break;
                    case EPISODES_COMING:
                        controller.AddToComingShow(freshEpisodes);
                        break;
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error refreshing episodes", e);
            }
            runOnUiThread(() -> {
                returnEpisodes();
                adapter = new EpisodeAdapter(episodes, new HashMap<>());
                RecyclerView rv = findViewById(R.id.recyclerViewListItemsEps);
                rv.setAdapter(adapter);
                adapter.submitList(episodes);
                loadRuntimeMapForEpisodes(episodes, adapter);
                if (swipeRefreshEps != null) {
                    swipeRefreshEps.setRefreshing(false);
                }
            });
        });
    }

    private EpisodeType getByShowType(EpisodeType type) {
        switch (type) {
            case EPISODES_TO_WATCH: return EpisodeType.WATCH_BY_SHOW;
            case EPISODES_TO_ACQUIRE: return EpisodeType.ACQUIRE_BY_SHOW;
            case EPISODES_COMING: return EpisodeType.COMING_BY_SHOW;
            default: return type;
        }
    }

    private final NavigationBarView.OnItemSelectedListener navigationItemSelectedListener = new NavigationBarView.OnItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            episodesType = (EpisodeType) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE);
            showMyEpisodeID = (String) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID);
            String Title = data.getString("Title");

            Bundle BundleInfoShowDetail = new Bundle();
            BundleInfoShowDetail.putSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodesType);
            BundleInfoShowDetail.putString(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, showMyEpisodeID);
            BundleInfoShowDetail.putString("Title", Title);

            final int previousItem = bottomNavigationView.getSelectedItemId();
            final int nextItem = item.getItemId();

            //TODO need to do something to open a blank list instead of failing back when clicking on a button that has no shows to watch.
            if (previousItem != nextItem) {
                //switch (nextItem) {
                    if( R.id.barShowDetail == nextItem) {
                        Log.w(LOG_TAG, "barShowDetail selected");
                        if (shows.size() > 0) {
                            //issue where is the epiosdetype doesn't have any episode then this will fail as show.size=0.
                            //need to figure out how to keep the show value current?
                            openShowSummary(shows.get(0).getFirstEpisode(), episodesType);
                        } else if (episodes.size() > 0) {
                            Log.w(LOG_TAG, "barShowDetail Executing openShowSummary");
                            openShowSummary(episodes.get(0), episodesType);
                        } else {

                            openShowSummary(episodesType);
                        }
                        return true;
                    }else if( R.id.barEpisodeOverview == nextItem) {
                        Log.w(LOG_TAG, "barEpisodeOverview selected");
                        //   if (shows.size() > 0) {
                        //make sure it opens the next WATCH episode details.
                        episodesType = EpisodeType.EPISODES_TO_WATCH;
                        returnEpisodes();
                        //returnEpisodesShowHash(EpisodesController.getInstance().getEpisodesShows(EpisodeType.WATCH_BY_SHOW));
                        if (shows.size() > 0) {
                            openEpisodeDetails(shows.get(0).getFirstEpisode(), episodesType);
                            //   }
                        } else {
                            Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarEpisodesView), "No episodes to watch", Snackbar.LENGTH_LONG);
                            snackbar.setAnchorView(bottomNavigationView);
                            snackbar.show();
                            com.google.android.material.appbar.MaterialToolbar ShowNameTitle = findViewById(R.id.topAppBarEpisodesView);
                            String title = data.getString("Title");
                            title = title + " - No episodes to watch";
                            ShowNameTitle.setTitle(title);
                            //  openShowSummary(episodesType);

                        }
                        return true;
                    }else if( R.id.barWatch == nextItem) {
                        Log.w(LOG_TAG, "barWatch selected");

                        if (shows.size() > 0) {
                            openEpisodeListing(shows.get(0), EpisodeType.EPISODES_TO_WATCH);
                        } else {
                            episodesType = EpisodeType.EPISODES_TO_WATCH;
                            //changed to the new hash feature
                            returnEpisodes();
                            //returnEpisodesShowHash(EpisodesController.getInstance().getEpisodesShows(EpisodeType.WATCH_BY_SHOW));
                            if (shows.size() > 0) {
                                openEpisodeListing(shows.get(0), EpisodeType.EPISODES_TO_WATCH);

                            } else {
                                //   bottomNavigationView.getMenu().getItem(2).setChecked(true);
                            }
                        }
                        return true;
                    }else if( R.id.barAcquire == nextItem) {
                        Log.w(LOG_TAG, "barAcquire selected");

                        if (shows.size() > 0) {
                            openEpisodeListing(shows.get(0), EpisodeType.EPISODES_TO_ACQUIRE);
                        } else {
                            episodesType = EpisodeType.EPISODES_TO_ACQUIRE;
                            //changed to the new hash feature
                            returnEpisodes();
                            //returnEpisodesShowHash(EpisodesController.getInstance().getEpisodesShows(EpisodeType.ACQUIRE_BY_SHOW));

                            if (shows.size() > 0) {
                                openEpisodeListing(shows.get(0), EpisodeType.EPISODES_TO_ACQUIRE);
                            }
                        }
                        return true;
                    }else if( R.id.barComing == nextItem) {
                        Log.w(LOG_TAG, "barComing selected");

                        if (shows.size() > 0) {
                            openEpisodeListing(shows.get(0), EpisodeType.EPISODES_COMING);
                        } else {
                            episodesType = EpisodeType.EPISODES_COMING;
                            //changed to the new hash feature
                            returnEpisodes();
                            //returnEpisodesShowHash(EpisodesController.getInstance().getEpisodesShows(EpisodeType.COMING_BY_SHOW));
                            if (shows.size() > 0) {
                                openEpisodeListing(shows.get(0), EpisodeType.EPISODES_COMING);
                            }
                        }
                        return true;
                }
                return false;
            }
            return false;
        }
    };

    private void returnEpisodesShowHash(HashMap<String, Show> hashMap) {

        //TODO - try to add the runtime to the show here? so show has runime early (OR add when first created in the episode service?)
        hashMap.size();
        hashMap.forEach((k, v) -> shows.add(v));

        //shows don't have runtime added yet?
        sortEpisodesOfShows(shows);

    }

    private void openEpisodeListing(Show show, EpisodeType episodeType) {
        finish();
        Intent updatedEpisodeListActivity = new Intent(this.getApplicationContext(), UpdatedEpisodeListingActivity.class);
        updatedEpisodeListActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        Episode nextEpisodeToWatch = show.getFirstEpisode();
        String myEpisodeID = nextEpisodeToWatch.getMyEpisodeID();

        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, myEpisodeID);
        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        updatedEpisodeListActivity.putExtra("Title", show.getShowName());
        startActivity(updatedEpisodeListActivity);
    }

    private void openShowSummary(Episode episode, EpisodeType episodeType) {
        finish();
        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), ShowSummaryActivity.class);
        episodeDetailsSubActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, episode.getMyEpisodeID());
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra("Title", episode.getShowName());
        startActivity(episodeDetailsSubActivity);
    }


    private void openShowSummary(EpisodeType episodeType) {
        finish();
        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), ShowSummaryActivity.class);
        episodeDetailsSubActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, showMyEpisodeID);
        // episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra("Title", data.getString("Title"));
        startActivity(episodeDetailsSubActivity);
    }

    private void openEpisodeDetails(Episode episode, EpisodeType episodeType) {
        //finish();
        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), EpisodeDetailsActivity.class);
        episodeDetailsSubActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, episode.getMyEpisodeID());
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra("Title", episode.getShowName());
        startActivity(episodeDetailsSubActivity);
    }

    private void returnEpisodes() {

        Log.d(LOG_TAG, "returnEpisodes called for: "+showMyEpisodeID);
        List<Episode> episodesRaw;
        //ideally this just grabs the data from the show somehow, loop is slow with lots of data
        episodesRaw = EpisodesController.getInstance().getEpisodes(episodesType);
        //setting to a new list when this is called risks there being now show left to work with if there are no episodes of a particular type left.
        //shows = new ArrayList<>();
        episodes = new ArrayList<>();

        String ShowTitle = "";
        if (episodesRaw != null && episodesRaw.size() > 0) {
            for (Episode ep : episodesRaw) {
                if (ep.getMyEpisodeID().equals(showMyEpisodeID)) {
                    //add to the episode List to show next
                    episodes.add(ep);
                    ShowTitle = ep.getShowName();
                    AddEpisodeToShow(ep);
                }
            }
        } else {
            Log.d(LOG_TAG, "Episode can't be added to show.");
        }
        Log.w(LOG_TAG, "returnEpisodes() completed");

        sortEpisodesOfShows(shows);
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

    private void AddEpisodeToShow(Episode episode) {

        Show currentShow = CheckShowDuplicate(episode.getShowName());

        if (currentShow == null) {
            Show tempShow = new Show(episode.getShowName());
            tempShow.addEpisode(episode);
            shows.add(tempShow);
        } else {
            currentShow.addEpisode(episode);
        }
    }


    private Show CheckShowDuplicate(String episodename) {
        for (Show show : shows) {
            if (show.getShowName().equals(episodename)) {
                return show;
            }
        }
        return null;
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
            }
        } catch (InternetConnectivityException e) {
            String message = "Could not connect to host";
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
            }
        } catch (InternetConnectivityException e) {
            String message = "Could not connect to host";
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

    private void markEpisodes(final int EpisodeStatus, final Episode episode) {
        showDialog(EPISODE_LOADING_DIALOG);
        TaskRunner.getExecutor().execute(() -> {
            markEpisode(EpisodeStatus, episode);
            if (exceptionMessageResId == null || exceptionMessageResId.equals("")) {
                getEpisodes();
                returnEpisodes();
            }
            runOnUiThread(() -> {
                removeDialog(EPISODE_LOADING_DIALOG);
                if (exceptionMessageResId != null && !exceptionMessageResId.equals("")) {
                    exceptionDialog(UpdatedEpisodeListingActivity.this);
                    exceptionMessageResId = null;
                } else {
                    refreshEpisodeAdapter();
                }
            });
        });
    }


    private void markEpisodes(final int episodeStatus, final List<Episode> episodes) {
        showDialog(EPISODE_LOADING_DIALOG);
        TaskRunner.getExecutor().execute(() -> {
            markAllEpisodes(episodeStatus, episodes);
            if (exceptionMessageResId == null || exceptionMessageResId.equals("")) {
                getEpisodes();
                returnEpisodes();
            }
            runOnUiThread(() -> {
                if (exceptionMessageResId != null && !exceptionMessageResId.equals("")) {
                    removeDialog(EPISODE_LOADING_DIALOG);
                    exceptionDialog(UpdatedEpisodeListingActivity.this);
                    exceptionMessageResId = null;
                } else {
                    removeDialog(EPISODE_LOADING_DIALOG);
                    refreshEpisodeAdapter();
                }
            });
        });
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


    private void getEpisodes() {
        //todo - move this to using the hash map maybe?
        episodes = EpisodesController.getInstance().getEpisodes(episodesType);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch (id) {
            case EPISODE_LOADING_DIALOG:
                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setMessage(this.getString(R.string.progressLoadingTitle));
                progressDialog.setCancelable(false);
                dialog = progressDialog;
                break;
            case EPISODE_LOADING_DIALOG_CACHE:
                ProgressDialog progressDialogCache = new ProgressDialog(this);
                progressDialogCache.setMessage(this.getString(R.string.progressLoadingTitleCache));
                progressDialogCache.setCancelable(false);
                dialog = progressDialogCache;
                //dialog.show();
                break;
            case ONLINE_CHECK_DIALOG:
                ProgressDialog progressDialogOnline = new ProgressDialog(this);
                progressDialogOnline.setMessage(this.getString(R.string.progressLoadingOnlineCheck));
                progressDialogOnline.setCancelable(false);
                //progressDialogOnline.show();
                dialog = progressDialogOnline;
                break;

            default:
                dialog = super.onCreateDialog(id);
                break;
        }
        return dialog;
    }

    private void loadRuntimeMapForEpisodes(List<Episode> episodes, EpisodeAdapter adapter) {
        if (episodes == null || episodes.isEmpty()) return;
        TaskRunner.getExecutor().execute(() -> {
            List<String> myEpsIds = new ArrayList<>();
            for (Episode episode : episodes) {
                if (episode.getMyEpisodeID() != null) {
                    myEpsIds.add(episode.getMyEpisodeID());
                }
            }
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            List<EpisodeRuntime> runtimes = db.getSeriesDAO().getEpisodeRuntimeWithMyEpsIds(myEpsIds);
            Map<String, EpisodeRuntime> map = new HashMap<>();
            if (runtimes != null) {
                for (EpisodeRuntime rt : runtimes) {
                    map.put(rt.getShowMyEpsID(), rt);
                }
            }
            Map<String, EpisodeRuntime> finalMap = map;
            runOnUiThread(() -> {
                EpisodeAdapter newAdapter = new EpisodeAdapter(episodes, finalMap);
                RecyclerView rv = findViewById(R.id.recyclerViewListItemsEps);
                rv.setAdapter(newAdapter);
                newAdapter.submitList(episodes);
            });
        });
    }

    private void refreshEpisodeAdapter() {
        RecyclerView rvEpisode = findViewById(R.id.recyclerViewListItemsEps);
        adapter = new EpisodeAdapter(episodes, new HashMap<>());
        adapter.submitList(episodes);
        rvEpisode.setAdapter(adapter);
        loadRuntimeMapForEpisodes(episodes, adapter);
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.topAppBarEpisodesView);
        String dataTitle = data.getString("Title");
        toolbar.setTitle(dataTitle + " (" + episodes.size() + ")");
    }

}
