package nz.mentalinc.watcher.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
                        cookieStore.addAll(cookies);
                        pruneExpiredCookies();
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        pruneExpiredCookies();
                        List<Cookie> result = new ArrayList<>();
                        for (Cookie cookie : cookieStore) {
                            if (cookie.matches(url)) {
                                result.add(cookie);
                            }
                        }
                        return result;
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
        Request request = new Request.Builder().url(url).get().build();
        return client.newCall(request).execute();
    }

    public String getBody(String url) throws IOException, InternetConnectivityException, ShowUpdateFailedException {
        Response response = get(url);
        if (!response.isSuccessful()) {
            throw new ShowUpdateFailedException("HTTP " + response.code() + " for GET " + url);
        }
        return response.body() != null ? response.body().string() : "";
    }

    public Response postForm(String url, Map<String, String> params) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                formBuilder.add(entry.getKey(), entry.getValue());
            }
        }
        Request request = new Request.Builder().url(url).post(formBuilder.build()).build();
        return client.newCall(request).execute();
    }

    public String postFormBody(String url, Map<String, String> params) throws IOException {
        Response response = postForm(url, params);
        return response.body() != null ? response.body().string() : "";
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

    public void clearCookies() {
        cookieStore.clear();
    }

    private void pruneExpiredCookies() {
        long now = System.currentTimeMillis();
        cookieStore.removeIf(cookie -> cookie.expiresAt() != -1 && cookie.expiresAt() < now);
    }
}
