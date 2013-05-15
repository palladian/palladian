package ws.palladian.classification.language;

import org.apache.commons.lang3.Validate;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.DocumentRetriever;

public class AlchemyLangDetect implements LanguageClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AlchemyLangDetect.class);

    private static final String API_URL = "http://access.alchemyapi.com/calls/text/TextGetLanguage?apikey=%s&text=%s&outputMode=json";

    /** The API key for the Alchemy API service. */
    private final String apiKey;

    private final DocumentRetriever documentRetriever;

    public AlchemyLangDetect(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.documentRetriever = new DocumentRetriever();
        this.apiKey = apiKey;
    }

    @Override
    public Language classify(String text) {
        JSONObject json = documentRetriever.getJsonObject(String.format(API_URL, apiKey,
                UrlHelper.encodeParameter(text)));
        try {
            return Language.getByIso6391(json.getString("iso-639-1"));
        } catch (JSONException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

}
