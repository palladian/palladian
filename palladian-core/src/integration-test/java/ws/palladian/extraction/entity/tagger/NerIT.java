package ws.palladian.extraction.entity.tagger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ws.palladian.extraction.entity.tagger.PalladianNerSettings.LanguageMode.English;
import static ws.palladian.extraction.entity.tagger.PalladianNerSettings.LanguageMode.LanguageIndependent;
import static ws.palladian.extraction.entity.tagger.PalladianNerSettings.TrainingMode.Complete;

import java.io.File;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.core.Annotation;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode;
import ws.palladian.extraction.entity.tagger.PalladianNerSettings.LanguageMode;
import ws.palladian.extraction.entity.tagger.PalladianNerSettings.TrainingMode;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.integrationtests.ITHelper;

/**
 * <p>
 * Tests the functionality of all Named Entity Recognition Algorithms implemented or wrapped in Palladian.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public class NerIT {

    private static Configuration config;
    private static File tempDirectory;

    @BeforeClass
    public static void setUp() throws ConfigurationException {
        ITHelper.assertMemory(750, SizeUnit.MEGABYTES);
        config = ITHelper.getTestConfig();
        tempDirectory = FileHelper.getTempDir();
    }

    @AfterClass
    public static void cleanUp() {
        config = null;
        tempDirectory = null;
    }

    @Test
    public void test_PalladianNerLi_CoNLL() {
        String trainPath = config.getString("dataset.conll.train");
        String testPath = config.getString("dataset.conll.test");
        ITHelper.assumeFile(trainPath, testPath);

        PalladianNerSettings settings = new PalladianNerSettings(LanguageIndependent, Complete);
        settings.setTagUrls(false);
        settings.setTagDates(false);
        PalladianNer tagger = new PalladianNer(settings);
        String tudnerLiModel = new File(tempDirectory, "tudnerLI.model.gz").getPath();
        boolean traininSuccessful = tagger.train(trainPath, tudnerLiModel);
        assertTrue(traininSuccessful);

        DictionaryModel entityDictionary = tagger.getModel().entityDictionary;
        DictionaryModel caseDictionary = tagger.getModel().caseDictionary;
        DictionaryModel contextClassifier = tagger.getModel().contextModel;
        DictionaryModel annotationDictionary = tagger.getModel().annotationModel;
        assertEquals(2185, entityDictionary.getNumUniqTerms());
        assertNull(caseDictionary);
        assertEquals(645, tagger.getModel().leftContexts.size());
        assertNull(tagger.getModel().removeAnnotations);
        assertEquals(58571, contextClassifier.getNumUniqTerms());
        assertEquals(4, contextClassifier.getNumCategories());
        assertEquals(169640, annotationDictionary.getNumUniqTerms());
        assertEquals(5, annotationDictionary.getNumCategories());

        // Palladian#f8c6aab on testing set
        // precision MUC: 55.95%, recall MUC: 49.91%, F1 MUC: 52.75%
        // precision exact: 42.54%, recall exact: 37.94%, F1 exact: 40.11%
        EvaluationResult er = tagger.evaluate(testPath, TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
        assertTrue(er.getF1(EvaluationMode.MUC) > 0.60);
        assertTrue(er.getF1(EvaluationMode.EXACT_MATCH) > 0.44);

        tagger.loadModel(tudnerLiModel);
        List<Annotation> annotations = tagger.getAnnotations(FileFormatParser.getText(testPath, TaggingFormat.COLUMN));

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        assertEquals(1709, annotations.size());
        assertEquals(21, annotations.get(0).getStartPosition());
        assertEquals(14, annotations.get(0).getValue().length());

        assertEquals(23029, annotations.get(500).getStartPosition());
        assertEquals(5, annotations.get(500).getValue().length());

        assertEquals(105072, annotations.get(annotations.size() - 1).getStartPosition());
        assertEquals(5, annotations.get(annotations.size() - 1).getValue().length());

    }

    @Test
    public void test_PalladianNerEnglish_CoNLL() {
        String trainPath = config.getString("dataset.conll.train");
        String testPath = config.getString("dataset.conll.test");
        ITHelper.assumeFile(trainPath, testPath);

        PalladianNerSettings settings = new PalladianNerSettings(LanguageMode.English, TrainingMode.Complete);
        PalladianNer tagger = new PalladianNer(settings);
        settings.setTagUrls(false);
        settings.setTagDates(false);
        String tudnerEnModel = new File(tempDirectory, "tudnerEn.model.gz").getPath();
        boolean trainingSuccessful = tagger.train(trainPath, tudnerEnModel);
        assertTrue(trainingSuccessful);

        DictionaryModel entityDictionary = tagger.getModel().entityDictionary;
        DictionaryModel caseDictionary = tagger.getModel().caseDictionary;
        DictionaryModel contextDictionary = tagger.getModel().contextModel;
        DictionaryModel annotationDictionary = tagger.getModel().annotationModel;

        assertEquals(2185, entityDictionary.getNumUniqTerms());
        assertEquals(4, entityDictionary.getNumCategories());
        assertEquals(5818, caseDictionary.getNumUniqTerms());
        assertEquals(3, caseDictionary.getNumCategories());
        assertEquals(645, tagger.getModel().leftContexts.size());
        assertEquals(377, tagger.getModel().removeAnnotations.size());
        assertEquals(58571, contextDictionary.getNumUniqTerms());
        assertEquals(4, contextDictionary.getNumCategories());
        assertEquals(102774, annotationDictionary.getNumUniqTerms());
        assertEquals(5, annotationDictionary.getNumCategories());

        // Palladian#f8c6aab on testing set
        // precision MUC: 68.49%, recall MUC: 83.88%, F1 MUC: 75.4%
        // precision exact: 60.13%, recall exact: 73.64%, F1 exact: 66.2%
        EvaluationResult er = tagger.evaluate(testPath, TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
        assertTrue(er.getF1(EvaluationMode.MUC) > 0.83);
        assertTrue(er.getF1(EvaluationMode.EXACT_MATCH) > 0.73);

        tagger.loadModel(tudnerEnModel);
        List<Annotation> annotations = tagger.getAnnotations(FileFormatParser.getText(testPath, TaggingFormat.COLUMN));

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        assertEquals(2166, annotations.size());
        assertEquals(21, annotations.get(0).getStartPosition());
        assertEquals(14, annotations.get(0).getValue().length());

        assertEquals(15437, annotations.get(500).getStartPosition());
        assertEquals(12, annotations.get(500).getValue().length());

        assertEquals(105072, annotations.get(annotations.size() - 1).getStartPosition());
        assertEquals(5, annotations.get(annotations.size() - 1).getValue().length());

    }

    @Test
    public void test_PalladianNerEnglish_TUDCS4() {
        String trainPath = config.getString("dataset.tudcs4.train");
        String testPath = config.getString("dataset.tudcs4.test");
        ITHelper.assumeFile(trainPath, testPath);
        PalladianNerSettings settings = new PalladianNerSettings(English, Complete);
        settings.setTagUrls(false);
        settings.setTagDates(false);
        PalladianNer ner = new PalladianNer(settings);
        ner.train(new File(trainPath), new File(tempDirectory, "palladianNerTUDCS4.model.gz"));
        EvaluationResult result = ner.evaluate(testPath, TaggingFormat.COLUMN);
        // precision MUC: 50.84%, recall MUC: 54.87%, F1 MUC: 52.78%
        // precision exact: 28.71%, recall exact: 30.99%, F1 exact: 29.81%
        // System.out.println(result.getMUCResultsReadable());
        // System.out.println(result.getExactMatchResultsReadable());
        ITHelper.assertMin("F1-MUC", 0.52, result.getF1(EvaluationMode.MUC));
        ITHelper.assertMin("F1-Exact", 0.31, result.getF1(EvaluationMode.EXACT_MATCH));
    }

    @Test
    public void test_PalladianNerLanguageIndependent_TUDCS4() {
        String trainPath = config.getString("dataset.tudcs4.train");
        String testPath = config.getString("dataset.tudcs4.test");
        ITHelper.assumeFile(trainPath, testPath);
        PalladianNerSettings settings = new PalladianNerSettings(LanguageIndependent, Complete);
        settings.setTagUrls(false);
        settings.setTagDates(false);
        PalladianNer ner = new PalladianNer(settings);
        ner.train(new File(trainPath), new File(tempDirectory, "palladianNerTUDCS4.model.gz"));
        EvaluationResult result = ner.evaluate(testPath, TaggingFormat.COLUMN);
        // precision MUC: 50.38%, recall MUC: 16.56%, F1 MUC: 24.93%
        // precision exact: 34.23%, recall exact: 11.25%, F1 exact: 16.93%
        // System.out.println(result.getMUCResultsReadable());
        // System.out.println(result.getExactMatchResultsReadable());
        ITHelper.assertMin("F1-MUC", 0.26, result.getF1(EvaluationMode.MUC));
        ITHelper.assertMin("F1-Exact", 0.16, result.getF1(EvaluationMode.EXACT_MATCH));
    }

    @Test
    public void test_StanfordNer_CoNLL() {
        String trainPath = config.getString("dataset.conll.train");
        String testPath = config.getString("dataset.conll.test");
        ITHelper.assumeFile(trainPath, testPath);

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
        assertTrue(er.getF1(EvaluationMode.EXACT_MATCH) > 0.75);

        List<Annotation> annotations = tagger.getAnnotations(FileFormatParser.getText(testPath, TaggingFormat.COLUMN));

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        assertEquals(2044, annotations.size());
        assertEquals(21, annotations.get(0).getStartPosition());
        assertEquals(14, annotations.get(0).getValue().length());

        assertEquals(17692, annotations.get(500).getStartPosition());
        assertEquals(4, annotations.get(500).getValue().length());

        assertEquals(105072, annotations.get(annotations.size() - 1).getStartPosition());
        assertEquals(5, annotations.get(annotations.size() - 1).getValue().length());
    }

    @Test
    public void test_LingPipeNer_CoNLL() {
        String trainPath = config.getString("dataset.conll.train");
        String testPath = config.getString("dataset.conll.test");
        ITHelper.assumeFile(trainPath, testPath);

        String lingpipeNerModelFile = new File(tempDirectory, "lingpipe.model").getPath();
        LingPipeNer tagger = new LingPipeNer();
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

        assertEquals(1906, annotations.size());
        assertEquals(21, annotations.get(0).getStartPosition());
        assertEquals(14, annotations.get(0).getValue().length());

        assertEquals(17108, annotations.get(500).getStartPosition());
        assertEquals(5, annotations.get(500).getValue().length());

        assertEquals(105048, annotations.get(annotations.size() - 1).getStartPosition());
        assertEquals(6, annotations.get(annotations.size() - 1).getValue().length());
    }

    @Test
    public void test_OpenNlpNer_CoNLL() {
        String trainPath = config.getString("dataset.conll.train");
        String testPath = config.getString("dataset.conll.test");
        ITHelper.assumeFile(trainPath, testPath);

        String openNlpModelFile = new File(tempDirectory, "openNLP.model").getPath();
        OpenNlpNer tagger = new OpenNlpNer();

        tagger.train(trainPath, openNlpModelFile);
        tagger.loadModel(openNlpModelFile);

        // precision MUC: 60.72%, recall MUC: 54.67%, F1 MUC: 57.54%
        // precision exact: 52.15%, recall exact: 46.96%, F1 exact: 49.42%
        EvaluationResult er = tagger.evaluate(testPath, TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
        assertTrue(er.getF1(EvaluationMode.MUC) > 0.57);
        assertTrue(er.getF1(EvaluationMode.EXACT_MATCH) > 0.49);

        List<Annotation> annotations = tagger.getAnnotations(FileFormatParser.getText(testPath, TaggingFormat.COLUMN));

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        assertEquals(1924, annotations.size());
        assertEquals(2, annotations.get(0).getStartPosition());
        assertEquals(8, annotations.get(0).getValue().length());

        assertEquals(16348, annotations.get(500).getStartPosition());
        assertEquals(1, annotations.get(500).getValue().length());

        assertEquals(105072, annotations.get(annotations.size() - 1).getStartPosition());
        assertEquals(5, annotations.get(annotations.size() - 1).getValue().length());
    }

    @Test
    @Ignore
    // Different results when run locally in Eclipse and on Jenkins...ignore for now.
    public void test_JulieNer_CoNLL() {
        String trainPath = config.getString("dataset.conll.train");
        String testPath = config.getString("dataset.conll.test");
        ITHelper.assumeFile(trainPath, testPath);

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
