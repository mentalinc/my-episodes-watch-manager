package nz.mentalinc.watcher.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import nz.mentalinc.watcher.R;
import nz.mentalinc.watcher.domain.User;
import nz.mentalinc.watcher.exception.LoginFailedException;
import nz.mentalinc.watcher.service.CredentialStore;
import nz.mentalinc.watcher.service.UserService;
import nz.mentalinc.watcher.constants.MyEpisodeConstants;
import nz.mentalinc.watcher.utils.TaskRunner;


public class LoginActivity extends AppCompatActivity {
    private UserService service;
    private int exceptionMessageResId = -1;
    private boolean enableLegacyOnSuccess = false;
    private boolean forceLogin = false;

    private static final String DIALOG_LOADING_TAG = "LOADING";
    private static final String DIALOG_ERROR_TAG = "ERROR";

    private void showLoadingDialog() {
        if (getSupportFragmentManager().findFragmentByTag(DIALOG_LOADING_TAG) == null) {
            LoadingDialogFragment.newInstance(R.string.loginStartLogin).show(getSupportFragmentManager(), DIALOG_LOADING_TAG);
        }
    }

    private void dismissLoadingDialog() {
        Fragment prev = getSupportFragmentManager().findFragmentByTag(DIALOG_LOADING_TAG);
        if (prev != null) ((DialogFragment) prev).dismiss();
    }

    private void showErrorDialog(int messageResId) {
        if (getSupportFragmentManager().findFragmentByTag(DIALOG_ERROR_TAG) == null) {
            int message = messageResId > 0 ? messageResId : R.string.defaultExceptionMessage;
            ErrorDialogFragment.newInstance(message).show(getSupportFragmentManager(), DIALOG_ERROR_TAG);
        }
    }

    private static final String LOG_TAG = LoginActivity.class.getSimpleName();


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

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
 
        init();

        enableLegacyOnSuccess = getIntent().getBooleanExtra("enableLegacyOnSuccess", false);
        forceLogin = getIntent().getBooleanExtra("forceLogin", false);

