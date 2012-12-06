package ws.palladian.classification.language;

import java.io.IOException;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * <p>
 * The WebKnoxLangDetect wraps the PalladianLangDetect and offers the service over a REST API. See here
 * http://webknox.com/api#!/text/language_GET.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class WebKnoxLangDetect extends LanguageClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(WebKnoxLangDetect.class);

    private final String apiKey;

    public WebKnoxLangDetect(String apiKey) {
        this.apiKey = apiKey;
    }

    public WebKnoxLangDetect(Configuration configuration) {
        this.apiKey = configuration.getString("api.webknox.apiKey");
    }

    @Override
    public String classify(String text) {

        DocumentRetriever retriever = new DocumentRetriever();
        String url = "http://webknox.com/api/text/language?text=";
        url += UrlHelper.encodeParameter(text);
        url += "&apiKey=" + apiKey;
        JSONArray result = retriever.getJsonArray(url);

        String answer = "";
        try {
            answer = result.getJSONObject(0).getString("language");
        } catch (JSONException e) {
            LOGGER.error(e.getMessage());
        }

        return answer;
    }

    public static void main(String[] args) throws IOException {
        WebKnoxLangDetect webKnoxLangDetect = new WebKnoxLangDetect(ConfigHolder.getInstance().getConfig());
        System.out.println(webKnoxLangDetect.classify("Dies ist ein ganz deutscher Text, soviel ist klar"));
    }

}