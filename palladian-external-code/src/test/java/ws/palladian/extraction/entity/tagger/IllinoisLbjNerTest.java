package ws.palladian.extraction.entity.tagger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

public class IllinoisLbjNerTest {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(IllinoisLbjNerTest.class);

    /** Necessary data file for the NER. Not included by default. Put it at the given path, to run this test. */
    private static final String BROWN_CLUSTERS_FILE = "data/models/illinoisner/data/BrownHierarchicalWordClusters/brownBllipClusters";

    private String trainingFile;
    private String testFile;

    @Before
    public void setUp() throws Exception {
        // skip the test, if the data file is missing; file is big and therefor not included by default.
        if (!new File(BROWN_CLUSTERS_FILE).isFile()) {
            LOGGER.warn("Data file \"{}\" is missing, Test {} will be skipped.", BROWN_CLUSTERS_FILE,
                    IllinoisLbjNer.class.getCanonicalName());
            Assume.assumeTrue(false);
        }
        trainingFile = ResourceHelper.getResourcePath("/ner/training.txt");
        testFile = ResourceHelper.getResourcePath("/ner/test.txt");
    }

    @After
    public void tearDown() throws Exception {
        trainingFile = null;
        testFile = null;
    }

    /**
     * <p>
     * For no apparent reason the Illinois NER test is non-deterministic. This test requires a model to be present at:
     * {@value #BROWN_CLUSTERS_FILE}.
     * </p>
     */
    @Test
    @Ignore
    // not working with maven currently
    public void testIllinoisNer() {

        IllinoisLbjNer tagger = new IllinoisLbjNer(2, false);

        String modelFile = new File(FileHelper.getTempDir(), "lbj.model").getPath();
        tagger.train(trainingFile, modelFile);

        EvaluationResult er = tagger.evaluate(testFile, TaggingFormat.COLUMN);

        // precision MUC: 78.28%, recall MUC: 80.07%, F1 MUC: 79.16%
        // precision exact: 69.3%, recall exact: 70.89%, F1 exact: 70.09%
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
        assertTrue(er.getF1(EvaluationMode.MUC) > 0.77);
        assertTrue(er.getF1(EvaluationMode.EXACT_MATCH) > 0.68);

        tagger.loadModel(modelFile);
        List<Annotation> annotations = tagger.getAnnotations(FileFormatParser.getText(testFile, TaggingFormat.COLUMN));
        // annotations.removeNestedAnnotations();
        // annotations.sort();

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        assertTrue(annotations.size() > 2100);
        assertEquals(21, annotations.get(0).getStartPosition());
        assertEquals(14, annotations.get(0).getLength());

        // assertEquals(annotations.get(500).getOffset(), 14506);
        // assertEquals(annotations.get(500).getLength(), 10);

        assertEquals(105072, annotations.get(annotations.size() - 1).getStartPosition());
        assertEquals(5, annotations.get(annotations.size() - 1).getLength());
    }

}
