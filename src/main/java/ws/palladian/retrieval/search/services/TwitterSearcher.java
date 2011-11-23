package ws.palladian.retrieval.search.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.search.WebResult;
import ws.palladian.retrieval.search.WebSearcher;
import ws.palladian.retrieval.search.WebSearcherManager;

public final class TwitterSearcher extends BaseWebSearcher implements WebSearcher {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(TwitterSearcher.class);

    private static final AtomicInteger requestCount = new AtomicInteger();

    @Override
    public List<WebResult> search(String query, int resultCount, WebSearcherLanguage language) {

        List<WebResult> webresults = new ArrayList<WebResult>();

        // FIXME dirty; WebSearcher surrounds query string with %22 when in "exact mode" ..
        // remove them here, as twitter will give no results ...
        if (query.contains("%22")) {
            query = query.replace("%22", "");
        }

        int resultsPerPage = Math.min(100, getResultCount());
        int numRequests = (int) Math.ceil(getResultCount() / 100.0);
        int rank = 1;

        for (int page = 1; page <= numRequests; page++) {

            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("http://search.twitter.com/search.json");
            urlBuilder.append("?q=").append(UrlHelper.urlEncode(query));
            urlBuilder.append("&page=").append(page);
            urlBuilder.append("&rpp=").append(resultsPerPage);

            HttpResult httpResult;
            try {
                httpResult = retriever.httpGet(urlBuilder.toString());
            } catch (HttpException e) {
                LOGGER.error(e);
                break;
            }

            requestCount.incrementAndGet();

            int statusCode = httpResult.getStatusCode();
            if (statusCode == 420) {
                LOGGER.error("twitter is currently blocked due to rate limit");
                break;
            }
            if (statusCode >= 400) {
                LOGGER.error("http error " + statusCode);
                break;
            }

            try {

                String responseString = new String(httpResult.getContent());
                LOGGER.debug("response for " + urlBuilder + " : " + responseString);

                JSONObject jsonObject = new JSONObject(responseString);
                JSONArray jsonResults = jsonObject.getJSONArray("results");
                int numResults = jsonResults.length();

                // stop, if we got no results
                if (numResults == 0) {
                    break;
                }

                for (int i = 0; i < numResults; i++) {

                    JSONObject jsonResult = jsonResults.getJSONObject(i);

                    String text = jsonResult.getString("text");
                    String dateString = jsonResult.getString("created_at");

                    // strange code from old implementation
                    if (text.contains("http:")) {

                        String currentURL = text.replaceAll("(.+)http:", "http:");

                        if (currentURL.contains(" ")) {
                            currentURL = currentURL.split(" ", 2)[0];
                        }

                        // Assigning the url format regular expression
                        String urlPattern = "^http(s{0,1})://[a-zA-Z0-9_/\\-\\.]+\\.([A-Za-z/]{2,5})[a-zA-Z0-9_/\\&\\?\\=\\-\\.\\~\\%]*";
                        if (currentURL.matches(urlPattern)) {
                            WebResult webresult = new WebResult(WebSearcherManager.TWITTER, rank, currentURL, null,
                                    text, dateString);
                            rank++;
                            webresults.add(webresult);
                        }
                    }
                }
            } catch (JSONException e) {
                LOGGER.error("error parsing the JSON response", e);
            }
        }

        LOGGER.info("twitter requests: " + requestCount.get());
        return webresults;

    }

    @Override
    public String getName() {
        return "Twitter";
    }

}
