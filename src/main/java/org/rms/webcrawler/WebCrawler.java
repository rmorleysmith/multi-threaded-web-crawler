package org.rms.webcrawler;

import org.apache.commons.validator.routines.IntegerValidator;
import org.apache.commons.validator.routines.UrlValidator;

public class WebCrawler {

    // We are masquerading as GoogleBot to help prevent crawl limitations
    public static final String USER_AGENT = "Googlebot/2.1 (+http://www.google.com/bot.html)";

    private static String seedURL;
    private static Integer maxPagesToCrawl;

    public static void main(String[] args) {
        if (inputIsValid(args)) {
            setCrawlParameters(args);
            startCrawling();
        }
    }

    private static void startCrawling() {
        SpiderManager spiderManager = new SpiderManager(seedURL, maxPagesToCrawl);
        spiderManager.executeCrawl();
    }

    private static boolean inputIsValid(String[] args) {

        if (!correctAmountOfArgsGiven(args.length)) {
            System.out.println("Please enter two arguments, a seed URL and the maximum number of pages to crawl");
            return false;
        }

        if (!urlIsValid(args[0])) {
            System.out.printf("URL %s is invalid, please enter a valid URL and try again%n", args[0]);
            return false;
        }

        if (!maxPagesToCrawlIsValid(args[1])) {
            System.out.println("Max pages to crawl is not an integer, please enter a valid integer");
            return false;
        }

        return true;
    }

    private static boolean correctAmountOfArgsGiven(Integer amountOfArgs) {
        return amountOfArgs == 2;
    }

    private static boolean urlIsValid(String URL) {
        return new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS).isValid(URL);
    }

    private static boolean maxPagesToCrawlIsValid(String maxPagesToCrawl) {
        return IntegerValidator.getInstance().isValid(maxPagesToCrawl);
    }

    private static void setCrawlParameters(String[] args) {
        seedURL = args[0];
        maxPagesToCrawl = Integer.valueOf(args[1]);
    }

}
