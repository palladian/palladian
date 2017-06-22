package ws.palladian.classifiers.cloudservices;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ImageClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassifier;
import org.apache.commons.configuration.Configuration;

import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.ImmutableCategory;
import ws.palladian.core.ImmutableCategoryEntries;
import ws.palladian.helper.collection.CollectionHelper;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.*;
import com.amazonaws.util.IOUtils;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;

/**
 * Watson Visual Recognition
 * 
 * @see https://www.ibm.com/watson/developercloud/visual-recognition/api/v3/
 * @author David Urbansky
 */
public class WatsonVisualRecognition {

    private static final String apiKeyKey = "api.watson.key";

    private VisualRecognition service;

    public WatsonVisualRecognition(Configuration configuration) {
        this(configuration.getString(apiKeyKey));
    }

    public WatsonVisualRecognition(String apiKey) {
        service = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);
        service.setApiKey(apiKey);
    }

    public CategoryEntries classify(File image) throws IOException {
        return classify(image, "default");
    }

    /**
     * Classify an image.
     * 
     * @param image The image to classify.
     * @param classifierIds Comma-separated list of classifier ids, possible ids are "default" and "food".
     * @return
     * @throws IOException
     */
    public CategoryEntries classify(File image, String classifierIds) throws IOException {

        Map<String, Category> entryMap = new LinkedHashMap<>();
        Category mostLikely = new ImmutableCategory("unknown", 0.);

        ClassifyImagesOptions options = new ClassifyImagesOptions.Builder().images(image).classifierIds(classifierIds).build();
        VisualClassification result = service.classify(options).execute();

        List<ImageClassification> images = result.getImages();
        for (ImageClassification imageClassification : images) {

            for (VisualClassifier visualClassifier : imageClassification.getClassifiers()) {
                for (VisualClassifier.VisualClass visualClass : visualClassifier.getClasses()) {

                    ImmutableCategory category = new ImmutableCategory(visualClass.getName(), visualClass.getScore());
                    entryMap.put(visualClass.getName(), category);

                    if (visualClass.getScore() > mostLikely.getProbability()) {
                        mostLikely = category;
                    }

                }
                break;
            }

        }

        return new ImmutableCategoryEntries(entryMap, mostLikely);
    }

    public static void main(String... args) throws Exception {
        WatsonVisualRecognition amazonRekognition = new WatsonVisualRecognition("TODO");
        CategoryEntries labels = amazonRekognition.classify(new File("burger.jpg"), "food");
        CollectionHelper.print(labels);
    }
}