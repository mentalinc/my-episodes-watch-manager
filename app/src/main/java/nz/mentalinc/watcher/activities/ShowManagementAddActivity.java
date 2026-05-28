package nz.mentalinc.watcher.activities;

import android.app.AlertDialog;
import android.app.Dialog;
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
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import nz.mentalinc.watcher.service.CredentialStore;
import nz.mentalinc.watcher.service.ShowService;
import nz.mentalinc.watcher.constants.ActivityConstants;
import nz.mentalinc.watcher.constants.MyEpisodeConstants;
import nz.mentalinc.watcher.utils.TaskRunner;

public class ShowManagementAddActivity extends AppCompatActivity {
    private static final String LOG_TAG = ShowManagementAddActivity.class.getSimpleName();

    private ShowService service;
    private User user;
    private ShowAdapter showAdapter;
    private List<Show> shows = new ArrayList<>(0);

    private Integer exceptionMessageResId = null;
    private Integer showListPosition = null;
    private ListView listView;

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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
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
 
        setContentView(R.layout.show_management_add);
        findViewById(R.id.appBarLayoutAdd).setZ(100f);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigationManage);
        bottomNav.setSelectedItemId(R.id.barAddShows);
        bottomNav.setOnItemSelectedListener(navigationItemSelectedListener);

        service = new ShowService();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        user = new User(
                sharedPref.getString(MyEpisodeConstants.PREF_USERNAME, null),
                CredentialStore.getPassword(ShowManagementAddActivity.this)
        );

        listView = findViewById(android.R.id.list);
        initializeShowList();
    }

    private void initializeShowList() {
        showAdapter = new ShowAdapter(this, shows);
        listView.setAdapter(showAdapter);
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
                text += getString(R.string.showSearchOneFound);
            } else {
                text += getString(R.string.showSearchMoreFound);
            }
            numberOfResults.setText(text);
            numberOfResults.setVisibility(TextView.VISIBLE);
        } else {
            numberOfResults.setVisibility(TextView.GONE);
        }
    }

    private static final String DIALOG_LOADING_TAG = "LOADING";
    private static final String DIALOG_EXCEPTION_TAG = "EXCEPTION";
    private static final String DIALOG_FINISHED_TAG = "FINISHED";
    private static final String DIALOG_ADD_SHOW_TAG = "ADD_SHOW";

    private void showLoadingDialog() {
        if (getSupportFragmentManager().findFragmentByTag(DIALOG_LOADING_TAG) == null) {
            LoadingDialogFragment.newInstance(R.string.progressLoadingTitle).show(getSupportFragmentManager(), DIALOG_LOADING_TAG);
        }
    }

    private void dismissLoadingDialog() {
        Fragment prev = getSupportFragmentManager().findFragmentByTag(DIALOG_LOADING_TAG);
        if (prev != null) ((DialogFragment) prev).dismiss();
    }

    private void showExceptionDialog(int messageResId) {
        if (getSupportFragmentManager().findFragmentByTag(DIALOG_EXCEPTION_TAG) == null) {
            ExceptionDialogFragment.newInstance(messageResId).show(getSupportFragmentManager(), DIALOG_EXCEPTION_TAG);
        }
    }

    private void showFinishedDialog() {
        if (getSupportFragmentManager().findFragmentByTag(DIALOG_FINISHED_TAG) == null) {
            new FinishedDialogFragment().show(getSupportFragmentManager(), DIALOG_FINISHED_TAG);
        }
    }

    private void showAddShowDialog(String showName, int position) {
        if (getSupportFragmentManager().findFragmentByTag(DIALOG_ADD_SHOW_TAG) == null) {
            AddShowDialogFragment.newInstance(showName, position).show(getSupportFragmentManager(), DIALOG_ADD_SHOW_TAG);
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

    public static class ExceptionDialogFragment extends DialogFragment {
        private static final String ARG_MESSAGE_RES = "messageRes";

        public static ExceptionDialogFragment newInstance(int messageResId) {
            ExceptionDialogFragment frag = new ExceptionDialogFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_MESSAGE_RES, messageResId);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int messageResId = getArguments().getInt(ARG_MESSAGE_RES);
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.exceptionDialogTitle)
                    .setMessage(messageResId)
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialogOK, (dialog, id) -> dismiss())
                    .create();
        }
    }

    public static class FinishedDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.showSearchFinished)
                    .setCancelable(false)
                    .setPositiveButton(R.string.done, (dialog, id) -> {
                        dialog.dismiss();
                        getActivity().finish();
                    })
                    .setNegativeButton(R.string.search, (dialog, id) -> dialog.dismiss())
                    .create();
        }
    }

    public static class AddShowDialogFragment extends DialogFragment {
        private static final String ARG_SHOW_NAME = "showName";
        private int showListPosition;

        public static AddShowDialogFragment newInstance(String showName, int position) {
            AddShowDialogFragment frag = new AddShowDialogFragment();
            Bundle args = new Bundle();
            args.putString(ARG_SHOW_NAME, showName);
            frag.showListPosition = position;
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String showName = getArguments().getString(ARG_SHOW_NAME);
            return new AlertDialog.Builder(getActivity())
                    .setTitle(showName)
                    .setMessage(R.string.showSearchAddShow)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes, (dialog, id) -> {
                        if (getActivity() instanceof ShowManagementAddActivity) {
                            ((ShowManagementAddActivity) getActivity()).addShowByListPosition(showListPosition);
                        }
                    })
                    .setNegativeButton(R.string.no, (dialog, id) -> {
                        dismiss();
                    })
                    .create();
        }
    }

    private void searchShows(final String query) {
        showLoadingDialog();
        TaskRunner.getExecutor().execute(() -> {
            doSearch(query);
            runOnUiThread(() -> {
                if (exceptionMessageResId != null && !exceptionMessageResId.equals("")) {
                    dismissLoadingDialog();
                    showExceptionDialog(exceptionMessageResId);
                    exceptionMessageResId = null;
                } else {
                    updateNumberOfResults();
                    updateShowList();
                    dismissLoadingDialog();
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
            String message = MyEpisodeConstants.CONNECT_ERROR;
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
                showAddShowDialog(shows.get(showListPosition).getShowName(), showListPosition);
            });

            return row;
        }
    }

    private void addShowByListPosition(final int position) {
        showLoadingDialog();
        TaskRunner.getExecutor().execute(() -> {
            Show show = shows.get(position);
            addShow(show);
            runOnUiThread(() -> {
                dismissLoadingDialog();
                if (exceptionMessageResId != null && !exceptionMessageResId.equals("")) {
                    showExceptionDialog(exceptionMessageResId);
                    exceptionMessageResId = null;
                } else {
                    showFinishedDialog();
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
            String message = MyEpisodeConstants.CONNECT_ERROR;
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
}
