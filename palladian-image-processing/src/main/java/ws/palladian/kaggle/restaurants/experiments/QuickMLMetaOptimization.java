package ws.palladian.kaggle.restaurants.experiments;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ws.palladian.helper.functional.Filters.not;
import static ws.palladian.helper.functional.Filters.regex;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import quickml.data.instances.ClassifierInstance;
import quickml.supervised.PredictiveModelBuilder;
import quickml.supervised.classifier.Classifier;
import quickml.supervised.ensembles.randomForest.randomDecisionForest.RandomDecisionForestBuilder;
import quickml.supervised.tree.decisionTree.DecisionTreeBuilder;
import quickml.supervised.tree.decisionTree.scorers.GRPenalizedGiniImpurityScorerFactory;
import quickml.supervised.tree.decisionTree.scorers.PenalizedGiniImpurityScorerFactory;
import quickml.supervised.tree.decisionTree.scorers.PenalizedInformationGainScorerFactory;
import quickml.supervised.tree.decisionTree.scorers.PenalizedMSEScorerFactory;
import quickml.supervised.tree.decisionTree.scorers.PenalizedSplitDiffScorerFactory;
import quickml.supervised.tree.decisionTree.valueCounters.ClassificationCounter;
import quickml.supervised.tree.scorers.ScorerFactory;
import ws.palladian.classification.evaluation.roc.RocCurves;
import ws.palladian.classification.quickml.QuickMlClassifier;
import ws.palladian.classification.quickml.QuickMlLearner;
import ws.palladian.classification.quickml.QuickMlModel;
import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.classification.utils.CsvDatasetReaderConfig.Builder;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DatasetWithFeatureAsCategory;
import ws.palladian.core.value.ImmutableBooleanValue;
import ws.palladian.core.value.ImmutableFloatValue;
import ws.palladian.core.value.ImmutableIntegerValue;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Filters;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.kaggle.restaurants.dataset.YelpKaggleDatasetReader.BusinessFilter;
import ws.palladian.kaggle.restaurants.utils.Config;

// @SuppressWarnings("unused")
public class QuickMLMetaOptimization {
	
	private static final File resultPath = Config.getFilePath("dataset.yelp.restaurants.data");
	
