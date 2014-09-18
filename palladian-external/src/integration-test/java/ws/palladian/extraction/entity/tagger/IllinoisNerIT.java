package ws.palladian.extraction.entity.tagger;

import static ws.palladian.extraction.entity.TaggingFormat.COLUMN;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode.EXACT_MATCH;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode.MUC;

import org.apache.commons.configuration.Configuration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.integrationtests.ITHelper; // FIXME copied class from palladian-core

public class IllinoisNerIT {

    private static Configuration config;

    @BeforeClass
    public static void setUp() throws Exception {
        ITHelper.assertMemory(750, SizeUnit.MEGABYTES);
        config = ITHelper.getTestConfig();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        config = null;
    }

    @Test
    public void testIllinoisNer_CoNLL() {
        String trainPath = config.getString("dataset.conll.train");
        String testPath = config.getString("dataset.conll.test");
        ITHelper.assumeFile(trainPath, testPath);

        IllinoisNer tagger = new IllinoisNer();
        String modelFiles = FileHelper.getTempFile().getPath();
        tagger.train(trainPath, modelFiles);
        EvaluationResult er = tagger.evaluate(testPath, COLUMN);

        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
        ITHelper.assertMin("F1-MUC", 0.84, er.getF1(MUC));
        ITHelper.assertMin("F1-Exact", 0.79, er.getF1(EXACT_MATCH));
    }

    @Test
    public void testIllinoisNer_TUDCS4() {
        String trainPath = config.getString("dataset.tudcs4.train");
        String testPath = config.getString("dataset.tudcs4.test");
        ITHelper.assumeFile(trainPath, testPath);

        IllinoisNer tagger = new IllinoisNer();
        String modelFiles = FileHelper.getTempFile().getPath();
        tagger.train(trainPath, modelFiles);
        EvaluationResult er = tagger.evaluate(testPath, COLUMN);
        
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
        ITHelper.assertMin("F1-MUC", 0.44, er.getF1(MUC));
        ITHelper.assertMin("F1-Exact", 0.31, er.getF1(EXACT_MATCH));
    }

}
