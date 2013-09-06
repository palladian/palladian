package ws.palladian.extraction.pos;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * <p>
 * The WebKnoxPosTagger is equal to the PalladianPosTagger but is available through a REST API making the use of local
 * models unnecessary. See also here http://webknox.com/api#!/text/posTags_GET
 * </p>
 * 
 * @author David Urbansky
 */
public class WebKnoxPosTagger extends BasePosTagger {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WebKnoxPosTagger.class);

    /** The name of this POS tagger. */
    private static final String TAGGER_NAME = "WebKnox POS Tagger";

    private final String apiKey;

    public WebKnoxPosTagger(String appId, String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("The required API key is missing.");
        }
        this.apiKey = apiKey;
    }

    public WebKnoxPosTagger(Configuration configuration) {
        this(configuration.getString("api.webknox.appId"), configuration.getString("api.webknox.apiKey"));
    }

    @Override
    public String getName() {
        return TAGGER_NAME;
    }

    @Override
    public void tag(List<PositionAnnotation> annotations) {
        StringBuilder text = new StringBuilder();
        for (PositionAnnotation annotation : annotations) {
            text.append(annotation.getValue()).append(" ");
        }

        DocumentRetriever retriever = new DocumentRetriever();
        String url = "http://webknox.com/api/text/posTags?text=";
        url += UrlHelper.encodeParameter(text.toString().trim());
        url += "&apiKey=" + apiKey;
        JSONObject result = retriever.getJsonObject(url);

        String taggedText = "";
        try {
            taggedText = result.getString("taggedText");
        } catch (JSONException e) {
            LOGGER.error(e.getMessage());
        }

        //        TagAnnotations tagAnnotations = new TagAnnotations();

        String[] words = taggedText.split("\\s");
        int i = 0;
        for (String word : words) {
            String[] parts = word.split("/");

            String tag = parts[1].toUpperCase();
            annotations.get(i).getFeatureVector().add(new NominalFeature(PROVIDED_FEATURE, tag));
            i++;

            //            TagAnnotation tagAnnotation = new TagAnnotation(sentence.indexOf(parts[0]), tag,
            //                    parts[0]);
            //            tagAnnotations.add(tagAnnotation);
        }


    }

    public static void main(String[] args) {
        // WebKnoxPosTagger palladianPosTagger = new WebKnoxPosTagger(ConfigHolder.getInstance().getConfig());
        // System.out.println(palladianPosTagger.tag("The quick brown fox jumps over the lazy dog").getTaggedString());
    }

}
