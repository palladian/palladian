package ws.palladian.extraction.keyphrase.extractors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import ws.palladian.extraction.keyphrase.Keyphrase;
import ws.palladian.extraction.keyphrase.KeyphraseExtractor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;

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
        
        String response = null;
        
        try {
            
            HttpRetriever retriever = new HttpRetriever();
            Map<String, String> params = new HashMap<String, String>();
            params.put("content", inputText);
            HttpResult postResult = retriever.httpPost("http://term-extraction.appspot.com/terms", params);
            
            response = new String(postResult.getContent());
            
        } catch (HttpException e) {
            LOGGER.error(e);
        }
        
        if (response != null) {

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
        List<Keyphrase> result = extractor
                .extract("The world's largest maker of solar inverters announced Monday that it will locate its first North American manufacturing plant in Denver. \"We see a huge market coming in the U.S.,\" said Pierre-Pascal Urbon, the company's chief financial officer. Solar inverters convert the direct current created by solar panels into an alternating current accessible to the larger electrical grid. The company, based in Kassel, north of Frankfurt, Germany, boasts growing sales of about $1.2 billion a year. \"We are creating economic opportunity,\" said Gov. Bill Ritter at a press conference. He added that creating core manufacturing jobs will help Colorado escape the recession sooner.");
        CollectionHelper.print(result);

    }

}
