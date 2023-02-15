package ws.palladian.features;

import org.junit.Test;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.value.NumericValue;
import ws.palladian.features.SymmetryFeatureExtractor;
import ws.palladian.helper.io.ResourceHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static ws.palladian.features.color.Luminosity.LUMINOSITY;

public class SymmetryFeatureExtractorTest {
    private final SymmetryFeatureExtractor extractor = new SymmetryFeatureExtractor(LUMINOSITY);

    @Test
    public void testSymmetryFeatureExtractor_toyExamples() throws FileNotFoundException, IOException {
        BufferedImage image = ImageIO.read(ResourceHelper.getResourceFile("/symmetry-1.png"));
        FeatureVector features = extractor.extract(image);
        assertSymmetry(features, "symmetry-horizontal-luminosity");

        image = ImageIO.read(ResourceHelper.getResourceFile("/symmetry-2.png"));
        features = extractor.extract(image);
        assertSymmetry(features, "symmetry-vertical-luminosity");

        image = ImageIO.read(ResourceHelper.getResourceFile("/symmetry-3.png"));
        features = extractor.extract(image);
        assertSymmetry(features, "symmetry-both-luminosity");
        assertSymmetry(features, "symmetry-180-rotated-luminosity");

        image = ImageIO.read(ResourceHelper.getResourceFile("/symmetry-4.png"));
        features = extractor.extract(image);
        assertSymmetry(features, "symmetry-horizontal-luminosity");
        assertSymmetry(features, "symmetry-vertical-luminosity");
        assertSymmetry(features, "symmetry-both-luminosity");
        assertSymmetry(features, "symmetry-180-rotated-luminosity");
    }

    @Test
    public void testSymmetryFeatureExtractor() throws FileNotFoundException, IOException {
        BufferedImage image = ImageIO.read(ResourceHelper.getResourceFile("/symmetric.jpg"));
        FeatureVector features = extractor.extract(image);
        NumericValue verticalSimilarity = (NumericValue) features.get("symmetry-horizontal-luminosity");
        NumericValue horizontalSimilarity = (NumericValue) features.get("symmetry-vertical-luminosity");
        assertTrue(horizontalSimilarity.getDouble() > verticalSimilarity.getDouble());
    }

    private static void assertSymmetry(FeatureVector features, String featureName) {
        NumericValue similarity = (NumericValue) features.get(featureName);
        assertTrue("expected " + featureName + " to be almost 1", similarity.getDouble() > 0.99);
    }

}
