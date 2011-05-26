package ws.palladian.extraction.keyphrase;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HTTPPoster;

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

        HttpPost postMethod = new HttpPost("http://access.alchemyapi.com/calls/text/TextGetRankedKeywords");

        // set input content type
        postMethod.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        // set response/output format
        postMethod.setHeader("Accept", "application/json");

        // create the content of the request
        try {
            NameValuePair[] data = { new BasicNameValuePair("text", inputText), new BasicNameValuePair("apikey", apiKey),
                    new BasicNameValuePair("outputMode", "json"),
                    new BasicNameValuePair("maxRetrieve", String.valueOf(getKeyphraseCount())),
                    new BasicNameValuePair("keywordExtractMode", strictExtractMode ? "strict" : "normal") };
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(Arrays.asList(data));
            postMethod.setEntity(entity);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e);
        }

        HTTPPoster poster = new HTTPPoster();
        String response = poster.handleRequest(postMethod);

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
