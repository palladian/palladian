package tud.iir.classification.language;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tud.iir.web.Crawler;

public class GoogleLangDetect extends LanguageClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(GoogleLangDetect.class);

    /** The API key for the Alchemy API service. */
    private final String API_KEY;

    private Crawler crawler;

    public GoogleLangDetect() {
        crawler = new Crawler();

        PropertiesConfiguration config = null;

        try {
            config = new PropertiesConfiguration("config/apikeys.conf");
        } catch (ConfigurationException e) {
            LOGGER.error("could not get api key from config/apikeys.conf, " + e.getMessage());
        }

        if (config != null) {
            API_KEY = config.getString("google.api.key");
        } else {
            API_KEY = "";
        }
    }

    @Override
    public String classify(String text) {
        // String ipMin = "90.255.255.255";
        // System.out.println(MathHelper.ipToNumber(ipMin));
        // long ipLong = (long) (Math.random() * 4294967295l) + 1526726655l;
        // System.out.println("use ip " + MathHelper.numberToIp(ipLong));
        // JSONObject json =
        // crawler.getJSONDocument("http://ajax.googleapis.com/ajax/services/language/detect?v=1.0&q="+ text);
        JSONObject json = crawler.getJSONDocument("https://www.googleapis.com/language/translate/v2?key=" + API_KEY
                + "&target=de&q=" + text);

        try {
            JSONArray translations = json.getJSONObject("data").getJSONArray("translations");
            return ((JSONObject) translations.get(0)).getString("detectedSourceLanguage");
            // return json.getJSONObject("data").getJSONArray("translations")[0].getString("detectedSourceLanguage");
        } catch (JSONException e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return "";
    }

}