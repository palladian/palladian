package ws.palladian.classification.language;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.retrieval.DocumentRetriever;

/**
 * <p>
 * The Google Language Detection using the tranlation API.
 * </p>
 * <p>
 * The language detector can not be trained, it supports a wide variety of 53 languages:
 * <ol>
 * <li>Afrikaans (af)</li>
 * <li>Albanian (sq)</li>
 * <li>Arabic (ar)</li>
 * <li>Basque (eu)</li>
 * <li>Belarusian (be)</li>
 * <li>Bulgarian (bg)</li>
 * <li>Catalan (ca)</li>
 * <li>Chinese Simplified (zh-CN)</li>
 * <li>Chinese Traditional (zh-TW)</li>
 * <li>Croatian (hr)</li>
 * <li>Czech (cs)</li>
 * <li>Danish (da)</li>
 * <li>Dutch (nl)</li>
 * <li>English ( n)</li>
 * <li>Estonian (et)</li>
 * <li>Filipino (tl)</li>
 * <li>Finnish (fi)</li>
 * <li>French (fr)</li>
 * <li>Galician (gl)</li>
 * <li>German (de)</li>
 * <li>Greek (el)</li>
 * <li>Haitian Creole (ht)</li>
 * <li>Hebrew (iw)</li>
 * <li>Hindi (hi)</li>
 * <li>Hungarian (hu)</li>
 * <li>Icelandic (is)</li>
 * <li>Indonesian (id)</li>
 * <li>Irish (ga)</li>
 * <li>Italian (it)</li>
 * <li>Japanese (ja)</li>
 * <li>Latvian (lv)</li>
 * <li>Lithuanian (lt)</li>
 * <li>Macedonian (mk)</li>
 * <li>Malay (ms)</li>
 * <li>Maltese (mt)</li>
 * <li>Norwegian (no)</li>
 * <li>Persian (fa)</li>
 * <li>Polish (pl)</li>
 * <li>Portuguese (pt)</li>
 * <li>Romanian (ro)</li>
 * <li>Russian (ru)</li>
 * <li>Serbian (sr)</li>
 * <li>Slovak (sk)</li>
 * <li>Slovenian (sl)</li>
 * <li>Spanish (es)</li>
 * <li>Swahili (sw)</li>
 * <li>Swedish (sv)</li>
 * <li>Thai (th)</li>
 * <li>Turkish (tr)</li>
 * <li>Ukrainian (uk)</li>
 * <li>Vietnamese(vi)</li>
 * <li>Welsh (cy)</li>
 * <li>Yiddish (yi)</li>
 * </ol>
 * 
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class GoogleLangDetect extends LanguageClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleLangDetect.class);

    /** The API key for the Alchemy API service. */
    private final String API_KEY;

    private DocumentRetriever crawler;

    public GoogleLangDetect() {
        crawler = new DocumentRetriever();

        PropertiesConfiguration config = null;

        try {
            config = new PropertiesConfiguration("config/apikeys.conf");
        } catch (ConfigurationException e) {
            LOGGER.error("could not get api key from config/apikeys.conf, " + e.getMessage());
        }

        if (config != null) {
            API_KEY = config.getString("google.tranlsate.api.key");
        } else {
            API_KEY = "";
        }
    }

    @Override
    public String classify(String text) {

        JSONObject json = crawler.getJsonObject("https://www.googleapis.com/language/translate/v2?key=" + API_KEY
                + "&target=de&q=" + text);

        try {
            JSONArray translations = json.getJSONObject("data").getJSONArray("translations");
            return ((JSONObject) translations.get(0)).getString("detectedSourceLanguage");
        } catch (JSONException e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return "";
    }

}