package ws.palladian.extraction.pos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.helper.ProcessHelper;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.ResourceHelper;

/**
 * <p>
 * Tests the Palladian POS Tagger.
 * </p>
 * 
 * @author David Urbansky
 */
public class PalladianPosTaggerTest {

    /** Path to the training data. */
    private static String trainDataSet;
    /** Path to the test data. */
    private static String testDataSet;

    @BeforeClass
    public static void readConfiguration() throws ConfigurationException {
        try {
            PropertiesConfiguration config = new PropertiesConfiguration(
                    ResourceHelper.getResourceFile("/palladian-test.properties"));
            trainDataSet = config.getString("dataset.brown.train");
            testDataSet = config.getString("dataset.brown.test");
            assumeDirectory(trainDataSet, testDataSet);
        } catch (FileNotFoundException e) {
            fail("palladian-test.properties not found; test is skipped!");
        }
        if (ProcessHelper.getFreeMemory() < SizeUnit.MEGABYTES.toBytes(1792)) {
            fail("Not enough memory. This test requires at least 2 GB heap memory.");
        }
    }

    /**
     * Make sure, all given paths are pointing to directories.
     * 
     * @param paths
     */
    private static void assumeDirectory(String... paths) {
        for (String path : paths) {
            assumeTrue(path + " not present", new File(path).isDirectory());
        }
    }

    @Test
    public void test() throws IOException {

        PalladianPosTagger ppt = new PalladianPosTagger();
        ppt.trainModel(trainDataSet, "palladianEnPos.gz");

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
