package ws.palladian.extraction.entity.ner;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.helper.math.MathHelper;
import ws.palladian.preprocessing.nlp.ner.Annotations;
import ws.palladian.preprocessing.nlp.ner.FileFormatParser;
import ws.palladian.preprocessing.nlp.ner.TaggingFormat;
import ws.palladian.preprocessing.nlp.ner.tagger.IllinoisLbjNER;
import ws.palladian.preprocessing.nlp.ner.tagger.JulieNER;
import ws.palladian.preprocessing.nlp.ner.tagger.LingPipeNER;
import ws.palladian.preprocessing.nlp.ner.tagger.OpenNLPNER;
import ws.palladian.preprocessing.nlp.ner.tagger.PalladianNer;
import ws.palladian.preprocessing.nlp.ner.tagger.StanfordNER;
import ws.palladian.preprocessing.nlp.ner.tagger.PalladianNer.LanguageMode;

public class NERTest {

    @Test
    public void testPalladianNer() {

        // language independent
        PalladianNer tagger = new PalladianNer();
        tagger.setLanguageMode(LanguageMode.LanguageIndependent);
        tagger.train(NERTest.class.getResource("/ner/training.txt").getFile(),
                NERTest.class.getResource("/ner/tudnerLI.model").getFile());

        // EvaluationResult er = tagger.evaluate(NERTest.class.getResource("/ner/training.txt").getFile(),
        // NERTest.class.getResource("/ner/tudnerLI.model").getFile(), TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel(NERTest.class.getResource("/ner/tudnerLI.model").getFile());
        tagger.setTagUrls(false);
        tagger.setTagDates(false);
        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText(
                NERTest.class.getResource("/ner/test.txt").getFile(), TaggingFormat.COLUMN));
        annotations.removeNestedAnnotations();
        annotations.sort();

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        Assert.assertEquals(1504, annotations.size());
        Assert.assertEquals(annotations.get(0).getOffset(), 21);
        Assert.assertEquals(annotations.get(0).getLength(), 14);

        Assert.assertEquals(annotations.get(500).getOffset(), 25542);
        Assert.assertEquals(annotations.get(500).getLength(), 7);

        Assert.assertEquals(annotations.get(annotations.size() - 1).getOffset(), 105072);
        Assert.assertEquals(annotations.get(annotations.size() - 1).getLength(), 5);

        // English
        tagger = new PalladianNer();
        tagger.setLanguageMode(LanguageMode.English);
        tagger.setTagUrls(false);
        tagger.setTagDates(false);
        tagger.train(NERTest.class.getResource("/ner/training.txt").getFile(),
                NERTest.class.getResource("/ner/tudnerEn.model").getFile());

        //        EvaluationResult er = tagger.evaluate(NERTest.class.getResource("/ner/training.txt").getFile(),
        //                NERTest.class.getResource("/ner/tudnerEn.model").getFile(), TaggingFormat.COLUMN);
        //        System.out.println(er.getMUCResultsReadable());
        //        System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel(NERTest.class.getResource("/ner/tudnerEn.model").getFile());
        tagger.setTagUrls(false);
        tagger.setTagDates(false);
        annotations = tagger.getAnnotations(FileFormatParser.getText(
                NERTest.class.getResource("/ner/test.txt")
                .getFile(), TaggingFormat.COLUMN));
        annotations.removeNestedAnnotations();
        annotations.sort();

                System.out.println(annotations.size());
                System.out.println(annotations.get(0));
                System.out.println(annotations.get(500));
                System.out.println(annotations.get(annotations.size() - 1));

        Assert.assertEquals(2241, annotations.size());
        Assert.assertEquals(annotations.get(0).getOffset(), 21);
        Assert.assertEquals(annotations.get(0).getLength(), 14);

        Assert.assertEquals(annotations.get(500).getOffset(), 15212);
        Assert.assertEquals(annotations.get(500).getLength(), 8);

