package tud.iir.classification.language;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import tud.iir.web.Crawler;

public class GoogleLangDetect extends LanguageClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(GoogleLangDetect.class);

    private Crawler crawler;

    public GoogleLangDetect() {
        crawler = new Crawler();
    }

    @Override
    public String classify(String text) {
        JSONObject json = crawler.getJSONDocument("http://ajax.googleapis.com/ajax/services/language/detect?v=1.0&q="
                + text);
        try {
            return json.getJSONObject("responseData").getString("language");
        } catch (JSONException e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return "";
    }

}