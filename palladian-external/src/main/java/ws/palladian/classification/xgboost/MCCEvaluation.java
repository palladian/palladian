package ws.palladian.classification.xgboost;

import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.IEvaluation;
import ml.dmlc.xgboost4j.java.XGBoostError;
import ws.palladian.helper.math.ConfusionMatrix;

/**
 * Evaluation function for Matthews Correlation Coefficient.
 * 
 * @author pk
 * @deprecated This one uses a fixed threshold of 0.5, however there might be a
 *             better threshold which gives a higher MCC. Use
 *             {@link MCCEvaluation2} instead.
 */
@Deprecated
public class MCCEvaluation implements IEvaluation {

	private static final long serialVersionUID = 1L;
	
	private static final float threshold = 0.5f;

	@Override
	public float eval(float[][] predicts, DMatrix dmat) {
		try {

			int tp = 0, tn = 0, fp = 0, fn = 0;

			float[] labels = dmat.getLabel();
			for (int idx = 0; idx < labels.length; idx ++) {
				float prediction = predicts[idx][0];
				float label = labels[idx];
				
				if (label == 1 && prediction >= threshold) tp++; 
				if (label == 0 && prediction < threshold) tn++; 
				if (label == 1 && prediction < threshold) fn++;
				if (label == 0 && prediction >= threshold) fp++;
			}
			
			double mcc = ConfusionMatrix.calculateMatthewsCorrelationCoefficient(tp, tn, fp, fn);
			
			// returning negative MCC, because the value needs to be maximized,
			// but XGB tries to minimize per default:
			// https://github.com/dmlc/xgboost/issues/1226
			return (float) -mcc;
			
		} catch (XGBoostError e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String getMetric() {
		return "mcc";
	}

}
