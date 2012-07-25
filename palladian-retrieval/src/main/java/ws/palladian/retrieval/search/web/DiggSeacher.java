package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.parser.JsonHelper;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Searcher for <a href="http://digg.com/">Digg</a> API.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href="http://developers.digg.com/version2/search-search">API documentation</a>
 */
public final class DiggSeacher extends WebSearcher<WebResult> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DiggSeacher.class);

    /** The number of items fetched with each request. */
    private static final int FETCH_SIZE = 100;

    @Override
    public String getName() {
        return "Digg";
    }

    @Override
    public List<WebResult> search(String query, int resultCount, Language language) throws SearcherException {
        List<WebResult> results = new ArrayList<WebResult>();

        int necessaryPages = (int)Math.ceil(resultCount / FETCH_SIZE);

        out: for (int offset = 0; offset <= necessaryPages; offset++) {

            String requestUrl = getRequestUrl(query, offset, FETCH_SIZE);
            LOGGER.debug("requestUrl=" + requestUrl);

            HttpResult httpResult;
            try {
                httpResult = retriever.httpGet(requestUrl);
            } catch (HttpException e) {
                throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + ": "
                        + e.getMessage() + ", request URL was: \"" + requestUrl + "\"", e);
            }

            String resultString = HttpHelper.getStringContent(httpResult);
            try {
                JSONObject jsonResult = new JSONObject(resultString);

                Integer availableResults = JsonHelper.getInteger(jsonResult, "count");
                if (availableResults == null || availableResults == 0) {
                    break;
                }

                JSONArray storiesArray = jsonResult.getJSONArray("stories");
                for (int i = 0; i < storiesArray.length(); i++) {
                    JSONObject story = storiesArray.getJSONObject(i);
                    String description = JsonHelper.getString(story, "description");
                    String title = JsonHelper.getString(story, "title");
                    String submitDate = JsonHelper.getString(story, "submit_date");
                    String link = JsonHelper.getString(story, "link");
                    Date date = parseDate(submitDate);
                    results.add(new WebResult(link, title, description, date));
                    if (results.size() == resultCount) {
                        break out;
                    }
                }
            } catch (JSONException e) {
                throw new SearcherException("JSON parse exception while searching for \"" + query + "\" with "
                        + getName() + ": " + e.getMessage() + ", retured JSON was: \"" + resultString + "\"", e);
            }
        }
        return results;
    }

    private Date parseDate(String submitDate) {
        Date date = null;
        try {
            date = new Date(Long.valueOf(submitDate) * 1000);
        } catch (NumberFormatException e) {
        }
        return date;
    }

    private String getRequestUrl(String query, int offset, int fetchSize) {
        return String.format("http://services.digg.com/2.0/search.search?query=%s&count=%d&offset=%s",
                UrlHelper.urlEncode(query), fetchSize, offset);
    }

    public static void main(String[] args) throws SearcherException {
        DiggSeacher diggSeacher = new DiggSeacher();
        List<WebResult> result = diggSeacher.search("euro 2012", 100);
        CollectionHelper.print(result);
    }

}
