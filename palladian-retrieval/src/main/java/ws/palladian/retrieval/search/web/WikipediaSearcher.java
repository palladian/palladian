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
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.parser.JsonHelper;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Search for <a href="http://www.wikipedia.org">Wikipedia</a> articles with fulltext search.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href="http://www.mediawiki.org/wiki/API:Search">MediaWiki API:Search</a>
 * @see <a href="http://de.wikipedia.org/w/api.php">MediaWiki API</a>
 * @see <a href="http://stackoverflow.com/questions/964454/how-to-use-wikipedia-api-if-it-exists">How to use wikipedia
 *      api if it exists?</a>
 */
public final class WikipediaSearcher extends WebSearcher<WebResult> {

    /** The name of this searcher. */
    private static final String NAME = "Wikipedia";

    /** Pattern used for parsing the returned date strings. */
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<WebResult> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebResult> results = CollectionHelper.newArrayList();
        String baseUrl = getBaseUrl(language);

        // fetch in chunks of 50 items, this is maximum size
        for (int offset = 0; offset < resultCount; offset += 50) {

            JSONObject jsonResult = fetchJsonResponse(query, baseUrl, offset, 50);

            try {
                JSONObject jsonQuery = jsonResult.getJSONObject("query");
                JSONArray searchResults = jsonQuery.getJSONArray("search");

                if (searchResults.length() == 0) {
                    break; // no more results
                }

                for (int i = 0; i < searchResults.length(); i++) {
                    JSONObject resultItem = searchResults.getJSONObject(i);
                    String title = JsonHelper.getString(resultItem, "title");
                    String snippet = HtmlHelper.stripHtmlTags(JsonHelper.getString(resultItem, "snippet"));
                    Date date = parseDate(JsonHelper.getString(resultItem, "timestamp"));
                    String url = getPageUrl(baseUrl, title);
                    results.add(new WebResult(url, title, snippet, date, NAME));

                    if (results.size() == resultCount) {
                        break;
                    }

                }
            } catch (JSONException e) {
                throw new SearcherException("JSON parse error: " + e.getMessage(), e);
            }
        }

        return results;
    }

    private JSONObject fetchJsonResponse(String query, String baseUrl, int offset, int limit) throws SearcherException {
        String queryUrl = getQueryUrl(baseUrl, query, offset, limit);
        HttpResult httpResult;
        try {
            httpResult = retriever.httpGet(queryUrl);
        } catch (HttpException e) {
            throw new SearcherException("HTTP error while accessing \"" + queryUrl + "\": " + e.getMessage(), e);
        }
        String jsonString = HttpHelper.getStringContent(httpResult);
        try {
            return new JSONObject(jsonString);
        } catch (JSONException e) {
            throw new SearcherException("JSON parse error while parsing \"" + jsonString + "\": " + e.getMessage(), e);
        }
    }

    @Override
    public int getTotalResultCount(String query, Language language) throws SearcherException {
        String baseUrl = getBaseUrl(language);
        JSONObject jsonResult = fetchJsonResponse(query, baseUrl, 0, 1);
        try {
            JSONObject jsonQuery = jsonResult.getJSONObject("query");
            JSONObject jsonInfo = jsonQuery.getJSONObject("searchinfo");
            return jsonInfo.getInt("totalhits");
        } catch (JSONException e) {
            throw new SearcherException("JSON parse error: " + e.getMessage(), e);
        }
    }

    private Date parseDate(String dateString) {
        if (dateString == null) {
            return null;
        }
        try {
            return new SimpleDateFormat(DATE_PATTERN).parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    private String getQueryUrl(String baseUrl, String query, int offset, int limit) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(baseUrl);
        queryBuilder.append("/w/api.php");
        queryBuilder.append("?action=query");
        queryBuilder.append("&list=search");
        queryBuilder.append("&format=json");
        if (offset > 0) {
            queryBuilder.append("&sroffset=").append(offset);
        }
        queryBuilder.append("&srlimit=").append(limit);
        queryBuilder.append("&srsearch=").append(UrlHelper.urlEncode(query));
        return queryBuilder.toString();
    }

    // build an article URL from an article title; unfortunately, I cannot find any rules from Wikipedia on how to
    // create a link from a article title. I implemented the rules described here:
    // http://stackoverflow.com/a/9354004/388827
    private String getPageUrl(String baseUrl, String pageTitle) {
        String pageUrl = StringHelper.upperCaseFirstLetter(pageTitle);
        pageUrl = pageUrl.replaceAll("\\s+$", "");
        pageUrl = pageUrl.replaceAll("\\s+", "_");
        return baseUrl + "/wiki/" + pageUrl;
    }

    private String getBaseUrl(Language language) {
        switch (language) {
            case GERMAN:
                return "http://de.wikipedia.org";
            default:
                return "http://en.wikipedia.org";
        }
    }

    public static void main(String[] args) throws SearcherException {
        WikipediaSearcher searcher = new WikipediaSearcher();
        List<WebResult> result = searcher.search("dresden", 500, Language.GERMAN);
        CollectionHelper.print(result);
        // System.out.println(searcher.getTotalResultCount("cat"));
    }

}
