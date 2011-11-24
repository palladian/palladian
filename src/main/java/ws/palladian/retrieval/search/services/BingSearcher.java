package ws.palladian.retrieval.search.services;

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
import ws.palladian.retrieval.search.WebResult;
import ws.palladian.retrieval.search.WebSearcher;

/**
 * @see http://www.bing.com/developers/s/APIBasics.html
 * @author Philipp Katz
 */
public final class BingSearcher extends BaseWebSearcher implements WebSearcher {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(BingSearcher.class);

    private static final AtomicInteger requestCount = new AtomicInteger();

    private final String apiKey;

    public BingSearcher(String apiKey) {
        super();
        this.apiKey = apiKey;
    }

    public BingSearcher() {
        ConfigHolder configHolder = ConfigHolder.getInstance();
        PropertiesConfiguration config = configHolder.getConfig();
        apiKey = config.getString("api.bing.key");
    }

    @Override
    public List<WebResult> search(String query) {

        List<WebResult> webresults = new ArrayList<WebResult>();

        // set the preferred language (Common request fields)
        String languageString = "en-us";
        if (getLanguage() == WebSearcherLanguage.GERMAN) {
            languageString = "de-de";
        }

        int rank = 1;
        int urlsCollected = 0;
        int grabSize = (int) Math.ceil((double) getResultCount() / 25);
        for (int i = 0; i < grabSize; i++) {

            int offset = i * 25 + 1;
            String urlString = "http://api.bing.net/json.aspx?AppId=" + apiKey + "&Web.Count=25&Web.Offset=" + offset
                    + "&Sources=Web&JsonType=raw&Adult=Moderate&Market=" + languageString + "&Query=" + query;

            try {

                JSONObject jsonOBJ = retriever.getJSONDocument(urlString);
                JSONObject jsonWeb = jsonOBJ.getJSONObject("SearchResponse").getJSONObject("Web");

                int total = jsonWeb.getInt("Total");
                if (offset > total) {
                    break;
                }

                JSONArray results = jsonWeb.getJSONArray("Results");
                int resultSize = results.length();

                for (int j = 0; j < resultSize; ++j) {
                    if (urlsCollected < getResultCount()) {

                        WebResult webResult = new WebResult();
                        JSONObject currentResult = results.getJSONObject(j);

                        String currentURL = currentResult.getString("Url");
                        webResult.setUrl(currentURL);

                        if (currentResult.has("Title")) {
                            webResult.setTitle(currentResult.getString("Title"));
                        }
                        if (currentResult.has("Description")) {
                            webResult.setSummary(currentResult.getString("Description"));
                        }
                        webResult.setRank(rank);

                        rank++;

                        LOGGER.debug("bing retrieved url " + currentURL);
                        webresults.add(webResult);

                        ++urlsCollected;
                    } else {
                        break;
                    }
                }

            } catch (JSONException e) {
                LOGGER.error(e.getMessage());
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }

            requestCount.incrementAndGet();
            LOGGER.info("bing requests: " + requestCount.get());
        }

        return webresults;

    }

    @Override
    public int getResultCount(String query) {

        int hitCount = 0;

        String urlString = "http://api.bing.net/json.aspx?AppId=" + apiKey
                + "&Web.Count=1&Sources=Web&JsonType=raw&Query=" + UrlHelper.urlEncode(query);

        try {
            JSONObject jsonOBJ = retriever.getJSONDocument(urlString);

            hitCount = jsonOBJ.getJSONObject("SearchResponse").getJSONObject("Web").getInt("Total");

        } catch (JSONException e) {
            LOGGER.error(e.getMessage());
        }

        return hitCount;
    }

    @Override
    public String getName() {
        return "Bing";
    }

}
