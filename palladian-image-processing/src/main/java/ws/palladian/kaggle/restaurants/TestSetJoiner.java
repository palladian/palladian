//package ws.palladian.kaggle.restaurants;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.Collection;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import ws.palladian.classification.utils.CsvDatasetReader;
//import ws.palladian.classification.utils.CsvDatasetReaderConfig;
//import ws.palladian.classification.utils.CsvDatasetReaderConfig.Builder;
//import ws.palladian.core.Instance;
//import ws.palladian.core.InstanceBuilder;
//import ws.palladian.core.value.ImmutableStringValue;
//import ws.palladian.helper.ProgressMonitor;
//import ws.palladian.helper.collection.DefaultMultiMap;
//import ws.palladian.helper.collection.MultiMap;
//import ws.palladian.helper.date.DateHelper;
//import ws.palladian.kaggle.restaurants.utils.Config;
//import ws.palladian.kaggle.restaurants.utils.CsvDatasetWriter;
//
//public class TestSetJoiner {
//	
//	/** The logger for this class. */
//	private static final Logger LOGGER = LoggerFactory.getLogger(MasterClassifier.class);
//	
//	public static void main(String[] args) throws IOException {
//		File photoToBizCsvTest = Config.getFilePath("dataset.yelp.restaurants.test.photoToBizCsv");
//		File testFeatures = Config.getFilePath("dataset.yelp.restaurants.classified.test");
//		File outputFile = Config.getDataPath("joined_classified_test_with_bizIds_" + DateHelper.getCurrentDatetime() + ".csv");
//		
//		LOGGER.info("Writing result to {}", outputFile);
//
//		// (1) read photoId to businessId mapping and keep in memory
//		MultiMap<String, String> photoIdToBusinessIds = DefaultMultiMap.createWithSet();
//		Builder csvReaderConfigBuilder = CsvDatasetReaderConfig.filePath(photoToBizCsvTest);
//		csvReaderConfigBuilder.setFieldSeparator(',');
//		csvReaderConfigBuilder.readHeader(true);
//		csvReaderConfigBuilder.readClassFromLastColumn(false);
//		csvReaderConfigBuilder.parser("photo_id", ImmutableStringValue.PARSER);
//		csvReaderConfigBuilder.parser("business_id", ImmutableStringValue.PARSER);
//		CsvDatasetReader photoToBizReader = csvReaderConfigBuilder.create();
//		for (Instance instance : photoToBizReader) {
//			String photoId = instance.getVector().get("photo_id").toString();
//			String businessId = instance.getVector().get("business_id").toString();
//			photoIdToBusinessIds.add(photoId, businessId);
//		}
//		
//		LOGGER.info("Read {} photo IDs", photoIdToBusinessIds.size());
//		int numPhotoInstances = photoIdToBusinessIds.allValues().size();
//		LOGGER.info("{} total photo instances", numPhotoInstances);
//
//		// (2) read feature file and join with data from (1)
//		ProgressMonitor progress = new ProgressMonitor(0.1);
//		progress.startTask("Writing joined test set", numPhotoInstances);
//		Builder csvReaderConfigBuilder2 = CsvDatasetReaderConfig.filePath(testFeatures);
//		csvReaderConfigBuilder2.parser("photoId", ImmutableStringValue.PARSER);
//		CsvDatasetReader featureReader = csvReaderConfigBuilder2.create();
//		CsvDatasetWriter writer = new CsvDatasetWriter(outputFile);
//		for (Instance instance : featureReader) {
//			String photoId = instance.getVector().get("photoId").toString();
//			Collection<String> businessIds = photoIdToBusinessIds.get(photoId);
//			for (String businessId : businessIds) {
//				writer.append(new InstanceBuilder().add(instance.getVector()).set("businessId", businessId).set("photoId", photoId).create(false));
//				progress.increment();
//			}
//		}
//		writer.close();
//
//	}
//
//}
