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

public class FiveFiltersTermExtraction extends KeyphraseExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FiveFiltersTermExtraction.class);

    @Override
    public List<Keyphrase> extract(String inputText) {
        List<Keyphrase> keyphrases = new ArrayList<Keyphrase>();
        String response = null;
        try {
            HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
            HttpRequest request = new HttpRequest(HttpMethod.POST, "http://term-extraction.appspot.com/terms");
            request.addParameter("content", inputText);
            HttpResult postResult = retriever.execute(request);
            response = new String(postResult.getContent());

        } catch (HttpException e) {
            LOGGER.error("HttpException while accessing the service", e);
        }
        if (response != null) {
            try {
                JsonArray jsonArray = new JsonArray(response);
                for (int i = 0; i < jsonArray.size(); i++) {
                    String text = jsonArray.getString(i);
                    keyphrases.add(new Keyphrase(text));
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
        return "Five Filters Term Extraction";
    }

    public static void main(String[] args) {
        FiveFiltersTermExtraction extractor = new FiveFiltersTermExtraction();
        String text = "The world's largest maker of solar inverters announced Monday that it will locate its first North American manufacturing plant in Denver. \"We see a huge market coming in the U.S.,\" said Pierre-Pascal Urbon, the company's chief financial officer. Solar inverters convert the direct current created by solar panels into an alternating current accessible to the larger electrical grid. The company, based in Kassel, north of Frankfurt, Germany, boasts growing sales of about $1.2 billion a year. \"We are creating economic opportunity,\" said Gov. Bill Ritter at a press conference. He added that creating core manufacturing jobs will help Colorado escape the recession sooner.";
        List<Keyphrase> result = extractor.extract(text);
        CollectionHelper.print(result);
    }

}
