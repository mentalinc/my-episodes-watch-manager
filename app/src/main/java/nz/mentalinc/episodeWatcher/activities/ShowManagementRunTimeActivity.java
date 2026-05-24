package nz.mentalinc.episodeWatcher.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.room.Room;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.database.AppDatabase;
import nz.mentalinc.episodeWatcher.enums.ShowType;
import nz.mentalinc.episodeWatcher.database.SeriesDAO;
import nz.mentalinc.episodeWatcher.domain.Show;
import nz.mentalinc.episodeWatcher.domain.ShowRuntimeAscendingComparator;
import nz.mentalinc.episodeWatcher.domain.User;
import nz.mentalinc.episodeWatcher.service.EpisodeRuntime;
import nz.mentalinc.episodeWatcher.utils.InputFilterMinMax;
import nz.mentalinc.episodeWatcher.utils.TaskRunner;


public class ShowManagementRunTimeActivity extends ListActivity {
    private static final String LOG_TAG = ShowManagementRunTimeActivity.class.getSimpleName();

    private static final int DIALOG_LOADING = 0;
    private static final int DIALOG_EXCEPTION = 1;
    private static final int DIALOG_FINISHED = 2;
    private static final int DIALOG_UPDATE_RUNTIME = 3;

    private User user;
    private ShowManagementRunTimeActivity.ShowAdapter showAdapter;
    private final List<Show> shows = new ArrayList<>(0);

    private Integer exceptionMessageResId = null;
    private Integer showListPosition = null;
    private String title;

