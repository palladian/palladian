//package ws.palladian.kaggle.restaurants;
//
//import static ws.palladian.helper.functional.Filters.not;
//import static ws.palladian.helper.functional.Filters.regex;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Objects;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import quickml.data.instances.ClassifierInstance;
//import quickml.supervised.ensembles.randomForest.randomDecisionForest.RandomDecisionForestBuilder;
//import quickml.supervised.tree.decisionTree.DecisionTreeBuilder;
//import quickml.supervised.tree.decisionTree.scorers.PenalizedInformationGainScorerFactory;
//import ws.palladian.classification.liblinear.LibLinearClassifier;
//import ws.palladian.classification.liblinear.LibLinearLearner;
//import ws.palladian.classification.liblinear.LibLinearModel;
//import ws.palladian.classification.quickml.QuickMlClassifier;
//import ws.palladian.classification.quickml.QuickMlLearner;
//import ws.palladian.classification.utils.CsvDatasetReader;
//import ws.palladian.classification.utils.CsvDatasetReaderConfig;
//import ws.palladian.classification.utils.CsvDatasetReaderConfig.Builder;
//import ws.palladian.core.CategoryEntries;
//import ws.palladian.core.Classifier;
//import ws.palladian.core.Instance;
//import ws.palladian.core.InstanceBuilder;
//import ws.palladian.core.Learner;
//import ws.palladian.core.Model;
//import ws.palladian.core.dataset.Dataset;
//import ws.palladian.core.dataset.DatasetWithFeatureAsCategory;
//import ws.palladian.core.value.ImmutableBooleanValue;
//import ws.palladian.core.value.ImmutableFloatValue;
//import ws.palladian.core.value.ImmutableIntegerValue;
//import ws.palladian.core.value.ImmutableStringValue;
//import ws.palladian.core.value.NominalValue;
//import ws.palladian.helper.ProgressMonitor;
//import ws.palladian.helper.date.DateHelper;
//import ws.palladian.helper.functional.Filter;
//import ws.palladian.helper.functional.Filters;
//import ws.palladian.helper.io.FileHelper;
//import ws.palladian.kaggle.restaurants.classifier.nn.EarlyStoppingMultiLayerNetworkLearner;
//import ws.palladian.kaggle.restaurants.classifier.nn.MultiLayerNetworkClassifier;
//import ws.palladian.kaggle.restaurants.dataset.Label;
//import ws.palladian.kaggle.restaurants.utils.Config;
//import ws.palladian.kaggle.restaurants.utils.CsvDatasetWriter;
//
//public class MasterClassifier<M extends Model> {
//	
//	/** The logger for this class. */
//	private static final Logger LOGGER = LoggerFactory.getLogger(MasterClassifier.class);
//	
//	private final Classifier<M> classifier;
//	
//	private final Map<Label, M> models;
//	
//	public MasterClassifier(Classifier<M> classifier) {
//		this.classifier = Objects.requireNonNull(classifier);
//		this.models = loadModels();
//	}
//
//	private final Map<Label, M> loadModels() {
//		Map<Label, M> classifiers = new HashMap<>();
//		for (Label label : Label.values()) {
//			File modelFile = Config.getFilePath("model.classification." + label.toString());
//			M model;
//			try {
//				model = FileHelper.deserialize(modelFile.getAbsolutePath());
//				LOGGER.info("Loaded model {} for label {}", modelFile, label);
//			} catch (IOException e) {
//				throw new IllegalStateException("Could not deserialize " + modelFile, e);
//			}
//			classifiers.put(label, model);
//		}
//		return classifiers;
//	}
//
//	public void classify(Filter<? super Instance> filter, boolean train) throws IOException {
//
//		File featureFilePath;
//		if (train) {
//			featureFilePath = Config.getFilePath("dataset.yelp.restaurants.features.train");
//		} else {
//			featureFilePath = Config.getFilePath("dataset.yelp.restaurants.features.test");
//		}
//		CsvDatasetReaderConfig.Builder csvConfigBuilder = CsvDatasetReaderConfig.filePath(featureFilePath);
//		csvConfigBuilder.parser("photoId", ImmutableStringValue.PARSER);
//		csvConfigBuilder.parser("businessId", ImmutableStringValue.PARSER);
//		Dataset testingInstances = csvConfigBuilder.create();
//		
//		if (filter != null && filter != Filters.ALL) {
//			testingInstances = testingInstances.subset(filter);
//		}
//
//		String trainOrTest = train ? "train" : "test";
//		File outputCsv = Config.getDataPath("classified_" + trainOrTest + "_" + filter.toString().toLowerCase() + "_" + DateHelper.getCurrentDatetime() + ".csv");
//		LOGGER.info("Writing output to {}", outputCsv);
//		CsvDatasetWriter writer = new CsvDatasetWriter(outputCsv);
//
//		// iterate through test data
//		long count = testingInstances.size();
//		ProgressMonitor progress = new ProgressMonitor(count, .1, "Classify");
//		for (Instance instance : testingInstances) {
//
//			InstanceBuilder result = new InstanceBuilder();
//			
//			if (train) { // in the training set, these are explicitly given
//				result.set("photoId", instance.getVector().get("photoId"));
//				result.set("businessId", instance.getVector().get("businessId"));
//			} else { // in the test set, extract photoId from image's file name
//				NominalValue imagePath = (NominalValue) instance.getVector().get("image");
//				String photoId = FileHelper.getFileName(imagePath.getString()).replace(".jpg", "");
//				result.set("photoId", photoId);
//			}
//			
//			// classify each photo with each label classifier
//			for (Label label : Label.values()) {
//				M model = models.get(label);
//				CategoryEntries categoryEntries = classifier.classify(instance.getVector(), model);
//				result.set(label.toString(), categoryEntries.getProbability("true"));
//			}
//			progress.incrementAndPrintProgress();
//			writer.append(result.create(true));
//
//		}
//		writer.close();
//	}
//	
//	public static void train(Learner<?> learner) throws IOException {
//		Builder configBuilder = CsvDatasetReaderConfig.filePath(Config.getFilePath("dataset.yelp.restaurants.features.train"));
//		configBuilder.gzip(true);
//		configBuilder.parser(regex("(SURF|SIFT)-.*"), ImmutableIntegerValue.PARSER);
//		configBuilder.parser(regex("ambience_is_classy|good_for_dinner|good_for_kids|good_for_lunch|has_alcohol|has_table_service|outdoor_seating|restaurant_is_expensive|takes_reservations"), ImmutableBooleanValue.PARSER);
//		configBuilder.parser(regex("main_color-.*"), ImmutableBooleanValue.PARSER);
//		configBuilder.parser(regex("image|photoId|businessId"), ImmutableStringValue.PARSER);
//		configBuilder.parser(Filters.ALL, ImmutableFloatValue.PARSER);
//		CsvDatasetReader instances = configBuilder.create();
//		Filter<String> featureFilter = not(regex("image|photoId|businessId|.*/(100|500)|.*(20|40|60|80)-percentile|.*_count|ambience_is_classy|good_for_dinner|good_for_kids|good_for_lunch|has_alcohol|has_table_service|outdoor_seating|restaurant_is_expensive|takes_reservations|softmax:.*"));
//		Label[] labelsToTrain = Label.values();
//		for (Label label : labelsToTrain) {
//			LOGGER.info("Learning label {}", label);
//			Dataset currentDataset = new DatasetWithFeatureAsCategory(instances, label.toString());
//			currentDataset = currentDataset.filterFeatures(featureFilter);
//			Model model = learner.train(currentDataset);
//			String fileName = "model_" + label.toString() + "_" + DateHelper.getCurrentDatetime() + ".ser.gz";
//			FileHelper.serialize(model, Config.getDataPath(fileName).getAbsolutePath());
//		}
//	}
//	
//	public static void main(String[] args) throws IOException {
//		// MasterClassifier<?> masterClassifier = new MasterClassifier<>(new QuickMlClassifier());
//		// masterClassifier.classify(Filters.ALL, true);
//		// masterClassifier.classify(Filters.ALL, false);
//		
//		// DecisionTreeBuilder<ClassifierInstance> treeBuilder = new DecisionTreeBuilder<>();
//		// treeBuilder.scorerFactory(new PenalizedInformationGainScorerFactory());
//		// treeBuilder.minLeafInstances(2000);
//		// treeBuilder.numNumericBins(2);
//		// treeBuilder.maxDepth(1000); // the tree's depth is actually limited by the minLeafInstances attribute
//		// treeBuilder.ignoreAttributeProbability(0.7);
//		// RandomDecisionForestBuilder<ClassifierInstance> randomForestBuilder = new RandomDecisionForestBuilder<>(treeBuilder);
//		// randomForestBuilder.numTrees(250);
//		// randomForestBuilder.executorThreadCount(10); // XXX do not use all cores on Semknox machine
//		// train(new QuickMlLearner(randomForestBuilder));
//
//		// train(new EarlyStoppingMultiLayerNetworkLearner(1000, 100));
//		train(new LibLinearLearner());
//		
//		// MasterClassifier<?> masterClassifier = new MasterClassifier<>(new MultiLayerNetworkClassifier());
//		// masterClassifier.classify(Filters.ALL, true);
//		// masterClassifier.classify(Filters.ALL, false);
//		
//	}
//}
