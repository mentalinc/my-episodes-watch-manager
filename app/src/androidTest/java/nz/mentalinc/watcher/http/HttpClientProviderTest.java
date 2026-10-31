package nz.mentalinc.watcher.http;

import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.*;

public class HttpClientProviderTest {
    private final HttpClientProvider client = HttpClientProvider.getInstance();

    @Test
    public void testGet() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("hello").setResponseCode(200));
        server.start();

        String body = client.getBody(server.url("").toString());
        assertEquals("hello", body);

        server.shutdown();
    }

    @Test
    public void testGet_handles404() throws IOException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(404));
        server.start();

        try {
            client.getBody(server.url("").toString());
            fail("Expected exception for 404");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("404"));
        }

        server.shutdown();
    }

    @Test
    public void testPostForm() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("ok").setResponseCode(200));
        server.start();

        Map<String, String> params = new HashMap<>();
        params.put("key1", "value1");
        params.put("key2", "value2");

        String body = client.postFormBody(server.url("").toString(), params);
        assertEquals("ok", body);

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        String reqBody = request.getBody().readUtf8();
        assertTrue(reqBody.contains("key1=value1"));
        assertTrue(reqBody.contains("key2=value2"));

        server.shutdown();
    }

    @Test
    public void testPostBody() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("response").setResponseCode(200));
        server.start();

        String body = client.postBodyResponse(
                server.url("").toString(),
                "{\"data\":\"test\"}",
                "application/json");
        assertEquals("response", body);

        RecordedRequest request = server.takeRequest();
        assertEquals("application/json; charset=utf-8", request.getHeader("Content-Type"));

        server.shutdown();
    }

    @Test
    public void testGetRawResponse() throws IOException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("raw").setResponseCode(200));
        server.start();

        okhttp3.Response response = client.get(server.url("").toString());
        assertEquals(200, response.code());
        assertEquals("raw", response.body().string());

        server.shutdown();
    }

    @Test
    public void testPostFormRawResponse() throws IOException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("formOk").setResponseCode(200));
        server.start();

        Map<String, String> params = new HashMap<>();
        params.put("a", "b");
        okhttp3.Response response = client.postForm(server.url("").toString(), params);
        assertEquals(200, response.code());

        server.shutdown();
    }
}
