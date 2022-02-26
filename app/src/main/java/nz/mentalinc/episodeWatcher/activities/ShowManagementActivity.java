package nz.mentalinc.episodeWatcher.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.domain.Show;
import nz.mentalinc.episodeWatcher.domain.User;
import nz.mentalinc.episodeWatcher.enums.ShowAction;
import nz.mentalinc.episodeWatcher.enums.ShowType;
import nz.mentalinc.episodeWatcher.exception.InternetConnectivityException;
import nz.mentalinc.episodeWatcher.exception.LoginFailedException;
import nz.mentalinc.episodeWatcher.service.ShowService;

//use RecyclerView instead of ListActivty
public class ShowManagementActivity extends ListActivity {
    private static final String LOG_TAG = ShowManagementActivity.class.getSimpleName();
    private ShowType showType;
    private User user;
    private ShowService service;
    private ShowAdapter showAdapter;
    private List<Show> shows = new ArrayList<>(0);

    private int selectedShow = -1;
    private ShowAction showAction = null;
    private int confirmationMessageResId = -1;
    private Integer exceptionMessageResId = null;

    private static final int DIALOG_LOADING = 0;

    private static final int CONTEXT_MENU_DELETE = 0;
    private static final int CONTEXT_MENU_UNIGNORE = 1;
    private static final int CONTEXT_MENU_IGNORE = 2;
    private static final int CONFIRMATION_DIALOG = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
        init(savedInstanceState);

        reloadShows();

        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> finish());
    }

    private void init(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.show_management);

        Bundle data = this.getIntent().getExtras();
        showType = (ShowType) Objects.requireNonNull(data).get(ShowType.class.getSimpleName());
        String title = (String) data.getSerializable("Title");
        Toolbar toolbar = findViewById(R.id.topAppBarShowManagement);
        toolbar.setTitle(title);


        if (Objects.requireNonNull(showType).equals(ShowType.FAVOURITE_SHOWS)) {
            Log.d(LOG_TAG, "Opening the favourite shows");
            this.setTitle(R.string.favouriteShows);


        } else if (showType.equals(ShowType.IGNORED_SHOWS)) {
            Log.d(LOG_TAG, "Opening the ignored shows");

            this.setTitle(R.string.ignoredShows);
        }
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        user = new User(                
                    sharedPref.getString("username",null),
                    sharedPref.getString("UserPassword",null)  
        );

        initializeShowList();

        service = new ShowService();
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
        }
        return dialog;
    }

    private void reloadShows() {
        AsyncTask<Object, Object, Object> asyncTask = new AsyncTask<Object, Object, Object>() {
            @Override
            protected void onPreExecute() {
                showDialog(DIALOG_LOADING);
            }

            @Override
            protected Object doInBackground(Object... objects) {
                getShows(user, showType);
                return 100L;
            }

            @Override
            protected void onPostExecute(Object o) {
                if (exceptionMessageResId != null && !exceptionMessageResId.equals("")) {
                    removeDialog(DIALOG_LOADING);
                    //showDialog(DIALOG_EXCEPTION);
                    exceptionDialog(ShowManagementActivity.this);
                } else {
                    updateShowList();
                    removeDialog(DIALOG_LOADING);
                }
            }
        };
        asyncTask.execute();
    }

    private void getShows(User user, ShowType showType) {
        try {
            shows = service.getFavoriteOrIgnoredShows(user, showType);
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

    private void updateShowList() {
        showAdapter.clear();
        for (Show show : shows) {
            showAdapter.add(show);
        }
        showAdapter.notifyDataSetChanged();
    }

    private void initializeShowList() {
        showAdapter = new ShowAdapter(this, R.layout.show_management_add_row, shows);
        setListAdapter(showAdapter);
        registerForContextMenu(getListView());
    }

    private class ShowAdapter extends ArrayAdapter<Show> {
        private final List<Show> shows;

        ShowAdapter(Context context, int textViewResourceId, List<Show> el) {
            super(context, textViewResourceId, el);
            this.shows = el;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.show_management_add_row, parent, false);
            }

            TextView topText = row.findViewById(R.id.showNameSearchResult);

            Show show = shows.get(position);
            topText.setText(show.getShowName());

            row.setOnClickListener(ShowManagementActivity.this::openContextMenu);

            return row;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        switch (showType) {
            case FAVOURITE_SHOWS:
                menu.add(Menu.NONE, CONTEXT_MENU_IGNORE, Menu.NONE, R.string.favoIgnoredIgnoreShow);
                break;
            case IGNORED_SHOWS:
                menu.add(Menu.NONE, CONTEXT_MENU_UNIGNORE, Menu.NONE, R.string.favoIgnoredUnignoreShow);
                break;
        }

        menu.add(Menu.NONE, CONTEXT_MENU_DELETE, Menu.NONE, R.string.favoIgnoredDeleteShow);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        selectedShow = info.position;
        switch (item.getItemId()) {
            case CONTEXT_MENU_IGNORE:
                showDialog(info.position, ShowAction.IGNORE, R.string.favoIgnoredConfirmationIgnoreMessage);
                break;
            case CONTEXT_MENU_UNIGNORE:
                showDialog(info.position, ShowAction.UNIGNORE, R.string.favoIgnoredConfirmationUnignoreMessage);
                break;
            case CONTEXT_MENU_DELETE:
                showDialog(info.position, ShowAction.DELETE, R.string.favoIgnoredConfirmationDeleteMessage);
                break;
            default:
                return false;
        }
        return true;
    }

    private void showDialog(int listPosition, ShowAction action, int messageId) {
        this.selectedShow = listPosition;
        this.showAction = action;
        this.confirmationMessageResId = messageId;

        if (selectedShow > -1) {
            final Show show = shows.get(selectedShow);

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(ShowManagementActivity.this);
        dialog.setTitle(show.getShowName());
        dialog.setMessage(confirmationMessageResId);
        dialog.setNegativeButton(R.string.no, (dialog1, which) -> {
            selectedShow = -1;
            confirmationMessageResId = -1;
            showAction = null;
            dialog1.dismiss();
        });
        dialog.setPositiveButton(R.string.yes, (dialog12, which) -> {
            markShow(show, showAction);

            selectedShow = -1;
            confirmationMessageResId = -1;
            showAction = null;
            dialog12.dismiss();
        });

        dialog.setCancelable(false);
        dialog.create();
        dialog.show();

        }
    }

    private void markShow(final Show show, final ShowAction action) {
        AsyncTask<Object, Object, Object> asyncTask = new AsyncTask<Object, Object, Object>() {
            @Override
            protected void onPreExecute() {
                showDialog(DIALOG_LOADING);
            }

            @Override
            protected Object doInBackground(Object... objects) {
                switch (action) {
                    case IGNORE:
                    case UNIGNORE:
                    case DELETE:
                        break;
                }

                markShow(user, showType, action, show);
                return 100L;
            }

            @Override
            protected void onPostExecute(Object o) {
                if (exceptionMessageResId != null && !exceptionMessageResId.equals("")) {
                    removeDialog(DIALOG_LOADING);
                    //showDialog(DIALOG_EXCEPTION);
                    exceptionDialog(ShowManagementActivity.this);
                } else {
                    updateShowList();
                    removeDialog(DIALOG_LOADING);
                }
            }
        };
        asyncTask.execute();
    }

    private void markShow(User user, ShowType showType, ShowAction showAction, Show show) {
        try {
            shows = service.markShow(user, show, showAction, showType);
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

    public void onHomeClick(View v) {
        finish();
    }
}
