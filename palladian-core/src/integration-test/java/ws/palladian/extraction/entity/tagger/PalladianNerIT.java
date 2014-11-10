package ws.palladian.extraction.entity.tagger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ws.palladian.extraction.entity.TaggingFormat.COLUMN;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode.EXACT_MATCH;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode.MUC;

import java.io.File;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.tagger.PalladianNerTrainingSettings.Builder;
import ws.palladian.helper.constants.SizeUnit;
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
        ITHelper.assumeFile("CoNLL", trainPath, testPath);

        PalladianNerTrainingSettings settings = PalladianNerTrainingSettings.Builder.languageIndependent().create();
        PalladianNer tagger = new PalladianNer(settings);
        String tudnerLiModel = new File(tempDirectory, "tudnerLI.model.gz").getPath();
        boolean traininSuccessful = tagger.train(trainPath, tudnerLiModel);
        assertTrue(traininSuccessful);

        DictionaryModel entityDictionary = tagger.getModel().entityDictionary;
        DictionaryModel contextClassifier = tagger.getModel().contextDictionary;
        DictionaryModel annotationDictionary = tagger.getModel().annotationDictionary;
        Set<String> lowercaseDictionary = tagger.getModel().lowerCaseDictionary;
        assertEquals(2185, entityDictionary.getNumUniqTerms());
        assertNull(lowercaseDictionary);
        assertEquals(591, tagger.getModel().leftContexts.size());
        assertNull(tagger.getModel().removeAnnotations);
        assertEquals(59051, contextClassifier.getNumUniqTerms());
        assertEquals(4, contextClassifier.getNumCategories());
        assertEquals(26575, annotationDictionary.getNumUniqTerms());
        assertEquals(5, annotationDictionary.getNumCategories());

        // Palladian#f8c6aab on testing set
        // precision MUC: 55.95%, recall MUC: 49.91%, F1 MUC: 52.75%
        // precision exact: 42.54%, recall exact: 37.94%, F1 exact: 40.11%
        EvaluationResult er = tagger.evaluate(testPath, COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
        ITHelper.assertMin("F1-MUC", 0.61, er.getF1(MUC));
        ITHelper.assertMin("F1-Exact", 0.44, er.getF1(EXACT_MATCH));
    }

    @Test
    public void test_PalladianNerEnglish_CoNLL() {
        String trainPath = config.getString("dataset.conll.train");
        String testPath = config.getString("dataset.conll.test");
        ITHelper.assumeFile("CoNLL", trainPath, testPath);

        PalladianNerTrainingSettings settings = PalladianNerTrainingSettings.Builder.english().create();
        PalladianNer tagger = new PalladianNer(settings);
        String tudnerEnModel = new File(tempDirectory, "tudnerEn.model.gz").getPath();
        boolean trainingSuccessful = tagger.train(trainPath, tudnerEnModel);
        assertTrue(trainingSuccessful);

        DictionaryModel entityDictionary = tagger.getModel().entityDictionary;
        DictionaryModel contextDictionary = tagger.getModel().contextDictionary;
        DictionaryModel annotationDictionary = tagger.getModel().annotationDictionary;
        Set<String> lowercaseDictionary = tagger.getModel().lowerCaseDictionary;
        assertEquals(2185, entityDictionary.getNumUniqTerms());
        assertEquals(4, entityDictionary.getNumCategories());
        assertEquals(3435, lowercaseDictionary.size());
        assertEquals(591, tagger.getModel().leftContexts.size());
        assertEquals(183, tagger.getModel().removeAnnotations.size());
        assertEquals(59051, contextDictionary.getNumUniqTerms());
        assertEquals(4, contextDictionary.getNumCategories());
        assertEquals(17293, annotationDictionary.getNumUniqTerms());
        assertEquals(5, annotationDictionary.getNumCategories());

        // Palladian#f8c6aab on testing set
        // precision MUC: 68.49%, recall MUC: 83.88%, F1 MUC: 75.4%
        // precision exact: 60.13%, recall exact: 73.64%, F1 exact: 66.2%
        EvaluationResult er = tagger.evaluate(testPath, COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
        ITHelper.assertMin("F1-MUC", 0.84, er.getF1(MUC));
        ITHelper.assertMin("F1-Exact", 0.74, er.getF1(EXACT_MATCH));
    }

    @Test
    public void test_PalladianNerEnglish_TUDCS4() {
        String trainPath = config.getString("dataset.tudcs4.train");
        String testPath = config.getString("dataset.tudcs4.test");
        ITHelper.assumeFile("TUDCS4", trainPath, testPath);
        PalladianNerTrainingSettings settings = PalladianNerTrainingSettings.Builder.english().create();
        PalladianNer ner = new PalladianNer(settings);
        ner.train(new File(trainPath), new File(tempDirectory, "palladianNerTUDCS4.model.gz"));
        EvaluationResult result = ner.evaluate(testPath, COLUMN);
        // precision MUC: 50.84%, recall MUC: 54.87%, F1 MUC: 52.78%
        // precision exact: 28.71%, recall exact: 30.99%, F1 exact: 29.81%
        // System.out.println(result.getMUCResultsReadable());
        // System.out.println(result.getExactMatchResultsReadable());
        ITHelper.assertMin("F1-MUC", 0.54, result.getF1(MUC));
        ITHelper.assertMin("F1-Exact", 0.31, result.getF1(EXACT_MATCH));
    }

    @Test
    public void test_PalladianNerLanguageIndependent_TUDCS4() {
        String trainPath = config.getString("dataset.tudcs4.train");
        String testPath = config.getString("dataset.tudcs4.test");
        ITHelper.assumeFile("TUDCS4", trainPath, testPath);
        PalladianNerTrainingSettings settings = PalladianNerTrainingSettings.Builder.languageIndependent().create();
        PalladianNer ner = new PalladianNer(settings);
        ner.train(new File(trainPath), new File(tempDirectory, "palladianNerTUDCS4.model.gz"));
        EvaluationResult result = ner.evaluate(testPath, COLUMN);
        // precision MUC: 50.38%, recall MUC: 16.56%, F1 MUC: 24.93%
        // precision exact: 34.23%, recall exact: 11.25%, F1 exact: 16.93%
        // System.out.println(result.getMUCResultsReadable());
        // System.out.println(result.getExactMatchResultsReadable());
        ITHelper.assertMin("F1-MUC", 0.26, result.getF1(MUC));
        ITHelper.assertMin("F1-Exact", 0.16, result.getF1(EXACT_MATCH));
    }

    @Test
    public void test_PalladianNerSparse_Wikipedia_CoNLL() {
        ITHelper.assertMemory(1500, SizeUnit.MEGABYTES);
        String trainPath = config.getString("dataset.wikipediaEntity.train");
        String testPath = config.getString("dataset.conll.test");
        ITHelper.assumeFile("Wikipedia Entity", trainPath);
        ITHelper.assumeFile("CoNLL", testPath);
        Builder builder = PalladianNerTrainingSettings.Builder.english();
        builder.sparse();
        builder.minDictCount(5);
        builder.equalizeTypeCounts();
        File trainingFile = new File(trainPath);
        PalladianNer ner = new PalladianNer(builder.create());
        ner.train(trainingFile, new File(tempDirectory, "palladianNerWikipedia.model.gz"));
        EvaluationResult result = ner.evaluate(testPath, COLUMN);
        // System.out.println(result.getMUCResultsReadable());
        // System.out.println(result.getExactMatchResultsReadable());
        // precision MUC: 80.45%, recall MUC: 84.63%, F1 MUC: 82.49%
        // precision exact: 70.9%, recall exact: 74.58%, F1 exact: 72.69%
        ITHelper.assertMin("F1-MUC", 0.81, result.getF1(MUC));
        ITHelper.assertMin("F1-Exact", 0.69, result.getF1(EXACT_MATCH));
    }

}