        if (forceLogin || !checkLoginCredentials()) {

            setContentView(R.layout.login);

            TextView register = findViewById(R.id.registerForm);
            register.setOnClickListener(v -> openRegisterScreen());

            Button loginButton = findViewById(R.id.loginLogin);
            loginButton.setOnClickListener(v -> {
                String username = ((EditText) findViewById(R.id.loginUsername)).getText().toString().trim();
                String password = ((EditText) findViewById(R.id.loginPassword)).getText().toString().trim();
                String apiKey = ((EditText) findViewById(R.id.loginApiKey)).getText().toString().trim();

                if (!apiKey.isEmpty()) {
                    // API-key login: no username/password required.
                    showLoadingDialog();
                    TaskRunner.getExecutor().execute(() -> {
                        SharedPreferences apiPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                        apiPrefs.edit().putString(MyEpisodeConstants.PREF_API_KEY, apiKey).apply();
                        apiPrefs.edit().putBoolean("legacyParsingEnabled", false).apply();
                        MyEpisodeConstants.LEGACY_PARSING_ENABLED = false;

                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }
                            dismissLoadingDialog();
                            ((EditText) findViewById(R.id.loginApiKey)).setText("");
                            finalizeLogin();
                        });
                    });
                } else if (username.length() > 0 && password.length() > 0) {
                    final User user = new User(username, password);

                    showLoadingDialog();

                    TaskRunner.getExecutor().execute(() -> {
                        boolean loginSucceeded = login(user);
                        final int errorMessageResId = exceptionMessageResId;
                        if (loginSucceeded) {
                            try {
                                storeLoginCredentials(user);
                            } catch (RuntimeException e) {
                                // Login itself worked; failing to persist the credentials must not
                                // turn a successful login into a failure.
                                Log.e(LOG_TAG, "Could not store login credentials", e);
                            }
                            if (enableLegacyOnSuccess) {
                                PreferenceManager.getDefaultSharedPreferences(getBaseContext())
                                        .edit().putBoolean("legacyParsingEnabled", true).apply();
                                MyEpisodeConstants.LEGACY_PARSING_ENABLED = true;
                            }
                        }

                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }
                            dismissLoadingDialog();
                            if (loginSucceeded) {
                                ((EditText) findViewById(R.id.loginUsername)).setText("");
                                ((EditText) findViewById(R.id.loginPassword)).setText("");
                                finalizeLogin();
                            } else {
                                showErrorDialog(errorMessageResId);
                            }
                        });
                    });
                } else {
                    validationError(LoginActivity.this);
                }
            });
        } else {
            finalizeLogin();
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

    public static class ErrorDialogFragment extends DialogFragment {
        private static final String ARG_MESSAGE = "message";

        public static ErrorDialogFragment newInstance(int messageResId) {
            ErrorDialogFragment frag = new ErrorDialogFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_MESSAGE, messageResId);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int messageResId = getArguments() != null
                    ? getArguments().getInt(ARG_MESSAGE, R.string.defaultExceptionMessage)
                    : R.string.defaultExceptionMessage;
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.exceptionDialogTitle)
                    .setMessage(messageResId)
                    .setCancelable(false)
                    .setNeutralButton(R.string.dialogOK, (dialog, id) -> dismiss())
                    .create();
        }
    }

    public void validationError(Context context) {

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
        dialog.setTitle(R.string.exceptionDialogTitle);
        dialog.setMessage(R.string.fillInAllFields);
        dialog.setNeutralButton(R.string.dialogOK, (dialog1, which) -> dialog1.dismiss());

        dialog.setCancelable(false);
        dialog.create();
        dialog.show();
    }


    public void loginDialog(Context context) {

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
        dialog.setTitle(R.string.exceptionDialogTitle);
        dialog.setMessage(R.string.fillInAllFields);
        dialog.setNeutralButton(R.string.dialogOK, (dialog1, which) -> dialog1.dismiss());

        dialog.setCancelable(false);
        dialog.create();
        dialog.show();
    }


    /**
     * Attempts to log in and returns whether it succeeded. Callers must only persist credentials
     * and leave the screen when this returns {@code true}.
     */
    private boolean login(User user) {
        exceptionMessageResId = -1;
        try {
            service.login(user);
            return true;
        } catch (LoginFailedException e) {
            Log.e(LOG_TAG, "Login failed", e);
            exceptionMessageResId = R.string.loginLoginFailed;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unexpected error during login", e);
            exceptionMessageResId = R.string.defaultExceptionMessage;
        }
        return false;
    }

    private boolean checkLoginCredentials() {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String apiKey = sharedPref.getString(MyEpisodeConstants.PREF_API_KEY, "");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            MyEpisodeConstants.LEGACY_PARSING_ENABLED = sharedPref.getBoolean("legacyParsingEnabled", false);
            return true;
        }
        String username = sharedPref.getString(MyEpisodeConstants.PREF_USERNAME, null);
        String password = CredentialStore.getPassword(getBaseContext());
        return username != null && password != null;
    }

    private void storeLoginCredentials(User user) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putString(MyEpisodeConstants.PREF_USERNAME, user.getUsername());
        prefEditor.apply();

        boolean storePassword = sharedPref.getBoolean("StorePassword", true);
        if (storePassword) {
            CredentialStore.savePassword(getBaseContext(), user.getPassword());
        }
    }


    private void finalizeLogin() {
        setResult(RESULT_OK);
        finish();
    }

    private void init() {
        this.service = new UserService();
    }

    private void openRegisterScreen() {
        Intent registerActivity = new Intent(this.getApplicationContext(), RegisterActivity.class);
        startActivity(registerActivity);
    }


    public void handleHomeMenu(android.view.MenuItem item) {
        android.content.Intent home = new android.content.Intent(this, HomeActivity.class);
        home.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(home);
    }
}
