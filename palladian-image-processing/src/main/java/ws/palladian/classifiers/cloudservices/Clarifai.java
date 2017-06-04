package ws.palladian.classifiers.cloudservices;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;

import ws.palladian.helper.collection.CollectionHelper;
import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.ClarifaiClient;
import clarifai2.api.ClarifaiResponse;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.input.image.ClarifaiImage;
import clarifai2.dto.model.ConceptModel;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;

/**
 * Clarifai image classifier (with option to use different models).
 * 
 * @see https://developer.clarifai.com/guide/#getting-started
 * @author David Urbansky
 */
public class Clarifai {

    private final ClarifaiClient client;
    private static final String appIdKey = "api.clarifai.appId";
    private static final String appSecretKey = "api.clarifai.appSecret";

    public Clarifai(Configuration configuration) {
        this(configuration.getString(appIdKey), configuration.getString(appSecretKey));
    }

    public Clarifai(String appId, String appSecret) {
        this.client = new ClarifaiBuilder(appId, appSecret).buildSync();
    }

    public ClarifaiClient getClient() {
        return client;
    }

    public List<String> classify(File image) throws IOException {
        return classify(image, client.getDefaultModels().generalModel());
    }

    public List<String> classify(File image, ConceptModel model) throws IOException {

        List<String> labels = new ArrayList<>();

        ClarifaiResponse<List<ClarifaiOutput<Concept>>> listClarifaiResponse = model.predict().withInputs(ClarifaiInput.forImage(ClarifaiImage.of(image))).executeSync();
        for (ClarifaiOutput<Concept> conceptClarifaiOutput : listClarifaiResponse.get()) {
            for (Concept concept : conceptClarifaiOutput.data()) {
                labels.add(concept.name());
            }
        }

        return labels;
    }

    public static void main(String... args) throws Exception {
        Clarifai clarifaiClient = new Clarifai("appId", "appSecret");
        List<String> labels = clarifaiClient.classify(new File("test.jpg"), clarifaiClient.getClient().getDefaultModels().generalModel());
        CollectionHelper.print(labels);
    }
}