package tud.iir.classification.language;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import tud.iir.web.Crawler;

public class AlchemyLangDetect extends LanguageClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(GoogleLangDetect.class);

    /** The API key for the Alchemy API service. */
    private final String API_KEY;

    private Crawler crawler;

    public AlchemyLangDetect() {
        crawler = new Crawler();

        PropertiesConfiguration config = null;

        try {
            config = new PropertiesConfiguration("config/apikeys.conf");
        } catch (ConfigurationException e) {
            LOGGER.error("could not get api key from config/apikeys.conf, " + e.getMessage());
        }

        if (config != null) {
            API_KEY = config.getString("alchemy.api.key");
        } else {
            API_KEY = "";
        }
    }

    @Override
    public String classify(String text) {
        JSONObject json = crawler.getJSONDocument("http://access.alchemyapi.com/calls/text/TextGetLanguage?apikey="
                + API_KEY + "&text=" + text + "&outputMode=json");
        try {
            return json.getString("iso-639-1");
        } catch (JSONException e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return "";
    }

}
