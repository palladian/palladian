//package ws.palladian.kaggle.restaurants.classifier.nn;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//
//import org.deeplearning4j.earlystopping.EarlyStoppingConfiguration;
//import org.deeplearning4j.earlystopping.EarlyStoppingResult;
//import org.deeplearning4j.earlystopping.saver.LocalFileModelSaver;
//import org.deeplearning4j.earlystopping.scorecalc.DataSetLossCalculator;
//import org.deeplearning4j.earlystopping.termination.MaxEpochsTerminationCondition;
//import org.deeplearning4j.earlystopping.termination.ScoreImprovementEpochTerminationCondition;
//import org.deeplearning4j.earlystopping.trainer.EarlyStoppingTrainer;
//import org.deeplearning4j.nn.api.OptimizationAlgorithm;
//import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
//import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
//import org.deeplearning4j.nn.conf.Updater;
//import org.deeplearning4j.nn.conf.layers.DenseLayer;
//import org.deeplearning4j.nn.conf.layers.OutputLayer;
//import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
//import org.deeplearning4j.nn.weights.WeightInit;
//import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
//import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import ws.palladian.core.AbstractLearner;
//import ws.palladian.core.dataset.Dataset;
//import ws.palladian.core.dataset.split.TrainTestSplit;
//import ws.palladian.core.dataset.statistics.DatasetStatistics;
//import ws.palladian.core.value.NumericValue;
//
///**
// * Multilayer neural network classification using
// * <a href="http://deeplearning4j.org">Deeplearning4J</a>. Make sure to read
// * <a href="http://deeplearning4j.org/troubleshootingneuralnets">this</a> very
// * helpful overview, on how to configure a NN for different use cases.
// * 
// * Deeplearning4J says, it <i>is the first commercial-grade, open-source,
// * distributed deep-learning library written for Java and Scala. Integrated with
// * Hadoop and Spark, DL4J is designed to be used in business environments,
// * rather than as a research tool. Skymind is its commercial support arm.</i>
// * 
// * Nets are usually trained in multiple epochs. Use the
// * {@link #trainWithEarlyStopping(TrainTestSplit, int)} for a predefined
// * training mechanism which automatically stops when the optimal number of
// * epochs have been reached.
// * 
// * @author Philipp Katz
// */
//public class MultiLayerNetworkLearner extends AbstractLearner<MultiLayerNetworkModel> {
//	
//	/** The logger for this class. */
//	private static final Logger LOGGER = LoggerFactory.getLogger(MultiLayerNetworkLearner.class);
//
//	private final MultiLayerConfiguration configuration;
//	
//	private final int batchSize;
//	
//	/**
//	 * Create a default configuration which somehow "seems to work". The number
//	 * of input and output nodes is automatically configured based on the
//	 * provided data. The net will use (numInputNodes+numOutputNodes)/2 hidden
//	 * nodes, as usually suggested by <a href=
//	 * "http://stats.stackexchange.com/questions/181/how-to-choose-the-number-of-hidden-layers-and-nodes-in-a-feedforward-neural-netw">
//	 * literature</a>.
//	 * 
//	 * This configuration performs <b>one</b> iteration over the batch. It is
//	 * intended to be combined with the
//	 * {@link #trainWithEarlyStopping(TrainTestSplit)} method.
//	 * 
//	 * @param instances
//	 *            The training set.
//	 * @return The configuration.
//	 * @throws IllegalArgumentException
//	 *             in case the dataset does not at least contain one numeric
//	 *             feature, or in case the dataset only provides one target
//	 *             category.
//	 */
//	public static MultiLayerConfiguration createNoBrainerConfig(Dataset dataset) {
//        // settings taken from https://github.com/deeplearning4j/dl4j-0.4-examples
//        // do NOT use the example settings from iris flower classification, they produce epically crappy results
//		Objects.requireNonNull(dataset, "dataset must not be null");
//		// http://stats.stackexchange.com/questions/181/how-to-choose-the-number-of-hidden-layers-and-nodes-in-a-feedforward-neural-netw
//		int numInputNodes = dataset.getFeatureInformation().getFeatureNamesOfType(NumericValue.class).size();
//		if (numInputNodes < 1) {
//			throw new IllegalArgumentException("dataset must contain at least one numeric feature");
//		}
//		int numOutputNodes = new DatasetStatistics(dataset).getCategoryStatistics().getNumUniqueValues();
//		if (numOutputNodes < 1) {
//			throw new IllegalArgumentException("dataset must contain at least one category");
//		}
//		int numHiddenNodes = (int) Math.round((numInputNodes + numOutputNodes) / 2.);
//		LOGGER.info("{} input nodes, {} hidden nodes, {} output nodes", numInputNodes, numHiddenNodes, numOutputNodes);
//		int seed = 123;
//		double learningRate = 0.01;
//		MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
//				.seed(seed)
//				.iterations(1) // http://deeplearning4j.org/troubleshootingneuralnets#number-of-epochs-and-number-of-iterations
//				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
//				.learningRate(learningRate)
//				.updater(Updater.NESTEROVS)
//				.momentum(0.9)
//				.list(2)
//				.layer(0, new DenseLayer.Builder()
//						.nIn(numInputNodes)
//						.nOut(numHiddenNodes)
//						.weightInit(WeightInit.XAVIER)
//						.activation("relu").build())
//				.layer(1, new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
//						.weightInit(WeightInit.XAVIER)
//						.activation("softmax")
//						.nIn(numHiddenNodes)
//						.nOut(numOutputNodes).build())
//				.pretrain(false)
//				.backprop(true)
//				.build();
//		return configuration;
//	}
//
//	/**
//	 * Create a new learner with the given configuration. Use
//	 * {@link #createNoBrainerConfig(Iterable)} for an automatically configured
//	 * configuration depending on the training data.
//	 * 
//	 * @param configuration
//	 *            The configuration.
//	 * @param batchSize
//	 *            The number of examples in one training batch.
//	 */
//	public MultiLayerNetworkLearner(MultiLayerConfiguration configuration, int batchSize) {
//		this.configuration = Objects.requireNonNull(configuration);
//		this.batchSize = batchSize;
//	}
//
//	@Override
//	public MultiLayerNetworkModel train(Dataset dataset) {
//		MultiLayerNetwork model = new MultiLayerNetwork(configuration);
//		model.init();
//		model.setListeners(new ScoreIterationListener(0));
//		
//		DataSetIteratorAdapter dataSetIterator = new DataSetIteratorAdapter(dataset, batchSize);
//		model.fit(dataSetIterator);
//		
//		List<String> featureNames = dataSetIterator.getFeatureNames();
//		List<String> categoryNames = dataSetIterator.getLabels();
//		return new MultiLayerNetworkModel(model, categoryNames, featureNames);
//	}
//	
//	/**
//	 * Train the network using
//	 * <a href="http://deeplearning4j.org/earlystopping">Early Stopping</a>.
//	 * Early stopping runs the training on the specified training/validation
//	 * split for the specified number of epochs, but stops the training, as soon
//	 * as sore on the validation set drops (i.e. overfitting of the model), or
//	 * the number of <tt>maxEpochs</tt> has been reached, or after
//	 * <tt>0.2 * maxEpochs</tt> epochs without any improvements.
//	 * 
//	 * @param split
//	 *            The training/validation split.
//	 * @param maxEpochs
//	 *            The maximum number of epochs to train.
//	 * @return The trained model.
//	 */
//	// TODO move this entirely to EarlyStoppingMultiLayerNetworkLearner
//	public MultiLayerNetworkModel trainWithEarlyStopping(TrainTestSplit split, int maxEpochs) {
//		// example code taken from here:
//		// https://github.com/deeplearning4j/dl4j-0.4-examples/blob/master/src/main/java/org/deeplearning4j/examples/misc/earlystopping/EarlyStoppingMNIST.java
//		
//		DataSetIteratorAdapter myTrainData = new DataSetIteratorAdapter(split.getTrain(), batchSize);
//		DataSetIteratorAdapter myTestData = new DataSetIteratorAdapter(split.getTest(), batchSize);
//		
//		Path tempDirectory;
//		try {
//			tempDirectory = Files.createTempDirectory(MultiLayerNetworkLearner.class.getName());
//			tempDirectory.toFile().deleteOnExit();
//		} catch (IOException e) {
//			throw new IllegalStateException("Could not create temporary directory", e);
//		}
//
//		EarlyStoppingConfiguration earlyStoppingConfig = new EarlyStoppingConfiguration.Builder()
//				.epochTerminationConditions(
//						new MaxEpochsTerminationCondition(maxEpochs), 
//						new ScoreImprovementEpochTerminationCondition((int) (maxEpochs * 0.2)))
//				// .iterationTerminationConditions(new MaxTimeIterationTerminationCondition(20, TimeUnit.MINUTES))
//				.scoreCalculator(new DataSetLossCalculator(myTestData, true))
//		        .evaluateEveryNEpochs(1)
//				.modelSaver(new LocalFileModelSaver(tempDirectory.toAbsolutePath().toString()))
//				.build();
//
//		EarlyStoppingTrainer trainer = new EarlyStoppingTrainer(earlyStoppingConfig, configuration, myTrainData);
//
//		//Conduct early stopping training:
//		EarlyStoppingResult result = trainer.fit();
//
//		//Print out the results:
//		LOGGER.info("Termination reason: " + result.getTerminationReason());
//		LOGGER.info("Termination details: " + result.getTerminationDetails());
//		LOGGER.info("Total epochs: " + result.getTotalEpochs());
//		LOGGER.info("Best epoch number: " + result.getBestModelEpoch());
//		LOGGER.info("Score at best epoch: " + result.getBestModelScore());
//
//		//Get the best model:
//		MultiLayerNetwork bestModel = result.getBestModel();
//		
//        //Print score vs. epoch
//		if (LOGGER.isDebugEnabled()) {
//	        Map<Integer,Double> scoreVsEpoch = result.getScoreVsEpoch();
//	        List<Integer> list = new ArrayList<>(scoreVsEpoch.keySet());
//	        Collections.sort(list);
//	        LOGGER.debug("Score vs. Epoch:");
//	        for( Integer i : list){
//	            LOGGER.debug(i + "\t" + scoreVsEpoch.get(i));
//	        }
//		}
//		
//		return new MultiLayerNetworkModel(bestModel, myTrainData.getLabels(), myTrainData.getFeatureNames());
//	}
//
//}
