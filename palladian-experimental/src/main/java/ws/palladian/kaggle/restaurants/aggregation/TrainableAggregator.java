//package ws.palladian.kaggle.restaurants.aggregation;
//
//import static ws.palladian.kaggle.restaurants.dataset.Label.GOOD_FOR_LUNCH;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import org.apache.commons.lang3.tuple.Pair;
//import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import de.bwaldvogel.liblinear.Parameter;
//import de.bwaldvogel.liblinear.SolverType;
//import ws.palladian.classification.LibSvmClassifier;
//import ws.palladian.classification.LibSvmLearner;
//import ws.palladian.classification.RBFKernel;
//import ws.palladian.classification.evaluation.RandomCrossValidator;
//import ws.palladian.classification.evaluation.RandomCrossValidator.RandomFold;
//import ws.palladian.classification.liblinear.LibLinearClassifier;
//import ws.palladian.classification.liblinear.LibLinearLearner;
//import ws.palladian.classification.liblinear.LibLinearModel;
//import ws.palladian.classification.quickml.QuickMlClassifier;
//import ws.palladian.classification.quickml.QuickMlLearner;
//import ws.palladian.classification.quickml.QuickMlModel;
//import ws.palladian.classification.utils.ClassificationUtils;
//import ws.palladian.classification.utils.ClassifierEvaluation;
//import ws.palladian.classification.utils.CsvDatasetReader;
//import ws.palladian.classification.utils.CsvDatasetReaderConfig;
//import ws.palladian.classification.utils.CsvDatasetReaderConfig.Builder;
//import ws.palladian.classification.utils.DummyVariableCreator;
//import ws.palladian.classification.utils.MinMaxNormalizer;
//import ws.palladian.classification.utils.NoNormalizer;
//import ws.palladian.classification.utils.ZScoreNormalizer;
//import ws.palladian.core.AbstractLearner;
//import ws.palladian.core.CategoryEntries;
//import ws.palladian.core.Classifier;
//import ws.palladian.core.FeatureVector;
//import ws.palladian.core.Instance;
//import ws.palladian.core.InstanceBuilder;
//import ws.palladian.core.Model;
//import ws.palladian.core.dataset.CollectionDataset;
//import ws.palladian.core.dataset.Dataset;
//import ws.palladian.core.dataset.DefaultDataset;
//import ws.palladian.core.value.ImmutableStringValue;
//import ws.palladian.core.value.NumericValue;
//import ws.palladian.helper.collection.CollectionHelper;
//import ws.palladian.helper.collection.LazyMap;
//import ws.palladian.helper.io.FileHelper;
//import ws.palladian.helper.math.ConfusionMatrix;
//import ws.palladian.helper.math.FatStats;
//import ws.palladian.helper.math.Stats;
//import ws.palladian.kaggle.restaurants.ClassificationAggregator;
//import ws.palladian.kaggle.restaurants.classifier.liblinear.SelfTuningLibLinearLearner;
//import ws.palladian.kaggle.restaurants.classifier.libsvm.SelfTuningLibSvmLearner;
//import ws.palladian.kaggle.restaurants.classifier.nn.MultiLayerNetworkClassifier;
//import ws.palladian.kaggle.restaurants.classifier.nn.MultiLayerNetworkLearner;
//import ws.palladian.kaggle.restaurants.classifier.nn.MultiLayerNetworkModel;
//import ws.palladian.kaggle.restaurants.dataset.Label;
//import ws.palladian.kaggle.restaurants.utils.ClassifierCombination;
//import ws.palladian.kaggle.restaurants.utils.Config;
//import ws.palladian.kaggle.restaurants.utils.ClassifierCombination.EvaluationResult;
//
//public class TrainableAggregator<M extends Model> implements AggregationStrategy {
//	
//	/** The logger for this class. */
//	private static final Logger LOGGER = LoggerFactory.getLogger(ClassificationAggregator.class);
//
//	
//	public static double learnThresholds(Label label, ClassifierCombination<? extends Model> classifierCombi) {
//		File trainFile = Config.getFilePath("dataset.yelp.restaurants.classified.train");
//		Dataset labelTrainingInstances = readRegressionInstances(label, trainFile);
//
//		File testFile = Config.getFilePath("dataset.yelp.restaurants.classified.train");
//		Dataset labelTestingInstances = readRegressionInstances(label, testFile);
//		EvaluationResult<? extends Model> result = classifierCombi.runEvaluation(labelTrainingInstances, labelTestingInstances);
//		// System.out.println(result.getThresholdAnalyzer());
//		
//		File serializationPath = Config.getDataPath("aggregation_model_" + label.toString() + ".ser.gz");
//		FileHelper.trySerialize(result.getModel(), serializationPath.getAbsolutePath());
//		
//		return result.getConfusionMatrix().getSuperiority();
//	}
//
//	private static Dataset readRegressionInstances(Label labelToTrain,File filePath) {
//		
//		LazyMap<Pair<String, Label>, Stats> businessIdLabelAggregationStats = new LazyMap<>(FatStats.FACTORY);
//		
//		Builder configBuilder = CsvDatasetReaderConfig.filePath(filePath);
//		configBuilder.parser("businessId", ImmutableStringValue.PARSER);
//		configBuilder.parser("photoId", ImmutableStringValue.PARSER);
//		CsvDatasetReader reader = configBuilder.create();
//
//		for (Instance instance : reader) {
//			for (Label label : Label.values()) {
//				NumericValue probability = (NumericValue) instance.getVector().get(label.toString());
//				String businessId = instance.getVector().get("businessId").toString();
//				businessIdLabelAggregationStats.get(Pair.of(businessId, label)).add(probability.getDouble());
//			}
//		}
//		
//		List<Instance> labelTrainingInstances = new ArrayList<>();
//		
//		File trainCsv = Config.getFilePath("dataset.yelp.restaurants.train.csv");
//		Builder trainConfigBuilder = CsvDatasetReaderConfig.filePath(trainCsv);
//		trainConfigBuilder.readClassFromLastColumn(false);
//		trainConfigBuilder.setFieldSeparator(',');
//		trainConfigBuilder.treatAsNullValue("");
//		trainConfigBuilder.parser("business_id", ImmutableStringValue.PARSER);
//		CsvDatasetReader trainCsvReader = trainConfigBuilder.create();
//		for (Instance instance : trainCsvReader) {
//			String businessId = instance.getVector().get("business_id").toString();
//			if (!businessIdLabelAggregationStats.containsKey(Pair.of(businessId, Label.AMBIENCE_IS_CLASSY))) continue;
//			InstanceBuilder builder = new InstanceBuilder();
//			for (Label label : Label.values()) {
//				Stats stats = businessIdLabelAggregationStats.get(Pair.of(businessId, label));
//				assignStatsFeatures(builder, label, stats);
//			}
//			String labelsString = instance.getVector().get("labels").toString();
//			Set<String> labelSet = new HashSet<>(Arrays.asList(labelsString.split(" ")));
//			boolean positiveClass = labelSet.contains(labelToTrain.getLabelId()+"");
//			labelTrainingInstances.add(builder.create(positiveClass));
//		}
////		for (Instance instance : labelTrainingInstances) {
////			System.out.println(instance);
////		}
//		return new DefaultDataset(labelTrainingInstances);
//	}
//
//	private static void assignStatsFeatures(InstanceBuilder builder, Label label, Stats stats) {
//		builder.set(label.toString().toLowerCase() + "_mean_probability", stats.getMean());
//		//builder.set(label.toString().toLowerCase() + "_sum_probability", stats.getSum());
//		//builder.set(label.toString().toLowerCase() + "_median_probability", stats.getMedian());
//		builder.set(label.toString().toLowerCase() + "_max_probability", stats.getMax());
//		//builder.set(label.toString().toLowerCase() + "_min_probability", stats.getMin());
//		//builder.set(label.toString().toLowerCase() + "_stdDev_probability", stats.getStandardDeviation());
//		//builder.set(label.toString().toLowerCase() + "_stdDevRel_probability", stats.getRelativeStandardDeviation());
//	}
//	
//	private final Classifier<M> classifier;
//	private final Map<Label, M> models = new HashMap<>();
//	
//	public TrainableAggregator(Classifier<M> classifier) {
//		for (Label label : Label.values()) {
//			String filePath = Config.getFilePath("model.aggregation." + label.toString()).getAbsolutePath();
//			LOGGER.info("Loading aggregation model {}", filePath);
//			M model = FileHelper.tryDeserialize(filePath);
//			models.put(label, model);
//		}
//		this.classifier = classifier;
//	}
//
//	@Override
//	public Map<Label, Double> aggregate(Collection<Map<Label, Double>> classifiedImages) {
//		Map<Label, Stats> labelStats = new LazyMap<>(FatStats.FACTORY);
//		for (Map<Label, Double> classifiedImage : classifiedImages) {
//			for (Label label : Label.values()) {
//				labelStats.get(label).add(classifiedImage.get(label));
//			}
//		}
//		InstanceBuilder builder = new InstanceBuilder();
//		for (Label label : Label.values()) {
//			Stats stats = labelStats.get(label);
//			assignStatsFeatures(builder, label, stats);
//		}
//		FeatureVector featureVector = builder.create();
//		
//		Map<Label,Double> result = new HashMap<>();
//		for (Label label : Label.values()) {
//			M model = models.get(label);
//			CategoryEntries classificationResult = classifier.classify(featureVector, model);
//			result.put(label, classificationResult.getProbability("true"));
//		}
//		return result;
//	}
//	
//	public static void main(String[] args) {
//		Map<String, Double> labelScore = new LinkedHashMap<>();
//		for (Label l : Label.values()) {
////			double score = learnThresholds(l, new ClassifierCombination<>(new SelfTuningLibLinearLearner(100), new LibLinearClassifier()));
////			double score = learnThresholds(l, new ClassifierCombination<>(new AbstractLearner<MultiLayerNetworkModel>() {
////				@Override
////				public MultiLayerNetworkModel train(Dataset dataset) {
////					MultiLayerConfiguration config = MultiLayerNetworkLearner.createNoBrainerConfig(dataset);
////					MultiLayerNetworkLearner learner = new MultiLayerNetworkLearner(config, 10000);
////					RandomSplit split = new RandomSplit(dataset, 0.75);
////					return learner.trainWithEarlyStopping(split, 1000);
////				}
////			}, new MultiLayerNetworkClassifier()));
////			double score = learnThresholds(l, new ClassifierCombination<>(new SelfTuningLibSvmLearner(10), new LibSvmClassifier()));
//			double score = learnThresholds(l, new ClassifierCombination<>(new LibLinearLearner(), new LibLinearClassifier()));
////			double score = learnThresholds(l, new ClassifierCombination<>(new LibLinearLearner(new MinMaxNormalizer()), new LibLinearClassifier()));
////			double score = learnThresholds(l, new ClassifierCombination<>(new LibLinearLearner(new NoNormalizer()), new LibLinearClassifier()));
////			double score = learnThresholds(l, new ClassifierCombination<>(QuickMlLearner.randomForest(100), new QuickMlClassifier()));
//			System.out.println(l + " : " + score);
//			labelScore.put(l.toString(), score);
//		}
//		
//		CollectionHelper.print(labelScore);
//		
////		for (Label l : Label.values()) {
////			System.out.println("********** " + l + " **********");
////			learnThresholds(l, new ClassifierCombination<>(new LibLinearLearner(), new LibLinearClassifier()));
////		}
//	}
//
//}
