package nz.mentalinc.episodeWatcher.activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import nz.mentalinc.episodeWatcher.utils.TaskRunner;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.constants.ActivityConstants;
import nz.mentalinc.episodeWatcher.constants.MyEpisodeConstants;
import nz.mentalinc.episodeWatcher.controllers.EpisodesController;
import nz.mentalinc.episodeWatcher.domain.Episode;
import nz.mentalinc.episodeWatcher.domain.Show;
import nz.mentalinc.episodeWatcher.domain.User;
import nz.mentalinc.episodeWatcher.enums.EpisodeType;
import nz.mentalinc.episodeWatcher.enums.ListMode;
import nz.mentalinc.episodeWatcher.enums.ShowType;
import nz.mentalinc.episodeWatcher.exception.InternetConnectivityException;
import nz.mentalinc.episodeWatcher.exception.LoginFailedException;
import nz.mentalinc.episodeWatcher.service.EpisodesService;
import nz.mentalinc.episodeWatcher.service.ShowService;
import nz.mentalinc.episodeWatcher.service.UserService;

public class HomeActivity extends AppCompatActivity {
    private static final String LOG_TAG = EpisodesService.class.getSimpleName();
    private EpisodesService service;
    private User user;
    private Resources res; // Resource object to get Drawables
    private android.content.res.Configuration conf;
    private static final int EPISODE_LOADING_DIALOG = 0;
    private static final int EPISODE_LOADING_DIALOG_CACHE = 7;
    private static final int LOGOUT_DIALOG = 1;
    private static final int EXCEPTION_DIALOG = 2;
    private static final int LOGIN_RESULT = 5;
    private static final int SETTINGS_RESULT = 6;
    private static final int RUNTIME_LOADING_DIALOG = 8;
    private UserService userService;
    private static Context sContext;
    private BottomNavigationView bottomNavigationView;

    private SwipeRefreshLayout swipeRefreshLayout;

    private ProgressDialog runtimeProgressDialog;


    private boolean exception;

    private Button btnWatched;
    private Button btnAcquired;

    private com.google.android.material.button.MaterialButton btn_ShowWatchNew;
    private com.google.android.material.button.MaterialButton btn_ShowAcquireNew;
    private com.google.android.material.button.MaterialButton btn_ShowListing;
    private com.google.android.material.button.MaterialButton btnComing;


    private Intent watchIntent;
    private Intent acquireIntent;
    private Intent comingIntent;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        init();


        userService = new UserService();
        setContentView(R.layout.main);

