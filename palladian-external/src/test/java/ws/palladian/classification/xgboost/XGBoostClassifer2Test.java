package ws.palladian.classification.xgboost;
//package ws.palladian.kaggle.redhat.classifier.xgboost;
//
//import static org.junit.Assert.assertTrue;
//
//import java.io.FileNotFoundException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Random;
//
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//
//import ws.palladian.classification.evaluation.ConfusionMatrixEvaluator;
//import ws.palladian.classification.utils.CsvDatasetReaderConfig;
//import ws.palladian.classification.utils.CsvDatasetReaderConfig.Builder;
//import ws.palladian.classification.utils.DummyVariableCreator;
//import ws.palladian.core.dataset.Dataset;
//import ws.palladian.core.dataset.split.RandomSplit;
//import ws.palladian.helper.StopWatch;
//import ws.palladian.helper.io.ResourceHelper;
//import ws.palladian.helper.math.ConfusionMatrix;
//import ws.palladian.kaggle.redhat.classifier.xgboost.XGBoostClassifier2.XGBoostClassifier2Model;
//
//public class XGBoostClassifer2Test {
//	private Dataset train;
//	private Dataset test;
//	
//	@Before
//	public void setup() throws FileNotFoundException {
//		Builder configBuilder = CsvDatasetReaderConfig.filePath(ResourceHelper.getResourceFile("/mushroom.csv"));
//		configBuilder.readHeader(true);
//		configBuilder.fieldSeparator(",");
//		Dataset dataset = configBuilder.create();
//		dataset = dataset.transform(new DummyVariableCreator(dataset, false, false)).buffer();
//		RandomSplit trainTestSplit = new RandomSplit(dataset, 0.75, new Random(123l));
//		
//		this.train = trainTestSplit.getTrain();
//		this.test = trainTestSplit.getTest();
//	}
//	
//	@After
//	public void cleanup() {
//		this.train = null;
//		this.test = null;
//	}
//	
//	@Test
//	public void testMushroom() {
//
//		Map<String, Object> params = new HashMap<>();
//		params.put("objective", "binary:logistic");
//		params.put("early_stopping_rounds", "50");
//		params.put("eval_metric", "auc");
//		params.put("booster", "gbtree");
//		params.put("eta", 0.02);
//		params.put("subsample", 0.7);
//		params.put("colsample_bytree", 0.7);
//		params.put("min_child_weight", 0);
//		params.put("max_depth", 10);
//		params.put("silent", 0);
//
//		XGBoostModel model = new XGBoostLearner(params, /* rounds */ 10).train(train);
//		
//		// System.out.println(model.getFeatureRanking());
//		// assertEquals("cap-shape:b", model.getFeatureRanking().getTopN(1).get(0).getName());
//		assertTrue(model.getFeatureRanking().getTopN(1).get(0).getName().startsWith("cap-shape"));
//		// System.out.println(model);
//
//		ConfusionMatrixEvaluator evaluator = new ConfusionMatrixEvaluator();
//
//		// final int numIterations = 1000;
//		final int numIterations = 1;
//		
//		StopWatch stopWatch = new StopWatch();
//		for (int i = 0; i < numIterations; i++) {
//			ConfusionMatrix confusionMatrix = evaluator.evaluate(new XGBoostClassifier(), model, test);
//			assertTrue("accuracy should be greater 0.98, but was " + confusionMatrix.getAccuracy(),
//					confusionMatrix.getAccuracy() > 0.98);
//		}
//		System.out.println("original: " + stopWatch);
//
//		XGBoostClassifier2 classifier2 = new XGBoostClassifier2();
//		XGBoostClassifier2Model model2 = XGBoostClassifier2.convertModel(model);
//
//		stopWatch = new StopWatch();
//		for (int i = 0; i < numIterations; i++) {
//			ConfusionMatrix confusionMatrix = evaluator.evaluate(classifier2, model2, test);
//			assertTrue("accuracy should be greater 0.98, but was " + confusionMatrix.getAccuracy(),
//					confusionMatrix.getAccuracy() > 0.98);
//		}
//		System.out.println("classifier2: " + stopWatch);
//		
//		// result classifier2 is twice as fast ... nice, but nowehere near the
//		// 6,000 x speedup claimed on the website :]
//
//	}
//
//
//}
