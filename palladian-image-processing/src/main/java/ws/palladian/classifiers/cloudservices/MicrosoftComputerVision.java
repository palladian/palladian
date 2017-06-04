package ws.palladian.classifiers.cloudservices;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

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

    public List<String> classify(File image) throws IOException {
        return classify(image, 10);
    }

    public List<String> classify(File image, int maxNumberOfLabels) throws IOException {

        List<String> labels = new ArrayList<>();

        if (maxNumberOfLabels < 1) {
            maxNumberOfLabels = 1;
        }

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
                    labels.add(tags.tryGetJsonObject(i).tryGetString("name"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return CollectionHelper.getSublist(labels, 0, maxNumberOfLabels);
    }

    public static void main(String... args) throws Exception {
        List<String> labels = new MicrosoftComputerVision("endpoint", "apiKey").classify(new File("dog.jpg"), 10);
        CollectionHelper.print(labels);
    }
}