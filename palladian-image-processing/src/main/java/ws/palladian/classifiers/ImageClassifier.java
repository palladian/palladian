package ws.palladian.classifiers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.distribution.NormalDistribution;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.modelimport.keras.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.UnsupportedKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.trainedmodels.TrainedModelHelper;
import org.deeplearning4j.nn.modelimport.keras.trainedmodels.TrainedModels;
import org.deeplearning4j.nn.modelimport.keras.trainedmodels.Utils.ImageNetLabels;
import org.deeplearning4j.nn.transferlearning.FineTuneConfiguration;
import org.deeplearning4j.nn.transferlearning.TransferLearning;
import org.deeplearning4j.nn.transferlearning.TransferLearningHelper;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.VGG16ImagePreProcessor;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.ImmutableCategory;
import ws.palladian.core.ImmutableCategoryEntries;
import ws.palladian.dataset.DatasetFeaturizer;
import ws.palladian.dataset.DatasetIteratorFeaturized;
import ws.palladian.dataset.ImageDataset;
import ws.palladian.extraction.multimedia.ImageHandler;
import ws.palladian.helper.StopWatch;
import ws.palladian.retrieval.parser.json.JsonException;

/**
 * An image classifier using a pretrained model and transfer learning.
 * 
 * @author David Urbansky
 */
