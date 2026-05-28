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
import nz.mentalinc.watcher.service.UserService;
import nz.mentalinc.watcher.utils.TaskRunner;


public class LoginActivity extends AppCompatActivity {
    private UserService service;
    private int exceptionMessageResId = -1;

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

    private void showErrorDialog() {
        if (getSupportFragmentManager().findFragmentByTag(DIALOG_ERROR_TAG) == null) {
            new ErrorDialogFragment().show(getSupportFragmentManager(), DIALOG_ERROR_TAG);
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

        if (!checkLoginCredentials()) {

            setContentView(R.layout.login);

            TextView register = findViewById(R.id.registerForm);
            register.setOnClickListener(v -> openRegisterScreen());

            Button loginButton = findViewById(R.id.loginLogin);
            loginButton.setOnClickListener(v -> {
                String username = ((EditText) findViewById(R.id.loginUsername)).getText().toString().trim();
                String password = ((EditText) findViewById(R.id.loginPassword)).getText().toString().trim();

                if (username.length() > 0 && password.length() > 0) {
                    final User user = new User(username, password);

                    //TODO login failed exception doesn't get shown to the user or stop and login takes user to homeactivity but is blank, so need to logout to try again.
                    showLoadingDialog();
                    //loginDialog(LoginActivity.this);

                    TaskRunner.getExecutor().execute(() -> {
                        login(user);
                        //todo add a test here to check for a type of cookie to show login has worked ok....
                        //remove true and add in the cookie test
                        //    if (msCookieManager.getCookieStore().getCookies().size() > 0) {
                        storeLoginCredentials(user);
                        //    }

                        runOnUiThread(() -> {
                            dismissLoadingDialog();
                            //todo add a test here to check for a type of cookie to show login has worked ok....
                            //remove true and add in the cookie test
                            // if (msCookieManager.getCookieStore().getCookies().size() > 0) {
                            //  Toast.makeText(LoginActivity.this, R.string.loginSuccessfullLogin, Toast.LENGTH_LONG).show();
                            finalizeLogin();
                            //   } else {
                            ((EditText) findViewById(R.id.loginUsername)).setText("");
                            ((EditText) findViewById(R.id.loginPassword)).setText("");
                            showErrorDialog();

                            //   }
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
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setCancelable(false)
                    .setNeutralButton(R.string.dialogOK, (dialog, id) -> dismiss())
                    .create();
        }
    }

    public void validationError(Context context) {

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
        dialog.setTitle(R.string.exceptionDialogTitle);
        dialog.setMessage(R.string.fillInAllFields);
        dialog.setNeutralButton(R.string.dialogOK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialog.setCancelable(false);
        dialog.create();
        dialog.show();
    }


    public void loginDialog(Context context) {

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
        dialog.setTitle(R.string.exceptionDialogTitle);
        dialog.setMessage(R.string.fillInAllFields);
        dialog.setNeutralButton(R.string.dialogOK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialog.setCancelable(false);
        dialog.create();
        dialog.show();
    }


    private void login(User user) {
        //java.net.CookieManager cookieManager = new java.net.CookieManager();
        try {
            service.login(user);
        } catch (LoginFailedException e) {
            String message = "Login failed";
            Log.e(LOG_TAG, message, e);
            exceptionMessageResId = R.string.loginLoginFailed;
        } catch (Exception e) {
            String message = "Some Exception occured";
            Log.e(LOG_TAG, message, e);
            exceptionMessageResId = R.string.defaultExceptionMessage;
        }
        // return cookieManager;
    }

    private boolean checkLoginCredentials() {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String username = sharedPref.getString("username", null);
        String password = sharedPref.getString("UserPassword", null);
        return username != null && password != null;
    }

    private void storeLoginCredentials(User user) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putString("username", user.getUsername());
        prefEditor.putString("UserPassword", user.getPassword());
        prefEditor.apply();

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

}