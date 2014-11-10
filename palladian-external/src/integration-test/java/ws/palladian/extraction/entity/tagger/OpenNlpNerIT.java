package ws.palladian.extraction.entity.tagger;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode;
import ws.palladian.extraction.location.ClassifiedAnnotation;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.integrationtests.ITHelper;

public class OpenNlpNerIT {
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
    public void test_OpenNlpNer_CoNLL() throws InvalidFormatException, IOException {
        File tokenizerModel = ResourceHelper.getResourceFile("/model/en-token.bin");
        File sentenceModel = ResourceHelper.getResourceFile("/model/en-sent.bin");
        Tokenizer tokenizer = new TokenizerME(new TokenizerModel(tokenizerModel));
        SentenceDetector sentenceDetector = new SentenceDetectorME(new SentenceModel(sentenceModel));
        String openNlpModelFile = new File(tempDirectory, "openNLP.model").getPath();
        OpenNlpNer tagger = new OpenNlpNer(tokenizer, sentenceDetector);

        tagger.train(trainPath, openNlpModelFile);
        tagger.loadModel(openNlpModelFile);

        // precision MUC: 60.72%, recall MUC: 54.67%, F1 MUC: 57.54%
        // precision exact: 52.15%, recall exact: 46.96%, F1 exact: 49.42%
        EvaluationResult er = tagger.evaluate(testPath, TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
        ITHelper.assertMin("F1-MUC", 0.73, er.getF1(EvaluationMode.MUC));
        ITHelper.assertMin("F1-Exact", 0.67, er.getF1(EvaluationMode.MUC));

        List<ClassifiedAnnotation> annotations = tagger.getAnnotations(FileFormatParser.getText(testPath,
                TaggingFormat.COLUMN));

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        assertEquals(1720, annotations.size());
        assertEquals(9, annotations.get(0).getStartPosition());
        assertEquals(14, annotations.get(0).getValue().length());

        assertEquals(20692, annotations.get(500).getStartPosition());
        assertEquals(15, annotations.get(500).getValue().length());

        assertEquals(104279, annotations.get(annotations.size() - 1).getStartPosition());
        assertEquals(5, annotations.get(annotations.size() - 1).getValue().length());
    }
}
