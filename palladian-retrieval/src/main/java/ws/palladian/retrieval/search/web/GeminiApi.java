package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.DocumentRetriever;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gemini API client implementation.
 *
 * @author David Urbansky
 * @since 23.01.2026
 **/
public class GeminiApi extends AiApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiApi.class);
    public static final String CONFIG_API_KEY = "api.google.gemini";

    public enum Model {
        GEMINI_3_0_PRO("gemini-3-pro-preview"), GEMINI_3_0_FLASH("gemini-3-flash-preview"), NANO_BANANA_PRO("gemini-3-pro-image-preview"), VEO_3_1_PRO("veo-3.1-generate-preview");

        private final String name;

        Model(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final String apiKey;
    private String model = GeminiApi.Model.GEMINI_3_0_FLASH.toString(); // Default

    private final AtomicInteger totalInputTokens = new AtomicInteger(0);
    private final AtomicInteger totalOutputTokens = new AtomicInteger(0);

    public GeminiApi(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    public GeminiApi(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setModel(Model model) {
        this.model = model.toString();
    }

    public String getModel() {
        return model;
    }

    public int getTotalInputTokens() {
        return totalInputTokens.get();
    }

    public int getTotalOutputTokens() {
        return totalOutputTokens.get();
    }

    public int getTotalTokens() {
        return totalInputTokens.get() + totalOutputTokens.get();
    }

    @Override
    public String chat(JsonArray messages, double temperature, AtomicInteger usedTokens) throws Exception {
        return chat(messages, temperature, usedTokens, this.model, null, null);
    }

    @Override
    public String chat(JsonArray messages, double temperature, AtomicInteger usedTokens, String modelName, Integer maxTokens, JsonObject responseSchema) throws Exception {
        // Convert AiApi messages (OpenAI style) to Gemini style
        JsonArray geminiContents = new JsonArray();
        for (int i = 0; i < messages.size(); i++) {
            JsonObject msg = (JsonObject) messages.get(i);
            String role = msg.getString("role");
            String content = msg.getString("content");

            JsonObject geminiMsg = new JsonObject();
            if ("user".equals(role)) {
                geminiMsg.put("role", "user");
            } else if ("assistant".equals(role) || "system".equals(
                    role)) { // Gemini uses 'model' for assistant, system instructions are separate but mapping system to user/model sometimes works, strictly system instructions are separate field.
                // For simplicity, map system to user or handle separately?
                // Gemini 1.5 supports system_instruction.
                // But here we'll map assistant to model.
                geminiMsg.put("role", "model");
            } else {
                geminiMsg.put("role", "user");
            }

            JsonArray parts = new JsonArray();
            JsonObject textPart = new JsonObject();
            textPart.put("text", content);
            parts.add(textPart);
            geminiMsg.put("parts", parts);

            geminiContents.add(geminiMsg);
        }

        return generateContent(geminiContents, temperature, usedTokens, modelName, maxTokens, responseSchema);
    }

    public String chat(JsonArray contents, String modelName) throws Exception {
        return generateContent(contents, 0.0, null, modelName != null ? modelName : this.model, null, null);
    }

    /**
     * Chat with optional image/video files.
     */
    public String chat(String prompt, String modelName, File... files) throws Exception {
        JsonArray contents = new JsonArray();
        JsonObject userContent = new JsonObject();
        userContent.put("role", "user");

        JsonArray parts = new JsonArray();

        // Add text prompt
        JsonObject textPart = new JsonObject();
        textPart.put("text", prompt);
        parts.add(textPart);

        // Add files
        if (files != null) {
            for (File file : files) {
                JsonObject filePart = new JsonObject();
                JsonObject inlineData = new JsonObject();
                inlineData.put("mime_type", guessMimeType(file));
                inlineData.put("data", Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath())));
                filePart.put("inline_data", inlineData);
                parts.add(filePart);
            }
        }

        userContent.put("parts", parts);
        contents.add(userContent);

        return generateContent(contents, 1.0, null, modelName != null ? modelName : this.model, null, null);
    }

    private String generateContent(JsonArray contents, double temperature, AtomicInteger usedTokens, String modelName, Integer maxTokens, JsonObject responseSchema)
            throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;

        JsonObject request = new JsonObject();
        request.put("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.put("temperature", temperature);
        if (maxTokens != null) {
            generationConfig.put("maxOutputTokens", maxTokens);
        }
        if (responseSchema != null) {
            generationConfig.put("response_mime_type", "application/json");
            generationConfig.put("response_schema", responseSchema);
        }
        request.put("generationConfig", generationConfig);

        String responseText = executeRequest(url, request);
        JsonObject response = JsonObject.tryParse(responseText);

        if (response == null) {
            throw new Exception("Empty or invalid response from Gemini API");
        }

        if (response.containsKey("error")) {
            throw new Exception("Gemini API Error: " + response.getJsonObject("error").getString("message"));
        }

        // Update tokens
        if (response.containsKey("usageMetadata")) {
            JsonObject usage = response.getJsonObject("usageMetadata");
            int promptTokens = usage.getInt("promptTokenCount");
            int candidatesTokens = usage.getInt("candidatesTokenCount"); // output

            totalInputTokens.addAndGet(promptTokens);
            totalOutputTokens.addAndGet(candidatesTokens);

            if (usedTokens != null) {
                usedTokens.addAndGet(promptTokens + candidatesTokens);
            }
        }

        JsonArray candidates = response.getJsonArray("candidates");
        if (candidates != null && candidates.size() > 0) {
            JsonObject candidate = (JsonObject) candidates.get(0);
            JsonObject content = candidate.getJsonObject("content");
            if (content != null) {
                JsonArray parts = content.getJsonArray("parts");
                if (parts != null && parts.size() > 0) {
                    return ((JsonObject) parts.get(0)).getString("text");
                }
            }
        }

        return null;
    }

    public String generateImage(String prompt, String modelName) throws Exception {
        // Using Imagen model via predict or generateContent if supported.
        // Assuming 'imagen-3.0-generate-001' or similar.
        // Endpoint structure might differ.
        // For 2026, assuming unified endpoint or standard predict.

        String targetModel = modelName != null ? modelName : "imagen-3.0-generate-001";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + targetModel + ":predict?key=" + apiKey;

        JsonObject request = new JsonObject();
        JsonArray instances = new JsonArray();
        JsonObject instance = new JsonObject();
        instance.put("prompt", prompt);
        instances.add(instance);
        request.put("instances", instances);

        JsonObject parameters = new JsonObject();
        parameters.put("sampleCount", 1);
        request.put("parameters", parameters);

        String responseText = executeRequest(url, request);
        JsonObject response = JsonObject.tryParse(responseText);

        if (response == null || response.containsKey("error")) {
            // Fallback or error
            if (response != null)
                throw new Exception("Image Gen Error: " + response.getJsonObject("error").getString("message"));
            throw new Exception("Empty response for image generation");
        }

        // Parse predictions
        JsonArray predictions = response.getJsonArray("predictions");
        if (predictions != null && predictions.size() > 0) {
            JsonObject prediction = (JsonObject) predictions.get(0);
            // Imagen usually returns base64 bytes or url.
            // Check for 'bytesBase64Encoded' or similar.
            if (prediction.containsKey("bytesBase64Encoded")) {
                return prediction.getString("bytesBase64Encoded"); // Return base64 string
            }
            if (prediction.containsKey("mimeType") && prediction.containsKey("bytesBase64Encoded")) {
                // return data URI?
                return "data:" + prediction.getString("mimeType") + ";base64," + prediction.getString("bytesBase64Encoded");
            }
        }

        return responseText; // Return raw if structure unknown
    }

    public String generateVideo(String prompt, String modelName) throws Exception {
        // Placeholder for video generation.
        // Assuming similar structure to image gen but with video model.
        String targetModel = modelName != null ? modelName : "veo-001-generate"; // Hypothetical
        // Video gen is likely async (long running operation).
        // If simple predict is not available, we might need LRO (Long Running Operations).
        // Given constraints, I'll implement a basic predict call.

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + targetModel + ":predict?key=" + apiKey;

        JsonObject request = new JsonObject();
        JsonArray instances = new JsonArray();
        JsonObject instance = new JsonObject();
        instance.put("prompt", prompt);
        instances.add(instance);
        request.put("instances", instances);

        String responseText = executeRequest(url, request);
        JsonObject response = JsonObject.tryParse(responseText);

        if (response != null && response.containsKey("predictions")) {
            // Extract video URL or content
            return response.toString();
        }

        return responseText;
    }

    protected String executeRequest(String url, JsonObject request) throws Exception {
        DocumentRetriever retriever = new DocumentRetriever();
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        retriever.setGlobalHeaders(headers);
        return retriever.tryPostJsonObject(url, request, false);
    }

    private String guessMimeType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png"))
            return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg"))
            return "image/jpeg";
        if (name.endsWith(".webp"))
            return "image/webp";
        if (name.endsWith(".heic"))
            return "image/heic";
        if (name.endsWith(".heif"))
            return "image/heif";
        if (name.endsWith(".mp4"))
            return "video/mp4";
        if (name.endsWith(".mpeg"))
            return "video/mpeg";
        if (name.endsWith(".mov"))
            return "video/quicktime";
        if (name.endsWith(".avi"))
            return "video/x-msvideo";
        if (name.endsWith(".flv"))
            return "video/x-flv";
        if (name.endsWith(".mpg"))
            return "video/mpg";
        if (name.endsWith(".webm"))
            return "video/webm";
        if (name.endsWith(".wmv"))
            return "video/wmv";
        if (name.endsWith(".3gp"))
            return "video/3gpp";
        return "application/octet-stream";
    }
}
