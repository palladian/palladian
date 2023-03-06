package ws.palladian.classifiers.cloudservices;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.commons.configuration.Configuration;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.ImmutableCategory;
import ws.palladian.core.ImmutableCategoryEntries;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;

import java.io.File;
import java.util.*;

/**
 * Imagga image labeling.
 *
 * @author David Urbansky
 * @see https://docs.imagga.com/
 */
public class Imagga implements ImageClassifier {
    private final String apiKey;
    private final String apiSecret;

    private static final String apiKeyKey = "api.imagga.key";
    private static final String apiSecretKey = "api.imagga.secret";

    private int maxLabels = 10;

    public Imagga(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public Imagga(Configuration configuration) {
        this(configuration.getString(apiKeyKey), configuration.getString(apiSecretKey));
    }

    @Override
    public void setMaxLabels(int maxLabels) {
        this.maxLabels = maxLabels;
    }

    @Override
    public List<String> classify(File image) throws Exception {
        CategoryEntries centries = classifyWithProbability(image);
        List<String> names = new ArrayList<>();
        for (Category centry : centries) {
            names.add(centry.getName());
        }
        return names;
    }

    public CategoryEntries classifyWithProbability(File image) throws Exception {
        Map<String, Category> entryMap = new LinkedHashMap<>();
        Category mostLikely = new ImmutableCategory("unknown", 0.);

        JsonObject resultJson = upload(image);

        JsonArray tagsArray = Optional.ofNullable(resultJson.tryQueryJsonArray("result/tags")).orElse(new JsonArray());

        for (int i = 0; i < tagsArray.size(); i++) {
            JsonObject jso = tagsArray.tryGetJsonObject(i);

            String tagName = jso.tryQueryString("tag/en");
            Double score = jso.tryQueryDouble("confidence") / 100;

            ImmutableCategory category = new ImmutableCategory(tagName, score);
            entryMap.put(tagName, category);

            if (score > mostLikely.getProbability()) {
                mostLikely = category;
            }

            entryMap.put(tagName, category);

            if (i >= maxLabels - 1) {
                break;
            }
        }

        return new ImmutableCategoryEntries(entryMap, mostLikely);
    }

    private JsonObject upload(File image) throws JsonException {
        String endpoint = "https://api.imagga.com/v2/tags";
        HttpResponse response = Unirest.post(endpoint).basicAuth(apiKey, apiSecret).field("image", image).asJson();
        return new JsonObject(response.getBody().toString());
    }

    public static void main(String... args) throws Exception {
        new Imagga("apiKey", "apiSecret").classify(new File("test.jpg"));
    }
}