	public static void main(String[] args) throws IOException {
		Builder configBuilder = CsvDatasetReaderConfig.filePath(Config.getFilePath("dataset.yelp.restaurants.features.train"));
		configBuilder.gzip(true);
		configBuilder.parser(regex("(SURF|SIFT)-.*"), ImmutableIntegerValue.PARSER);
		configBuilder.parser(regex("ambience_is_classy|good_for_dinner|good_for_kids|good_for_lunch|has_alcohol|has_table_service|outdoor_seating|restaurant_is_expensive|takes_reservations"), ImmutableBooleanValue.PARSER);
		configBuilder.parser(regex("main_color-.*"), ImmutableBooleanValue.PARSER);
		configBuilder.parser(regex("image|photoId|businessId"), ImmutableStringValue.PARSER);
		configBuilder.parser(Filters.ALL, ImmutableFloatValue.PARSER);
		Dataset dataset = configBuilder.create();
		
		// the category on which to test
		Dataset currentDataset = new DatasetWithFeatureAsCategory(dataset, "good_for_lunch");
		
		// split the training set in TRAIN and VALIDATION subsets
		Dataset training = currentDataset.subset(BusinessFilter.TRAIN);
		Dataset testing = currentDataset.subset(BusinessFilter.VALIDATE);

		Filter<String> featureFilter = not(regex("image|photoId|businessId|.*/(100|500)|.*(20|40|60|80)-percentile|.*_count|ambience_is_classy|good_for_dinner|good_for_kids|good_for_lunch|has_alcohol|has_table_service|outdoor_seating|restaurant_is_expensive|takes_reservations"));
		training = training.filterFeatures(featureFilter);
		testing = testing.filterFeatures(featureFilter);

		// the Palladian default setting
        // DecisionTreeBuilder<ClassifierInstance> treeBuilder = new DecisionTreeBuilder<>().ignoreAttributeProbability(0.7);
        // RandomDecisionForestBuilder<ClassifierInstance> randomForestBuilder = new RandomDecisionForestBuilder<>(treeBuilder).numTrees(100);
		// test(training, testing, randomForestBuilder, "default");
		
		// perform some optimization on the optimal tree size, which is
		// determined by the number of instances at each leaf,
		// the more instances which need to be at a leaf, the smaller the tree,
		// obviously, and the shorter the time for building it and the less
		// accuracy we get

//		RocCurvesPainter allRocCurves = new RocCurvesPainter();

//		DecisionTreeBuilder<ClassifierInstance> treeBuilder = new DecisionTreeBuilder<>();
//		RocCurves defaultCurves = test(training, testing, treeBuilder, "default-tree-builder");
//		allRocCurves.add(defaultCurves, "default-tree-builder");
		
		// optimization settings
		
		// test for optimal tradeoff between # leaf instances and speed + accuracy:
		// 50,000 instances is ~ 50 % of the training data
		// ...
		// 100 instances is ~ 0,1 % of the training data
		
		
/*		for (int leafInstances : new int[]{ 50000, 25000, 10000, 5000, 1000, 500, 100 }) {
			DecisionTreeBuilder<ClassifierInstance> optimizedTreeBuilder = new DecisionTreeBuilder<>();
			optimizedTreeBuilder.scorerFactory(new PenalizedGiniImpurityScorerFactory());
			optimizedTreeBuilder.minLeafInstances(leafInstances);
			optimizedTreeBuilder.numNumericBins(2);
			optimizedTreeBuilder.maxDepth(1000);
			String name = "min-leaf-" + leafInstances;
			RocCurves curves = test(training, testing, optimizedTreeBuilder, name);
//			allRocCurves.add(curves, name);
		}
*/		
//		allRocCurves.saveCurves(new File(resultPath, "_all_roc_" + DateHelper.getCurrentDatetime() + ".png"));
		
//		testWithNumTrees(training, testing, 1);
//		testWithNumTrees(training, testing, 5);
//		testWithNumTrees(training, testing, 10);
//		testWithNumTrees(training, testing, 25);
//		testWithNumTrees(training, testing, 50);
//		testWithNumTrees(training, testing, 100);
//		testWithNumTrees(training, testing, 250);
//		testWithNumTrees(training, testing, 500);
//		testWithNumTrees(training, testing, 1000);
//		
		testWithScorer(training, testing, new PenalizedGiniImpurityScorerFactory());
		testWithScorer(training, testing, new PenalizedInformationGainScorerFactory());
		testWithScorer(training, testing, new PenalizedMSEScorerFactory());
		testWithScorer(training, testing, new PenalizedSplitDiffScorerFactory());
		testWithScorer(training, testing, new GRPenalizedGiniImpurityScorerFactory());
		
//		testWithNumLeafInstances(training, testing, 1000);
//		testWithNumLeafInstances(training, testing, 500);
//		testWithNumLeafInstances(training, testing, 250);
//		testWithNumLeafInstances(training, testing, 100);
//		testWithNumLeafInstances(training, testing, 50);
//		testWithNumLeafInstances(training, testing, 25);
//		testWithNumLeafInstances(training, testing, 10);
//		testWithNumLeafInstances(training, testing, 5);
//		testWithNumLeafInstances(training, testing, 2);
//		testWithNumLeafInstances(training, testing, 1);
		
//		testWithNumNumericBins(training, testing, 50);
//		testWithNumNumericBins(training, testing, 25);
//		testWithNumNumericBins(training, testing, 10);
//		testWithNumNumericBins(training, testing, 5);
//		testWithNumNumericBins(training, testing, 2);
//		testWithNumNumericBins(training, testing, 1);
		
		// TODO max depth?
	}

