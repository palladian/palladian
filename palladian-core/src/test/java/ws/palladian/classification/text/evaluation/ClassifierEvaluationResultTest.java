package ws.palladian.classification.text.evaluation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ClassifierEvaluationResultTest {

    private static final double DELTA = 0.0000001;

    @Test
    public void testClassifierEvaluationResult() {
        ClassifierEvaluationResult result = new ClassifierEvaluationResult();
        result.add("cat", "cat", 5);
        result.add("cat", "dog", 3);
        result.add("cat", "rabbit", 0);
        result.add("dog", "cat", 2);
        result.add("dog", "dog", 3);
        result.add("dog", "rabbit", 1);
        result.add("rabbit", "cat", 0);
        result.add("rabbit", "dog", 2);
        result.add("rabbit", "rabbit", 11);

        assertEquals(8, result.getRealNumberOfCategory("cat"));
        assertEquals(6, result.getRealNumberOfCategory("dog"));
        assertEquals(13, result.getRealNumberOfCategory("rabbit"));

        assertEquals(7, result.getClassifiedNumberOfCategory("cat"));
        assertEquals(8, result.getClassifiedNumberOfCategory("dog"));
        assertEquals(12, result.getClassifiedNumberOfCategory("rabbit"));

        assertEquals(19, result.getCorrectlyClassified());
        assertEquals(19. / 27, result.getAccuracy(), DELTA);

        assertEquals(5, result.getNumberOfCorrectClassifiedDocumentsInCategory("cat"));
        assertEquals(3, result.getNumberOfCorrectClassifiedDocumentsInCategory("dog"));
        assertEquals(11, result.getNumberOfCorrectClassifiedDocumentsInCategory("rabbit"));

        assertEquals(0, result.getNumberOfConfusionsBetween("cat", "rabbit"));
        assertEquals(3, result.getNumberOfConfusionsBetween("cat", "dog"));
        assertEquals(2, result.getNumberOfConfusionsBetween("rabbit", "dog"));
        assertEquals(1, result.getNumberOfConfusionsBetween("dog", "rabbit"));

        assertEquals(3, result.getCategories().size());

        assertEquals(27, result.getTotalDocuments());

        assertEquals(13. / 27, result.getHighestPrior(), DELTA);

        assertEquals(5. / 7, result.getPrecisionForCategory("cat"), DELTA);
        assertEquals(5. / 8, result.getRecallForCategory("cat"), DELTA);
        assertEquals(22. / 27, result.getAccuracyForCategory("cat"), DELTA);
        assertEquals(2 * 5. / 7 * 5. / 8 / (5. / 7 + 5. / 8), result.getFForCategory("cat", 0.5), DELTA);

        assertEquals(3. / 8, result.getPrecisionForCategory("dog"), DELTA);
        assertEquals(3. / 6, result.getRecallForCategory("dog"), DELTA);
        assertEquals(19. / 27, result.getAccuracyForCategory("dog"), DELTA);
        assertEquals(2 * 3. / 8 * 3. / 6 / (3. / 8 + 3. / 6), result.getFForCategory("dog", 0.5), DELTA);

        assertEquals(11. / 12, result.getPrecisionForCategory("rabbit"), DELTA);
        assertEquals(11. / 13, result.getRecallForCategory("rabbit"), DELTA);
        assertEquals(24. / 27, result.getAccuracyForCategory("rabbit"), DELTA);
        assertEquals(2 * 11. / 12 * 11. / 13 / (11. / 12 + 11. / 13), result.getFForCategory("rabbit", 0.5), DELTA);

        assertEquals(8. / 27, result.getWeightForCategory("cat"), DELTA);
        assertEquals(6. / 27, result.getWeightForCategory("dog"), DELTA);
        assertEquals(13. / 27, result.getWeightForCategory("rabbit"), DELTA);

        assertEquals((5. / 7 + 3. / 8 + 11. / 12) / 3, result.getAveragePrecision(false), DELTA);
        assertEquals((5. / 8 + 3. / 6 + 11. / 13) / 3, result.getAverageRecall(false), DELTA);

        assertEquals(8. / 27 * 5. / 7 + 6. / 27 * 3. / 8 + 13. / 27 * 11. / 12, result.getAveragePrecision(true), DELTA);
        assertEquals(8. / 27 * 5. / 8 + 6. / 27 * 3. / 6 + 13. / 27 * 11. / 13, result.getAverageRecall(true), DELTA);

        // TODO test for superiority

    }

}
