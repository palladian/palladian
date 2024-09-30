package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.ThreadHelper;
import ws.palladian.helper.collection.MapBuilder;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.persistence.json.JsonUtils;
import ws.palladian.retrieval.DocumentRetriever;

import java.util.List;

/**
 * A wrapper for Recplicate APIs.
 *
 * https://replicate.com/black-forest-labs/flux-schnell/api
 *
 * @author David Urbansky
 * Created 30.09.2024
 */
public class ReplicateAiApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicateAiApi.class);

    private final String apiKey;

    public static final String CONFIG_API_KEY = "api.replicate.key";

    public ReplicateAiApi(String apiKey) {
        this.apiKey = apiKey;
    }

    public ReplicateAiApi(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    public List<String> createImages(String prompt, String model, String aspectRatio, String outputFormat, double outputQuality, int results) {
        DocumentRetriever documentRetriever = new DocumentRetriever();
        documentRetriever.setGlobalHeaders(MapBuilder.createPut("Authorization", "Bearer " + apiKey).put("Content-Type", "application/json").create());

        JsonObject jsonObject = new JsonObject();
        JsonObject input = new JsonObject();
        input.put("prompt", prompt);
        input.put("aspect_ratio", aspectRatio);
        input.put("output_format", outputFormat);
        input.put("output_quality", outputQuality);
        input.put("num_outputs", results);
        jsonObject.put("input", input);
        String responseText = documentRetriever.tryPostJsonObject("https://api.replicate.com/v1/models/" + model + "/predictions", jsonObject, false);

        JsonObject responseJson = JsonObject.tryParse(responseText);
        if (responseJson == null) {
            LOGGER.error("could not generate image " + responseText);
            return null;
        }

        // check if there is output, if not poll get url
        int maxTries = 100;
        int tries = 0;
        while ((responseJson.tryGetJsonArray("output") == null || responseJson.tryGetJsonArray("output").size() < results) && tries < maxTries) {
            responseJson = documentRetriever.tryGetJsonObject(responseJson.tryQueryString("urls/get"));
            ThreadHelper.deepSleep(200);
            tries++;
        }

        JsonArray dataArray = responseJson.tryGetJsonArray("output");
        if (StringHelper.nullOrEmpty(dataArray)) {
            LOGGER.error("could not generate image " + responseText);
            return null;
        }
        return JsonUtils.toStringList(dataArray);
    }
}
