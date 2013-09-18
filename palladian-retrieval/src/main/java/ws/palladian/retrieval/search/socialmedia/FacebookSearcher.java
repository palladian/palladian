package ws.palladian.retrieval.search.socialmedia;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
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
 * Search for public Facebook posts using <a href="http://www.facebook.com">Facebook</a>'s Graph API.
 * </p>
 * 
 * @see <a href="http://developers.facebook.com/docs/reference/api/">Facebook Graph API</a>
 * @author Philipp Katz
 */
public final class FacebookSearcher extends AbstractSearcher<WebContent> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookSearcher.class);

    /** The name of the WebSearcher. */
    private static final String SEARCHER_NAME = "Facebook";

    /** Pattern used for parsing the returned date strings. */
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ssZ";
    
    /** Key of the {@link Configuration} entry with the access token. */
    public static final String CONFIG_ACCESS_TOKEN = "api.facebook.accesstoken";

    /** Determine which results to return; URLs to exernal resources or URLs to posts within Facebook. */
    public static enum ResultType {
        /** Provide URLs as result, which link to the Facebook posts. To access them, authentication is necessary. */
        FACEBOOK_URLS, 
        /** Provide resolved URLs as result, that means, links to external content referenced in the Facebook posts. */
        RESOLVED_URLS
    }
    
    /** Type of result to return, see {@link ResultType}. */
    private final ResultType resultType;

    private final String accessToken;
    
    private final HttpRetriever retriever;

    /**
     * <p>
     * Create a new {@link FacebookSearcher}.
     * </p>
     * 
     * @param accessToken The (user scoped) access token for Facebook, not <code>null</code>.
     */
    public FacebookSearcher(String accessToken) {
        this(accessToken, ResultType.FACEBOOK_URLS);
    }
    
    /**
     * <p>
     * Create a new {@link FacebookSearcher}.
     * </p>
     * 
     * @param configuration The configuration which must provide an access token as {@value #CONFIG_ACCESS_TOKEN}, not
     *            <code>null</code>.
     */
    public FacebookSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_ACCESS_TOKEN));
    }

    /**
     * <p>
     * Create a new {@link FacebookSearcher}.
     * </p>
     * 
     * @param accessToken The (user scoped) access token for Facebook, not <code>null</code>.
     * @param resultType The type of the results to deliver.
     */
    public FacebookSearcher(String accessToken, ResultType resultType) {
        Validate.notEmpty(accessToken, "accessToken must not be empty");
        Validate.notNull(resultType, "resultType must not be null");
        this.resultType = resultType;
        this.accessToken = accessToken;
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    @Override
    public List<WebContent> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebContent> result = CollectionHelper.newArrayList();
        Set<String> urlDeduplication = CollectionHelper.newHashSet();
        
        // XXX paging is no longer supported
        if (resultCount > 100) {
            LOGGER.warn("Paging is no longer supported, returning 100 results max.");
        }
        int page = 0;

//        for (int page = 0; result.size() < resultCount; page++) {
            String requestUrl = buildRequestUrl(query, page);
            HttpResult httpResult;
            try {
                httpResult = retriever.httpGet(requestUrl);
            } catch (HttpException e) {
                throw new SearcherException("Encountered HTTP exception while accessing \"" + requestUrl + "\"", e);
            }
            String jsonString = httpResult.getStringContent();
            // LOGGER.debug(jsonString);

            try {
                JsonObject jsonResult = new JsonObject(jsonString);
                checkError(jsonResult);
                JsonArray jsonData = jsonResult.getJsonArray("data");
//                if (jsonData.size() == 0 || result.size() == resultCount) {
//                    break; // no more results
//                }

                for (int i = 0; i < jsonData.size(); i++) {
                    JsonObject jsonEntry = jsonData.getJsonObject(i);
                    
                    BasicWebContent webResult;
                    if (resultType == ResultType.RESOLVED_URLS) {
                        webResult = processUrls(jsonEntry);
                    } else {
                        webResult = processPosts(jsonEntry);
                    }
                    if (webResult == null) {
                        continue; // ignore entries without URLs for now.
                    }
                    if (!urlDeduplication.add(webResult.getUrl())) {
                        continue; // we already had this URL.
                    }
                    result.add(webResult);

                    if (result.size() == resultCount) {
                        break;
                    }
                }

            } catch (JsonException e) {
                throw new SearcherException("Error parsing the JSON response from \"" + requestUrl
                        + "\" (result was: \"" + jsonString + "\")", e);
            }
//        }
        return result;
    }

    private void checkError(JsonObject jsonObject) throws SearcherException, JsonException {
        if (jsonObject.containsKey("error")) {
            String type = jsonObject.queryString("error/type");
            String message = jsonObject.queryString("error/message");
            String errorMsg = String.format("Error from Facebook: %s, %s", type, message);
            throw new SearcherException(errorMsg);
        }
    }

    private BasicWebContent processPosts(JsonObject jsonEntry) {
        String id = jsonEntry.tryGetString("id");
        // http://stackoverflow.com/questions/4729477/what-is-the-url-to-a-facebook-open-graph-post
        String url = String.format("http://www.facebook.com/%s", id);
        String message = jsonEntry.tryGetString("message");
        // String description = JsonHelper.getString(jsonEntry, "description");
        Date date = parseDate(jsonEntry.tryGetString("created_time"));
        return new BasicWebContent(url, message, null, date);
    }

    private BasicWebContent processUrls(JsonObject jsonEntry) {
        String url = jsonEntry.tryGetString("link");
        if (url == null) {
            return null; // ignore entries without URLs for now.
        }
        String title = jsonEntry.tryGetString("name");
        String summary = jsonEntry.tryGetString("caption");
        Date date = parseDate(jsonEntry.tryGetString("created_time"));
        return new BasicWebContent(url, title, summary, date);
    }

    public String buildRequestUrl(String query, int page) {
        StringBuilder requestUrl = new StringBuilder();
        requestUrl.append("https://graph.facebook.com/search");
        requestUrl.append("?q=").append(UrlHelper.encodeParameter(query));
        // TODO further types would be possible, see API doc.
        requestUrl.append("&type=post");
        requestUrl.append("&limit=100");
//        if (page > 0) {
//            requestUrl.append("&offset=").append(page * 100);
//        }
        requestUrl.append("&access_token=").append(UrlHelper.encodeParameter(accessToken));
        System.out.println(requestUrl);
        return requestUrl.toString();
    }

    private Date parseDate(String string) {
        try {
            return new SimpleDateFormat(DATE_PATTERN).parse(string);
        } catch (Exception e) {
            LOGGER.warn("Error parsing date \"" + string + "\"");
            return null;
        }
    }

    public static void main(String[] args) throws SearcherException {
        FacebookSearcher searcher = new FacebookSearcher(
                "CAAFFWgvRbnUBALYRiSRM4PPPu6wVpgGyZAYdpXBUZC45nHfdheK9ZCn9uVWMAGMo4frZCW2jiC0t7GQg2rAkmk5XneAubKtvK1czfeiCm1bUg82PGZBZACoXjyfZCbY6qwVFwd7D7gGdZBPsIXLvMGGuEgnz9MveQe77HAEf5LuEc3dQUOOZBdQKl5XuxNvILy1paEemns0rbDAZDZD");
        List<WebContent> result = searcher.search("palladian", 200);
        CollectionHelper.print(result);
    }

}
