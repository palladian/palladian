package ws.palladian.retrieval.search.news;

import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.JPathHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.MashapeUtil;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Search news on <a href="http://newsseecr.com">NewsSeecr</a>
 * </p>
 * 
 * @see <a href="https://www.mashape.com/qqilihq/newsseecr">API documentation on Mashape</a>
 * @see <a href="http://blog.mashape.com/important-changes-to-mashape-authorization-ke">Information on new
 *      authentication mechanism</a>
 * @author Philipp Katz
 */
public final class NewsSeecrSearcher extends AbstractSearcher<WebContent> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NewsSeecrSearcher.class);

    private static final String SEARCHER_NAME = "NewsSeecr";

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private static final String BASE_URL = "https://qqilihq-newsseecr.p.mashape.com/news/search";

    private static final int RESULTS_PER_REQUEST = 100;

    private final String mashapePublicKey;

    private final String mashapePrivateKey;

    private final String mashapeKey;
    
    private final HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

    /** Configuraiton key for the Mashape public key. */
    @Deprecated
    public static final String CONFIG_MASHAPE_PUBLIC_KEY = "api.newsseecr.mashapePublicKey";
    /** Configuration key for the Mashape private key. */
    @Deprecated
    public static final String CONFIG_MASHAPE_PRIVATE_KEY = "api.newsseecr.mashapePrivateKey";
    /** Configuration key for the Mashape key. */
    public static final String CONFIG_MASHAPE_KEY = "api.newsseecr.mashapeKey";

    /**
     * <p>
     * Create a new {@link NewsSeecrSearcher} with the provided credentials from Mashape.
     * </p>
     * 
     * @param mashapePublicKey The Mashape public key, not empty or <code>null</code>.
     * @param mashapePrivateKey The Mashape private key, not empty or <code>null</code>.
     * @deprecated Prefer using the {@link #NewsSeecrSearcher(String)} and supply the new Mashape key. See <a
     *             href="http://blog.mashape.com/important-changes-to-mashape-authorization-ke">here</a> for more
     *             information.
     */
    @Deprecated
    public NewsSeecrSearcher(String mashapePublicKey, String mashapePrivateKey) {
        Validate.notEmpty(mashapePublicKey, "mashapePublicKey must not be empty");
        Validate.notEmpty(mashapePrivateKey, "mashapePrivateKey must not be empty");
        this.mashapePublicKey = mashapePublicKey;
        this.mashapePrivateKey = mashapePrivateKey;
        this.mashapeKey = null;
    }

    /**
     * <p>
     * Create a new {@link NewsSeecrSearcher} with the provided Mashape key.
     * </p>
     * 
     * @param mashapeKey The Mashape key, not empty or <code>null</code>.
     */
    public NewsSeecrSearcher(String mashapeKey) {
        Validate.notEmpty(mashapeKey, "mashapeKey must not be empty");
        this.mashapeKey = mashapeKey;
        this.mashapePublicKey = null;
        this.mashapePrivateKey = null;
    }

    /**
     * <p>
     * Create a new {@link NewsSeecrSearcher} with the provided crendentials from Mashape supplied via a
     * {@link Configuration}.
     * </p>
     * 
     * @param configuration The configuration supplying the Mashape key as {@value #CONFIG_MASHAPE_KEY}. (Old,
     *            deprecated authentication scheme is also accepted with public key as
     *            {@value #CONFIG_MASHAPE_PUBLIC_KEY}, private key as {@value #CONFIG_MASHAPE_PRIVATE_KEY}). Not
     *            <code>null</code>.
     */
    public NewsSeecrSearcher(Configuration configuration) {
        String mashapeKey = configuration.getString(CONFIG_MASHAPE_KEY);
        String publicKey = configuration.getString(CONFIG_MASHAPE_PUBLIC_KEY);
        String privateKey = configuration.getString(CONFIG_MASHAPE_PRIVATE_KEY);
        if (StringUtils.isNotEmpty(mashapeKey)) {
            this.mashapeKey = mashapeKey;
            this.mashapePublicKey = null;
            this.mashapePrivateKey = null;
        } else if (StringUtils.isNotEmpty(publicKey) && StringUtils.isNotEmpty(privateKey)) {
            this.mashapeKey = null;
            this.mashapePublicKey = publicKey;
            this.mashapePrivateKey = privateKey;
        } else {
            throw new IllegalArgumentException(
                    "The authentication must either be supplied as one Mashape key, or as public/private key combination (old scheme).");
        }
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<WebContent> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebContent> webResults = CollectionHelper.newArrayList();

        for (int offset = 0; offset < Math.ceil((double)resultCount / RESULTS_PER_REQUEST); offset++) {

            HttpRequest request = new HttpRequest(HttpMethod.GET, BASE_URL);
            request.addParameter("query", query);
            request.addParameter("page", offset);
            request.addParameter("numResults", Math.min(resultCount, RESULTS_PER_REQUEST));

            if (mashapePrivateKey != null && mashapePublicKey != null) {
                LOGGER.debug("Use old authentication scheme with private/public key");
                MashapeUtil.signRequest(request, mashapePublicKey, mashapePrivateKey);
            } else {
                LOGGER.debug("Use now authentication scheme");
                request.addHeader("X-Mashape-Authorization", mashapeKey);
            }

            LOGGER.debug("Performing request: " + request);
            HttpResult result;
            try {
                result = retriever.execute(request);
            } catch (HttpException e) {
                throw new SearcherException("Encountered HTTP error when executing the request: " + request + ": "
                        + e.getMessage(), e);
            }
            if (result.getStatusCode() != 200) {
				// TODO get message
				throw new SearcherException("Encountered HTTP status "
						+ result.getStatusCode()
						+ " when executing the request: " + request
						+ ", result: " + result.getStringContent());
            }

            String jsonString = result.getStringContent();
            LOGGER.debug("JSON result: " + jsonString);

            try {
                JSONArray resultArray = JPathHelper.get(jsonString, "/results", JSONArray.class);
                for (int i = 0; i < resultArray.length(); i++) {
                    JSONObject resultObject = resultArray.getJSONObject(i);
                    String title = JPathHelper.get(resultObject, "/title", String.class);
                    String dateString = JPathHelper.get(resultObject, "/publishedDate", String.class);
                    String link = JPathHelper.get(resultObject, "/link", String.class);
                    String text = JPathHelper.get(resultObject, "/text", String.class);
                    Date date = parseDate(dateString);
                    webResults.add(new BasicWebContent(link, title, text, date));
                    if (webResults.size() == resultCount) {
                        break;
                    }
                }
            } catch (Exception e) {
                throw new SearcherException("Error while parsing the JSON response (" + jsonString + "): "
                        + e.getMessage(), e);
            }
        }

        return webResults;
    }

    public static Date parseDate(String dateString) {
        DateFormat dateParser = new SimpleDateFormat(DATE_FORMAT);
        try {
            return dateParser.parse(dateString);
        } catch (ParseException e) {
            LOGGER.warn("Error parsing date " + dateString);
            return null;
        }
    }

    public static void main(String[] args) throws SearcherException, GeneralSecurityException {
        // old, deprecated:
        // String publicKey = "u3ewnlzvxvbg3gochzqcrulimgngsb";
        // String privateKey = "dxkyimj8rjoyti1mqx2lqragbbg71k";
        // NewsSeecrSearcher searcher = new NewsSeecrSearcher(publicKey, privateKey);

        // new:
        String mashapeKey = "...";
        NewsSeecrSearcher searcher = new NewsSeecrSearcher(mashapeKey);
        List<WebContent> results = searcher.search("obama", 250);
        CollectionHelper.print(results);
    }

}
