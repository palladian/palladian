package ws.palladian.retrieval.search.videos;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.*;
import ws.palladian.retrieval.configuration.ConfigurationOption;
import ws.palladian.retrieval.configuration.StringConfigurationOption;
import ws.palladian.retrieval.resources.BasicWebVideo;
import ws.palladian.retrieval.resources.WebVideo;
import ws.palladian.retrieval.search.*;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * WebSearcher for <a href="https://vimeo.com/">Vimeo</a>.
 *
 * @author Philipp Katz
 * @see <a href="https://developer.vimeo.com/api/guides/start">API documentation</a>
 */
public final class VimeoSearcher extends AbstractMultifacetSearcher<WebVideo> {
    public static final class VimeoSearcherMetaInfo implements SearcherMetaInfo<VimeoSearcher, WebVideo> {
        private static final StringConfigurationOption ACCESS_TOKEN_OPTION = new StringConfigurationOption(
                "Access Token", "accessToken");

        @Override
        public String getSearcherName() {
            return SEARCHER_NAME;
        }

        @Override
        public String getSearcherId() {
            return "vimeo";
        }

        @Override
        public Class<WebVideo> getResultType() {
            return WebVideo.class;
        }

        @Override
        public List<ConfigurationOption<?>> getConfigurationOptions() {
            return Arrays.asList(ACCESS_TOKEN_OPTION);
        }

        @Override
        public VimeoSearcher create(Map<ConfigurationOption<?>, ?> config) {
            var accessToken = ACCESS_TOKEN_OPTION.get(config);
            return new VimeoSearcher(accessToken);
        }

        @Override
        public String getSearcherDocumentationUrl() {
            return "https://developer.vimeo.com/api/guides/start";
        }

        @Override
        public String getSearcherDescription() {
            return "Search videos on <a href=\"https://vimeo.com/\">Vimeo</a>.";
        }
    }

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(VimeoSearcher.class);

    private static final int MAX_RESULTS_PER_PAGE = 100;

    /** Constant for the name of this searcher. */
    private static final String SEARCHER_NAME = "Vimeo";

    private final String accessToken;

    private final HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

    public VimeoSearcher(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    private HttpRequest2 buildRequest(MultifacetQuery query, int page, int resultCount) {
        HttpRequest2Builder builder = new HttpRequest2Builder(HttpMethod.GET, "https://api.vimeo.com/videos");
        builder.addUrlParam("query", query.getText());
        builder.addUrlParam("page", String.valueOf(page));
        builder.addUrlParam("per_page", String.valueOf(resultCount));
        // https://developer.vimeo.com/api/changelog
        builder.addHeader("Accept", "application/vnd.vimeo.*+json;version=3.4");
        builder.addHeader("Authorization", "Bearer " + accessToken);
        return builder.create();
    }

    @Override
    public SearchResults<WebVideo> search(MultifacetQuery query) throws SearcherException {

        if (StringUtils.isBlank(query.getText())) {
            throw new SearcherException("The query must specify a search string (other parameters are not supported).");
        }

        List<WebVideo> webResults = new ArrayList<>();
        int requestedResults = query.getResultCount();
        Long availableResults = null;
        for (int page = 1; page <= Math.ceil((double) requestedResults / MAX_RESULTS_PER_PAGE); page++) {
            int itemsToGet = Math.min(MAX_RESULTS_PER_PAGE, requestedResults - webResults.size());
            HttpRequest2 request = buildRequest(query, page, itemsToGet);
            LOGGER.debug("request = " + request);
            HttpResult httpResult;
            try {
                httpResult = retriever.execute(request);
            } catch (HttpException e) {
                throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName()
                        + " (request: " + request + "): " + e.getMessage(), e);
            }
            try {
                checkError(httpResult);
                checkRateLimits(httpResult);
                JsonObject json = new JsonObject(httpResult.getStringContent());
                availableResults = json.tryGetLong("total");
                List<WebVideo> parsedVideos = parseVideoResult(json);
                if (parsedVideos.isEmpty()) {
                    break;
                }
                webResults.addAll(parsedVideos);
            } catch (JsonException e) {
                throw new SearcherException("Exception parsing the JSON response while searching for \"" + query
                        + "\" with " + getName() + ": " + e.getMessage(), e);
            }
        }
        return new SearchResults<WebVideo>(webResults, availableResults);
    }

    private static void checkError(HttpResult httpResult) throws JsonException, SearcherException {
        var json = new JsonObject(httpResult.getStringContent());
        var err = json.tryGetString("error");
        if (err != null) {
            throw new SearcherException(err);
        }
    }

    private static void checkRateLimits(HttpResult httpResult) throws RateLimitedException {
        // http://developer.vimeo.com/guidelines/rate-limiting
        int rateLimit = Integer.parseInt(httpResult.getHeaderString("X-RateLimit-Limit"));
        int rateLimitRemaining = Integer.parseInt(httpResult.getHeaderString("X-RateLimit-Remaining"));
        int rateLimitReset = Math.round(parseDate(httpResult.getHeaderString("X-RateLimit-Reset")).getTime() / 1000);
        LOGGER.debug("Rate limit: " + rateLimit + ", remaining: " + rateLimitRemaining + ", reset: " + rateLimitReset);
        if (rateLimitRemaining == 0) {
            int timeUntilReset = rateLimitReset - (int) (System.currentTimeMillis() / 1000);
            throw new RateLimitedException("Rate limit exceeded, rate limit is " + rateLimit, timeUntilReset);
        }
    }

    public static List<WebVideo> parseVideoResult(JsonObject json) throws JsonException {
        List<WebVideo> result = new ArrayList<>();
        JsonArray jsonVideos = json.getJsonArray("data");
        for (int i = 0; i < jsonVideos.size(); i++) {
            JsonObject jsonVideo = jsonVideos.getJsonObject(i);
            BasicWebVideo.Builder builder = new BasicWebVideo.Builder();
            builder.setTitle(jsonVideo.getString("name"));
            builder.setSummary(jsonVideo.tryGetString("description"));
            builder.setPublished(parseDate(jsonVideo.getString("release_time")));
            builder.setUrl(jsonVideo.getString("link"));
            builder.setDuration(jsonVideo.getInt("duration"));
            if (jsonVideo.get("tags") != null) {
                JsonArray tagArray = jsonVideo.queryJsonArray("/tags");
                for (int j = 0; j < tagArray.size(); j++) {
                    var normalizedTag = tagArray.getJsonObject(j).getString("canonical");
                    builder.addTag(normalizedTag);
                }
            }
            builder.setSource(SEARCHER_NAME);
            builder.setIdentifier(jsonVideo.getString("resource_key"));
            result.add(builder.create());
        }
        return result;
    }

    private static Date parseDate(String dateString) {
        try {
            return Date.from(ZonedDateTime.parse(dateString).toInstant());
        } catch (DateTimeParseException e) {
            LOGGER.error("Error parsing date string '" + dateString + "'", e);
            return null;
        }
    }

    public static void main(String[] args) throws SearcherException {
        var accessToken = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        var vimeoSearcher = new VimeoSearcher(accessToken);
        var results = vimeoSearcher.search("olympic games", 200);
        CollectionHelper.print(results);
    }

}
