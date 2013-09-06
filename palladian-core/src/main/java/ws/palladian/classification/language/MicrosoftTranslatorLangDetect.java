package ws.palladian.classification.language;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.constants.Language;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;

/**
 * <p>
 * Language detector using Microsoft Translator API.
 * </p>
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/dd576287.aspx">Microsoft Translator API doc.</a>
 * @see <a href="https://datamarket.azure.com/dataset/1899a118-d202-492c-aa16-ba21c33c06cb">Azure Marketplace</a>
 * @see <a href="https://datamarket.azure.com/developer/applications/">Get client ID and secret here.</a>
 * @author Philipp Katz
 */
public class MicrosoftTranslatorLangDetect implements LanguageClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MicrosoftTranslatorLangDetect.class);

    /** Duration, how long an obtained access token is valid (10 Minutes, take 9 to be on the safe side). */
    private static final long ACCESS_TOKEN_VALIDITY_MS = TimeUnit.MINUTES.toMillis(9);

    /** For performing HTTP requests. */
    private final HttpRetriever httpRetriever;

    /** The client ID from the Azure DataMarket, necessary for retrieving an access token. */
    private final String clientId;

    /** The client secret from the Azure DataMarket, necessary for retrieving an access token. */
    private final String clientSecret;

    /** The access token for each request. */
    private String accessToken;

    /** The timestamp, when the current access token was obtained. */
    private long accessTokenTimestamp;

    /**
     * <p>
     * Create a new {@link MicrosoftTranslatorLangDetect} with the given authentication data.
     * </p>
     * 
     * @param clientId The client ID from the Azure DataMarket, not <code>null</code> or empty.
     * @param clientSecret The client secret from the Azure DataMarket, not <code>null</code> or empty.
     */
    public MicrosoftTranslatorLangDetect(String clientId, String clientSecret) {
        Validate.notEmpty(clientId, "clientId must not be empty");
        Validate.notEmpty(clientSecret, "clientSecret must not be empty");
        this.httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public Language classify(String text) {
        Validate.notNull(text, "text must not be null");

        conditionallyObtainAccessToken();

        // text must be no longer than 10.000 characters
        String shortenedText = text;
        if (text.length() > 10000) {
            shortenedText = text.substring(0, 9999);
            LOGGER.debug("Text was longer than 10000 characters, and was shortened.");
        }

        HttpRequest request = new HttpRequest(HttpMethod.GET, "http://api.microsofttranslator.com/V2/Http.svc/Detect");
        request.addParameter("text", shortenedText);
        request.addHeader("Authorization", "Bearer " + accessToken);
        try {
            HttpResult result = httpRetriever.execute(request);
            String langString = StringHelper.getSubstringBetween(HttpHelper.getStringContent(result), ">", "<");
            return Language.getByIso6391(langString);
        } catch (HttpException e) {
            throw new IllegalStateException("HTTP error while classifying language: " + e);
        }
    }

    /**
     * Check, if we need an access token. Access tokens are valid for 10 minutes, after that time, a new one has to be
     * requested.
     */
    private void conditionallyObtainAccessToken() {
        long current = System.currentTimeMillis();
        if (accessToken == null || current - accessTokenTimestamp > ACCESS_TOKEN_VALIDITY_MS) {
            accessToken = obtainAccessToken();
            accessTokenTimestamp = current;
            LOGGER.debug("Got (new) access token.");
        } else {
            LOGGER.debug("Access token still valid.");
        }
    }

    /**
     * Request an access token, see <a href="http://msdn.microsoft.com/en-us/library/hh454950.aspx">here</a>.
     * 
     * @return The access token.
     * @throws IllegalStateException in case of any error.
     */
    private String obtainAccessToken() {
        HttpRequest request = new HttpRequest(HttpMethod.POST,
                "https://datamarket.accesscontrol.windows.net/v2/OAuth2-13");
        request.addParameter("client_id", clientId);
        request.addParameter("client_secret", clientSecret);
        request.addParameter("scope", "http://api.microsofttranslator.com");
        request.addParameter("grant_type", "client_credentials");
        HttpResult result;
        try {
            result = httpRetriever.execute(request);
        } catch (HttpException e) {
            throw new IllegalStateException("HTTP error while trying to obtain access token: " + e, e);
        }
        String jsonString = HttpHelper.getStringContent(result);
        try {
            return new JSONObject(jsonString).getString("access_token");
        } catch (JSONException e) {
            throw new IllegalStateException("JSON parse error while trying to obtain access token: " + e
                    + ", JSON was: '" + jsonString + "'", e);
        }
    }

}
