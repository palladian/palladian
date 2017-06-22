package ws.palladian.classifiers.cloudservices;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.ImmutableCategory;
import ws.palladian.core.ImmutableCategoryEntries;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * Microsoft Azure Computer Vision API for label detection (using large generic model).
 * 
 * @see https://docs.microsoft.com/en-us/azure/cognitive-services/computer-vision/quickstarts/java#AnalyzeImage
 * @author David Urbansky
 */
public class MicrosoftComputerVision {

    private final String endpoint;
    private final String apiKey;

    public MicrosoftComputerVision(String endpoint, String apiKey) {
        this.endpoint = endpoint;
        this.apiKey = apiKey;
    }

    public CategoryEntries classify(File image) throws IOException {

        Map<String, Category> entryMap = new LinkedHashMap<>();
        Category mostLikely = new ImmutableCategory("unknown", 0.);

        HttpClient httpclient = new DefaultHttpClient();

        try {
            URIBuilder builder = new URIBuilder(endpoint + "/analyze");

            builder.setParameter("visualFeatures", "Tags");
            builder.setParameter("language", "en");

            URI uri = builder.build();
            HttpPost request = new HttpPost(uri);

            // request headers
            request.setHeader("Ocp-Apim-Subscription-Key", apiKey);
            request.setHeader("Content-Type", "application/octet-stream");

            // Request body. Replace the example URL with the URL for the JPEG image of a celebrity.
            FileEntity reqEntity = new FileEntity(image);
            request.setEntity(reqEntity);

            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                JsonObject responseJson = new JsonObject(EntityUtils.toString(entity));
                JsonArray tags = responseJson.tryGetJsonArray("tags");

                for (int i = 0; i < tags.size(); i++) {

                    String tagName = tags.tryGetJsonObject(i).tryGetString("name");
                    Double score = tags.tryGetJsonObject(i).tryGetDouble("confidence");

                    ImmutableCategory category = new ImmutableCategory(tagName, score);
                    entryMap.put(tagName, category);

                    if (score > mostLikely.getProbability()) {
                        mostLikely = category;
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ImmutableCategoryEntries(entryMap, mostLikely);
    }

    public static void main(String... args) throws Exception {
        CategoryEntries labels = new MicrosoftComputerVision("endpoint", "apiKey").classify(new File("dog.jpg"));
        CollectionHelper.print(labels);
    }
}