    private final NavigationBarView.OnItemSelectedListener navigationItemSelectedListener =
            new NavigationBarView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    final int itemId = item.getItemId();
                    if (itemId == R.id.barFavouriteShows) {
                        openFavouriteOrIgnoredShows(ShowType.FAVOURITE_SHOWS);
                        return true;
                    } else if (itemId == R.id.barIgnoredShows) {
                        openFavouriteOrIgnoredShows(ShowType.IGNORED_SHOWS);
                        return true;
                    } else if (itemId == R.id.barAddShows) {
                        openSearchActivity();
                        return true;
                    } else if (itemId == R.id.barShowRuntime) {
                        openRunTimeActivity();
                        return true;
                    }
                    return false;
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
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
        super.onCreate(savedInstanceState);
        init();

        ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {
            Intent home = new Intent(ShowManagementRunTimeActivity.this, HomeActivity.class);
            home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(home);
        });

    }

    private void init() {
        setContentView(R.layout.show_management);
        findViewById(R.id.appBarLayoutShowManagement).setZ(100f);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigationManage);
        bottomNav.setSelectedItemId(R.id.barShowRuntime);
        bottomNav.setOnItemSelectedListener(navigationItemSelectedListener);

        Bundle data = this.getIntent().getExtras();
        title = (String) data.getSerializable("Title");
        Toolbar toolbar = findViewById(R.id.topAppBarShowManagement);
        toolbar.setTitle(title);

        initializeShowList();
    }

    private void initializeShowList() {
        showAdapter = new ShowManagementRunTimeActivity.ShowAdapter(this, shows);
        setListAdapter(showAdapter);

        //read database of runtime and put on the page...
        populateShowRuntimeList();
        showAdapter.notifyDataSetChanged();

    }


    private void getRuntimeShows() {

        AppDatabase database = Room.databaseBuilder(nz.mentalinc.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                // .allowMainThreadQueries()   //Allows room to do operation on main thread
                .fallbackToDestructiveMigration()
                .build();

        SeriesDAO seriesDAO = database.getSeriesDAO();
        List<EpisodeRuntime> runtimeList = seriesDAO.getEpisodeRuntime();


        for (int i = 0; i < runtimeList.size(); i++) {
            EpisodeRuntime showRuntime = runtimeList.get(i);

            if (showRuntime.getShowRuntime() == null || showRuntime.getShowRuntime().equals("null")) {

                //get the runtime for the null from TVMaze
                HashMap<String, String> showSummaryHashMap = new HashMap<>() {{
                    put("a", "b");
                }};

                downloadShowSummary(showSummaryHashMap, showRuntime.getShowTVMazeID());

                shows.add(new Show(showRuntime.getShowName(), showRuntime.getShowRuntime(), showRuntime.getShowMyEpsID()));
                //shows.add(new Show(showRuntime.getShowName(), showRuntime.getShowRuntime(),  showSummaryHashMap.get("showRuntime")));
            } else {
                shows.add(new Show(showRuntime.getShowName(), showRuntime.getShowRuntime(), showRuntime.getShowMyEpsID()));
            }
        }
        Collections.sort(shows, new ShowRuntimeAscendingComparator());

        database.close();
    }


    private void downloadShowSummary(HashMap<String, String> showSummaryHash, String... params) {
        TaskRunner.getExecutor().execute(() -> {
            AppDatabase database = Room.databaseBuilder(nz.mentalinc.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                    .fallbackToDestructiveMigration()
                    .build();

            SeriesDAO seriesDAO = database.getSeriesDAO();
            EpisodeRuntime showInfo = seriesDAO.getEpisodeRuntimeWithTVMazeId(params[0]);
            String showRuntime = showInfo.getShowRuntime();

            HttpsURLConnection connection = null;
            BufferedReader reader = null;
            String episodeSummaryAPIURL = "https://api.tvmaze.com/shows/" + params[0];

            try {
                URL url = new URL(episodeSummaryAPIURL);
                connection = (HttpsURLConnection) url.openConnection();
                connection.connect();
                int code = connection.getResponseCode();
                Log.d(LOG_TAG, "API HTTP Status Code: " + code);

                if (code == 429) {
                    Thread.sleep(10000);
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.connect();
                }

                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuilder buffer = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                    buffer.append("\n");
                }

                String jsonString = buffer.toString();
                JSONObject jObj;

                try {
                    jObj = new JSONObject(jsonString);

                    showRuntime = jObj.getString("runtime");

                    if (showRuntime.equals("null") || showRuntime == null) {
                        showRuntime = jObj.getString("averageRuntime");
                    }

                    showSummaryHash.put("showRuntime", showRuntime);
                    EpisodeRuntime showSummaryInfo = seriesDAO.getEpisodeRuntimeWithMyEpsId(showInfo.getShowMyEpsID());

                    showSummaryInfo.setShowRuntime(showSummaryHash.get("showRuntime"));

                    seriesDAO.update(showSummaryInfo);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            database.close();
        });
    }

    protected void onPostExecute(HashMap<String, String> result) {
        showAdapter.notifyDataSetChanged();

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

    public void dialogUpdateRuntime(Context context) {

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        final EditText runTimeInput = new EditText(this);
        runTimeInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        runTimeInput.setTransformationMethod(new NumericKeyBoardTransformationMethod());
        runTimeInput.setText(shows.get(showListPosition).getRunTime());

        runTimeInput.setFilters(new InputFilter[]{new InputFilterMinMax("1", "150")}); //set 150 minutes as longest runtime
        // consider using this if it doesn't work properly due to entering values that are not ok - https://stackoverflow.com/questions/8806492/monodroid-set-max-value-for-edittext/13812853#13812853
        final InputMethodManager imm = (InputMethodManager) runTimeInput.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        Objects.requireNonNull(imm).showSoftInput(runTimeInput, InputMethodManager.SHOW_IMPLICIT);
        runTimeInput.requestFocus();


        dialog.setView(runTimeInput);
        dialog.setTitle(shows.get(showListPosition).getShowName());
        dialog.setMessage((R.string.runTimeEditMessage));
        dialog.setCancelable(true);
        dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newRuntimeValue = runTimeInput.getText().toString();
                //silent fail if user has entered a blank runtime
                if (runTimeInput.getText().toString().trim().length() < 1) {
                    // runTimeInput.setError("Error: Can't be blank");
                    String text = "Error: Runtime can't be blank!";

                    Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarShowManagement), text, Snackbar.LENGTH_LONG);
                    snackbar.show();


                } else if (runTimeInput.getText().toString().trim().equals(shows.get(showListPosition).getRunTime())) {
                    Context context = getApplicationContext();
                    String text = "Runtime unchanged";

                    Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarShowManagement), text, Snackbar.LENGTH_LONG);
                    snackbar.show();

                } else {
                    runTimeInput.setError(null);


                    AppDatabase database = Room.databaseBuilder(HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                            .allowMainThreadQueries()   //Allows room to do operation on main thread
                            .fallbackToDestructiveMigration()
                            .build();
                    SeriesDAO seriesDAO = database.getSeriesDAO();

                    //Updating an episodeRuntime
                    EpisodeRuntime epsRunTime = new EpisodeRuntime();
                    epsRunTime.setshowMyepsID(shows.get(showListPosition).getMyEpisodeID());
                    epsRunTime.setShowName(shows.get(showListPosition).getShowName());
                    // epsRunTime.setShowTVMazeID(epsRunTime.getShowTVMazeID());
                    epsRunTime.setShowRuntime(newRuntimeValue);
                    Log.d("epsRunTime: ", epsRunTime.toString());
                    seriesDAO.update(epsRunTime);


                    Context context = getApplicationContext();
                    String text = "Runtime for updated " + shows.get(showListPosition).getShowName() + " updated to " + newRuntimeValue + " mins";

                    Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarShowManagement), text, Snackbar.LENGTH_LONG);
                    snackbar.show();

                    populateShowRuntimeList();
                    showListPosition = null;
                    showAdapter.notifyDataSetChanged();
                    database.close();
                }
                dialog.dismiss();
            }
        });
        dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showListPosition = null;
                dialog.dismiss();
            }
        });

        dialog.setCancelable(false);
        dialog.create();
        dialog.show();
    }


    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
            case DIALOG_LOADING: {
                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setMessage(this.getString(R.string.progressLoadingTitle));
                progressDialog.setCancelable(false);
                dialog = progressDialog;
                break;
            }


            case DIALOG_FINISHED: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.showSearchFinished)
                        .setCancelable(false)
                        .setPositiveButton(R.string.done, (dialog14, id14) -> {
                            dialog14.dismiss();
                            finish();
                        })
                        .setNegativeButton(R.string.search, (dialog13, id13) -> dialog13.dismiss());
                dialog = builder.create();
                break;
            }
            case DIALOG_UPDATE_RUNTIME: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final EditText runTimeInput = new EditText(this);
                runTimeInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                runTimeInput.setTransformationMethod(new NumericKeyBoardTransformationMethod());
                runTimeInput.setText(shows.get(showListPosition).getRunTime());

                runTimeInput.setFilters(new InputFilter[]{new InputFilterMinMax("1", "150")}); //set 150 minutes as longest runtime
                // consider using this if it doesn't work properly due to entering values that are not ok - https://stackoverflow.com/questions/8806492/monodroid-set-max-value-for-edittext/13812853#13812853
                final InputMethodManager imm = (InputMethodManager) runTimeInput.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                Objects.requireNonNull(imm).showSoftInput(runTimeInput, InputMethodManager.SHOW_IMPLICIT);
                runTimeInput.requestFocus();


                builder.setView(runTimeInput);


                builder.setTitle(shows.get(showListPosition).getShowName())
                        //builder.setTitle(shows.get(showListPosition).toString()) //this works ad does what is required showing the runtime and name in the title
                        .setMessage((R.string.runTimeEditMessage))
                        .setCancelable(true)
                        .setPositiveButton(R.string.ok, (dialog12, id12) -> {
                            removeDialog(DIALOG_UPDATE_RUNTIME);
                            String newRuntimeValue = runTimeInput.getText().toString();
                            //silent fail if user has entered a blank runtime
                            if (runTimeInput.getText().toString().trim().length() < 1) {
                                // runTimeInput.setError("Error: Can't be blank");
                                Context context = getApplicationContext();
                                String text = "Error: Runtime can't be blank!";

                                Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarShowManagement), text, Snackbar.LENGTH_LONG);
                                snackbar.show();

                            } else if (runTimeInput.getText().toString().trim().equals(shows.get(showListPosition).getRunTime())) {
                                Context context = getApplicationContext();
                                String text = "Runtime unchanged";

                                Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarShowManagement), text, Snackbar.LENGTH_LONG);
                                snackbar.show();
                            } else {
                                runTimeInput.setError(null);


                                AppDatabase database = Room.databaseBuilder(HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                                        .allowMainThreadQueries()   //Allows room to do operation on main thread
                                        .fallbackToDestructiveMigration()
                                        .build();
                                SeriesDAO seriesDAO = database.getSeriesDAO();

                                //Updating an episodeRuntime
                                EpisodeRuntime epsRunTime = new EpisodeRuntime();
                                epsRunTime.setshowMyepsID(shows.get(showListPosition).getMyEpisodeID());
                                epsRunTime.setShowName(shows.get(showListPosition).getShowName());
                                // epsRunTime.setShowTVMazeID(epsRunTime.getShowTVMazeID());
                                epsRunTime.setShowRuntime(newRuntimeValue);
                                Log.d("epsRunTime: ", epsRunTime.toString());
                                seriesDAO.update(epsRunTime);


                                Context context = getApplicationContext();
                                String text = "Runtime for updated " + shows.get(showListPosition).getShowName() + " updated to " + newRuntimeValue + " mins";

                                Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarShowManagement), text, Snackbar.LENGTH_LONG);
                                snackbar.show();

                                populateShowRuntimeList();
                                showListPosition = null;
                                showAdapter.notifyDataSetChanged();
                                database.close();
                            }
                        })
                        //; //remove this ; if add the .negative back int
                        .setNegativeButton(R.string.cancel, (dialog1, id1) -> {
                            showListPosition = null;
                            removeDialog(DIALOG_UPDATE_RUNTIME);
                        });
                dialog = builder.create();

                dialog.setOnShowListener(dialogInterface -> runTimeInput.post(() -> {
                    final InputMethodManager imm1 = (InputMethodManager) runTimeInput.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm1.showSoftInput(runTimeInput, InputMethodManager.SHOW_IMPLICIT);
                    runTimeInput.requestFocus(); // needed if you have more then one input
                }));
            }
        }
        return dialog;
    }

    private static class NumericKeyBoardTransformationMethod extends PasswordTransformationMethod {
        @Override
        public CharSequence getTransformation(CharSequence source, View view) {
            return source;
        }
    }

    private void populateShowRuntimeList() {
        showDialog(DIALOG_LOADING);
        showAdapter.clear();
        TaskRunner.getExecutor().execute(() -> {
            getRuntimeShows();
            runOnUiThread(() -> {
                if (exceptionMessageResId != null && !exceptionMessageResId.equals("")) {
                    removeDialog(DIALOG_LOADING);
                    exceptionDialog(ShowManagementRunTimeActivity.this);
                } else {

                    try {

                       // showAdapter.notifyDataSetChanged();

                    } catch (Exception e) {
                        String message = "ShowFailure";
                        Log.e(LOG_TAG, message, e);
                    }

                    removeDialog(DIALOG_LOADING);
                }
            });
        });
    }

    @Override
    public void finish() {
        //   boolean showsAdded = false;
        //    if (showsAdded) {
        setResult(RESULT_OK);
        //    } else {
        //        setResult(RESULT_CANCELED);
        //     }
        super.finish();
    }

    public void onHomeClick(View v) {
        finish();
    }

    private void openSearchActivity() {
        Intent searchIntent = new Intent(this.getApplicationContext(), ShowManagementAddActivity.class);
        searchIntent.putExtra("Title", getString(R.string.addShow));
        startActivity(searchIntent);
    }

    private void openRunTimeActivity() {
        Intent runTimeIntent = new Intent(this.getApplicationContext(), ShowManagementRunTimeActivity.class);
        runTimeIntent.putExtra("Title", getString(R.string.ShowRuntime));
        startActivity(runTimeIntent);
    }

    private void openFavouriteOrIgnoredShows(ShowType showType) {
        Intent intent = new Intent(this.getApplicationContext(), ShowManagementActivity.class);
        intent.putExtra(ShowType.class.getSimpleName(), showType);
        if (showType.toString().equals("FAVOURITE_SHOWS"))
            intent.putExtra("Title", getString(R.string.favouriteShows));
        else if (showType.toString().equals("IGNORED_SHOWS"))
            intent.putExtra("Title", getString(R.string.ignoredShows));
        startActivity(intent);
    }

    private class ShowAdapter extends ArrayAdapter<Show> {
        private final List<Show> shows;

        ShowAdapter(Context context, List<Show> el) {
            super(context, R.layout.show_management_add_row, el);
            this.shows = el;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final int i = position;
            View row = convertView;
            if (row == null) {
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.show_management_add_row, parent, false);
            }

            TextView topText = row.findViewById(R.id.showNameSearchResult);

            Show show = shows.get(position);
            String topTextString = show.getRunTime() + " mins - " + show.getShowName();
            topText.setText(topTextString);
            row.setOnClickListener(view -> {
                showListPosition = i;
                //showDialog(DIALOG_UPDATE_RUNTIME);
                dialogUpdateRuntime(ShowManagementRunTimeActivity.this);
            });

            return row;
        }
    }
}

