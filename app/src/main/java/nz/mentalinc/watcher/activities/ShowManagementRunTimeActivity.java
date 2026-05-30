package nz.mentalinc.watcher.activities;

import android.app.AlertDialog;
import android.app.Dialog;
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
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;
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

import nz.mentalinc.watcher.R;
import nz.mentalinc.watcher.database.AppDatabase;
import nz.mentalinc.watcher.database.SeriesDAO;
import nz.mentalinc.watcher.domain.Show;
import nz.mentalinc.watcher.domain.ShowRuntimeAscendingComparator;
import nz.mentalinc.watcher.domain.User;
import nz.mentalinc.watcher.enums.ShowType;
import nz.mentalinc.watcher.service.EpisodeRuntime;
import nz.mentalinc.watcher.service.ShowService;
import nz.mentalinc.watcher.constants.ActivityConstants;
import nz.mentalinc.watcher.utils.InputFilterMinMax;
import nz.mentalinc.watcher.utils.TaskRunner;


public class ShowManagementRunTimeActivity extends AppCompatActivity {
    private static final String LOG_TAG = ShowManagementRunTimeActivity.class.getSimpleName();

    private static final String DIALOG_LOADING_TAG = "LOADING";
    private static final String DIALOG_FINISHED_TAG = "FINISHED";

    private User user;
    private ShowManagementRunTimeActivity.ShowAdapter showAdapter;
    private final List<Show> shows = new ArrayList<>(0);

    private Integer exceptionMessageResId = null;
    private Integer showListPosition = null;
    private String title;
    private ListView listView;

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
            default: //added for code quality
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
        title = data.getSerializable(ActivityConstants.EXTRA_TITLE, String.class);
        Toolbar toolbar = findViewById(R.id.topAppBarShowManagement);
        toolbar.setTitle(title);

