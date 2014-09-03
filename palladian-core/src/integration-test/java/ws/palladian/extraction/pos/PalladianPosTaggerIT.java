package ws.palladian.extraction.pos;

import static org.junit.Assert.assertEquals;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.classification.universal.UniversalClassifierModel;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.integrationtests.ITHelper;

/**
 * <p>
 * Tests the Palladian POS Tagger.
 * </p>
 * 
 * @author David Urbansky
 */
public class PalladianPosTaggerIT {

    /** Path to the training data. */
    private static String trainDataSet;
    /** Path to the test data. */
    private static String testDataSet;

    @BeforeClass
    public static void readConfiguration() throws ConfigurationException {
        Configuration config = ITHelper.getTestConfig();
        trainDataSet = config.getString("dataset.brown.train");
        testDataSet = config.getString("dataset.brown.test");
        ITHelper.assertDirectory(trainDataSet, testDataSet);
    }

    @Test
    public void testTagging() {

        UniversalClassifierModel model = PalladianPosTagger.trainModel(trainDataSet);
        PalladianPosTagger ppt = new PalladianPosTagger(model);

        String taggedString;

        taggedString = ppt.getTaggedString("The quick brown fox jumps over the lazy dog.");
        // System.out.println(taggedString);
        assertEquals("The/AT quick/RB brown/JJ fox/NN jumps/NNS over/RP the/AT lazy/JJ dog/NN ./.", taggedString);

        taggedString = ppt.getTaggedString("I like my cake.");
        // System.out.println(taggedString);
        assertEquals("I/PPSS like/CS my/PP$ cake/NN ./.", taggedString);

        taggedString = ppt.getTaggedString("Your gun is the best friend you have.");
        // System.out.println(taggedString);
        assertEquals("Your/PP$ gun/NN is/BEZ the/AT best/JJT friend/NN you/PPSS have/HV ./.", taggedString);

        taggedString = ppt.getTaggedString("I'm here to say that we're about to do that.");
        // System.out.println(taggedString);
        assertEquals("I/PPSS '/' m/NN here/RN to/TO say/VB that/CS we/PPSS '/' re/QL about/RB to/TO do/DO that/CS ./.",
                taggedString);

        // XXX run evaluation on test data for integration tests
        // String taggedTextFilePath = FileHelper.readFileToString(DATASET_TEST_PATH + "ca01");
        // Annotations<ContextAnnotation> annotations = FileFormatParser.getAnnotations(taggedTextFilePath,
        // TaggingFormat.SLASHES);
        // CollectionHelper.print(annotations);
    }

    @Test
    public void testAccuracy() {
        UniversalClassifierModel model = PalladianPosTagger.trainModel(trainDataSet);
        PalladianPosTagger ppt = new PalladianPosTagger(model);
        ConfusionMatrix result = ppt.evaluate(testDataSet);
        ITHelper.assertMin("accuracy", 0.89, result.getAccuracy());
    }

}
