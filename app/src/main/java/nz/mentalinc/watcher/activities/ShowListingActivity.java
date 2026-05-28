package nz.mentalinc.watcher.activities;


import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import nz.mentalinc.watcher.R;
import nz.mentalinc.watcher.constants.ActivityConstants;
import nz.mentalinc.watcher.constants.MyEpisodeConstants;
import nz.mentalinc.watcher.controllers.EpisodesController;
import nz.mentalinc.watcher.database.AppDatabase;
import nz.mentalinc.watcher.domain.Episode;
import nz.mentalinc.watcher.domain.EpisodeAscendingComparator;
import nz.mentalinc.watcher.domain.EpisodeDescendingComparator;
import nz.mentalinc.watcher.domain.Show;
import nz.mentalinc.watcher.domain.ShowAscendingComparator;
import nz.mentalinc.watcher.domain.ShowDescendingComparator;
import nz.mentalinc.watcher.domain.ShowRuntimeAscendingComparator;
import nz.mentalinc.watcher.domain.User;
import nz.mentalinc.watcher.enums.EpisodeType;
import nz.mentalinc.watcher.exception.FeedUrlParsingException;
import nz.mentalinc.watcher.exception.InternetConnectivityException;
import nz.mentalinc.watcher.service.CredentialStore;
import nz.mentalinc.watcher.service.EpisodeRuntime;
import nz.mentalinc.watcher.service.EpisodesService;
import nz.mentalinc.watcher.service.ItemClickSupport;
import nz.mentalinc.watcher.service.UserService;
import nz.mentalinc.watcher.utils.TaskRunner;

public class ShowListingActivity extends AppCompatActivity {

    private static final String LOG_TAG = ShowListingActivity.class.getSimpleName();
    List<Show> shows = new ArrayList<>();
    private List<Show> showsFull = new ArrayList<>();
    private List<Episode> episodes = new ArrayList<>();
    private Map<String, EpisodeRuntime> runtimeMap = new HashMap<>();
    private boolean sortByRuntime = false;
    private static EpisodeType episodesType;
    private static final String DIALOG_LOADING_TAG = "LOADING";
    private static final String DIALOG_ONLINE_TAG = "ONLINE";
    private Integer exceptionMessageResId = null;
    private EpisodesService service;
    private User user;
    private UserService userService;

    private BottomNavigationView bottomNavigationView;
    private SwipeRefreshLayout swipeRefreshShows;
    private LinearLayoutManager layoutManager;
    private SharedPreferences sharedPref;
    private static final String SCROLL_POS_SHOWS = "scroll_pos_shows";

    public ShowListingActivity() {
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
            default: //added for code quality
        }
 
        setContentView(R.layout.recycle_view_shows);
        userService = new UserService();
        this.service = new EpisodesService();

        findViewById(R.id.appBarLayout2).setZ(100f);

