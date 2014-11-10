package ws.palladian.extraction.pos;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * <p>
 * The WebKnoxPosTagger is equal to the PalladianPosTagger but is available through a REST API making the use of local
 * models unnecessary. See also here http://webknox.com/api#!/text/posTags_GET
 * </p>
 * 
 * @author David Urbansky
 */
public class WebKnoxPosTagger extends AbstractPosTagger {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WebKnoxPosTagger.class);

    /** The name of this POS tagger. */
    private static final String TAGGER_NAME = "WebKnox POS Tagger";

    private final String apiKey;
    
    private final HttpRetriever retriever;

    public WebKnoxPosTagger(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("The required API key is missing.");
        }
        this.apiKey = apiKey;
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
    }

    public WebKnoxPosTagger(Configuration configuration) {
        this(configuration.getString("api.webknox.apiKey"));
    }

    @Override
    public String getName() {
        return TAGGER_NAME;
    }

    @Override
    protected List<String> getTags(List<String> tokens) {
        StringBuilder text = new StringBuilder();
        for (String token : tokens) {
            text.append(token).append(" ");
        }

        String taggedText = "";
        try {
            String url = String.format("http://webknox.com/api/text/posTags?text=%s&apiKey=%s",UrlHelper.encodeParameter(text.toString().trim()),apiKey);  
            HttpResult httpResult = retriever.httpGet(url);
            JsonObject result = new JsonObject(httpResult.getStringContent());
            taggedText = result.getString("taggedText");
        } catch (JsonException e) {
            LOGGER.error(e.getMessage());
        } catch (HttpException e) {
            LOGGER.error(e.getMessage());
        }

        String[] words = taggedText.split("\\s");
        List<String> tags = CollectionHelper.newArrayList();
        for (String word : words) {
            String[] parts = word.split("/");
            String tag = parts[1].toUpperCase();
            tags.add(tag);
        }
        return tags;
    }

    public static void main(String[] args) {
        // WebKnoxPosTagger palladianPosTagger = new WebKnoxPosTagger(ConfigHolder.getInstance().getConfig());
        // System.out.println(palladianPosTagger.tag("The quick brown fox jumps over the lazy dog").getTaggedString());
    }

}
