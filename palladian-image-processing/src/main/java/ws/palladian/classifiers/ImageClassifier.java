package ws.palladian.classifiers;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.distribution.NormalDistribution;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
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
import org.deeplearning4j.spark.api.TrainingMaster;
import org.deeplearning4j.spark.impl.graph.SparkComputationGraph;
import org.deeplearning4j.spark.impl.paramavg.ParameterAveragingTrainingMaster;
import org.deeplearning4j.util.ModelSerializer;
import org.deeplearning4j.zoo.PretrainedType;
import org.deeplearning4j.zoo.ZooModel;
import org.deeplearning4j.zoo.model.VGG16;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
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
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.parser.json.JsonException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.mashape.unirest.http.exceptions.UnirestException;

/**
 * An image classifier using a pretrained model and transfer learning.
 * 
 * @author David Urbansky
 */
public class ImageClassifier implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageClassifier.class);

    private ComputationGraph vgg16;
    private List<String> labels;

    /** Optional S3 credentials. */
    private AWSCredentials credentials = null;
    private static final String apiKeyKey = "api.amazon.s3.key";
    private static final String apiSecretKey = "api.amazon.s3.secret";

    /**
     * The default constructor will create a VGG16 classifier with ImageNet labels.
     */
    public ImageClassifier() {
        this(null, null, ImageNetLabels.getLabels());
    }

    public ImageClassifier(String h5Path, String jsonPath, List<String> labels) {
//        TrainedModelHelper helper = new TrainedModelHelper(TrainedModels.VGG16);
        ZooModel zooModel = new VGG16();
//        if (h5Path != null && jsonPath != null) {
//            helper.setPathToH5(h5Path);
//            helper.setPathToJSON(jsonPath);
//        }
        try {
            this.labels = labels;
            vgg16 = (ComputationGraph) zooModel.initPretrained(PretrainedType.IMAGENET);
//            vgg16 = helper.loadModel();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            // FIXME
            String apiKey = "";
            String apiSecret = "";
            credentials = new BasicAWSCredentials(apiKey, apiSecret);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ImageClassifier(String modelZipPath, List<String> labels) {

        File locationToSave = new File(modelZipPath);
        try {
            this.labels = labels;
            vgg16 = ModelSerializer.restoreComputationGraph(locationToSave);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            // FIXME
            String apiKey = "";
            String apiSecret = "";
            credentials = new BasicAWSCredentials(apiKey, apiSecret);
        } catch (Exception e) {
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
    public void transferLearnNew(ImageDataset imageDataset, File modelPath, int trainPercentage)
            throws IOException, JsonException, UnsupportedKerasConfigurationException, InvalidKerasConfigurationException {

        StopWatch stopWatch = new StopWatch();

        // first we'll featurize the dataset
        DatasetFeaturizer featurizeDataset = new DatasetFeaturizer();
        featurizeDataset.featurizeDataset(imageDataset, trainPercentage);

        // import vgg base model
        // Note that the model imported does not have an output layer (check printed summary)
        // nor any training related configs (model from keras was imported with only weights and json)
        ZooModel zooModel = new VGG16();
        LOGGER.info("loading vgg16...");
        ComputationGraph vgg16 = (ComputationGraph) zooModel.initPretrained(PretrainedType.IMAGENET);
        LOGGER.info("...loaded in " + stopWatch.getElapsedTimeStringAndIncrement());

        // decide on a fine tune configuration to use.
        // in cases where there already exists a setting the fine tune setting will override the setting for all layers that are not "frozen"
        long seed = 12345;
        int nEpochs = 3;
        int numClasses = imageDataset.getNumberOfClasses();
        FineTuneConfiguration fineTuneConf = new FineTuneConfiguration.Builder()
                .learningRate(5e-5)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(Updater.NESTEROVS)
                .seed(seed)
                .build();

//        ComputationGraph vgg16Transfer = new TransferLearning.GraphBuilder(vgg16)
//                .fineTuneConfiguration(fineTuneConf)
//                .setFeatureExtractor("fc2")
//                .removeVertexKeepConnections("predictions")
//                .addLayer("predictions",
//                        new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
//                                .nIn(4096).nOut(numClasses)
//                                .weightInit(WeightInit.XAVIER)
//                                .activation(Activation.SOFTMAX).build(), "fc2")
//                .build();

        ComputationGraph vgg16Transfer = new TransferLearning.GraphBuilder(vgg16)
                .fineTuneConfiguration(fineTuneConf)
                .setFeatureExtractor("block5_pool")
                .nOutReplace("fc2",1024, WeightInit.XAVIER)
                .removeVertexAndConnections("predictions")
                .addLayer("fc3",new DenseLayer.Builder()
                        .activation(Activation.RELU)
                        .nIn(1024).nOut(256).build(),"fc2")
                .addLayer("newpredictions",new OutputLayer
                        .Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .activation(Activation.SOFTMAX)
                        .nIn(256).nOut(numClasses).build(),"fc3")
                        .setOutputs("newpredictions")
                        .build();


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

    /**
     * Use a VGG16 base model and exchange the output layer with a new one. Steps are:
     * 1. Featurize the dataset.
     * 2. Transfer learning, aka fit the model to the new dataset.
     * 3. Save the model.
     */
    public void transferLearn(ImageDataset imageDataset, File modelPath, int trainPercentage)
            throws IOException, JsonException, UnsupportedKerasConfigurationException, InvalidKerasConfigurationException {

        StopWatch stopWatch = new StopWatch();

        // first we'll featurize the dataset
        DatasetFeaturizer featurizeDataset = new DatasetFeaturizer();
        featurizeDataset.featurizeDataset(imageDataset, trainPercentage);

        // import vgg base model
        // Note that the model imported does not have an output layer (check printed summary)
        // nor any training related configs (model from keras was imported with only weights and json)
//        ZooModel zooModel = new VGG16();
//        Model vgg16 = zooModel.initPretrained(PretrainedType.IMAGENET);

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

    /**
     * Use a VGG16 base model and exchange the output layer with a new one. Steps are:
     * 1. Featurize the dataset.
     * 2. Transfer learning, aka fit the model to the new dataset.
     * 3. Save the model.
     */
    public void transferLearnOnSpark(ImageDataset imageDataset, File modelPath, int trainPercentage)
            throws IOException, JsonException, UnsupportedKerasConfigurationException, InvalidKerasConfigurationException {
        transferLearnOnSpark(imageDataset, modelPath, trainPercentage, null, null);
    }

    public void transferLearnOnSpark(ImageDataset imageDataset, File modelPath, int trainPercentage, String s3AccessKey, String s3SecretKey)
            throws IOException, JsonException, UnsupportedKerasConfigurationException, InvalidKerasConfigurationException {

        StopWatch stopWatch = new StopWatch();

        // first we'll featurize the dataset
//        FIXME DatasetFeaturizer featurizeDataset = new DatasetFeaturizer();
//        FIXME trying this without for now featurizeDataset.featurizeDataset(imageDataset, trainPercentage);

        // import vgg base model
        // Note that the model imported does not have an output layer (check printed summary)
        // nor any training related configs (model from keras was imported with only weights and json)
//        TrainedModelHelper modelImportHelper = new TrainedModelHelper(TrainedModels.VGG16);
//        LOGGER.info("loading vgg16...");
//        ComputationGraph vgg16 = modelImportHelper.loadModel();
//        LOGGER.info("...loaded");
        ZooModel zooModel = new VGG16();
        LOGGER.info("loading vgg16...");
        ComputationGraph vgg16 = (ComputationGraph) zooModel.initPretrained(PretrainedType.IMAGENET);
        LOGGER.info("...loaded in " + stopWatch.getElapsedTimeStringAndIncrement());
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

        LOGGER.info("finished dataset iterators");

        // Instantiate the transfer learning helper to fit and output from the featurized dataset
        // The .unfrozenGraph() is the unfrozen subset of the computation graph passed in.
        // If using with a UI or a listener attach them directly to the unfrozenGraph instance
        // With each iteration updated params from unfrozenGraph are copied over to the original model
        TransferLearningHelper transferLearningHelper = new TransferLearningHelper(vgg16Transfer);
        // LOGGER.info(transferLearningHelper.unfrozenGraph().summary());

        //////////////////////////////////////////////////////
        int batchSizePerWorker = 16;
        boolean useSparkLocal = false; // FIXME
        String hdfsRoot = "/hdfstemp";

        // Configuration for Spark training: see http://deeplearning4j.org/spark for explanation of these configuration options
        LOGGER.info("configuring for spark training..");
        TrainingMaster tm = new ParameterAveragingTrainingMaster.Builder(batchSizePerWorker) // Each DataSet object: contains (by default) 32 examples
                .averagingFrequency(5).workerPrefetchNumBatches(2) // Async prefetching: 2 examples per worker
                .batchSizePerWorker(batchSizePerWorker).build();

        LOGGER.info(vgg16Transfer.summary());
        SparkConf sparkConf = new SparkConf();
        if (useSparkLocal) {
            sparkConf.setMaster("local[*]");
        }
        sparkConf.setAppName("vgg16");
        JavaSparkContext sc = new JavaSparkContext(sparkConf);

        if (s3AccessKey != null && s3SecretKey != null) {
            sc.hadoopConfiguration().set("fs.s3a.access.key", s3AccessKey);
            sc.hadoopConfiguration().set("fs.s3a.secret.key", s3SecretKey);
        }

        FileSystem fs = FileSystem.get(sc.hadoopConfiguration());

        SparkComputationGraph sparkComputationGraph = new SparkComputationGraph(sc, transferLearningHelper.unfrozenGraph(), tm);

        LOGGER.info("Writing train to hdfs");
        int trainCountWrote = 0;
        while (trainIter.hasNext()) {
            OutputStream os = fs.create(new Path(hdfsRoot + "/" + "train", "dataset" + trainCountWrote++));
            trainIter.next().save(os);
            os.close();
        }

        LOGGER.info("Writing test to hdfs");
        String testDir = hdfsRoot + "/" + "test";
        int testCountWrote = 0;
        while (testIter.hasNext()) {
            OutputStream os = fs.create(new Path(testDir, "dataset" + testCountWrote++));
            testIter.next().save(os);
            os.close();
        }

        for (int epoch = 0; epoch < nEpochs; epoch++) {
            sparkComputationGraph.fit(hdfsRoot + "/train");
            LOGGER.info("Epoch #" + epoch + " complete");
        }

        // JavaRDD<DataSet> data = sc.binaryFiles(testDir + "/*").map(new Function<Tuple2<String, PortableDataStream>, DataSet>() {
        // @Override
        // public DataSet call(Tuple2<String, PortableDataStream> v1) throws Exception {
        // DataSet d = new DataSet();
        // d.load(v1._2().open());
        // return d;
        // }
        // });
        JavaRDD<DataSet> data = sc.binaryFiles(testDir + "/*").map(new SparkFunction());

        Evaluation eval = sparkComputationGraph.evaluate(data);
        LOGGER.info("Eval stats BEFORE fit.....");
        LOGGER.info(eval.stats() + "\n");
        testIter.reset();
//
//        IEvaluateFlatMapFunction<Evaluation> evalFn = new IEvaluateFlatMapFunction<>(sc.broadcast(vgg16.getConfiguration().toJson()),
//                sc.broadcast(sparkComputationGraph.getNetwork().params()), batchSizePerWorker, new Evaluation(numClasses));
//        JavaRDD<Evaluation> evaluations = data.mapPartitions(evalFn);
//        evaluations.reduce(new IEvaluationReduceFunction<>());
//        Evaluation eval = sparkComputationGraph.getNetwork().evaluate(testIter);
//        LOGGER.info("Eval stats BEFORE fit.....");
//        LOGGER.info(eval.stats() + "\n");
//        testIter.reset();
        //////////////////////////////////////////////////////

        for (int epoch = 0; epoch < nEpochs; epoch++) {
            if (epoch == 0) {
                eval = transferLearningHelper.unfrozenGraph().evaluate(testIter);
                LOGGER.info("Evaluation stats BEFORE fit:");
                LOGGER.info(eval.stats() + "\n");
                testIter.reset();
            }
            int iter = 0;
            while (trainIter.hasNext()) {
                transferLearningHelper.fitFeaturized(trainIter.next());
                if (iter % 10 == 0) {
                    LOGGER.info("Evaluate model at iteration " + iter + ":");
                    eval = transferLearningHelper.unfrozenGraph().evaluate(testIter);
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

    private void downloadS3File(String bucketName, String key, String targetPath) {
        AmazonS3 s3Client = new AmazonS3Client(new AWSStaticCredentialsProvider(credentials));
        S3Object object = s3Client.getObject(new GetObjectRequest(bucketName, key));
        InputStream objectData = object.getObjectContent();

        try {
            FileHelper.createDirectoriesAndFile(targetPath);
            Files.deleteIfExists(Paths.get(targetPath));
            Files.copy(objectData, Paths.get(targetPath));
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }

        try {
            objectData.close();
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }

        LOGGER.info("copied " + bucketName + "/" + key + " to " + targetPath);
    }

    public void downloadS3Dataset(String bucketName, String key, String targetPath) throws IOException {

//        try {
            downloadS3File(bucketName, "datasets/recipes50/dataset.json", "datasets/recipes50/dataset.json");
            downloadS3File(bucketName, key, targetPath);
//            ImageDataset imageDataset = new ImageDataset(new File(targetPath));

            // unzip the dataset
            FileHelper.unzip(targetPath, FileHelper.getFilePath(targetPath));

            // "foldered"
//            File parentDir = new File(imageDataset.getFolderedPath());
//
//            AmazonS3 s3Client = new AmazonS3Client(new AWSStaticCredentialsProvider(credentials));
//            ObjectListing folderedDirectories = s3Client.listObjects(bucketName, "datasets/recipes50/foldered");
//            do {
//                int i = 1;
//                ProgressMonitor progressMonitor = new ProgressMonitor(folderedDirectories.getObjectSummaries().size(), 1., "Downloading Dataset " + i);
//                for (S3ObjectSummary s3ObjectSummary : folderedDirectories.getObjectSummaries()) {
//                    downloadS3File(bucketName, s3ObjectSummary.getKey(), s3ObjectSummary.getKey());
//                    progressMonitor.incrementAndPrintProgress();
//                }
//                folderedDirectories = s3Client.listNextBatchOfObjects(folderedDirectories);
//
//                i++;
//            } while (folderedDirectories != null && !folderedDirectories.getObjectSummaries().isEmpty());

//        } catch (IOException | JsonException e) {
//            e.printStackTrace();
//            LOGGER.error(e.getMessage());
//        }

        LOGGER.info("copied " + bucketName + "/" + key + " to " + targetPath);
    }

    public static void main(String[] args) throws IOException, InvalidKerasConfigurationException, UnsupportedKerasConfigurationException, JsonException, UnirestException {

//        ImageDataset imageDataset1 = new ImageDataset(new File("F:\\PalladianData\\Datasets\\recipes50\\dataset.json"));
//        ImageClassifier newImageClassifier1 = new ImageClassifier();
//        File newModelPath1 = new File("recipe50-model.zip");
//        newImageClassifier1.transferLearn(imageDataset1, newModelPath1, 80);
//        System.exit(0);
         System.out.println(new File(".").getAbsolutePath());

        String bucketName = "webknox-dl4j";
        String datasetPath = "datasets/recipes50/dataset.json";
//        String datasetZip = "datasets/recipes50/foldered.zip";
        String datasetZip = "datasets/recipes50.zip";
        String modelPath = "models/";

        if (args.length > 0) {
            datasetPath = args[0];
            modelPath = args[1];
        }

        ImageClassifier newImageClassifier = new ImageClassifier();
//        newImageClassifier.downloadS3Dataset(bucketName, datasetPath, datasetPath);
        newImageClassifier.downloadS3Dataset(bucketName, datasetZip, "datasets/recipes50/recipes50.zip");
//        System.exit(0);

        // System.setProperty("hadoop.home.dir", "D:\\software\\hadoop-2.8.0\\");
        // ImageDataset imageDataset = new ImageDataset(new File("F:\\PalladianData\\Datasets\\recipes50\\dataset.json"));
        ImageDataset imageDataset = new ImageDataset(new File(datasetPath));
        // ImageDataset imageDataset = new ImageDataset(new File("F:\\PalladianData\\Datasets\\spoonacular-menu-items\\dataset.json"));
        // ImageDataset imageDataset = new ImageDataset(new File("F:\\PalladianData\\Datasets\\spoonacular-menu-items\\dataset.json"));
        // File newModelPath = new File("data\\models\\deeplearning4j\\" + imageDataset.getName() + ".zip");
        FileHelper.createDirectory(modelPath);
        File newModelPath = new File(modelPath + imageDataset.getName() + ".zip");

        // newImageClassifier.transferLearn(imageDataset, newModelPath, 80);
//        System.exit(0);
//
//        ImageClassifier imageClassifier = new ImageClassifier(newModelPath.getPath(), imageDataset.getClassNames());
        // ImageClassifier imageClassifier = new ImageClassifier("data\\models\\deeplearning4j\\flower-model.zip",
        // Arrays.asList("daisy", "dandelion", "roses", "sunflowers", "tulips"));

//        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
//
//        while (true) {
//            System.out.println("url:");
//            String imageUrl = in.readLine();
//            String filePath = ImageHandler.downloadAndSave(imageUrl, "");
//
//            File file = new File(filePath);
//            CategoryEntries categoryEntries = imageClassifier.classify(file);
//            file.delete();
//
//            for (Category categoryEntry : categoryEntries) {
//                System.out.println(categoryEntry.getName() + " : " + categoryEntry.getProbability());
//            }
//        }

    }

}
