package tud.iir.classification.controlledtagging;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tud.iir.web.HTTPPoster;

/**
 * 
 * http://www.alchemyapi.com/api/keyword/textc.html
 * 
 * @author Philipp Katz
 * 
 */
public class AlchemyKeywordExtraction {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(AlchemyKeywordExtraction.class);

    /** Alchemy API key. */
    private String apiKey = "";

    /** Number of keywords to retrieve. */
    private int maxRetrieve = 10;

    /** See {@link #setStrictExtractMode(boolean)}. */
    private boolean strictExtractMode = false;

    public AlchemyKeywordExtraction() {

        try {
            PropertiesConfiguration config = new PropertiesConfiguration("config/apikeys.conf");
            apiKey = config.getString("alchemy.api.key");
        } catch (ConfigurationException e) {
            LOGGER.error("could not get api key from config/apikeys.conf, " + e.getMessage());
        }

    }

    /**
     * TODO add return type.
     * TODO extract interface.
     * 
     * @param inputText
     */
    public void extract(String inputText) {

        PostMethod postMethod = new PostMethod("http://access.alchemyapi.com/calls/text/TextGetRankedKeywords");

        // set input content type
        postMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        // set response/output format
        postMethod.setRequestHeader("Accept", "application/json");

        // create the content of the request
        NameValuePair[] data = { 
                new NameValuePair("text", inputText), 
                new NameValuePair("apikey", apiKey),
                new NameValuePair("outputMode", "json"), 
                new NameValuePair("maxRetrieve", String.valueOf(maxRetrieve)),
                new NameValuePair("keywordExtractMode", strictExtractMode ? "strict" : "normal") };
        postMethod.setRequestBody(data);

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

                LOGGER.info("text:" + text + " relevance:" + relevance);

            }

        } catch (JSONException e) {
            LOGGER.error(e);
        }

    }
    
    /**
     * TODO extract interface.
     * 
     * @param maxRetrieve
     */
    public void setMaxRetrieve(int maxRetrieve) {
        this.maxRetrieve = maxRetrieve;
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

    public static void main(String[] args) {

        AlchemyKeywordExtraction extractor = new AlchemyKeywordExtraction();
        extractor
                .extract("The world's largest maker of solar inverters announced Monday that it will locate its first North American manufacturing plant in Denver. \"We see a huge market coming in the U.S.,\" said Pierre-Pascal Urbon, the company's chief financial officer. Solar inverters convert the direct current created by solar panels into an alternating current accessible to the larger electrical grid. The company, based in Kassel, north of Frankfurt, Germany, boasts growing sales of about $1.2 billion a year. \"We are creating economic opportunity,\" said Gov. Bill Ritter at a press conference. He added that creating core manufacturing jobs will help Colorado escape the recession sooner.");

    }

}
