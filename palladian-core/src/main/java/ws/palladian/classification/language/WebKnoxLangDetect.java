package ws.palladian.classification.language;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.DocumentRetriever;

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

    private final DocumentRetriever documentRetriever;

    public WebKnoxLangDetect(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
        this.documentRetriever = new DocumentRetriever();
    }

    public WebKnoxLangDetect(Configuration configuration) {
        this(configuration.getString("api.webknox.apiKey"));
    }

    @Override
    public Language classify(String text) {
        JSONArray result = documentRetriever.getJsonArray(String.format(API_URL, UrlHelper.encodeParameter(text),
                apiKey));
        try {
            return Language.getByIso6391(result.getJSONObject(0).getString("language"));
        } catch (JSONException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

}