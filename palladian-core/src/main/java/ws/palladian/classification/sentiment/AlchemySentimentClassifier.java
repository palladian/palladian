package ws.palladian.classification.sentiment;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.parser.json.JsonObject;

public class AlchemySentimentClassifier {

    private static final String API_URL = "http://access.alchemyapi.com/calls/text/TextGetTextSentiment?apikey=%s&text=%s&outputMode=json";

    /** The API key for the Alchemy API service. */
    private final String apiKey;

    private final DocumentRetriever documentRetriever;

    public AlchemySentimentClassifier(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.documentRetriever = new DocumentRetriever();
        this.apiKey = apiKey;
    }

    public CategoryEntries classify(String text) {
        JsonObject json = documentRetriever.getJsonObject(String.format(API_URL, apiKey,
                UrlHelper.encodeParameter(text)));

        JsonObject docSentiment = json.getJsonObject("docSentiment");
        String category = docSentiment.getString("type");
        Double score = 0.5 * docSentiment.getDouble("score") + 0.5;
        if (docSentiment.getDouble("score") < 0) {
            score = 1 - score;
        }

        Map<String, Double> map = new HashMap<String, Double>();
        CategoryEntriesMap categoryEntries = new CategoryEntriesMap(map);
        categoryEntries.set(category, score);

        return categoryEntries;
    }

    public static void main(String[] args) {
        AlchemySentimentClassifier asc = new AlchemySentimentClassifier("TODO");
        CategoryEntries result = asc.classify("This really sucks!!!");
        System.out.println(result);
    }

}
