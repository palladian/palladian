package ws.palladian.extraction.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.tagger.JulieNer;
import ws.palladian.extraction.entity.tagger.LingPipeNer;
import ws.palladian.extraction.entity.tagger.OpenNlpNer;
import ws.palladian.extraction.entity.tagger.PalladianNer;
import ws.palladian.extraction.entity.tagger.PalladianNer.LanguageMode;
import ws.palladian.extraction.entity.tagger.StanfordNer;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.ResourceHelper;

/**
 * <p>
 * Tests the functionality of all Named Entity Recognition Algorithms implemented or wrapped in Palladian.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @version 1.0
 * @since 1.0
 * 
 */
public class NERTest {

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
    public void testPalladianNer() throws FileNotFoundException {

        // language independent
        PalladianNer tagger = new PalladianNer();
        tagger.setLanguageMode(LanguageMode.LanguageIndependent);
        String tudnerLiModel = ResourceHelper.getResourcePath("/ner/tudnerLI.model.gz");
        tagger.train(trainingFile, tudnerLiModel);

        // precision MUC: 63.07%, recall MUC: 75.17%, F1 MUC: 68.59%
        // precision exact: 48.89%, recall exact: 58.26%, F1 exact: 53.17%
        EvaluationResult er = tagger
                .evaluate(ResourceHelper.getResourcePath("/ner/training.txt"), TaggingFormat.COLUMN);
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());
        
        assertTrue(er.getF1(EvaluationResult.MUC) > 0.68);
        assertTrue(er.getF1(EvaluationResult.EXACT_MATCH) > 0.52);

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

        // English
        tagger = new PalladianNer();
        tagger.setLanguageMode(LanguageMode.English);
        tagger.setTagUrls(false);
        tagger.setTagDates(false);
        String tudnerEnModel = ResourceHelper.getResourcePath("/ner/tudnerEn.model.gz");
        tagger.train(trainingFile, tudnerEnModel);

        // precision MUC: 94.18%, recall MUC: 94.92%, F1 MUC: 94.55%
        // precision exact: 90.53%, recall exact: 91.24%, F1 exact: 90.88%
        er = tagger.evaluate(ResourceHelper.getResourcePath("/ner/training.txt"), TaggingFormat.COLUMN);
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());
        
        assertTrue(er.getF1(EvaluationResult.MUC) > 0.94);
        assertTrue(er.getF1(EvaluationResult.EXACT_MATCH) > 0.90);


        tagger.loadModel(tudnerEnModel);
        tagger.setTagUrls(false);
        tagger.setTagDates(false);
        annotations = tagger.getAnnotations(FileFormatParser.getText(testFile, TaggingFormat.COLUMN));
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
    public void testStanfordNER() throws FileNotFoundException {
        StanfordNer tagger = new StanfordNer();

        String stanfordNerModel = ResourceHelper.getResourcePath("/ner/stanfordner.ser.gz");
        tagger.train(trainingFile, stanfordNerModel);

        // EvaluationResult er = tagger.evaluate(ResourceHelper.getResourcePath("/ner/test.txt"),
        // ResourceHelper.getResourcePath("/ner/stanfordner.ser.gz"),
        // TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel(stanfordNerModel);
        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText(testFile, TaggingFormat.COLUMN));
        annotations.removeNestedAnnotations();
        annotations.sort();

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        assertEquals(2048, annotations.size());
        assertEquals(annotations.get(0).getOffset(), 21);
        assertEquals(annotations.get(0).getLength(), 14);

        assertEquals(annotations.get(500).getOffset(), 17692);
        assertEquals(annotations.get(500).getLength(), 4);

        assertEquals(annotations.get(annotations.size() - 1).getOffset(), 105072);
        assertEquals(annotations.get(annotations.size() - 1).getLength(), 5);
    }

    // /**
    // * For no apparent reason the Illinois NER test is non-deterministic.
    // * To enable this test you must have a valid model at:
    // * data\models\illinoisner\data\BrownHierarchicalWordClusters\brownBllipClusters
    // * @throws FileNotFoundException
    // */
    // @Test
    // @Ignore
    // public void testIllinoisNER() throws FileNotFoundException {
    // String illinoisNerModelFile = ResourceHelper.getResourcePath("/ner/lbj.model");
    // IllinoisLbjNer tagger = new IllinoisLbjNer();
    //
    // tagger.setTrainingRounds(2);
    // tagger.train(trainingFile, illinoisNerModelFile);
    //
    // // EvaluationResult er = tagger.evaluate(ResourceHelper.getResourcePath("/ner/test.txt"),
    // // ResourceHelper.getResourcePath("/ner/lbj.model")ResourceHelper.getResourcePath, TaggingFormat.COLUMN);
    // // System.out.println(er.getMUCResultsReadable());
    // // System.out.println(er.getExactMatchResultsReadable());
    //
    // tagger.loadModel(illinoisNerModelFile);
    // Annotations annotations = tagger.getAnnotations(FileFormatParser.getText(testFile, TaggingFormat.COLUMN));
    // annotations.removeNestedAnnotations();
    // annotations.sort();
    //
    // // System.out.println(annotations.size());
    // // System.out.println(annotations.get(0));
    // // System.out.println(annotations.get(500));
    // // System.out.println(annotations.get(annotations.size() - 1));
    //
    // assertEquals(21.0, MathHelper.round(annotations.size() / 100, 0), 0);
    // assertEquals(annotations.get(0).getOffset(), 21);
    // assertEquals(annotations.get(0).getLength(), 14);
    //
    // // assertEquals(annotations.get(500).getOffset(), 14506);
    // // assertEquals(annotations.get(500).getLength(), 10);
    //
    // assertEquals(annotations.get(annotations.size() - 1).getOffset(), 105072);
    // assertEquals(annotations.get(annotations.size() - 1).getLength(), 5);
    // }

    @Test
    public void testLingPipeNER() throws FileNotFoundException {
        String lingpipeNerModelFile = ResourceHelper.getResourcePath("/ner/lingpipe.model");
        LingPipeNer tagger = new LingPipeNer();
        tagger.train(trainingFile, lingpipeNerModelFile);
        // EvaluationResult er = tagger.evaluate(ResourceHelper.getResourcePath("/ner/test.txt").getFile(),
        // ResourceHelper.getResourcePath("/ner/lingpipe.model"),
        // TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

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
    public void testOpenNlpNer() throws FileNotFoundException {
        String openNlpModelFile = ResourceHelper.getResourcePath("/ner/openNLP.bin");
        OpenNlpNer tagger = new OpenNlpNer();

        tagger.train(trainingFile, openNlpModelFile);

        // EvaluationResult er = tagger.evaluate(
        // ResourceHelper.getResourcePath("/ner/test.txt"),
        // ResourceHelper.getResourcePath("/ner/openNLP_PER.bin") + ","
        // + ResourceHelper.getResourcePath("/ner/openNLP_MISC.bin") + ","
        // + ResourceHelper.getResourcePath("/ner/openNLP_LOC.bin") + ","
        // + ResourceHelper.getResourcePath("/ner/openNLP_ORG.bin"), TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel(ResourceHelper.getResourcePath("/ner/"));

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

    /**
     * Different results when run locally in Eclipse and on Jenkins...ignore for now.
     * 
     * @throws FileNotFoundException
     */
    @Test
    @Ignore
    public void testJulieNER() throws FileNotFoundException {
        JulieNer tagger = new JulieNer();
        String julieNerModelFile = ResourceHelper.getResourcePath("/ner/juliener.mod");
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