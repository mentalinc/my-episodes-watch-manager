package eu.vranckaert.episodeWatcher.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

import eu.vranckaert.episodeWatcher.R;
import eu.vranckaert.episodeWatcher.constants.ActivityConstants;
import eu.vranckaert.episodeWatcher.constants.MyEpisodeConstants;
import eu.vranckaert.episodeWatcher.controllers.EpisodesController;
import eu.vranckaert.episodeWatcher.domain.User;
import eu.vranckaert.episodeWatcher.enums.EpisodeType;
import eu.vranckaert.episodeWatcher.enums.ListMode;
import eu.vranckaert.episodeWatcher.exception.InternetConnectivityException;
import eu.vranckaert.episodeWatcher.pager.HorizontalPager;
import eu.vranckaert.episodeWatcher.pager.PagerControl;
import eu.vranckaert.episodeWatcher.preferences.Preferences;
import eu.vranckaert.episodeWatcher.preferences.PreferencesKeys;
import eu.vranckaert.episodeWatcher.service.EpisodesService;
import eu.vranckaert.episodeWatcher.service.UserService;

public class HomeActivity extends Activity {
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
    private UserService userService;
    private static Context sContext;

    private boolean exception;

    private Button btnWatched;
    private Button btnAcquired;

    private Intent watchIntent;
    private Intent acquireIntent;
    private Intent comingIntent;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home_menu, menu);
        return true;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        init();
        userService = new UserService();
        checkPreferences();
        String LanguageCode = Preferences.getPreference(this, PreferencesKeys.LANGUAGE_KEY);


        //fix issue where app run and no days back has been set by the user.
        Preferences.getPreference(this, PreferencesKeys.CACHE_EPISODES_CACHE_AGE);
        if (Objects.equals(Preferences.getPreference(this, PreferencesKeys.CACHE_EPISODES_CACHE_AGE), "")) {
            MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE = "Disabled";
            Preferences.setPreference(this, PreferencesKeys.CACHE_EPISODES_CACHE_AGE, MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE);
        } else {
            MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE = Preferences.getPreference(this, PreferencesKeys.CACHE_EPISODES_CACHE_AGE);
        }

        Preferences.getPreference(this, PreferencesKeys.DAYS_BACKWARDCP);
        if (Objects.equals(Preferences.getPreference(this, PreferencesKeys.DAYS_BACKWARDCP), "")) {
            MyEpisodeConstants.DAYS_BACK_CP = "365";
            Preferences.setPreference(this, PreferencesKeys.DAYS_BACKWARDCP, MyEpisodeConstants.DAYS_BACK_CP);
        } else {
            MyEpisodeConstants.DAYS_BACK_CP = Preferences.getPreference(this, PreferencesKeys.DAYS_BACKWARDCP);
        }

        MyEpisodeConstants.DAYS_BACK_ENABLED = Preferences.getPreferenceBoolean(this, PreferencesKeys.DAYS_BACKWARD_ENABLED_KEY, false);
        MyEpisodeConstants.CACHE_EPISODES_ENABLED = Preferences.getPreferenceBoolean(this, PreferencesKeys.CACHE_EPISODES_ENABLED_KEY, false);
        MyEpisodeConstants.SHOW_RUNTIME_ENABLED = Preferences.getPreferenceBoolean(this, PreferencesKeys.RUNTIME_ENABLED_KEY, false);


        MyEpisodeConstants.SHOW_LISTING_UNACQUIRED_ENABLED = Preferences.getPreferenceBoolean(this, PreferencesKeys.SHOW_LISTING_UNACQUIRED_KEY, false);
        MyEpisodeConstants.SHOW_LISTING_UNWATCHED_ENABLED = Preferences.getPreferenceBoolean(this, PreferencesKeys.SHOW_LISTING_UNWATCHED_KEY, false);
        MyEpisodeConstants.SHOW_LISTING_IGNORED_ENABLED = Preferences.getPreferenceBoolean(this, PreferencesKeys.SHOW_LISTING_IGNORED_KEY, false);
        MyEpisodeConstants.SHOW_LISTING_PILOTS_ENABLED = Preferences.getPreferenceBoolean(this, PreferencesKeys.SHOW_LISTING_PILOTS_KEY, false);
        MyEpisodeConstants.SHOW_LISTING_LOCALIZED_AIRDATES__ENABLED = Preferences.getPreferenceBoolean(this, PreferencesKeys.SHOW_LISTING_LOCALIZED_AIRDATES_KEY, false);





        conf.locale = new Locale(LanguageCode);
        res.updateConfiguration(conf, null);
        openLoginActivity();

        setTheme(Preferences.getPreferenceInt(this, PreferencesKeys.THEME_KEY) == 0 ? android.R.style.Theme_Material_Light : android.R.style.Theme_Material);
        super.onCreate(savedInstanceState);
        this.service = new EpisodesService();
        sContext = getApplicationContext();

        setContentView(R.layout.main);
        user = new User(
                Preferences.getPreference(this, User.USERNAME),
                Preferences.getPreference(this, User.PASSWORD)
        );

        final PagerControl control = findViewById(R.id.control);
        final HorizontalPager pager = findViewById(R.id.pager);
        control.setNumPages(pager.getChildCount());

        MyEpisodeConstants.CONTEXT = getApplicationContext();

        pager.addOnScrollListener(new HorizontalPager.OnScrollListener() {
            public void onScroll(int scrollX) {
                float scale = (float) (pager.getPageWidth() * pager.getChildCount()) / (float) control.getWidth();
                control.setPosition((int) (scrollX / scale));
            }

            public void onViewScrollFinished(int currentPage) {
                control.setCurrentPage(currentPage);
/*
                if (currentPage == 0)
                    ((ImageView) findViewById(R.id.menu_indicator)).setImageResource(R.drawable.home_indicator1);
                else
                    ((ImageView) findViewById(R.id.menu_indicator)).setImageResource(R.drawable.home_indicator2)*/
            }
        });

        String[] showOrderOptions = getResources().getStringArray(R.array.showOrderOptionsValues);

        btnWatched = findViewById(R.id.btn_watched);
        watchIntent = new Intent().setClass(this, EpisodeListingActivity.class).putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_WATCH);
        String watch_sorting = Preferences.getPreference(this, PreferencesKeys.WATCH_SHOW_SORTING_KEY);
        if (watch_sorting.equals(showOrderOptions[3])) {
            watchIntent.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_DATE);
        } else {
            watchIntent.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_SHOW);
        }
        btnWatched.setOnClickListener(v -> startActivity(watchIntent));

        acquireIntent = new Intent().setClass(this, EpisodeListingActivity.class).putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_ACQUIRE);
        String acquire_sorting = Preferences.getPreference(this, PreferencesKeys.ACQUIRE_SHOW_SORTING_KEY);
        if (acquire_sorting.equals(showOrderOptions[3])) {
            acquireIntent.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_DATE);
        } else {
            acquireIntent.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_SHOW);
        }
        btnAcquired = findViewById(R.id.btn_acquired);
        btnAcquired.setOnClickListener(v -> startActivity(acquireIntent));
        if (Preferences.getPreferenceBoolean(this, PreferencesKeys.DISABLE_ACQUIRE, false)) {
            btnAcquired.setVisibility(View.GONE);
        }

        Button btnComing = findViewById(R.id.btn_coming);
        comingIntent = new Intent().setClass(this, EpisodeListingActivity.class).putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_COMING);
        String coming_sorting = Preferences.getPreference(this, PreferencesKeys.COMING_SHOW_SORTING_KEY);
        if (coming_sorting.equals(showOrderOptions[3])) {
            comingIntent.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_DATE);
        } else {
            comingIntent.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_SHOW);
        }
        btnComing.setOnClickListener(v -> startActivity(comingIntent));
        if (Preferences.getPreferenceBoolean(this, PreferencesKeys.DISABLE_COMING, false)) {
            btnComing.setVisibility(View.GONE);
        }

        Button btnMore = findViewById(R.id.btn_more);
        btnMore.setOnClickListener(v -> pager.scrollRight());
    }

    private void getEpisodesInLoadingDialog() {
        final EpisodesController episodesController = EpisodesController.getInstance();
        user = new User(
                Preferences.getPreference(this, User.USERNAME),
                Preferences.getPreference(this, User.PASSWORD)
        );
        if (episodesController.areListsEmpty()) {
            AsyncTask<Object, Object, Object> asyncTask = new AsyncTask<Object, Object, Object>() {

                @Override
                protected void onPreExecute() {
                    showDialog(EPISODE_LOADING_DIALOG);

                    if (MyEpisodeConstants.CACHE_EPISODES_ENABLED) {
                        showDialog(EPISODE_LOADING_DIALOG_CACHE);
                    } else {
                        showDialog(EPISODE_LOADING_DIALOG);
                    }
                }

                @Override
                protected Object doInBackground(Object... objects) {
                    try {
                        episodesController.setEpisodes(EpisodeType.EPISODES_TO_WATCH, service.retrieveEpisodes(EpisodeType.EPISODES_TO_WATCH, user));
                        String acquire = Preferences.getPreference(HomeActivity.this, PreferencesKeys.ACQUIRE_KEY);
                        if (acquire != null && acquire.equals("1")) {
                            EpisodesController.getInstance().setEpisodes(EpisodeType.EPISODES_TO_YESTERDAY1, service.retrieveEpisodes(EpisodeType.EPISODES_TO_YESTERDAY1, user));
                            EpisodesController.getInstance().addEpisodes(EpisodeType.EPISODES_TO_YESTERDAY2, service.retrieveEpisodes(EpisodeType.EPISODES_TO_YESTERDAY2, user));
                        } else {
                            EpisodesController.getInstance().setEpisodes(EpisodeType.EPISODES_TO_ACQUIRE, service.retrieveEpisodes(EpisodeType.EPISODES_TO_ACQUIRE, user));
                        }
                        episodesController.setEpisodes(EpisodeType.EPISODES_COMING, service.retrieveEpisodes(EpisodeType.EPISODES_COMING, user));


                        resetPageFilters(user);

                    } catch (InternetConnectivityException e) {
                        exception = true;
                    } catch (Exception e) {
                        //e.printStackTrace();
                        String message = "Error in backgroud task";
                        Log.e(LOG_TAG, message, e);

                    }
                    return 100L;
                }

                @Override
                protected void onPostExecute(Object o) {
                    removeDialog(EPISODE_LOADING_DIALOG);
                    removeDialog(EPISODE_LOADING_DIALOG_CACHE);

                    if (exception) {
                        exception = false;
                        showDialog(EXCEPTION_DIALOG);
                    } else {
                        btnWatched.setText(getString(R.string.watchhome, EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_TO_WATCH)));
                        btnAcquired.setText(getString(R.string.acquirehome, EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_TO_ACQUIRE)));
                    }
                }
            };
            asyncTask.execute();
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
                if(urlParameters.length() <1)
                    urlParameters += "eps_filters%5B%5D=1";
                else{
                    urlParameters += "&eps_filters%5B%5D=1";
                }

                Log.d(LOG_TAG, "SHOW_LISTING_UNACQUIRED_ENABLED" + " " + urlParameters);
            }
            if (MyEpisodeConstants.SHOW_LISTING_UNWATCHED_ENABLED) {
                //Unwatched 2
                if(urlParameters.length() < 1)
                urlParameters += "eps_filters%5B%5D=2";
                else{
                    urlParameters += "&eps_filters%5B%5D=2";
                }
                Log.d(LOG_TAG, "SHOW_LISTING_UNWATCHED_ENABLED" + " " +urlParameters);

            }

            if (MyEpisodeConstants.SHOW_LISTING_IGNORED_ENABLED) {
                //Ignored 4
                if(urlParameters.length() < 1)
                urlParameters += "eps_filters%5B%5D=4";
                else{
                    urlParameters += "&eps_filters%5B%5D=4";
                }
                Log.d(LOG_TAG, "SHOW_LISTING_IGNORED_ENABLED" + " " + urlParameters);
            }

            if (MyEpisodeConstants.SHOW_LISTING_PILOTS_ENABLED) {
                //Pilots 2048
                if(urlParameters.length() < 1)
                urlParameters += "eps_filters%5B%5D=2048";
                else{
                    urlParameters += "&eps_filters%5B%5D=2048";
                }
                Log.d(LOG_TAG, "SHOW_LISTING_PILOTS_ENABLED" + " " + urlParameters);

            }


            if (MyEpisodeConstants.SHOW_LISTING_LOCALIZED_AIRDATES__ENABLED) {
                //Localized Airdate 4096
                if(urlParameters.length() < 1)
                urlParameters += "eps_filters%5B%5D=4096";
                else{
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
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"), 8);
            String result = reader.readLine();

        } catch (Exception e) {
            String message = "Error resetting episode filter";
            Log.e(LOG_TAG, message, e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        btnWatched.setText(getString(R.string.watchhome, EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_TO_WATCH)));
        btnAcquired.setText(getString(R.string.acquirehome, EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_TO_ACQUIRE)));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
            case LOGOUT_DIALOG:
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setTitle(R.string.logoutDialogTitle)
                        .setMessage(R.string.logoutDialogMessage)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, (dialog14, which) -> logout())
                        .setNegativeButton(R.string.no, (dialog13, which) -> dialog13.cancel());
                dialog = alertBuilder.create();
                break;
            case EXCEPTION_DIALOG:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.exceptionDialogTitle)
                        .setMessage(R.string.internetConnectionFailureTryAgain)
                        .setCancelable(false)
                        .setPositiveButton(R.string.refresh, (dialog12, id12) -> {
                            removeDialog(EXCEPTION_DIALOG);
                            getEpisodesInLoadingDialog();
                        })
                        .setNegativeButton(R.string.close, (dialog1, id1) -> {
                            removeDialog(EXCEPTION_DIALOG);
                            finish();
                        });
                dialog = builder.create();
                break;
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
            default:
                dialog = super.onCreateDialog(id);
                break;
        }
        return dialog;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.preferences:
                openPreferencesActivity();
                return true;
        }
        return false;
    }

    private void init() {
        res = getResources();
        conf = res.getConfiguration();
    }

    /**
     * Check if all preferences exist upon loading the application.
     */
    private void checkPreferences() {
        //Checks preference for show sorting and sets the default to ascending (A-Z)
        String[] episodeOrderOptions = getResources().getStringArray(R.array.episodeOrderOptionsValues);
        Preferences.checkDefaultPreference(this, PreferencesKeys.EPISODE_SORTING_KEY, episodeOrderOptions[0]);
        //Checks preference for episode sorting and sets default to ascending (oldest episode on top)
        String[] showOrderOptions = getResources().getStringArray(R.array.showOrderOptionsValues);
        Preferences.checkDefaultPreference(this, PreferencesKeys.WATCH_SHOW_SORTING_KEY, showOrderOptions[0]);
        Preferences.checkDefaultPreference(this, PreferencesKeys.ACQUIRE_SHOW_SORTING_KEY, showOrderOptions[0]);
        Preferences.checkDefaultPreference(this, PreferencesKeys.COMING_SHOW_SORTING_KEY, showOrderOptions[3]);
        Preferences.checkDefaultPreference(this, PreferencesKeys.LANGUAGE_KEY, conf.locale.getLanguage());
        Preferences.checkDefaultPreference(this, PreferencesKeys.ACQUIRE_KEY, "0");
        Preferences.checkDefaultPreference(this, PreferencesKeys.DAYS_BACKWARDCP, "365");
        Preferences.checkDefaultPreference(this, PreferencesKeys.CACHE_EPISODES_CACHE_AGE, "0");
        Preferences.getPreferenceBoolean(this, PreferencesKeys.DISABLE_COMING, false);
    }

    private void openPreferencesActivity() {
        Intent preferencesActivity = new Intent(this.getApplicationContext(), PreferencesActivity.class);
        startActivityForResult(preferencesActivity, SETTINGS_RESULT);
    }

    public void onManageClick(View v) {
        Intent manageShowsActivity = new Intent(this.getApplicationContext(), ShowManagementPortalActivity.class);
        startActivity(manageShowsActivity);
    }

    public void onLogoutClick(View v) {
        showDialog(LOGOUT_DIALOG);
    }

    private void logout() {
        Preferences.removePreference(this, User.USERNAME);
        Preferences.removePreference(this, User.PASSWORD);
        EpisodesController.getInstance().deleteAll();
        openLoginActivity();
    }

    private void openLoginActivity() {
        Intent loginSubActivity = new Intent(this.getApplicationContext(), LoginActivity.class);
        startActivityForResult(loginSubActivity, LOGIN_RESULT);
    }

    public void onAboutClick(View v) {
        Intent manageShowsActivity = new Intent(this.getApplicationContext(), AboutActivity.class);
        startActivity(manageShowsActivity);
    }

    public void onTodoClick(View v) {
        Intent manageShowsActivity = new Intent(this.getApplicationContext(), ChangelogActivity.class);
        startActivity(manageShowsActivity);
    }

    public void onRandomClick(View v) {
        Intent randomActivity = new Intent(this.getApplicationContext(), RandomEpPickerActivity.class);
        startActivity(randomActivity);
    }


    public static Context getContext() {
        return sContext;
    }
}
