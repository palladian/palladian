package ws.palladian.classifiers.cloudservices;

import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.visual_recognition.v3.model.*;
import org.apache.commons.configuration.Configuration;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.ImmutableCategory;
import ws.palladian.core.ImmutableCategoryEntries;
import ws.palladian.helper.collection.CollectionHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Watson Visual Recognition
 *
 * @author David Urbansky
 * @see https://cloud.ibm.com/apidocs/visual-recognition/visual-recognition-v3
 */
public class WatsonVisualRecognition implements ImageClassifier {
    private static final String apiKeyKey = "api.watson.key";

    private VisualRecognition service;

    private int maxLabels = 10;

    public WatsonVisualRecognition(Configuration configuration) {
        this(configuration.getString(apiKeyKey));
    }

    public WatsonVisualRecognition(String apiKey) {
        IamAuthenticator authenticator = new IamAuthenticator(apiKey);
        service = new VisualRecognition("2018-03-19", authenticator);
        service.setServiceUrl("https://api.eu-de.visual-recognition.watson.cloud.ibm.com");
    }

    @Override
    public void setMaxLabels(int maxLabels) {
        this.maxLabels = maxLabels;
    }

    @Override
    public List<String> classify(File image) throws IOException {
        return classify(image, "default");
    }

    public List<String> classify(File image, String modelName) throws IOException {
        CategoryEntries centries = classifyWithProbability(image, modelName);
        List<String> names = new ArrayList<>();
        for (Category centry : centries) {
            names.add(centry.getName());
        }
        return names;
    }

    public CategoryEntries classifyWithProbability(File image) throws IOException {
        return classifyWithProbability(image, "default");
    }

    /**
     * Classify an image.
     *
     * @param image         The image to classify.
     * @param classifierIds Comma-separated list of classifier ids, possible ids are "default" and "food".
     * @return
     * @throws IOException
     */
    public CategoryEntries classifyWithProbability(File image, String classifierIds) throws IOException {
        Map<String, Category> entryMap = new LinkedHashMap<>();
        Category mostLikely = new ImmutableCategory("unknown", 0.);

        InputStream imagesStream = new FileInputStream(image);
        ClassifyOptions classifyOptions = new ClassifyOptions.Builder().imagesFile(imagesStream)
                .imagesFilename(image.getName())
                .classifierIds(Arrays.asList(classifierIds))
                .build();
        ClassifiedImages result = service.classify(classifyOptions).execute().getResult();

        List<ClassifiedImage> images = result.getImages();
        for (ClassifiedImage imageClassification : images) {
            for (ClassifierResult visualClassifier : imageClassification.getClassifiers()) {
                for (ClassResult visualClass : visualClassifier.getClasses()) {
                    ImmutableCategory category = new ImmutableCategory(visualClass.getXClass(), visualClass.getScore());
                    entryMap.put(visualClass.getXClass(), category);

                    if (visualClass.getScore() > mostLikely.getProbability()) {
                        mostLikely = category;
                    }

                    if (entryMap.keySet().size() >= maxLabels) {
                        break;
                    }
                }
                break;
            }
        }

        return new ImmutableCategoryEntries(entryMap, mostLikely);
    }

    public static void main(String... args) throws Exception {
        WatsonVisualRecognition watsonRecognizer = new WatsonVisualRecognition("apiKey");
        CategoryEntries labels = watsonRecognizer.classifyWithProbability(new File("food.jpg"), "food");
        CollectionHelper.print(labels);
    }
}