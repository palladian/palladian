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

        assertEquals(8, confusionMatrix.getRealNumberOfCategory("cat"));
        assertEquals(6, confusionMatrix.getRealNumberOfCategory("dog"));
        assertEquals(13, confusionMatrix.getRealNumberOfCategory("rabbit"));

        assertEquals(7, confusionMatrix.getClassifiedNumberOfCategory("cat"));
        assertEquals(8, confusionMatrix.getClassifiedNumberOfCategory("dog"));
        assertEquals(12, confusionMatrix.getClassifiedNumberOfCategory("rabbit"));

        assertEquals(19, confusionMatrix.getCorrectlyClassified());
        assertEquals(19. / 27, confusionMatrix.getAccuracy(), DELTA);

        assertEquals(5, confusionMatrix.getNumberOfCorrectClassifiedDocumentsInCategory("cat"));
        assertEquals(3, confusionMatrix.getNumberOfCorrectClassifiedDocumentsInCategory("dog"));
        assertEquals(11, confusionMatrix.getNumberOfCorrectClassifiedDocumentsInCategory("rabbit"));

        assertEquals(0, confusionMatrix.getNumberOfConfusionsBetween("cat", "rabbit"));
        assertEquals(3, confusionMatrix.getNumberOfConfusionsBetween("cat", "dog"));
        assertEquals(2, confusionMatrix.getNumberOfConfusionsBetween("rabbit", "dog"));
        assertEquals(1, confusionMatrix.getNumberOfConfusionsBetween("dog", "rabbit"));

        assertEquals(3, confusionMatrix.getCategories().size());

        assertEquals(27, confusionMatrix.getTotalDocuments());

        assertEquals(13. / 27, confusionMatrix.getHighestPrior(), DELTA);

        assertEquals(5. / 7, confusionMatrix.getPrecisionForCategory("cat"), DELTA);
        assertEquals(5. / 8, confusionMatrix.getRecallForCategory("cat"), DELTA);
        assertEquals(22. / 27, confusionMatrix.getAccuracyForCategory("cat"), DELTA);
        assertEquals(2 * 5. / 7 * 5. / 8 / (5. / 7 + 5. / 8), confusionMatrix.getFForCategory("cat", 0.5), DELTA);

        assertEquals(3. / 8, confusionMatrix.getPrecisionForCategory("dog"), DELTA);
        assertEquals(3. / 6, confusionMatrix.getRecallForCategory("dog"), DELTA);
        assertEquals(19. / 27, confusionMatrix.getAccuracyForCategory("dog"), DELTA);
        assertEquals(2 * 3. / 8 * 3. / 6 / (3. / 8 + 3. / 6), confusionMatrix.getFForCategory("dog", 0.5), DELTA);

        assertEquals(11. / 12, confusionMatrix.getPrecisionForCategory("rabbit"), DELTA);
        assertEquals(11. / 13, confusionMatrix.getRecallForCategory("rabbit"), DELTA);
        assertEquals(24. / 27, confusionMatrix.getAccuracyForCategory("rabbit"), DELTA);
        assertEquals(2 * 11. / 12 * 11. / 13 / (11. / 12 + 11. / 13), confusionMatrix.getFForCategory("rabbit", 0.5), DELTA);

        assertEquals(8. / 27, confusionMatrix.getWeightForCategory("cat"), DELTA);
        assertEquals(6. / 27, confusionMatrix.getWeightForCategory("dog"), DELTA);
        assertEquals(13. / 27, confusionMatrix.getWeightForCategory("rabbit"), DELTA);

        assertEquals((5. / 7 + 3. / 8 + 11. / 12) / 3, confusionMatrix.getAveragePrecision(false), DELTA);
        assertEquals((5. / 8 + 3. / 6 + 11. / 13) / 3, confusionMatrix.getAverageRecall(false), DELTA);

        assertEquals(8. / 27 * 5. / 7 + 6. / 27 * 3. / 8 + 13. / 27 * 11. / 12, confusionMatrix.getAveragePrecision(true), DELTA);
        assertEquals(8. / 27 * 5. / 8 + 6. / 27 * 3. / 6 + 13. / 27 * 11. / 13, confusionMatrix.getAverageRecall(true), DELTA);

        // TODO test for superiority
        
        // System.out.println(confusionMatrix);

    }

}
