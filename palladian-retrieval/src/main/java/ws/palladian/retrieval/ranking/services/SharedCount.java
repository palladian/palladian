package ws.palladian.retrieval.ranking.services;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpMethod;
import ws.palladian.retrieval.HttpRequest2Builder;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.Ranking.Builder;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Get share counts via <a href="https://www.sharedcount.com">SharedCount</a>.
 * </p>
 *
 * @author Philipp Katz
 * @see <a href="https://www.sharedcount.com/api-docs/getting-started">API Information</a>
 */
public final class SharedCount extends AbstractRankingService {

    /** The id of this service. */
    private static final String SERVICE_ID = "sharedcount";

    /** The ranking value types of this service. */
    public static final RankingType FACEBOOK_TOTAL = new RankingType("sharedcount_facebook_total", "Facebook total count (SharedCount)");
    public static final RankingType FACEBOOK_COMMENT = new RankingType("sharedcount_facebook_comment", "Facebook comment count (SharedCount)");
    public static final RankingType FACEBOOK_SHARE = new RankingType("sharedcount_facebook_share", "Facebook share count (SharedCount)");
    public static final RankingType FACEBOOK_REACTION = new RankingType("sharedcount_facebook_reaction", "Facebook reaction count (SharedCount)");
    public static final RankingType FACEBOOK_COMMENT_PLUGIN = new RankingType("sharedcount_facebook_comment_plugin", "Facebook comment plugin count (SharedCount)");
    public static final RankingType PINTEREST = new RankingType("sharedcount_pinterest", "Pinterest (SharedCount)");

    /** All available ranking types by AlexaRank. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList( //
            FACEBOOK_TOTAL, //
            FACEBOOK_COMMENT, //
            FACEBOOK_SHARE, //
            FACEBOOK_REACTION, //
            FACEBOOK_COMMENT_PLUGIN, //
            PINTEREST //
    );

    /** {@link Configuration} key for the API key. */
    public static final String CONFIG_API_KEY = "api.sharedcount.apiKey";

    private final String apiKey;

    /**
     * Create a new {@link SharedCount}.
     *
     * @param configuration The configuration which must provide {@value #CONFIG_API_KEY}.
     */
    public SharedCount(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    /**
     * Create a new {@link SharedCount}.
     *
     * @param The API key, not <code>null</code> or empty.
     */
    public SharedCount(String apiKey) {
        this.apiKey = apiKey;
    }


    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Validate.notEmpty(url, "url must not be empty");
        HttpRequest2Builder requestBuilder = new HttpRequest2Builder(HttpMethod.GET, "https://api.sharedcount.com/v1.1");
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
            builder.add(FACEBOOK_TOTAL, jsonResult.queryInt("Facebook/total_count"));
            // builder.add(FACEBOOK_OG_OBJECT, jsonResult.queryInt("Facebook/og_object"));
            builder.add(FACEBOOK_COMMENT, jsonResult.queryInt("Facebook/comment_count"));
            builder.add(FACEBOOK_SHARE, jsonResult.queryInt("Facebook/share_count"));
            builder.add(FACEBOOK_REACTION, jsonResult.queryInt("Facebook/reaction_count"));
            builder.add(FACEBOOK_COMMENT_PLUGIN, jsonResult.queryInt("Facebook/comment_plugin_count"));
            builder.add(PINTEREST, jsonResult.getInt("Pinterest"));
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
