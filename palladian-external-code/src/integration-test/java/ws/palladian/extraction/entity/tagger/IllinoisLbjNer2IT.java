package ws.palladian.extraction.entity.tagger;

import static org.junit.Assert.assertTrue;
import static ws.palladian.extraction.entity.TaggingFormat.COLUMN;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode.EXACT_MATCH;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode.MUC;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

public class IllinoisLbjNer2IT {

    private static String trainingFile;
    private static String testFile;

    @BeforeClass
    public static void setUp() throws Exception {
        trainingFile = ResourceHelper.getResourcePath("/ner/training.txt");
        testFile = ResourceHelper.getResourcePath("/ner/test.txt");
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        trainingFile = null;
        testFile = null;
    }

    @Test
    public void testIllinoisNer() {
        IllinoisLbj2Ner tagger = new IllinoisLbj2Ner(2);

        String modelFiles = FileHelper.getTempFile().getPath();
        tagger.train(trainingFile, modelFiles);

        EvaluationResult er = tagger.evaluate(testFile, COLUMN);

        // precision MUC: 78.28%, recall MUC: 80.07%, F1 MUC: 79.16%
        // precision exact: 69.3%, recall exact: 70.89%, F1 exact: 70.09%
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());
        assertTrue(er.getF1(MUC) > 0.77);
        assertTrue(er.getF1(EXACT_MATCH) > 0.70);
    }

}
