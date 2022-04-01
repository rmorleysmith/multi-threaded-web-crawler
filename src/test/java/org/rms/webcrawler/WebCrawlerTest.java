package org.rms.webcrawler;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WebCrawlerTest {

    private static final Integer PORT = 3000;

    private static final String PAGE_WITH_MULTIPLE_LINKS = "http://localhost:3000/PageWithMultipleLinks.html";
    private static final String PAGE_WITH_NO_META_DESCRIPTION = "http://localhost:3000/PageWithNoMetaDescription.html";
    private static final String PAGE_WITH_NO_LINKS = "http://localhost:3000/PageWithNoLinks.html";
    private static final String PAGE_GIVES_404_STATUS = "http://localhost:3000/PageGives404Status.html";
    private static final String PAGE_WITH_INACCESSIBLE_LINK = "http://localhost:3000/PageWithInaccessibleLink.html";
    private static final String INVALID_URL = "InvalidURL.fail";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(PORT);

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Before
    public void setup() throws IOException {
        setupSuccessResponse("PageWithMultipleLinks.html");
        setupSuccessResponse("PageWithNoMetaDescription.html");
        setupSuccessResponse("PageWithNoLinks.html");
        setupSuccessResponse("PageWithInaccessibleLink.html");
        setupFailureResponse("PageGives404Status.html");
    }

    private void setupSuccessResponse(String page) throws IOException {
        givenThat(get(urlMatching("/" + page)).willReturn(aResponse()
                .withStatus(200)
                .withBody(Resources.toString(Resources.getResource(page), Charset.defaultCharset()))
                .withHeader("Content-type", "text/html")));
    }

    private void setupFailureResponse(String page) {
        givenThat(get(urlMatching("/" + page)).willReturn(aResponse()
                .withStatus(404)));
    }

    @Test
    public void testPageWithMultipleLinks_PrintsMetaDescription_SavesAllLinks() {
        SpiderWorker spiderWorker = new SpiderWorker(PAGE_WITH_MULTIPLE_LINKS);
        CrawlResult crawlResult = spiderWorker.call();
        Assert.assertEquals("Page With Multiple Links - A page with multiple links\n",
                systemOutRule.getLogWithNormalizedLineSeparator()
        );
        Assert.assertEquals(4, crawlResult.getLinksFound().size());
        Assert.assertEquals(PAGE_WITH_MULTIPLE_LINKS, crawlResult.getLinksFound().pollFirst());
        Assert.assertEquals(PAGE_WITH_NO_LINKS, crawlResult.getLinksFound().pollFirst());
        Assert.assertEquals(PAGE_WITH_NO_LINKS, crawlResult.getLinksFound().pollFirst());
        Assert.assertEquals(PAGE_WITH_NO_META_DESCRIPTION, crawlResult.getLinksFound().pollFirst());
    }

    @Test
    public void testPageWithNoMetaDescriptionAndLinks_PrintsDefaultMetaDescription_FindsNoLinks() {
        SpiderWorker spiderWorker = new SpiderWorker(PAGE_WITH_NO_META_DESCRIPTION);
        CrawlResult crawlResult = spiderWorker.call();
        Assert.assertEquals("Page With No Meta Description - No meta description found\n",
                systemOutRule.getLogWithNormalizedLineSeparator()
        );
        Assert.assertEquals(0, crawlResult.getLinksFound().size());
    }

    @Test
    public void testInvalidURL_PrintsWarning() {
        SpiderWorker spiderWorker = new SpiderWorker(INVALID_URL);
        spiderWorker.call();
        Assert.assertEquals(String.format("URL %s is not valid, please enter a valid URL\n", INVALID_URL),
                systemOutRule.getLogWithNormalizedLineSeparator()
        );
    }

    @Test
    public void testInaccessibleURL_PrintsWarning() {
        SpiderWorker spiderWorker = new SpiderWorker(PAGE_GIVES_404_STATUS);
        spiderWorker.call();
        Assert.assertEquals(String.format("Failed to get HTML document for URL: %s\n", PAGE_GIVES_404_STATUS),
                systemOutRule.getLogWithNormalizedLineSeparator()
        );
    }

    @Test
    public void testPageWithMultipleDuplicateLinks_DoesNotCrawlDuplicateLinks() {
        WebCrawler.main(new String[]{PAGE_WITH_MULTIPLE_LINKS, "10"});

        List<String> linesPrinted = convertConsoleOutputToListOfLines();
        Assert.assertTrue(linesPrinted.contains("Page With Multiple Links - A page with multiple links"));
        Assert.assertTrue(linesPrinted.contains("Page With No Links - A page with no links"));
        Assert.assertTrue(linesPrinted.contains("Page With No Meta Description - No meta description found"));
        Assert.assertEquals(3, linesPrinted.size());
    }

    @Test
    public void testCrawlDoesNotExceedMaximumCrawlPagesSpecified() {
        WebCrawler.main(new String[]{PAGE_WITH_MULTIPLE_LINKS, "2"});

        List<String> linesPrinted = convertConsoleOutputToListOfLines();
        Assert.assertTrue(linesPrinted.contains("Page With Multiple Links - A page with multiple links"));
        Assert.assertTrue(linesPrinted.contains("Page With No Links - A page with no links"));
        Assert.assertEquals(2, linesPrinted.size());
    }

    @Test
    public void testCrawlZeroPages_DoesNotCrawl() {
        WebCrawler.main(new String[]{PAGE_WITH_MULTIPLE_LINKS, "0"});
        Assert.assertEquals("", systemOutRule.getLogWithNormalizedLineSeparator());
    }

    @Test
    public void testPageWithInaccessibleLink_DoesNotHaltCrawl() {
        WebCrawler.main(new String[]{PAGE_WITH_INACCESSIBLE_LINK, "10"});

        List<String> linesPrinted = convertConsoleOutputToListOfLines();
        Assert.assertTrue(linesPrinted.contains("Page With Links Including Inaccessible - A page with links including inaccessible one"));
        Assert.assertTrue(linesPrinted.contains(String.format("Failed to get HTML document for URL: %s", PAGE_GIVES_404_STATUS)));
        Assert.assertTrue(linesPrinted.contains("Page With Multiple Links - A page with multiple links"));
        Assert.assertTrue(linesPrinted.contains("Page With No Links - A page with no links"));
        Assert.assertTrue(linesPrinted.contains("Page With No Meta Description - No meta description found"));
        Assert.assertEquals(5, linesPrinted.size());
    }

    @Test
    public void testWrongAmountOfArgsGivesError() {
        WebCrawler.main(new String[]{"arg1", "arg2", "arg3"});
        Assert.assertEquals(
                "Please enter two arguments, a seed URL and the maximum number of pages to crawl\n",
                systemOutRule.getLogWithNormalizedLineSeparator()
        );
    }

    @Test
    public void testInvalidURLGivesError() {
        WebCrawler.main(new String[]{INVALID_URL, "100"});
        Assert.assertEquals(
                String.format("URL %s is invalid, please enter a valid URL and try again\n", INVALID_URL),
                systemOutRule.getLogWithNormalizedLineSeparator()
        );
    }

    @Test
    public void testInvalidNumberGivesError() {
        WebCrawler.main(new String[]{PAGE_WITH_MULTIPLE_LINKS, "InvalidNumber"});
        Assert.assertEquals(
                "Max pages to crawl is not an integer, please enter a valid integer\n",
                systemOutRule.getLogWithNormalizedLineSeparator()
        );
    }

    private List<String> convertConsoleOutputToListOfLines() {
        return Arrays.asList(systemOutRule.getLogWithNormalizedLineSeparator().split("\n"));
    }

}
