package ws.palladian.retrieval.cooccurrence;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PhraseProbabilityCalculatorTest {

    @Test
    public void testPhraseProbabilities() {
        CooccurrenceMatrix matrix = CooccurrenceMatrixTest.createTestMatrix();
        PhraseProbabilityCalculator calculator = new PhraseProbabilityCalculator(matrix, null);
        double probability = calculator.getProbability("I want chinese food");
        assertEquals(-3.44, probability, 0.001);
    }

}
