package ws.palladian.kaggle.restaurants.experiments;

import static ws.palladian.helper.collection.CollectionHelper.convert;
import static ws.palladian.helper.functional.Filters.and;
import static ws.palladian.helper.functional.Filters.not;
import static ws.palladian.helper.functional.Filters.regex;

import java.io.File;
import java.io.IOException;

import ws.palladian.classification.evaluation.roc.RocCurves;
import ws.palladian.classification.featureselection.FeatureSelector;
import ws.palladian.classification.featureselection.FeatureSelectorConfig;
import ws.palladian.classification.featureselection.FeatureSelector;
import ws.palladian.classification.liblinear.LibLinearClassifier;
import ws.palladian.classification.liblinear.LibLinearLearner;
import ws.palladian.classification.nb.NaiveBayesClassifier;
import ws.palladian.classification.nb.NaiveBayesLearner;
import ws.palladian.classification.quickml.QuickMlClassifier;
import ws.palladian.classification.quickml.QuickMlLearner;
import ws.palladian.classification.quickml.QuickMlModel;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DatasetWithFeatureAsCategory;
import ws.palladian.core.dataset.DefaultDataset;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.functional.Filter;
import ws.palladian.kaggle.restaurants.dataset.Label;
import ws.palladian.kaggle.restaurants.utils.Config;
import ws.palladian.utils.NaNInfiniteInstanceTransformer;

@SuppressWarnings("unused")
public class BFEExperiment {
	public static void main(String[] args) throws IOException {
		FeatureSelectorConfig.Builder<?> builder = FeatureSelectorConfig.with(QuickMlLearner.randomForest(100), new QuickMlClassifier());
		// Builder<?> builder = BackwardFeatureEliminationConfig.with(new NaiveBayesLearner(), new NaiveBayesClassifier());
		// Builder<?> builder = BackwardFeatureEliminationConfig.with((Factory<LibLinearLearner>) () -> new LibLinearLearner(), (Factory<LibLinearClassifier>) () -> new LibLinearClassifier());
		// builder.scorer(m -> m.getMatthewsCorrelationCoefficient());
		builder.evaluator(new RocCurves.RocCurvesEvaluator("true"), input -> input.getAreaUnderCurve());
		// builder.numThreads(4);
		FeatureSelector elimination = builder.create();
		
		File trainingFile = Config.getFilePath("dataset.yelp.restaurants.features.train");
		File testingFile = Config.getFilePath("dataset.yelp.restaurants.features.test");
		
		Dataset trainSet = CsvDatasetReaderConfig.filePath(trainingFile).create();
		Dataset validationSet = CsvDatasetReaderConfig.filePath(testingFile).create();
		
		String className = "good_for_kids";
		trainSet = new DatasetWithFeatureAsCategory(trainSet, className);
		validationSet = new DatasetWithFeatureAsCategory(validationSet, className);
		
		Filter<String> filter = and(regex("(hue|saturation|brightness).*|"+className), not(regex(".*_(max|min|range|sum|count|(10|20|30|40|60|70|80|90)-percentile)")));
		trainSet = trainSet.filterFeatures(filter);
		validationSet = validationSet.filterFeatures(filter);
		
		// elimination.rankFeatures(trainSet, validationSet, new ProgressMonitor(0.01));
		RocCurves.RocCurvesEvaluator evaluator = new RocCurves.RocCurvesEvaluator("true");
		RocCurves result = evaluator.evaluate(QuickMlLearner.randomForest(100), new QuickMlClassifier(), trainSet, validationSet);
		result.saveCurves(new File("/Users/pk/Desktop/ROC.png"));
		result.showCurves();
		// result.writeEntries(System.out, '\t');
		
//		for (Label l : Label.values()) {
//		
//	//		String className = "good_for_kids";
//	//		String className = "ambience_is_classy";
////			String className = "good_for_lunch";
//	//		String className = "has_alcohol";
//	//		String className = "takes_reservations";
//	//		String className = "outdoor_seating";
//			String className = l.toString();
//			
//			Filter<String> filter = and(regex("(hue|saturation|brightness).*|"+className), not(regex(".*_(max|min|range|sum|count|(10|20|30|40|60|70|80|90)-percentile)")));
//			Iterable<? extends Instance>currentTrainSet = ClassificationUtils.filterFeaturesIterable(trainSet, filter);
//			Iterable<? extends Instance>currentValidationSet = ClassificationUtils.filterFeaturesIterable(validationSet, filter);
//			
//			// System.out.println(ClassificationUtils.getFeatureNames(ClassificationUtils.unwrapInstances(trainSet)));
//			
//			currentValidationSet = useFeatureAsCategory(currentValidationSet, className);
//			currentTrainSet = useFeatureAsCategory(currentTrainSet, className);
//			
//			currentTrainSet = convert(currentTrainSet, NaNInfiniteInstanceTransformer.TRANSFORMER);
//			currentValidationSet = convert(currentValidationSet, NaNInfiniteInstanceTransformer.TRANSFORMER);
//			
//			// Filter<String> selectedFeatures =
//			// regex("brightness_mean|saturation_skewness|saturation_80-percentile|hue_50-percentile|brightness_skewness|saturation_kurtosis|brightness_10-percentile|hue_stdDev|saturation_stdDev|hue_kurtosis|saturation_20-percentile|saturation_90-percentile|saturation_60-percentile|hue_40-percentile|saturation_relStdDev|hue_90-percentile|hue_skewness");
//			// trainSet = filterFeaturesIterable(trainSet, selectedFeatures);
//			QuickMlModel model = QuickMlLearner.randomForest(100).train(currentTrainSet);
//			RocEvaluator result = ClassifierEvaluation.evaluateBinary(new QuickMlClassifier(), model, currentValidationSet, "true", new RocEvaluator(className));
//			// result.print(0.05);
//			// System.out.println();
//			result.paintROCData();
//			System.out.println("AUC for " + className + " = " + result.getAUC());
//		}
		//ConfusionMatrix matrix = evaluate(new QuickMlClassifier(), validationSet, model);
		//ThresholdAnalyzer thresholds = thresholdAnalysis(new QuickMlClassifier(), model, validationSet, "true");
		//System.out.println(matrix);
		//System.out.println(thresholds);
		
		// elimination.rankFeatures(trainSet, validationSet, new ProgressMonitor(0.01));
	}
}
