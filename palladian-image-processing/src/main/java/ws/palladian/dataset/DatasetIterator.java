//package ws.palladian.dataset;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.Random;
//
//import org.datavec.api.io.filters.BalancedPathFilter;
//import org.datavec.api.io.labels.ParentPathLabelGenerator;
//import org.datavec.api.split.FileSplit;
//import org.datavec.api.split.InputSplit;
//import org.datavec.image.loader.BaseImageLoader;
//import org.datavec.image.recordreader.ImageRecordReader;
//import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
//import org.deeplearning4j.nn.modelimport.keras.trainedmodels.TrainedModels;
//import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * An iterator on a folder of subfolders of images.
// *
// * @author David Urbansky
// */
//public class DatasetIterator {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetIterator.class);
//
//    private static final String[] ALLOWED_EXTENSIONS = BaseImageLoader.ALLOWED_FORMATS;
//    private static final Random rng = new Random(13);
//
//    private ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();
//    private InputSplit trainData, testData;
//    private int batchSize;
//    private ImageDataset imageDataset;
//
//    public DatasetIterator(ImageDataset imageDataset, int batchSize) {
//        this.batchSize = batchSize;
//        this.imageDataset = imageDataset;
//    }
//
//    public DataSetIterator getTrainingIterator() throws IOException {
//        return makeIterator(trainData);
//
//    }
//
//    public DataSetIterator getTestingIterator() throws IOException {
//        return makeIterator(testData);
//
//    }
//
//    public void setup(int trainingPercentage) throws IOException {
//        File parentDir = new File(imageDataset.getFolderedPath());
//        FileSplit filesInDir = new FileSplit(parentDir, ALLOWED_EXTENSIONS, rng);
//        BalancedPathFilter pathFilter = new BalancedPathFilter(rng, ALLOWED_EXTENSIONS, labelMaker);
//
//        if (trainingPercentage <= 0) {
//            throw new IllegalArgumentException(
//                    "Percentage of data set aside for training has to be less more than 0%. Test percentage = 100 - training percentage, has to be greater than 0");
//        } else if (trainingPercentage >= 100) {
//            throw new IllegalArgumentException(
//                    "Percentage of data set aside for training has to be less than 100%. Test percentage = 100 - training percentage, has to be greater than 0");
//        }
//
//        InputSplit[] filesInDirSplit = filesInDir.sample(pathFilter, trainingPercentage, 100 - trainingPercentage);
//        trainData = filesInDirSplit[0];
//        testData = filesInDirSplit[1];
//    }
//
//    private DataSetIterator makeIterator(InputSplit split) throws IOException {
//        // height, width, and channels should stay like this for the VGG16 model
//        int height = 224;
//        int width = 224;
//        int channels = 3;
//
//        ImageRecordReader recordReader = new ImageRecordReader(height, width, channels, labelMaker);
//        recordReader.initialize(split);
//        DataSetIterator iter = new RecordReaderDataSetIterator(recordReader, batchSize, 1, imageDataset.getNumberOfClasses());
//        iter.setPreProcessor(TrainedModels.VGG16.getPreProcessor());
//        return iter;
//    }
//
//}