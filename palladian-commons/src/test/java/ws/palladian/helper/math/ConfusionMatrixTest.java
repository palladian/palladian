package ws.palladian.helper.math;

import org.junit.Test;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.*;

public class ConfusionMatrixTest {

    private static final double DELTA = 0.0000001;

    @Test
    public void testConfusionMatrix() {
        ConfusionMatrix confusionMatrix = new ConfusionMatrix();
        confusionMatrix.add("cat", "cat", 5);
        confusionMatrix.add("cat", "dog", 3);
        confusionMatrix.add("cat", "rabbit", 0);
        confusionMatrix.add("dog", "cat", 2);
        confusionMatrix.add("dog", "dog", 3);
        confusionMatrix.add("dog", "rabbit", 1);
        confusionMatrix.add("rabbit", "cat", 0);
        confusionMatrix.add("rabbit", "dog", 2);
        confusionMatrix.add("rabbit", "rabbit", 11);

        assertEquals(8, confusionMatrix.getRealDocuments("cat"));
        assertEquals(6, confusionMatrix.getRealDocuments("dog"));
        assertEquals(13, confusionMatrix.getRealDocuments("rabbit"));

        assertEquals(7, confusionMatrix.getClassifiedDocuments("cat"));
        assertEquals(8, confusionMatrix.getClassifiedDocuments("dog"));
        assertEquals(12, confusionMatrix.getClassifiedDocuments("rabbit"));

        assertEquals(19, confusionMatrix.getTotalCorrect());
        assertEquals(19. / 27, confusionMatrix.getAccuracy(), DELTA);

        assertEquals(5, confusionMatrix.getCorrectlyClassifiedDocuments("cat"));
        assertEquals(3, confusionMatrix.getCorrectlyClassifiedDocuments("dog"));
        assertEquals(11, confusionMatrix.getCorrectlyClassifiedDocuments("rabbit"));

        assertEquals(0, confusionMatrix.getConfusions("cat", "rabbit"));
        assertEquals(3, confusionMatrix.getConfusions("cat", "dog"));
        assertEquals(2, confusionMatrix.getConfusions("rabbit", "dog"));
        assertEquals(1, confusionMatrix.getConfusions("dog", "rabbit"));

        assertEquals(3, confusionMatrix.getCategories().size());

        assertEquals(27, confusionMatrix.getTotalDocuments());

        assertEquals(13. / 27, confusionMatrix.getHighestPrior(), DELTA);

        assertEquals(5. / 7, confusionMatrix.getPrecision("cat"), DELTA);
        assertEquals(5. / 8, confusionMatrix.getRecall("cat"), DELTA);
        assertEquals(22. / 27, confusionMatrix.getAccuracy("cat"), DELTA);
        assertEquals(2 * 5. / 7 * 5. / 8 / (5. / 7 + 5. / 8), confusionMatrix.getF(1.0, "cat"), DELTA);

        assertEquals(3. / 8, confusionMatrix.getPrecision("dog"), DELTA);
        assertEquals(3. / 6, confusionMatrix.getRecall("dog"), DELTA);
        assertEquals(19. / 27, confusionMatrix.getAccuracy("dog"), DELTA);
        assertEquals(2 * 3. / 8 * 3. / 6 / (3. / 8 + 3. / 6), confusionMatrix.getF(1.0, "dog"), DELTA);

        assertEquals(11. / 12, confusionMatrix.getPrecision("rabbit"), DELTA);
        assertEquals(11. / 13, confusionMatrix.getRecall("rabbit"), DELTA);
        assertEquals(24. / 27, confusionMatrix.getAccuracy("rabbit"), DELTA);
        assertEquals(2 * 11. / 12 * 11. / 13 / (11. / 12 + 11. / 13), confusionMatrix.getF(1.0, "rabbit"), DELTA);

        assertEquals(8. / 27, confusionMatrix.getPrior("cat"), DELTA);
        assertEquals(6. / 27, confusionMatrix.getPrior("dog"), DELTA);
        assertEquals(13. / 27, confusionMatrix.getPrior("rabbit"), DELTA);

        assertEquals((5. / 7 + 3. / 8 + 11. / 12) / 3, confusionMatrix.getAveragePrecision(false), DELTA);
        assertEquals((5. / 8 + 3. / 6 + 11. / 13) / 3, confusionMatrix.getAverageRecall(false), DELTA);

        assertEquals(8. / 27 * 5. / 7 + 6. / 27 * 3. / 8 + 13. / 27 * 11. / 12, confusionMatrix.getAveragePrecision(true), DELTA);
        assertEquals(8. / 27 * 5. / 8 + 6. / 27 * 3. / 6 + 13. / 27 * 11. / 13, confusionMatrix.getAverageRecall(true), DELTA);

        // TODO test for superiority

        // System.out.println(confusionMatrix);

    }

