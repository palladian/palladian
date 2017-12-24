package ws.palladian.classification.evaluation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.classification.evaluation.LogLossEvaluator;

public class LogLossEvaluatorTest {

	private static final double DELTA = 0.0001;

	@Test
	public void testCalcLogLoss() {
		assertEquals(0.69315, LogLossEvaluator.logLoss(true, 0.5), DELTA);
		assertEquals(0.10536, LogLossEvaluator.logLoss(true, 0.9), DELTA);
		assertEquals(2.3026, LogLossEvaluator.logLoss(true, 0.1), DELTA);
	}

}
