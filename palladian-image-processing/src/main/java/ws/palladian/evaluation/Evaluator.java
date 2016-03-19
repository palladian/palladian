package ws.palladian.evaluation;

import ws.palladian.classification.nb.NaiveBayesClassifier;
import ws.palladian.classification.nb.NaiveBayesLearner;
import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.dataset.ImageDataset;
import ws.palladian.dataset.ImageValue;
import ws.palladian.features.FeatureExtractor;
import ws.palladian.features.StatisticsFeatureExtractor;
import ws.palladian.features.color.ColorExtractor;
import ws.palladian.features.color.Luminosity;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.utils.CsvDatasetWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ws.palladian.helper.functional.Filters.or;
import static ws.palladian.helper.functional.Filters.regex;

/**
 * Take a dataset and evaluate.
 *
 * @author Philipp Katz
 * @author David Urbansky
 */
public class Evaluator {

    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();

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
                    // FIXME
                    System.err.println("problem with file "  + imagePath);
                }
            }
            synchronized (writer) {
                writer.append(instanceBuilder.create(instance.getCategory()));
                progressMonitor.increment();
            }
        }
    }

    public static void extractFeatures(Collection<FeatureExtractor> extractors, ImageDataset imageDataset, int type)
            throws IOException {

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

    public static void extractFeatures(Iterable<Instance> instances, File csvOutput, Collection<FeatureExtractor> extractors, String basePath)
            throws IOException {

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

    public static void main(String[] args) throws IOException, JsonException {

        ImageDataset imageDataset = new ImageDataset(new File("E:\\Projects\\Programming\\Java\\WebKnox\\data\\temp\\images\\recipes50\\dataset.json"));

        ColorExtractor[] colorExtractors = new ColorExtractor[]{Luminosity.LUMINOSITY};
        List<FeatureExtractor> extractors = new ArrayList<>();
        extractors.add(new StatisticsFeatureExtractor(colorExtractors));

        //// read training data and create features
//        extractFeatures(extractors, imageDataset, ImageDataset.TRAIN);

        //// read test data and create features
//        extractFeatures(extractors, imageDataset, ImageDataset.TEST);

        // train and test
        CsvDatasetReaderConfig.Builder csvConfigBuilder = CsvDatasetReaderConfig.filePath(imageDataset.getTrainFeaturesFile());
        csvConfigBuilder.setFieldSeparator(";");
        Iterable<Instance> trainingInstances = csvConfigBuilder.create();
        csvConfigBuilder = CsvDatasetReaderConfig.filePath(imageDataset.getTestFeaturesFile());
        Iterable<Instance> testingInstances = csvConfigBuilder.create();

        File resultDirectory = new File(imageDataset.getBasePath() + "results-" + DateHelper.getCurrentDatetime());
        Experimenter experimenter = new Experimenter(trainingInstances, testingInstances, resultDirectory);

        Filter<String> surfFeatures = regex("SURF.*");
        Filter<String> siftFeatures = regex("SIFT.*");
        Filter<String> boundsFeatures = regex("width|height|ratio");
        Filter<String> colorFeatures = regex("main_color.*");
        Filter<String> statisticsFeatures = regex("(?!(cell|4x4)-).*_(max|mean|min|range|stdDev|relStdDev|sum|count|\\d{2}-percentile)");
        Filter<String> local4StatisticsFeatures = regex("cell-\\d/4.*");
        Filter<String> local9StatisticsFeatures = regex("cell-\\d/9.*");
        Filter<String> symmetryFeatures = regex("symmetry-.*");
        Filter<String> regionFeatures = regex(".*_region.*");
        Filter<String> frequencyFeatures = regex("frequency-.*");
        Filter<String> gridFeatures = regex("4x4-similarity_.*");

        Filter<String> allQuantitativeFeatures = or(boundsFeatures, colorFeatures, statisticsFeatures, symmetryFeatures, local4StatisticsFeatures, local9StatisticsFeatures, regionFeatures, frequencyFeatures, gridFeatures);
        Filter<String> allFeatures = or(surfFeatures, siftFeatures, allQuantitativeFeatures);
        List<Filter<String>> allCombinations = asList(surfFeatures, siftFeatures, boundsFeatures, colorFeatures, statisticsFeatures, symmetryFeatures, regionFeatures, frequencyFeatures, gridFeatures, allQuantitativeFeatures, allFeatures);

//		experimenter.addClassifier(new ZeroRLearner(), new ZeroRClassifier(), asList(Filters.NONE));
		experimenter.addClassifier(new NaiveBayesLearner(), new NaiveBayesClassifier(), asList(allQuantitativeFeatures));
//		experimenter.addClassifier(new NaiveBayesLearner(), new NaiveBayesClassifier(), allCombinations);
//		experimenter.addClassifier(new KnnLearner(), new KnnClassifier(), allCombinations);
//		experimenter.addClassifier(QuickMlLearner.tree(), new QuickMlClassifier(), allCombinations);
//		experimenter.addClassifier(QuickMlLearner.randomForest(100), new QuickMlClassifier(), asList(siftFeatures));

        experimenter.run();
    }
}
