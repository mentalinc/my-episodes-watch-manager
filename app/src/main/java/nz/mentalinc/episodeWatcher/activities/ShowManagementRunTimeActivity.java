package nz.mentalinc.episodeWatcher.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;

import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.room.Room;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.database.AppDatabase;
import nz.mentalinc.episodeWatcher.database.SeriesDAO;
import nz.mentalinc.episodeWatcher.domain.Show;
import nz.mentalinc.episodeWatcher.domain.ShowRuntimeAscendingComparator;
import nz.mentalinc.episodeWatcher.domain.User;
import nz.mentalinc.episodeWatcher.service.EpisodeRuntime;
import nz.mentalinc.episodeWatcher.utils.InputFilterMinMax;


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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(savedInstanceState);

        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {
            finish();
        });

    }

    private void init(Bundle savedInstanceState) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_management);

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

    }

    private void updateShowList() {
        for (Show show : shows) {
            showAdapter.add(show);
        }
        showAdapter.notifyDataSetChanged();
    }


    private void getRuntimeShows() {

        AppDatabase database = Room.databaseBuilder(nz.mentalinc.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                .allowMainThreadQueries()   //Allows room to do operation on main thread
                .fallbackToDestructiveMigration()
                .build();

        SeriesDAO seriesDAO = database.getSeriesDAO();
        List<EpisodeRuntime> runtimeList = seriesDAO.getEpisodeRuntime();

        for (int i = 0; i < runtimeList.size(); i++) {
            EpisodeRuntime showRuntime = runtimeList.get(i);
            shows.add(new Show(showRuntime.getShowName(), showRuntime.getShowRuntime(), showRuntime.getShowMyEpsID()));
        }
        //todo: add this in when bump the version up again
        //shows.sort(new ShowRuntimeAscendingComparator());

        Collections.sort(shows,new ShowRuntimeAscendingComparator());
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
        dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                        {
                            String newRuntimeValue = runTimeInput.getText().toString();
                            //silent fail if user has entered a blank runtime
                            if (runTimeInput.getText().toString().trim().length() < 1) {
                                // runTimeInput.setError("Error: Can't be blank");
                                String text = "Error: Runtime can't be blank!";
                               // int duration = Toast.LENGTH_SHORT;
                                //Toast toast = Toast.makeText(ShowManagementRunTimeActivity.this, text, duration);
                                //toast.show();

                                Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarShowManagement),text,Snackbar.LENGTH_LONG);
                                snackbar.show();


                            } else if (runTimeInput.getText().toString().trim().equals(shows.get(showListPosition).getRunTime())) {
                                Context context = getApplicationContext();
                                String text = "Runtime unchanged";
                               /* int duration = Toast.LENGTH_SHORT;
                                Toast toast = Toast.makeText(context, text, duration);
                                toast.show();*/

                                Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarShowManagement),text,Snackbar.LENGTH_LONG);
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
                               /* int duration = Toast.LENGTH_SHORT;
                                Toast toast = Toast.makeText(context, text, duration);
                                toast.show();*/

                                Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarShowManagement),text,Snackbar.LENGTH_LONG);
                                snackbar.show();

                                populateShowRuntimeList();
                                showListPosition = null;
                            }
                            dialog.dismiss();
                        }
                });
        dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
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
                              /*  int duration = Toast.LENGTH_SHORT;
                                Toast toast = Toast.makeText(context, text, duration);
                                toast.show();*/

                                Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarShowManagement),text,Snackbar.LENGTH_LONG);
                                snackbar.show();

                            } else if (runTimeInput.getText().toString().trim().equals(shows.get(showListPosition).getRunTime())) {
                                Context context = getApplicationContext();
                                String text = "Runtime unchanged";
                                /*int duration = Toast.LENGTH_SHORT;
                                Toast toast = Toast.makeText(context, text, duration);
                                toast.show();*/

                                Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarShowManagement),text,Snackbar.LENGTH_LONG);
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
                                /*int duration = Toast.LENGTH_SHORT;
                                Toast toast = Toast.makeText(context, text, duration);
                                toast.show();*/

                                Snackbar snackbar = Snackbar.make(findViewById(R.id.topAppBarShowManagement),text,Snackbar.LENGTH_LONG);
                                snackbar.show();

                                populateShowRuntimeList();
                                showListPosition = null;
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
        AsyncTask<Object, Object, Object> asyncTask = new AsyncTask<Object, Object, Object>() {
            @Override
            protected void onPreExecute() {
                showDialog(DIALOG_LOADING);
                showAdapter.clear();
            }

            @Override
            protected Object doInBackground(Object... objects) {
                getRuntimeShows();
                return 100L;
            }

            @Override
            protected void onPostExecute(Object o) {
                if (exceptionMessageResId != null && !exceptionMessageResId.equals("")) {
                    removeDialog(DIALOG_LOADING);
                    //showDialog(DIALOG_EXCEPTION);
                    exceptionDialog(ShowManagementRunTimeActivity.this);
                } else {

                    try {
                        updateShowList();
                    } catch (Exception e) {
                        String message = "ShowFailure";
                        Log.e(LOG_TAG, message, e);
                    }

                    removeDialog(DIALOG_LOADING);
                }
            }
        };
        asyncTask.execute();
    }

    @Override
    public void finish() {
        boolean showsAdded = false;
        if (showsAdded) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        super.finish();
    }

    public void onHomeClick(View v) {
        finish();
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

