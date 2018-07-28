package ws.palladian.kaggle.restaurants.experiments;

import static ws.palladian.helper.functional.Filters.regex;

import java.io.File;
import java.io.IOException;

import ws.palladian.classification.evaluation.roc.RocCurves;
import ws.palladian.classification.quickml.QuickMlClassifier;
import ws.palladian.classification.quickml.QuickMlModel;
import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.classification.utils.CsvDatasetReaderConfig.Builder;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DatasetWithFeatureAsCategory;
import ws.palladian.core.value.ImmutableBooleanValue;
import ws.palladian.core.value.ImmutableFloatValue;
import ws.palladian.core.value.ImmutableIntegerValue;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.helper.functional.Filters;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.kaggle.restaurants.classifier.quickml.ModelStats;
import ws.palladian.kaggle.restaurants.dataset.Label;
import ws.palladian.kaggle.restaurants.utils.Config;

public class ClassifierStats {
	public static void main(String[] args) throws IOException {
		QuickMlModel model = FileHelper
				.deserialize("/Users/pk/Desktop/model_good_for_lunch_2016-03-26_04-03-54.ser.gz");
//		System.out.println(model);
//		System.out.println(model.traverseModel(new ModelStats()).toString());
//		System.exit(0);
		
		Builder configBuilder = CsvDatasetReaderConfig.filePath(Config.getFilePath("dataset.yelp.restaurants.features.train"));
		configBuilder.gzip(true);
		configBuilder.parser(regex("(SURF|SIFT)-.*"), ImmutableIntegerValue.PARSER);
		configBuilder.parser(regex("ambience_is_classy|good_for_dinner|good_for_kids|good_for_lunch|has_alcohol|has_table_service|outdoor_seating|restaurant_is_expensive|takes_reservations"), ImmutableBooleanValue.PARSER);
		configBuilder.parser(regex("main_color-.*"), ImmutableBooleanValue.PARSER);
		configBuilder.parser(regex("image|photoId|businessId"), ImmutableStringValue.PARSER);
		configBuilder.parser(Filters.ALL, ImmutableFloatValue.PARSER);
		CsvDatasetReader dataset = configBuilder.create();
		
		Dataset currentDataset = new DatasetWithFeatureAsCategory(dataset, Label.GOOD_FOR_LUNCH.toString());
		
		QuickMlClassifier classifier = new QuickMlClassifier();
		RocCurves.RocCurvesEvaluator evaluator = new RocCurves.RocCurvesEvaluator("true");
		RocCurves rocCurves = evaluator.evaluate(classifier, model, currentDataset);
		// rocCurves.saveCurves(new File("/Users/pk/Desktop/curves-model_good_for_lunch_2016-03-17_03-55-45.png"));
		rocCurves.showCurves();
	}
}
