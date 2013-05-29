package ws.palladian.extraction.multimedia;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.io.ResourceHelper;

/**
 * Test cases for the image handler.
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public class ImageHandlerTest {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageHandlerTest.class);

    /**
     * Some of these tests fail when run under Mac OS X. In contrast to other platforms where the JAI implementation is
     * supplied directly by Sun, Apple provides its own version of JAI, which i suspect contains a bug or behaves
     * somehow differently. Therefor, we check the platform here and simply skip this test if run under Mac OS X.
     * 
     * Philipp, 2010-06-28.
     */
    @Before
    public void checkOperatingSystem() {
        boolean macOsX = System.getProperty("os.name").contains("Mac OS X");
        if (macOsX) {
            LOGGER.warn("skipping ImageHandlerTest due to bugs in Mac OS X.");
        }
        Assume.assumeTrue(!macOsX);
    }

    @Test
    public void testClusterImages() throws FileNotFoundException {

        Collection<String> imageUrls = new ArrayList<String>();
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
        BufferedImage bufferedImage = null;

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

    // public void testSaveImage() {
    // BufferedImage bufferedImage = null;
    //
    // bufferedImage = ImageHandler.load(ResourceHelper.getResourcePath("/images/tdk1.jpg"));
    // assertEquals(true, ImageHandler.saveImage(bufferedImage, "jpg",
    // ResourceHelper.getResourcePath("/images/generated0.jpg")));
    // assertEquals(true,
    // FileHelper.fileExists(ResourceHelper.getResourcePath("/images/generated0.jpg")));
    // assertEquals(true, FileHelper.delete(ResourceHelper.getResourcePath("/images/generated0.jpg")));
    //
    // bufferedImage = ImageHandler.load(ResourceHelper.getResourcePath("/images/tdk5.jpg"));
    // assertEquals(true, ImageHandler.saveImage(bufferedImage, "jpg",
    // ResourceHelper.getResourcePath("/images/generated1.jpg")));
    // assertEquals(true,
    // FileHelper.fileExists(ResourceHelper.getResourcePath("/images/generated1.jpg")));
    // assertEquals(true, FileHelper.delete(ResourceHelper.getResourcePath("/images/generated1.jpg")));
    //
    // bufferedImage = ImageHandler.load(ResourceHelper.getResourcePath("/images/batman3.png"));
    // assertEquals(true, ImageHandler.saveImage(bufferedImage, "png",
    // ResourceHelper.getResourcePath("/images/generated2.png")));
    // assertEquals(true,
    // FileHelper.fileExists(ResourceHelper.getResourcePath("/images/generated2.png")));
    // assertEquals(true, FileHelper.delete(ResourceHelper.getResourcePath("/images/generated2.png")));
    //
    // bufferedImage = ImageHandler.load(ResourceHelper.getResourcePath("/images/homer.gif"));
    // assertEquals(true, ImageHandler.saveImage(bufferedImage, "gif",
    // ResourceHelper.getResourcePath("/images/generated3.gif")));
    // assertEquals(true,
    // FileHelper.fileExists(ResourceHelper.getResourcePath("/images/generated3.gif")));
    // assertEquals(true, FileHelper.delete(ResourceHelper.getResourcePath("/images/generated3.gif")));
    // }

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
}