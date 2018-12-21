package eu.vranckaert.episodeWatcher.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.DialogInterface;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.vranckaert.episodeWatcher.R;
import eu.vranckaert.episodeWatcher.database.AppDatabase;
import eu.vranckaert.episodeWatcher.database.SeriesDAO;
import eu.vranckaert.episodeWatcher.domain.Show;
import eu.vranckaert.episodeWatcher.domain.ShowRuntimeAscendingComparator;
import eu.vranckaert.episodeWatcher.domain.User;
import eu.vranckaert.episodeWatcher.preferences.Preferences;
import eu.vranckaert.episodeWatcher.service.EpisodeRuntime;
import eu.vranckaert.episodeWatcher.service.ShowService;
import eu.vranckaert.episodeWatcher.utils.InputFilterMinMax;
import roboguice.activity.GuiceListActivity;

public class ShowManagementRunTimeActivity extends GuiceListActivity {
    private static final String LOG_TAG = ShowManagementRunTimeActivity.class.getSimpleName();

    private static final int DIALOG_LOADING = 0;
    private static final int DIALOG_EXCEPTION = 1;
    private static final int DIALOG_FINISHED = 2;
    private static final int DIALOG_UPDATE_RUNTIME = 3;

    private ShowService service;
    private User user;
    private ShowManagementRunTimeActivity.ShowAdapter showAdapter;
    private List<Show> shows = new ArrayList<>(0);

    private Integer exceptionMessageResId = null;
    private Integer showListPosition = null;

    private boolean showsAdded = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(savedInstanceState);

    }

    private void init(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_management);

        ((TextView) findViewById(R.id.title_text)).setText(R.string.ShowRuntime);

        service = new ShowService();
        user = new User(
                Preferences.getPreference(this, User.USERNAME),
                Preferences.getPreference(this, User.PASSWORD)
        );

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

        AppDatabase database = Room.databaseBuilder(eu.vranckaert.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                .allowMainThreadQueries()   //Allows room to do operation on main thread
                .build();

        SeriesDAO seriesDAO = database.getSeriesDAO();
        List runtimeList = seriesDAO.getEpisodeRuntime();

        for (int i = 0; i < runtimeList.size(); i++) {
            EpisodeRuntime showRuntime = (EpisodeRuntime) runtimeList.get(i);
            shows.add(new Show(showRuntime.showName, showRuntime.showRuntime, showRuntime.getShowMyEpsID()));
        }
        Collections.sort(shows, new ShowRuntimeAscendingComparator());
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
            case DIALOG_EXCEPTION: {
                if (exceptionMessageResId == null) {
                    exceptionMessageResId = R.string.defaultExceptionMessage;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.exceptionDialogTitle)
                        .setMessage(exceptionMessageResId)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialogOK, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                exceptionMessageResId = null;
                                removeDialog(DIALOG_EXCEPTION);
                            }
                        });
                dialog = builder.create();
                break;
            }
            case DIALOG_FINISHED: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.showSearchFinished)
                        .setCancelable(false)
                        .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.search, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
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
                imm.showSoftInput(runTimeInput, InputMethodManager.SHOW_IMPLICIT);
                runTimeInput.requestFocus();
                builder.setView(runTimeInput);


                builder.setTitle(shows.get(showListPosition).getShowName())
                        //builder.setTitle(shows.get(showListPosition).toString()) //this works ad does what is required showing the runtime and name in the title
                        .setMessage((R.string.runTimeEditMessage))
                        .setCancelable(true)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                removeDialog(DIALOG_UPDATE_RUNTIME);
                                String newRuntimeValue = runTimeInput.getText().toString();
                                //silent fail if user has entered a blank runtime
                                if (runTimeInput.getText().toString().trim().length() < 1) {
                                    // runTimeInput.setError("Error: Can't be blank");
                                    Context context = getApplicationContext();
                                    String text = "Error: Runtime can't be blank!";
                                    int duration = Toast.LENGTH_SHORT;
                                    Toast toast = Toast.makeText(context, text, duration);
                                    toast.show();
                                } else if (runTimeInput.getText().toString().trim().equals(shows.get(showListPosition).getRunTime())) {
                                    Context context = getApplicationContext();
                                    String text = "Runtime unchanged";
                                    int duration = Toast.LENGTH_SHORT;
                                    Toast toast = Toast.makeText(context, text, duration);
                                    toast.show();
                                } else {
                                    runTimeInput.setError(null);


                                    AppDatabase database = Room.databaseBuilder(eu.vranckaert.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext(), AppDatabase.class, "EpisodeRuntime")
                                            .allowMainThreadQueries()   //Allows room to do operation on main thread
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
                                    int duration = Toast.LENGTH_SHORT;
                                    Toast toast = Toast.makeText(context, text, duration);
                                    toast.show();

                                    populateShowRuntimeList();
                                    showListPosition = null;
                                }
                            }
                        })
                        //; //remove this ; if add the .negative back int
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                showListPosition = null;
                                removeDialog(DIALOG_UPDATE_RUNTIME);
                            }
                        });
                dialog = builder.create();
                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialogInterface) {
                        runTimeInput.post(new Runnable() {
                            @Override
                            public void run() {
                                final InputMethodManager imm = (InputMethodManager) runTimeInput.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.showSoftInput(runTimeInput, InputMethodManager.SHOW_IMPLICIT);
                                runTimeInput.requestFocus(); // needed if you have more then one input
                            }
                        });
                    }
                });
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
                    showDialog(DIALOG_EXCEPTION);
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
            topText.setText(show.getRunTime() + " mins - " + show.getShowName());
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showListPosition = i;
                    showDialog(DIALOG_UPDATE_RUNTIME);
                }
            });

            return row;
        }
    }

}