public class ImageClassifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageClassifier.class);

    private ComputationGraph vgg16;
    private List<String> labels;

    /**
     * The default constructor will create a VGG16 classifier with ImageNet labels.
     */
    public ImageClassifier() {
        this(null, null, ImageNetLabels.getLabels());
    }

    private ImageClassifier(String h5Path, String jsonPath, List<String> labels) {
        TrainedModelHelper helper = new TrainedModelHelper(TrainedModels.VGG16);
        if (h5Path != null && jsonPath != null) {
            helper.setPathToH5(h5Path);
            helper.setPathToJSON(jsonPath);
        }
        try {
            this.labels = labels;
            vgg16 = helper.loadModel();
        } catch (IOException | InvalidKerasConfigurationException | UnsupportedKerasConfigurationException e) {
            e.printStackTrace();
        }
    }

    private ImageClassifier(String modelZipPath, List<String> labels) {

        File locationToSave = new File(modelZipPath);
        try {
            this.labels = labels;
            vgg16 = ModelSerializer.restoreComputationGraph(locationToSave);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CategoryEntries classify(File file) {
        return classify(file, 10);
    }

    public CategoryEntries classify(File file, int maxCategoryEntries) {

        if (maxCategoryEntries < 1) {
            maxCategoryEntries = 1;
        }

        // convert file to INDArray
        NativeImageLoader loader = new NativeImageLoader(224, 224, 3);

        INDArray image;
        Category mostLikely = new ImmutableCategory("unknown", 0.);
        try {
            image = loader.asMatrix(file);
        } catch (IOException e) {
            e.printStackTrace();
            return new ImmutableCategoryEntries(new HashMap<>(), mostLikely);
        }

        // mean subtraction pre-processing step for VGG
        DataNormalization scaler = new VGG16ImagePreProcessor();
        scaler.transform(image);

        // Inference returns array of INDArray, index[0] has the predictions
        INDArray[] output = vgg16.output(false, image);

        INDArray indArray = output[0];

        int i = 0;
        int[] topX = new int[maxCategoryEntries];
        float[] topXProb = new float[maxCategoryEntries];

        Map<String, Category> entryMap = new LinkedHashMap<>();
        for (int batch = 0; batch < indArray.size(0); ++batch) {

            for (INDArray currentBatch = indArray.getRow(batch).dup(); i < maxCategoryEntries; ++i) {
                topX[i] = Nd4j.argMax(currentBatch, new int[] {1}).getInt(new int[] {0, 0});
                topXProb[i] = currentBatch.getFloat(batch, topX[i]);
                currentBatch.putScalar(0, topX[i], 0.0D);

                ImmutableCategory category = new ImmutableCategory(labels.get(topX[i]), topXProb[i]);
                entryMap.put(labels.get(topX[i]), category);
                if (i == 0) {
                    mostLikely = category;
                }

            }
        }

        // // convert 1000 length numeric index of probabilities per label
        // // to sorted return top 5 convert to string using helper function VGG16.decodePredictions
        // // "predictions" is string of our results
        // String predictions = TrainedModels.VGG16.decodePredictions(output[0]);

        return new ImmutableCategoryEntries(entryMap, mostLikely);
    }

    /**
     * Use a VGG16 base model and exchange the output layer with a new one. Steps are:
     * 1. Featurize the dataset.
     * 2. Transfer learning, aka fit the model to the new dataset.
     * 3. Save the model.
     */
    public void transferLearn(ImageDataset imageDataset, File modelPath)
            throws IOException, JsonException, UnsupportedKerasConfigurationException, InvalidKerasConfigurationException {

        StopWatch stopWatch = new StopWatch();

        // first we'll featurize the dataset
        DatasetFeaturizer featurizeDataset = new DatasetFeaturizer();
        featurizeDataset.featurizeDataset(imageDataset, 80);

        // import vgg base model
        // Note that the model imported does not have an output layer (check printed summary)
        // nor any training related configs (model from keras was imported with only weights and json)
        TrainedModelHelper modelImportHelper = new TrainedModelHelper(TrainedModels.VGG16);
        LOGGER.info("loading vgg16...");
        ComputationGraph vgg16 = modelImportHelper.loadModel();
        LOGGER.info("...loaded");
        // LOGGER.info(vgg16.summary());

        // decide on a fine tune configuration to use.
        // in cases where there already exists a setting the fine tune setting will override the setting for all layers that are not "frozen"
        long seed = 12345;
        int nEpochs = 3;
        int numClasses = imageDataset.getNumberOfClasses();
        FineTuneConfiguration fineTuneConf = new FineTuneConfiguration.Builder().learningRate(3e-5).optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(Updater.NESTEROVS).seed(seed).build();

        // construct a new model with the intended architecture and print summary
        ComputationGraph vgg16Transfer = new TransferLearning.GraphBuilder(vgg16).fineTuneConfiguration(fineTuneConf)
                .setFeatureExtractor(DatasetFeaturizer.featurizeExtractionLayer) // the specified
                // layer and below are "frozen"
                .removeVertexKeepConnections("predictions") // replace the functionality of the final vertex
                .addLayer("predictions",
                        new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD).nIn(4096).nOut(numClasses).weightInit(WeightInit.DISTRIBUTION)
                                .dist(new NormalDistribution(0, 0.2 * (2.0 / (4096 + numClasses)))) // This weight init dist gave better results than Xavier
                                .activation(Activation.SOFTMAX).build(),
                        "fc2")
                .build();
                // LOGGER.info(vgg16Transfer.summary());

        // create iterators for the featurized dataset
        DatasetIteratorFeaturized datasetIteratorFeaturized = new DatasetIteratorFeaturized(imageDataset, DatasetFeaturizer.featurizeExtractionLayer);
        DataSetIterator trainIter = datasetIteratorFeaturized.getTrainingIterator();
        DataSetIterator testIter = datasetIteratorFeaturized.getTestingIterator();

        // Instantiate the transfer learning helper to fit and output from the featurized dataset
        // The .unfrozenGraph() is the unfrozen subset of the computation graph passed in.
        // If using with a UI or a listener attach them directly to the unfrozenGraph instance
        // With each iteration updated params from unfrozenGraph are copied over to the original model
        TransferLearningHelper transferLearningHelper = new TransferLearningHelper(vgg16Transfer);
        // LOGGER.info(transferLearningHelper.unfrozenGraph().summary());

        for (int epoch = 0; epoch < nEpochs; epoch++) {
            if (epoch == 0) {
                Evaluation eval = transferLearningHelper.unfrozenGraph().evaluate(testIter);
                LOGGER.info("Evaluation stats BEFORE fit:");
                LOGGER.info(eval.stats() + "\n");
                testIter.reset();
            }
            int iter = 0;
            while (trainIter.hasNext()) {
                transferLearningHelper.fitFeaturized(trainIter.next());
                if (iter % 10 == 0) {
                    LOGGER.info("Evaluate model at iteration " + iter + ":");
                    Evaluation eval = transferLearningHelper.unfrozenGraph().evaluate(testIter);
                    LOGGER.info(eval.stats());
                    testIter.reset();
                }
                iter++;
            }
            trainIter.reset();
            LOGGER.info("epoch #" + epoch + " complete");
        }
        LOGGER.info("model build complete, saving now...");

        // Save the model
        // Note that the saved model will not know which layers were frozen during training.
        // Frozen models always have to specified before training.
        // Models with frozen layers can be constructed in the following two ways:
        // 1. .setFeatureExtractor in the transfer learning API which will always a return a new model (as seen in this example)
        // 2. in place with the TransferLearningHelper constructor which will take a model, and a specific vertexname
        // and freeze it and the vertices on the path from an input to it (as seen in the FeaturizePreSave class)
        // The saved model can be "fine-tuned" further as in the class "FitFromFeaturized"
        boolean saveUpdater = false;
        ModelSerializer.writeModel(vgg16Transfer, modelPath, saveUpdater);

        LOGGER.info("...model saved successfully, total time " + stopWatch.getElapsedTimeString());
    }

    public static void main(String[] args) throws IOException, InvalidKerasConfigurationException, UnsupportedKerasConfigurationException, JsonException {

        ImageDataset imageDataset = new ImageDataset(new File("F:\\PalladianData\\Datasets\\recipes50\\dataset.json"));
        File newModelPath = new File("data\\models\\deeplearning4j\\" + imageDataset.getName() + ".zip");
//        ImageClassifier newImageClassifier = new ImageClassifier();
//        newImageClassifier.transferLearn(imageDataset, newModelPath);

        ImageClassifier imageClassifier = new ImageClassifier(newModelPath.getPath(), imageDataset.getClassNames());
        // ImageClassifier imageClassifier = new ImageClassifier("data\\models\\deeplearning4j\\flower-model.zip",
        // Arrays.asList("daisy", "dandelion", "roses", "sunflowers", "tulips"));

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));

        while (true) {
            System.out.println("url:");
            String imageUrl = in.readLine();
            String filePath = ImageHandler.downloadAndSave(imageUrl, "");

            File file = new File(filePath);
            CategoryEntries categoryEntries = imageClassifier.classify(file);
            file.delete();

            for (Category categoryEntry : categoryEntries) {
                System.out.println(categoryEntry.getName() + " : " + categoryEntry.getProbability());
            }
        }

    }

}
