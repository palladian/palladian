package ws.palladian.classification.xgboost;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import ws.palladian.classification.evaluation.ConfusionMatrixEvaluator;
import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.classification.utils.CsvDatasetReaderConfig.Builder;
import ws.palladian.classification.utils.DummyVariableCreator;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.split.RandomSplit;
import ws.palladian.core.dataset.split.TrainTestSplit;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.helper.math.ConfusionMatrix;

public class XGBoostTest {

	private static final double DELTA = 0.0001;

	@Test
	public void simpleTest_twoClass() {
		Collection<Instance> instances = new ArrayList<>();
		instances.add(new InstanceBuilder().set("feature", 1).create("a"));
		instances.add(new InstanceBuilder().set("feature", 1).create("b"));

		Map<String, Object> params = new HashMap<>();
		params.put("objective", "binary:logistic");
		params.put("booster", "gbtree");

		XGBoostModel model = new XGBoostLearner(params, /* rounds */ 1).train(instances);

		CategoryEntries categoryEntries = new XGBoostClassifier()
				.classify(new InstanceBuilder().set("feature", 1).create(), model);
		assertEquals(0.5, categoryEntries.getProbability("a"), DELTA);
		assertEquals(0.5, categoryEntries.getProbability("b"), DELTA);
		// System.out.println(categoryEntries);
	}

	@Test
	public void simpleTest_multiClass() {
		Collection<Instance> instances = new ArrayList<>();
		instances.add(new InstanceBuilder().set("feature", 1).create("a"));
		instances.add(new InstanceBuilder().set("feature", 1).create("b"));
		instances.add(new InstanceBuilder().set("feature", 1).create("c"));
		instances.add(new InstanceBuilder().set("feature", 1).create("d"));

		Map<String, Object> params = new HashMap<>();
		params.put("objective", "multi:softprob");
		params.put("booster", "gbtree");

		XGBoostModel model = new XGBoostLearner(params, /* rounds */ 1).train(instances);

		CategoryEntries categoryEntries = new XGBoostClassifier()
				.classify(new InstanceBuilder().set("feature", 1).create(), model);
		assertEquals(0.25, categoryEntries.getProbability("a"), DELTA);
		assertEquals(0.25, categoryEntries.getProbability("b"), DELTA);
		assertEquals(0.25, categoryEntries.getProbability("c"), DELTA);
		assertEquals(0.25, categoryEntries.getProbability("d"), DELTA);
		// System.out.println(categoryEntries);
	}

	@Test
	public void testHeart() throws FileNotFoundException {
		TrainTestSplit heartDataset = getMushroomDataset();

		Map<String, Object> params = new HashMap<>();
		params.put("objective", "binary:logistic");
		params.put("early_stopping_rounds", "50");
		params.put("eval_metric", "auc");
		params.put("booster", "gbtree");
		params.put("eta", 0.02);
		params.put("subsample", 0.7);
		params.put("colsample_bytree", 0.7);
		params.put("min_child_weight", 0);
		params.put("max_depth", 10);
		params.put("silent", 0);

		XGBoostModel model = new XGBoostLearner(params, /* rounds */ 10).train(heartDataset.getTrain());

		// System.out.println(model.getFeatureRanking());
		// assertEquals("cap-shape:b",
		// model.getFeatureRanking().getTopN(1).get(0).getName());
		assertTrue(model.getFeatureRanking().getTopN(1).get(0).getName().startsWith("cap-shape"));
		// System.out.println(model);

		ConfusionMatrixEvaluator evaluator = new ConfusionMatrixEvaluator();
		ConfusionMatrix confusionMatrix = evaluator.evaluate(new XGBoostClassifier(), model, heartDataset.getTest());

		assertTrue("accuracy should be greater 0.98, but was " + confusionMatrix.getAccuracy(),
				confusionMatrix.getAccuracy() > 0.98);
	}

	private static TrainTestSplit getMushroomDataset() throws FileNotFoundException {
		Builder configBuilder = CsvDatasetReaderConfig.filePath(ResourceHelper.getResourceFile("/mushroom.csv"));
		configBuilder.readHeader(true);
		configBuilder.setFieldSeparator(',');
		Dataset dataset = configBuilder.create();
		dataset = dataset.transform(new DummyVariableCreator(dataset, false, false)).buffer();
		return new RandomSplit(dataset, 0.75, new Random(123l));

	}

}
