package ws.palladian.extraction.multimedia;

import org.junit.Test;
import ws.palladian.helper.io.ResourceHelper;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Test cases for the image handler.
 *
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public class ImageHandlerTest {
    @Test
    public void testClusterImages() throws FileNotFoundException {
        Collection<String> imageUrls = new ArrayList<>();
        imageUrls.add(ResourceHelper.getResourcePath("/images/imageA1.jpg"));
        imageUrls.add(ResourceHelper.getResourcePath("/images/imageA2.jpg"));
        imageUrls.add(ResourceHelper.getResourcePath("/images/imageB1.jpg"));
        imageUrls.add(ResourceHelper.getResourcePath("/images/imageA3.jpg"));
        imageUrls.add(ResourceHelper.getResourcePath("/images/imageC1.jpg"));
        imageUrls.add(ResourceHelper.getResourcePath("/images/imageB2.jpg"));

        Set<String> representatives = ImageHandler.clusterImagesAndPickRepresentatives(imageUrls);

        // CollectionHelper.print(representatives);

        assertEquals(3, representatives.size());
        assertEquals(true, representatives.contains(ResourceHelper.getResourcePath("/images/imageA3.jpg")));
        assertEquals(true, representatives.contains(ResourceHelper.getResourcePath("/images/imageB2.jpg")));
        assertEquals(true, representatives.contains(ResourceHelper.getResourcePath("/images/imageC1.jpg")));

    }

    @Test
    public void testRescaleImage() throws FileNotFoundException {
        BufferedImage bufferedImage;

        bufferedImage = ImageHandler.load(ResourceHelper.getResourcePath("/images/imageB1.jpg"));
        bufferedImage = ImageHandler.boxFit(bufferedImage, 250);
        assertEquals(250, bufferedImage.getWidth());
        assertEquals(370, bufferedImage.getHeight());

        bufferedImage = ImageHandler.load(ResourceHelper.getResourcePath("/images/batman3.png"));
        bufferedImage = ImageHandler.boxCrop(bufferedImage, 200, 200);
        assertEquals(200, bufferedImage.getWidth());
        assertEquals(200, bufferedImage.getHeight());

        bufferedImage = ImageHandler.load(ResourceHelper.getResourcePath("/images/af1.jpg"));
        bufferedImage = ImageHandler.boxFit(bufferedImage, 200, 200);
        assertEquals(200, bufferedImage.getWidth());
        assertEquals(134, bufferedImage.getHeight());

        bufferedImage = ImageHandler.load(ResourceHelper.getResourcePath("/images/tdk1.jpg"));
        bufferedImage = ImageHandler.boxFit(bufferedImage, 200, 200);
        assertEquals(133, bufferedImage.getWidth());
        assertEquals(200, bufferedImage.getHeight());

        bufferedImage = ImageHandler.load(ResourceHelper.getResourcePath("/images/tdk1.jpg"));
        bufferedImage = ImageHandler.boxFit(bufferedImage, 100, 100);
        assertEquals(66, bufferedImage.getWidth());
        assertEquals(100, bufferedImage.getHeight());

        bufferedImage = ImageHandler.load(ResourceHelper.getResourcePath("/images/tdk5.jpg"));
        bufferedImage = ImageHandler.boxFit(bufferedImage, 200, 200);
        assertEquals(200, bufferedImage.getHeight());

        bufferedImage = ImageHandler.load(ResourceHelper.getResourcePath("/images/batman3.png"));
        bufferedImage = ImageHandler.boxCrop(bufferedImage, 189, 125);
        assertEquals(189, bufferedImage.getWidth());
        assertEquals(125, bufferedImage.getHeight());

        bufferedImage = ImageHandler.load(ResourceHelper.getResourcePath("/images/homer.gif"));
        bufferedImage = ImageHandler.boxCrop(bufferedImage, 300, 300);
        assertEquals(300, bufferedImage.getWidth());
        assertEquals(300, bufferedImage.getHeight());

        bufferedImage = ImageHandler.load(ResourceHelper.getResourcePath("/images/batman3.png"));
        bufferedImage = ImageHandler.boxCrop(bufferedImage, 1000, 1010);
        assertEquals(1000, bufferedImage.getWidth());
        assertEquals(1010, bufferedImage.getHeight());
    }

    @Test
    public void testIsDuplicate() throws FileNotFoundException {
        BufferedImage image1;
        BufferedImage image2;

        image1 = ImageHandler.load(ResourceHelper.getResourcePath("/images/jc1.jpg"));
        image2 = ImageHandler.load(ResourceHelper.getResourcePath("/images/jc2.jpg"));
        assertEquals(true, ImageHandler.isDuplicate(image1, image2));

        image1 = ImageHandler.load(ResourceHelper.getResourcePath("/images/tdk1.jpg"));
        image2 = ImageHandler.load(ResourceHelper.getResourcePath("/images/tdk2.jpg"));
        assertEquals(false, ImageHandler.isDuplicate(image1, image2));

        image1 = ImageHandler.load(ResourceHelper.getResourcePath("/images/tdk3.jpg"));
        image2 = ImageHandler.load(ResourceHelper.getResourcePath("/images/tdk4.jpg"));
        assertEquals(true, ImageHandler.isDuplicate(image1, image2));

        image1 = ImageHandler.load(ResourceHelper.getResourcePath("/images/tdk4.jpg"));
        image2 = ImageHandler.load(ResourceHelper.getResourcePath("/images/tdk5.jpg"));
        assertEquals(true, ImageHandler.isDuplicate(image1, image2));

        image1 = ImageHandler.load(ResourceHelper.getResourcePath("/images/af1.jpg"));
        image2 = ImageHandler.load(ResourceHelper.getResourcePath("/images/af2.jpg"));
        assertEquals(true, ImageHandler.isDuplicate(image1, image2));

        // image1 = ImageHandler.load("data/test/images/af2.jpg");
        // image2 = ImageHandler.load("data/test/images/af3.gif");
        // assertEquals(true, ImageHandler.isDuplicate(image1,image2));

        image1 = ImageHandler.load(ResourceHelper.getResourcePath("/images/af3.jpg"));
        image2 = ImageHandler.load(ResourceHelper.getResourcePath("/images/af4.jpg"));
        // TODO? gif colors are different (af3.gif compared with af4.jpg)
        assertEquals(true, ImageHandler.isDuplicate(image1, image2));

        image1 = ImageHandler.load(ResourceHelper.getResourcePath("/images/af1.jpg"));
        image2 = ImageHandler.load(ResourceHelper.getResourcePath("/images/gf1.jpg"));
        assertEquals(false, ImageHandler.isDuplicate(image1, image2));

        // TODO flags too similar
        // image1 = ImageHandler.load("data/test/images/af4.jpg");
        // image2 = ImageHandler.load("data/test/images/nzf1.jpg");
        // assertEquals(false, ImageHandler.isDuplicate(image1,image2));
    }

    @Test
    public void testDetectColors() throws FileNotFoundException {
        BufferedImage image = ImageHandler.load(ResourceHelper.getResourcePath("/images/af1.jpg"));
        List<Color> detectedColors = ImageHandler.detectColors(image);
        assertEquals(3, detectedColors.size());
        assertEquals(new Color("#273e7a", "Congress Blue", "Blue"), detectedColors.get(0));
        assertEquals(new Color("#cc1b36", "Crimson", "Red"), detectedColors.get(1));
        assertEquals(new Color("#eddfeb", "Carousel Pink", "Pink"), detectedColors.get(2));
    }
}