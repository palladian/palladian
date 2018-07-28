package ws.palladian.classification.xgboost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.IEvaluation;
import ml.dmlc.xgboost4j.java.XGBoostError;
import ws.palladian.helper.math.ConfusionMatrix;

/**
 * Evaluation function for Matthews Correlation Coefficient. For getting an
 * optimal MCC, a classification threshold needs to be used, which defines the
 * binary class assignment.
 * 
 * The most simplistic case would be to assume a threshold of 0.5.
 * 
 * However, this is in general not the best possible result. This evaluator
 * tries out different thresholds and selects the optimal threshold, which
 * yields the highest MCC. The highest MCC is then returned as evaluation result
 * 
 * @author pk
 */
public class MCCEvaluation2 implements IEvaluation {

	private static final long serialVersionUID = 1L;

	/** The logger for this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(MCCEvaluation2.class);

	/**
	 * The number of steps for testing the threshold. This means, with each
	 * iteration, the threshold is incremented by 1 / NUM_STEPS.
	 */
	private static final int NUM_STEPS = 500;

	@Override
	public float eval(float[][] predicts, DMatrix dmat) {
		try {

			double bestThreshold = 0;
			double bestMcc = Double.MIN_VALUE;
			float[] labels = dmat.getLabel();

			for (int step = 0; step < NUM_STEPS; step++) {
				double threshold = (double) step / NUM_STEPS;

				int tp = 0, tn = 0, fp = 0, fn = 0;

				for (int idx = 0; idx < labels.length; idx++) {
					float prediction = predicts[idx][0];
					float label = labels[idx];

					if (label == 1 && prediction >= threshold) {
						tp++;
					}
					if (label == 0 && prediction < threshold) {
						tn++;
					}
					if (label == 1 && prediction < threshold) {
						fn++;
					}
					if (label == 0 && prediction >= threshold) {
						fp++;
					}
				}

				double mcc = ConfusionMatrix.calculateMatthewsCorrelationCoefficient(tp, tn, fp, fn);
				if (mcc > bestMcc) {
					bestMcc = mcc;
					bestThreshold = threshold;
				}

			}

			LOGGER.info("MCC @ {} = {}", bestThreshold, bestMcc);

			// returning negative MCC, because the value needs to be maximized,
			// but XGB tries to minimize per default:
			// https://github.com/dmlc/xgboost/issues/1226
			return (float) -bestMcc;

		} catch (XGBoostError e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String getMetric() {
		return "mcc";
	}

}
