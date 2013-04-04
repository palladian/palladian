package ws.palladian.extraction.entity.tagger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode;
import ws.palladian.extraction.entity.tagger.PalladianNer.LanguageMode;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

/**
 * <p>
 * Tests the functionality of all Named Entity Recognition Algorithms implemented or wrapped in Palladian.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public class NerTest {

    private String trainingFile;
    private String testFile;

    @Before
    public void setUp() throws Exception {
        trainingFile = ResourceHelper.getResourcePath("/ner/training.txt");
        testFile = ResourceHelper.getResourcePath("/ner/test.txt");
    }

    @After
    public void tearDown() throws Exception {
        trainingFile = null;
        testFile = null;
    }

    @Test
    public void testPalladianNerLi() {
        PalladianNer tagger = new PalladianNer(LanguageMode.LanguageIndependent);
        String tudnerLiModel = new File(FileHelper.getTempDir(), "tudnerLI.model.gz").getPath();
        boolean traininSuccessful = tagger.train(trainingFile, tudnerLiModel);
        assertTrue(traininSuccessful);

        DictionaryModel caseDictionary = tagger.getCaseDictionary();
        DictionaryModel contextClassifier = tagger.getContextClassifier();
        DictionaryModel annotationDictionary = tagger.getAnnotationDictionary();
        assertEquals(2185, tagger.getEntityDictionary().getNumTerms());
        assertEquals(0, caseDictionary.getNumTerms());
        assertEquals(0, caseDictionary.getNumCategories());
        assertEquals(1103, tagger.getLeftContextMap().size());
        assertEquals(0, tagger.getRemoveAnnotations().size());
        assertEquals(89639, contextClassifier.getNumTerms());
        assertEquals(4, contextClassifier.getNumCategories());
        assertEquals(54040, annotationDictionary.getNumTerms());
        assertEquals(5, annotationDictionary.getNumCategories());

        // precision MUC: 63.07%, recall MUC: 75.17%, F1 MUC: 68.59%
        // precision exact: 48.89%, recall exact: 58.26%, F1 exact: 53.17%
        EvaluationResult er = tagger.evaluate(trainingFile, TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
        
        assertTrue(er.getF1(EvaluationMode.MUC) > 0.68);
        assertTrue(er.getF1(EvaluationMode.EXACT_MATCH) > 0.52);

        tagger.loadModel(tudnerLiModel);
        tagger.setTagUrls(false);
        tagger.setTagDates(false);
        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText(testFile, TaggingFormat.COLUMN));
        annotations.removeNestedAnnotations();
        annotations.sort();

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

//        assertEquals(1504, annotations.size());
//        assertEquals(21, annotations.get(0).getOffset());
//        assertEquals(14, annotations.get(0).getLength());
//
//        assertEquals(25542, annotations.get(500).getOffset());
//        assertEquals(7, annotations.get(500).getLength());
//
//        assertEquals(105072, annotations.get(annotations.size() - 1).getOffset());
//        assertEquals(5, annotations.get(annotations.size() - 1).getLength());

    }

    @Test
    public void testPalladianNerEnglish() {
        PalladianNer tagger = new PalladianNer(LanguageMode.English);
        tagger.setTagUrls(false);
        tagger.setTagDates(false);
        String tudnerEnModel = new File(FileHelper.getTempDir(), "tudnerEn.model.gz").getPath();
        boolean trainingSuccessful = tagger.train(trainingFile, tudnerEnModel);
        assertTrue(trainingSuccessful);

        DictionaryModel entityDictionary = tagger.getEntityDictionary();
        DictionaryModel caseDictionary = tagger.getCaseDictionary();
        DictionaryModel contextDictionary = tagger.getContextClassifier();
        DictionaryModel annotationDictionary = tagger.getAnnotationDictionary();
        assertEquals(2185, entityDictionary.getNumTerms());
        assertEquals(4, entityDictionary.getNumCategories());
        assertEquals(5818, caseDictionary.getNumTerms());
        assertEquals(3, caseDictionary.getNumCategories());
        assertEquals(1103, tagger.getLeftContextMap().size());
        assertEquals(1109, tagger.getRemoveAnnotations().size());
        assertEquals(89639, contextDictionary.getNumTerms());
        assertEquals(4, contextDictionary.getNumCategories());
        assertEquals(59597, annotationDictionary.getNumTerms());
        assertEquals(5, annotationDictionary.getNumCategories());

        // precision MUC: 94.18%, recall MUC: 94.92%, F1 MUC: 94.55%
        // precision exact: 90.53%, recall exact: 91.24%, F1 exact: 90.88%
        EvaluationResult er = tagger.evaluate(trainingFile, TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
        
        assertTrue(er.getF1(EvaluationMode.MUC) > 0.94);
        assertTrue(er.getF1(EvaluationMode.EXACT_MATCH) > 0.90);

        tagger.loadModel(tudnerEnModel);
        tagger.setTagUrls(false);
        tagger.setTagDates(false);
        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText(testFile, TaggingFormat.COLUMN));
        annotations.removeNestedAnnotations();
        annotations.sort();

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        // CollectionHelper.print(annotations);

//        assertEquals(2241, annotations.size());
//        assertEquals(21, annotations.get(0).getOffset());
//        assertEquals(14, annotations.get(0).getLength());
//
//        assertEquals(15212, annotations.get(500).getOffset());
//        assertEquals(8, annotations.get(500).getLength());
//
//        assertEquals(105072, annotations.get(annotations.size() - 1).getOffset());
//        assertEquals(5, annotations.get(annotations.size() - 1).getLength());

    }

    @Test
    public void testStanfordNer() {
        StanfordNer tagger = new StanfordNer();

        String stanfordNerModel = new File(FileHelper.getTempDir(), "stanfordner.ser.gz").getPath();
        tagger.train(trainingFile, stanfordNerModel);
        tagger.loadModel(stanfordNerModel);

        // precision MUC: 85.22%, recall MUC: 83.55%, F1 MUC: 84.38%
        // precision exact: 76.6%, recall exact: 75.09%, F1 exact: 75.84%
        EvaluationResult er = tagger.evaluate(testFile, TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
        assertTrue(er.getF1(EvaluationMode.MUC) > 0.84);
        assertTrue(er.getF1(EvaluationMode.EXACT_MATCH) > 0.75);

        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText(testFile, TaggingFormat.COLUMN));
        annotations.removeNestedAnnotations();
        annotations.sort();

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        assertEquals(2044, annotations.size());
        assertEquals(annotations.get(0).getOffset(), 21);
        assertEquals(annotations.get(0).getLength(), 14);

        assertEquals(annotations.get(500).getOffset(), 17692);
        assertEquals(annotations.get(500).getLength(), 4);

        assertEquals(annotations.get(annotations.size() - 1).getOffset(), 105072);
        assertEquals(annotations.get(annotations.size() - 1).getLength(), 5);
    }

    @Test
    public void testLingPipeNer() {
        String lingpipeNerModelFile = new File(FileHelper.getTempDir(), "lingpipe.model").getPath();
        LingPipeNer tagger = new LingPipeNer();
        tagger.train(trainingFile, lingpipeNerModelFile);
        tagger.loadModel(lingpipeNerModelFile);

        // precision MUC: 81.93%, recall MUC: 74.04%, F1 MUC: 77.79%
        // precision exact: 72.96%, recall exact: 65.93%, F1 exact: 69.27%
        EvaluationResult er = tagger.evaluate(testFile, TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
        assertTrue(er.getF1(EvaluationMode.MUC) > 0.77);
        assertTrue(er.getF1(EvaluationMode.EXACT_MATCH) > 0.69);

        tagger.loadModel(lingpipeNerModelFile);
        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText(testFile, TaggingFormat.COLUMN));
        annotations.removeNestedAnnotations();
        annotations.sort();

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        assertEquals(1906, annotations.size());
        assertEquals(annotations.get(0).getOffset(), 21);
        assertEquals(annotations.get(0).getLength(), 14);

        assertEquals(annotations.get(500).getOffset(), 17108);
        assertEquals(annotations.get(500).getLength(), 5);

        assertEquals(annotations.get(annotations.size() - 1).getOffset(), 105048);
        assertEquals(annotations.get(annotations.size() - 1).getLength(), 6);
    }

    @Test
    public void testOpenNlpNer() {
        String openNlpModelFile = new File(FileHelper.getTempDir(), "openNLP.bin").getPath();
        OpenNlpNer tagger = new OpenNlpNer();

        tagger.train(trainingFile, openNlpModelFile);
        tagger.loadModel(FileHelper.getTempDir().getPath());

        // precision MUC: 60.72%, recall MUC: 54.67%, F1 MUC: 57.54%
        // precision exact: 52.15%, recall exact: 46.96%, F1 exact: 49.42%
        EvaluationResult er = tagger.evaluate(testFile, TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
        assertTrue(er.getF1(EvaluationMode.MUC) > 0.57);
        assertTrue(er.getF1(EvaluationMode.EXACT_MATCH) > 0.49);

        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText(testFile, TaggingFormat.COLUMN));
        annotations.removeNestedAnnotations();
        annotations.sort();

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        assertEquals(1924, annotations.size());
        assertEquals(annotations.get(0).getOffset(), 2);
        assertEquals(annotations.get(0).getLength(), 8);

        assertEquals(annotations.get(500).getOffset(), 16348);
        assertEquals(annotations.get(500).getLength(), 1);

        assertEquals(annotations.get(annotations.size() - 1).getOffset(), 105072);
        assertEquals(annotations.get(annotations.size() - 1).getLength(), 5);
    }

    @Test
    @Ignore
    // Different results when run locally in Eclipse and on Jenkins...ignore for now.
    public void testJulieNer() {
        JulieNer tagger = new JulieNer();
        String julieNerModelFile = new File(FileHelper.getTempDir(), "juliener.mod").getPath();
        tagger.train(trainingFile, julieNerModelFile);

        // EvaluationResult er = tagger.evaluate(ResourceHelper.getResourcePath("/ner/test.txt"),
        // ResourceHelper.getResourcePath("/ner/juliener.mod"), TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel(julieNerModelFile);
        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText(testFile, TaggingFormat.COLUMN));
        annotations.removeNestedAnnotations();
        annotations.sort();

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        assertEquals(2035, annotations.size());
        assertEquals(annotations.get(0).getOffset(), 76);
        assertEquals(annotations.get(0).getLength(), 6);

        assertEquals(annotations.get(500).getOffset(), 17768);
        assertEquals(annotations.get(500).getLength(), 7);

        assertEquals(annotations.get(annotations.size() - 1).getOffset(), 105072);
        assertEquals(annotations.get(annotations.size() - 1).getLength(), 5);
    }

}