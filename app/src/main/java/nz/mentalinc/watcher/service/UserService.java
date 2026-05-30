package nz.mentalinc.watcher.service;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import nz.mentalinc.watcher.constants.MyEpisodeConstants;
import nz.mentalinc.watcher.domain.User;
import nz.mentalinc.watcher.exception.LoginFailedException;
import nz.mentalinc.watcher.exception.PasswordEnctyptionFailedException;
import nz.mentalinc.watcher.exception.RegisterFailedException;
import nz.mentalinc.watcher.http.HttpClientProvider;

public class UserService {
    private static final String LOG_TAG = UserService.class.getSimpleName();

    public void login(User user) throws LoginFailedException {
        login(user.getUsername(), user.getPassword());
    }

    public boolean register(User user, String email) {

        boolean status = false;
        try {
            status = RegisterUser(user.getUsername(), user.getPassword(), email);
        } catch (RegisterFailedException e) {
            //e.printStackTrace();
            e.getCause();
        }

        return status;
    }

    public void login(String username, String password) throws LoginFailedException {
        String response;
        HttpClientProvider http = HttpClientProvider.getInstance();
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put(MyEpisodeConstants.MYEPISODES_LOGIN_PAGE_PARAM_USERNAME, username);
            params.put(MyEpisodeConstants.MYEPISODES_LOGIN_PAGE_PARAM_PASSWORD, password);
            params.put(MyEpisodeConstants.MYEPISODES_FORM_PARAM_ACTION, MyEpisodeConstants.MYEPISODES_LOGIN_PAGE_PARAM_ACTION_VALUE);
            response = http.postFormBody(MyEpisodeConstants.MYEPISODES_LOGIN_PAGE, params);
        } catch (Exception e) {
            response = "";
            Log.e(LOG_TAG, "Login failed", e);
        }

        boolean result = false;

        String responsePage = response;

        if (responsePage.contains("Invalid password") || responsePage.contains("Username not found") || responsePage.contains("ERR_INVALID_REQ")) { // || !responsePage.contains(username)) {
            String message = "Login to MyEpisodes failed. Login page: " + MyEpisodeConstants.MYEPISODES_LOGIN_PAGE + " Username: " + username +
                    " Password: ***** Leaving with status code " + result;
            Log.w(LOG_TAG, message);
            throw new LoginFailedException(message);
        } else {
            Log.d(LOG_TAG, "Successful login to " + MyEpisodeConstants.MYEPISODES_LOGIN_PAGE);
            result = true;
        }
        //  return msCookieManager;
    }

    private boolean RegisterUser(String username, String password, String email) throws RegisterFailedException {
        String response = "";
        HttpClientProvider http = HttpClientProvider.getInstance();
        try {
            HashMap<String, String> params = new HashMap<>();
            params.put(MyEpisodeConstants.MYEPISODES_LOGIN_PAGE_PARAM_USERNAME, username);
            params.put(MyEpisodeConstants.MYEPISODES_LOGIN_PAGE_PARAM_PASSWORD, password);
            params.put(MyEpisodeConstants.MYEPISODES_REGISTER_PAGE_PARAM_EMAIL, email);
            params.put(MyEpisodeConstants.MYEPISODES_FORM_PARAM_ACTION, MyEpisodeConstants.MYEPISODES_REGISTER_PAGE_PARAM_ACTION_VALUE);
            response = http.postFormBody(MyEpisodeConstants.MYEPISODES_REGISTER_PAGE, params);
        } catch (Exception e) {
            String message = "An error occurred during registration";
            Log.e(LOG_TAG, message, e);
        }

        boolean result;


        if (response.contains("Username already exists, please choose another username.")) {
            String message = "Username already exists!";
            Log.w(LOG_TAG, message);
            throw new RegisterFailedException(message);
        } else if (response.contains("Please fill in all fields.")) {
            String message = "Not all fields!";
            Log.w(LOG_TAG, message);
            throw new RegisterFailedException(message);
        } else if (response.contains("Email already exists, please choose another email.")) {
            String message = "Email already excists";
            Log.w(LOG_TAG, message);
            throw new RegisterFailedException(message);
        } else {
            Log.i(LOG_TAG, "User succesfully created in. " + MyEpisodeConstants.MYEPISODES_REGISTER_PAGE);
            result = true;
        }
        return result;
    }

    public String encryptPassword(final String password) throws PasswordEnctyptionFailedException {
        String encryptedPwd;
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(MyEpisodeConstants.PASSWORD_ENCRYPTION_TYPE);
            digest.reset();
            digest.update(password.getBytes());

            byte[] messageDigest = digest.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                hexString.append(String.format("%02x", aMessageDigest));
            }
            encryptedPwd = hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            String message = "The password could not be encrypted because there is no such algorithm (" + MyEpisodeConstants.PASSWORD_ENCRYPTION_TYPE + ")";
            Log.e(LOG_TAG, message, e);
            throw new PasswordEnctyptionFailedException(message, e);
        }
        //Log.d(LOG_TAG, "The encrypted password is " + encryptedPwd); //don't print to the logs unless needed
        return encryptedPwd;
    }
}
