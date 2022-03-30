package org.rms.webcrawler;

import java.util.ArrayList;
import java.util.List;

public class CrawlResult {

    public final List<String> linksFound = new ArrayList<>();

    public void addLinkFound(String link) {
        linksFound.add(link);
    }

    public List<String> getLinksFound() {
        return this.linksFound;
    }

}
