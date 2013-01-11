package ws.palladian.extraction.keyphrase.extractors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.keyphrase.Keyphrase;
import ws.palladian.extraction.keyphrase.KeyphraseExtractor;
import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;

/**
 * Keyword extraction based on OpenCalais' <a href=
 * "http://www.opencalais.com/documentation/opencalais-web-service-api/api-metadata-english/api-metadata-english-social-tags"
 * >SocialTags</a>. Description from the web page: <i>SocialTags is a new feature of OpenCalais that attempts to emulate
 * how a person would tag a specific piece of content. For example, if you submit a story about Barack Obama and a piece
 * of legislation – at least one reasonable tag would be "U.S. legislation". A story about the relative merits of BMWs,
 * Ferraris and Porsches should probably be tagged "sports cars" and "luxury makes" as well as "auto racing" and
 * "motorsport". SocialTags isn’t true semantic extraction -- but rather an attempt to provide common-sense tags for a
 * piece of content as a whole that you can then use for organizing and navigating your content. In conjunction with
 * Calais semantic extraction capabilities you have the best of both worlds: Semantic Tagging to extract predefined
 * structured information and Social Tagging to provide your users with another way to use your content.</i></p>
 * 
 * @author Philipp Katz
 */
public final class OpenCalaisSocialTagger extends KeyphraseExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenCalaisSocialTagger.class);

    /** OpenCalais API key. */
    private final String apiKey;

    /**
     * <p>
     * Create a new OpenCalais extractor for SocialTags.
     * </p>
     * 
     * @param apiKey The API key for accessing OpenCalais. Must not be <code>null</code> or empty.
     */
    public OpenCalaisSocialTagger(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("The required API key is missing.");
        }
        this.apiKey = apiKey;
    }

    @Override
    public List<Keyphrase> extract(String inputText) {

        List<Keyphrase> keyphrases = new ArrayList<Keyphrase>();

        Map<String, String> header = new HashMap<String, String>();

        // set mandatory parameters
        header.put("x-calais-licenseID", apiKey);

        // set input content type
        header.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        // set response/output format
        header.put("Accept", "application/json");

        // create the content of the request
        Map<String, String> content = new HashMap<String, String>();
        String paramsXML = "<c:params xmlns:c=\"http://s.opencalais.com/1/pred/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><c:processingDirectives c:contentType=\"text/raw\" c:outputFormat=\"application/json\" c:enableMetadataType=\"SocialTags\"></c:processingDirectives><c:userDirectives c:allowDistribution=\"true\" c:allowSearch=\"true\"></c:userDirectives></c:params>";
        content.put("content", inputText);
        content.put("paramsXML", paramsXML);

        String response = null;
        HttpRetriever retriever = new HttpRetriever();
        try {
            HttpResult postResult = retriever.httpPost("http://api.opencalais.com/tag/rs/enrich", header, content);
            response = new String(postResult.getContent());
        } catch (HttpException e) {
            LOGGER.error("HttpException while accessing OpenCalais API", e);
        }

        if (response != null) {
            try {
                JSONObject json = new JSONObject(response);
                @SuppressWarnings("unchecked")
                Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject jsonObj = json.getJSONObject(key);
                    if (jsonObj.has("_typeGroup") && jsonObj.get("_typeGroup").equals("socialTag")) {
                        String name = jsonObj.getString("name");
                        int importance = jsonObj.getInt("importance");
                        LOGGER.debug(name + " " + importance);
                        keyphrases.add(new Keyphrase(name, importance));
                    }
                }
            } catch (JSONException e) {
                LOGGER.error("JSONException while parsing the response", e);
            }
        }
        // FIXME limit to specified count
        return keyphrases;
    }

    @Override
    public String getExtractorName() {
        return "OpenCalais SocialTags";
    }

    @Override
    public boolean needsTraining() {
        return false;
    }

    public static void main(String[] args) {
        @SuppressWarnings("deprecation")
        OpenCalaisSocialTagger extractor = new OpenCalaisSocialTagger(ConfigHolder.getInstance().getConfig()
                .getString("api.opencalais.key"));
        extractor.setKeyphraseCount(5);
        List<Keyphrase> keywords = extractor
                .extract("The world's largest maker of solar inverters announced Monday that it will locate its first North American manufacturing plant in Denver. \"We see a huge market coming in the U.S.,\" said Pierre-Pascal Urbon, the company's chief financial officer. Solar inverters convert the direct current created by solar panels into an alternating current accessible to the larger electrical grid. The company, based in Kassel, north of Frankfurt, Germany, boasts growing sales of about $1.2 billion a year. \"We are creating economic opportunity,\" said Gov. Bill Ritter at a press conference. He added that creating core manufacturing jobs will help Colorado escape the recession sooner.");
        CollectionHelper.print(keywords);
    }

}
