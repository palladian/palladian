package ws.palladian.kaggle.fisheries.classifier;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.classification.utils.CsvDatasetReaderConfig.Builder;
import ws.palladian.classification.xgboost.XGBoostClassifier;
import ws.palladian.classification.xgboost.XGBoostLearner;
import ws.palladian.classification.xgboost.XGBoostModel;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.value.ValueDefinitions;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.functional.Filters;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.kaggle.fisheries.utils.Config;

public class FisheryClassifier {
	
	public static void main(String[] args) throws IOException {

		File trainingFile = Config.getDataPath("training_inception_features.csv");
		Builder configBuilder = CsvDatasetReaderConfig.filePath(trainingFile);
		configBuilder.defaultParsers(ValueDefinitions.floatValue(),ValueDefinitions.stringValue());
		Dataset trainingSet = configBuilder.create();

		// start with a very small feature set
		// trainingSet = trainingSet.filterFeatures(Filters.equal("ratio"));
		// trainingSet = trainingSet.filterFeatures(Filters.regex("width|height|ratio|luminosity_.*"));
		// trainingSet = trainingSet.filterFeatures(Filters.regex("width|height|ratio|red_mean|green_mean|blue_mean"));
		trainingSet = trainingSet.filterFeatures(Filters.regex("pool_3:.*"));
		// trainingSet = trainingSet.filterFeatures(Filters.regex("softmax:.*"));
		
		// classifier configuration
		Map<String, Object> params = new HashMap<>();
		params.put("objective", "multi:softprob");
		params.put("eval_metric", "mlogloss");
		// params.put("eta", 0.02);
		params.put("booster", "gblinear");

		// params.put("booster", "gbtree");
		// params.put("min_child_weight", 0);
		// params.put("subsample", 0.7);
		// params.put("colsample_bytree", 0.7);
		// params.put("max_depth", 20);

		// params.put("silent", 1);
		
		int rounds = 50;

		/* RandomCrossValidator crossValidator = new RandomCrossValidator(trainingSet, 5);

		Collection<Pair<String, CategoryEntries>> overallData = new ArrayList<>();
		
		for (Fold fold : crossValidator) {

			XGBoostLearner learner = new XGBoostLearner(params, rounds);
			XGBoostClassifier classifier = new XGBoostClassifier();
			XGBoostModel model = learner.train(fold.getTrain(), fold.getTest());

			Collection<Pair<String, CategoryEntries>> data = new ArrayList<>();
			for (Instance instance : fold.getTest()) {
				CategoryEntries result = classifier.classify(instance.getVector(), model);
				data.add(Pair.of(instance.getCategory(), result));
			}
			overallData.addAll(data);
			System.out.println("logloss @ " + fold.getFold() + " = " + EvaluationUtils.logLoss(data));

		}
		
		System.out.println("overall logloss = " + EvaluationUtils.logLoss(overallData));
		System.exit(0); */
		
		XGBoostLearner learner = new XGBoostLearner(params, rounds);
		XGBoostModel model = learner.train(trainingSet);
		XGBoostClassifier classifier = new XGBoostClassifier();
		
		File testingFile = Config.getDataPath("testing_inception_features.csv");
		Dataset testingSet = CsvDatasetReaderConfig.filePath(testingFile).create();
		StringBuilder csvBuilder = new StringBuilder();

		String[] categories = "ALB,BET,DOL,LAG,NoF,OTHER,SHARK,YFT".split(",");
		csvBuilder.append("image,");
		csvBuilder.append(String.join(",", categories));
		csvBuilder.append('\n');

		for (Instance instance : testingSet) {
			CategoryEntries result = classifier.classify(instance.getVector(), model);
			String filePath = instance.getVector().getNominal("image").getString();
			String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);

			csvBuilder.append(fileName);
			for (String category : categories) {
				csvBuilder.append(',');
				csvBuilder.append(result.getProbability(category));
			}
			csvBuilder.append('\n');
		}

		String csvPath = Config.getDataPath("submission_" + DateHelper.getCurrentDatetime() + ".csv").getPath();
		FileHelper.writeToFile(csvPath, csvBuilder);

	}

}
