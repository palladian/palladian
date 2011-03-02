package ws.palladian.extraction.entity.ner;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.extraction.entity.ner.tagger.IllinoisLbjNER;
import ws.palladian.extraction.entity.ner.tagger.LingPipeNER;
import ws.palladian.extraction.entity.ner.tagger.OpenNLPNER;
import ws.palladian.extraction.entity.ner.tagger.StanfordNER;
import ws.palladian.extraction.entity.ner.tagger.TUDNER;

public class NERTest {

    @Test
    @Ignore
    public void testPalladianNER() {
        TUDNER tagger = new TUDNER();
        // tagger.train("data/test/ner/training.txt", "data/test/ner/tudner_.model");
        // EvaluationResult er = tagger
        // .evaluate("data/test/ner/test.txt", "data/test/ner/tudner_.model",
        // TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel(NERTest.class.getResource("/ner/tudner.model").getFile());
        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText(
                NERTest.class.getResource("/ner/test.txt").getFile(), TaggingFormat.COLUMN));
        annotations.removeNestedAnnotations();
        annotations.sort();

        // System.out.println(annotations.size());
        // System.out.println(annotations.get(0));
        // System.out.println(annotations.get(500));
        // System.out.println(annotations.get(annotations.size() - 1));

        Assert.assertEquals(2652, annotations.size());
        Assert.assertEquals(annotations.get(0).getOffset(), 12);
        Assert.assertEquals(annotations.get(0).getLength(), 62);

        Assert.assertEquals(annotations.get(500).getOffset(), 13931);
        Assert.assertEquals(annotations.get(500).getLength(), 11);

        Assert.assertEquals(annotations.get(annotations.size() - 1).getOffset(), 105072);
        Assert.assertEquals(annotations.get(annotations.size() - 1).getLength(), 5);
    }

    @Test
    @Ignore
    public void testStanfordNER() {
        StanfordNER tagger = new StanfordNER();
        // // tagger.train("data/test/ner/training.txt", "data/test/ner/stanfordner.ser.gz");
        // EvaluationResult er = tagger.evaluate("data/test/ner/test.txt", "data/test/ner/stanfordner.ser.gz",
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

    @Test
    @Ignore
    public void testIllinoisNER() {
        IllinoisLbjNER tagger = new IllinoisLbjNER();
        // tagger.train("data/test/ner/training.txt", "data/test/ner/lbj.model");
        // EvaluationResult er = tagger.evaluate("data/test/ner/test.txt", "data/temp/lbj.model", TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        tagger.loadModel(NERTest.class.getResource("/ner/lbj.model.level2").getFile());
        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText(
                NERTest.class.getResource("/ner/test.txt").getFile(), TaggingFormat.COLUMN));
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
    @Ignore
    public void testLingPipeNER() {
        LingPipeNER tagger = new LingPipeNER();
        tagger.train(NERTest.class.getResource("/ner/training.txt").getFile(),
                NERTest.class.getResource("/ner/lingpipe.model").getFile());
        // EvaluationResult er = tagger.evaluate("data/test/ner/test.txt", "data/test/ner/lingpipe.model",
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
    @Ignore
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

        tagger.loadModel(NERTest.class.getResource("/ner/openNLP_PER.bin").getFile() + ","
                + NERTest.class.getResource("/ner/openNLP_MISC.bin").getFile() + ","
                + NERTest.class.getResource("/ner/openNLP_LOC.bin").getFile() + ","
                + NERTest.class.getResource("/ner/openNLP_ORG.bin").getFile());
        // tagger.loadModel("data/temp/openNLP_LOC.bin,data/temp/openNLP_ORG.bin");
        Annotations annotations = tagger.getAnnotations(FileFormatParser.getText(
                NERTest.class.getResource("/ner/test.txt").getFile(), TaggingFormat.COLUMN));
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