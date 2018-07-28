//package ws.palladian.kaggle.restaurants;
//
//import static ws.palladian.kaggle.restaurants.features.BoundsFeatureExtractor.BOUNDS;
//import static ws.palladian.kaggle.restaurants.features.ColorFeatureExtractor.COLOR;
//import static ws.palladian.kaggle.restaurants.features.EdginessFeatureExtractor.EDGINESS;
//import static ws.palladian.kaggle.restaurants.features.FrequencyFeatureExtractor.FREQUENCY;
//import static ws.palladian.kaggle.restaurants.features.RegionFeatureExtractor.REGION;
//import static ws.palladian.kaggle.restaurants.features.color.HSB.BRIGHTNESS;
//import static ws.palladian.kaggle.restaurants.features.color.HSB.HUE;
//import static ws.palladian.kaggle.restaurants.features.color.HSB.SATURATION;
//import static ws.palladian.kaggle.restaurants.features.color.Luminosity.LUMINOSITY;
//import static ws.palladian.kaggle.restaurants.features.color.RGB.BLUE;
//import static ws.palladian.kaggle.restaurants.features.color.RGB.GREEN;
//import static ws.palladian.kaggle.restaurants.features.color.RGB.RED;
//import static ws.palladian.kaggle.restaurants.features.descriptors.SiftDescriptorExtractor.SIFT;
//import static ws.palladian.kaggle.restaurants.features.descriptors.SurfDescriptorExtractor.SURF;
//
//import java.io.Closeable;
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.List;
//import java.util.Objects;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;
//
//import ws.palladian.core.Instance;
//import ws.palladian.core.InstanceBuilder;
//import ws.palladian.helper.ProgressMonitor;
//import ws.palladian.helper.ProgressReporter;
//import ws.palladian.helper.date.DateHelper;
//import ws.palladian.helper.functional.Function;
//import ws.palladian.kaggle.restaurants.dataset.ImageValue;
//import ws.palladian.kaggle.restaurants.dataset.YelpKaggleDatasetReader;
//import ws.palladian.kaggle.restaurants.dataset.YelpKaggleDatasetReader.Subset;
//import ws.palladian.kaggle.restaurants.dataset.YelpKaggleTestsetReader;
//import ws.palladian.kaggle.restaurants.features.FeatureExtractor;
//import ws.palladian.kaggle.restaurants.features.GridSimilarityExtractor;
//import ws.palladian.kaggle.restaurants.features.LocalFeatureExtractor;
//import ws.palladian.kaggle.restaurants.features.PoiFeatureExtractor;
//import ws.palladian.kaggle.restaurants.features.StatisticsFeatureExtractor;
//import ws.palladian.kaggle.restaurants.features.SymmetryFeatureExtractor;
//import ws.palladian.kaggle.restaurants.features.color.ColorExtractor;
//import ws.palladian.kaggle.restaurants.features.descriptors.DescriptorExtractor;
//import ws.palladian.kaggle.restaurants.features.imagenet.ImageNetFeatureExtractor;
//import ws.palladian.kaggle.restaurants.utils.Config;
//import ws.palladian.kaggle.restaurants.utils.CsvDatasetWriter;
//
//public class Extractor {
//	
//	private static final String TIMESTAMP = DateHelper.getCurrentDatetime();
//	
//	/** Number of threads to use during vocabulary building; equals the number of cores of the current machine. */
//	private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
//	
//	private static final class FeatureExtractionTask implements Runnable {
//
//		private final Instance instance;
//		private final Collection<FeatureExtractor> extractors;
//		private final CsvDatasetWriter writer;
//		private final ProgressReporter progressMonitor;
//
//		FeatureExtractionTask(Instance instance, Collection<FeatureExtractor> extractors, CsvDatasetWriter writer, ProgressReporter progressMonitor) {
//			this.instance = instance;
//			this.extractors = extractors;
//			this.writer = writer;
//			this.progressMonitor = progressMonitor;
//		}
//
//		@Override
//		public void run() {
//			InstanceBuilder instanceBuilder = new InstanceBuilder().add(instance.getVector());
//			for (FeatureExtractor extractor : extractors) {
//				ImageValue imageValue = (ImageValue) instance.getVector().get("image");
//				instanceBuilder.add(extractor.extract(imageValue.getImage()));
//			}
//			synchronized (writer) {
//				writer.append(instanceBuilder.create(instance.getCategory()));
//				progressMonitor.increment();
//			}
//		}
//	}
//
//	public static void run(Iterable<Instance> instances, File csvOutput, Collection<FeatureExtractor> extractors)
//			throws IOException {
//		try (CsvDatasetWriter writer = new CsvDatasetWriter(csvOutput)) {
//			ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
//			ProgressReporter progressMonitor = new ProgressMonitor(0.01);
//			int count = 0;
//			for (Instance instance : instances) {
//				executor.execute(new FeatureExtractionTask(instance, extractors, writer, progressMonitor));
//				count++;
//			}
//			progressMonitor.startTask("Extracting features using " + extractors.size() + " extractors.", count);
//			executor.shutdown();
//			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
//			for (FeatureExtractor extractor : extractors) {
//				if (extractor instanceof Closeable) {
//					try {
//						Closeable closeable = (Closeable) extractor;
//						closeable.close();
//					} catch (IOException e) {
//						// ignore
//					}
//				}
//			}
//		} catch (InterruptedException e) {
//			throw new IllegalStateException(e);
//		}
//	}
//
//	public static void createTrainingAndValidatationData(Subset subset, Collection<FeatureExtractor> extractors) throws IOException {
//		Objects.requireNonNull(subset);
//		
//		File trainCsv = Config.getFilePath("dataset.yelp.restaurants.train.csv");
//		File photoToBizCsv = Config.getFilePath("dataset.yelp.restaurants.train.photoToBizCsv");
//		File baseImagePath = Config.getFilePath("dataset.yelp.restaurants.train.photos");
//
//		YelpKaggleDatasetReader dataset = new YelpKaggleDatasetReader(photoToBizCsv, trainCsv, baseImagePath);
//		
//		Iterable<Instance> trainingSet = dataset.subset(subset, true);
//		Iterable<Instance> testingSet = dataset.subset(subset, false);
//
//		File trainingCsvOutput = Config.getDataPath("yelp_features_" + subset + "_train_" + TIMESTAMP + ".csv");
//		File testingCsvOutput = Config.getDataPath("yelp_features_" + subset + "_test_" + TIMESTAMP + ".csv");
//		run(trainingSet, trainingCsvOutput, extractors);
//		run(testingSet, testingCsvOutput, extractors);
//	}
//
//	/**
//	 * Add vocabulary feature extractors.
//	 * 
//	 * @param extractors
//	 *            The list of extractors where to add.
//	 * @param descriptorExtractor
//	 *            The descriptor extractor.
//	 * @param vocabularyPaths
//	 *            The paths to the clustered vocabulary files.
//	 * @throws IOException
//	 *             In case a vocabulary could not be loaded.
//	 */
//	private static void addVocabularyExtractor(List<FeatureExtractor> extractors,
//			DescriptorExtractor descriptorExtractor, List<File> vocabularyPaths) throws IOException {
//		for (File vocabularyPath : vocabularyPaths) {
//			extractors.add(PoiFeatureExtractor.loadVocabulary(descriptorExtractor, vocabularyPath));
//		}
//	}
//
//	public static Function<? super Instance, File> getImageFile() {
//		return input -> ((ImageValue) input.getVector().get("image")).getFile();
//	}
//	
//	/** Use {@link YelpKaggleTestsetReader} instead. */
//	@Deprecated
//	public static List<Instance> readInstancesFromPath(File testImagePath) {
//		return Arrays.stream(testImagePath.listFiles()).map(f -> {
//			return new InstanceBuilder().set("image", new ImageValue(f)).create(true);
//		}).collect(Collectors.toList());
//	}
//	
//	public static void main(String[] args) throws IOException {
//		ColorExtractor[] colorExtractors = new ColorExtractor[] { LUMINOSITY, RED, GREEN, BLUE, HUE, SATURATION, BRIGHTNESS };
//		List<FeatureExtractor> extractors = new ArrayList<>();
////		extractors.add(new StatisticsFeatureExtractor(colorExtractors));
////		extractors.add(new LocalFeatureExtractor(2, new StatisticsFeatureExtractor(colorExtractors)));
////		extractors.add(new LocalFeatureExtractor(3, new StatisticsFeatureExtractor(colorExtractors)));
////		extractors.add(new LocalFeatureExtractor(4, new StatisticsFeatureExtractor(colorExtractors)));
////		// extractors.add(new LocalFeatureExtractor(5, new StatisticsFeatureExtractor(colorExtractors)));
////		extractors.add(BOUNDS);
////		addVocabularyExtractor(extractors, SURF, Config.getFilePaths("dataset.yelp.restaurants.vocabulary.surf"));
////		extractors.add(COLOR);
////		// extractors.add(PoiFeatureExtractor.buildVocabulary(MOPS, vocabularyImages));
////		addVocabularyExtractor(extractors, SIFT, Config.getFilePaths("dataset.yelp.restaurants.vocabulary.sift"));
////		extractors.add(new SymmetryFeatureExtractor(colorExtractors));
////		extractors.add(REGION);
////		extractors.add(FREQUENCY);
////		extractors.add(new GridSimilarityExtractor(2));
////		extractors.add(new GridSimilarityExtractor(3));
////		extractors.add(new GridSimilarityExtractor(4));
////		// extractors.add(new GridSimilarityExtractor(5));
////		extractors.add(EDGINESS);
//		File pathToPython = Config.getFilePath("python.path");
//		File pathToGraph = Config.getFilePath("graphdeph.path");
//		extractors.add(new ImageNetFeatureExtractor(pathToPython, pathToGraph, ImageNetFeatureExtractor.TENSOR_SOFTMAX));
//		extractors.add(new ImageNetFeatureExtractor(pathToPython, pathToGraph, ImageNetFeatureExtractor.TENSOR_POOL_3));
//		
//		/////////////////// training data ///////////////////
//		
//		File trainCsv = Config.getFilePath("dataset.yelp.restaurants.train.csv");
//		File photoToBizCsv = Config.getFilePath("dataset.yelp.restaurants.train.photoToBizCsv");
//		File trainImagePath = Config.getFilePath("dataset.yelp.restaurants.train.photos");
//		YelpKaggleDatasetReader trainingSet = new YelpKaggleDatasetReader(photoToBizCsv, trainCsv, trainImagePath);
//		
//		File trainingCsvOutput = Config.getDataPath("yelp_features_full_train_" + TIMESTAMP + ".csv");
//		run(trainingSet, trainingCsvOutput, extractors);
//		
//		/////////////////// testing data ///////////////////
//		
//		File testImagePath = Config.getFilePath("dataset.yelp.restaurants.test.photos");
//		File testingCsvOutput = Config.getDataPath("yelp_features_full_test_" + TIMESTAMP + ".csv");
//		
//		YelpKaggleTestsetReader testingSet = new YelpKaggleTestsetReader(testImagePath);
//		run(testingSet, testingCsvOutput, extractors);
//		
//	}
//
//}
