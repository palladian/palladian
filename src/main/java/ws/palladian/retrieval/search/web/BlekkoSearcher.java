package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;

/**
 * <p>
 * {@link WebSearcher} implementation for blekko.
 * </p>
 * 
 * @see http://blekko.com
 * @author Philipp Katz
 */
public final class BlekkoSearcher extends WebSearcher<WebResult> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(BlekkoSearcher.class);

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    /** The time in milliseconds we wait between two requests. */
    private static final int THROTTLING_INTERVAL_MS = 1000;

    /** The timestamp of the last request. */
    private static Long lastRequestTimestamp;

    private final String apiKey;

    public BlekkoSearcher(String apiKey) {
        super();
        this.apiKey = apiKey;
    }

    public BlekkoSearcher() {
        ConfigHolder configHolder = ConfigHolder.getInstance();
        PropertiesConfiguration config = configHolder.getConfig();
        apiKey = config.getString("api.blekko.key");
    }

    @Override
    public List<WebResult> search(String query) {

        List<WebResult> webResults = new ArrayList<WebResult>();
        int pageSize = Math.min(getResultCount(), 100);
        int necessaryPages = (int) Math.ceil(getResultCount() / 100.);

        try {

            for (int i = 0; i < necessaryPages; i++) {

                String requestUrl = getRequestUrl(query, pageSize, i);
                checkQueryThrottling();
                HttpResult httpResult = retriever.httpGet(requestUrl);
                TOTAL_REQUEST_COUNT.incrementAndGet();

                String jsonString = new String(httpResult.getContent());
                JSONObject jsonObject = new JSONObject(jsonString);
                JSONArray jsonResults = jsonObject.getJSONArray("RESULT");

                for (int j = 0; j < jsonResults.length(); j++) {
                    JSONObject jsonResult = jsonResults.getJSONObject(j);
                    String summary = null;
                    if (jsonResult.has("snippet")) {
                        summary = jsonResult.getString("snippet");
                    }
                    String url = jsonResult.getString("url");
                    String title = jsonResult.getString("url_title");
                    WebResult webResult = new WebResult(url, title, summary);
                    webResults.add(webResult);
                    if (webResults.size() >= getResultCount()) {
                        break;
                    }
                }

            }

        } catch (HttpException e) {
            LOGGER.error(e);
        } catch (JSONException e) {
            LOGGER.error(e);
        }

        return webResults;
    }

    private String getRequestUrl(String query, int pageSize, int page) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://blekko.com/ws/");
        urlBuilder.append("?q=").append(UrlHelper.urlEncode(query));
        urlBuilder.append("+/json");
        urlBuilder.append("+/ps=").append(pageSize);
        urlBuilder.append("&auth=").append(apiKey);
        urlBuilder.append("&p=").append(page);
        return urlBuilder.toString();
    }

    /**
     * <p>
     * Make sure, we consider the one-second limit between successive requests. If we are below one second, this method
     * blocks and waits.
     * </p>
     */
    private static synchronized void checkQueryThrottling() {
        if (lastRequestTimestamp != null) {
            long millisSinceLastRequest = System.currentTimeMillis() - lastRequestTimestamp;
            if (millisSinceLastRequest < THROTTLING_INTERVAL_MS) {
                try {
                    long millisToSleep = THROTTLING_INTERVAL_MS - millisSinceLastRequest;
                    Thread.sleep(millisToSleep);
                } catch (InterruptedException e) {
                    LOGGER.error(e);
                }
            }
        }
        lastRequestTimestamp = System.currentTimeMillis();
    }

    @Override
    public String getName() {
        return "Blekko";
    }

    @Override
    public int getRequestCount() {
        return TOTAL_REQUEST_COUNT.intValue();
    }

    public static void main(String[] args) {
        WebSearcher<WebResult> searcher = new BlekkoSearcher();
        searcher.setResultCount(250);
        List<WebResult> searchResult = searcher.search("oranges");
        CollectionHelper.print(searchResult);
    }

}
