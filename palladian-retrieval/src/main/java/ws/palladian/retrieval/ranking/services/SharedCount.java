package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    public static final RankingType SHARED_COUNT_FACEBOOK = new RankingType("sharedcount_facebook",
            "Facebook (SharedCount)", "");
    public static final RankingType SHARED_COUNT_TWITTER = new RankingType("sharedcount_twitter",
            "Twitter (SharedCount)", "");
    public static final RankingType SHARED_COUNT_REDDIT = new RankingType("sharedcount_reddit", "Reddit (SharedCount)",
            "");
    public static final RankingType SHARED_COUNT_LINKEDIN = new RankingType("sharedcount_linkedin",
            "LinkedIn (SharedCount)", "");
    public static final RankingType SHARED_COUNT_DIGG = new RankingType("sharedcount_digg", "Digg (SharedCount)", "");
    public static final RankingType SHARED_COUNT_DELICIOUS = new RankingType("sharedcount_delicious",
            "Delicious (SharedCount)", "");
    public static final RankingType SHARED_COUNT_STUMBLEUPON = new RankingType("sharedcount_stumbleupon",
            "StumbleUpon (SharedCount)", "");
    public static final RankingType SHARED_COUNT_PINTEREST = new RankingType("sharedcount_pinterest",
            "Pinterest (SharedCount)", "");
    public static final RankingType SHARED_COUNT_GOOGLE_PLUS_ONE = new RankingType("sharedcount_googleplusone",
            "Google +1 (SharedCount)", "");

    /** All available ranking types by AlexaRank. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList( //
            SHARED_COUNT_FACEBOOK, //
            SHARED_COUNT_TWITTER, //
            SHARED_COUNT_REDDIT, //
            SHARED_COUNT_LINKEDIN, //
            SHARED_COUNT_DIGG, //
            SHARED_COUNT_DELICIOUS, //
            SHARED_COUNT_STUMBLEUPON, //
            SHARED_COUNT_PINTEREST, //
            SHARED_COUNT_GOOGLE_PLUS_ONE //
            );

    private static final String SERVICE_URL = "http://free.sharedcount.com/";

    private final String apiKey;

    public SharedCount(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Validate.notEmpty(url, "url must not be empty");
        HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, SERVICE_URL);
        httpRequest.addParameter("url", url);
        if (StringUtils.isNotBlank(apiKey)) {
            httpRequest.addParameter("apikey", apiKey);
        }
        retriever.setUserAgent("test");
        HttpResult httpResult;
        try {
            httpResult = retriever.execute(httpRequest);
            if (httpResult.errorStatus()) {
                throw new RankingServiceException("Received HTTP status " + httpResult.getStatusCode() + " for '" + httpResult.getUrl() + "'.");
            }
        } catch (HttpException e) {
            throw new RankingServiceException(e);
        }
        return new Ranking(this, url, parseResult(httpResult));
    }

    private static Map<RankingType, Float> parseResult(HttpResult httpResult) throws RankingServiceException {
        String stringContent = httpResult.getStringContent();
        try {
            JsonObject jsonResult = new JsonObject(stringContent);
            // {"StumbleUpon":603192,"Reddit":0,"Facebook":{"commentsbox_count":15,"click_count":27523,"total_count":335835,"comment_count":100278,"like_count":84709,"share_count":150848},"Delicious":11969,"GooglePlusOne":76352,"Buzz":0,"Twitter":22199,"Diggs":0,"Pinterest":16081,"LinkedIn":1532}
            Map<RankingType, Float> results = CollectionHelper.newHashMap();
            results.put(SHARED_COUNT_STUMBLEUPON, (float)jsonResult.getInt("StumbleUpon"));
            results.put(SHARED_COUNT_REDDIT, (float)jsonResult.getInt("Reddit"));
            // TODO facebook
            results.put(SHARED_COUNT_DELICIOUS, (float)jsonResult.getInt("Delicious"));
            results.put(SHARED_COUNT_GOOGLE_PLUS_ONE, (float)jsonResult.getInt("GooglePlusOne"));
            // TODO buzz
            results.put(SHARED_COUNT_TWITTER, (float)jsonResult.getInt("Twitter"));
            results.put(SHARED_COUNT_DIGG, (float)jsonResult.getInt("Diggs"));
            results.put(SHARED_COUNT_PINTEREST, (float)jsonResult.getInt("Pinterest"));
            results.put(SHARED_COUNT_LINKEDIN, (float)jsonResult.getInt("LinkedIn"));
            return results;
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
        Ranking ranking = new SharedCount("a8f11e90419d4eddd7a0423c25d6ebbd74b3cf70").getRanking("http://arstechnica.com/information-technology/2014/04/taking-e-mail-back-part-4-the-finale-with-webmail-everything-after/");
        System.out.println(ranking);
    }

}
