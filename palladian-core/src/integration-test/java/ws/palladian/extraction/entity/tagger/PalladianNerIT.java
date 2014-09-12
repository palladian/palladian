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
import org.junit.Test;

import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.core.Annotation;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode;
import ws.palladian.extraction.entity.tagger.PalladianNerSettings.LanguageMode;
import ws.palladian.extraction.entity.tagger.PalladianNerSettings.TrainingMode;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.integrationtests.ITHelper;

/**
 * <p>
 * Tests the functionality of Palladian's NER.
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public class PalladianNerIT {

    private static Configuration config;
    private static File tempDirectory;

    @BeforeClass
    public static void setUp() throws ConfigurationException {
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
        assertEquals(654, tagger.getModel().leftContexts.size());
        assertNull(tagger.getModel().removeAnnotations);
        assertEquals(59051, contextClassifier.getNumUniqTerms());
        assertEquals(4, contextClassifier.getNumCategories());
        assertEquals(169598, annotationDictionary.getNumUniqTerms());
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
        assertEquals(9, annotations.get(0).getStartPosition());
        assertEquals(14, annotations.get(0).getValue().length());

        assertEquals(22830, annotations.get(500).getStartPosition());
        assertEquals(5, annotations.get(500).getValue().length());

        assertEquals(104279, annotations.get(annotations.size() - 1).getStartPosition());
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
        assertEquals(5817, caseDictionary.getNumUniqTerms());
        assertEquals(3, caseDictionary.getNumCategories());
        assertEquals(654, tagger.getModel().leftContexts.size());
        assertEquals(375, tagger.getModel().removeAnnotations.size());
        assertEquals(59051, contextDictionary.getNumUniqTerms());
        assertEquals(4, contextDictionary.getNumCategories());
        assertEquals(102717, annotationDictionary.getNumUniqTerms());
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

        assertEquals(2187, annotations.size());
        assertEquals(9, annotations.get(0).getStartPosition());
        assertEquals(14, annotations.get(0).getValue().length());

        assertEquals(15248, annotations.get(500).getStartPosition());
        assertEquals(18, annotations.get(500).getValue().length());

        assertEquals(104279, annotations.get(annotations.size() - 1).getStartPosition());
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

}
