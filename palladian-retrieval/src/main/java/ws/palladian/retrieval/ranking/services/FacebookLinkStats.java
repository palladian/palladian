package ws.palladian.retrieval.ranking.services;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpMethod;
import ws.palladian.retrieval.HttpRequest2Builder;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.configuration.ConfigurationOption;
import ws.palladian.retrieval.configuration.StringConfigurationOption;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * RankingService implementation for likes, shares and comments on Facebook.
 * </p>
 *
 * @author Julien Schmehl
 * @author Philipp Katz
 * @author David Urbansky
 */
public final class FacebookLinkStats extends AbstractRankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookLinkStats.class);

    public static final class FacebookLinkStatsMetaInfo implements RankingServiceMetaInfo<FacebookLinkStats> {
        private static final StringConfigurationOption ACCESS_TOKEN_OPTION = new StringConfigurationOption("Access Token", "access_token");

        @Override
        public String getServiceName() {
            return "Facebook Link Stats";
        }

        @Override
        public String getServiceId() {
            return SERVICE_ID;
        }

        @Override
        public List<ConfigurationOption<?>> getConfigurationOptions() {
            return Arrays.asList(ACCESS_TOKEN_OPTION);
        }

        @Override
        public FacebookLinkStats create(Map<ConfigurationOption<?>, ?> config) {
            var apiKey = ACCESS_TOKEN_OPTION.get(config);
            return new FacebookLinkStats(apiKey);
        }

        @Override
        public String getServiceDocumentationUrl() {
            return "https://developers.facebook.com/docs/graph-api/";
        }

        @Override
        public String getServiceDescription() {
            return "Get the number of reactions, shares, and comments on Facebook.";
        }
    }

    /** The id of this service. */
    public static final String SERVICE_ID = "facebook";

    /** The ranking value types of this service **/
    public static final RankingType<Long> REACTIONS = new RankingType<>("facebook_reactions", "Facebook Reactions", null, Long.class);

    public static final RankingType<Long> SHARES = new RankingType<>("facebook_shares", "Facebook Shares", "The number of times users have shared the page on Facebook.", Long.class);

    public static final RankingType<Long> COMMENTS = new RankingType<>("facebook_comments", "Facebook Comments", "The number of comments users have made on the shared story.", Long.class);

    public static final RankingType<Long> COMMENTS_PLUGIN = new RankingType<>("facebook_comments_plugin", "Facebook Comments Plugin", null, Long.class);

    public static final RankingType<Long> ALL = new RankingType<>("facebook_all", "Facebook Reactions+Shares+Comments", "The sum of reactions, shares and comments on Facebook.", Long.class);

    /** All available ranking types by {@link FacebookLinkStats}. */
    private static final List<RankingType<?>> RANKING_TYPES = Arrays.asList(REACTIONS, SHARES, COMMENTS, COMMENTS_PLUGIN, ALL);

    /**
     * Facebook allows 600 calls per 600 seconds; see:
     * http://stackoverflow.com/questions/9272391/facebook-application-request-limit-reached
     */
    private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(600, TimeUnit.SECONDS, 550);

    /** Maximum number of URLs to fetch during each request. */
    private static final int BATCH_SIZE = 50;

    private static final String CONFIG_ACCESS_TOKEN = "api.facebook.app.token";
    private String accessToken;

    /**
     * <p>
     * Create a new {@link FacebookLinkStats} ranking service.
     * </p>
     *
     * @param configuration The configuration which must provide an access token <tt>api.facebook.app.token</tt>.
     */
    public FacebookLinkStats(Configuration configuration) {
        this(configuration.getString(CONFIG_ACCESS_TOKEN));
    }

    /**
     * <p>
     * Create a new {@link FacebookLinkStats} ranking service.
     * </p>
     *
     * @param accessToken The required access token, not <code>null</code> or empty.
     */
    public FacebookLinkStats(String accessToken) {
        Validate.notEmpty(accessToken, "The accessToken is missing.");
        this.accessToken = accessToken;
    }

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        return getRanking(Collections.singletonList(url)).values().iterator().next();
    }

    @Override
    public Map<String, Ranking> getRanking(Collection<String> urls) throws RankingServiceException {
        Map<String, Ranking> results = new HashMap<>();
        List<String> urlBatch = new ArrayList<>();
        for (String url : new HashSet<>(urls)) {
            urlBatch.add(url);
            if (urlBatch.size() >= BATCH_SIZE) {
                Map<String, Ranking> batchRanking = getRanking2(urlBatch);
                results.putAll(batchRanking);
                urlBatch.clear();
            }
        }
        if (urlBatch.size() > 0) {
            Map<String, Ranking> batchRanking = getRanking2(urlBatch);
            results.putAll(batchRanking);
        }
        return results;
    }

    private Map<String, Ranking> getRanking2(List<String> urls) throws RankingServiceException {
        Validate.isTrue(urls.size() <= BATCH_SIZE);
        THROTTLE.hold();
        Map<String, Ranking> results = new HashMap<>();
        HttpResult response;
        try {
            String requestUrl = "https://graph.facebook.com/v19.0/?fields=engagement&ids=";
            requestUrl += urls.stream().map(UrlHelper::encodeParameter).collect(Collectors.joining(","));
            requestUrl += "&access_token=" + accessToken;
            HttpRequest2Builder requestBuilder = new HttpRequest2Builder(HttpMethod.GET, requestUrl);
            response = retriever.execute(requestBuilder.create());
        } catch (HttpException e) {
            throw new RankingServiceException("HttpException " + e.getMessage(), e);
        }
        String content = response.getStringContent();
        LOGGER.debug("JSON response = {}", content);
        checkError(content);
        try {
            JsonObject json = new JsonObject(content);
            for (String url : urls) {
                JsonObject currentObject = json.getJsonObject(url);
                Ranking.Builder builder = new Ranking.Builder(this, url);
                Long reactions = currentObject.tryQueryLong("engagement/reaction_count");
                Long shares = currentObject.tryQueryLong("engagement/share_count");
                Long comments = currentObject.tryQueryLong("engagement/comment_count");
                Long commentsPlugin = currentObject.tryQueryLong("engagement/comment_plugin_count");
                builder.add(REACTIONS, reactions);
                builder.add(SHARES, shares);
                builder.add(COMMENTS, comments);
                builder.add(COMMENTS_PLUGIN, commentsPlugin);
                builder.add(ALL, reactions + shares + comments + commentsPlugin);
                Ranking result = builder.create();
                results.put(url, result);
                LOGGER.trace("Facebook link stats for {}: {}", url, result);
            }
        } catch (JsonException e) {
            throw new RankingServiceException("Error while parsing JSON response (" + content + ")", e);
        }
        return results;
    }

    /**
     * Check for error (see <a
     * href="https://developers.facebook.com/docs/graph-api/using-graph-api/v2.0#errors">here</a> for error codes).
     *
     * @param content The result content from the API invocation.
     * @throws RankingServiceException In case an error was returned in the JSON.
     */
    private void checkError(String content) throws RankingServiceException {
        try {
            JsonObject jsonObject = new JsonObject(content);
            Integer errorCode = jsonObject.tryGetInt("error_code");
            String errorMessage = jsonObject.tryGetString("error_msg");
            if (errorCode != null || errorMessage != null) {
                throw new RankingServiceException("Error from API: " + errorMessage + "(" + errorCode + ")");
            }
        } catch (JsonException e) {
            // ignore
        }
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType<?>> getRankingTypes() {
        return RANKING_TYPES;
    }

    public static void main(String[] args) throws RankingServiceException, JsonException, IOException {
        var accessToken = "...";
        FacebookLinkStats facebookLinkStats = new FacebookLinkStats(accessToken);
        List<String> urls = Arrays.asList("http://example.com", "https://wikipedia.org");
        CollectionHelper.print(facebookLinkStats.getRanking(urls));
    }
}
