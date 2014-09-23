package ws.palladian.extraction.entity.tagger;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.core.Annotation;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.integrationtests.ITHelper;

/**
 * <p>
 * Tests the functionality of external NER algorithms wrapped in Palladian.
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public class ExternalNerIT {

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
    @Ignore
    // Different results when run locally in Eclipse and on Jenkins...ignore for now.
    public void test_JulieNer_CoNLL() {
        JulieNer tagger = new JulieNer();
        String julieNerModelFile = new File(tempDirectory, "juliener.mod").getPath();
        tagger.train(trainPath, julieNerModelFile);

        // EvaluationResult er = tagger.evaluate(ResourceHelper.getResourcePath("/ner/test.txt"),
        // ResourceHelper.getResourcePath("/ner/juliener.mod"), TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel(julieNerModelFile);
        List<Annotation> annotations = tagger.getAnnotations(FileFormatParser.getText(testPath, TaggingFormat.COLUMN));

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        assertEquals(2035, annotations.size());
        assertEquals(76, annotations.get(0).getStartPosition());
        assertEquals(6, annotations.get(0).getValue().length());

        assertEquals(17768, annotations.get(500).getStartPosition());
        assertEquals(7, annotations.get(500).getValue().length());

        assertEquals(105072, annotations.get(annotations.size() - 1).getStartPosition());
        assertEquals(5, annotations.get(annotations.size() - 1).getValue().length());
    }

}
