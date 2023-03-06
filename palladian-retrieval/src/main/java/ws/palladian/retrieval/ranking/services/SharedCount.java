package ws.palladian.retrieval.ranking.services;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpMethod;
import ws.palladian.retrieval.HttpRequest2Builder;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.Ranking.Builder;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Get share counts via <a href="http://www.sharedcount.com">SharedCount</a>.
 * </p>
 *
 * @author Philipp Katz
 * @see <a href="http://www.sharedcount.com/documentation.php">API Information</a>
 */
public final class SharedCount extends AbstractRankingService {

    /** The id of this service. */
    private static final String SERVICE_ID = "sharedcount";

    /** The ranking value types of this service. */
    public static final RankingType FACEBOOK_COMMENTSBOX = new RankingType("sharedcount_facebook_commentsbox", "Facebook commentsbox count (SharedCount)");
    public static final RankingType FACEBOOK_CLICK = new RankingType("sharedcount_facebook_click", "Facebook click count (SharedCount)");
    public static final RankingType FACEBOOK_TOTAL = new RankingType("sharedcount_facebook_total", "Facebook total count (SharedCount)");
    public static final RankingType FACEBOOK_COMMENT = new RankingType("sharedcount_facebook_comment", "Facebook comment count (SharedCount)");
    public static final RankingType FACEBOOK_LIKE = new RankingType("sharedcount_facebook_like", "Facebook like count (SharedCount)");
    public static final RankingType FACEBOOK_SHARE = new RankingType("sharedcount_facebook_share", "Facebook share count (SharedCount)");
    /** @deprecated This is not available any longer. */
    @Deprecated
    public static final RankingType TWITTER = new RankingType("sharedcount_twitter", "Twitter (SharedCount)");
    public static final RankingType LINKEDIN = new RankingType("sharedcount_linkedin", "LinkedIn (SharedCount)");
    public static final RankingType STUMBLEUPON = new RankingType("sharedcount_stumbleupon", "StumbleUpon (SharedCount)");
    public static final RankingType PINTEREST = new RankingType("sharedcount_pinterest", "Pinterest (SharedCount)");
    public static final RankingType GOOGLE_PLUS_ONE = new RankingType("sharedcount_googleplusone", "Google +1 (SharedCount)");

    /** All available ranking types by AlexaRank. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList( //
            FACEBOOK_COMMENTSBOX, //
            FACEBOOK_CLICK, //
            FACEBOOK_TOTAL, //
            FACEBOOK_COMMENT, //
            FACEBOOK_LIKE, //
            FACEBOOK_SHARE,//
            LINKEDIN, //
            STUMBLEUPON, //
            PINTEREST, //
            GOOGLE_PLUS_ONE);

    /** The endpoint to use without API key. */
    public static final String FREE_URL = "https://free.sharedcount.com/";

    /** {@link Configuration} key for the service URL. */
    public static final String CONFIG_SERVICE_URL = "api.sharedcount.serviceUrl";

    /** {@link Configuration} key for the API key. */
    public static final String CONFIG_API_KEY = "api.sharedcount.apiKey";

    private final String serviceUrl;

    private final String apiKey;

    /**
     * Create a new {@link SharedCount}.
     *
     * @param configuration The configuration which must provide {@value #CONFIG_API_KEY} and
     *                      {@value #CONFIG_SERVICE_URL} (if <code>null</code>, the free endpoint {@value #FREE_URL} is assumed).
     */
    public SharedCount(Configuration configuration) {
        this(configuration.getString(CONFIG_SERVICE_URL, FREE_URL), configuration.getString(CONFIG_API_KEY));
    }

    /**
     * Create a new {@link SharedCount} which is accessing the free plan.
     *
     * @param The API key, not <code>null</code> or empty.
     */
    public SharedCount(String apiKey) {
        this.apiKey = apiKey;
        this.serviceUrl = FREE_URL;
    }

    /**
     * Create a new {@link SharedCount} which is accessing the specified service URL.
     *
     * @param serviceUrl The service URL to use, corresponding to the free/plus/business plan.
     * @param apiKey     The API key, not <code>null</code> or empty.
     */
    public SharedCount(String serviceUrl, String apiKey) {
        Validate.notEmpty(serviceUrl, "serviceUrl must not be empty");
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.serviceUrl = serviceUrl;
        this.apiKey = apiKey;
    }

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Validate.notEmpty(url, "url must not be empty");
        HttpRequest2Builder requestBuilder = new HttpRequest2Builder(HttpMethod.GET, serviceUrl);
        requestBuilder.addUrlParam("url", url);
        if (StringUtils.isNotBlank(apiKey)) {
            requestBuilder.addUrlParam("apikey", apiKey);
        }
        HttpResult httpResult;
        try {
            httpResult = retriever.execute(requestBuilder.create());
            checkForError(httpResult);
        } catch (HttpException e) {
            throw new RankingServiceException(e);
        }
        Ranking.Builder builder = new Ranking.Builder(this, url);
        return parseResult(httpResult, builder);
    }

    private static void checkForError(HttpResult httpResult) throws RankingServiceException {
        if (httpResult.errorStatus()) {
            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append("Received HTTP status ").append(httpResult.getStatusCode());
            String error = null;
            try {
                error = new JsonObject(httpResult.getStringContent()).getString("Error");
                errorMessage.append(": ").append(error);
            } catch (JsonException e) {
            }
            if (error == null) {
                errorMessage.append(httpResult.getStringContent());
            }
            errorMessage.append(" (URL='").append(httpResult.getUrl()).append("')");
            throw new RankingServiceException(errorMessage.toString());
        }
    }

    private static Ranking parseResult(HttpResult httpResult, Builder builder) throws RankingServiceException {
        String stringContent = httpResult.getStringContent();
        try {
            JsonObject jsonResult = new JsonObject(stringContent);
            builder.add(STUMBLEUPON, jsonResult.getInt("StumbleUpon"));
            builder.add(FACEBOOK_COMMENTSBOX, jsonResult.queryInt("Facebook/commentsbox_count"));
            builder.add(FACEBOOK_CLICK, jsonResult.queryInt("Facebook/click_count"));
            builder.add(FACEBOOK_TOTAL, jsonResult.queryInt("Facebook/total_count"));
            builder.add(FACEBOOK_COMMENT, jsonResult.queryInt("Facebook/comment_count"));
            builder.add(FACEBOOK_LIKE, jsonResult.queryInt("Facebook/like_count"));
            builder.add(FACEBOOK_SHARE, jsonResult.queryInt("Facebook/share_count"));
            builder.add(GOOGLE_PLUS_ONE, jsonResult.getInt("GooglePlusOne"));
            builder.add(TWITTER, 0);
            builder.add(PINTEREST, jsonResult.getInt("Pinterest"));
            builder.add(LINKEDIN, jsonResult.getInt("LinkedIn"));
            return builder.create();
        } catch (JsonException e) {
            throw new RankingServiceException("JSON exception while trying to parse " + stringContent, e);
        }
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return RANKING_TYPES;
    }

    public static void main(String[] args) throws RankingServiceException {
        String url = "http://arstechnica.com/information-technology/2014/04/taking-e-mail-back-part-4-the-finale-with-webmail-everything-after/";
        Ranking ranking = new SharedCount("...").getRanking(url);
        CollectionHelper.print(ranking.getValues());
    }

}
