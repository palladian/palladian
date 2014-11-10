package ws.palladian.extraction.entity.tagger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.core.Annotation;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.integrationtests.ITHelper;

public class LingPipeNerIT {

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
    public void test_LingPipeNer_CoNLL() {
        LingPipeNer tagger = new LingPipeNer();
        String lingpipeNerModelFile = new File(tempDirectory, "lingpipe.model").getPath();
        tagger.train(trainPath, lingpipeNerModelFile);
        tagger.loadModel(lingpipeNerModelFile);

        // precision MUC: 81.93%, recall MUC: 74.04%, F1 MUC: 77.79%
        // precision exact: 72.96%, recall exact: 65.93%, F1 exact: 69.27%
        EvaluationResult er = tagger.evaluate(testPath, TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
        assertTrue(er.getF1(EvaluationMode.MUC) > 0.77);
        assertTrue(er.getF1(EvaluationMode.EXACT_MATCH) > 0.69);

        tagger.loadModel(lingpipeNerModelFile);
        List<Annotation> annotations = tagger.getAnnotations(FileFormatParser.getText(testPath, TaggingFormat.COLUMN));

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        assertEquals(1903, annotations.size());
        assertEquals(34, annotations.get(0).getStartPosition());
        assertEquals(2, annotations.get(0).getValue().length());

        assertEquals(17251, annotations.get(500).getStartPosition());
        assertEquals(10, annotations.get(500).getValue().length());

        assertEquals(104255, annotations.get(annotations.size() - 1).getStartPosition());
        assertEquals(6, annotations.get(annotations.size() - 1).getValue().length());
    }

}
