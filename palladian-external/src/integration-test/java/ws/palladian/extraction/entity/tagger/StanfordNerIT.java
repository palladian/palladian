package ws.palladian.extraction.entity.tagger;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.integrationtests.ITHelper;

public class StanfordNerIT {

    private static File tempDirectory;
    private static String trainPath;
    private static String testPath;

    @BeforeClass
    public static void setUp() throws ConfigurationException {
        ITHelper.assertMemory(750, SizeUnit.MEGABYTES);
        Configuration config = ITHelper.getTestConfig();
        trainPath = config.getString("dataset.conll.train");
        testPath = config.getString("dataset.conll.test");
        ITHelper.assumeFile(trainPath, testPath);
        tempDirectory = FileHelper.getTempDir();
    }

    @AfterClass
    public static void cleanUp() {
        trainPath = null;
        testPath = null;
        tempDirectory = null;
    }

    @Test
    public void test_StanfordNer_CoNLL() {
        StanfordNer tagger = new StanfordNer();
        String stanfordNerModel = new File(tempDirectory, "stanfordner.ser.gz").getPath();
        tagger.train(trainPath, stanfordNerModel);
        tagger.loadModel(stanfordNerModel);

        // precision MUC: 85.22%, recall MUC: 83.55%, F1 MUC: 84.38%
        // precision exact: 76.6%, recall exact: 75.09%, F1 exact: 75.84%
        EvaluationResult er = tagger.evaluate(testPath, TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
        assertTrue(er.getF1(EvaluationMode.MUC) > 0.84);
        assertTrue(er.getF1(EvaluationMode.EXACT_MATCH) > 0.76);
    }

}
