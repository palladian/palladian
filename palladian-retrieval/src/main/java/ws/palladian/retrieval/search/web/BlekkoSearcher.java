package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * {@link WebSearcher} implementation for blekko.
 * </p>
 * 
 * <p>
 * Note: blekko also works without an API key and seems to return different results depending on whether an api key is
 * set or not (as of June 30, 2012).
 * </p>
 * 
 * @see http://blekko.com
 * @see http://help.blekko.com/index.php/tag/api/
 * @author Philipp Katz
 */
public final class BlekkoSearcher extends WebSearcher<WebResult> {


    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BlekkoSearcher.class);

    /** Key of the {@link Configuration} key for the API key. */
    public static final String CONFIG_API_KEY = "api.blekko.key";
    
    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    /** The time in milliseconds we wait between two requests. */
    private static final int THROTTLING_INTERVAL_MS = 1000;

    /** The timestamp of the last request. */
    private static Long lastRequestTimestamp;

    private final String apiKey;

    /**
     * <p>
     * Creates a new blekko searcher.
     * </p>
     * 
     * @param apiKey The API key for accessing blekko.
     */
    public BlekkoSearcher(String apiKey) {
        super();
        this.apiKey = apiKey;
    }

    /**
     * <p>
     * Creates a new blekko searcher WITHOUT api key.
     * </p>
     * 
     */
    public BlekkoSearcher() {
        super();
        this.apiKey = null;
    }

    /**
     * <p>
     * Creates a new blekko searcher.
     * </p>
     * 
     * @param configuration The configuration which must provide an API key for accessing blekko, which must be provided
     *            as string via key <tt>api.blekko.key</tt> in the configuration.
     */
    public BlekkoSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    @Override
    public List<WebResult> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebResult> webResults = new ArrayList<WebResult>();
        int pageSize = Math.min(resultCount, 100);
        int necessaryPages = (int) Math.ceil(resultCount / 100.);

        try {

            for (int i = 0; i < necessaryPages; i++) {

                String requestUrl = getRequestUrl(query, pageSize, i);
                checkQueryThrottling();
                HttpResult httpResult = retriever.httpGet(requestUrl);
                TOTAL_REQUEST_COUNT.incrementAndGet();

                String jsonString = HttpHelper.getStringContent(httpResult);
                JSONObject jsonObject = new JSONObject(jsonString);
                
                if (!jsonObject.has("RESULT")) {
                    continue;
                }
                
                JSONArray jsonResults = jsonObject.getJSONArray("RESULT");

                for (int j = 0; j < jsonResults.length(); j++) {
                    JSONObject jsonResult = jsonResults.getJSONObject(j);
                    String summary = null;
                    if (jsonResult.has("snippet")) {
                        summary = jsonResult.getString("snippet");
                    }
                    String url = jsonResult.getString("url");
                    String title = jsonResult.getString("url_title");
                    WebResult webResult = new WebResult(url, title, summary, getName());
                    webResults.add(webResult);
                    if (webResults.size() >= resultCount) {
                        break;
                    }
                }

            }

        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + ": "
                    + e.getMessage(), e);
        } catch (JSONException e) {
            throw new SearcherException("Error parsing the JSON response while searching for \"" + query + "\" with "
                    + getName() + ": " + e.getMessage(), e);
        }

        return webResults;
    }

    private String getRequestUrl(String query, int pageSize, int page) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://blekko.com/ws/");
        urlBuilder.append("?q=").append(UrlHelper.encodeParameter(query));
        urlBuilder.append("+/json");
        urlBuilder.append("+/ps=").append(pageSize);
        if (this.apiKey != null) {
            urlBuilder.append("&auth=").append(apiKey);
        }
        urlBuilder.append("&p=").append(page);

        // System.out.println(urlBuilder.toString());

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
                    LOGGER.warn("InterruptedException");
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
    public int getTotalResultCount(String query, Language language) throws SearcherException {
        int totalResults = 0;

        String requestUrl = getRequestUrl(query, 1, 0);
        checkQueryThrottling();
        HttpResult httpResult;
        try {
            httpResult = retriever.httpGet(requestUrl);
            TOTAL_REQUEST_COUNT.incrementAndGet();
    
            String jsonString = HttpHelper.getStringContent(httpResult);
            JSONObject jsonObject = new JSONObject(jsonString);
            
            // System.out.println(jsonObject.toString(2));

            if (jsonObject != null && jsonObject.has("universal_total_results")) {
                String string = jsonObject.getString("universal_total_results");
                string = string.replace("K", "000");
                string = string.replace("M", "000000");
                try {
                    totalResults = Integer.parseInt(string);
                } catch (Exception e) {
                    // ccl pattern in action
                }
            }
            
        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching total result count for \"" + query + "\" with " + getName() + ": "
                    + e.getMessage(), e);
        } catch (JSONException e) {
            throw new SearcherException("Error parsing the JSON response while searching total result count for \"" + query + "\" with "
                    + getName() + ": " + e.getMessage(), e);
        }
        
        return totalResults;
    }

    /**
     * <p>
     * Gets the number of HTTP requests sent to blekko.
     * </p>
     * 
     * @return
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.intValue();
    }

    public static void main(String[] args) throws SearcherException {
        // CollectionHelper.print(new BlekkoSearcher(ConfigHolder.getInstance().getConfig()).search("cinefreaks", 10));
        // System.out.println(new
        // BlekkoSearcher(ConfigHolder.getInstance().getConfig()).getTotalResultCount("inurl:\"cinefreaks\""));
    }
}
