package ws.palladian.retrieval.ranking.services;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpMethod;
import ws.palladian.retrieval.HttpRequest2Builder;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.Ranking.Builder;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * RankingService implementation to get the number of bookmarks of a given URL on BibSonomy. At the moment it returns
 * number for all bookmarks containing the url or a longer version - e.g. www.google.com will give number for all
 * bookmarks containing www.google.com/...
 * </p>
 * <p>
 * No information about request limits.
 * </p>
 *
 * @author Julien Schmehl
 * @author Philipp Katz
 * @see http://www.bibsonomy.org
 */
public final class BibsonomyBookmarks extends AbstractRankingService implements RankingService {

    public static final class BibsonomyBookmarksMetaInfo implements RankingServiceMetaInfo<BibsonomyBookmarks> {
        private static final StringConfigurationOption LOGIN_OPTION = new StringConfigurationOption("Login", "login");
        private static final StringConfigurationOption API_KEY_OPTION = new StringConfigurationOption("API Key", "apikey");

        @Override
        public String getServiceName() {
            return "BibSonomy";
        }

        @Override
        public String getServiceId() {
            return SERVICE_ID;
        }

        @Override
        public List<ConfigurationOption<?>> getConfigurationOptions() {
            return Arrays.asList(LOGIN_OPTION, API_KEY_OPTION);
        }

        @Override
        public BibsonomyBookmarks create(Map<ConfigurationOption<?>, ?> config) {
            var login = LOGIN_OPTION.get(config);
            var apiKey = API_KEY_OPTION.get(config);
            return new BibsonomyBookmarks(login, apiKey);
        }
    }

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BibsonomyBookmarks.class);

    /** {@link Configuration} key for the API key. */
    public static final String CONFIG_API_KEY = "api.bibsonomy.key";

    /** {@link Configuration} key for the login. */
    public static final String CONFIG_LOGIN = "api.bibsonomy.login";

    /** The config values. */
    private final String login;
    private final String apiKey;

    /** The id of this service. */
    private static final String SERVICE_ID = "bibsonomy";

    /** The ranking value types of this service **/
    public static final RankingType<Integer> BOOKMARKS = new RankingType<Integer>("bibsonomy_bookmarks", "Bibsonomy Bookmarks", "The number of bookmarks users have created for this url.", Integer.class);

    /** All available ranking tpyes by {@link BibsonomyBookmarks}. */
    private static final List<RankingType<?>> RANKING_TYPES = Arrays.asList(BOOKMARKS);

    /**
     * <p>
     * Create a new {@link BibsonomyBookmarks} ranking service.
     * </p>
     *
     * @param configuration The configuration which must provide a login (<tt>api.bibsonomy.login</tt>)and an API key (
     *                      <tt>api.bibsonomy.key</tt>) for accessing the service.
     */
    public BibsonomyBookmarks(Configuration configuration) {
        this(configuration.getString(CONFIG_LOGIN), configuration.getString(CONFIG_API_KEY));
    }

    /**
     * <p>
     * Create a new {@link BibsonomyBookmarks} ranking service.
     * </p>
     *
     * @param login  The required login for accessing the service, not <code>null</code> or empty.
     * @param apiKey The required API key for accessing the service, not <code>null</code> or empty.
     */
    public BibsonomyBookmarks(String login, String apiKey) {
        Validate.notEmpty(login, "The required login is missing.");
        Validate.notEmpty(apiKey, "The required API key is missing.");
        this.login = login;
        this.apiKey = apiKey;
    }

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Builder builder = new Ranking.Builder(this, url);

        try {

            // authenticate via HTTP Auth and send GET request
            HttpRequest2Builder requestBuilder = new HttpRequest2Builder(HttpMethod.GET, "http://www.bibsonomy.org/api/posts?format=json&resourcetype=bookmark&start=0&end=1000");
            requestBuilder.setBasicAuth(login, apiKey);
            requestBuilder.addUrlParam("search", url);

            HttpResult getResult = retriever.execute(requestBuilder.create());
            String response = getResult.getStringContent();

            // create JSON-Object from response
            if (response.length() > 0) {
                JsonObject json = new JsonObject(response);
                int result = json.getJsonObject("posts").getInt("end");
                builder.add(BOOKMARKS, result);
                LOGGER.trace("Bibsonomy bookmarks for " + url + " : " + result);
            } else {
                builder.add(BOOKMARKS, null);
                LOGGER.trace("Bibsonomy bookmarks for " + url + " could not be fetched");
            }

        } catch (JsonException e) {
            throw new RankingServiceException(e);
        } catch (IOException e) {
            throw new RankingServiceException(e);
        }

        return builder.create();
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType<?>> getRankingTypes() {
        return RANKING_TYPES;
    }

    public static void main(String[] args) throws RankingServiceException {
        BibsonomyBookmarks ranking = new BibsonomyBookmarks("jumehl", "e954a3a053193c36283af8a760918302");
        ranking.getRanking("http://ard.de");
    }

}
