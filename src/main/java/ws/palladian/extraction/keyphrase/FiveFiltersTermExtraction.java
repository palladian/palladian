package ws.palladian.extraction.keyphrase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import ws.palladian.web.HTTPPoster;

public class FiveFiltersTermExtraction extends KeyphraseExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FiveFiltersTermExtraction.class);

    /**
     * 
     * @param inputText
     * @return 
     */
    @Override
    public List<Keyphrase> extract(String inputText) {
        
        List<Keyphrase> keyphrases = new ArrayList<Keyphrase>();

        PostMethod postMethod = new PostMethod("http://term-extraction.appspot.com/terms");

        // set input content type
        postMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        // set response/output format
        postMethod.setRequestHeader("Accept", "application/json");

        // create the content of the request
        NameValuePair[] data = { new NameValuePair("content", inputText), };
        postMethod.setRequestBody(data);

        HTTPPoster poster = new HTTPPoster();
        String response = poster.handleRequest(postMethod);

        // parse the JSON response
        try {

            JSONArray jsonArray = new JSONArray(response);
            for (int i = 0; i < jsonArray.length(); i++) {
                String text = jsonArray.getString(i);
                LOGGER.trace(text);
                keyphrases.add(new Keyphrase(text));
            }

        } catch (JSONException e) {
            LOGGER.error(e);
        }
        
        return keyphrases;

    }
    
    @Override
    public boolean needsTraining() {
        return false;
    }
    
    @Override
    public String getExtractorName() {
        return "Five Filters Term Extraction";
    }

    public static void main(String[] args) {

        FiveFiltersTermExtraction extractor = new FiveFiltersTermExtraction();
        extractor
                .extract("The world's largest maker of solar inverters announced Monday that it will locate its first North American manufacturing plant in Denver. \"We see a huge market coming in the U.S.,\" said Pierre-Pascal Urbon, the company's chief financial officer. Solar inverters convert the direct current created by solar panels into an alternating current accessible to the larger electrical grid. The company, based in Kassel, north of Frankfurt, Germany, boasts growing sales of about $1.2 billion a year. \"We are creating economic opportunity,\" said Gov. Bill Ritter at a press conference. He added that creating core manufacturing jobs will help Colorado escape the recession sooner.");

    }

}
