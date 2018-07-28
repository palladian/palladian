//package ws.palladian.kaggle.restaurants.classifier.nn;
//
//import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
//
//import ws.palladian.classification.utils.Normalization;
//import ws.palladian.classification.utils.Normalizer;
//import ws.palladian.classification.utils.ZScoreNormalizer;
//import ws.palladian.core.AbstractLearner;
//import ws.palladian.core.dataset.Dataset;
//import ws.palladian.core.dataset.split.RandomSplit;
//import ws.palladian.kaggle.restaurants.utils.SimpleSplit;
//
///**
// * Wrapper which executes
// * {@link MultiLayerNetworkLearner#trainWithEarlyStopping(ws.palladian.kaggle.restaurants.utils.TrainTestSplit, int)}
// * with a 75:25 split on the given dataset.
// *
// * @author pk
// */
//public class EarlyStoppingMultiLayerNetworkLearner extends AbstractLearner<MultiLayerNetworkModel> {
//	private final int batchSize;
//	private final int maxEpochs;
//	private final Normalizer normalizer;
//
//	public EarlyStoppingMultiLayerNetworkLearner(int batchSize, int maxEpochs) {
//		this(batchSize, maxEpochs, new ZScoreNormalizer());
//	}
//
//	public EarlyStoppingMultiLayerNetworkLearner(int batchSize, int maxEpochs, Normalizer normalizer) {
//		this.batchSize = batchSize;
//		this.maxEpochs = maxEpochs;
//		this.normalizer = normalizer;
//	}
//
//	@Override
//	public MultiLayerNetworkModel train(Dataset dataset) {
//		RandomSplit split = new RandomSplit(dataset, 0.75);
//		return train(split.getTrain(), split.getTest());
//	}
//	
//	@Override
//	public MultiLayerNetworkModel train(Dataset training, Dataset validation) {
//
//		Normalization normalization = normalizer.calculate(training);
//		training = training.transform(normalization);
//		validation = validation.transform(normalization);
//
//		MultiLayerConfiguration config = MultiLayerNetworkLearner.createNoBrainerConfig(training);
//		MultiLayerNetworkLearner learner = new MultiLayerNetworkLearner(config, batchSize);
//		MultiLayerNetworkModel model = learner.trainWithEarlyStopping(new SimpleSplit(training, validation), maxEpochs);
//
//		return new MultiLayerNetworkModel(model.getModel(), model.getCategoryNames(), model.getFeatureNames(),
//				normalization);
//
//	}
//
//	@Override
//	public String toString() {
//		return getClass().getSimpleName() + " (batchSize=" + batchSize + ", maxEpochs=" + maxEpochs + ", normalizer="
//				+ normalizer + ")";
//	}
//
//}
