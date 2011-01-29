package tud.iir.extraction.entity.ner;

import junit.framework.Assert;

import org.junit.Test;

import tud.iir.extraction.entity.ner.evaluation.EvaluationResult;
import tud.iir.extraction.entity.ner.tagger.IllinoisLbjNER;
import tud.iir.extraction.entity.ner.tagger.LingPipeNER;
import tud.iir.extraction.entity.ner.tagger.OpenNLPNER;

public class NERTest {

    @Test
    public void testIllinoisNER() {
        IllinoisLbjNER tagger = new IllinoisLbjNER();
        // tagger.train("data/test/ner/training.txt", "data/temp/lbj.model");
        // EvaluationResult er = tagger.evaluate("data/test/ner/test.txt", "data/temp/lbj.model", TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel("data/temp/lbj.model");
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
        tagger.train("data/test/ner/training.txt", "data/temp/lingpipe.model");
        // EvaluationResult er = tagger.evaluate("data/test/ner/test.txt", "data/temp/lingpipe.model",
        // TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel("data/temp/lingpipe.model");
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
        tagger.train("data/test/ner/training.txt", "data/temp/openNLP_PER.bin");

        EvaluationResult er = tagger.evaluate("data/test/ner/test.txt", "data/temp/openNLP_PER.bin",
                TaggingFormat.COLUMN);
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel("data/temp/openNLP_PER.bin");
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