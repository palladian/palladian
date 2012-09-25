package ws.palladian.retrieval.search.web;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
 * Search for posts on Google+.
 * </p>
 * 
 * @see <a href="https://developers.google.com/+/api/latest/activities/search">API documentation for search</a>
 * @author Philipp Katz
 */
public final class GooglePlusSearcher extends WebSearcher<WebResult> {

    /** The name of this searcher. */
    private static final String SEARCHER_NAME = "Google+";

    /** The pattern for parsing the returned dates. */
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SZ";

    /** The API key for accessing Google+ API. */
    private final String apiKey;

    /**
     * @param apiKey
     */
    public GooglePlusSearcher(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    @Override
    public List<WebResult> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebResult> results = CollectionHelper.newArrayList();

        String requestUrl = String.format("https://www.googleapis.com/plus/v1/activities?query=%s&key=%s",
                UrlHelper.urlDecode(query), apiKey);
        HttpResult httpResult;
        try {
            httpResult = retriever.httpGet(requestUrl);
        } catch (HttpException e) {
            throw new SearcherException("Encountered HTTP error while accessing \"" + requestUrl + "\": "
                    + e.getMessage(), e);
        }

        String jsonString = HttpHelper.getStringContent(httpResult);
        try {
            JSONObject jsonResult = new JSONObject(jsonString);
            JSONArray jsonItems = jsonResult.getJSONArray("items");
            for (int i = 0; i < jsonItems.length(); i++) {
                JSONObject jsonItem = jsonItems.getJSONObject(i);

                String url = JsonHelper.getString(jsonItem, "url");
                String title = JsonHelper.getString(jsonItem, "title");
                String content = JsonHelper.getString(jsonItem, "content");
                Date date = getCreationDate(JsonHelper.getString(jsonItem, "published"));
                results.add(new WebResult(url, title, content, date, SEARCHER_NAME));

            }
        } catch (JSONException e) {
            throw new SearcherException("Error parsing the JSON response from \"" + requestUrl + "\": " + jsonString, e);
        }

        return results;
    }

    private Date getCreationDate(String dateString) {
        if (dateString == null) {
            return null;
        }
        try {
            return new SimpleDateFormat(DATE_PATTERN).parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    public static void main(String[] args) throws SearcherException {
        GooglePlusSearcher searcher = new GooglePlusSearcher("AIzaSyDPsLByNcOyrAFPlsldd8B2SoBHH3sywmo");
        List<WebResult> result = searcher.search("cat", 10);
        CollectionHelper.print(result);
    }

}
