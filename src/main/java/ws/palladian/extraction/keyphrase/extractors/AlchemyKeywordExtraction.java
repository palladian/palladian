package ws.palladian.extraction.keyphrase.extractors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.extraction.keyphrase.Keyphrase;
import ws.palladian.extraction.keyphrase.KeyphraseExtractor;
import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;

/**
 * 
 * http://www.alchemyapi.com/api/keyword/textc.html
 * 
 * @author Philipp Katz
 * 
 */
public class AlchemyKeywordExtraction extends KeyphraseExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(AlchemyKeywordExtraction.class);

    public static void main(String[] args) {

        AlchemyKeywordExtraction extractor = new AlchemyKeywordExtraction();
        List<Keyphrase> keywords = extractor
                .extract("The world's largest maker of solar inverters announced Monday that it will locate its first North American manufacturing plant in Denver. \"We see a huge market coming in the U.S.,\" said Pierre-Pascal Urbon, the company's chief financial officer. Solar inverters convert the direct current created by solar panels into an alternating current accessible to the larger electrical grid. The company, based in Kassel, north of Frankfurt, Germany, boasts growing sales of about $1.2 billion a year. \"We are creating economic opportunity,\" said Gov. Bill Ritter at a press conference. He added that creating core manufacturing jobs will help Colorado escape the recession sooner.");
        CollectionHelper.print(keywords);

    }

    /** Alchemy API key. */
    private String apiKey = "";

    /** See {@link #setStrictExtractMode(boolean)}. */
    private boolean strictExtractMode = false;

    public AlchemyKeywordExtraction() {
        final PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
        apiKey = config.getString("api.alchemy.key");
    }

    /**
     * 
     * @param inputText
     * @return
     */
    @Override
    public List<Keyphrase> extract(String inputText) {

        List<Keyphrase> keyphrases = new ArrayList<Keyphrase>();
        
        Map<String, String> headers = new HashMap<String, String>();

        // set input content type
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        // set response/output format
        headers.put("Accept", "application/json");

        // create the content of the request
        Map<String, String> content = new HashMap<String, String>();
        content.put("text", inputText);
        content.put("apikey", apiKey);
        content.put("outputMode", "json");
        content.put("maxRetrieve", String.valueOf(getKeyphraseCount()));
        content.put("keywordExtractMode", strictExtractMode ? "strict" : "normal");

        DocumentRetriever retriever = new DocumentRetriever();
        String response = null;
        try {
            HttpResult postResult = retriever.httpPost("http://access.alchemyapi.com/calls/text/TextGetRankedKeywords", headers, content);
            response = new String(postResult.getContent());
        } catch (HttpException e) {
            LOGGER.error(e);
        }
        
        if (response != null) {

            // parse the JSON response
            try {

                JSONObject json = new JSONObject(response);

                JSONArray jsonKeywords = json.getJSONArray("keywords");
                for (int i = 0; i < jsonKeywords.length(); i++) {
                    JSONObject jsonObject = jsonKeywords.getJSONObject(i);

                    String text = jsonObject.getString("text");
                    double relevance = jsonObject.getDouble("relevance");

                    LOGGER.trace("text:" + text + " relevance:" + relevance);
                    keyphrases.add(new Keyphrase(text, relevance));

                }

            } catch (JSONException e) {
                LOGGER.error(e);
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

    /**
     * keyword extraction mode (normal or strict)
     * normal - normal keyword extraction mode (default)
     * strict - strict keyword extraction mode (returns more "well-formed" keywords). refines results at the expense of
     * returning fewer keywords.
     * 
     * @param strictExtractMode <code>false</code> for normal mode, <code>true</code> for strict mode
     */
    public void setStrictExtractMode(boolean strictExtractMode) {
        this.strictExtractMode = strictExtractMode;
    }

}
