package nz.mentalinc.watcher.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.List;

import nz.mentalinc.watcher.R;
import nz.mentalinc.watcher.domain.Show;
import nz.mentalinc.watcher.domain.User;
import nz.mentalinc.watcher.enums.ShowType;
import nz.mentalinc.watcher.exception.InternetConnectivityException;
import nz.mentalinc.watcher.exception.LoginFailedException;
import nz.mentalinc.watcher.exception.ShowAddFailedException;
import nz.mentalinc.watcher.service.ShowService;
import nz.mentalinc.watcher.utils.TaskRunner;

//use RecyclerView instead of ListActivty
public class ShowManagementAddActivity extends ListActivity {
    private static final String LOG_TAG = ShowManagementAddActivity.class.getSimpleName();

    private static final int DIALOG_LOADING = 0;
    private static final int DIALOG_EXCEPTION = 1;
    private static final int DIALOG_FINISHED = 2;
    private static final int DIALOG_ADD_SHOW = 3;

    private ShowService service;
    private User user;
    private ShowAdapter showAdapter;
    private List<Show> shows = new ArrayList<>(0);

    private Integer exceptionMessageResId = null;
    private Integer showListPosition = null;

    private boolean showsAdded = false;

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
        super.onCreate(savedInstanceState);
        init();


        ImageButton searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(view -> {
            EditText searchQuery = findViewById(R.id.searchQuery);
            if (searchQuery != null) {
                CharSequence query = searchQuery.getText();
                if (query != null && query.length() > 0) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    ShowManagementAddActivity.this.searchShows(query.toString());
                }
            }
        });


        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {
            Intent home = new Intent(ShowManagementAddActivity.this, HomeActivity.class);
            home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(home);
        });
    }

    private void init() {

        setContentView(R.layout.show_management_add);
        findViewById(R.id.appBarLayoutAdd).setZ(100f);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigationManage);
        bottomNav.setSelectedItemId(R.id.barAddShows);
        bottomNav.setOnItemSelectedListener(navigationItemSelectedListener);

        service = new ShowService();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        user = new User(
                sharedPref.getString("username", null),
                sharedPref.getString("UserPassword", null)
        );

        initializeShowList();
    }

    private void initializeShowList() {
        showAdapter = new ShowAdapter(this, shows);
        setListAdapter(showAdapter);
    }

    private void updateShowList() {
        showAdapter.clear();
        for (Show show : shows) {
            showAdapter.add(show);
        }
        showAdapter.notifyDataSetChanged();
    }

    private void updateNumberOfResults() {
        TextView numberOfResults = findViewById(R.id.showNameSearchNumberOfResults);

        if (shows.size() > 0) {
            String text = shows.size() + " ";

            if (shows.size() == 1) {
                text += getText(R.string.showSearchOneFound);
            } else {
                text += getText(R.string.showSearchMoreFound);
            }
            numberOfResults.setText(text);
            numberOfResults.setVisibility(TextView.VISIBLE);
        } else {
            numberOfResults.setVisibility(TextView.GONE);
        }
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
                        .setPositiveButton(R.string.dialogOK, (dialog15, id15) -> {
                            exceptionMessageResId = null;
                            removeDialog(DIALOG_EXCEPTION);
                        });
                dialog = builder.create();
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
            case DIALOG_ADD_SHOW: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(shows.get(showListPosition).getShowName())
                        .setMessage(R.string.showSearchAddShow)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, (dialog12, id12) -> {
                            removeDialog(DIALOG_ADD_SHOW);
                            addShowByListPosition(showListPosition);
                            showListPosition = null;
                        })
                        .setNegativeButton(R.string.no, (dialog1, id1) -> {
                            showListPosition = null;
                            removeDialog(DIALOG_ADD_SHOW);
                        });
                dialog = builder.create();
                break;
            }
        }
        return dialog;
    }

    private void searchShows(final String query) {
        showDialog(DIALOG_LOADING);
        TaskRunner.getExecutor().execute(() -> {
            doSearch(query);
            runOnUiThread(() -> {
                if (exceptionMessageResId != null && !exceptionMessageResId.equals("")) {
                    removeDialog(DIALOG_LOADING);
                    showDialog(DIALOG_EXCEPTION);
                } else {
                    updateNumberOfResults();
                    updateShowList();
                    removeDialog(DIALOG_LOADING);
                }
            });
        });
    }

    private void doSearch(String query) {
        try {
            shows = service.searchShows(query, user);
            Log.d(LOG_TAG, shows.size() + " show(s) found!!!");
            exceptionMessageResId = null;
        } catch (InternetConnectivityException e) {
            String message = "Could not connect to host";
            Log.e(LOG_TAG, message, e);
            exceptionMessageResId = R.string.internetConnectionFailureReload;
        } catch (LoginFailedException e) {
            String message = "Login failure";
            Log.e(LOG_TAG, message, e);
            exceptionMessageResId = R.string.networkIssues;
        }
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
            topText.setText(show.getShowName());
            row.setOnClickListener(view -> {
                showListPosition = i;
                showDialog(DIALOG_ADD_SHOW);
            });

            return row;
        }
    }

    private void addShowByListPosition(final int position) {
        showDialog(DIALOG_LOADING);
        TaskRunner.getExecutor().execute(() -> {
            Show show = shows.get(position);
            addShow(show);
            runOnUiThread(() -> {
                removeDialog(DIALOG_LOADING);
                if (exceptionMessageResId != null && !exceptionMessageResId.equals("")) {
                    showDialog(DIALOG_EXCEPTION);
                } else {
                    showDialog(DIALOG_FINISHED);
                }
            });
        });
    }

    private void addShow(Show show) {
        try {
            Log.d(LOG_TAG, "Adding show with id " + show.getMyEpisodeID() + " to the account of user " + user.getUsername());
            service.addShow(show.getMyEpisodeID(), user);
            showsAdded = true;
        } catch (InternetConnectivityException e) {
            String message = "Could not connect to host";
            Log.e(LOG_TAG, message, e);
            exceptionMessageResId = R.string.internetConnectionFailureReload;
        } catch (LoginFailedException e) {
            String message = "Login failure";
            Log.e(LOG_TAG, message, e);
            exceptionMessageResId = R.string.networkIssues;
        } catch (ShowAddFailedException e) {
            String message = "Could not add show";
            Log.e(LOG_TAG, message, e);
            exceptionMessageResId = R.string.searchShowUnabletoAdd;
        }
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
}
