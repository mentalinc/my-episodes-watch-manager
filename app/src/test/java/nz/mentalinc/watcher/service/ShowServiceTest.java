package nz.mentalinc.watcher.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ShowServiceTest {

    @Test
    public void stripHtmlTags_removesTags() {
        assertEquals("Hello world", ShowService.stripHtmlTags("<p>Hello <b>world</b></p>"));
    }

    @Test
    public void stripHtmlTags_removesAdjacentTags() {
        assertEquals("ab", ShowService.stripHtmlTags("<i></i>a<br/>b"));
    }

    @Test
    public void stripHtmlTags_keepsPlainText() {
        assertEquals("Just text", ShowService.stripHtmlTags("Just text"));
    }

    @Test
    public void stripHtmlTags_keepsUnclosedTagText() {
        assertEquals("before <unclosed", ShowService.stripHtmlTags("before <unclosed"));
    }

    @Test
    public void stripHtmlTags_handlesEmptyAndNull() {
        assertEquals("", ShowService.stripHtmlTags(""));
        assertNull(ShowService.stripHtmlTags(null));
    }

    @Test
    public void stripHtmlTags_handlesLargeInput() {
        StringBuilder input = new StringBuilder();
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < 100_000; i++) {
            input.append("<b>").append(i).append("</b>");
            expected.append(i);
        }

        assertEquals(expected.toString(), ShowService.stripHtmlTags(input.toString()));
    }
}
