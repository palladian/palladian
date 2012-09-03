package ws.palladian.extraction.feature;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.ProcessingPipeline;

/**
 * @author Philipp Katz
 */
public class AnnotatorCorpusTest {

    // sample texts taken from http://lsirwww.epfl.ch/courses/dis/2006ws/exercises/IR/Exercise8.htm
    private static final PipelineDocument<String> doc1 = new PipelineDocument<String>(
            "If it walks like a duck and quacks like a duck, it must be a duck.");
    private static final PipelineDocument<String> doc2 = new PipelineDocument<String>(
            "Beijing Duck is mostly prized for the thin, crispy duck skin with authentic versions of the dish serving mostly the skin.");
    private static final PipelineDocument<String> doc3 = new PipelineDocument<String>(
            "Bugs' ascension to stardom also prompted the Warner animators to recast Daffy Duck as the rabbit's rival, intensely jealous and determined to steal back the spotlight while Bugs remained indifferent to the duck's jealousy, or used it to his advantage. This turned out to be the recipe for the success of the duo.");
    private static final PipelineDocument<String> doc4 = new PipelineDocument<String>(
            "6:25 PM 1/7/2007 blog entry: I found this great recipe for Rabbit Braised in Wine on cookingforengineers.com.");
    private static final PipelineDocument<String> doc5 = new PipelineDocument<String>(
            "Last week Li has shown you how to make the Sechuan duck. Today we'll be making Chinese dumplings (Jiaozi), a popular dish that I had a chance to try last summer in Beijing. There are many recipies for Jiaozi.");

    @Test
    public void testTfIdfAnnotator() throws DocumentUnprocessableException {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        TermCorpus termCorpus = new TermCorpus();

        pipeline.add(new LowerCaser());
        pipeline.add(new RegExTokenizer());
        pipeline.add(new TokenMetricsCalculator());
        pipeline.add(new TermCorpusBuilder(termCorpus));

        pipeline.process(doc1);
        pipeline.process(doc2);
        pipeline.process(doc3);
        pipeline.process(doc4);
        pipeline.process(doc5);

        assertEquals(5, termCorpus.getNumDocs());
        assertEquals(4, termCorpus.getCount("duck"));
        assertEquals(2, termCorpus.getCount("rabbit"));

        assertEquals(0, termCorpus.getIdf("duck"), 0);
        assertEquals(Math.log10(5. / 3), termCorpus.getIdf("rabbit"), 0);
        assertEquals(Math.log10(5. / 1), termCorpus.getIdf("giraffe"), 0);
    }
}