	private static void testWithNumNumericBins(Dataset training, Dataset testing, int numNumericBins) throws IOException {
		DecisionTreeBuilder<ClassifierInstance> treeBuilder = new DecisionTreeBuilder<>().ignoreAttributeProbability(0.7);
		treeBuilder.numNumericBins(numNumericBins);
		treeBuilder.maxDepth(10000);
		RandomDecisionForestBuilder<ClassifierInstance> randomForestBuilder = new RandomDecisionForestBuilder<>(treeBuilder).numTrees(100);
		test(training, testing, randomForestBuilder, "numericBins-" + numNumericBins);
	}

	private static void testWithNumLeafInstances(Dataset training, Dataset testing, int minLeafInstances) throws IOException {
		DecisionTreeBuilder<ClassifierInstance> treeBuilder = new DecisionTreeBuilder<>().ignoreAttributeProbability(0.7);
		treeBuilder.minLeafInstances(minLeafInstances);
		treeBuilder.maxDepth(10000);
		RandomDecisionForestBuilder<ClassifierInstance> randomForestBuilder = new RandomDecisionForestBuilder<>(treeBuilder).numTrees(100);
		test(training, testing, randomForestBuilder, "minLeaf-" + minLeafInstances);
	}

	private static void testWithScorer(Dataset training, Dataset testing, ScorerFactory<ClassificationCounter> scorerFactory) throws IOException {
		// DecisionTreeBuilder<ClassifierInstance> treeBuilder = new DecisionTreeBuilder<>().ignoreAttributeProbability(0.7);
		// treeBuilder.scorerFactory(scorerFactory);
		// treeBuilder.maxDepth(10000);
		// RandomDecisionForestBuilder<ClassifierInstance> randomForestBuilder = new RandomDecisionForestBuilder<>(treeBuilder).numTrees(100);
		DecisionTreeBuilder<ClassifierInstance> optimizedTreeBuilder = new DecisionTreeBuilder<>();
		optimizedTreeBuilder.scorerFactory(new PenalizedGiniImpurityScorerFactory());
		optimizedTreeBuilder.minLeafInstances(1000);
		optimizedTreeBuilder.numNumericBins(2);
		optimizedTreeBuilder.maxDepth(1000);
		test(training, testing, optimizedTreeBuilder, "scorer-" + scorerFactory.getClass().getSimpleName());
	}
	
	private static void testWithNumTrees(Dataset training, Dataset testing, int numTrees) throws IOException {
		DecisionTreeBuilder<ClassifierInstance> treeBuilder = new DecisionTreeBuilder<>().ignoreAttributeProbability(0.7);
		treeBuilder.maxDepth(10000);
		RandomDecisionForestBuilder<ClassifierInstance> randomForestBuilder = new RandomDecisionForestBuilder<>(treeBuilder).numTrees(numTrees);
		test(training, testing, randomForestBuilder, "numTrees-" + numTrees);
	}
	
	private static RocCurves test(Dataset training, Dataset testing, PredictiveModelBuilder<? extends Classifier, ClassifierInstance> randomForestBuilder, String fileName) throws IOException {
		long start = System.currentTimeMillis();
		QuickMlModel model = new QuickMlLearner(randomForestBuilder).train(training);
		long trainingTime = System.currentTimeMillis() - start;
		FileHelper.serialize(model, new File(resultPath, "model_" + fileName + ".ser.gz").getAbsolutePath());

		QuickMlClassifier classifier = new QuickMlClassifier();
		RocCurves.RocCurvesEvaluator evaluator = new RocCurves.RocCurvesEvaluator("true");
		RocCurves rocCurves = evaluator.evaluate(classifier, model, testing);
		// rocCurves.saveCurves(new File(resultPath, "roc_" + fileName + ".png"));
		try (PrintStream printStream = new PrintStream(new File(resultPath, "roc_data_" + fileName + ".csv"))) {
			rocCurves.writeEntries(printStream, ';');
		}
		
		StringBuilder line = new StringBuilder();
		line.append(fileName).append(';');
		line.append(rocCurves.getAreaUnderCurve()).append(';');
		line.append(MILLISECONDS.toSeconds(trainingTime)).append('\n');
		FileHelper.appendFile(new File(resultPath, "_summary.csv").getAbsolutePath(), line);
		
		return rocCurves;
	}

}
