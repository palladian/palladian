package ws.palladian.helper.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
        assertEquals(2 * 5. / 7 * 5. / 8 / (5. / 7 + 5. / 8), confusionMatrix.getF("cat", 0.5), DELTA);

        assertEquals(3. / 8, confusionMatrix.getPrecision("dog"), DELTA);
        assertEquals(3. / 6, confusionMatrix.getRecall("dog"), DELTA);
        assertEquals(19. / 27, confusionMatrix.getAccuracy("dog"), DELTA);
        assertEquals(2 * 3. / 8 * 3. / 6 / (3. / 8 + 3. / 6), confusionMatrix.getF("dog", 0.5), DELTA);

        assertEquals(11. / 12, confusionMatrix.getPrecision("rabbit"), DELTA);
        assertEquals(11. / 13, confusionMatrix.getRecall("rabbit"), DELTA);
        assertEquals(24. / 27, confusionMatrix.getAccuracy("rabbit"), DELTA);
        assertEquals(2 * 11. / 12 * 11. / 13 / (11. / 12 + 11. / 13), confusionMatrix.getF("rabbit", 0.5), DELTA);

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
    }

}
