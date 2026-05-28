package nz.mentalinc.watcher.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import nz.mentalinc.watcher.R;
import nz.mentalinc.watcher.domain.Show;
import nz.mentalinc.watcher.domain.User;
import nz.mentalinc.watcher.enums.ShowAction;
import nz.mentalinc.watcher.enums.ShowType;
import nz.mentalinc.watcher.exception.InternetConnectivityException;
import nz.mentalinc.watcher.exception.LoginFailedException;
import nz.mentalinc.watcher.service.CredentialStore;
import nz.mentalinc.watcher.service.ShowService;
import nz.mentalinc.watcher.constants.ActivityConstants;
import nz.mentalinc.watcher.constants.MyEpisodeConstants;
import nz.mentalinc.watcher.utils.TaskRunner;

public class ShowManagementActivity extends AppCompatActivity {
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
    private ListView listView;

    private static final String DIALOG_LOADING_TAG = "LOADING";

    private static final int CONTEXT_MENU_DELETE = 0;
    private static final int CONTEXT_MENU_UNIGNORE = 1;
    private static final int CONTEXT_MENU_IGNORE = 2;
    private static final int CONFIRMATION_DIALOG = 3;

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

        reloadShows();

        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {
            Intent home = new Intent(ShowManagementActivity.this, HomeActivity.class);
            home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(home);
        });
    }

    private void init() {

        super.setContentView(R.layout.show_management);
        findViewById(R.id.appBarLayoutShowManagement).setZ(100f);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigationManage);

        Bundle data = this.getIntent().getExtras();
        showType = Objects.requireNonNull(data).getSerializable(ShowType.class.getSimpleName(), ShowType.class);
        if (showType.equals(ShowType.FAVOURITE_SHOWS)) {
            bottomNav.setSelectedItemId(R.id.barFavouriteShows);
        } else if (showType.equals(ShowType.IGNORED_SHOWS)) {
            bottomNav.setSelectedItemId(R.id.barIgnoredShows);
        }
        bottomNav.setOnItemSelectedListener(navigationItemSelectedListener);
        String title = data.getSerializable(ActivityConstants.EXTRA_TITLE, String.class);
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
                sharedPref.getString(MyEpisodeConstants.PREF_USERNAME, null),
                CredentialStore.getPassword(ShowManagementActivity.this)
        );

        listView = findViewById(android.R.id.list);
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

    private void showLoadingDialog() {
        if (getSupportFragmentManager().findFragmentByTag(DIALOG_LOADING_TAG) == null) {
            LoadingDialogFragment.newInstance(R.string.progressLoadingTitle).show(getSupportFragmentManager(), DIALOG_LOADING_TAG);
        }
    }

    private void dismissLoadingDialog() {
        Fragment prev = getSupportFragmentManager().findFragmentByTag(DIALOG_LOADING_TAG);
        if (prev != null) ((DialogFragment) prev).dismiss();
    }

    private void reloadShows() {
        showLoadingDialog();
        TaskRunner.getExecutor().execute(() -> {
            getShows(user, showType);
            runOnUiThread(() -> {
                if (exceptionMessageResId != null && !exceptionMessageResId.equals("")) {
                    dismissLoadingDialog();
                    exceptionDialog(ShowManagementActivity.this);
                } else {
                    updateShowList();
                    dismissLoadingDialog();
                }
            });
        });
    }

    private void getShows(User user, ShowType showType) {
        try {
            shows = service.getFavoriteOrIgnoredShows(user, showType);
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

    private void updateShowList() {
        showAdapter.clear();
        for (Show show : shows) {
            showAdapter.add(show);
        }
        showAdapter.notifyDataSetChanged();
    }

    private void initializeShowList() {
        showAdapter = new ShowAdapter(this, R.layout.show_management_add_row, shows);
        listView.setAdapter(showAdapter);
        registerForContextMenu(listView);
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
            default: //added for code quality
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
        showLoadingDialog();
        TaskRunner.getExecutor().execute(() -> {
            switch (action) {
                case IGNORE:
                case UNIGNORE:
                case DELETE:
                    break;
                default: //added for code quality
            }

            markShow(user, showType, action, show);
            runOnUiThread(() -> {
                if (exceptionMessageResId != null && !exceptionMessageResId.equals("")) {
                    dismissLoadingDialog();
                    exceptionDialog(ShowManagementActivity.this);
                } else {
                    updateShowList();
                    dismissLoadingDialog();
                }
            });
        });
    }

    private void markShow(User user, ShowType showType, ShowAction showAction, Show show) {
        try {
            shows = service.markShow(user, show, showAction, showType);
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
