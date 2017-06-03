package ws.palladian.classifiers.cloudservices;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

import clarifai2.api.ClarifaiBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * Imagga image labeling.
 * 
 * @see https://docs.imagga.com/
 * @author David Urbansky
 */
public class Imagga {

    private final String apiKey;
    private final String apiSecret;

    private static final String apiKeyKey = "api.imagga.key";
    private static final String apiSecretKey = "api.imagga.secret";

    public Imagga(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public Imagga(Configuration configuration) {
        this(configuration.getString(apiKeyKey), configuration.getString(apiSecretKey));
    }

    public List<String> classify(File image) throws Exception {
        return classify(image, null);
    }

    public List<String> classify(File image, String modelName) throws Exception {

        List<String> labels = new ArrayList<>();

        String contentId = upload(image);

        String jsonName1 = "tags";
        String jsonName2 = "tag";
        String endpoint = "https://api.imagga.com/v1/tagging";
        if (modelName != null) {
            endpoint = "https://api.imagga.com/v1/categorizations/" + modelName;
            jsonName1 = "categories";
            jsonName2 = "name";
        }

        HttpResponse response = Unirest.get(endpoint).queryString("content", contentId).basicAuth(apiKey, apiSecret).header("Accept", "application/json").asJson();

        JsonObject jsonObject = new JsonObject(response.getBody().toString());

        JsonArray results = jsonObject.tryGetJsonArray("results").getJsonObject(0).tryGetJsonArray(jsonName1);
        for (int i = 0; i < results.size(); i++) {
            JsonObject jso = results.tryGetJsonObject(i);
            labels.add(jso.tryGetString(jsonName2));
        }

        return labels;
    }

    private String upload(File image) throws UnirestException, JsonException {
        String endpoint = "https://api.imagga.com/v1/content";

        HttpResponse response = Unirest.post(endpoint).basicAuth(apiKey, apiSecret).field("image", image).asJson();

        String jsonResponse = response.getBody().toString();

        JsonObject jsonObject = new JsonObject(jsonResponse);

        return jsonObject.tryGetJsonArray("uploaded").tryGetJsonObject(0).tryGetString("id");
    }

    public static void main(String... args) throws Exception {
        new Imagga("TODO", "TODO").classify(new File("test.jpg"), null);
    }
}