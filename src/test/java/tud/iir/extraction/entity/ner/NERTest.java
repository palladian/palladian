package tud.iir.extraction.entity.ner;

import junit.framework.Assert;

import org.junit.Test;

import tud.iir.extraction.entity.ner.tagger.IllinoisLbjNER;
import tud.iir.extraction.entity.ner.tagger.LingPipeNER;
import tud.iir.extraction.entity.ner.tagger.OpenNLPNER;
import tud.iir.extraction.entity.ner.tagger.StanfordNER;
import tud.iir.extraction.entity.ner.tagger.TUDNER;

public class NERTest {

    @Test
    public void testPalladianNER() {
        TUDNER tagger = new TUDNER();
        // tagger.train("data/test/ner/training.txt", "data/test/ner/tudner.model");
        // EvaluationResult er = tagger.evaluate("data/test/ner/test.txt", "data/temp/tudner.model",
        // TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel("data/test/ner/tudner.model");
        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText("data/test/ner/test.txt",
                TaggingFormat.COLUMN));
        annotations.removeNestedAnnotations();
        annotations.sort();

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        Assert.assertEquals(2888, annotations.size());
        Assert.assertEquals(annotations.get(0).getOffset(), 2);
        Assert.assertEquals(annotations.get(0).getLength(), 72);

        Assert.assertEquals(annotations.get(500).getOffset(), 12885);
        Assert.assertEquals(annotations.get(500).getLength(), 3);

        Assert.assertEquals(annotations.get(annotations.size() - 1).getOffset(), 105072);
        Assert.assertEquals(annotations.get(annotations.size() - 1).getLength(), 5);
    }

    @Test
    public void testStanfordNER() {
        StanfordNER tagger = new StanfordNER();
        // // tagger.train("data/test/ner/training.txt", "data/test/ner/stanfordner.ser.gz");
        // EvaluationResult er = tagger.evaluate("data/test/ner/test.txt", "data/test/ner/stanfordner.ser.gz",
        // TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel("data/test/ner/stanfordner.ser.gz");
        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText("data/test/ner/test.txt",
                TaggingFormat.COLUMN));
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

    @Test
    public void testIllinoisNER() {
        IllinoisLbjNER tagger = new IllinoisLbjNER();
        // tagger.train("data/test/ner/training.txt", "data/test/ner/lbj.model");
        // EvaluationResult er = tagger.evaluate("data/test/ner/test.txt", "data/temp/lbj.model", TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel("data/test/ner/lbj.model");
        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText("data/test/ner/test.txt",
                TaggingFormat.COLUMN));
        annotations.removeNestedAnnotations();
        annotations.sort();

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        Assert.assertEquals(1853, annotations.size());
        Assert.assertEquals(annotations.get(0).getOffset(), 12);
        Assert.assertEquals(annotations.get(0).getLength(), 23);

        Assert.assertEquals(annotations.get(500).getOffset(), 19833);
        Assert.assertEquals(annotations.get(500).getLength(), 3);

        Assert.assertEquals(annotations.get(annotations.size() - 1).getOffset(), 105072);
        Assert.assertEquals(annotations.get(annotations.size() - 1).getLength(), 5);
    }

    @Test
    public void testLingPipeNER() {
        LingPipeNER tagger = new LingPipeNER();
        tagger.train("data/test/ner/training.txt", "data/test/ner/lingpipe.model");
        // EvaluationResult er = tagger.evaluate("data/test/ner/test.txt", "data/test/ner/lingpipe.model",
        // TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel("data/test/ner/lingpipe.model");
        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText("data/test/ner/test.txt",
                TaggingFormat.COLUMN));
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
        // tagger.train("data/test/ner/training.txt", "data/test/openNLP.bin");
        // EvaluationResult er = tagger
        // .evaluate(
        // "data/test/ner/test.txt",
        // "data/temp/openNLP_PER.bin,data/temp/openNLP_MISC.bin,data/temp/openNLP_LOC.bin,data/temp/openNLP_ORG.bin",
        // TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel("data/test/ner/openNLP_PER.bin,data/test/ner/openNLP_MISC.bin,data/test/ner/openNLP_LOC.bin,data/test/ner/openNLP_ORG.bin");
        // tagger.loadModel("data/temp/openNLP_LOC.bin,data/temp/openNLP_ORG.bin");
        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText("data/test/ner/test.txt",
                TaggingFormat.COLUMN));
        annotations.removeNestedAnnotations();
        annotations.sort();

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        Assert.assertEquals(2313, annotations.size());
        Assert.assertEquals(annotations.get(0).getOffset(), 2);
        Assert.assertEquals(annotations.get(0).getLength(), 8);

        Assert.assertEquals(annotations.get(500).getOffset(), 14547);
        Assert.assertEquals(annotations.get(500).getLength(), 3);

        Assert.assertEquals(annotations.get(annotations.size() - 1).getOffset(), 105072);
        Assert.assertEquals(annotations.get(annotations.size() - 1).getLength(), 5);
    }

    /*
     * @Test
     * public void testJulieNER() {
     * JulieNER tagger = new JulieNER();
     * // tagger.train("data/test/ner/training.txt", "data/temp/juliener.mod");
     * EvaluationResult er = tagger.evaluate("data/test/ner/test.txt", "data/temp/juliener.mod",
     * TaggingFormat.COLUMN);
     * System.out.println(er.getMUCResultsReadable());
     * System.out.println(er.getExactMatchResultsReadable());
     * tagger.loadModel("data/temp/juliener.mod");
     * Annotations annotations = tagger.getAnnotations(FileFormatParser.getText("data/test/ner/test.txt",
     * TaggingFormat.COLUMN));
     * annotations.removeNestedAnnotations();
     * annotations.sort();
     * System.out.println(annotations.size());
     * System.out.println(annotations.get(0));
     * System.out.println(annotations.get(500));
     * System.out.println(annotations.get(annotations.size() - 1));
     * Assert.assertEquals(1853, annotations.size());
     * Assert.assertEquals(annotations.get(0).getOffset(), 12);
     * Assert.assertEquals(annotations.get(0).getLength(), 23);
     * Assert.assertEquals(annotations.get(500).getOffset(), 19833);
     * Assert.assertEquals(annotations.get(500).getLength(), 3);
     * Assert.assertEquals(annotations.get(annotations.size() - 1).getOffset(), 105072);
     * Assert.assertEquals(annotations.get(annotations.size() - 1).getLength(), 5);
     * }
     */

}