package ws.palladian.preprocessing.nlp.pos;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.preprocessing.nlp.TagAnnotation;
import ws.palladian.preprocessing.nlp.TagAnnotations;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * <p>
 * The WebKnoxPosTagger is equal to the PalladianPosTagger but is available through a REST API making the use of local
 * models unnecessary. See also here http://webknox.com/api#!/text/posTags_GET
 * </p>
 * 
 * @author David Urbansky
 */
public class WebKnoxPosTagger extends PosTagger {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(WebKnoxPosTagger.class);

    private final String appId;
    private final String apiKey;

    public WebKnoxPosTagger(String appId, String apiKey) {
        this.appId = appId;
        this.apiKey = apiKey;
    }

    public WebKnoxPosTagger(Configuration configuration) {
        this.appId = configuration.getString("api.webknox.appId");
        this.apiKey = configuration.getString("api.webknox.apiKey");
    }

    @Override
    public PosTagger loadModel(String modelFilePath) {
        return null;
    }

    @Override
    public PosTagger tag(String sentence) {
        DocumentRetriever retriever = new DocumentRetriever();
        String url = "http://webknox.com/api/text/posTags?text=";
        try {
            url += URLEncoder.encode(sentence, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            LOGGER.error(e1.getMessage());
        }
        url += "&appId=" + appId;
        url += "&apiKey=" + apiKey;
        JSONObject result = retriever.getJsonObject(url);

        String taggedText = "";
        try {
            taggedText = result.getString("taggedText");
        } catch (JSONException e) {
            LOGGER.error(e.getMessage());
        }

        TagAnnotations tagAnnotations = new TagAnnotations();

        String[] words = taggedText.split("\\s");
        for (String word : words) {
            String[] parts = word.split("/");

            TagAnnotation tagAnnotation = new TagAnnotation(sentence.indexOf(parts[0]), parts[1].toUpperCase(),
                    parts[0]);
            tagAnnotations.add(tagAnnotation);
        }

        setTagAnnotations(tagAnnotations);

        return this;
    }

    @Override
    public PosTagger tag(String sentence, String modelFilePath) {
        return tag(sentence);
    }

    public static void main(String[] args) {
        WebKnoxPosTagger palladianPosTagger = new WebKnoxPosTagger(ConfigHolder.getInstance().getConfig());
        System.out.println(palladianPosTagger.tag("The quick brown fox jumps over the lazy dog").getTaggedString());
    }

}
