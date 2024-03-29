package ws.palladian.classification.language;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;

/**
 * <p>
 * The WebKnoxLangDetect wraps the PalladianLangDetect and offers the service over a REST API. See <a
 * href="http://webknox.com/api#!/text/language_GET">here</a>.
 * </p>
 *
 * @author David Urbansky
 */
public class WebKnoxLangDetect implements LanguageClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WebKnoxLangDetect.class);

    private static final String API_URL = "http://webknox.com/api/text/language?text=%s&apiKey=%s";

    private final String apiKey;

    private final HttpRetriever httpRetriever;

    public WebKnoxLangDetect(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
        this.httpRetriever = HttpRetrieverFactory.getHttpRetriever();
    }

    public WebKnoxLangDetect(Configuration configuration) {
        this(configuration.getString("api.webknox.apiKey"));
    }

    @Override
    public Language classify(String text) {
        try {
            String requestUrl = String.format(API_URL, UrlHelper.encodeParameter(text), apiKey);
            HttpResult httpResult = httpRetriever.httpGet(requestUrl);
            JsonArray result = new JsonArray(httpResult.getStringContent());
            return Language.getByIso6391(result.getJsonObject(0).getString("language"));
        } catch (JsonException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (HttpException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

}