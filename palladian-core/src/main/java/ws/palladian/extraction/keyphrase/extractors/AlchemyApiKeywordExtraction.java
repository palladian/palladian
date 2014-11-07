package ws.palladian.extraction.keyphrase.extractors;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.keyphrase.Keyphrase;
import ws.palladian.extraction.keyphrase.KeyphraseExtractor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * <p>
 * Keyword/term extraction based on <a href="http://www.alchemyapi.com/api/keyword/textc.html">AlchemyAPI</a>. The
 * following description is directly from the web page: <i>AlchemyAPI provides easy-to-use facilities for extracting
 * topic keywords from any textual content […]. Posted content is analyzed to detect the primary document language, and
 * topic keywords are extracted automatically.</i>
 * </p>
 * 
 * @author Philipp Katz
 */
public final class AlchemyApiKeywordExtraction extends KeyphraseExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AlchemyApiKeywordExtraction.class);

    /** The API key for AlchemyAPI. */
    private final String apiKey;

    /** Indicate whether to use strict or normal extraction mode. */
    private final boolean strictExtractMode;

    /**
     * <p>
     * Creates a new AlchemyAPI Keyword/Term Extraction instance.
     * </p>
     * 
     * @param apiKey The API key for accessing AlchemyAPI. Must not be <code>null</code> or empty.
     * @param strictExtractMode <code>true</code> to use <i>strict keyword extract mode</i>, which returns more
     *            "well-formed" keywords by refining the result, <code>false</code> for normal extraction mode.
     */
    public AlchemyApiKeywordExtraction(String apiKey, boolean strictExtractMode) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("The required API key is missing.");
        }
        this.apiKey = apiKey;
        this.strictExtractMode = strictExtractMode;
    }

    /**
     * <p>
     * Creates a new AlchemyAPI Keyword/Term Extraction instance.
     * </p>
     * 
     * @param apiKey The API key for accessing AlchemyAPI. Must not be <code>null</code> or empty.
     */
    public AlchemyApiKeywordExtraction(String apiKey) {
        this(apiKey, false);
    }

    @Override
    public List<Keyphrase> extract(String inputText) {

        List<Keyphrase> keyphrases = new ArrayList<Keyphrase>();

        HttpRequest request = new HttpRequest(HttpMethod.POST,
                "http://access.alchemyapi.com/calls/text/TextGetRankedKeywords");

        // set input content type
        request.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        // set response/output format
        request.addHeader("Accept", "application/json");

        // create the content of the request
        request.addParameter("text", inputText);
        request.addParameter("apikey", apiKey);
        request.addParameter("outputMode", "json");
        request.addParameter("maxRetrieve", String.valueOf(getKeyphraseCount()));
        request.addParameter("keywordExtractMode", strictExtractMode ? "strict" : "normal");

        HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
        String response = null;
        try {
            HttpResult postResult = retriever.execute(request);
            response = new String(postResult.getContent());
        } catch (HttpException e) {
            LOGGER.error("HttpException while accessing Alchemy API", e);
        }

        if (response != null) {

            // parse the JSON response
            try {

                JsonObject json = new JsonObject(response);

                JsonArray jsonKeywords = json.getJsonArray("keywords");
                for (int i = 0; i < jsonKeywords.size(); i++) {
                    JsonObject jsonObject = jsonKeywords.getJsonObject(i);

                    String text = jsonObject.getString("text");
                    double relevance = jsonObject.getDouble("relevance");

                    LOGGER.trace("text:" + text + " relevance:" + relevance);
                    keyphrases.add(new Keyphrase(text, relevance));

                }

            } catch (JsonException e) {
                LOGGER.error("JSONException while parsing the response", e);
            }
        }

        return keyphrases;

    }

    @Override
    public String getExtractorName() {
        return "Alchemy API; Keyword/Term Extraction";
    }

    @Override
    public boolean needsTraining() {
        return false;
    }

    public static void main(String[] args) {
        AlchemyApiKeywordExtraction extractor = new AlchemyApiKeywordExtraction("");
        String text = "The world's largest maker of solar inverters announced Monday that it will locate its first North American manufacturing plant in Denver. \"We see a huge market coming in the U.S.,\" said Pierre-Pascal Urbon, the company's chief financial officer. Solar inverters convert the direct current created by solar panels into an alternating current accessible to the larger electrical grid. The company, based in Kassel, north of Frankfurt, Germany, boasts growing sales of about $1.2 billion a year. \"We are creating economic opportunity,\" said Gov. Bill Ritter at a press conference. He added that creating core manufacturing jobs will help Colorado escape the recession sooner.";
        List<Keyphrase> keywords = extractor.extract(text);
        CollectionHelper.print(keywords);
    }

}
