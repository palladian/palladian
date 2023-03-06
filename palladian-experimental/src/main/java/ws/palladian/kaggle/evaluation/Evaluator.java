package ws.palladian.kaggle.evaluation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.classification.DatasetManager;
import ws.palladian.classification.text.BayesScorer;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.DefaultDataset;
import ws.palladian.core.value.ImmutableTextValue;
import ws.palladian.dataset.ImageDataset;
import ws.palladian.dataset.ImageValue;
import ws.palladian.features.*;
import ws.palladian.features.color.ColorExtractor;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.kaggle.restaurants.Experimenter;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.utils.CsvDatasetWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static ws.palladian.features.BoundsFeatureExtractor.BOUNDS;
import static ws.palladian.features.ColorFeatureExtractor.COLOR;
import static ws.palladian.features.EdginessFeatureExtractor.EDGINESS;
import static ws.palladian.features.FrequencyFeatureExtractor.FREQUENCY;
import static ws.palladian.features.RegionFeatureExtractor.REGION;
import static ws.palladian.features.color.HSB.*;
import static ws.palladian.features.color.Luminosity.LUMINOSITY;
import static ws.palladian.features.color.RGB.*;
import static ws.palladian.helper.functional.Predicates.or;
import static ws.palladian.helper.functional.Predicates.regex;

/**
 * Take a dataset and evaluate.
 *
 * @author Philipp Katz
 * @author David Urbansky
 */
