package org.rms.webcrawler;

import java.util.*;
import java.util.concurrent.*;

public class SpiderManager {

    private static final Integer MAX_THREADS = 10;

    private final ExecutorService spiderExecutor = Executors.newFixedThreadPool(MAX_THREADS);
    private final Queue<Future<CrawlResult>> pendingCrawlTaskResults = new ArrayDeque<>();
    private final Set<String> pagesCrawledOrQueued = new HashSet<>();
    private final RobotRulesManager robotRulesManager = new RobotRulesManager();

    private final String seedURL;
    private final Integer maxPagesToCrawl;

    public SpiderManager(String seedURL, Integer maxPagesToCrawl) {
        this.seedURL = seedURL;
        this.maxPagesToCrawl = maxPagesToCrawl;
    }

    public void executeCrawl() {
        queuePageToCrawlIfAllowed(seedURL);

        // Submit new crawl tasks from links we get from crawl task results, until we run out or reach the limit
        while (!pendingCrawlTaskResults.isEmpty()) {
            try {
                queueNewPagesToCrawlIfAllowed(getNewPagesToCrawlFromCrawlResultQueue());
            } catch (TimeoutException e) {
                System.out.println("A thread timed out while crawling");
            } catch (Exception e) {
                System.out.println("Exception occurred while attempting to get a crawl result");
            }
        }

        spiderExecutor.shutdown();
    }

    private List<String> getNewPagesToCrawlFromCrawlResultQueue() throws ExecutionException, InterruptedException, TimeoutException {
        // We wait a maximum of 10 seconds to get the result of a crawl
        return Objects.requireNonNull(pendingCrawlTaskResults.poll())
                .get(10, TimeUnit.SECONDS)
                .getLinksFound();
    }

    private void queueNewPagesToCrawlIfAllowed(List<String> listOfPages) {
        listOfPages.forEach(this::queuePageToCrawlIfAllowed);
    }

    private void queuePageToCrawlIfAllowed(String link) {
        if (permittedToCrawl(link) && notCrawledOrQueued(link) && withinPageLimit())
            queuePageToCrawl(link);
    }

    private boolean permittedToCrawl(String link) {
        return robotRulesManager.urlAllowed(link);
    }

    private boolean notCrawledOrQueued(String link) {
        return !pagesCrawledOrQueued.contains(link);
    }

    private boolean withinPageLimit() {
        return pagesCrawledOrQueued.size() < maxPagesToCrawl;
    }

    private void queuePageToCrawl(String link) {
        try {
            pagesCrawledOrQueued.add(link);
            pendingCrawlTaskResults.add(spiderExecutor.submit(new SpiderWorker(link)));
        } catch (Exception e) {
            System.out.printf("Exception occurred while attempting to submit URL '%s' to crawl queue", link);
        }
    }

}
