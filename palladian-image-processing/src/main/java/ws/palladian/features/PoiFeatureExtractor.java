package ws.palladian.features;

import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.clustering.Cluster;
import ws.palladian.clustering.Clusterer;
import ws.palladian.clustering.CommonsKMeansClusterer;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.extraction.multimedia.ImageHandler;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.StopWatch;
import ws.palladian.kaggle.restaurants.features.descriptors.DescriptorExtractor;
import ws.palladian.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class PoiFeatureExtractor implements FeatureExtractor {
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PoiFeatureExtractor.class);

    /** The default number of cluster. */
    private static final int DEFAULT_K = 200;

    /** Number of threads to use during vocabulary building; equals the number of cores of the current machine. */
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();

    private static final class DescriptorExtractionTask implements Callable<List<double[]>> {
        private final File imageFile;
        private final DescriptorExtractor strategy;
        private final ProgressReporter progress;

        DescriptorExtractionTask(File imageFile, DescriptorExtractor strategy, ProgressReporter progress) {
            this.imageFile = imageFile;
            this.strategy = strategy;
            this.progress = progress;
        }

        @Override
        public List<double[]> call() throws Exception {
            BufferedImage image = ImageHandler.load(imageFile);
            BufferedImage greyscaleImage = ImageUtils.getGrayscaleImage(image);
            List<double[]> result = strategy.extract(greyscaleImage);
            progress.increment();
            return result;
        }
    }

    public static PoiFeatureExtractor buildVocabulary(DescriptorExtractor strategy, Collection<File> imageFiles) throws IOException {
        return buildVocabulary(strategy, imageFiles, new CommonsKMeansClusterer(DEFAULT_K));
    }

    public static PoiFeatureExtractor buildVocabulary(DescriptorExtractor strategy, Collection<File> imageFiles, Clusterer clusterer) throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        ProgressMonitor progressMonitor = new ProgressMonitor(1);
        progressMonitor.startTask("Initializing vocabulary for " + strategy, imageFiles.size());
        List<DescriptorExtractionTask> tasks = imageFiles.stream().map(f -> new DescriptorExtractionTask(f, strategy, progressMonitor)).collect(toList());
        List<double[]> descriptors = new ArrayList<>();
        try {
            List<Future<List<double[]>>> descriptorFutures = executor.invokeAll(tasks);
            for (Future<List<double[]>> future : descriptorFutures) {
                descriptors.addAll(future.get());
            }
            executor.shutdown();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
        LOGGER.info("Clustering {} descriptors", descriptors.size());
        StopWatch stopWatch = new StopWatch();
        Collection<Cluster> clusters = clusterer.cluster(descriptors);
        LOGGER.info("Clustering took {}", stopWatch);
        List<double[]> centroidPoints = clusters.stream().map(c -> c.center()).collect(toList());
        return new PoiFeatureExtractor(strategy, centroidPoints);
    }

    /**
     * Create a {@link PoiFeatureExtractor} from an existing vocabulary file.
     *
     * @param strategy       The strategy for extracting descriptors.
     * @param vocabularyFile The vocabulary file which has been created in advance.
     * @return The {@link PoiFeatureExtractor}.
     * @throws IOException In case the file could not be read.
     */
    public static PoiFeatureExtractor loadVocabulary(DescriptorExtractor strategy, File vocabularyFile) throws IOException {
        List<double[]> clusters;
        try (Stream<String> lines = Files.lines(vocabularyFile.toPath())) {
            clusters = lines.map(line -> {
                String[] split = line.split(";");
                double[] values = new double[split.length];
                for (int i = 0; i < split.length; i++) {
                    values[i] = Double.parseDouble(split[i]);
                }
                return values;
            }).collect(toList());
        }
        LOGGER.info("Loaded {} clusters from {}", clusters.size(), vocabularyFile);
        return new PoiFeatureExtractor(strategy, clusters);
    }

    private final DescriptorExtractor strategy;
    private final List<double[]> clusters;

    private PoiFeatureExtractor(DescriptorExtractor strategy, List<double[]> clusters) {
        this.strategy = strategy;
        this.clusters = clusters;
    }

    @Override
    public FeatureVector extract(BufferedImage image) {
        DistanceMeasure distanceMeasure = new EuclideanDistance();
        int[] histogram = new int[clusters.size()];
        BufferedImage greyscaleImage = ImageUtils.getGrayscaleImage(image);
        List<double[]> descriptors = strategy.extract(greyscaleImage);
        for (double[] featurePoint : descriptors) {
            int currentCentroidIdx = 0;
            int closestCentroidIdx = 0;
            double closestDistance = Float.MAX_VALUE;
            for (double[] centroidPoint : clusters) {
                double distance = distanceMeasure.compute(featurePoint, centroidPoint);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestCentroidIdx = currentCentroidIdx;
                }
                currentCentroidIdx++;
            }
            histogram[closestCentroidIdx]++;
        }
        return createBagOfWordsFeature(histogram);
    }

    private FeatureVector createBagOfWordsFeature(int[] histogram) {
        InstanceBuilder instanceBuilder = new InstanceBuilder();
        for (int i = 0; i < histogram.length; i++) {
            instanceBuilder.set(getFeatureName(i), histogram[i]);
        }
        return instanceBuilder.create();
    }

    private String getFeatureName(int i) {
        return strategy + "-" + (i + 1) + "/" + clusters.size();
    }

    /**
     * Write the vocabulary to a file, so that it can be reused later. E.g. when
     * running on training and test set, or when you simply want to avoid
     * creating the vocabulary over-and-over again.
     *
     * @param vocabularyFile Location, where the CSV file is written. File must not exist.
     * @throws IOException In case writing fails.
     */
    public void writeVocabularyToFile(File vocabularyFile) throws IOException {
        List<String> lines = clusters.stream().map(d -> {
            return Arrays.stream(d).mapToObj(String::valueOf).collect(Collectors.joining(";"));
        }).collect(toList());
        Files.write(vocabularyFile.toPath(), lines, StandardOpenOption.CREATE_NEW);
    }
}