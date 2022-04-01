package org.rms.webcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.rms.webcrawler.WebCrawler.USER_AGENT;

public class SpiderWorker implements Callable<CrawlResult> {

    private final CrawlResult crawlResult = new CrawlResult();
    private final String pageToCrawl;
    private Document htmlDocument;

    public SpiderWorker(String pageToCrawl)  {
        this.pageToCrawl = pageToCrawl;
    }

    @Override
    public CrawlResult call() {
        crawl();
        return crawlResult;
    }

    private void crawl() {
        try {
            htmlDocument = getHTMLDocument(pageToCrawl);
            performCrawlTasks();
        } catch (IOException e) {
            System.out.printf("Failed to get HTML document for URL: %s%n", pageToCrawl);
        } catch (IllegalArgumentException e) {
            System.out.printf("URL %s is not valid, please enter a valid URL%n", pageToCrawl);
        }
    }

    private Document getHTMLDocument(String pageURL) throws IOException, IllegalArgumentException {
        // Connect to the URL and parse the HTML document
        // We wait a maximum of 10 seconds before timing out
        return Jsoup.connect(pageURL).timeout(10 * 1000).userAgent(USER_AGENT).get();
    }

    private void performCrawlTasks() {
        outputTitleAndMetaDescription();
        addPageLinksToLinksFound();
    }

    private void outputTitleAndMetaDescription() {
        System.out.printf("%s - %s%n", getPageTitle(), getPageDescriptionFromMetaDataOrDefault());
    }

    private String getPageTitle() {
        return htmlDocument.title();
    }

    private String getPageDescriptionFromMetaDataOrDefault() {
        return Optional.ofNullable(htmlDocument.selectFirst("meta[name=description]"))
                .map(element -> element.attr("content"))
                .orElse("No meta description found");
    }

    private void addPageLinksToLinksFound() {
        getAllLinksOnPage().forEach(this::AddLinkToLinksFound);
    }

    private void AddLinkToLinksFound(Element linkElement) {
        crawlResult.addLinkFound(getURLFromLinkElement(linkElement));
    }

    private Elements getAllLinksOnPage() {
        return htmlDocument.select("a[href]");
    }

    private String getURLFromLinkElement(Element link) {
        return link.absUrl("href");
    }

}
