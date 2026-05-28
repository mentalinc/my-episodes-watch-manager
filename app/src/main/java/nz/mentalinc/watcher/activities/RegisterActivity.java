package nz.mentalinc.watcher.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import nz.mentalinc.watcher.R;
import nz.mentalinc.watcher.domain.User;
import nz.mentalinc.watcher.exception.InternetConnectivityException;
import nz.mentalinc.watcher.exception.UnsupportedHttpPostEncodingException;
import nz.mentalinc.watcher.service.UserService;
import nz.mentalinc.watcher.utils.TaskRunner;


public class RegisterActivity extends AppCompatActivity {

    private UserService service;
    private User user;
    private boolean registerStatus;
    private String email;

    private static final String LOG_TAG = RegisterActivity.class.getSimpleName();

    private static final String DIALOG_LOADING_TAG = "LOADING";


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
                email = ((EditText) findViewById(R.id.registerEmail)).getText().toString();
                if (username.length() > 0 && password.length() > 0 && email != null && email.length() > 0) {
                    user = new User(
                            username, password
                    );
                    registerStatus = false;

                    showLoadingDialog();

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
                            dismissLoadingDialog();
                            if (registerStatus) {
                                Toast.makeText(RegisterActivity.this, R.string.registerSuccessfull, Toast.LENGTH_LONG).show();
                                finalizeLogin();
                            } else {
                                ((EditText) findViewById(R.id.registerUsername)).setText("");
                                ((EditText) findViewById(R.id.registerPassword)).setText("");
                                ((EditText) findViewById(R.id.registerEmail)).setText("");
                                validationErrorUserExists(RegisterActivity.this);


                            }
                        });
                    });
                } else {
                    validationError(RegisterActivity.this);
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

    private void showLoadingDialog() {
        if (getSupportFragmentManager().findFragmentByTag(DIALOG_LOADING_TAG) == null) {
            LoadingDialogFragment.newInstance(R.string.registerStart).show(getSupportFragmentManager(), DIALOG_LOADING_TAG);
        }
    }

    private void dismissLoadingDialog() {
        Fragment prev = getSupportFragmentManager().findFragmentByTag(DIALOG_LOADING_TAG);
        if (prev != null) ((DialogFragment) prev).dismiss();
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