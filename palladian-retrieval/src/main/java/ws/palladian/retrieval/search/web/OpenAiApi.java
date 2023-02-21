package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;
import ws.palladian.helper.collection.MapBuilder;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * A wrapper for OpenAI's APIs.
 *
 * @author David Urbansky
 * Created 15.02.2023
 */
public class OpenAiApi {
    private final String apiKey;

    public static final String CONFIG_API_KEY = "api.openai.apiKey";

    public OpenAiApi(String apiKey) {
        this.apiKey = apiKey;
    }

    public OpenAiApi(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    public float[] getEmbedding(String text) throws Exception {
        DocumentRetriever documentRetriever = new DocumentRetriever();
        documentRetriever.setGlobalHeaders(MapBuilder.createPut("Content-Type", "application/json").put("Authorization", "Bearer " + apiKey).create());
        JsonObject requestJson = new JsonObject();
        requestJson.put("input", text);
        requestJson.put("model", "text-embedding-ada-002");
        String postResponseText = documentRetriever.tryPostJsonObject("https://api.openai.com/v1/embeddings", requestJson, false);
        JsonObject responseJson = JsonObject.tryParse(postResponseText);
        if (responseJson == null) {
            throw new Exception("Could not parse json " + postResponseText);
        }
        if (responseJson.tryQueryString("error/message") != null) {
            throw new Exception(responseJson.tryQueryString("error/message"));
        }

        JsonArray embeddingArray = responseJson.tryQueryJsonArray("data[0]/embedding");
        float[] embedding = new float[embeddingArray.size()];
        for (Object o : embeddingArray) {
            embedding[embeddingArray.indexOf(o)] = Float.parseFloat(o.toString());
        }

        return embedding;
    }

    public String ask(String text) throws Exception {
        DocumentRetriever documentRetriever = new DocumentRetriever();
        documentRetriever.setGlobalHeaders(MapBuilder.createPut("Content-Type", "application/json").put("Authorization", "Bearer " + apiKey).create());
        JsonObject requestJson = new JsonObject();
        requestJson.put("prompt", text);
        requestJson.put("model", "text-davinci-003");
        requestJson.put("temperature", 1.);
        requestJson.put("max_tokens", 64);
        requestJson.put("top_p", 1);
        String postResponseText = documentRetriever.tryPostJsonObject("https://api.openai.com/v1/completions", requestJson, false);
        JsonObject responseJson = JsonObject.tryParse(postResponseText);
        if (responseJson == null) {
            throw new Exception("Could not parse json " + postResponseText);
        }
        if (responseJson.tryQueryString("error/message") != null) {
            throw new Exception(responseJson.tryQueryString("error/message"));
        }

        String answer = responseJson.tryQueryString("choices[0]/text");

        return StringHelper.clean(answer);
    }
}