        Bundle data = this.getIntent().getExtras();
        //episodeType is set based on the button on the home page that is press.
        episodesType = data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.class);
        sharedPref = prefs;
        user = new User(
                sharedPref.getString(MyEpisodeConstants.PREF_USERNAME, null),
                CredentialStore.getPassword(ShowListingActivity.this)
        );

        com.google.android.material.appbar.MaterialToolbar episodeTypeTitle = findViewById(R.id.topAppBarShowsView);
        bottomNavigationView = findViewById(R.id.bottom_navigationRecyclerShow);
        bottomNavigationView.setOnItemSelectedListener(navigationItemSelectedListener);


        TextView showEpCount = findViewById(R.id.ShowEpCount);


        if (episodesType.toString().equals("EPISODES_TO_WATCH")) {
            int showCount = EpisodesController.getInstance().getShowCountType(episodesType);
            int episodeCount = EpisodesController.getInstance().getEpisodesCount(episodesType);
            String showEpCountString = getString(R.string.showCountFormat, showCount, episodeCount);
            showEpCount.setText(showEpCountString);
            episodeTypeTitle.setTitle(getString(R.string.watch));
        }
        if (episodesType.toString().equals("EPISODES_TO_ACQUIRE")) {
            int showCount = EpisodesController.getInstance().getShowCountType(episodesType);
            int episodeCount = EpisodesController.getInstance().getEpisodesCount(episodesType);
            String showEpCountString = getString(R.string.showCountFormat, showCount, episodeCount);
            showEpCount.setText(showEpCountString);
            episodeTypeTitle.setTitle(getString(R.string.acquire));
        }
        if (episodesType.toString().equals("EPISODES_COMING")) {
            int showCount = EpisodesController.getInstance().getShowCountType(episodesType);
            int episodeCount = EpisodesController.getInstance().getEpisodesCount(episodesType);
            String showEpCountString = getString(R.string.showCountFormat, showCount, episodeCount);
            showEpCount.setText(showEpCountString);
            episodeTypeTitle.setTitle(getString(R.string.coming));
        }
        episodes = EpisodesController.getInstance().getEpisodes(episodesType);

        int countEpisodes = EpisodesController.getInstance().getEpisodesCount(episodesType);


        if (countEpisodes == 200) {
            @SuppressLint("CutPasteId") // this is to stop and error - the below is meant to be used so can pop up the snackbar when 200 shows are found.
            Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarShowsView), R.string.watchListFull, Snackbar.LENGTH_LONG);
            snackbar.setAnchorView(bottomNavigationView);
            snackbar.show();
        }


        if (episodesType.equals(EpisodeType.EPISODES_TO_ACQUIRE)) {
            bottomNavigationView.getMenu().getItem(2).setChecked(true);
            //   returnEpisodesShowHash(EpisodesController.getInstance().getEpisodesShows(EpisodeType.ACQUIRE_BY_SHOW));
        } else if (episodesType.equals(EpisodeType.EPISODES_TO_WATCH)) {
            bottomNavigationView.getMenu().getItem(1).setChecked(true);
            //  returnEpisodesShowHash(EpisodesController.getInstance().getEpisodesShows(EpisodeType.WATCH_BY_SHOW));

        } else if (episodesType.equals(EpisodeType.EPISODES_COMING)) {
            bottomNavigationView.getMenu().getItem(3).setChecked(true);
            //  returnEpisodesShowHash(EpisodesController.getInstance().getEpisodesShows(EpisodeType.COMING_BY_SHOW));
        }

        //DISABLE TO TEST NEW HASHMAP which is in the returnEpisodesShowHash in the if else if else if above. Hashmap has lots of issues removing for now
        returnEpisodes();
        showsFull = new ArrayList<>(shows);

        RecyclerView rvShows = findViewById(R.id.recyclerViewListItemsShows);
        ShowAdapter adapter = new ShowAdapter(shows, new HashMap<>());
        rvShows.setAdapter(adapter);
        loadRuntimeMapForShows(shows, adapter);
        // Set layout manager to position the items
        layoutManager = new LinearLayoutManager(this);
        rvShows.setLayoutManager(layoutManager);
        rvShows.setHasFixedSize(true);

        swipeRefreshShows = findViewById(R.id.swipe_refresh_shows);
        swipeRefreshShows.setOnRefreshListener(this::onRefreshClick);
        swipeRefreshShows.setColorSchemeResources(R.color.colorAccent, android.R.color.holo_green_dark, android.R.color.holo_orange_dark);

        int savedPos = sharedPref.getInt(SCROLL_POS_SHOWS + episodesType, 0);
        if (savedPos > 0) {
            layoutManager.scrollToPosition(savedPos);
        }

        EditText searchEditText = findViewById(R.id.searchEditText);
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.topAppBarShowsView);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.home) {
                Log.w(LOG_TAG, "Home button clicked.");
                Intent home = new Intent(ShowListingActivity.this, HomeActivity.class);
                home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(home);
                return true;
            } else if (id == R.id.btn_title_sort_runtime) {
                sortByRuntime = !sortByRuntime;
                toggleRuntimeSort();
                return true;
            } else if (id == R.id.btn_title_refresh) {
                Log.w(LOG_TAG, "Refresh button clicked.");
                onRefreshClick();
                return true;
            } else if (id == R.id.action_search) {
                if (searchEditText.getVisibility() == View.VISIBLE) {
                    searchEditText.setVisibility(View.GONE);
                    searchEditText.setText("");
                    filterShows("");
                } else {
                    searchEditText.setVisibility(View.VISIBLE);
                    searchEditText.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(searchEditText, 0);
                    }
                }
                return true;
            }
            return false;
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterShows(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        ItemClickSupport.addTo(rvShows).setOnItemClickListener((recyclerView, position, v) -> {
                    ShowAdapter listAdapter = (ShowAdapter) recyclerView.getAdapter();
                    Show showSelected = listAdapter.getCurrentList().get(position);
                    Episode nextEpisodeToWatch = showSelected.getFirstEpisode();

                    if (showSelected.getNumberEpisodes() == 1) {
                        openEpisodeDetails(nextEpisodeToWatch, episodesType);
                    } else if (episodesType.equals(EpisodeType.EPISODES_TO_ACQUIRE)) {
                        openEpisodeListing(showSelected, episodesType);
                    } else if (episodesType.equals(EpisodeType.EPISODES_TO_WATCH)) {
                        openEpisodeListing(showSelected, episodesType);
                    } else if (episodesType.equals(EpisodeType.EPISODES_COMING)) {
                        openEpisodeListing(showSelected, episodesType);
                    } else {
                        ShowSummaryActivity(showSelected, episodesType);
                    }
                }
        );

        ItemClickSupport.addTo(rvShows).setOnItemLongClickListener((recyclerView, position, v) -> {
                    ShowAdapter listAdapter = (ShowAdapter) recyclerView.getAdapter();
                    Show showSelected = listAdapter.getCurrentList().get(position);
                    Episode episode = showSelected.getFirstEpisode();
                    EpisodeRuntime rt = runtimeMap.get(episode.getMyEpisodeID());
                    if (rt != null && rt.getShowURL() != null && !rt.getShowURL().isEmpty()) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(rt.getShowURL()));
                        startActivity(browserIntent);
                    }
                    return true;
                }
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (layoutManager != null) {
            int pos = layoutManager.findFirstVisibleItemPosition();
            sharedPref.edit().putInt(SCROLL_POS_SHOWS + episodesType, pos).apply();
        }
    }

    // This is only here to test the old view of showing all the episodes for an app instead grouping them via the show overview view now.
    private void openEpisodeListing(Show show, EpisodeType episodeType) {
        finish();
        Intent updatedEpisodeListActivity = new Intent(this.getApplicationContext(), UpdatedEpisodeListingActivity.class);

        Episode nextEpisodeToWatch = show.getFirstEpisode();
        String myepisodeID = nextEpisodeToWatch.getMyEpisodeID();


        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, myepisodeID);
        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        updatedEpisodeListActivity.putExtra(ActivityConstants.EXTRA_TITLE, show.getShowName());
        startActivity(updatedEpisodeListActivity, ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_left, R.anim.slide_out_right).toBundle());
    }


    private final NavigationBarView.OnItemSelectedListener navigationItemSelectedListener = new NavigationBarView.OnItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            final int nextItem = item.getItemId();
            //switch (item.getItemId()) {

                if( R.id.barHome  == nextItem) {
                    finish();
                    bottomNavigationView.getMenu().getItem(0).setChecked(true);
                    Intent HomeActivity = new Intent(getApplicationContext(), HomeActivity.class);
                    HomeActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    HomeActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_WATCH);
                    startActivity(HomeActivity);
                    return true;

                }else if( R.id.barWatch == nextItem) {
                    finish();
                    Log.w(LOG_TAG, "barWatch selected");
                    Intent newWatchShowListing = new Intent(getApplicationContext(), ShowListingActivity.class);
                    //Intent newWatchShowListing = new Intent(getApplicationContext(), UpdatedEpisodeListingActivity.class);
                    newWatchShowListing.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    newWatchShowListing.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_WATCH);
                    startActivity(newWatchShowListing);

                    return true;
                }else if (R.id.barAcquire == nextItem) {
                    finish();
                    Log.w(LOG_TAG, "barAcquire selected");
                    Intent newAcquireShowListing = new Intent(getApplicationContext(), ShowListingActivity.class);
                    // Intent newAcquireShowListing = new Intent(getApplicationContext(), UpdatedEpisodeListingActivity.class);
                    newAcquireShowListing.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    newAcquireShowListing.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_ACQUIRE);
                    startActivity(newAcquireShowListing);

                    return true;
                }else if(R.id.barComing == nextItem) {
                    finish();
                    Log.w(LOG_TAG, "barComing selected");
                    Intent newComingShowListing = new Intent(getApplicationContext(), ShowListingActivity.class);
                    // Intent newComingShowListing = new Intent(getApplicationContext(), UpdatedEpisodeListingActivity.class);
                    newComingShowListing.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    newComingShowListing.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_COMING);
                    startActivity(newComingShowListing);

                    return true;
                }else if(R.id.barCalendar == nextItem) {
                    finish();
                    Log.w(LOG_TAG, "barCalendar selected");
                    Intent calendarIntent = new Intent(getApplicationContext(), CalendarActivity.class);
                    calendarIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(calendarIntent);
                    return true;

               /* case R.id.barRandom:
                    Log.w(LOG_TAG, "barRandom selected");

                    List<Show> showList = EpisodesController.getInstance().getRandomWatchEpisodeShowList();
                    Show show = showList.get(0);
                    Episode randomEpisode = show.getFirstEpisode();

                    Intent episodeDetailsSubActivity = new Intent(getApplicationContext(), EpisodeDetailsActivity.class);
                    episodeDetailsSubActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, randomEpisode);
                    episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_WATCH);
                    episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, randomEpisode.getMyEpisodeID());
                    episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_TITLE, randomEpisode.getShowName());
                    startActivity(episodeDetailsSubActivity);

                    return true;*/
            }
            return false;
        }


    };


    //this is the new view.
    private void ShowSummaryActivity(Show show, EpisodeType episodeType) {

        Intent ShowSummaryActivity = new Intent(this.getApplicationContext(), ShowSummaryActivity.class);

        Episode nextEpisodeToWatch = show.getFirstEpisode();
        String myEpisodeID = nextEpisodeToWatch.getMyEpisodeID();
        ShowSummaryActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, nextEpisodeToWatch);
        ShowSummaryActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, myEpisodeID);
        ShowSummaryActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        ShowSummaryActivity.putExtra(ActivityConstants.EXTRA_TITLE, show.getShowName());
        Log.w(LOG_TAG, "openShowHomePage method called");
        startActivity(ShowSummaryActivity, ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_left, R.anim.slide_out_right).toBundle());
    }

    private void openEpisodeDetails(Episode episode, EpisodeType episodeType) {

        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), EpisodeDetailsActivity.class);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, episode)
                .putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodeType);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_TITLE, episode.getShowName());
        startActivity(episodeDetailsSubActivity, ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_left, R.anim.slide_out_right).toBundle());
    }

    private void returnEpisodes() {
        //shows = new ArrayList<>();
        if (episodes != null && episodes.size() > 0) {
            for (Episode ep : episodes) {
                AddEpisodeToShow(ep);
            }
        } else {
            Log.d(LOG_TAG, "Episode can't be added to show.");
        }

        //shows don't have runtime added yet?
        sortShows(shows);
        sortEpisodesOfShows(shows);

    }


    private void filterShows(String query) {
        if (query == null || query.trim().isEmpty()) {
            List<Show> listToShow = sortByRuntime ? getRuntimeSortedShows() : new ArrayList<>(showsFull);
            RecyclerView rvShows = findViewById(R.id.recyclerViewListItemsShows);
            ShowAdapter adapter = rvShows != null ? (ShowAdapter) rvShows.getAdapter() : null;
            if (adapter != null) {
                adapter.submitList(listToShow);
            }
            updateShowEpCount(listToShow);
            return;
        }

        String lowerQuery = query.toLowerCase().trim();
        List<Show> filtered = new ArrayList<>();
        for (Show show : showsFull) {
            if (show.getShowName() != null && show.getShowName().toLowerCase().contains(lowerQuery)) {
                filtered.add(show);
            }
        }

        if (sortByRuntime) {
            filtered.sort(getRuntimeComparator());
        }

        RecyclerView rvShows = findViewById(R.id.recyclerViewListItemsShows);
        if (rvShows != null) {
            ShowAdapter adapter = (ShowAdapter) rvShows.getAdapter();
            if (adapter != null) {
                adapter.submitList(filtered);
            }
        }
        updateShowEpCount(filtered);
    }

    private void updateShowEpCount(List<Show> shows) {
        TextView showEpCount = findViewById(R.id.ShowEpCount);
        if (showEpCount != null) {
            int showCount = shows.size();
            int episodeCount = 0;
            for (Show show : shows) {
                episodeCount += show.getNumberEpisodes();
            }
            showEpCount.setText(showCount + " shows - " + episodeCount + " episodes");
        }
    }

    private List<Show> getRuntimeSortedShows() {
        List<Show> sorted = new ArrayList<>(showsFull);
        sorted.sort(getRuntimeComparator());
        return sorted;
    }

    private java.util.Comparator<Show> getRuntimeComparator() {
        return (a, b) -> {
            String idA = a.getFirstEpisode() != null ? a.getFirstEpisode().getMyEpisodeID() : "";
            String idB = b.getFirstEpisode() != null ? b.getFirstEpisode().getMyEpisodeID() : "";
            EpisodeRuntime ra = runtimeMap.get(idA);
            EpisodeRuntime rb = runtimeMap.get(idB);
            int runtimeA = 0, runtimeB = 0;
            try {
                runtimeA = ra != null && ra.getShowRuntime() != null ? Integer.parseInt(ra.getShowRuntime()) : 0;
            } catch (NumberFormatException ignored) {}
            try {
                runtimeB = rb != null && rb.getShowRuntime() != null ? Integer.parseInt(rb.getShowRuntime()) : 0;
            } catch (NumberFormatException ignored) {}
            int cmp = Integer.compare(runtimeB, runtimeA);
            if (cmp == 0) return a.getShowName().compareTo(b.getShowName());
            return cmp;
        };
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
            Show tempShow = new Show(episode.getShowName(), episode.getMyEpisodeID());
            tempShow.addEpisode(episode);
            shows.add(tempShow);
        } else {
            currentShow.addEpisode(episode);
        }
    }

    private void toggleRuntimeSort() {
        if (runtimeMap == null || runtimeMap.isEmpty()) {
            sortByRuntime = false;
            Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarShowsView),
                    "Runtime data loading, try again", Snackbar.LENGTH_SHORT);
            snackbar.setAnchorView(bottomNavigationView);
            snackbar.show();
            return;
        }

        List<Show> listToShow = sortByRuntime ? getRuntimeSortedShows() : new ArrayList<>(showsFull);

        RecyclerView rvShows = findViewById(R.id.recyclerViewListItemsShows);
        ShowAdapter adapter = (ShowAdapter) rvShows.getAdapter();
        if (adapter != null) {
            adapter.submitList(listToShow);
        }
        updateShowEpCount(listToShow);

        com.google.android.material.appbar.MaterialToolbar tb = findViewById(R.id.topAppBarShowsView);
        MenuItem sortMenuItem = tb.getMenu().findItem(R.id.btn_title_sort_runtime);
        if (sortMenuItem != null) {
            sortMenuItem.setTitle(getString(sortByRuntime ? R.string.sort_by_default : R.string.sort_by_runtime));
        }
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
        } else if (sorting.equals(showOrderOptions[4])) {
            Log.d(LOG_TAG, "Sort by Runtime!");
            showList.sort(new ShowRuntimeAscendingComparator());
        }
    }

    private Show CheckShowDuplicate(String episodeName) {
        for (Show show : shows) {
            if (show.getShowName().equals(episodeName)) {
                return show;
            }
        }
        return null;
    }


    private void reloadEpisodes() {
        if (MyEpisodeConstants.CACHE_EPISODES_ENABLED) {
            showLoadingDialog(R.string.progressLoadingTitleCache);
        } else {
            showLoadingDialog(R.string.progressLoadingTitle);
        }
        TaskRunner.getExecutor().execute(() -> {
            getEpisodesMyEpisodes();
            runOnUiThread(() -> {
                returnEpisodes();
                showsFull = new ArrayList<>(shows);
                sortByRuntime = false;
                com.google.android.material.appbar.MaterialToolbar tb = findViewById(R.id.topAppBarShowsView);
                MenuItem sortMenuItem = tb.getMenu().findItem(R.id.btn_title_sort_runtime);
                if (sortMenuItem != null) {
                    sortMenuItem.setTitle(getString(R.string.sort_by_runtime));
                }
                RecyclerView rvShows = findViewById(R.id.recyclerViewListItemsShows);
                ShowAdapter newAdapter = new ShowAdapter(shows, new HashMap<>());
                rvShows.setAdapter(newAdapter);
                loadRuntimeMapForShows(shows, newAdapter);
                dismissLoadingDialog();
                if (swipeRefreshShows != null) {
                    swipeRefreshShows.setRefreshing(false);
                }
            });
        });
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

    private void loadRuntimeMapForShows(List<Show> showsList, ShowAdapter adapter) {
        if (showsList == null || showsList.isEmpty()) return;
        TaskRunner.getExecutor().execute(() -> {
            List<String> myEpsIds = new ArrayList<>();
            for (Show show : showsList) {
                if (show.getFirstEpisode() != null && show.getFirstEpisode().getMyEpisodeID() != null) {
                    myEpsIds.add(show.getFirstEpisode().getMyEpisodeID());
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
            runOnUiThread(() -> {
                runtimeMap = map;
                ShowAdapter newAdapter = new ShowAdapter(showsList, map);
                RecyclerView rv = findViewById(R.id.recyclerViewListItemsShows);
                rv.setAdapter(newAdapter);
            });
        });
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
