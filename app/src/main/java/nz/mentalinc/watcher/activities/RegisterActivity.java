package nz.mentalinc.watcher.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import nz.mentalinc.watcher.R;
import nz.mentalinc.watcher.domain.User;
import nz.mentalinc.watcher.exception.InternetConnectivityException;
import nz.mentalinc.watcher.exception.UnsupportedHttpPostEncodingException;
import nz.mentalinc.watcher.service.UserService;
import nz.mentalinc.watcher.utils.TaskRunner;


public class RegisterActivity extends Activity {

    private UserService service;
    private User user;
    private boolean registerStatus;
    private String email;

    private static final int MY_EPISODES_REGISTER_DIALOG_LOADING = 0;
    private static final int MY_EPISODES_ERROR_DIALOG = 1;
    private static final int MY_EPISODES_VALIDATION_REQUIRED_ALL_FIELDS = 2;

    private static final String LOG_TAG = RegisterActivity.class.getSimpleName();


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

        if (!checkLoginCredentials()) {
            setContentView(R.layout.register);

            Button registerButton = findViewById(R.id.registerRegister);
            registerButton.setOnClickListener(v -> {
                String username = ((EditText) findViewById(R.id.registerUsername)).getText().toString();
                String password = ((EditText) findViewById(R.id.registerPassword)).getText().toString();
                //TODO work out what this email is used for as says always null
                email = ((EditText) findViewById(R.id.registerEmail)).getText().toString();
                if (username.length() > 0 && password.length() > 0 && email != null && email.length() > 0) {
                    user = new User(
                            username, password
                    );
                    registerStatus = false;

                    showDialog(MY_EPISODES_REGISTER_DIALOG_LOADING);

                    TaskRunner.getExecutor().execute(() -> {
                        try {
                            registerStatus = register(user);
                        } catch (InternetConnectivityException e) {
                            String message = "Could not connect to host";
                            Log.e(LOG_TAG, message, e);

                        } catch (Exception e) {
                            String message = "Some Exception occured";
                            Log.e(LOG_TAG, message, e);
                        }
                        if (registerStatus) {
                            storeLoginCredentials(user);
                        }

                        runOnUiThread(() -> {
                            removeDialog(MY_EPISODES_REGISTER_DIALOG_LOADING);
                            if (registerStatus) {
                                Toast.makeText(RegisterActivity.this, R.string.registerSuccessfull, Toast.LENGTH_LONG).show();
                                finalizeLogin();
                            } else {
                                ((EditText) findViewById(R.id.registerUsername)).setText("");
                                ((EditText) findViewById(R.id.registerPassword)).setText("");
                                ((EditText) findViewById(R.id.registerEmail)).setText("");
                                //showDialog(MY_EPISODES_ERROR_DIALOG);
                                validationErrorUserExists(RegisterActivity.this);


                            }
                        });
                    });
                } else {
                    //showDialog(MY_EPISODES_VALIDATION_REQUIRED_ALL_FIELDS);
                    validationError(RegisterActivity.this);
                }
            });
        } else {
            finalizeLogin();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
            case MY_EPISODES_REGISTER_DIALOG_LOADING:
                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setMessage(this.getString(R.string.registerStart));
                progressDialog.setCancelable(false);
                dialog = progressDialog;
                break;
            default: //added for code quality
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

    public void validationErrorUserExists(Context context) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
        dialog.setTitle(R.string.exceptionDialogTitle);
        dialog.setMessage(R.string.registerFailed);
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


    private boolean checkLoginCredentials() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String username = sharedPref.getString("username", null);
        String password = sharedPref.getString("UserPassword", null);
        return username != null && password != null;
    }

    private boolean register(User user) throws UnsupportedHttpPostEncodingException, InternetConnectivityException {
        return service.register(user, email);
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

}