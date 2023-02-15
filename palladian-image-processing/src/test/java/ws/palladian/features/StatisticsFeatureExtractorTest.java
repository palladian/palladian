package ws.palladian.features;

import org.junit.Test;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.value.NumericValue;
import ws.palladian.features.StatisticsFeatureExtractor;
import ws.palladian.features.color.HSB;
import ws.palladian.helper.io.ResourceHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.junit.Assert.assertFalse;

public class StatisticsFeatureExtractorTest {
    @Test
    public void testStatisticsFeatureExtractor() throws IOException {
        BufferedImage image = ImageIO.read(ResourceHelper.getResourceFile("/51612.jpg"));
        FeatureVector features = new StatisticsFeatureExtractor(HSB.values()).extract(image);
        features.forEach(v -> {
            NumericValue n = (NumericValue) v.value();
            assertFalse(v.key() + " was NaN", Double.isNaN(n.getDouble()));
        });
    }
}
