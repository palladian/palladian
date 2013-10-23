package ws.palladian.extraction.feature;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;

/**
 * @author Philipp Katz
 */
public class TermCorpusBuilderTest {

    // sample texts taken from http://lsirwww.epfl.ch/courses/dis/2006ws/exercises/IR/Exercise8.htm
    private static final TextDocument doc1 = new TextDocument(
            "If it walks like a duck and quacks like a duck, it must be a duck.");
    private static final TextDocument doc2 = new TextDocument(
            "Beijing Duck is mostly prized for the thin, crispy duck skin with authentic versions of the dish serving mostly the skin.");
    private static final TextDocument doc3 = new TextDocument(
            "Bugs' ascension to stardom also prompted the Warner animators to recast Daffy Duck as the rabbit's rival, intensely jealous and determined to steal back the spotlight while Bugs remained indifferent to the duck's jealousy, or used it to his advantage. This turned out to be the recipe for the success of the duo.");
    private static final TextDocument doc4 = new TextDocument(
            "6:25 PM 1/7/2007 blog entry: I found this great recipe for Rabbit Braised in Wine on cookingforengineers.com.");
    private static final TextDocument doc5 = new TextDocument(
            "Last week Li has shown you how to make the Sechuan duck. Today we'll be making Chinese dumplings (Jiaozi), a popular dish that I had a chance to try last summer in Beijing. There are many recipies for Jiaozi.");

    @Test
    public void testTermCorpusBuilder() throws DocumentUnprocessableException {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        MapTermCorpus termCorpus = new MapTermCorpus();

        pipeline.connectToPreviousProcessor(new LowerCaser());
        pipeline.connectToPreviousProcessor(new RegExTokenizer());
        pipeline.connectToPreviousProcessor(new TermCorpusBuilder(termCorpus));

        pipeline.process(doc1);
        pipeline.process(doc2);
        pipeline.process(doc3);
        pipeline.process(doc4);
        pipeline.process(doc5);

        assertEquals(5, termCorpus.getNumDocs());
        assertEquals(4, termCorpus.getCount("duck"));
        assertEquals(2, termCorpus.getCount("rabbit"));

        assertEquals(6. / 5, termCorpus.getIdf("duck", true), 0);
        assertEquals(6. / 3, termCorpus.getIdf("rabbit", true), 0);
        assertEquals(6. / 1, termCorpus.getIdf("giraffe", true), 0);
    }
}
