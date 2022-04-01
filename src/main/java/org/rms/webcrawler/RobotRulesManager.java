package org.rms.webcrawler;

import crawlercommons.robots.SimpleRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Optional;

import static org.rms.webcrawler.WebCrawler.USER_AGENT;

public class RobotRulesManager {

    private final SimpleRobotRulesParser robotRulesParser = new SimpleRobotRulesParser();
    private final HashMap<String, SimpleRobotRules> robotRulesMap = new HashMap<>();

    public boolean urlAllowed(String URL) {
        // If string conversion to URL fails we just return true
        return getHostFromStringURLIfValidElseEmpty(URL)
                .map(host -> getSavedRulesForHostOrFetchThem(host).isAllowed(URL))
                .orElse(true);
    }

    private Optional<String> getHostFromStringURLIfValidElseEmpty(String url) {
        try {
            return Optional.of(new URL(url).getHost());
        } catch (MalformedURLException e) {
            return Optional.empty();
        }
    }

    private SimpleRobotRules getSavedRulesForHostOrFetchThem(String host) {
        return Optional.ofNullable(robotRulesMap.get(host))
                .orElseGet(() -> fetchRobotRulesForHost(host));
    }

    private SimpleRobotRules fetchRobotRulesForHost(String host) {
        SimpleRobotRules rules = tryGetRulesViaHttp(host).orElseGet(this::getDefaultRobotRules);
        robotRulesMap.put(host, rules);

        return rules;
    }

    private Optional<SimpleRobotRules> tryGetRulesViaHttp(String host) {
        try {
            return Optional.of(getRulesViaHttp(host));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private SimpleRobotRules getRulesViaHttp(String host) throws IOException {
        HttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(getDefaultRequestConfig()).build();
        HttpGet httpGet = new HttpGet("https://" + host + "/robots.txt");
        HttpContext context = new BasicHttpContext();
        HttpResponse response = httpClient.execute(httpGet, context);

        if (response.getStatusLine() != null && response.getStatusLine().getStatusCode() == 404) {
            // Free resources and return default rules
            EntityUtils.consumeQuietly(response.getEntity());
            return getDefaultRobotRules();
        } else {
            // Parse rules from robots.txt
            BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());
            return parseRobotRulesFromByteArray(host, IOUtils.toByteArray(entity.getContent()));
        }
    }

    private RequestConfig getDefaultRequestConfig() {
        return RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();
    }

    private SimpleRobotRules getDefaultRobotRules() {
        return new SimpleRobotRules((SimpleRobotRules.RobotRulesMode.ALLOW_ALL));
    }

    private SimpleRobotRules parseRobotRulesFromByteArray(String host, byte[] byteArray) {
        return robotRulesParser.parseContent(host, byteArray, "text/plain", USER_AGENT);
    }
}