        listView = findViewById(android.R.id.list);
        initializeShowList();
    }

    private void initializeShowList() {
        showAdapter = new ShowManagementRunTimeActivity.ShowAdapter(this, shows);
        listView.setAdapter(showAdapter);

        //read database of runtime and put on the page...
        populateShowRuntimeList();
        showAdapter.notifyDataSetChanged();

    }


    private void getRuntimeShows() {

        AppDatabase database = AppDatabase.getInstance(nz.mentalinc.watcher.activities.HomeActivity.getContext().getApplicationContext());

        SeriesDAO seriesDAO = database.getSeriesDAO();
        List<EpisodeRuntime> runtimeList = seriesDAO.getEpisodeRuntime();


        for (int i = 0; i < runtimeList.size(); i++) {
            EpisodeRuntime showRuntime = runtimeList.get(i);

            if (showRuntime.getShowRuntime() == null || showRuntime.getShowRuntime().equals("null")) {

                //get the runtime for the null from TVMaze
                HashMap<String, String> showSummaryHashMap = new HashMap<>();

                downloadShowSummary(showSummaryHashMap, showRuntime.getShowTVMazeID());

                shows.add(new Show(showRuntime.getShowName(), showRuntime.getShowRuntime(), showRuntime.getShowMyEpsID()));
                //shows.add(new Show(showRuntime.getShowName(), showRuntime.getShowRuntime(),  showSummaryHashMap.get("showRuntime")));
            } else {
                shows.add(new Show(showRuntime.getShowName(), showRuntime.getShowRuntime(), showRuntime.getShowMyEpsID()));
            }
        }
        Collections.sort(shows, new ShowRuntimeAscendingComparator());
    }


    private void downloadShowSummary(HashMap<String, String> showSummaryHash, String... params) {
        TaskRunner.getExecutor().execute(() -> {
            AppDatabase database = AppDatabase.getInstance(nz.mentalinc.watcher.activities.HomeActivity.getContext().getApplicationContext());

            SeriesDAO seriesDAO = database.getSeriesDAO();
            EpisodeRuntime showInfo = seriesDAO.getEpisodeRuntimeWithTVMazeId(params[0]);
            String showRuntime = showInfo.getShowRuntime();

            try {
                JSONObject jObj = ShowService.fetchTvMazeShowJson(params[0]);

                showRuntime = jObj.getString("runtime");

                if (showRuntime.equals("null") || showRuntime == null) {
                    showRuntime = jObj.getString("averageRuntime");
                }

                showSummaryHash.put("showRuntime", showRuntime);
                EpisodeRuntime showSummaryInfo = seriesDAO.getEpisodeRuntimeWithMyEpsId(showInfo.getShowMyEpsID());

                showSummaryInfo.setShowRuntime(showSummaryHash.get("showRuntime"));

                seriesDAO.update(showSummaryInfo);

            } catch (Exception e) {
                e.printStackTrace();
            }
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
        Objects.requireNonNull(imm).showSoftInput(runTimeInput, 0);
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

                    String newRuntime = newRuntimeValue;
                    String showName = shows.get(showListPosition).getShowName();
                    String showMyEpsID = shows.get(showListPosition).getMyEpisodeID();
                    int pos = showListPosition;

                    TaskRunner.getExecutor().execute(() -> {
                        AppDatabase database = AppDatabase.getInstance(HomeActivity.getContext().getApplicationContext());
                        SeriesDAO seriesDAO = database.getSeriesDAO();

                        EpisodeRuntime epsRunTime = new EpisodeRuntime();
                        epsRunTime.setshowMyepsID(showMyEpsID);
                        epsRunTime.setShowName(showName);
                        epsRunTime.setShowRuntime(newRuntime);
                        Log.d("epsRunTime: ", epsRunTime.toString());
                        seriesDAO.update(epsRunTime);

                        runOnUiThread(() -> {
                            Context ctx = getApplicationContext();
                            String text = "Runtime for updated " + showName + " updated to " + newRuntime + " mins";
                            Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarShowManagement), text, Snackbar.LENGTH_LONG);
                            snackbar.show();

                            populateShowRuntimeList();
                            showListPosition = null;
                            showAdapter.notifyDataSetChanged();
                        });
                    });
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


    private void showLoadingDialog() {
        if (getSupportFragmentManager().findFragmentByTag(DIALOG_LOADING_TAG) == null) {
            LoadingDialogFragment.newInstance(R.string.progressLoadingTitle).show(getSupportFragmentManager(), DIALOG_LOADING_TAG);
        }
    }

    private void dismissLoadingDialog() {
        Fragment prev = getSupportFragmentManager().findFragmentByTag(DIALOG_LOADING_TAG);
        if (prev != null) ((DialogFragment) prev).dismiss();
    }

    private void showFinishedDialog() {
        if (getSupportFragmentManager().findFragmentByTag(DIALOG_FINISHED_TAG) == null) {
            new FinishedDialogFragment().show(getSupportFragmentManager(), DIALOG_FINISHED_TAG);
        }
    }

    private static class NumericKeyBoardTransformationMethod extends PasswordTransformationMethod {
        @Override
        public CharSequence getTransformation(CharSequence source, View view) {
            return source;
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

    public static class FinishedDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.showSearchFinished)
                    .setCancelable(false)
                    .setPositiveButton(R.string.done, (dialog, id) -> {
                        dialog.dismiss();
                        getActivity().finish();
                    })
                    .setNegativeButton(R.string.search, (dialog, id) -> dialog.dismiss());
            return builder.create();
        }
    }

    private void populateShowRuntimeList() {
        showLoadingDialog();
        showAdapter.clear();
        TaskRunner.getExecutor().execute(() -> {
            getRuntimeShows();
            runOnUiThread(() -> {
                if (exceptionMessageResId != null && !exceptionMessageResId.equals("")) {
                    dismissLoadingDialog();
                    exceptionDialog(ShowManagementRunTimeActivity.this);
                } else {

                    try {

                       // showAdapter.notifyDataSetChanged();

                    } catch (Exception e) {
                        String message = "ShowFailure";
                        Log.e(LOG_TAG, message, e);
                    }

                    dismissLoadingDialog();
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
        searchIntent.putExtra(ActivityConstants.EXTRA_TITLE, getString(R.string.addShow));
        startActivity(searchIntent);
    }

    private void openRunTimeActivity() {
        Intent runTimeIntent = new Intent(this.getApplicationContext(), ShowManagementRunTimeActivity.class);
        runTimeIntent.putExtra(ActivityConstants.EXTRA_TITLE, getString(R.string.ShowRuntime));
        startActivity(runTimeIntent);
    }

    private void openFavouriteOrIgnoredShows(ShowType showType) {
        Intent intent = new Intent(this.getApplicationContext(), ShowManagementActivity.class);
        intent.putExtra(ShowType.class.getSimpleName(), showType);
        if (showType.toString().equals("FAVOURITE_SHOWS"))
            intent.putExtra(ActivityConstants.EXTRA_TITLE, getString(R.string.favouriteShows));
        else if (showType.toString().equals("IGNORED_SHOWS"))
            intent.putExtra(ActivityConstants.EXTRA_TITLE, getString(R.string.ignoredShows));
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

                dialogUpdateRuntime(ShowManagementRunTimeActivity.this);
            });

            return row;
        }
    }
}

