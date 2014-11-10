package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.Ranking.Builder;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

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
    public static final RankingType FACEBOOK_COMMENTSBOX = new RankingType("sharedcount_facebook_commentsbox",
            "Facebook commentsbox count (SharedCount)");
    public static final RankingType FACEBOOK_CLICK = new RankingType("sharedcount_facebook_click",
            "Facebook click count (SharedCount)");
    public static final RankingType FACEBOOK_TOTAL = new RankingType("sharedcount_facebook_total",
            "Facebook total count (SharedCount)");
    public static final RankingType FACEBOOK_COMMENT = new RankingType("sharedcount_facebook_comment",
            "Facebook comment count (SharedCount)");
    public static final RankingType FACEBOOK_LIKE = new RankingType("sharedcount_facebook_like",
            "Facebook like count (SharedCount)");
    public static final RankingType FACEBOOK_SHARE = new RankingType("sharedcount_facebook_share",
            "Facebook share count (SharedCount)");
    public static final RankingType TWITTER = new RankingType("sharedcount_twitter", "Twitter (SharedCount)");
    public static final RankingType REDDIT = new RankingType("sharedcount_reddit", "Reddit (SharedCount)");
    public static final RankingType LINKEDIN = new RankingType("sharedcount_linkedin", "LinkedIn (SharedCount)");
    public static final RankingType DIGG = new RankingType("sharedcount_digg", "Digg (SharedCount)");
    public static final RankingType DELICIOUS = new RankingType("sharedcount_delicious", "Delicious (SharedCount)");
    public static final RankingType STUMBLEUPON = new RankingType("sharedcount_stumbleupon",
            "StumbleUpon (SharedCount)");
    public static final RankingType PINTEREST = new RankingType("sharedcount_pinterest", "Pinterest (SharedCount)");
    public static final RankingType GOOGLE_PLUS_ONE = new RankingType("sharedcount_googleplusone",
            "Google +1 (SharedCount)");
    public static final RankingType BUZZ = new RankingType("sharedcount_buzz", "Buzz (SharedCount)");

    /** All available ranking types by AlexaRank. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList( //
            FACEBOOK_COMMENTSBOX, //
            FACEBOOK_CLICK, //
            FACEBOOK_TOTAL, //
            FACEBOOK_COMMENT, //
            FACEBOOK_LIKE, //
            FACEBOOK_SHARE,//
            TWITTER, //
            REDDIT, //
            LINKEDIN, //
            DIGG, //
            DELICIOUS, //
            STUMBLEUPON, //
            PINTEREST, //
            GOOGLE_PLUS_ONE, //
            BUZZ);

    /** The endpoint to use without API key. */
    private static final String UNAUTHENTICATED_URL = "http://api.sharedcount.com/";

    /** The endpoint to use with free API key. */
    private static final String FREE_URL = "http://free.sharedcount.com/";

    private final String serviceUrl;

    private final String apiKey;

    /**
     * Create a new {@link SharedCount} which is accessing the unauthenticated endpoint.
     */
    public SharedCount() {
        this.apiKey = null;
        this.serviceUrl = UNAUTHENTICATED_URL;
    }

    /**
     * Create a new {@link SharedCount} which is accessing the authorized endpoint.
     * 
     * @param apiKey The API key, not <code>null</code> or empty (use #{@link SharedCount} for access without API key).
     */
    public SharedCount(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty (use zero-arg constructor for usage without API key.)");
        this.apiKey = apiKey;
        this.serviceUrl = FREE_URL;
    }

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Validate.notEmpty(url, "url must not be empty");
        HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, serviceUrl);
        httpRequest.addParameter("url", url);
        if (StringUtils.isNotBlank(apiKey)) {
            httpRequest.addParameter("apikey", apiKey);
        }
        retriever.setUserAgent("test");
        HttpResult httpResult;
        try {
            httpResult = retriever.execute(httpRequest);
            checkForError(httpResult);
        } catch (HttpException e) {
            throw new RankingServiceException(e);
        }
        Ranking.Builder builder = new Ranking.Builder(this, url);
        parseResult(httpResult, builder);
        return builder.create();
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
            builder.add(REDDIT, jsonResult.getInt("Reddit"));
            builder.add(FACEBOOK_COMMENTSBOX, jsonResult.queryInt("Facebook/commentsbox_count"));
            builder.add(FACEBOOK_CLICK, jsonResult.queryInt("Facebook/click_count"));
            builder.add(FACEBOOK_TOTAL, jsonResult.queryInt("Facebook/total_count"));
            builder.add(FACEBOOK_COMMENT, jsonResult.queryInt("Facebook/comment_count"));
            builder.add(FACEBOOK_LIKE, jsonResult.queryInt("Facebook/like_count"));
            builder.add(FACEBOOK_SHARE, jsonResult.queryInt("Facebook/share_count"));
            builder.add(DELICIOUS, jsonResult.getInt("Delicious"));
            builder.add(GOOGLE_PLUS_ONE, jsonResult.getInt("GooglePlusOne"));
            builder.add(BUZZ, jsonResult.getInt("Buzz"));
            builder.add(TWITTER, jsonResult.getInt("Twitter"));
            builder.add(DIGG, jsonResult.getInt("Diggs"));
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
        Ranking ranking = new SharedCount().getRanking(url);
        // Ranking ranking = new SharedCount("...").getRanking(url);
        CollectionHelper.print(ranking.getValues());
    }

}
