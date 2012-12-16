package ws.palladian.retrieval.search.web;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.JPathHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.OAuthParams;
import ws.palladian.retrieval.OAuthUtil;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * WebSearcher for <a href="http://vimeo.com/">Vimeo</a>.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href="http://developer.vimeo.com/apis/advanced/methods/vimeo.videos.search">API documentation</a>
 */
public final class VimeoSearcher extends WebSearcher<WebVideoResult> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(VimeoSearcher.class);

    /** Constant for the name of this searcher. */
    private static final String SEARCHER_NAME = "Vimeo";

    /** Pattern for parsing the returned date strings. */
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** Authentication data. */
    private final OAuthParams oAuthParams;

    /**
     * Create a new {@link VimeoSearcher}.
     * 
     * @param oAuthParams The parameters for the OAuth-based authentication, not <code>null</code>
     */
    public VimeoSearcher(OAuthParams oAuthParams) {
        Validate.notNull(oAuthParams, "oAuthParams must not be null");
        this.oAuthParams = oAuthParams;
    }

    /**
     * <p>
     * Create a new {@link VimeoSearcher}.
     * </p>
     * 
     * @param consumerKey The OAuth consumer key, not <code>null</code> or empty.
     * @param consumerSecret The OAuth consumer secret, not <code>null</code> or empty.
     * @param accessToken The OAuth access token, not <code>null</code> or empty.
     * @param accessTokenSecret The OAuth access token secret, not <code>null</code> or empty.
     */
    public VimeoSearcher(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret) {
        this(new OAuthParams(consumerKey, consumerSecret, accessToken, accessTokenSecret));
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    private HttpRequest buildRequest(String query, int page, int resultCount) {
        HttpRequest request = new HttpRequest(HttpMethod.GET, "http://vimeo.com/api/rest/v2");
        request.addParameter("method", "vimeo.videos.search");
        request.addParameter("query", query);
        request.addParameter("full_response", "true");
        request.addParameter("format", "json");
        request.addParameter("page", page);
        request.addParameter("per_page", resultCount);
        return OAuthUtil.createSignedRequest(request, oAuthParams);
    }

    @Override
    public List<WebVideoResult> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebVideoResult> webResults = CollectionHelper.newArrayList();

        for (int page = 0; page < Math.ceil((double)resultCount / 50); page++) {
            int itemsToGet = Math.min(50, resultCount - page * 50);
            HttpRequest request = buildRequest(query, page, itemsToGet);
            LOGGER.debug("request = " + request);
            HttpResult httpResult;
            try {
                httpResult = retriever.execute(request);
            } catch (HttpException e) {
                throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName()
                        + " (request: " + request + "): " + e.getMessage(), e);
            }
            logRateLimits(httpResult);
            try {
                String jsonResult = HttpHelper.getStringContent(httpResult);
                List<WebVideoResult> parsedVideos = parseVideoResult(jsonResult);
                if (parsedVideos.isEmpty()) {
                    break;
                }
                webResults.addAll(parsedVideos);
            } catch (JSONException e) {
                throw new SearcherException("Exception parsing the JSON response while searching for \"" + query
                        + "\" with " + getName() + ": " + e.getMessage(), e);
            }
        }
        return webResults;
    }

    private static void logRateLimits(HttpResult httpResult) {
        int rateLimit = Integer.valueOf(httpResult.getHeaderString("X-RateLimit-Limit"));
        int rateLimitRemaining = Integer.valueOf(httpResult.getHeaderString("X-RateLimit-Remaining"));
        int rateLimitReset = Integer.valueOf(httpResult.getHeaderString("X-RateLimit-Reset"));
        LOGGER.debug("Rate limit: " + rateLimit + ", remaining: " + rateLimitRemaining + ", reset: " + rateLimitReset);
    }

    static List<WebVideoResult> parseVideoResult(String jsonString) throws JSONException {
        List<WebVideoResult> result = CollectionHelper.newArrayList();
        JSONArray jsonVideos = JPathHelper.get(jsonString, "videos/video", JSONArray.class);
        for (int i = 0; i < jsonVideos.length(); i++) {
            JSONObject jsonVideo = jsonVideos.getJSONObject(i);
            String title = JPathHelper.get(jsonVideo, "title", String.class);
            String description = JPathHelper.get(jsonVideo, "description", String.class);
            String uploadDateString = JPathHelper.get(jsonVideo, "upload_date", String.class);
            Date uploadDate = parseDate(uploadDateString);
            String id = JPathHelper.get(jsonVideo, "id", String.class);
            String url = String.format("https://vimeo.com/%s", id);
            long duration = JPathHelper.get(jsonVideo, "duration", Long.class);
            result.add(new WebVideoResult(url, null, title, description, duration, uploadDate));
        }
        return result;
    }

    @Override
    public int getTotalResultCount(String query, Language language) throws SearcherException {
        HttpRequest request = buildRequest(query, 0, 1);
        try {
            HttpResult result = retriever.execute(request);
            logRateLimits(result);
            return parseResultCount(HttpHelper.getStringContent(result));
        } catch (HttpException e) {
            throw new SearcherException("HTTP error (request: " + request + "): " + e.getMessage(), e);
        }
    }

    static int parseResultCount(String jsonString) {
        return JPathHelper.get(jsonString, "videos/total", Integer.class);
    }

    private static Date parseDate(String dateString) {
        DateFormat dateParser = new SimpleDateFormat(DATE_PATTERN);
        try {
            return dateParser.parse(dateString);
        } catch (ParseException e) {
            LOGGER.error("Error parsing date string '" + dateString + "'", e);
            return null;
        }
    }

}