    @Test
    public void testConfusionMatrix_issue161() {
        ConfusionMatrix confusionMatrix = new ConfusionMatrix();
        confusionMatrix.add("c1", "c1", 12);
        confusionMatrix.add("c2", "c1", 3);

        assertEquals(0.8, confusionMatrix.getPrior("c1"), DELTA);
        assertEquals(0.2, confusionMatrix.getPrior("c2"), DELTA);

        // System.out.println(confusionMatrix);
    }

    @Test
    public void testConfusionMatrix_issue125() {
        ConfusionMatrix confusionMatrix = new ConfusionMatrix();
        confusionMatrix.add("true", "true");
        confusionMatrix.add("true", "true");
        confusionMatrix.add("true", "true");
        confusionMatrix.add("false", "true");
        assertEquals(1.0, confusionMatrix.getRecall("true"), DELTA);
        assertEquals(0.75, confusionMatrix.getPrecision("true"), DELTA);
        assertEquals(1.0, confusionMatrix.getSensitivity("true"), DELTA);
        assertEquals(0.0, confusionMatrix.getSpecificity("true"), DELTA);
        assertEquals(0.8571428, confusionMatrix.getF(1.0, "true"), DELTA);

        assertEquals(0, confusionMatrix.getRecall("false"), DELTA);
        assertTrue(Double.isNaN(confusionMatrix.getPrecision("false")));
        assertEquals(0.0, confusionMatrix.getSensitivity("false"), DELTA);
        assertEquals(1.0, confusionMatrix.getSpecificity("false"), DELTA);
        assertTrue(Double.isNaN(confusionMatrix.getF(1.0, "false")));

        // System.out.println(confusionMatrix);
    }

    @Test
    public void testWeightedFMeasure() throws Exception {
        ConfusionMatrix confusionMatrix = new ConfusionMatrix();
        confusionMatrix.add("true", "true");
        confusionMatrix.add("true", "true");
        confusionMatrix.add("true", "true");
        confusionMatrix.add("true", "true");
        confusionMatrix.add("true", "true");
        confusionMatrix.add("true", "true");
        confusionMatrix.add("true", "true");
        confusionMatrix.add("false", "true");
        confusionMatrix.add("true", "false");
        confusionMatrix.add("true", "false");
        confusionMatrix.add("true", "true");
        confusionMatrix.add("true", "true");
        confusionMatrix.add("true", "false");
        confusionMatrix.add("true", "true");
        confusionMatrix.add("true", "true");
        confusionMatrix.add("true", "true");

        assertThat(confusionMatrix.getF(0.5, "true"), closeTo(0.895522388, DELTA));
        assertThat(confusionMatrix.getF(2.0, "true"), closeTo(0.821917808, DELTA));
    }

    @Test
    public void binaryTest2() {
        ConfusionMatrix confusionMatrix = new ConfusionMatrix();
        confusionMatrix.add("true", "true", 37);
        confusionMatrix.add("false", "false", 15);
        confusionMatrix.add("true", "false", 8);
        confusionMatrix.add("false", "true", 3);
        assertEquals(confusionMatrix.getSensitivity("true"), 0.8222, 0.0001);
        assertEquals(confusionMatrix.getSpecificity("true"), 0.8333, 0.0001);
        assertEquals(confusionMatrix.getHighestPrior(), 0.7143, 0.0001);
        assertEquals(confusionMatrix.getAccuracy(), 0.8254, 0.0001);
        assertEquals(confusionMatrix.getSuperiority(), 1.1556, 0.0001);
        assertEquals(confusionMatrix.getMatthewsCorrelationCoefficient(), 0.6151, 0.0001);

        confusionMatrix = new ConfusionMatrix();
        confusionMatrix.add("true", "true", 45);
        confusionMatrix.add("false", "true", 18);
        assertEquals(confusionMatrix.getSensitivity("true"), 1, 0.0001);
        assertEquals(confusionMatrix.getSpecificity("true"), 0, 0.0001);
        assertEquals(confusionMatrix.getHighestPrior(), 0.7143, 0.0001);
        assertEquals(confusionMatrix.getAccuracy(), 0.7143, 0.0001);
        assertEquals(confusionMatrix.getSuperiority(), 1, 0.0001);
        assertEquals(confusionMatrix.getMatthewsCorrelationCoefficient(), 0, 0.0001);

        confusionMatrix = new ConfusionMatrix();
        confusionMatrix.add("true", "true", 191);
        confusionMatrix.add("false", "false", 501);
        confusionMatrix.add("false", "true", 396);
        confusionMatrix.add("true", "false", 87);
        assertEquals(confusionMatrix.getMatthewsCorrelationCoefficient(), 0.2087, 0.0001);
    }

    @Test
    public void testCalculateMcc() {
        int tp = 5363;
        int tn = 1176868;
        int fp = 0;
        int fn = 1516;
        double mcc = ConfusionMatrix.calculateMatthewsCorrelationCoefficient(tp, tn, fp, fn);
        assertEquals(0.8823922851, mcc, DELTA);
    }

}
