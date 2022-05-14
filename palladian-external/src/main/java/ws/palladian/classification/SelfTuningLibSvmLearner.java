package ws.palladian.classification;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.evaluation.ConfusionMatrixEvaluator;
import ws.palladian.classification.evaluation.RandomCrossValidator;
import ws.palladian.classification.evaluation.CrossValidator.Fold;
import ws.palladian.core.AbstractLearner;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.helper.math.ConfusionMatrix;

/**
 * Performs a tuning on the penalty parameter C and gamma for the
 * {@link LibSvmLearner} by applying a k-fold cross validation, as suggested in
 * "<a href=" http://www.csie.ntu.edu.tw/~cjlin/papers/guide/guide.pdf
 * ">A Practical Guide to Support Vector Classification</a>", Chih-Wei Hsu,
 * Chih-Chung Chang, and Chih-Jen Lin, 2010.
 * 
 * @author pk
 */
public class SelfTuningLibSvmLearner extends AbstractLearner<LibSvmModel> {

	/** The logger for this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(SelfTuningLibSvmLearner.class);
	
	private static final int STEP_SIZE = 2;
	
	private final int numFolds;

	public SelfTuningLibSvmLearner(int numFolds) {
		this.numFolds = numFolds;
	}

	@Override
	public LibSvmModel train(Dataset dataset) {
		// (1) determine the optimal setting for penalty paremter C and gamma using k-fold cross validation
		
		RandomCrossValidator crossValidator = new RandomCrossValidator(dataset, numFolds);
		double bestAccuracy = 0;
		Pair<Double,Double> bestCombination = null;
		
		// paper suggest to use 2^-5 to 2^15 for C
		for (int i = -5; i <= 15; i+=STEP_SIZE) {
			double C = FastMath.pow(2, i);
			
			for (int j = -15; j <= 3; j+=STEP_SIZE) {
				
				double gamma = FastMath.pow(2, j);
				
				double averageAccuracy = 0;
				
				for (Fold fold : crossValidator) {
					
					LibSvmLearner learner = new LibSvmLearner(new RBFKernel(C, gamma));
					LibSvmModel model = learner.train(fold.getTrain());
					
					ConfusionMatrix confusionMatrix = new ConfusionMatrixEvaluator().evaluate(new LibSvmClassifier(), model, fold.getTest());
					averageAccuracy += confusionMatrix.getAccuracy();
				}
				averageAccuracy /= numFolds;
				if (averageAccuracy > bestAccuracy) {
					bestAccuracy = averageAccuracy;
					bestCombination = Pair.of(C, gamma);
				}
				LOGGER.info("C = {}, gamma = {}, avg. accuracy = {}", C, gamma, averageAccuracy);
			}
			
		}
		LOGGER.info("[BEST] C = {}, gamma = {}, avg. accuracy = {}", bestCombination.getLeft(), bestCombination.getRight(), bestAccuracy);
		
		// (2) train on the entire dataset using the determined setting for C
		LibSvmLearner learner = new LibSvmLearner(new RBFKernel(bestCombination.getLeft(), bestCombination.getRight()));
		return learner.train(dataset);
		
	}

}