public class Evaluator {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Evaluator.class);

    // private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    private static final int NUM_THREADS = 6;

    private static final class FeatureExtractionTask implements Runnable {

        private String basePath = "";
        private final Instance instance;
        private final Collection<FeatureExtractor> extractors;
        private final CsvDatasetWriter writer;
        private final ProgressReporter progressMonitor;

        FeatureExtractionTask(Instance instance, Collection<FeatureExtractor> extractors, CsvDatasetWriter writer, String basePath, ProgressReporter progressMonitor) {
            this.instance = instance;
            this.extractors = extractors;
            this.writer = writer;
            this.basePath = basePath;
            this.progressMonitor = progressMonitor;
        }

        @Override
        public void run() {
            InstanceBuilder instanceBuilder = new InstanceBuilder().add(instance.getVector());
            for (FeatureExtractor extractor : extractors) {
                String imagePath = String.valueOf(instance.getVector().get("image"));
                ImageValue imageValue = new ImageValue(new File(basePath + imagePath));
                try {
                    instanceBuilder.add(extractor.extract(imageValue.getImage()));
                } catch (Exception e) {
                    e.printStackTrace();
                    // TODO
                    System.err.println("problem with file " + imagePath + ", base path: " + basePath);
                }
            }
            synchronized (writer) {
                writer.append(instanceBuilder.create(instance.getCategory()));
                progressMonitor.increment();
            }
        }
    }

    public static void extractFeatures(Collection<FeatureExtractor> extractors, ImageDataset imageDataset, int type) throws IOException {

        File file = null;
        String stringType = "";
        if (type == ImageDataset.TEST) {
            stringType = "test";
            file = imageDataset.getTestFile();
        } else if (type == ImageDataset.TRAIN) {
            stringType = "train";
            file = imageDataset.getTrainFile();
        }

        File csvOutput = new File(imageDataset.getBasePath() + stringType + "-features.csv");

        CsvDatasetReaderConfig.Builder csvConfigBuilder = CsvDatasetReaderConfig.filePath(file);
        csvConfigBuilder.setFieldSeparator(imageDataset.getSeparator());
        Iterable<Instance> instances = csvConfigBuilder.create();

        extractFeatures(instances, csvOutput, extractors, imageDataset.getBasePath());
    }

    public static void extractFeatures(Iterable<Instance> instances, File csvOutput, Collection<FeatureExtractor> extractors, String basePath) throws IOException {

        FileHelper.delete(csvOutput);

        try (CsvDatasetWriter writer = new CsvDatasetWriter(csvOutput)) {
            ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
            ProgressReporter progressMonitor = new ProgressMonitor(0.01);
            int count = 0;
            for (Instance instance : instances) {
                executor.execute(new FeatureExtractionTask(instance, extractors, writer, basePath, progressMonitor));
                count++;
            }
            progressMonitor.startTask("Extracting features using " + extractors.size() + " extractors", count);
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void evaluateTrainTestSplits() throws IOException, JsonException {
        List<FeatureExtractor> extractors = new ArrayList<>();
        extractors.add(new BlockCodeExtractor());
        Predicate<String> blockCodeFeatures = regex("text");

        ImageDataset imageDataset = new ImageDataset(new File("E:\\Projects\\Programming\\Java\\WebKnox\\data\\temp\\images\\recipes50\\dataset.json"));

        File resultDirectory = new File(imageDataset.getBasePath() + "results-" + DateHelper.getCurrentDatetime());

        // for 10% to 90% training data
        for (int i = 10; i <= 90; i += 10) {
            DatasetManager.splitIndex(imageDataset.getBasePath() + "index.txt", i, imageDataset.getSeparator(), true);

            imageDataset.setTrainFilePath("train-" + i + ".txt");
            imageDataset.setTrainFilePath("test-" + (100 - i) + ".txt");

            //// read training data and create features
            extractFeatures(extractors, imageDataset, ImageDataset.TRAIN);

            //// read test data and create features
            extractFeatures(extractors, imageDataset, ImageDataset.TEST);

            // train and test
            CsvDatasetReaderConfig.Builder csvConfigBuilder = CsvDatasetReaderConfig.filePath(imageDataset.getTrainFeaturesFile());
            csvConfigBuilder.setFieldSeparator(";");
            csvConfigBuilder.parser("text", ImmutableTextValue.PARSER);
            Iterable<Instance> trainingInstances = csvConfigBuilder.create();
            csvConfigBuilder = CsvDatasetReaderConfig.filePath(imageDataset.getTestFeaturesFile());
            csvConfigBuilder.setFieldSeparator(";");
            csvConfigBuilder.parser("text", ImmutableTextValue.PARSER);
            Iterable<Instance> testingInstances = csvConfigBuilder.create();

            //            Experimenter experimenter = new Experimenter(trainingInstances, testingInstances, resultDirectory);
            //            List<Filter<String>> smallList = asList(blockCodeFeatures);
            //            experimenter.addClassifier(
            //                    new PalladianTextClassifier(FeatureSettingBuilder.words(1, 3).create(), new BayesScorer(
            //                            BayesScorer.Options.FREQUENCIES, BayesScorer.Options.LAPLACE, BayesScorer.Options.PRIORS)),
            //                    smallList);
        }

    }

    /**
     * Run multiple block code configurations to see what works best.
     */
    public static void runBlockCodeExperiments(ImageDataset imageDataset) throws IOException, JsonException {

        Predicate<String> blockCodeFeatures = regex("text");

        File resultDirectory = new File(imageDataset.getBasePath() + "blockcode-evaluation-results-" + DateHelper.getCurrentDatetime());

        // number of colors we want to normalize the image to
        BlockCodeExtractor.Colors[] numberOfColors = new BlockCodeExtractor.Colors[]{BlockCodeExtractor.Colors.TWENTY_EIGHT,
                BlockCodeExtractor.Colors.TWENTY_EIGHT_AND_BRIGHTNESS_2, BlockCodeExtractor.Colors.TWENTY_EIGHT_AND_BRIGHTNESS_3,
                BlockCodeExtractor.Colors.TWENTY_EIGHT_AND_BRIGHTNESS_4,
                //                BlockCodeExtractor.Colors.TWENTY_EIGHT_AND_BRIGHTNESS_6,
                BlockCodeExtractor.Colors.TWENTY_EIGHT_AND_BRIGHTNESS_8, BlockCodeExtractor.Colors.TWENTY_EIGHT_AND_BRIGHTNESS_10, BlockCodeExtractor.Colors.FIFTY_ONE};

        // number of pixels to cluster when pixelating the image
        int[] pixelationSizes = new int[]{5, 6, 7, 8, 9, 10, 11};

        // block size in pixels. This is basically the word size
        BlockCodeExtractor.BlockSize[] blockSizes = new BlockCodeExtractor.BlockSize[]{BlockCodeExtractor.BlockSize.ONE_BY_ONE, BlockCodeExtractor.BlockSize.TWO_BY_TWO,
                BlockCodeExtractor.BlockSize.THREE_BY_THREE};

        // image sections. Has to be a square number starting with 4
        BlockCodeExtractor.BlockSize[] imageSections = new BlockCodeExtractor.BlockSize[]{BlockCodeExtractor.BlockSize.TWO_BY_TWO, BlockCodeExtractor.BlockSize.THREE_BY_THREE,
                BlockCodeExtractor.BlockSize.FOUR_BY_FOUR};

        boolean[] includeNumberOfColors = new boolean[]{false, true};

        int combinations = numberOfColors.length * pixelationSizes.length * blockSizes.length * imageSections.length * includeNumberOfColors.length;

        LOGGER.info("block code experiments with " + combinations + " combinations");

        int c = 1;
        for (BlockCodeExtractor.Colors numberOfColor : numberOfColors) {
            for (int pixelationSize : pixelationSizes) {
                for (BlockCodeExtractor.BlockSize blockSize : blockSizes) {
                    for (BlockCodeExtractor.BlockSize imageSection : imageSections) {
                        for (boolean includeNumberOfColor : includeNumberOfColors) {

                            LOGGER.info("======================== running combination " + c + "/" + combinations + " ========================");

                            BlockCodeExtractor blockCodeExtractor = new BlockCodeExtractor(numberOfColor, pixelationSize, blockSize, imageSection, includeNumberOfColor);
                            List<FeatureExtractor> extractors = new ArrayList<>();
                            extractors.add(blockCodeExtractor);

                            //// read training data and create features
                            extractFeatures(extractors, imageDataset, ImageDataset.TRAIN);

                            //// read test data and create features
                            extractFeatures(extractors, imageDataset, ImageDataset.TEST);

                            // train and test
                            CsvDatasetReaderConfig.Builder csvConfigBuilder = CsvDatasetReaderConfig.filePath(imageDataset.getTrainFeaturesFile());
                            csvConfigBuilder.setFieldSeparator(";");
                            csvConfigBuilder.parser("text", ImmutableTextValue.PARSER);
                            Iterable<Instance> trainingInstances = csvConfigBuilder.create();
                            csvConfigBuilder = CsvDatasetReaderConfig.filePath(imageDataset.getTestFeaturesFile());
                            csvConfigBuilder.setFieldSeparator(";");
                            csvConfigBuilder.parser("text", ImmutableTextValue.PARSER);
                            Iterable<Instance> testingInstances = csvConfigBuilder.create();

                            //                            Experimenter experimenter = new Experimenter(trainingInstances, testingInstances,
                            //                                    resultDirectory);
                            //                            List<Filter<String>> smallList = asList(blockCodeFeatures);
                            //                            // experimenter.addClassifier(
                            //                            // new PalladianTextClassifier(FeatureSettingBuilder.words(1, 3).create()), smallList);
                            //                            experimenter.addClassifier(
                            //                                    new PalladianTextClassifier(FeatureSettingBuilder.words(1, 3).create(),
                            //                                            new BayesScorer(BayesScorer.Options.FREQUENCIES,
                            //                                                    BayesScorer.Options.LAPLACE, BayesScorer.Options.PRIORS)),
                            //                                    smallList);
                            //
                            //                            experimenter.setDescription("Number of colors: " + numberOfColor + ", pixelation size: "
                            //                                    + pixelationSize + ", block size: " + blockSize + ", image sections: "
                            //                                    + imageSection + ", include#colors: " + includeNumberOfColor);
                            //
                            //                            experimenter.run();
                            c++;
                        }
                    }
                }
            }
        }

    }

    public static void main(String[] args) throws IOException, JsonException {
        System.out.println("Run evaluator");
        ////// 1. path to dataset
        //         String datasetPath = "E:\\Projects\\Programming\\Java\\WebKnox\\data\\temp\\images\\recipes50\\dataset.json";
        //         String datasetPath = "D:\\PalladianData\\Datasets\\recipes50\\dataset.json";
        String datasetPath = "C:\\Workspace\\data\\recipes50\\dataset.json";
        //        String datasetPath = "/home/david/datasets/dataset.json";

        if (args.length > 0) {
            datasetPath = args[0];
        }

        ImageDataset imageDataset = new ImageDataset(new File(datasetPath));

        // evaluateTrainTestSplits();
        //        runBlockCodeExperiments(imageDataset);
        //        System.exit(0);

        ////// 2. adding feature extractors
        ColorExtractor[] colorExtractors = new ColorExtractor[]{LUMINOSITY, RED, GREEN, BLUE, HUE, SATURATION, BRIGHTNESS};
        List<FeatureExtractor> extractors = new ArrayList<>();
        extractors.add(new StatisticsFeatureExtractor(colorExtractors));
        extractors.add(new LocalFeatureExtractor(2, new StatisticsFeatureExtractor(colorExtractors)));
        extractors.add(new LocalFeatureExtractor(3, new StatisticsFeatureExtractor(colorExtractors)));
        extractors.add(new LocalFeatureExtractor(4, new StatisticsFeatureExtractor(colorExtractors)));
        extractors.add(BOUNDS);
        extractors.add(COLOR);
        extractors.add(new SymmetryFeatureExtractor(colorExtractors));
        extractors.add(REGION);
        extractors.add(FREQUENCY);
        extractors.add(new GridSimilarityExtractor(2));
        extractors.add(new GridSimilarityExtractor(3));
        extractors.add(new GridSimilarityExtractor(4));
        // extractors.add(new GridSimilarityExtractor(5));
        extractors.add(EDGINESS);

        extractors.clear();
        extractors.add(new BlockCodeExtractor());

        ////// 3. extract features for train and test set

        //// read training data and create features
        extractFeatures(extractors, imageDataset, ImageDataset.TRAIN);

        //// read test data and create features
        extractFeatures(extractors, imageDataset, ImageDataset.TEST);

        // System.exit(0);

        ////// 4. create training and test instances
        CsvDatasetReaderConfig.Builder csvConfigBuilder = CsvDatasetReaderConfig.filePath(imageDataset.getTrainFeaturesFile());
        csvConfigBuilder.parser("text", ImmutableTextValue.PARSER);
        csvConfigBuilder.setFieldSeparator(";");
        Iterable<Instance> trainingInstances = csvConfigBuilder.create();
        csvConfigBuilder = CsvDatasetReaderConfig.filePath(imageDataset.getTestFeaturesFile());
        csvConfigBuilder.parser("text", ImmutableTextValue.PARSER);
        Iterable<Instance> testingInstances = csvConfigBuilder.create();

        File resultDirectory = new File(imageDataset.getBasePath() + "results-" + DateHelper.getCurrentDatetime());
        DefaultDataset trainingDs = new DefaultDataset(trainingInstances);
        DefaultDataset testDs = new DefaultDataset(testingInstances);
        Experimenter experimenter = new Experimenter(trainingDs, testDs, resultDirectory);
        //        Experimenter experimenter = new Experimenter(trainingInstances, testingInstances, resultDirectory);

        ////// 5. decide which features and combinations should be evaluated
        Predicate<String> surfFeatures = regex("SURF.*");
        Predicate<String> siftFeatures = regex("SIFT.*");
        Predicate<String> boundsFeatures = regex("width|height|ratio");
        Predicate<String> colorFeatures = regex("main_color.*");
        Predicate<String> statisticsFeatures = regex("(?!(cell|4x4)-).*_(max|mean|min|range|stdDev|relStdDev|sum|count|\\d{2}-percentile)");
        Predicate<String> local4StatisticsFeatures = regex("cell-\\d/4.*");
        Predicate<String> local9StatisticsFeatures = regex("cell-\\d/9.*");
        Predicate<String> symmetryFeatures = regex("symmetry-.*");
        Predicate<String> edginessFeatures = regex("edginess-.*");
        Predicate<String> regionFeatures = regex(".*_region.*");
        Predicate<String> frequencyFeatures = regex("frequency-.*");
        Predicate<String> gridFeatures = regex("4x4-similarity_.*");
        Predicate<String> blockCodeFeatures = regex("text");

        Predicate<String> allQuantitativeFeatures = or(colorFeatures, statisticsFeatures, symmetryFeatures, local4StatisticsFeatures, local9StatisticsFeatures, regionFeatures,
                edginessFeatures, frequencyFeatures, gridFeatures);
        // Predicate<String> allFeatures = or(surfFeatures, siftFeatures, allQuantitativeFeatures);
        List<Predicate<String>> allCombinations = asList(colorFeatures, statisticsFeatures, symmetryFeatures, regionFeatures, frequencyFeatures, gridFeatures,
                allQuantitativeFeatures);

        //         List<Predicate<String>> smallList = asList(colorFeatures,statisticsFeatures);
        //         List<Predicate<String>> smallList = asList(allQuantitativeFeatures);
        List<Predicate<String>> smallList = asList(blockCodeFeatures);

        ////// 6. add classifiers with feature sets to the experiment and run it
        //         experimenter.withClassifier(QuickMlLearner.randomForest(100), new QuickMlClassifier(), allCombinations);
        //         experimenter.addClassifier(new PalladianDictionaryClassifier(), smallList);
        // experimenter.addClassifier(new PalladianTextClassifier(FeatureSettingBuilder.words(1, 3).create()),
        // smallList);
        // experimenter.addClassifier(new PalladianTextClassifier(FeatureSettingBuilder.words(1, 7).create()),
        // smallList);
        // experimenter.addClassifier(new PalladianTextClassifier(FeatureSettingBuilder.words(1, 3).create(), new
        // BayesScorer(BayesScorer.Options.COMPLEMENT, BayesScorer.Options.LAPLACE, BayesScorer.Options.PRIORS)),
        // smallList);
        // experimenter.addClassifier(new PalladianTextClassifier(FeatureSettingBuilder.words(1).create(), new
        // BayesScorer(BayesScorer.Options.FREQUENCIES, BayesScorer.Options.LAPLACE, BayesScorer.Options.PRIORS)),
        // smallList);
        // experimenter.addClassifier(new PalladianTextClassifier(FeatureSettingBuilder.words(1, 2).create(), new
        // BayesScorer(BayesScorer.Options.FREQUENCIES, BayesScorer.Options.LAPLACE, BayesScorer.Options.PRIORS)),
        // smallList);
        /////////////////////////////////////////////
        //        experimenter.addClassifier(
        //                new PalladianTextClassifier(FeatureSettingBuilder.words(1, 3).create(), new BayesScorer(
        //                        BayesScorer.Options.FREQUENCIES, BayesScorer.Options.LAPLACE, BayesScorer.Options.PRIORS)),
        //                smallList);
        /////////////////////////////////////////////
        experimenter.withClassifier(new PalladianTextClassifier(FeatureSettingBuilder.words(1, 4).create(),
                new BayesScorer(BayesScorer.Options.FREQUENCIES, BayesScorer.Options.LAPLACE, BayesScorer.Options.PRIORS)), smallList);
        // experimenter.addClassifier(new PalladianTextClassifier(FeatureSettingBuilder.words(1, 5).create(), new
        // BayesScorer(BayesScorer.Options.FREQUENCIES, BayesScorer.Options.LAPLACE, BayesScorer.Options.PRIORS)),
        // smallList);
        // experimenter.addClassifier(new PalladianTextClassifier(FeatureSettingBuilder.words(1, 3).create(), new
        // BayesScorer(BayesScorer.Options.COMPLEMENT,BayesScorer.Options.FREQUENCIES, BayesScorer.Options.LAPLACE,
        // BayesScorer.Options.PRIORS)), smallList);
        // experimenter.addClassifier(new PalladianTextClassifier(FeatureSettingBuilder.words(1, 3).create(), new
        // BayesScorer(BayesScorer.Options.COMPLEMENT,BayesScorer.Options.LAPLACE)), smallList);
        // experimenter.addClassifier(new NaiveBayesLearner(), new NaiveBayesClassifier(), smallList);
        // experimenter.addClassifier(new KnnLearner(), new KnnClassifier(), smallList);
        // experimenter.addClassifier(new ZeroRLearner(), new ZeroRClassifier(), asList(Filters.NONE));
        //         experimenter.withClassifier(new NaiveBayesLearner(), new NaiveBayesClassifier(), allCombinations);
        //         experimenter.addClassifier(QuickMlLearner.tree(), new QuickMlClassifier(), allCombinations);

        experimenter.run();
    }
}