        //Below makes sure all settings have their default value set to limit null errors etc
        PreferenceManager.setDefaultValues(getBaseContext(), R.xml.settings_screen, false);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String LanguageCode = sharedPref.getString("language", "en");
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
        }

        bottomNavigationView = findViewById(R.id.bottom_navigationActivityHome);
        bottomNavigationView.getMenu().getItem(0).setChecked(true);
        bottomNavigationView.setOnItemSelectedListener(navigationItemSelectedListener);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(this::refreshEpisodesData);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, android.R.color.holo_green_dark, android.R.color.holo_orange_dark);

        MyEpisodeConstants.DAYS_BACK_CP = sharedPref.getString("daysBack", "365");
        MyEpisodeConstants.DAYS_FORWARD_CP = sharedPref.getString("daysForward", "20");
        MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE = sharedPref.getString("CacheFileAge", "false");
        MyEpisodeConstants.DAYS_BACK_ENABLED = sharedPref.getBoolean("daysBackwardEnable", false);
        MyEpisodeConstants.CACHE_EPISODES_ENABLED = sharedPref.getBoolean("CacheEpisodes", false);
        MyEpisodeConstants.SHOW_RUNTIME_ENABLED = sharedPref.getBoolean("RunTime", false);

        MyEpisodeConstants.SHOW_LISTING_UNACQUIRED_ENABLED = sharedPref.getBoolean("listingUnacquiredFilter", true);
        MyEpisodeConstants.SHOW_LISTING_UNWATCHED_ENABLED = sharedPref.getBoolean("listingUnwatchedFilter", true);
        MyEpisodeConstants.SHOW_LISTING_IGNORED_ENABLED = sharedPref.getBoolean("listingIgnoredFilter", false);
        MyEpisodeConstants.SHOW_LISTING_PILOTS_ENABLED = sharedPref.getBoolean("listingPilotsFilter", false);
        MyEpisodeConstants.SHOW_LISTING_LOCALIZED_AIRDATES__ENABLED = sharedPref.getBoolean("listingLocalizedAirdatesFilter", true);

        conf.locale = new Locale(LanguageCode);
        res.updateConfiguration(conf, null);

        openLoginActivity();

        super.onCreate(savedInstanceState);
        this.service = new EpisodesService();
        sContext = getApplicationContext();

        user = new User(
                sharedPref.getString("username", User.USERNAME),
                sharedPref.getString("UserPassword", User.PASSWORD)
        );


        MyEpisodeConstants.CONTEXT = getApplicationContext();


        String[] showOrderOptions = getResources().getStringArray(R.array.showOrderOptionsValues);

        btnWatched = findViewById(R.id.btn_watched);
        btn_ShowWatchNew = findViewById(R.id.btn_ShowWatchNew);
        watchIntent = new Intent().setClass(this, EpisodeListingActivity.class).putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_WATCH);

        String watch_sorting = sharedPref.getString("showWatchOrder", "show_myepisodes_default_sort");
        if (watch_sorting.equals(showOrderOptions[3])) {
            watchIntent.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_DATE);
        } else {
            watchIntent.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_SHOW);
        }
        watchIntent.putExtra("Title", getString(R.string.watch));
        btnWatched.setOnClickListener(v -> startActivity(watchIntent));

        acquireIntent = new Intent().setClass(this, EpisodeListingActivity.class).putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_ACQUIRE);

        String acquire_sorting = sharedPref.getString("ACQUIRE_KEY", "0");
        if (acquire_sorting.equals(showOrderOptions[3])) {
            acquireIntent.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_DATE);
        } else {
            acquireIntent.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_SHOW);
        }
        acquireIntent.putExtra("Title", getString(R.string.acquire));
        btnAcquired = findViewById(R.id.btn_acquired);
        btnAcquired.setOnClickListener(v -> startActivity(acquireIntent));
        btn_ShowAcquireNew = findViewById(R.id.btn_ShowAcquireNew);
        btn_ShowListing = findViewById(R.id.btn_ShowListing);


        if (sharedPref.getBoolean("disableAcquire", false)) {
            btnAcquired.setVisibility(View.GONE);
        }

        btnComing = findViewById(R.id.btn_coming);
        comingIntent = new Intent().setClass(this, EpisodeListingActivity.class).putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_COMING);

        String coming_sorting = sharedPref.getString("showComingOrder", "show_myepisodes_default_sort");
        if (coming_sorting.equals(showOrderOptions[3])) {
            comingIntent.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_DATE);
        } else {
            comingIntent.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_SHOW);
        }
        comingIntent.putExtra("Title", getString(R.string.coming));
        btnComing.setOnClickListener(v -> startActivity(comingIntent));

        if (sharedPref.getBoolean("disableComing", false)) {
            btnComing.setVisibility(View.GONE);
        }


        androidx.appcompat.view.menu.ActionMenuItemView appBarLogout = findViewById(R.id.logout);
        appBarLogout.setOnClickListener(v -> {

            Log.w(LOG_TAG, "logout button clicked.");
            onLogoutClick(HomeActivity.this);
        });


        androidx.appcompat.view.menu.ActionMenuItemView appBarSettings = findViewById(R.id.btn_settings);
        appBarSettings.setOnClickListener(v -> {

            Log.w(LOG_TAG, "Settings button clicked.");
            onSettingsClick(v);
        });


    }


    private final NavigationBarView.OnItemSelectedListener navigationItemSelectedListener = new NavigationBarView.OnItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            final int nextItem = item.getItemId();
            //switch (item.getItemId()) {

                if(R.id.barHome == nextItem) {
                    //finish();
                    bottomNavigationView.getMenu().getItem(0).setChecked(true);

                    return true;
                }else if( R.id.barWatch== nextItem) {
                    Log.w(LOG_TAG, "barWatch selected");
                    Intent newWatchShowListing = new Intent(getApplicationContext(), ShowListingActivity.class);
                    newWatchShowListing.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    newWatchShowListing.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_WATCH);
                    startActivity(newWatchShowListing);

                    return true;
                }else if( R.id.barAcquire== nextItem) {
                    Log.w(LOG_TAG, "barAcquire selected");
                    Intent newAcquireShowListing = new Intent(getApplicationContext(), ShowListingActivity.class);
                    newAcquireShowListing.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    newAcquireShowListing.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_ACQUIRE);
                    startActivity(newAcquireShowListing);

                    return true;
                }else if( R.id.barComing == nextItem) {
                    Log.w(LOG_TAG, "barComing selected");
                    Intent newComingShowListing = new Intent(getApplicationContext(), ShowListingActivity.class);
                    newComingShowListing.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    newComingShowListing.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_COMING);
                    startActivity(newComingShowListing);

                    return true;
                }else{


              /*  case R.id.barRandom:
                    Log.w(LOG_TAG, "barRandom selected");

                    List<Show> showList = EpisodesController.getInstance().getRandomWatchEpisodeShowList();
                    Show show = showList.get(0);
                    Episode randomEpisode = show.getFirstEpisode();

                    Intent episodeDetailsSubActivity = new Intent(getApplicationContext(), EpisodeDetailsActivity.class);
                    episodeDetailsSubActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, randomEpisode);
                    episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_WATCH);
                    episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, randomEpisode.getMyEpisodeID());
                    episodeDetailsSubActivity.putExtra("Title", randomEpisode.getShowName());
                    startActivity(episodeDetailsSubActivity);

                    return true;*/
            }
            return false;
        }


    };


    private void getEpisodesInLoadingDialog() {
        final EpisodesController episodesController = EpisodesController.getInstance();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        user = new User(

                sharedPref.getString("username", User.USERNAME),
                sharedPref.getString("UserPassword", User.PASSWORD)
        );
        if (episodesController.areListsEmpty()) {
            showDialog(EPISODE_LOADING_DIALOG);

            if (MyEpisodeConstants.CACHE_EPISODES_ENABLED) {
                showDialog(EPISODE_LOADING_DIALOG_CACHE);
            } else {
                showDialog(EPISODE_LOADING_DIALOG);
            }
            loadEpisodesData(() -> runOnUiThread(() -> {
                removeDialog(EPISODE_LOADING_DIALOG);
                removeDialog(EPISODE_LOADING_DIALOG_CACHE);

                if (exception) {
                    exception = false;
                    exceptionErrorDialog(HomeActivity.this);
                } else {
                    updateHomeButtonCounts();
                }
            }));
        }

        if (MyEpisodeConstants.SHOW_RUNTIME_ENABLED && user != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            if (!prefs.getBoolean("runtime_populated", false)) {
                showDialog(RUNTIME_LOADING_DIALOG);
                TaskRunner.getExecutor().execute(() -> {
                    populateFavouriteShowRuntimes();
                    runOnUiThread(() -> {
                        if (runtimeProgressDialog != null && runtimeProgressDialog.isShowing()) {
                            runtimeProgressDialog.dismiss();
                        }
                        prefs.edit().putBoolean("runtime_populated", true).apply();
                    });
                });
            }
        }
    }

    private void refreshEpisodesData() {
        EpisodesController episodesController = EpisodesController.getInstance();
        episodesController.setEpisodes(EpisodeType.EPISODES_TO_WATCH, new ArrayList<>());
        episodesController.setEpisodes(EpisodeType.EPISODES_TO_ACQUIRE, new ArrayList<>());
        episodesController.setEpisodes(EpisodeType.EPISODES_COMING, new ArrayList<>());
        episodesController.getEpisodesShows(EpisodeType.WATCH_BY_SHOW).clear();
        episodesController.getEpisodesShows(EpisodeType.ACQUIRE_BY_SHOW).clear();
        episodesController.getEpisodesShows(EpisodeType.COMING_BY_SHOW).clear();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        user = new User(
                sharedPref.getString("username", User.USERNAME),
                sharedPref.getString("UserPassword", User.PASSWORD)
        );
        loadEpisodesData(() -> runOnUiThread(() -> {
            swipeRefreshLayout.setRefreshing(false);
            if (exception) {
                exception = false;
                exceptionErrorDialog(HomeActivity.this);
            } else {
                updateHomeButtonCounts();
            }
        }));
    }

    private void loadEpisodesData(Runnable onComplete) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        TaskRunner.getExecutor().execute(() -> {

            try {
                final EpisodesController episodesController = EpisodesController.getInstance();
                episodesController.setEpisodes(EpisodeType.EPISODES_TO_WATCH, service.retrieveEpisodes(EpisodeType.EPISODES_TO_WATCH, user));
                episodesController.AddToWatchShow(episodesController.getEpisodes(EpisodeType.EPISODES_TO_WATCH));

                String acquire = sharedPref.getString("ACQUIRE_KEY", "0");
                if (acquire != null && acquire.equals("1")) {
                    EpisodesController.getInstance().setEpisodes(EpisodeType.EPISODES_TO_YESTERDAY1, service.retrieveEpisodes(EpisodeType.EPISODES_TO_YESTERDAY1, user));
                    EpisodesController.getInstance().addEpisodes(EpisodeType.EPISODES_TO_YESTERDAY2, service.retrieveEpisodes(EpisodeType.EPISODES_TO_YESTERDAY2, user));
                    episodesController.AddToAcquireShow(episodesController.getEpisodes(EpisodeType.EPISODES_TO_YESTERDAY1));
                } else {
                    EpisodesController.getInstance().setEpisodes(EpisodeType.EPISODES_TO_ACQUIRE, service.retrieveEpisodes(EpisodeType.EPISODES_TO_ACQUIRE, user));
                    episodesController.AddToAcquireShow(episodesController.getEpisodes(EpisodeType.EPISODES_TO_ACQUIRE));
                }
                episodesController.setEpisodes(EpisodeType.EPISODES_COMING, service.retrieveEpisodes(EpisodeType.EPISODES_COMING, user));
                episodesController.AddToComingShow(episodesController.getEpisodes(EpisodeType.EPISODES_COMING));

                resetPageFilters(user);

            } catch (InternetConnectivityException e) {
                exception = true;
            } catch (Exception e) {
                String message = "Error in background task";
                Log.e(LOG_TAG, message, e);
            }

            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    private void updateHomeButtonCounts() {
        btnWatched.setText(getString(R.string.watchhome, EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_TO_WATCH)));
        btnAcquired.setText(getString(R.string.acquirehome, EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_TO_ACQUIRE)));

        btn_ShowWatchNew.setText(getString(R.string.newWatchhome, EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_TO_WATCH)));
        btn_ShowAcquireNew.setText(getString(R.string.newAcquirehome, EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_TO_ACQUIRE)));
        btn_ShowListing.setText(getString(R.string.newCominghome, EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_COMING)));
        btnComing.setText(getString(R.string.cominghome, EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_COMING)));
    }


    private void populateFavouriteShowRuntimes() {
        ShowService showService = new ShowService();
        try {
            showService.getFavoriteOrIgnoredShows(user, ShowType.FAVOURITE_SHOWS,
                    (processed, total, showName) -> {
                        runOnUiThread(() -> {
                            if (runtimeProgressDialog != null && runtimeProgressDialog.isShowing()) {
                                runtimeProgressDialog.setMessage(
                                        "Processing " + processed + " of " + total + " shows\n"
                                                + "Current: " + showName);
                            }
                        });
                    });
        } catch (InternetConnectivityException e) {
            Log.e(LOG_TAG, "Could not connect to populate show runtimes", e);
        } catch (LoginFailedException e) {
            Log.e(LOG_TAG, "Login failed while populating show runtimes", e);
        }
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

    @Override
    protected void onResume() {
        super.onResume();
/*
        //pass a value that a setting has changed.
        if() {
            super.recreate();
        }*/
        bottomNavigationView.getMenu().getItem(0).setChecked(true);
        btnWatched.setText(getString(R.string.watchhome, EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_TO_WATCH)));
        btnAcquired.setText(getString(R.string.acquirehome, EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_TO_ACQUIRE)));
        btn_ShowWatchNew.setText(getString(R.string.newWatchhome, EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_TO_WATCH)));
        btn_ShowAcquireNew.setText(getString(R.string.newAcquirehome, EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_TO_ACQUIRE)));
        btn_ShowListing.setText(getString(R.string.newCominghome, EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_COMING)));
        btnComing.setText(getString(R.string.cominghome, EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_COMING)));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGIN_RESULT && resultCode == RESULT_OK)
            getEpisodesInLoadingDialog();
        if (requestCode == SETTINGS_RESULT && resultCode == RESULT_OK) {
            EpisodesController.getInstance().deleteAll();
            finish();

            Intent homeActivity = new Intent(this.getApplicationContext(), HomeActivity.class);
            startActivity(homeActivity);
        }
        if (requestCode == LOGIN_RESULT && resultCode != RESULT_OK) {
            finish();
        }
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
            case RUNTIME_LOADING_DIALOG:
                runtimeProgressDialog = new ProgressDialog(this);
                runtimeProgressDialog.setMessage(this.getString(R.string.processingFavouriteShows));
                runtimeProgressDialog.setCancelable(false);
                dialog = runtimeProgressDialog;
                break;
            default:
                dialog = super.onCreateDialog(id);
                break;
        }
        return dialog;
    }

    private void init() {
        res = getResources();
        conf = res.getConfiguration();
    }


    public void onManageClick(View v) {
        Intent manageShowsActivity = new Intent(this.getApplicationContext(), ShowManagementPortalActivity.class);
        startActivity(manageShowsActivity);
    }

    public void onLogoutClick(Context context) {

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
        dialog.setTitle(R.string.logoutDialogTitle);
        dialog.setMessage(R.string.logoutDialogMessage);
        dialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                logout();
            }
        });
        dialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialog.setCancelable(false);
        dialog.create();
        dialog.show();
    }


    public void exceptionErrorDialog(Context context) {

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
        dialog.setTitle(R.string.exceptionDialogTitle);
        dialog.setMessage(R.string.internetConnectionFailureTryAgain);
        dialog.setPositiveButton(R.string.refresh, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getEpisodesInLoadingDialog();
                dialog.dismiss();
            }
        });
        dialog.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });

        dialog.setCancelable(false);
        dialog.create();
        dialog.show();
    }

    private void logout() {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.remove("username");
        prefEditor.remove("UserPassword");
        prefEditor.apply();
        EpisodesController.getInstance().deleteAll();
        openLoginActivity();
    }

    private void openLoginActivity() {
        Intent loginSubActivity = new Intent(this.getApplicationContext(), LoginActivity.class);
        startActivityForResult(loginSubActivity, LOGIN_RESULT);
    }

    public void onAboutClick(View v) {
        Intent aboutApp = new Intent(this.getApplicationContext(), AboutActivity.class);
        startActivity(aboutApp);
    }

    public void onTodoClick(View v) {
        Intent changeLog = new Intent(this.getApplicationContext(), ChangelogActivity.class);
        startActivity(changeLog);
    }

    public void onRandomClick(View v) {
        //Intent randomActivity = new Intent(this.getApplicationContext(), RandomEpPickerActivity.class);
        //startActivity(randomActivity);

        //trying to just use the normal episode detail instead of the random one above.

        List<Show> showList = EpisodesController.getInstance().getRandomWatchEpisodeShowList();
        Show show = showList.get(0);

        Episode randomEpisode = show.getFirstEpisode();
        Intent episodeDetailsSubActivity = new Intent(this.getApplicationContext(), EpisodeDetailsActivity.class);
        episodeDetailsSubActivity.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, randomEpisode);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_WATCH);
        episodeDetailsSubActivity.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, randomEpisode.getMyEpisodeID());
        episodeDetailsSubActivity.putExtra("Title", randomEpisode.getShowName());
        startActivity(episodeDetailsSubActivity);
    }

    public void onSettingsClick(View v) {
        Intent settingsActivity = new Intent(this.getApplicationContext(), SettingsScreenActivity.class);
        startActivity(settingsActivity);
    }


    public void onNewComingShowListingClick(View v) {

        Intent newComingShowListing = new Intent(this.getApplicationContext(), ShowListingActivity.class);
        newComingShowListing.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_COMING);
        startActivity(newComingShowListing);
    }

    public void onNewWatchShowListingClick(View v) {

        Intent newWatchShowListing = new Intent(this.getApplicationContext(), ShowListingActivity.class);
        newWatchShowListing.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_WATCH);
        startActivity(newWatchShowListing);
    }


    public void onNewAcquireShowListingClick(View v) {

        Intent newWatchShowListing = new Intent(this.getApplicationContext(), ShowListingActivity.class);
        newWatchShowListing.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_ACQUIRE);
        startActivity(newWatchShowListing);
    }


    public static Context getContext() {
        return sContext;
    }
}
