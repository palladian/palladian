package ws.palladian.classification.language;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.*;
import ws.palladian.retrieval.FormEncodedHttpEntity.Builder;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Language detector using Microsoft Translator API.
 * </p>
 *
 * @author Philipp Katz
 * @see <a href="http://msdn.microsoft.com/en-us/library/dd576287.aspx">Microsoft Translator API doc.</a>
 * @see <a href="https://datamarket.azure.com/dataset/1899a118-d202-492c-aa16-ba21c33c06cb">Azure Marketplace</a>
 * @see <a href="https://datamarket.azure.com/developer/applications/">Get client ID and secret here.</a>
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
     * @param clientId     The client ID from the Azure DataMarket, not <code>null</code> or empty.
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

        HttpRequest2Builder builder = new HttpRequest2Builder(HttpMethod.GET, "http://api.microsofttranslator.com/V2/Http.svc/Detect");
        builder.addUrlParam("text", shortenedText);
        builder.addHeader("Authorization", "Bearer " + accessToken);
        try {
            HttpResult result = httpRetriever.execute(builder.create());
            String langString = StringHelper.getSubstringBetween(result.getStringContent(), ">", "<");
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
        HttpRequest2Builder builder = new HttpRequest2Builder(HttpMethod.POST, "https://datamarket.accesscontrol.windows.net/v2/OAuth2-13");
        Builder entityBuilder = new FormEncodedHttpEntity.Builder();
        entityBuilder.addData("client_id", clientId);
        entityBuilder.addData("client_secret", clientSecret);
        entityBuilder.addData("scope", "http://api.microsofttranslator.com");
        entityBuilder.addData("grant_type", "client_credentials");
        builder.setEntity(entityBuilder.create());
        HttpResult result;
        try {
            result = httpRetriever.execute(builder.create());
        } catch (HttpException e) {
            throw new IllegalStateException("HTTP error while trying to obtain access token: " + e, e);
        }
        try {
            return new JsonObject(result.getStringContent()).getString("access_token");
        } catch (JsonException e) {
            throw new IllegalStateException("JSON parse error while trying to obtain access token: " + e + ", JSON was: '" + result.getStringContent() + "'", e);
        }
    }

}
