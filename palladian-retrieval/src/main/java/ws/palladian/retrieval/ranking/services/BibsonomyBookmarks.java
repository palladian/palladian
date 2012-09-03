package ws.palladian.retrieval.ranking.services;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

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
public final class BibsonomyBookmarks extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(BibsonomyBookmarks.class);

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
    public static final RankingType BOOKMARKS = new RankingType("bibsonomy_bookmarks", "Bibsonomy Bookmarks",
            "The number of bookmarks users have created for this url.");

    /** All available ranking tpyes by {@link BibsonomyBookmarks}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(BOOKMARKS);

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000 * 60 * 1;

    /**
     * <p>
     * Create a new {@link BibsonomyBookmarks} ranking service.
     * </p>
     * 
     * @param configuration The configuration which must provide a login (<tt>api.bibsonomy.login</tt>)and an API key (
     *            <tt>api.bibsonomy.key</tt>) for accessing the service.
     */
    public BibsonomyBookmarks(Configuration configuration) {
        this(configuration.getString(CONFIG_LOGIN), configuration.getString(CONFIG_API_KEY));
    }

    /**
     * <p>
     * Create a new {@link BibsonomyBookmarks} ranking service.
     * </p>
     * 
     * @param login The required login for accessing the service.
     * @param apiKey The required API key for accessing the service.
     */
    public BibsonomyBookmarks(String login, String apiKey) {
        super();
        if (login == null || login.isEmpty()) {
            throw new IllegalStateException("The required login is missing.");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("The required API key is missing.");
        }
        this.login = login;
        this.apiKey = apiKey;
    }

    @Override
    public Ranking getRanking(String url) {
        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);
        if (isBlocked()) {
            return ranking;
        }

        try {

            String encUrl = UrlHelper.urlEncode(url);
            // authenticate via HTTP Auth and send GET request
            String pass = getLogin() + ":" + getApiKey();
            Map<String, String> headerParams = new HashMap<String, String>();
            headerParams.put("Authorization", "Basic " + StringHelper.encodeBase64(pass));
            HttpResult getResult = retriever.httpGet(
                    "http://www.bibsonomy.org/api/posts?format=json&resourcetype=bookmark&start=0&end=999999&search="
                            + encUrl, headerParams);
            String response = HttpHelper.getStringContent(getResult);

            // create JSON-Object from response
            JSONObject json = null;
            if (response.length() > 0) {
                json = new JSONObject(response);
            }

            if (json != null) {
                float result = json.getJSONObject("posts").getInt("end");
                results.put(BOOKMARKS, result);
                LOGGER.trace("Bibsonomy bookmarks for " + url + " : " + result);
            } else {
                results.put(BOOKMARKS, null);
                LOGGER.trace("Bibsonomy bookmarks for " + url + "could not be fetched");
            }

        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
            checkBlocked();
        } catch (IOException e) {
            LOGGER.error("IOException " + e.getMessage());
            checkBlocked();
        }

        return ranking;
    }

    @Override
    public boolean checkBlocked() {
        int status = -1;
        try {
            // authenticate via HTTP Auth and send GET request
            if (getLogin() == null || getApiKey() == null) {
                throw new IllegalStateException("login or api key is missing.");
            }
            String pass = getLogin() + ":" + getApiKey();
            Map<String, String> headerParams = new HashMap<String, String>();
            headerParams.put("Authorization", "Basic " + StringHelper.encodeBase64(pass));
            HttpResult getResult;
            getResult = retriever
                    .httpGet(
                            "http://www.bibsonomy.org/api/posts?format=json&resourcetype=bookmark&start=0&end=999999&search=http://www.google.com/",
                            headerParams);
            status = getResult.getStatusCode();
        } catch (HttpException e) {
            LOGGER.error("HttpException " + e.getMessage());
        }
        if (status == 200) {
            blocked = false;
            lastCheckBlocked = new Date().getTime();
            return false;
        }
        blocked = true;
        lastCheckBlocked = new Date().getTime();
        LOGGER.error("Bibsonomy Ranking Service is momentarily blocked. Will check again in 1min.");
        return true;
    }

    @Override
    public boolean isBlocked() {
        if (new Date().getTime() - lastCheckBlocked < checkBlockedIntervall) {
            return blocked;
        } else {
            return checkBlocked();
        }
    }

    @Override
    public void resetBlocked() {
        blocked = false;
        lastCheckBlocked = new Date().getTime();
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return RANKING_TYPES;
    }

    public String getLogin() {
        return login;
    }

    public String getApiKey() {
        return apiKey;
    }
    
    public static void main(String[] args) {
        BibsonomyBookmarks ranking = new BibsonomyBookmarks("jumehl", "e954a3a053193c36283af8a760918302");
        ranking.getRanking("http://ard.de");
    }

}
