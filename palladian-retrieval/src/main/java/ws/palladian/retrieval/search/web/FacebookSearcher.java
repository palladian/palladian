package ws.palladian.retrieval.search.web;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
 * Search for public Facebook posts using <a href="http://www.facebook.com">Facebook</a>'s Graph API.
 * </p>
 * 
 * @see <a href="http://developers.facebook.com/docs/reference/api/">Facebook Graph API</a>
 * @author Philipp Katz
 */
public final class FacebookSearcher extends WebSearcher<WebResult> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FacebookSearcher.class);

    /** The name of the WebSearcher. */
    private static final String SEARCHER_NAME = "Facebook";

    /** Pattern used for parsing the returned date strings. */
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ssZ";

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    @Override
    public List<WebResult> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebResult> result = CollectionHelper.newArrayList();
        Set<String> urlDeduplication = CollectionHelper.newHashSet();

        for (int page = 0; result.size() < resultCount; page++) {
            String requestUrl = buildRequestUrl(query, page);
            HttpResult httpResult;
            try {
                httpResult = retriever.httpGet(requestUrl);
            } catch (HttpException e) {
                throw new SearcherException("Encountered HTTP exception while accessing \"" + requestUrl + "\"", e);
            }
            String jsonString = HttpHelper.getStringContent(httpResult);
            // LOGGER.debug(jsonString);

            try {
                JSONObject jsonResult = new JSONObject(jsonString);
                JSONArray jsonData = jsonResult.getJSONArray("data");
                if (jsonData.length() == 0 || result.size() == resultCount) {
                    break; // no more results
                }

                for (int i = 0; i < jsonData.length(); i++) {
                    JSONObject jsonEntry = jsonData.getJSONObject(i);

                    String url = JsonHelper.getString(jsonEntry, "link");

                    if (url == null) {
                        continue; // ignore entries without URLs for now.
                    }
                    if (!urlDeduplication.add(url)) {
                        continue; // we already had this URL.
                    }

                    String title = JsonHelper.getString(jsonEntry, "name");
                    String summary = JsonHelper.getString(jsonEntry, "caption");
                    Date date = parseDate(jsonEntry.getString("created_time"));

                    result.add(new WebResult(url, title, summary, date, SEARCHER_NAME));

                    if (result.size() == resultCount) {
                        break;
                    }
                }

            } catch (JSONException e) {
                throw new SearcherException("Error parsing the JSON response from \"" + requestUrl
                        + "\" (result was: \"" + jsonString + "\")", e);
            }
        }
        return result;
    }

    public String buildRequestUrl(String query, int page) {
        StringBuilder requestUrl = new StringBuilder();
        requestUrl.append("https://graph.facebook.com/search");
        requestUrl.append("?q=").append(UrlHelper.urlEncode(query));
        // TODO further types would be possible, see API doc.
        requestUrl.append("&type=post");
        requestUrl.append("&limit=100");
        if (page > 0) {
            requestUrl.append("&offset=").append(page * 100);
        }
        return requestUrl.toString();
    }

    private Date parseDate(String string) {
        try {
            return new SimpleDateFormat(DATE_PATTERN).parse(string);
        } catch (ParseException e) {
            LOGGER.warn("Error parsing date \"" + string + "\"");
            return null;
        }
    }

    public static void main(String[] args) throws SearcherException {
        FacebookSearcher searcher = new FacebookSearcher();
        List<WebResult> result = searcher.search("palladian", 10);
        CollectionHelper.print(result);
    }

}
