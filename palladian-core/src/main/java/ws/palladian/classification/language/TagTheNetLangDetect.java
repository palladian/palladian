package ws.palladian.classification.language;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.collection.MapBuilder;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;

/**
 * <p>
 * Language detection via tagthe.net
 * </p>
 * 
 * <a href="http://www.tagthe.net/">http://www.tagthe.net/</a>
 * <a href="http://www.tagthe.net/fordevelopers">http://www.tagthe.net/fordevelopers</a>
 * 
 * @author Philipp Katz
 * 
 */
public class TagTheNetLangDetect extends LanguageClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(TagTheNetLangDetect.class);

    /** tagthe.net provides no information, which languages it supports, so we record all detected ones here in the set. */
    private Set<String> detectedLanguages = new HashSet<String>();

    private final HttpRetriever httpRetriever;

    public TagTheNetLangDetect() {
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
    }

    @Override
    public String classify(String text) {

        String result = "";

        Map<String, String> headers = new MapBuilder<String, String>().add("Content-Type",
                "application/x-www-form-urlencoded; charset=UTF-8");

        Map<String, String> content = MapBuilder.createAdd("text", text).add("view", "json");

        try {
            HttpResult httpResult = httpRetriever.httpPost("http://tagthe.net/api", headers, content);
            String response = new String(httpResult.getContent(), Charset.forName("UTF-8"));
            JSONObject json = new JSONObject(response);

            String language = json.getJSONArray("memes").getJSONObject(0).getJSONObject("dimensions")
                    .getJSONArray("language").getString(0);

            result = mapLanguage(language);

            detectedLanguages.add(result);

        } catch (JSONException e) {
            LOGGER.error(e);
        } catch (HttpException e) {
            LOGGER.error(e);
        }

        return result;

    }

    public Set<String> getDetectedLanguages() {
        return detectedLanguages;
    }

    public static void main(String[] args) {
        LanguageClassifier lc = new TagTheNetLangDetect();
        for (int i = 0; i < 1000; i++) {
            System.out.println(lc.classify("olala, mademoiselle. c'est la vie."));
        }
    }

}
