package ws.palladian.classification.liblinear;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.SolverType;
import ws.palladian.classification.evaluation.ConfusionMatrixEvaluator;
import ws.palladian.classification.evaluation.RandomCrossValidator;
import ws.palladian.classification.evaluation.CrossValidator.Fold;
import ws.palladian.classification.utils.ZScoreNormalizer;
import ws.palladian.core.AbstractLearner;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.helper.math.ConfusionMatrix;

/**
 * Performs a tuning on the penalty parameter C for the {@link LibLinearLearner}
 * by applying a k-fold cross validation, as suggested in "<a href="
 * http://www.csie.ntu.edu.tw/~cjlin/papers/guide/guide.pdf
 * ">A Practical Guide to Support Vector Classification</a>", Chih-Wei Hsu,
 * Chih-Chung Chang, and Chih-Jen Lin, 2010.
 * 
 * @author pk
 */
public class SelfTuningLibLinearLearner extends AbstractLearner<LibLinearModel> {
	
	/** The logger for this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(SelfTuningLibLinearLearner.class);
	
	private final int numFolds;

	public SelfTuningLibLinearLearner(int numFolds) {
		this.numFolds = numFolds;
	}

	@Override
	public LibLinearModel train(Dataset dataset) {
		// (1) determine the optimal setting for penalty paremter C using k-fold cross validation
		
		RandomCrossValidator crossValidator = new RandomCrossValidator(dataset, numFolds);
		double bestAccuracy = 0;
		double bestC = Double.NaN;
		
		// paper suggest to use 2^-5 to 2^15 for C
		for (int i = -5; i <= 15; i++) {
			double C = Math.pow(2, i);
			
			double averageAccuracy = 0;

			for (Fold fold : crossValidator) {
				
				Parameter parameter = new Parameter(SolverType.L2R_LR, C, 0.01);
				LibLinearLearner learner = new LibLinearLearner(parameter, 1, new ZScoreNormalizer());
				LibLinearModel model = learner.train(fold.getTrain());
				
				ConfusionMatrix confusionMatrix = new ConfusionMatrixEvaluator().evaluate(new LibLinearClassifier(), model, fold.getTest());
				averageAccuracy += confusionMatrix.getAccuracy();
			}
			averageAccuracy /= numFolds;
			if (averageAccuracy > bestAccuracy) {
				bestAccuracy = averageAccuracy;
				bestC = C;
			}
			LOGGER.info("C = {}, avg. accuracy = {}", C, averageAccuracy);
		}
		LOGGER.info("[BEST] C = {}, avg. accuracy = {}", bestC, bestAccuracy);
		
		// (2) train on the entire dataset using the determined setting for C
		Parameter parameter = new Parameter(SolverType.L2R_LR, bestC, 0.01);
		LibLinearLearner learner = new LibLinearLearner(parameter, 1, new ZScoreNormalizer());
		return learner.train(dataset);
		
	}

}
