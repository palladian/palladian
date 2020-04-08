//package ws.palladian.dataset;
//
//import org.deeplearning4j.datasets.iterator.AsyncDataSetIterator;
//import org.deeplearning4j.nn.modelimport.keras.InvalidKerasConfigurationException;
//import org.deeplearning4j.nn.modelimport.keras.UnsupportedKerasConfigurationException;
//import org.nd4j.linalg.dataset.ExistingMiniBatchDataSetIterator;
//import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.File;
//import java.io.IOException;
//
///**
// * Iterator for featurized data.
// *
// * @author David Urbansky
// */
//public class DatasetIteratorFeaturized {
//    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetIteratorFeaturized.class);
//
//    private String featureExtractorLayer = DatasetFeaturizer.featurizeExtractionLayer;
//    private ImageDataset imageDataset;
//
//    public DatasetIteratorFeaturized(ImageDataset imageDataset, String featureExtractorLayerArg) {
//        this.imageDataset = imageDataset;
//        this.featureExtractorLayer = featureExtractorLayerArg;
//    }
//
//    public DataSetIterator getTrainingIterator() throws UnsupportedKerasConfigurationException, IOException, InvalidKerasConfigurationException {
//        runFeaturize();
//        DataSetIterator existingTrainingData = new ExistingMiniBatchDataSetIterator(new File(imageDataset.getBasePath() + "featurizedTraining"),
//                imageDataset.getName() + "-" + featureExtractorLayer + "-train-%d.bin");
//        return new AsyncDataSetIterator(existingTrainingData);
//    }
//
//    public DataSetIterator getTestingIterator() {
//        DataSetIterator existingTestData = new ExistingMiniBatchDataSetIterator(new File(imageDataset.getBasePath() + "featurizedTesting"),
//                imageDataset.getName() + "-" + featureExtractorLayer + "-test-%d.bin");
//        return new AsyncDataSetIterator(existingTestData);
//    }
//
//    private void runFeaturize() throws InvalidKerasConfigurationException, IOException, UnsupportedKerasConfigurationException {
//        File trainDir = new File(imageDataset.getBasePath() + "featurizedTraining" + File.separator + imageDataset.getName() + "-" + featureExtractorLayer + "-train-0.bin");
//        if (!trainDir.isFile()) {
//            throw new IOException("No featurized data found. Run \"DatasetFeaturizer\" first to pre-save featurized data");
//        }
//    }
//}