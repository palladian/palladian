package ws.palladian.kaggle.restaurants.experiments;

import static java.util.Arrays.asList;
import static ws.palladian.helper.collection.CollectionHelper.convert;
import static ws.palladian.helper.collection.CollectionHelper.filter;
import static ws.palladian.helper.functional.Filters.regex;

import java.io.File;

import ws.palladian.classification.liblinear.LibLinearClassifier;
import ws.palladian.classification.liblinear.LibLinearLearner;
import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.helper.functional.Filter;
import ws.palladian.kaggle.restaurants.Experimenter;
import ws.palladian.kaggle.restaurants.dataset.Label;
import ws.palladian.kaggle.restaurants.utils.Config;
import ws.palladian.utils.InstanceValueBinarizer;
import ws.palladian.utils.InstanceValueRelativizer;
import ws.palladian.utils.NaNInfiniteInstanceFilter;

public class BoWFeatureTransformationExperiment {
	public static void main(String[] args) {
		// question; which feature type performs best for SURF BoW?

		File trainingFile = Config.getFilePath("dataset.yelp.restaurants.features.train");
		CsvDatasetReaderConfig.Builder csvConfigBuilder = CsvDatasetReaderConfig.filePath(trainingFile);
		Dataset trainingInstances = csvConfigBuilder.create();
		trainingInstances = trainingInstances.subset(NaNInfiniteInstanceFilter.FILTER);

		File testingFile = Config.getFilePath("dataset.yelp.restaurants.features.test");
		csvConfigBuilder = CsvDatasetReaderConfig.filePath(testingFile);
		Dataset testingInstances = csvConfigBuilder.create();
		testingInstances = testingInstances.subset(NaNInfiniteInstanceFilter.FILTER);

		File resultDirectory = Config.getDataPath("results-" + System.currentTimeMillis());
		Filter<String> surfFeatures = regex("SURF.*");

		// (1) absolute counts
		run(trainingInstances, testingInstances, resultDirectory, surfFeatures);

		// (2) frequencies (i.e. row values sum to one)
		Dataset freqTrainInstances = trainingInstances.transform(new InstanceValueRelativizer(surfFeatures));
		Dataset freqTestInstances = testingInstances.transform(new InstanceValueRelativizer(surfFeatures));
		run(freqTrainInstances, freqTestInstances, resultDirectory, surfFeatures);

		// (3) boolean (i.e. word appears at least once?)
		Dataset binTrainInstances = trainingInstances.transform(new InstanceValueBinarizer(surfFeatures));
		Dataset binTestInstances = testingInstances.transform(new InstanceValueBinarizer(surfFeatures));
		run(binTrainInstances, binTestInstances, resultDirectory, surfFeatures);
	}

	private static void run(Dataset trainingInstances, Dataset testingInstances,
			File resultDirectory, Filter<String> features) {
		Experimenter experimenter = new Experimenter(trainingInstances, testingInstances, resultDirectory);
		experimenter.withClassLabels(Label.values());
		// experimenter.withClassifier(QuickMlLearner.randomForest(100), new QuickMlClassifier(), asList(features));
		experimenter.withClassifier(new LibLinearLearner(), new LibLinearClassifier(), asList(features));
		experimenter.run();
	}
}
