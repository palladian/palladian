//package ws.palladian.kaggle.restaurants.experiments;
//
//import static ws.palladian.helper.functional.Filters.equal;
//import static ws.palladian.helper.functional.Filters.not;
//import static ws.palladian.helper.functional.Filters.or;
//import static ws.palladian.helper.functional.Filters.regex;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.Set;
//
//import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
//
//import ws.palladian.classification.evaluation.roc.RocCurves;
//import ws.palladian.classification.liblinear.LibLinearClassifier;
//import ws.palladian.classification.liblinear.LibLinearLearner;
//import ws.palladian.classification.utils.CsvDatasetReader;
//import ws.palladian.classification.utils.CsvDatasetReaderConfig;
//import ws.palladian.classification.utils.CsvDatasetReaderConfig.Builder;
//import ws.palladian.core.AbstractLearner;
//import ws.palladian.core.Classifier;
//import ws.palladian.core.Instance;
//import ws.palladian.core.InstanceBuilder;
//import ws.palladian.core.Learner;
//import ws.palladian.core.Model;
//import ws.palladian.core.dataset.Dataset;
//import ws.palladian.core.dataset.DatasetWithFeatureAsCategory;
//import ws.palladian.core.dataset.split.RandomSplit;
//import ws.palladian.core.value.ImmutableDoubleValue;
//import ws.palladian.core.value.ImmutableFloatValue;
//import ws.palladian.core.value.ImmutableIntegerValue;
//import ws.palladian.core.value.ImmutableStringValue;
//import ws.palladian.helper.ProgressMonitor;
//import ws.palladian.helper.io.FileHelper;
//import ws.palladian.kaggle.restaurants.Extractor;
//import ws.palladian.kaggle.restaurants.classifier.nn.MultiLayerNetworkClassifier;
//import ws.palladian.kaggle.restaurants.classifier.nn.MultiLayerNetworkLearner;
//import ws.palladian.kaggle.restaurants.classifier.nn.MultiLayerNetworkModel;
//import ws.palladian.kaggle.restaurants.dataset.Label;
//import ws.palladian.kaggle.restaurants.dataset.YelpKaggleDatasetReader;
//import ws.palladian.kaggle.restaurants.dataset.YelpKaggleDatasetReader.BusinessFilter;
//import ws.palladian.kaggle.restaurants.features.imagenet.ImageNetFeatureExtractor;
//import ws.palladian.kaggle.restaurants.utils.Config;
//import ws.palladian.kaggle.restaurants.utils.CsvDatasetWriter;
//import ws.palladian.retrieval.parser.json.JsonObject;
//
///**
// * @deprecated This is now available as a dedicated feature extractor, see
// *             {@link ImageNetFeatureExtractor}.
// * @author pk
// */
//@Deprecated
//public class DescriptionDatasetReader {
//	
//	public static void main(String[] args) throws Exception {
//		// convertToCSV(new File("/Users/pk/Downloads/train_index.json"), new File("/Users/pk/Desktop/descriptions-train.csv"));
//		// convertToCSV(new File("/Users/pk/Downloads/test_index.json"), new File("/Users/pk/Desktop/descriptions-test.csv"));
//		// joinData(true);
//		// joinData(false);
//		
//		/* for (Label l : Label.values()) {
//			testClassifier(new LibLinearLearner(), new LibLinearClassifier(), l);
//		} */
//		
//		// testClassifier(new LibLinearLearner(), new LibLinearClassifier(), Label.GOOD_FOR_LUNCH);
//		
//		testClassifier(new AbstractLearner<MultiLayerNetworkModel>() {
//			@Override
//			public MultiLayerNetworkModel train(Dataset dataset) {
//				MultiLayerConfiguration config = MultiLayerNetworkLearner.createNoBrainerConfig(dataset);
//				MultiLayerNetworkLearner learner = new MultiLayerNetworkLearner(config, 5000);
//				RandomSplit split = new RandomSplit(dataset, 0.75);
//				return learner.trainWithEarlyStopping(split, 100);
//			}
//		}, new MultiLayerNetworkClassifier(), Label.GOOD_FOR_LUNCH);
//		
//		
////		DecisionTreeBuilder<ClassifierInstance> treeBuilder = new DecisionTreeBuilder<>().ignoreAttributeProbability(0.7);
////		treeBuilder.scorerFactory(new PenalizedGiniImpurityScorerFactory());
////		treeBuilder.minLeafInstances(1000);
////		treeBuilder.numNumericBins(2);
////		treeBuilder.maxDepth(1000);
////		RandomDecisionForestBuilder<ClassifierInstance> randomForestBuilder = new RandomDecisionForestBuilder<>(treeBuilder).numTrees(100);
////		testClassifier(new QuickMlLearner(randomForestBuilder), new QuickMlClassifier());
//	}
//
//	public static <M extends Model> void testClassifier(Learner<M> learner, Classifier<M> classifier, Label label) throws IOException {
//		Builder config = CsvDatasetReaderConfig.filePath(new File("/Users/pk/temp/joined-train.csv"));
//		config.parser(regex("description-.*"), ImmutableIntegerValue.PARSER);
//		config.parser(regex("businessId"), ImmutableStringValue.PARSER);
//		Dataset dataset = config.create();
//		dataset = dataset.filterFeatures(or(regex("description-.*|businessId"), equal(label.toString())));
//		dataset = new DatasetWithFeatureAsCategory(dataset, label.toString());
//		Dataset trainSet = dataset.subset(BusinessFilter.TRAIN);
//		Dataset testSet = dataset.subset(BusinessFilter.VALIDATE);
//		trainSet = trainSet.filterFeatures(not(equal("businessId")));
//		testSet = testSet.filterFeatures(not(equal("businessId")));
//		trainSet = trainSet.buffer();
//		testSet = testSet.buffer();
//		
//		M model = learner.train(trainSet);
//		RocCurves roc = new RocCurves.RocCurvesEvaluator("true").evaluate(classifier, model, testSet);
//		// roc.saveCurves(new File("/Users/pk/Desktop/roc_inception_" + label.toString() + ".png"));
//		// FileHelper.serialize(model, "/Users/pk/Desktop/model_inception_" + label.toString() + ".ser.gz");
//		roc.showCurves();
//	}
//
//	public static void convertToCSV(File inputJson, File outputCsv) throws Exception {
//		String jsonString = FileHelper.readFileToString(inputJson);
//		// File outputCsv = new File("/Users/pk/Desktop/descriptions.csv");
//
//		JsonObject jsonObject = new JsonObject(jsonString);
//		Set<Entry<String, Object>> entries = jsonObject.entrySet();
//		CsvDatasetWriter writer = new CsvDatasetWriter(outputCsv, true);
//		ProgressMonitor progress = new ProgressMonitor();
//		progress.startTask("Writing CSV", entries.size());
//		for (Entry<String, Object> entry : entries) {
//			InstanceBuilder resultBuilder = new InstanceBuilder();
//			String photoFileName = entry.getKey();
//			String photoId = photoFileName.replace(".jpg", "");
//			resultBuilder.set("photoId", photoId);
//			@SuppressWarnings("unchecked")
//			// key: [1...50]; value: [1...1000] 
//			Map<String, String> wordsPlaces = (Map<String, String>) entry.getValue();
//			// write lines with scores determined by word's rank;
//			// e.g. a word with rank 10, will have a score of 0.82
//			// (1-((10-1)/50)) = 0.82
//			for (int i = 1; i <= 1000; i++) {
//				resultBuilder.set("inception-" + i, 0.);
//			}
//			for (Entry<String, String> placeWord : wordsPlaces.entrySet()) {
//				double score = 1. - (Integer.valueOf(placeWord.getKey()) - 1) / 50.;
//				resultBuilder.set("inception-" + placeWord.getValue(), score);
//			}
////			for (int i = 0; i < 1000; i++) {
////				boolean value = wordsPlaces.values().contains(String.valueOf(i));
////				resultBuilder.set("description-" + i, value ? 1 : 0);
////			}
//			Instance result = resultBuilder.create(false);
//			writer.append(result);
//			progress.increment();
//		}
//		writer.close();
//	}
//	
//	public static void joinData(boolean train) throws IOException {
//		Iterable<Instance> dataset;
//		if (train) {
//			File trainCsv = Config.getFilePath("dataset.yelp.restaurants.train.csv");
//			File photoToBizCsv = Config.getFilePath("dataset.yelp.restaurants.train.photoToBizCsv");
//			File trainImagePath = Config.getFilePath("dataset.yelp.restaurants.train.photos");
//			dataset = new YelpKaggleDatasetReader(photoToBizCsv, trainCsv, trainImagePath);
//		} else {
//			File testImagePath = Config.getFilePath("dataset.yelp.restaurants.test.photos");
//			dataset = Extractor.readInstancesFromPath(testImagePath);
//		}
//
//		Map<String, Instance> photoInstances = new HashMap<>();
//		for (Instance instance : dataset) {
//			String photoId;
//			// XXX branch is not required any longer when changing above's deprecated code
//			if (train) {
//				photoId = instance.getVector().get("photoId").toString();
//			} else {
//				String imagePath = instance.getVector().get("image").toString();
//				photoId = FileHelper.getFileName(imagePath).replace(".jpg", "");
//			}
//			photoInstances.put(photoId, instance);
//		}
//		
//		File featureCsv;
//		if (train) {
//			featureCsv = new File("/Users/pk/Desktop/descriptions-train.csv");
//		} else {
//			featureCsv = new File("/Users/pk/Desktop/descriptions-test.csv");
//		}
//		Builder configBuilder = CsvDatasetReaderConfig.filePath(featureCsv);
//		configBuilder.parser(regex("inception-.*"), ImmutableDoubleValue.PARSER);
//		configBuilder.parser("photoId", ImmutableStringValue.PARSER);
//		
//		CsvDatasetReader csvReader = configBuilder.create();
//		File outputCsv;
//		if (train) {
//			outputCsv = new File("/Users/pk/Desktop/joined-train.csv");
//		} else {
//			outputCsv = new File("/Users/pk/Desktop/joined-test.csv");
//		}
//		CsvDatasetWriter writer = new CsvDatasetWriter(outputCsv);
//		for (Instance instance : csvReader) {
//			String photoId = instance.getVector().get("photoId").toString();
//			Instance trainingInstance = photoInstances.get(photoId);
//			Instance newInstance = new InstanceBuilder().add(instance.getVector()).add(trainingInstance.getVector()).create(true);
//			writer.append(newInstance);
//		}
//		writer.close();
//	}
//
//}
