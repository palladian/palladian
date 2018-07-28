//package ws.palladian.kaggle.restaurants;
//
//import static ws.palladian.helper.collection.CollectionHelper.convert;
//import static ws.palladian.helper.collection.CollectionHelper.newArrayList;
//import static ws.palladian.kaggle.restaurants.Extractor.getImageFile;
//import static ws.palladian.kaggle.restaurants.dataset.YelpKaggleDatasetReader.Subset.MEDIUM;
//import static ws.palladian.kaggle.restaurants.features.descriptors.SiftDescriptorExtractor.SIFT;
//import static ws.palladian.kaggle.restaurants.features.descriptors.SurfDescriptorExtractor.SURF;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.Collection;
//
//import org.apache.spark.api.java.JavaSparkContext;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import ws.palladian.core.Instance;
//import ws.palladian.helper.date.DateHelper;
//import ws.palladian.kaggle.restaurants.clusterer.Clusterer;
//import ws.palladian.kaggle.restaurants.clusterer.SparkKMeansClusterer;
//import ws.palladian.kaggle.restaurants.dataset.YelpKaggleDatasetReader;
//import ws.palladian.kaggle.restaurants.features.PoiFeatureExtractor;
//import ws.palladian.kaggle.restaurants.features.descriptors.DescriptorExtractor;
//import ws.palladian.kaggle.restaurants.utils.Config;
//
//public class VocabularyClusterer {
//	
//	/** The logger for this class. */
//	private static final Logger LOGGER = LoggerFactory.getLogger(VocabularyClusterer.class);
//	
//	/**
//	 * Initialize a {@link PoiFeatureExtractor} by creating a vocabulary clustering.
//	 * 
//	 * @param dataset
//	 *            The instances which are used for building the vocabulary.
//	 * @param extractor
//	 *            The descriptor extractor (SIFT, SURF, PUSSYCAT, ...)
//	 * @param numClusters
//	 *            The number of clusters to create.
//	 * @param sparkContext
//	 *            The Spark context.
//	 * @param clusterer
//	 *            The clustering implementation for clustering the features.
//	 * @return The extractor.
//	 * @throws IOException
//	 *             In case loading a file fails.
//	 */
//	public static PoiFeatureExtractor initPoiExtractor(Iterable<Instance> dataset, DescriptorExtractor extractor,
//			int numClusters, JavaSparkContext sparkContext) throws IOException {
//		Collection<File> vocabularyImages = newArrayList(convert(dataset, getImageFile()));
//
//		LOGGER.info("Running POI clustering for {} images using {} with {} clusters", 
//				vocabularyImages.size(), extractor, numClusters);
//
//		String extractorName = extractor.toString().toLowerCase();
//		Clusterer clusterer = new SparkKMeansClusterer(sparkContext, numClusters, 20);
//		PoiFeatureExtractor poiExtractor = PoiFeatureExtractor.buildVocabulary(extractor, vocabularyImages, clusterer);
//		File outFile = Config.getDataPath("yelp_" + extractorName + "_vocabulary_" + numClusters + "_"
//				+ DateHelper.getCurrentDatetime() + ".csv");
//		poiExtractor.writeVocabularyToFile(outFile);
//		LOGGER.info("Wrote vocabulary for {} to {}", extractor, outFile);
//		return poiExtractor;
//	}
//
//	public static void main(String[] args) throws IOException {
//		File trainCsv = Config.getFilePath("dataset.yelp.restaurants.train.csv");
//		File photoToBizCsv = Config.getFilePath("dataset.yelp.restaurants.train.photoToBizCsv");
//		File baseImagePath = Config.getFilePath("dataset.yelp.restaurants.train.photos");
//
//		YelpKaggleDatasetReader dataset = new YelpKaggleDatasetReader(photoToBizCsv, trainCsv, baseImagePath);
//		Iterable<Instance> trainingSet = dataset.subset(MEDIUM, true);
//
//		JavaSparkContext sparkContext = new JavaSparkContext(Config.getSparkConf());
//		// for (int k : new int[] { 100, 500, 1000, 5000, 10000 }) {
//		for (int k : new int[] { 5000, 10000 }) {
//			for (DescriptorExtractor extractor : new DescriptorExtractor[] { SURF, SIFT }) {
//				initPoiExtractor(trainingSet, extractor, k, sparkContext);
//			}
//		}
//		sparkContext.close();
//	}
//
//}
