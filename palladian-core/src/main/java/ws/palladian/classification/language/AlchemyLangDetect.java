package ws.palladian.classification.language;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

public class AlchemyLangDetect implements LanguageClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AlchemyLangDetect.class);

    private static final String API_URL = "http://access.alchemyapi.com/calls/text/TextGetLanguage?apikey=%s&text=%s&outputMode=json";

    /** The API key for the Alchemy API service. */
    private final String apiKey;

    private final HttpRetriever httpRetriever;

    public AlchemyLangDetect(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        this.apiKey = apiKey;
    }

    @Override
    public Language classify(String text) {
        try {
            String requestUrl = String.format(API_URL, apiKey, UrlHelper.encodeParameter(text));
            HttpResult httpResult = httpRetriever.httpGet(requestUrl);
            JsonObject json = new JsonObject(httpResult.getStringContent());
            return Language.getByIso6391(json.getString("iso-639-1"));
        } catch (JsonException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

}
