package ws.palladian.extraction.keyphrase.extractors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.keyphrase.Keyphrase;
import ws.palladian.extraction.keyphrase.KeyphraseExtractor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;

/**
 * 
 * http://developer.yahoo.com/yql/console/?q=select%20*%20from%20search.termextract%20where%20context%3D%22Italian%20sculptors%20and%20painters%20of%20the%20renaissance%20favored%20the%20Virgin%20Mary%20for%20inspiration%22
 * 
 * @author Philipp Katz
 *
 */
public final class YahooTermExtraction extends KeyphraseExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(YahooTermExtraction.class);

    @Override
    public List<Keyphrase> extract(String inputText) {
        
        List<Keyphrase> keyphrases = new ArrayList<Keyphrase>();

        // headers for the request
        Map<String, String> headers = new HashMap<String, String>();
        // set input content type
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        // set response/output format
        headers.put("Accept", "application/json");

        // create the YQL query string
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("select * from search.termextract ");
        queryBuilder.append("where context=\"");
        queryBuilder.append(inputText.replace("\"", "\\\""));
        queryBuilder.append("\"");

        // create the content of the request
        Map<String, String> content = new HashMap<String, String>();
        content.put("q", queryBuilder.toString());
        content.put("format", "json");
        
        HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
        String response = null;
        try {
            HttpResult postResult = retriever.httpPost("http://query.yahooapis.com/v1/public/yql", headers, content);
            response = new String(postResult.getContent());
        } catch (HttpException e) {
            LOGGER.error("HttpException while accessing Yahoo API", e);
        }
        
        if (response != null) {
            try {
                JSONObject json = new JSONObject(response);
                JSONArray resultArray = json.getJSONObject("query").getJSONObject("results").getJSONArray("Result");
                for (int i = 0; i < resultArray.length(); i++) {
                    String term = resultArray.getString(i);
                    keyphrases.add(new Keyphrase(term));
                }
            } catch (JSONException e) {
                LOGGER.error("JSONException while parsing the response", e);
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
        return "Yahoo! Term Extraction";
    }

    public static void main(String[] args) {

        YahooTermExtraction extractor = new YahooTermExtraction();
        List<Keyphrase> extract = extractor
        // .extract("The world's largest maker of solar inverters announced Monday that it will locate its first North American manufacturing plant in Denver. \"We see a huge market coming in the U.S.,\" said Pierre-Pascal Urbon, the company's chief financial officer. Solar inverters convert the direct current created by solar panels into an alternating current accessible to the larger electrical grid. The company, based in Kassel, north of Frankfurt, Germany, boasts growing sales of about $1.2 billion a year. \"We are creating economic opportunity,\" said Gov. Bill Ritter at a press conference. He added that creating core manufacturing jobs will help Colorado escape the recession sooner.");
                .extract("Nach den Explosionen in Stockholm rätselt Schweden: Welche Motive stehen hinter der Tat? Die Polizei geht zwar von einem Einzeltäter aus, will sich aber noch nicht festlegen. Bei den Schweden wecken die Explosionen derweil Erinnerungen an die Ermordung von Ministerpräsident Palme.");
        CollectionHelper.print(extract);

    }

}
