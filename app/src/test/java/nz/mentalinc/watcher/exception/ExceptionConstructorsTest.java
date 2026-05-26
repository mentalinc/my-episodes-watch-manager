package nz.mentalinc.watcher.exception;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExceptionConstructorsTest {
    @Test
    public void testFeedUrlParsingException() {
        FeedUrlParsingException ex1 = new FeedUrlParsingException("msg", new Throwable());
        assertEquals("msg", ex1.getMessage());
        assertNotNull(ex1.getCause());

        FeedUrlParsingException ex2 = new FeedUrlParsingException("msg2");
        assertEquals("msg2", ex2.getMessage());

        FeedUrlParsingException ex3 = new FeedUrlParsingException(new Throwable("cause"));
        assertEquals("cause", ex3.getCause().getMessage());
    }

    @Test
    public void testFeedUrlBuildingFaildException() {
        FeedUrlBuildingFaildException ex = new FeedUrlBuildingFaildException("msg", new Throwable());
        assertEquals("msg", ex.getMessage());
        assertNotNull(ex.getCause());
    }

    @Test
    public void testLoginFailedException() {
        LoginFailedException ex1 = new LoginFailedException("msg", new Throwable());
        assertEquals("msg", ex1.getMessage());

        LoginFailedException ex2 = new LoginFailedException("msg2");
        assertEquals("msg2", ex2.getMessage());
    }

    @Test
    public void testShowAddFailedException() {
        ShowAddFailedException ex1 = new ShowAddFailedException("msg", new Throwable());
        assertEquals("msg", ex1.getMessage());

        ShowAddFailedException ex2 = new ShowAddFailedException("msg2");
        assertEquals("msg2", ex2.getMessage());
    }

    @Test
    public void testRssFeedParserException() {
        RssFeedParserException ex1 = new RssFeedParserException("msg", new Throwable());
        assertEquals("msg", ex1.getMessage());

        RssFeedParserException ex2 = new RssFeedParserException("msg2");
        assertEquals("msg2", ex2.getMessage());

        RssFeedParserException ex3 = new RssFeedParserException(new Throwable("cause"));
        assertEquals("cause", ex3.getCause().getMessage());
    }

    @Test
    public void testInternetConnectivityException() {
        InternetConnectivityException ex1 = new InternetConnectivityException("msg", new Throwable());
        assertEquals("msg", ex1.getMessage());

        InternetConnectivityException ex2 = new InternetConnectivityException("msg2");
        assertEquals("msg2", ex2.getMessage());
    }

    @Test
    public void testShowUpdateFailedException() {
        ShowUpdateFailedException ex1 = new ShowUpdateFailedException("msg", new Throwable());
        assertEquals("msg", ex1.getMessage());

        ShowUpdateFailedException ex2 = new ShowUpdateFailedException("msg2");
        assertEquals("msg2", ex2.getMessage());
    }

    @Test
    public void testRegisterFailedException() {
        RegisterFailedException ex1 = new RegisterFailedException("msg", new Throwable());
        assertEquals("msg", ex1.getMessage());

        RegisterFailedException ex2 = new RegisterFailedException("msg2");
        assertEquals("msg2", ex2.getMessage());
    }

    @Test
    public void testUnsupportedHttpPostEncodingException() {
        UnsupportedHttpPostEncodingException ex = new UnsupportedHttpPostEncodingException("msg", new Throwable());
        assertEquals("msg", ex.getMessage());
        assertNotNull(ex.getCause());
    }

    @Test
    public void testPasswordEnctyptionFailedException() {
        PasswordEnctyptionFailedException ex = new PasswordEnctyptionFailedException("msg", new Throwable());
        assertEquals("msg", ex.getMessage());
        assertNotNull(ex.getCause());
    }
}
