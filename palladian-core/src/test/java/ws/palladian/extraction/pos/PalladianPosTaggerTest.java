package ws.palladian.extraction.pos;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

/**
 * <p>
 * Tests the Palladian POS Tagger.
 * </p>
 * 
 * @author David Urbansky
 */
public class PalladianPosTaggerTest {

    private static final String DATASET_TRAINING_PATH = "H:\\PalladianData\\Datasets\\Brown Corpus\\prepared\\train\\";// "PUT YOUR DATASET PATH IN HERE";
    private static final String DATASET_TEST_PATH = "H:\\PalladianData\\Datasets\\Brown Corpus\\prepared\\test\\";// "PUT YOUR DATASET PATH IN HERE";

    @Ignore
    @Test
    public void test() throws IOException {

        PalladianPosTagger ppt = new PalladianPosTagger();
        ppt.trainModel(DATASET_TRAINING_PATH, "palladianEnPos.gz");

        String taggedString = "";

        taggedString = ppt.getTaggedString("The quick brown fox jumps over the lazy dog.");
        System.out.println(taggedString);
        assertEquals("The/AT quick/RB brown/JJ fox/NN jumps/NNS over/RP the/AT lazy/JJ dog/NN ./.", taggedString);

        taggedString = ppt.getTaggedString("I like my cake.");
        System.out.println(taggedString);
        assertEquals("I/PPSS like/CS my/PP$ cake/NN ./.", taggedString);

        taggedString = ppt.getTaggedString("Your gun is the best friend you have.");
        System.out.println(taggedString);
        assertEquals("Your/PP$ gun/NN is/BEZ the/AT best/JJT friend/NN you/PPSS have/HV ./.", taggedString);

        taggedString = ppt.getTaggedString("I'm here to say that we're about to do that.");
        System.out.println(taggedString);
        assertEquals("I/PPSS '/' m/NN here/RN to/TO say/VB that/CS we/PPSS '/' re/QL about/RB to/TO do/DO that/CS ./.",
                taggedString);


        // XXX run evaluation on test data for integration tests
        // String taggedTextFilePath = FileHelper.readFileToString(DATASET_TEST_PATH + "ca01");
        // Annotations<ContextAnnotation> annotations = FileFormatParser.getAnnotations(taggedTextFilePath,
        // TaggingFormat.SLASHES);
        // CollectionHelper.print(annotations);
    }

}
