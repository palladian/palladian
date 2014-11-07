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
 * Yahoo! Term Extraction
 * </p>
 * 
 * @author Philipp Katz
 */
public final class YahooTermExtraction extends KeyphraseExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(YahooTermExtraction.class);

    @Override
    public List<Keyphrase> extract(String inputText) {

        List<Keyphrase> keyphrases = new ArrayList<Keyphrase>();

        HttpRequest request = new HttpRequest(HttpMethod.POST, "http://query.yahooapis.com/v1/public/yql");
        request.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        request.addHeader("Accept", "application/json");

        // create the YQL query string
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("select * from search.termextract ");
        queryBuilder.append("where context=\"");
        queryBuilder.append(inputText.replace("\"", "\\\""));
        queryBuilder.append("\"");

        request.addParameter("q", queryBuilder.toString());
        request.addParameter("format", "json");

        HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
        String response = null;
        try {
            HttpResult postResult = retriever.execute(request);
            response = postResult.getStringContent();
        } catch (HttpException e) {
            LOGGER.error("HttpException while accessing Yahoo API", e);
        }
        if (response != null) {
            try {
                JsonObject json = new JsonObject(response);
                JsonArray resultArray = json.getJsonObject("query").getJsonObject("results").getJsonArray("Result");
                for (int i = 0; i < resultArray.size(); i++) {
                    String term = resultArray.getString(i);
                    keyphrases.add(new Keyphrase(term));
                }
            } catch (JsonException e) {
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
        String text = "The world's largest maker of solar inverters announced Monday that it will locate its first North American manufacturing plant in Denver. \"We see a huge market coming in the U.S.,\" said Pierre-Pascal Urbon, the company's chief financial officer. Solar inverters convert the direct current created by solar panels into an alternating current accessible to the larger electrical grid. The company, based in Kassel, north of Frankfurt, Germany, boasts growing sales of about $1.2 billion a year. \"We are creating economic opportunity,\" said Gov. Bill Ritter at a press conference. He added that creating core manufacturing jobs will help Colorado escape the recession sooner.";
        List<Keyphrase> extract = extractor.extract(text);
        CollectionHelper.print(extract);
    }

}
