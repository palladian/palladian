package ws.palladian.classifiers.cloudservices;

import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.ClarifaiClient;
import clarifai2.api.ClarifaiResponse;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.model.ConceptModel;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;
import org.apache.commons.configuration.Configuration;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.ImmutableCategory;
import ws.palladian.core.ImmutableCategoryEntries;
import ws.palladian.helper.collection.CollectionHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Clarifai image classifier (with option to use different models).
 *
 * @author David Urbansky
 * @see https://developer.clarifai.com/guide/#getting-started
 */
public class Clarifai implements ImageClassifier {
    private final ClarifaiClient client;
    private static final String apiKeyIdentifier = "api.clarifai.key";

    private int maxLabels = 10;

    public Clarifai(Configuration configuration) {
        this(configuration.getString(apiKeyIdentifier));
    }

    public Clarifai(String apiKey) {
        this.client = new ClarifaiBuilder(apiKey).buildSync();
    }

    public ClarifaiClient getClient() {
        return client;
    }

    @Override
    public void setMaxLabels(int maxLabels) {
        this.maxLabels = maxLabels;
    }

    @Override
    public List<String> classify(File image) {
        return classify(image, client.getDefaultModels().generalModel());
    }

    public List<String> classify(File image, ConceptModel model) {
        CategoryEntries centries = classifyWithProbability(image, model);
        List<String> names = new ArrayList<>();
        for (Category centry : centries) {
            names.add(centry.getName());
        }
        return names;
    }

    public CategoryEntries classifyWithProbability(File image, ConceptModel model) {
        Map<String, Category> entryMap = new LinkedHashMap<>();
        Category mostLikely = new ImmutableCategory("unknown", 0.);

        ClarifaiResponse<List<ClarifaiOutput<Concept>>> listClarifaiResponse = model.predict().withInputs(ClarifaiInput.forImage(image)).executeSync();
        for (ClarifaiOutput<Concept> conceptClarifaiOutput : listClarifaiResponse.get()) {
            for (Concept concept : conceptClarifaiOutput.data()) {
                if (concept == null) {
                    continue;
                }
                ImmutableCategory category = new ImmutableCategory(concept.name(), concept.value());
                entryMap.put(concept.name(), category);

                if (concept.value() > mostLikely.getProbability()) {
                    mostLikely = category;
                }

                if (entryMap.keySet().size() >= maxLabels) {
                    break;
                }
            }
        }

        return new ImmutableCategoryEntries(entryMap, mostLikely);
    }

    public static void main(String... args) throws Exception {
        Clarifai clarifaiClient = new Clarifai("apiKey");
        CategoryEntries entries = clarifaiClient.classifyWithProbability(new File("food.jpg"), clarifaiClient.getClient().getDefaultModels().foodModel());
        CollectionHelper.print(entries);
    }
}