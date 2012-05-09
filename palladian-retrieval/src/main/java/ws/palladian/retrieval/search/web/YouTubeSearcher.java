package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.date.DateGetterHelper;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.search.SearcherException;

public class YouTubeSearcher extends WebSearcher<WebResult> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(YouTubeSearcher.class);

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    protected final String apiKey;

    public YouTubeSearcher(String apiKey) {
        this.apiKey = apiKey;
    }

    public YouTubeSearcher(Configuration configuration) {
        this.apiKey = configuration.getString("api.youtube.key");
    }

    @Override
    public String getName() {
        return "YouTube";
    }

    private String getRequestUrl(String query, int resultCount, Language language) {
        String url = "https://gdata.youtube.com/feeds/api/videos?q=" + UrlHelper.urlEncode(query);
        url += "&orderby=relevance";
        url += "&start-index=1";
        url += "&max-results=" + Math.min(50, resultCount);
        url += "&v=2";
        url += "&alt=json";
        if (apiKey != null && !apiKey.isEmpty()) {
            url += "&key=" + apiKey;
        }

        return url;
    }

    @Override
    public List<WebResult> search(String query, int resultCount, Language language) throws SearcherException {

        String url = getRequestUrl(query, resultCount, language);

        DocumentRetriever retriever = new DocumentRetriever();

        JSONObject root = retriever.getJsonObject(url);
        TOTAL_REQUEST_COUNT.incrementAndGet();
        JSONObject feed;
        JSONArray entries = new JSONArray();
        try {
            if (root.has("feed")) {
                feed = root.getJSONObject("feed");
                if (feed.has("entry")) {
                    entries = feed.getJSONArray("entry");
                }
            }
        } catch (Exception e) {
            LOGGER.warn(e.getMessage() + ", url: " + url);
        }

        List<WebResult> webResults = new ArrayList<WebResult>();
        for (int i = 0; i < entries.length(); i++) {

            JSONObject entry;
            try {
                entry = entries.getJSONObject(i);

                String published = entry.getJSONObject("published").getString("$t");
                ExtractedDate date = DateGetterHelper.findDate(published);

                String title = entry.getJSONObject("title").getString("$t");

                String link = entry.getJSONObject("content").getString("src");

                WebResult webResult = new WebResult(link, title, "", date.getNormalizedDate());
                webResults.add(webResult);

                if (webResults.size() >= resultCount) {
                    break;
                }

            } catch (JSONException e) {
                LOGGER.error(e.getMessage());
            }
        }

        return webResults;
    }

    @Override
    public int getTotalResultCount(String query, Language language) throws SearcherException {
        int hitCount = 0;
        try {
            String url = getRequestUrl(query, 1, language);

            DocumentRetriever retriever = new DocumentRetriever();
            JSONObject root = retriever.getJsonObject(url);
            TOTAL_REQUEST_COUNT.incrementAndGet();

            hitCount = root.getJSONObject("feed").getJSONObject("openSearch$totalResults").getInt("$t");

        } catch (JSONException e) {
            throw new SearcherException("Exception parsing the JSON response while searching for \"" + query
                    + "\" with " + getName() + ": " + e.getMessage(), e);
        }
        return hitCount;
    }

    public static void main(String[] args) throws SearcherException {
        YouTubeSearcher yts = new YouTubeSearcher("");
        // List<WebResult> results = yts.search("\"htc evo 4g\"", 4);
        System.out.println(yts.getTotalResultCount("\"htc evo 4g\""));
        // CollectionHelper.print(results);
    }
}
