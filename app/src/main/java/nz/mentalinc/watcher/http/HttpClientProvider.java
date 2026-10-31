package nz.mentalinc.watcher.http;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nz.mentalinc.watcher.constants.MyEpisodeConstants;
import nz.mentalinc.watcher.exception.InternetConnectivityException;
import nz.mentalinc.watcher.exception.ShowUpdateFailedException;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpClientProvider {
    private static final String LOG_TAG = HttpClientProvider.class.getSimpleName();
    private static final Pattern CSRF_TOKEN_PATTERN = Pattern.compile(
            "name=[\"']csrf[-_]token[\"'][^>]*(?:value|content)=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CSRF_JSON_TOKEN_PATTERN = Pattern.compile(
            "\"token\"\\s*:\\s*\"([^\"]+)\"");
    private static HttpClientProvider instance;
    private final OkHttpClient client;
    private final List<Cookie> cookieStore = new ArrayList<>();

    private HttpClientProvider() {
        client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .cookieJar(new CookieJar() {
                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        long now = System.currentTimeMillis();
                        synchronized (cookieStore) {
                            for (Cookie cookie : cookies) {
                                // A cookie with the same name, domain and path replaces the old one.
                                // This also covers deletions, where the server sends an expired cookie.
                                removeCookie(cookie);
                                if (cookie.expiresAt() > now) {
                                    cookieStore.add(cookie);
                                }
                            }
                        }
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        long now = System.currentTimeMillis();
                        synchronized (cookieStore) {
                            cookieStore.removeIf(cookie -> cookie.expiresAt() <= now);
                            List<Cookie> result = new ArrayList<>();
                            for (Cookie cookie : cookieStore) {
                                if (cookie.matches(url)) {
                                    result.add(cookie);
                                }
                            }
                            return result;
                        }
                    }
                })
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
    }

    public static synchronized HttpClientProvider getInstance() {
        if (instance == null) {
            instance = new HttpClientProvider();
        }
        return instance;
    }

    public OkHttpClient getClient() {
        return client;
    }

    public Response get(String url) throws IOException {
        return get(url, null);
    }

    public Response get(String url, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url).get();
        addHeaders(builder, headers);
        return client.newCall(builder.build()).execute();
    }

    public String getBody(String url) throws IOException, InternetConnectivityException, ShowUpdateFailedException {
        Response response = get(url);
        if (!response.isSuccessful()) {
            throw new ShowUpdateFailedException("HTTP " + response.code() + " for GET " + url);
        }
        return response.body() != null ? response.body().string() : "";
    }

    public Response postForm(String url, Map<String, String> params) throws IOException {
        return postForm(url, params, null);
    }

    public Response postForm(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                formBuilder.add(entry.getKey(), entry.getValue());
            }
        }
        return post(url, formBuilder.build(), headers);
    }

    /**
     * Posts an already URL-encoded form body, which allows repeated parameter names such as
     * {@code eps_filters[]}.
     */
    public Response postFormEncoded(String url, String encodedBody, Map<String, String> headers) throws IOException {
        RequestBody body = RequestBody.create(encodedBody,
                MediaType.parse("application/x-www-form-urlencoded; charset=utf-8"));
        return post(url, body, headers);
    }

    public String postFormBody(String url, Map<String, String> params) throws IOException {
        Response response = postForm(url, params);
        return response.body() != null ? response.body().string() : "";
    }

    private Response post(String url, RequestBody body, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url).post(body);
        addHeaders(builder, headers);
        return client.newCall(builder.build()).execute();
    }

    private void addHeaders(Request.Builder builder, Map<String, String> headers) {
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                builder.addHeader(header.getKey(), header.getValue());
            }
        }
    }

    public Response postBody(String url, String body, String contentType) throws IOException {
        RequestBody requestBody = RequestBody.create(body, MediaType.parse(contentType));
        Request request = new Request.Builder().url(url).post(requestBody).build();
        return client.newCall(request).execute();
    }

    public String postBodyResponse(String url, String body, String contentType) throws IOException {
        Response response = postBody(url, body, contentType);
        return response.body() != null ? response.body().string() : "";
    }

    /**
     * Fetches a page with the shared client so its session cookie is stored, then returns the
     * page's CSRF token.
     */
    public String getCsrfToken(String url) throws IOException, InternetConnectivityException, ShowUpdateFailedException {
        return extractCsrfToken(getBody(url));
    }

    public String extractCsrfToken(String page) {
        if (page == null) {
            return null;
        }
        Matcher matcher = CSRF_TOKEN_PATTERN.matcher(page);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * Fetches a fresh session-bound token from the AJAX endpoint, which returns JSON such as
     * {@code {"token":"..."}}.
     */
    public String getAjaxCsrfToken() {
        try {
            return extractJsonCsrfToken(getBody(MyEpisodeConstants.MYEPISODES_AJAX_CSRF_PAGE));
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not retrieve AJAX CSRF token", e);
            return null;
        }
    }

    public String extractJsonCsrfToken(String json) {
        if (json == null) {
            return null;
        }
        Matcher matcher = CSRF_JSON_TOKEN_PATTERN.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * Returns the headers required by non-GET AJAX calls to MyEpisodes.
     */
    public Map<String, String> getCsrfHeaders() {
        String token = getAjaxCsrfToken();
        if (token == null || token.isEmpty()) {
            Log.w(LOG_TAG, "No AJAX CSRF token found on " + MyEpisodeConstants.MYEPISODES_AJAX_CSRF_PAGE);
            return Collections.emptyMap();
        }
        return Collections.singletonMap(MyEpisodeConstants.MYEPISODES_CSRF_TOKEN_HEADER, token);
    }

    /**
     * Fetches the form page with the shared client so its session cookie is stored, then adds
     * the page's CSRF token to the supplied POST parameters.
     *
     * @return true when a token was found and added.
     */
    public boolean addCsrfToken(String pageUrl, Map<String, String> params) {
        if (params == null) {
            return false;
        }
        try {
            String token = getCsrfToken(pageUrl);
            if (token == null || token.isEmpty()) {
                Log.w(LOG_TAG, "No CSRF token found on " + pageUrl);
                return false;
            }
            params.put(MyEpisodeConstants.MYEPISODES_CSRF_TOKEN_PARAM, token);
            return true;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not retrieve CSRF token from " + pageUrl, e);
            return false;
        }
    }

    private void removeCookie(Cookie cookie) {
        cookieStore.removeIf(existing -> existing.name().equals(cookie.name())
                && existing.domain().equalsIgnoreCase(cookie.domain())
                && existing.path().equals(cookie.path()));
    }

    /**
     * Downloads a URL with the shared client so that session cookies are sent, and returns the
     * complete response body.
     */
    public byte[] getBytes(String url) throws IOException {
        try (Response response = get(url)) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + " for GET " + url);
            }
            return response.body() != null ? response.body().bytes() : new byte[0];
        }
    }
}
