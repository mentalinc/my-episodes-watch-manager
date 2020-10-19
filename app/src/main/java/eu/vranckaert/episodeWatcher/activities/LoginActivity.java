package eu.vranckaert.episodeWatcher.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import eu.vranckaert.episodeWatcher.R;
import eu.vranckaert.episodeWatcher.domain.User;
import eu.vranckaert.episodeWatcher.exception.LoginFailedException;
import eu.vranckaert.episodeWatcher.preferences.Preferences;
import eu.vranckaert.episodeWatcher.preferences.PreferencesKeys;
import eu.vranckaert.episodeWatcher.service.UserService;
import roboguice.activity.GuiceActivity;

public class LoginActivity extends GuiceActivity {
    //private Button loginButton;
    // private TextView register;
    private UserService service;
    private int exceptionMessageResId = -1;

    private static final int MY_EPISODES_LOGIN_DIALOG_LOADING = 0;
    private static final int MY_EPISODES_ERROR_DIALOG = 1;
    private static final int MY_EPISODES_VALIDATION_REQUIRED_ALL_FIELDS = 2;

    private static final String LOG_TAG = LoginActivity.class.getSimpleName();
    //CustomAnalyticsTracker tracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Preferences.getPreferenceInt(this, PreferencesKeys.THEME_KEY) == 0 ? android.R.style.Theme_Material_Light : android.R.style.Theme_Material);
        super.onCreate(savedInstanceState);
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
                    final User user = new User(
                            username, password
                    );

                    AsyncTask<Object, Object, Object> asyncTask = new AsyncTask<Object, Object, Object>() {
                        boolean loginStatus = false;


                        //probably don't need cookie manager - need to use the accept all cookies policy thing
                      //  java.net.CookieManager msCookieManager = new java.net.CookieManager();

                        @Override
                        protected void onPreExecute() {
                            showDialog(MY_EPISODES_LOGIN_DIALOG_LOADING);
                        }

                        @Override
                        protected Object doInBackground(Object... objects) {
                            login(user);
                            //todo add a test here to check for a type of cookie to show login has worked ok....
                            //remove true and add in the cookie test
                            //    if (msCookieManager.getCookieStore().getCookies().size() > 0) {
                            storeLoginCredentials(user);
                            //    }
                            return 100L;
                        }

                        @Override
                        protected void onPostExecute(Object o) {
                            removeDialog(MY_EPISODES_LOGIN_DIALOG_LOADING);
                            //todo add a test here to check for a type of cookie to show login has worked ok....
                            //remove true and add in the cookie test
                           // if (msCookieManager.getCookieStore().getCookies().size() > 0) {
                                //  Toast.makeText(LoginActivity.this, R.string.loginSuccessfullLogin, Toast.LENGTH_LONG).show();
                                finalizeLogin();
                         //   } else {
                                ((EditText) findViewById(R.id.loginUsername)).setText("");
                                ((EditText) findViewById(R.id.loginPassword)).setText("");
                                showDialog(MY_EPISODES_ERROR_DIALOG);
                         //   }
                        }
                    };
                    asyncTask.execute();
                } else {
                    showDialog(MY_EPISODES_VALIDATION_REQUIRED_ALL_FIELDS);
                }
            });
        } else {
            finalizeLogin();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch (id) {
            case MY_EPISODES_LOGIN_DIALOG_LOADING:
                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setMessage(this.getString(R.string.loginStartLogin));
                progressDialog.setCancelable(false);
                dialog = progressDialog;
                break;
            case MY_EPISODES_ERROR_DIALOG:
                dialog = new AlertDialog.Builder(this)
                        //                   .setMessage(exceptionMessageResId)
                        .setCancelable(false)
                        .setNeutralButton(R.string.dialogOK, (dialog1, id1) -> removeDialog(MY_EPISODES_ERROR_DIALOG)).create();
                break;
            case MY_EPISODES_VALIDATION_REQUIRED_ALL_FIELDS:
                dialog = new AlertDialog.Builder(this)
                        .setMessage(R.string.fillInAllFields)
                        .setCancelable(false)
                        .setNeutralButton(R.string.dialogOK, (dialog1, id1) -> dialog1.cancel()).create();
                break;
            default:
                dialog = super.onCreateDialog(id);
                break;
        }
        return dialog;
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
        String username = Preferences.getPreference(this, User.USERNAME);
        String password = Preferences.getPreference(this, User.PASSWORD);

        return username != null && password != null;
    }

    private void storeLoginCredentials(User user) {
        Preferences.setPreference(this, User.USERNAME, user.getUsername());
        Preferences.setPreference(this, User.PASSWORD, user.getPassword());
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