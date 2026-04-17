package nz.mentalinc.episodeWatcher.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.domain.User;
import nz.mentalinc.episodeWatcher.exception.LoginFailedException;
import nz.mentalinc.episodeWatcher.service.UserService;


public class LoginActivity extends Activity {
    private UserService service;
    private int exceptionMessageResId = -1;

    private static final int MY_EPISODES_LOGIN_DIALOG_LOADING = 0;
    private static final int MY_EPISODES_ERROR_DIALOG = 1;
    private static final int MY_EPISODES_VALIDATION_REQUIRED_ALL_FIELDS = 2;

    private static final String LOG_TAG = LoginActivity.class.getSimpleName();


    @Override
    public void onCreate(Bundle savedInstanceState) {

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
                    final User user = new User(username, password);

                    //TODO login failed exception doesn't get shown to the user or stop and login takes user to homeactivity but is blank, so need to logout to try again.
                    //todo remove AsyncTask
                    AsyncTask<Object, Object, Object> asyncTask = new AsyncTask<Object, Object, Object>() {
                        final boolean loginStatus = false;


                        //probably don't need cookie manager - need to use the accept all cookies policy thing
                        //  java.net.CookieManager msCookieManager = new java.net.CookieManager();

                        @Override
                        protected void onPreExecute() {
                            showDialog(MY_EPISODES_LOGIN_DIALOG_LOADING);
                            //loginDialog(LoginActivity.this); //--Need to make this work with the thread so will fix and solve as part of moving away from AsyncTask
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
                    //showDialog(MY_EPISODES_VALIDATION_REQUIRED_ALL_FIELDS);
                    validationError(LoginActivity.this);
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
                        //.setMessage(exceptionMessageResId)
                        .setCancelable(false)
                        .setNeutralButton(R.string.dialogOK, (dialog1, id1) -> removeDialog(MY_EPISODES_ERROR_DIALOG)).create();
                break;

            default:
                dialog = super.onCreateDialog(id);
                break;
        }
        return dialog;
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