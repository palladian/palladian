package ws.palladian.classification.sentiment;

import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.parser.json.JsonObject;

public class AlchemySentimentClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(AlchemySentimentClassifier.class);

    private static final String API_URL = "http://access.alchemyapi.com/calls/text/TextGetTextSentiment?apikey=%s&outputMode=json";

    /** The API key for the Alchemy API service. */
    private final String apiKey;

    public AlchemySentimentClassifier(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
    }

    public CategoryEntries classify(String text) throws HttpException {
        CategoryEntriesMap categoryEntries = new CategoryEntriesMap();

        HttpRequest request = new HttpRequest(HttpMethod.POST, String.format(API_URL, apiKey));
        request.addParameter("text", text.trim());
        HttpResult result = HttpRetrieverFactory.getHttpRetriever().execute(request);
        JsonObject json = new JsonObject(HttpHelper.getStringContent(result));

        if (json.getString("status").equalsIgnoreCase("ok")) {

            JsonObject docSentiment = json.getJsonObject("docSentiment");
            String category = docSentiment.getString("type");

            // sometimes results are neutral, we assign score = 0;
            Double score = 0.;

            if (docSentiment.containsKey("score")) {
                score = 0.5 * docSentiment.getDouble("score") + 0.5;
                if (docSentiment.getDouble("score") < 0) {
                    score = 1 - score;
                }
            }
            categoryEntries.set(category, score);

        } else {
            LOGGER.error(json.getString("statusInfo"));
            categoryEntries.set("neutral", 0.);
        }

        return categoryEntries;
    }

    public static void main(String[] args) throws HttpException {
        AlchemySentimentClassifier asc = new AlchemySentimentClassifier("TODO");
        CategoryEntries result = asc.classify("This really sucks!!!");
        System.out.println(result);
    }

}
