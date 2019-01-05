package eu.vranckaert.episodeWatcher.service;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import eu.vranckaert.episodeWatcher.constants.MyEpisodeConstants;
import eu.vranckaert.episodeWatcher.domain.User;
import eu.vranckaert.episodeWatcher.exception.InternetConnectivityException;
import eu.vranckaert.episodeWatcher.exception.LoginFailedException;
import eu.vranckaert.episodeWatcher.exception.PasswordEnctyptionFailedException;
import eu.vranckaert.episodeWatcher.exception.RegisterFailedException;
import eu.vranckaert.episodeWatcher.exception.UnsupportedHttpPostEncodingException;

public class UserService {
    private static final String LOG_TAG = UserService.class.getSimpleName();

    public java.net.CookieManager login(User user) throws LoginFailedException {
        return login(user.getUsername(), user.getPassword());
    }

    public boolean register(User user, String email) throws UnsupportedHttpPostEncodingException, InternetConnectivityException {

        boolean status = false;
        try {
            status = RegisterUser(user.getUsername(), user.getPassword(), email);
        } catch (RegisterFailedException e) {
            e.printStackTrace();
        }

        return status;
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    public java.net.CookieManager login(String username, String password) throws LoginFailedException {

        URL url;
        String response = "";
        java.net.CookieManager msCookieManager = new java.net.CookieManager();
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        try {
            url = new URL(MyEpisodeConstants.MYEPISODES_LOGIN_PAGE);

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));

            HashMap postDataParams = new HashMap<String, String>() {{
                put(MyEpisodeConstants.MYEPISODES_LOGIN_PAGE_PARAM_USERNAME, username);
                put(MyEpisodeConstants.MYEPISODES_LOGIN_PAGE_PARAM_PASSWORD, password);
                put(MyEpisodeConstants.MYEPISODES_FORM_PARAM_ACTION, MyEpisodeConstants.MYEPISODES_LOGIN_PAGE_PARAM_ACTION_VALUE);
            }};

            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
/*                final String COOKIES_HEADER = "Set-Cookie";
                //CookieHandler.setDefault( new CookieManager( null, CookiePolicy.ACCEPT_ALL ) );

                Map<String, List<String>> headerFields = conn.getHeaderFields();
                List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);
                if (cookiesHeader != null) {
                    for (String cookie : cookiesHeader) {
                        msCookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                    }
                }

                if (msCookieManager.getCookieStore().getCookies().size() > 0) {
                    // While joining the Cookies, use ',' or ';' as needed. Most of the servers are using ';'
                  //  conn.setRequestProperty(COOKIES_HEADER, TextUtils.join(";", msCookieManager.getCookieStore().getCookies()));
                    String cookiesString = TextUtils.join(";", msCookieManager.getCookieStore().getCookies());
                    Log.d(LOG_TAG + "cookiesString", cookiesString);
                }
*/
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
            } else {
                response = "";

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean result = false;

        String responsePage = response;

        if (responsePage.contains("Wrong username/password") || responsePage.contains("ERR_INVALID_REQ")) { // || !responsePage.contains(username)) {
            String message = "Login to MyEpisodes failed. Login page: " + MyEpisodeConstants.MYEPISODES_LOGIN_PAGE + " Username: " + username +
                    " Password: ***** Leaving with status code " + result;
            Log.w(LOG_TAG, message);
            throw new LoginFailedException(message);
        } else {
            Log.d(LOG_TAG, "Successful login to " + MyEpisodeConstants.MYEPISODES_LOGIN_PAGE);
            result = true;
        }
        return msCookieManager;
    }

    private boolean RegisterUser(String username, String password, String email) throws RegisterFailedException, UnsupportedHttpPostEncodingException, InternetConnectivityException {


        URL url;
        String response = "";
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        try {
            url = new URL(MyEpisodeConstants.MYEPISODES_REGISTER_PAGE);

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));

            HashMap postDataParams = new HashMap<String, String>() {{
                put(MyEpisodeConstants.MYEPISODES_LOGIN_PAGE_PARAM_USERNAME, username);
                put(MyEpisodeConstants.MYEPISODES_LOGIN_PAGE_PARAM_PASSWORD, password);
                put(MyEpisodeConstants.MYEPISODES_REGISTER_PAGE_PARAM_EMAIL, email);
                put(MyEpisodeConstants.MYEPISODES_FORM_PARAM_ACTION, MyEpisodeConstants.MYEPISODES_REGISTER_PAGE_PARAM_ACTION_VALUE);
            }};

            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;

                }
            } else {
                response = "";
            }
        } catch (UnsupportedEncodingException e) {
            String message = "Could not start register because the HTTP post encoding is not supported";
            Log.e(LOG_TAG, message, e);
            throw new UnsupportedHttpPostEncodingException(message, e);
        } catch (MalformedURLException e) {
            String message = "The feed URL could not be build";
            Log.e(LOG_TAG, message, e);
        } catch (Exception e) {
            String message = "An error occured";
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

    public String encryptPassword(final String password) throws
            PasswordEnctyptionFailedException {
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
