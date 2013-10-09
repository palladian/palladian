package ws.palladian.retrieval.search.web;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractSearcher;
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
public final class WikipediaSearcher extends AbstractSearcher<WebContent> {

    /** The name of this searcher. */
    private static final String NAME = "Wikipedia";

    /** Pattern used for parsing the returned date strings. */
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    
    private final HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<WebContent> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebContent> results = CollectionHelper.newArrayList();
        String baseUrl = getBaseUrl(language);

        // fetch in chunks of 50 items, this is maximum size
        for (int offset = 0; offset < resultCount; offset += 50) {

            JsonObject jsonResult = fetchJsonResponse(query, baseUrl, offset, 50);

            try {
                JsonArray searchResults = jsonResult.queryJsonArray("/query/search");

                if (searchResults.size() == 0) {
                    break; // no more results
                }

                for (Object result : searchResults) {
                    JsonObject resultItem = (JsonObject)result;
                    BasicWebContent.Builder builder = new BasicWebContent.Builder();
                    String title = resultItem.getString("title");
                    builder.setTitle(title);
                    builder.setSummary(HtmlHelper.stripHtmlTags(resultItem.getString("snippet")));
                    builder.setPublished(parseDate(resultItem.getString("timestamp")));
                    builder.setUrl(getPageUrl(baseUrl, title));
                    results.add(builder.create());

                    if (results.size() == resultCount) {
                        break;
                    }
                }

            } catch (Exception e) {
                throw new SearcherException("JSON parse error: " + e.getMessage(), e);
            }
        }

        return results;
    }

    private JsonObject fetchJsonResponse(String query, String baseUrl, int offset, int limit) throws SearcherException {
        String queryUrl = getQueryUrl(baseUrl, query, offset, limit);
        HttpResult httpResult;
        try {
            httpResult = retriever.httpGet(queryUrl);
        } catch (HttpException e) {
            throw new SearcherException("HTTP error while accessing \"" + queryUrl + "\": " + e.getMessage(), e);
        }
        String jsonString = httpResult.getStringContent();
        try {
            return new JsonObject(jsonString);
        } catch (JsonException e) {
            throw new SearcherException("JSON parse error while parsing \"" + jsonString + "\": " + e.getMessage(), e);
        }
    }

    @Override
    public long getTotalResultCount(String query, Language language) throws SearcherException {
        String baseUrl = getBaseUrl(language);
        JsonObject jsonResult = fetchJsonResponse(query, baseUrl, 0, 1);
        try {
            return jsonResult.queryLong("/query/searchinfo/totalhits");
        } catch (JsonException e) {
            throw new SearcherException("Error while getting the result count.");
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
        queryBuilder.append("&srsearch=").append(UrlHelper.encodeParameter(query));
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
        List<WebContent> result = searcher.search("dresden", 500, Language.GERMAN);
        CollectionHelper.print(result);
        // System.out.println(searcher.getTotalResultCount("cat"));
    }

}