        Assert.assertEquals(annotations.get(annotations.size() - 1).getOffset(), 105072);
        Assert.assertEquals(annotations.get(annotations.size() - 1).getLength(), 5);
    }

    @Test
    public void testStanfordNER() {
        StanfordNER tagger = new StanfordNER();

        tagger.train(NERTest.class.getResource("/ner/training.txt").getFile(),
                NERTest.class.getResource("/ner/stanfordner.ser.gz").getFile());

        // EvaluationResult er = tagger.evaluate(NERTest.class.getResource("/ner/test.txt").getFile(), NERTest.class
        // .getResource("/ner/stanfordner.ser.gz").getFile(),
        // TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel(NERTest.class.getResource("/ner/stanfordner.ser.gz").getFile());
        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText(
                NERTest.class.getResource("/ner/test.txt").getFile(), TaggingFormat.COLUMN));
        annotations.removeNestedAnnotations();
        annotations.sort();

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        Assert.assertEquals(2048, annotations.size());
        Assert.assertEquals(annotations.get(0).getOffset(), 21);
        Assert.assertEquals(annotations.get(0).getLength(), 14);

        Assert.assertEquals(annotations.get(500).getOffset(), 17692);
        Assert.assertEquals(annotations.get(500).getLength(), 4);

        Assert.assertEquals(annotations.get(annotations.size() - 1).getOffset(), 105072);
        Assert.assertEquals(annotations.get(annotations.size() - 1).getLength(), 5);
    }

    /**
     * For no apparent reason the Illinois NER test is non-deterministic.
     * To enable this test you must have a valid model at:
     * data\models\illinoisner\data\BrownHierarchicalWordClusters\brownBllipClusters
     */
    @Test
    @Ignore
    public void testIllinoisNER() {
        IllinoisLbjNER tagger = new IllinoisLbjNER();

        tagger.setTrainingRounds(2);
        tagger.train(NERTest.class.getResource("/ner/training.txt").getFile(),
                NERTest.class.getResource("/ner/lbj.model").getFile());

        // EvaluationResult er = tagger.evaluate(NERTest.class.getResource("/ner/test.txt").getFile(), NERTest.class
        // .getResource("/ner/lbj.model").getFile(), TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel(NERTest.class.getResource("/ner/lbj.model").getFile());
        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText(
                NERTest.class.getResource("/ner/test.txt").getFile(), TaggingFormat.COLUMN));
        annotations.removeNestedAnnotations();
        annotations.sort();

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        Assert.assertEquals(21.0, MathHelper.round(annotations.size() / 100, 0));
        Assert.assertEquals(annotations.get(0).getOffset(), 21);
        Assert.assertEquals(annotations.get(0).getLength(), 14);

        // Assert.assertEquals(annotations.get(500).getOffset(), 14506);
        // Assert.assertEquals(annotations.get(500).getLength(), 10);

        Assert.assertEquals(annotations.get(annotations.size() - 1).getOffset(), 105072);
        Assert.assertEquals(annotations.get(annotations.size() - 1).getLength(), 5);
    }

    @Test
    public void testLingPipeNER() {
        LingPipeNER tagger = new LingPipeNER();
        tagger.train(NERTest.class.getResource("/ner/training.txt").getFile(),
                NERTest.class.getResource("/ner/lingpipe.model").getFile());
        // EvaluationResult er = tagger.evaluate(NERTest.class.getResource("/ner/test.txt").getFile(), NERTest.class
        // .getResource("/ner/lingpipe.model").getFile(),
        // TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel(NERTest.class.getResource("/ner/lingpipe.model").getFile());
        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText(
                NERTest.class.getResource("/ner/test.txt").getFile(), TaggingFormat.COLUMN));
        annotations.removeNestedAnnotations();
        annotations.sort();

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        Assert.assertEquals(1906, annotations.size());
        Assert.assertEquals(annotations.get(0).getOffset(), 21);
        Assert.assertEquals(annotations.get(0).getLength(), 14);

        Assert.assertEquals(annotations.get(500).getOffset(), 17108);
        Assert.assertEquals(annotations.get(500).getLength(), 5);

        Assert.assertEquals(annotations.get(annotations.size() - 1).getOffset(), 105048);
        Assert.assertEquals(annotations.get(annotations.size() - 1).getLength(), 6);
    }

    @Test
    public void testOpenNLPNER() {
        OpenNLPNER tagger = new OpenNLPNER();

        tagger.train(NERTest.class.getResource("/ner/training.txt").getFile(),
                NERTest.class.getResource("/ner/openNLP.bin").getFile());

        // EvaluationResult er = tagger.evaluate(
        // NERTest.class.getResource("/ner/test.txt").getFile(),
        // NERTest.class.getResource("/ner/openNLP_PER.bin").getFile() + ","
        // + NERTest.class.getResource("/ner/openNLP_MISC.bin").getFile() + ","
        // + NERTest.class.getResource("/ner/openNLP_LOC.bin").getFile() + ","
        // + NERTest.class.getResource("/ner/openNLP_ORG.bin").getFile(), TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel(NERTest.class.getResource("/ner/").getFile());

        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText(
                NERTest.class.getResource("/ner/test.txt").getFile(), TaggingFormat.COLUMN));
        annotations.removeNestedAnnotations();
        annotations.sort();

        //         System.out.println(annotations.size());
        //         System.out.println(annotations.get(0));
        //         System.out.println(annotations.get(500));
        //         System.out.println(annotations.get(annotations.size() - 1));

        Assert.assertEquals(1988, annotations.size());
        Assert.assertEquals(annotations.get(0).getOffset(), 2);
        Assert.assertEquals(annotations.get(0).getLength(), 8);

        Assert.assertEquals(annotations.get(500).getOffset(), 16902);
        Assert.assertEquals(annotations.get(500).getLength(), 3);

        Assert.assertEquals(annotations.get(annotations.size() - 1).getOffset(), 105072);
        Assert.assertEquals(annotations.get(annotations.size() - 1).getLength(), 5);
    }

    /**
     * Different results when run locally in Eclipse and on Jenkins...ignore for now.
     */
    @Test
    @Ignore
    public void testJulieNER() {
        JulieNER tagger = new JulieNER();
        tagger.train(NERTest.class.getResource("/ner/training.txt").getFile(),
                NERTest.class.getResource("/ner/juliener.mod").getFile());

        // EvaluationResult er = tagger.evaluate(NERTest.class.getResource("/ner/test.txt").getFile(), NERTest.class
        // .getResource("/ner/juliener.mod").getFile(), TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel(NERTest.class.getResource("/ner/juliener.mod").getFile());
        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText(
                NERTest.class.getResource("/ner/test.txt").getFile(), TaggingFormat.COLUMN));
        annotations.removeNestedAnnotations();
        annotations.sort();

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        Assert.assertEquals(2035, annotations.size());
        Assert.assertEquals(annotations.get(0).getOffset(), 76);
        Assert.assertEquals(annotations.get(0).getLength(), 6);

        Assert.assertEquals(annotations.get(500).getOffset(), 17768);
        Assert.assertEquals(annotations.get(500).getLength(), 7);

        Assert.assertEquals(annotations.get(annotations.size() - 1).getOffset(), 105072);
        Assert.assertEquals(annotations.get(annotations.size() - 1).getLength(), 5);
    }

}