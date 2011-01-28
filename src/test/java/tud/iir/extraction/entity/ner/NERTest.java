package tud.iir.extraction.entity.ner;

import junit.framework.Assert;

import org.junit.Test;

import tud.iir.extraction.entity.ner.evaluation.EvaluationResult;
import tud.iir.extraction.entity.ner.tagger.IllinoisLbjNER;
import tud.iir.extraction.entity.ner.tagger.JulieNER;
import tud.iir.extraction.entity.ner.tagger.LingPipeNER;

public class NERTest {

    @Test
    public void testIllinoisNER() {
        IllinoisLbjNER tagger = new IllinoisLbjNER();
        tagger.train("data/test/ner/training.txt", "data/temp/lbj.model");
        // EvaluationResult er = tagger.evaluate("data/test/ner/test.txt", "data/temp/lbj.model",
        // TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        Annotations annotations = tagger.getAnnotations("data/test/ner/test.txt", "data/temp/lbj.model");
        System.out.println(annotations.get(234));

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

        Assert.assertEquals(1637, annotations.size());
        Assert.assertEquals(annotations.get(0).getOffset(), 21);
        Assert.assertEquals(annotations.get(0).getLength(), 14);

        Assert.assertEquals(annotations.get(500).getOffset(), 17433);
        Assert.assertEquals(annotations.get(500).getLength(), 10);

        Assert.assertEquals(annotations.get(annotations.size() - 1).getOffset(), 76158);
        Assert.assertEquals(annotations.get(annotations.size() - 1).getLength(), 12);
    }

    @Test
    public void testJulieNER() {
        JulieNER tagger = new JulieNER();
        tagger.train("data/test/ner/training.txt", "data/temp/lbj.model");
        EvaluationResult er = tagger.evaluate("data/test/ner/test.txt", "data/temp/lbj.model",
                TaggingFormat.COLUMN);
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());
    }

}
