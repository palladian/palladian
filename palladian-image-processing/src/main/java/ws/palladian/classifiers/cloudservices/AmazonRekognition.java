package ws.palladian.classifiers.cloudservices;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;

import ws.palladian.helper.collection.CollectionHelper;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.*;
import com.amazonaws.util.IOUtils;

/**
 * Amazon Rekognition image classifier.
 * 
 * @see http://docs.aws.amazon.com/rekognition/latest/dg/get-started-exercise-detect-labels.html
 * @author David Urbansky
 */
public class AmazonRekognition {

    private AWSCredentials credentials = null;
    private static final String apiKeyKey = "api.amazon.cloud.key";
    private static final String apiSecretKey = "api.amazon.cloud.secret";

    public AmazonRekognition(Configuration configuration) {
        this(configuration.getString(apiKeyKey), configuration.getString(apiSecretKey));
    }

    public AmazonRekognition(String apiKey, String apiSecret) {
        try {
            credentials = new BasicAWSCredentials(apiKey, apiSecret);
        } catch (Exception e) {
            throw new AmazonClientException("Incorrect apiKey and/or apiSecret.", e);
        }
    }

    public List<String> classify(File image) throws IOException {
        return classify(image, 10);
    }

    public List<String> classify(File image, int maxLabels) throws IOException {

        List<String> labels = new ArrayList<>();

        if (maxLabels < 1) {
            maxLabels = 1;
        }

        com.amazonaws.services.rekognition.AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard().withRegion(Regions.DEFAULT_REGION)
                .withCredentials(new AWSStaticCredentialsProvider(credentials)).build();

        ByteBuffer imageBytes;
        try (InputStream inputStream = new FileInputStream(image)) {
            imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
        }

        Image amazonImage = new Image();
        amazonImage.setBytes(imageBytes);

        DetectLabelsRequest request = new DetectLabelsRequest().withImage(amazonImage).withMaxLabels(maxLabels).withMinConfidence(0F);

        try {
            DetectLabelsResult result = rekognitionClient.detectLabels(request);
            List<Label> apiLabels = result.getLabels();

            for (Label label : apiLabels) {
                labels.add(label.getName());
            }
        } catch (AmazonRekognitionException e) {
            e.printStackTrace();
        }

        return labels;
    }

    public static void main(String... args) throws Exception {
        AmazonRekognition amazonRekognition = new AmazonRekognition("appId", "appSecret");
        List<String> labels = amazonRekognition.classify(new File("dog.jpg"), 10);
        CollectionHelper.print(labels);
    }
}