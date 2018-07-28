//package ws.palladian.kaggle.restaurants.experiments;
//
//import static ws.palladian.helper.functional.Filters.equal;
//import static ws.palladian.helper.functional.Filters.not;
//import static ws.palladian.helper.functional.Filters.or;
//import static ws.palladian.helper.functional.Filters.regex;
//
//import java.io.File;
//
//import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
//
//import ws.palladian.classification.evaluation.roc.RocCurves;
//import ws.palladian.classification.liblinear.LibLinearClassifier;
//import ws.palladian.classification.liblinear.LibLinearLearner;
//import ws.palladian.classification.liblinear.LibLinearModel;
//import ws.palladian.classification.utils.CsvDatasetReaderConfig;
//import ws.palladian.classification.utils.CsvDatasetReaderConfig.Builder;
//import ws.palladian.classification.utils.Normalization;
//import ws.palladian.classification.utils.ZScoreNormalizer;
//import ws.palladian.core.dataset.Dataset;
//import ws.palladian.core.dataset.DatasetWithFeatureAsCategory;
//import ws.palladian.core.dataset.split.RandomSplit;
//import ws.palladian.core.value.ImmutableFloatValue;
//import ws.palladian.core.value.ImmutableStringValue;
//import ws.palladian.helper.collection.CollectionHelper;
//import ws.palladian.kaggle.restaurants.classifier.nn.MultiLayerNetworkClassifier;
//import ws.palladian.kaggle.restaurants.classifier.nn.MultiLayerNetworkLearner;
//import ws.palladian.kaggle.restaurants.classifier.nn.MultiLayerNetworkModel;
//import ws.palladian.kaggle.restaurants.dataset.Label;
//import ws.palladian.kaggle.restaurants.dataset.YelpKaggleDatasetReader.BusinessFilter;
//
//public class InceptionDataClassifierTrainer {
//	
//	public static void main(String[] args) {
//		Builder config = CsvDatasetReaderConfig.filePath(new File("/Volumes/iMac HD/Research/Yelp_Kaggle_Restaurants/data/yelp_features_full_train_2016-04-02_09-00-51.csv.gz"));
//		config.parser(regex("(softmax|pool_3):.*"), ImmutableFloatValue.PARSER);
//		config.parser(regex("businessId"), ImmutableStringValue.PARSER);
//		config.skipColumns(regex("pool_3:.*"));
//		
//		Dataset dataset = config.create();
//		// dataset = dataset.filterFeatures(not(regex("softmax:.*")));
//		// dataset = dataset.filterFeatures(not(regex("pool_3:.*")));
//		CollectionHelper.print(dataset.getFeatureInformation());
//		// System.out.println(dataset.getFeatureInformation());
//		// dataset = dataset.buffer();
//		
//		Normalization normalization = new ZScoreNormalizer().calculate(dataset);
//		System.out.println(normalization);
//		dataset = normalization.normalize(dataset);
//		
//		final Dataset trainSet = dataset.subset(BusinessFilter.TRAIN);
//		final Dataset validationSet = dataset.subset(BusinessFilter.VALIDATE);
//		
//		for (Label label : Label.values()) {
//			Dataset current = trainSet.filterFeatures(or(regex("softmax:.*"), equal(label.toString())));
//			// Dataset current = trainSet.filterFeatures(or(regex("pool_3:.*"), equal(label.toString())));
//			current = new DatasetWithFeatureAsCategory(current, label.toString());
//			current = current.buffer();
//			// LibLinearModel model = new LibLinearLearner().train(current);
//			MultiLayerConfiguration mlConfig = MultiLayerNetworkLearner.createNoBrainerConfig(current);
////			MultiLayerNetworkLearner learner = new MultiLayerNetworkLearner(mlConfig, 500);
//			MultiLayerNetworkLearner learner = new MultiLayerNetworkLearner(mlConfig, 1000);
//			RandomSplit split = new RandomSplit(current, 0.75);
//			MultiLayerNetworkModel model = learner.trainWithEarlyStopping(split, 100);
//			
//			Dataset currentValidation = new DatasetWithFeatureAsCategory(validationSet, label.toString());
//			// RocCurves roc = new RocCurves.RocCurvesEvaluator("true").evaluate(new LibLinearClassifier(), model, currentValidation);
//			RocCurves roc = new RocCurves.RocCurvesEvaluator("true").evaluate(new MultiLayerNetworkClassifier(), model, currentValidation);
//			roc.showCurves();
//			
//			break;
//		}
//		
//		
//	}
//
//}
