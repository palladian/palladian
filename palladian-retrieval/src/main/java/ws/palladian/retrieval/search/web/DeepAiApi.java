package ws.palladian.retrieval.search.web;

import okhttp3.*;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.collection.MapBuilder;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.DocumentRetriever;

import java.io.File;
import java.io.IOException;

/**
 * A wrapper for DeepAi's APIs.
 *
 * See: https://deepai.org/docs
 *
 * @author David Urbansky
 * Created 03.07.2024
 */
public class DeepAiApi /* TODO extends AiApi*/ {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeepAiApi.class);

    private final String apiKey;

    public static final String CONFIG_API_KEY = "api.deepai.key";

    public DeepAiApi(String apiKey) {
        this.apiKey = apiKey;
    }

    public DeepAiApi(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    /**
     * @param prompt The textual prompt, e.g. "a black dog with a Christmas hat on"
     * @param size   The picture size. Recommended below 700 pixel
     */
    public String createImage(String prompt, String width, String height, String model, String negativePrompt) {
        DocumentRetriever documentRetriever = new DocumentRetriever();
        documentRetriever.setGlobalHeaders(MapBuilder.createPut("api-key", apiKey).create());

        JsonObject bodyJson = new JsonObject();
        bodyJson.put("text", prompt);
        bodyJson.put("width", width);
        bodyJson.put("height", height);
        bodyJson.put("image_generator_version", model);
        if (negativePrompt != null) {
            bodyJson.put("negative_prompt", negativePrompt);
        }
        String responseText = documentRetriever.tryPostJsonObject("https://api.deepai.org/api/text2img", bodyJson, true);

        JsonObject responseJson = JsonObject.tryParse(responseText);
        if (responseJson == null) {
            LOGGER.error("could not generate image " + responseText);
            return null;
        }

        return responseJson.tryGetString("output_url");
    }

    public String editImage(String prompt, String filePath) throws IOException {
        File imageFile = new File(filePath);
        OkHttpClient client = new OkHttpClient();

        // Build the form body
        RequestBody formBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("image", imageFile.getName(),
                RequestBody.create(MediaType.parse("image/jpeg"), imageFile)).addFormDataPart("text", prompt).build();

        // Build the request
        Request request = new Request.Builder().url("https://api.deepai.org/api/image-editor").addHeader("api-key", apiKey).post(formBody).build();

        // Send the request
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            LOGGER.error("could not edit image " + response.body().string());
            return null;
        }

        // Get the response body
        ResponseBody responseBody = response.body();
        if (responseBody != null) {
            String responseText = responseBody.string();
            JsonObject responseJson = JsonObject.tryParse(responseText);
            if (responseJson == null) {
                LOGGER.error("could not generate image " + responseText);
                return null;
            }

            return responseJson.tryGetString("output_url");
        }

        return null;
    }

    public static void main(String[] args) throws IOException {
        DeepAiApi deepAiApi = new DeepAiApi("API_KEY");
    }
}
