package ws.palladian.dataset;

import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.modelimport.keras.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.UnsupportedKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.trainedmodels.TrainedModelHelper;
import org.deeplearning4j.nn.modelimport.keras.trainedmodels.TrainedModels;
import org.deeplearning4j.nn.transferlearning.TransferLearningHelper;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.retrieval.parser.json.JsonException;

import java.io.File;
import java.io.IOException;

/**
 * The TransferLearningHelper class allows users to "featurize" a dataset at specific intermediate vertices/layers of a model
 * This class pre-saves a featurized dataset. This can then be used to fit a model.
 * 
 * @author David Urbansky
 */
public class DatasetFeaturizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetFeaturizer.class);

    protected static final int batchSize = 15;
    public static final String featurizeExtractionLayer = "fc2";

    public void featurizeDataset(ImageDataset dataset, int trainPercentage) throws UnsupportedKerasConfigurationException, IOException, InvalidKerasConfigurationException {

        // do we really still need to featurize the dataset, check folders
        if (new File(dataset.getBasePath() + "featurizedTraining").exists()) {
            LOGGER.info("Featurized data exists already, we do NOT featurize it again! Remove the folder to re-run featurization");
            return;
        }

        // import org.deeplearning4j.transferlearning.vgg16 and print summary
        TrainedModelHelper modelImportHelper = new TrainedModelHelper(TrainedModels.VGG16);
        LOGGER.info("loading vgg16 model");
        ComputationGraph vgg16 = modelImportHelper.loadModel();
//        LOGGER.info(vgg16.summary());

        // use the TransferLearningHelper to freeze the specified vertices and below
        // NOTE: This is done in place! Pass in a cloned version of the model if you would prefer to not do this in place
        TransferLearningHelper transferLearningHelper = new TransferLearningHelper(vgg16, featurizeExtractionLayer);
//        LOGGER.info(vgg16.summary());

        DatasetIterator datasetIterator = new DatasetIterator(dataset, batchSize);
        datasetIterator.setup(trainPercentage);
        DataSetIterator trainIter = datasetIterator.getTrainingIterator();
        DataSetIterator testIter = datasetIterator.getTestingIterator();

        int trainDataSaved = 0;
        while (trainIter.hasNext()) {
            DataSet currentFeaturized = transferLearningHelper.featurize(trainIter.next());
            saveToDisk(dataset, currentFeaturized, trainDataSaved, true);
            trainDataSaved++;
        }

        int testDataSaved = 0;
        while (testIter.hasNext()) {
            DataSet currentFeaturized = transferLearningHelper.featurize(testIter.next());
            saveToDisk(dataset, currentFeaturized, testDataSaved, false);
            testDataSaved++;
        }

        LOGGER.info("finished pre-saving featurized test and train data");
    }

    public static void saveToDisk(ImageDataset imageDataset, DataSet currentFeaturized, int iterNum, boolean isTrain) {
        File fileFolder = isTrain ? new File(imageDataset.getBasePath() + "featurizedTraining")
                : new File(imageDataset.getBasePath() + "featurizedTesting");
        if (iterNum == 0) {
            fileFolder.mkdirs();
        }
        String fileName = imageDataset.getName() + "-" + featurizeExtractionLayer + "-";
        fileName += isTrain ? "train-" : "test-";
        fileName += iterNum + ".bin";
        currentFeaturized.save(new File(fileFolder, fileName));
        LOGGER.info("saved " + (isTrain ? "train " : "test ") + "file #" + iterNum);
    }

    public static void main(String[] args) throws UnsupportedKerasConfigurationException, IOException, InvalidKerasConfigurationException, JsonException {

        DatasetFeaturizer featurizeDataset = new DatasetFeaturizer();
        ImageDataset imageDataset = new ImageDataset(new File("F:\\PalladianData\\Datasets\\recipes50\\dataset.json"));
        featurizeDataset.featurizeDataset(imageDataset, 80);

    }
}