package nz.mentalinc.episodeWatcher.activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;


import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.ShowListingFrag;
import nz.mentalinc.episodeWatcher.constants.ActivityConstants;
import nz.mentalinc.episodeWatcher.constants.MyEpisodeConstants;
import nz.mentalinc.episodeWatcher.controllers.EpisodesController;
import nz.mentalinc.episodeWatcher.domain.User;
import nz.mentalinc.episodeWatcher.enums.EpisodeType;
import nz.mentalinc.episodeWatcher.enums.ListMode;
import nz.mentalinc.episodeWatcher.exception.InternetConnectivityException;
import nz.mentalinc.episodeWatcher.service.EpisodesService;
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
    private UserService userService;
    private static Context sContext;


    private boolean exception;

    private Button btnWatched;
    private Button btnAcquired;

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

        //Below makes sure all settings have their default value set to limit null errors etc
        PreferenceManager.setDefaultValues(getBaseContext(),R.xml.settings_screen,false);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String LanguageCode = sharedPref.getString("language","en");
        String themeSetting = sharedPref.getString("ThemeSetting","0");
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

        MyEpisodeConstants.DAYS_BACK_CP =sharedPref.getString("daysBack","365");
        MyEpisodeConstants.CACHE_EPISODES_CACHE_AGE = sharedPref.getString("CacheFileAge","false");
        MyEpisodeConstants.DAYS_BACK_ENABLED = sharedPref.getBoolean("daysBackwardEnable",false);
        MyEpisodeConstants.CACHE_EPISODES_ENABLED =  sharedPref.getBoolean("CacheEpisodes",false);
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

        setContentView(R.layout.main);
        user = new User(
                sharedPref.getString("username",User.USERNAME),
                sharedPref.getString("UserPassword",User.PASSWORD)
        );


        MyEpisodeConstants.CONTEXT = getApplicationContext();


        String[] showOrderOptions = getResources().getStringArray(R.array.showOrderOptionsValues);

        btnWatched = findViewById(R.id.btn_watched);
        watchIntent = new Intent().setClass(this, EpisodeListingActivity.class).putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_WATCH);

        String watch_sorting = sharedPref.getString("showWatchOrder","show_myepisodes_default_sort");
        if (watch_sorting.equals(showOrderOptions[3])) {
            watchIntent.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_DATE);
        } else {
            watchIntent.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_SHOW);
        }
        watchIntent.putExtra("Title", getString(R.string.watch));
        btnWatched.setOnClickListener(v -> startActivity(watchIntent));

        acquireIntent = new Intent().setClass(this, EpisodeListingActivity.class).putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_ACQUIRE );

        String acquire_sorting =sharedPref.getString("ACQUIRE_KEY","0");
        if (acquire_sorting.equals(showOrderOptions[3])) {
            acquireIntent.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_DATE);
        } else {
            acquireIntent.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_SHOW);
        }
        acquireIntent.putExtra("Title", getString(R.string.acquire));
        btnAcquired = findViewById(R.id.btn_acquired);
        btnAcquired.setOnClickListener(v -> startActivity(acquireIntent));


        if (sharedPref.getBoolean("disableAcquire",false)) {
            btnAcquired.setVisibility(View.GONE);
        }

        Button btnComing = findViewById(R.id.btn_coming);
        comingIntent = new Intent().setClass(this, EpisodeListingActivity.class).putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_COMING);

        String coming_sorting = sharedPref.getString("showComingOrder","show_myepisodes_default_sort");
        if (coming_sorting.equals(showOrderOptions[3])) {
            comingIntent.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_DATE);
        } else {
            comingIntent.putExtra(ActivityConstants.EXTRA_BUILD_VAR_LIST_MODE, ListMode.EPISODES_BY_SHOW);
        }
        comingIntent.putExtra("Title", getString(R.string.coming));
        btnComing.setOnClickListener(v -> startActivity(comingIntent));

        if (sharedPref.getBoolean("disableComing",false)) {
            btnComing.setVisibility(View.GONE);
        }


        androidx.appcompat.view.menu.ActionMenuItemView appBarLogout =  findViewById(R.id.logout);
        appBarLogout.setOnClickListener(v -> {

            Log.w(LOG_TAG, "logout button clicked.");
            onLogoutClick(HomeActivity.this);
        });


        androidx.appcompat.view.menu.ActionMenuItemView appBarSettings =  findViewById(R.id.btn_settings);
        appBarSettings.setOnClickListener(v -> {

            Log.w(LOG_TAG, "Settings button clicked.");
            onSettingsClick(v);
        });
    }

    private void getEpisodesInLoadingDialog() {
        final EpisodesController episodesController = EpisodesController.getInstance();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        user = new User(

                sharedPref.getString("username",User.USERNAME),
                sharedPref.getString("UserPassword",User.PASSWORD)
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

                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                        String acquire = sharedPref.getString("ACQUIRE_KEY","0");
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
                        String message = "Error in background task";
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

                        exceptionErrorDialog(HomeActivity.this);
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
/*
        //pass a value that a setting has changed.
        if() {
            super.recreate();
        }*/
        btnWatched.setText(getString(R.string.watchhome, EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_TO_WATCH)));
        btnAcquired.setText(getString(R.string.acquirehome, EpisodesController.getInstance().getEpisodesCount(EpisodeType.EPISODES_TO_ACQUIRE)));
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
        dialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                logout();
            }
        });
        dialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
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
        dialog.setPositiveButton(R.string.refresh, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                getEpisodesInLoadingDialog();
                dialog.dismiss();
            }
        });
        dialog.setNegativeButton(R.string.close, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
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
        Intent randomActivity = new Intent(this.getApplicationContext(), RandomEpPickerActivity.class);
        startActivity(randomActivity);
    }

    public void onSettingsClick(View v) {
        SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
        Intent settingsActivity = new Intent(this.getApplicationContext(), SettingsScreenActivity.class);
        startActivity(settingsActivity);
    }


    public void onShowListingClick(View v) {
        Intent showListing = new Intent(this.getApplicationContext(), ShowListingActivity.class);
        //need to start a fragment here not activity
        startActivity(showListing);
    }

    public static Context getContext() {
        return sContext;
    }
}
