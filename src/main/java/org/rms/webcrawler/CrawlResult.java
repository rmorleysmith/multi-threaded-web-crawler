package org.rms.webcrawler;

import java.util.ArrayDeque;
import java.util.Deque;

public class CrawlResult {

    private final Deque<String> linksFound = new ArrayDeque<>();

    public void addLinkFound(String link) {
        linksFound.addLast(link);
    }

    public Deque<String> getLinksFound() {
        return this.linksFound;
    }

}
