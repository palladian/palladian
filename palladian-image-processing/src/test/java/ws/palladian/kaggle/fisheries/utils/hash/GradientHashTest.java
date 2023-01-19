package ws.palladian.kaggle.fisheries.utils.hash;

import org.junit.Test;
import ws.palladian.extraction.multimedia.ImageHandler;
import ws.palladian.helper.io.ResourceHelper;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class GradientHashTest {
    @Test
    public void testGradientHash() throws FileNotFoundException, IOException {
        BufferedImage image = ImageHandler.load(ResourceHelper.getResourceFile("medium_5530248040.jpg"));
        String hash = new GradientHash().hash(image);
        assertEquals("f8f9f7d4e8e6d7c0", hash);
    }
}
