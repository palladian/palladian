package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * {@link WebSearcher} implementation for webknox.
 * </p>
 * 
 * 
 * @see http://www.webknox.com/
 * @see http://webknox.com/api#!/index/search_GET
 * @author David Urbansky
 */
public class WebKnoxSearcher extends WebSearcher<WebResult> {

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    private final String appId;
    private final String apiKey;

    /**
     * <p>
     * Creates a new webknox searcher.
     * </p>
     */
    public WebKnoxSearcher(String appId, String apiKey) {
        this.appId = appId;
        this.apiKey = apiKey;
    }

    public WebKnoxSearcher(Configuration configuration) {
        this.appId = configuration.getString("api.webknox.appId");
        this.apiKey = configuration.getString("api.webknox.apiKey");
    }

    /**
     * <p>
     * Only English is supported.
     * </p>
     */
    @Override
    public List<WebResult> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebResult> webResults = new ArrayList<WebResult>();

        try {
            String requestUrl = getRequestUrl(query, resultCount, language);
            HttpResult httpResult = retriever.httpGet(requestUrl);
            TOTAL_REQUEST_COUNT.incrementAndGet();

            String jsonString = HttpHelper.getStringContent(httpResult);
            JSONObject jsonObject = new JSONObject(jsonString);

            if (!jsonObject.has("results")) {
                return webResults;
            }

            JSONArray jsonResults = jsonObject.getJSONArray("results");

            for (int j = 0; j < jsonResults.length(); j++) {
                JSONObject jsonResult = jsonResults.getJSONObject(j);
                String summary = null;
                String url = jsonResult.getString("url");
                String title = jsonResult.getString("title");
                WebResult webResult = new WebResult(url, title, summary, getName());
                webResults.add(webResult);
                if (webResults.size() >= resultCount) {
                    break;
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

    private String getRequestUrl(String query, int resultCount, Language language) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://webknox.com/api/index/websites");
        urlBuilder.append("?query=").append(UrlHelper.urlEncode(query));
        urlBuilder.append("&offset=0");
        urlBuilder.append("&numResults=").append(Math.min(resultCount, 100));
        urlBuilder.append("&apiKey=").append(apiKey);
        urlBuilder.append("&appId=").append(appId);

        System.out.println(urlBuilder);

        return urlBuilder.toString();
    }

    @Override
    public String getName() {
        return "WebKnox";
    }

    /**
     * <p>
     * Gets the number of HTTP requests sent to webknox.
     * </p>
     * 
     * @return
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.intValue();
    }

    public static void main(String[] args) throws SearcherException {
        WebKnoxSearcher webKnoxSearcher = new WebKnoxSearcher(ConfigHolder.getInstance().getConfig());
        CollectionHelper.print(webKnoxSearcher.search("Conan O'Brien", 10));
    }
}
