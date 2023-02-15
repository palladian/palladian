//package ws.palladian.kaggle.restaurants.classifier.nn;
//
//import static org.junit.Assert.assertTrue;
//
//import java.io.FileNotFoundException;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.junit.Test;
//
//import ws.palladian.classification.evaluation.ConfusionMatrixEvaluator;
//import ws.palladian.classification.utils.CsvDatasetReaderConfig;
//import ws.palladian.classification.utils.CsvDatasetReaderConfig.Builder;
//import ws.palladian.classification.utils.DummyVariableCreator;
//import ws.palladian.classification.utils.Normalization;
//import ws.palladian.classification.utils.ZScoreNormalizer;
//import ws.palladian.core.FeatureVector;
//import ws.palladian.core.Instance;
//import ws.palladian.core.InstanceBuilder;
//import ws.palladian.core.dataset.Dataset;
//import ws.palladian.core.dataset.split.RandomSplit;
//import ws.palladian.helper.io.ResourceHelper;
//import ws.palladian.helper.math.ConfusionMatrix;
//
//public class MultiLayerNetworkTest {
//	
////	@Test
////	public void testMultiLayerNetworkIrisData() throws FileNotFoundException {
////		
////
////		Builder configBuilder = CsvDatasetReaderConfig.filePath(ResourceHelper.getResourceFile("/iris.csv"));
////		configBuilder.readHeader(true);
////		configBuilder.fieldSeparator(",");
////		CsvDatasetReader datasetReader = configBuilder.create();
////		List<Instance> dataset = datasetReader.readAll();
////
////		Collections.shuffle(dataset);
////		// dataset = normalize(dataset);
////		
////        int numSamples = dataset.size();		
////        int splitTrainNum = (int) (numSamples * .8);		
////        
////        List<Instance> training = dataset.subList(0, splitTrainNum);
////        List<Instance> validation = dataset.subList(splitTrainNum+1,dataset.size());
////        
////		MultiLayerNetworkLearner learner = MultiLayerNetworkLearner.createNoBrainerConfig(new DefaultDataset(training));
////		MultiLayerNetworkModel model = learner.train(training);
////		
////		ConfusionMatrix confusionMatrix = new ConfusionMatrixEvaluator().evaluate(new MultiLayerNetworkClassifier(), model, validation);
////		// System.out.println(confusionMatrix);
////		
////		assertTrue("accuracy should be greater 0.9", confusionMatrix.getAccuracy() > 0.9);
////
////	}
//	
////	@Test
////	public void testMultiLayerNetworkIrisData_EarlyStopping() throws FileNotFoundException {
////		
////		
////		Builder configBuilder = CsvDatasetReaderConfig.filePath(ResourceHelper.getResourceFile("/iris.csv"));
////		configBuilder.readHeader(true);
////		configBuilder.fieldSeparator(",");
////		Dataset dataset = configBuilder.create();
////		dataset = dataset.buffer();
////		
////		RandomSplit trainTestSplit = new RandomSplit(dataset, 0.75);
////		RandomSplit trainEvaluateSplit = new RandomSplit(trainTestSplit.getTrain(), 0.75);
////		
////		//System.out.println("train = " + trainTestSplit.getTrain().size());
////		//System.out.println("test = " + trainTestSplit.getTest().size());
////
////		//System.out.println("trainTrain = " + trainEvaluateSplit.getTrain().size());
////		//System.out.println("trainEvaluate = " + trainEvaluateSplit.getTest().size());
////		
////		//System.exit(0);
////
////		MultiLayerConfiguration config = MultiLayerNetworkLearner.createNoBrainerConfig(trainEvaluateSplit.getTrain());
////		MultiLayerNetworkLearner learner = new MultiLayerNetworkLearner(config, 5000);
////		MultiLayerNetworkModel model = learner.trainWithEarlyStopping(trainEvaluateSplit, 1000);
////		
////		ConfusionMatrix confusionMatrix = new ConfusionMatrixEvaluator().evaluate(new MultiLayerNetworkClassifier(), model, trainTestSplit.getTest());
////		// System.out.println(confusionMatrix);
////		
////		System.out.println(confusionMatrix.getAccuracy());
////		assertTrue("accuracy should be greater 0.9", confusionMatrix.getAccuracy() > 0.9);
////		
////	}
//	
//	@Test
//	public void testMultiLayerNetworkIrisData_EarlyStopping_2() throws FileNotFoundException {
//		
//		
//		Builder configBuilder = CsvDatasetReaderConfig.filePath(ResourceHelper.getResourceFile("/iris.csv"));
//		configBuilder.readHeader(true);
//		configBuilder.fieldSeparator(",");
//		Dataset dataset = configBuilder.create();
//		dataset = dataset.buffer();
//		
//		RandomSplit trainTestSplit = new RandomSplit(dataset, 0.75);
//		
//		EarlyStoppingMultiLayerNetworkLearner learner = new EarlyStoppingMultiLayerNetworkLearner(5000, 1000);
//		MultiLayerNetworkModel model = learner.train(trainTestSplit.getTrain());
//		
//		ConfusionMatrix confusionMatrix = new ConfusionMatrixEvaluator().evaluate(new MultiLayerNetworkClassifier(), model, trainTestSplit.getTest());
//		// System.out.println(confusionMatrix);
//		
//		System.out.println(confusionMatrix.getAccuracy());
//		assertTrue("accuracy should be greater 0.9", confusionMatrix.getAccuracy() > 0.9);
//		
//	}
//	
//	@Test
//	public void testMultiLayerNetworkMushroomData() throws FileNotFoundException {
//		
//		Builder configBuilder = CsvDatasetReaderConfig.filePath(ResourceHelper.getResourceFile("/mushroom.csv"));
//		configBuilder.readHeader(true);
//		configBuilder.fieldSeparator(",");
//		Dataset dataset = configBuilder.create();
//		dataset = dataset.transform(new DummyVariableCreator(dataset)).buffer();
//		RandomSplit trainTestSplit = new RandomSplit(dataset, 0.75);
//
//		EarlyStoppingMultiLayerNetworkLearner learner = new EarlyStoppingMultiLayerNetworkLearner(5000, 100);
//		MultiLayerNetworkModel model = learner.train(trainTestSplit.getTrain());
//
//		ConfusionMatrix confusionMatrix = new ConfusionMatrixEvaluator().evaluate(new MultiLayerNetworkClassifier(), model, trainTestSplit.getTest());
//		// System.out.println(confusionMatrix);
//		
//		System.out.println(confusionMatrix.getAccuracy());
//		assertTrue("accuracy should be greater 0.98", confusionMatrix.getAccuracy() > 0.98);
//
//		
//	}
//
//	@SuppressWarnings("unused")
//	private static List<Instance> normalize(Dataset dataset) {
//		Normalization normalization = new ZScoreNormalizer().calculate(dataset);
//		List<Instance> normalizedInstances = new ArrayList<>();
//		for (Instance instance : dataset) {
//			FeatureVector normalizedVector = normalization.normalize(instance.getVector());
//			Instance normalizedInstance = new InstanceBuilder().add(normalizedVector).create(instance.getCategory());
//			normalizedInstances.add(normalizedInstance);
//		}
//		return normalizedInstances;
//	}
//
//}
