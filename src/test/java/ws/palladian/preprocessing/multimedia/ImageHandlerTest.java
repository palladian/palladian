package ws.palladian.preprocessing.multimedia;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the image handler.
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public class ImageHandlerTest {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ImageHandlerTest.class);

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
    public void testClusterImages() {

        Collection<String> imageUrls = new ArrayList<String>();
        imageUrls.add(ImageHandlerTest.class.getResource("/images/imageA1.jpg").getFile());
        imageUrls.add(ImageHandlerTest.class.getResource("/images/imageA2.jpg").getFile());
        imageUrls.add(ImageHandlerTest.class.getResource("/images/imageB1.jpg").getFile());
        imageUrls.add(ImageHandlerTest.class.getResource("/images/imageA3.jpg").getFile());
        imageUrls.add(ImageHandlerTest.class.getResource("/images/imageC1.jpg").getFile());
        imageUrls.add(ImageHandlerTest.class.getResource("/images/imageB2.jpg").getFile());

        Set<String> representatives = ImageHandler.clusterImagesAndPickRepresentatives(imageUrls);

        // CollectionHelper.print(representatives);

        assertEquals(3, representatives.size());
        assertEquals(true,
                representatives.contains(ImageHandlerTest.class.getResource("/images/imageA3.jpg").getFile()));
        assertEquals(true,
                representatives.contains(ImageHandlerTest.class.getResource("/images/imageB2.jpg").getFile()));
        assertEquals(true,
                representatives.contains(ImageHandlerTest.class.getResource("/images/imageC1.jpg").getFile()));

    }

    @Test
    public void testRescaleImage() {
        BufferedImage bufferedImage = null;

        bufferedImage = ImageHandler.load(ImageHandlerTest.class.getResource("/images/tdk1.jpg").getFile());
        bufferedImage = ImageHandler.rescaleImage(bufferedImage, 200);
        assertEquals(200, bufferedImage.getWidth());

        bufferedImage = ImageHandler.load(ImageHandlerTest.class.getResource("/images/tdk5.jpg").getFile());
        bufferedImage = ImageHandler.rescaleImage(bufferedImage, 200);
        assertEquals(200, bufferedImage.getWidth());

        bufferedImage = ImageHandler.load(ImageHandlerTest.class.getResource("/images/batman3.png").getFile());
        bufferedImage = ImageHandler.rescaleImage(bufferedImage, 200);
        assertEquals(200, bufferedImage.getWidth());

        bufferedImage = ImageHandler.load(ImageHandlerTest.class.getResource("/images/homer.gif").getFile());
        bufferedImage = ImageHandler.rescaleImage(bufferedImage, 200);
        assertEquals(200, bufferedImage.getWidth());
    }

    // public void testSaveImage() {
    // BufferedImage bufferedImage = null;
    //
    // bufferedImage = ImageHandler.load(ImageHandlerTest.class.getResource("/images/tdk1.jpg").getFile());
    // assertEquals(true, ImageHandler.saveImage(bufferedImage, "jpg",
    // ImageHandlerTest.class.getResource("/images/generated0.jpg").getFile()));
    // assertEquals(true,
    // FileHelper.fileExists(ImageHandlerTest.class.getResource("/images/generated0.jpg").getFile()));
    // assertEquals(true, FileHelper.delete(ImageHandlerTest.class.getResource("/images/generated0.jpg").getFile()));
    //
    // bufferedImage = ImageHandler.load(ImageHandlerTest.class.getResource("/images/tdk5.jpg").getFile());
    // assertEquals(true, ImageHandler.saveImage(bufferedImage, "jpg",
    // ImageHandlerTest.class.getResource("/images/generated1.jpg").getFile()));
    // assertEquals(true,
    // FileHelper.fileExists(ImageHandlerTest.class.getResource("/images/generated1.jpg").getFile()));
    // assertEquals(true, FileHelper.delete(ImageHandlerTest.class.getResource("/images/generated1.jpg").getFile()));
    //
    // bufferedImage = ImageHandler.load(ImageHandlerTest.class.getResource("/images/batman3.png").getFile());
    // assertEquals(true, ImageHandler.saveImage(bufferedImage, "png",
    // ImageHandlerTest.class.getResource("/images/generated2.png").getFile()));
    // assertEquals(true,
    // FileHelper.fileExists(ImageHandlerTest.class.getResource("/images/generated2.png").getFile()));
    // assertEquals(true, FileHelper.delete(ImageHandlerTest.class.getResource("/images/generated2.png").getFile()));
    //
    // bufferedImage = ImageHandler.load(ImageHandlerTest.class.getResource("/images/homer.gif").getFile());
    // assertEquals(true, ImageHandler.saveImage(bufferedImage, "gif",
    // ImageHandlerTest.class.getResource("/images/generated3.gif").getFile()));
    // assertEquals(true,
    // FileHelper.fileExists(ImageHandlerTest.class.getResource("/images/generated3.gif").getFile()));
    // assertEquals(true, FileHelper.delete(ImageHandlerTest.class.getResource("/images/generated3.gif").getFile()));
    // }

    @Test
    public void testIsDuplicate() {
        BufferedImage image1;
        BufferedImage image2;

        image1 = ImageHandler.load(ImageHandlerTest.class.getResource("/images/jc1.jpg").getFile());
        image2 = ImageHandler.load(ImageHandlerTest.class.getResource("/images/jc2.jpg").getFile());
        assertEquals(true, ImageHandler.isDuplicate(image1, image2));

        image1 = ImageHandler.load(ImageHandlerTest.class.getResource("/images/tdk1.jpg").getFile());
        image2 = ImageHandler.load(ImageHandlerTest.class.getResource("/images/tdk2.jpg").getFile());
        assertEquals(false, ImageHandler.isDuplicate(image1, image2));

        image1 = ImageHandler.load(ImageHandlerTest.class.getResource("/images/tdk3.jpg").getFile());
        image2 = ImageHandler.load(ImageHandlerTest.class.getResource("/images/tdk4.jpg").getFile());
        assertEquals(true, ImageHandler.isDuplicate(image1, image2));

        image1 = ImageHandler.load(ImageHandlerTest.class.getResource("/images/tdk4.jpg").getFile());
        image2 = ImageHandler.load(ImageHandlerTest.class.getResource("/images/tdk5.jpg").getFile());
        assertEquals(true, ImageHandler.isDuplicate(image1, image2));

        image1 = ImageHandler.load(ImageHandlerTest.class.getResource("/images/af1.jpg").getFile());
        image2 = ImageHandler.load(ImageHandlerTest.class.getResource("/images/af2.jpg").getFile());
        assertEquals(true, ImageHandler.isDuplicate(image1, image2));

        // image1 = ImageHandler.load("data/test/images/af2.jpg");
        // image2 = ImageHandler.load("data/test/images/af3.gif");
        // assertEquals(true, ImageHandler.isDuplicate(image1,image2));

        image1 = ImageHandler.load(ImageHandlerTest.class.getResource("/images/af3.jpg").getFile());
        image2 = ImageHandler.load(ImageHandlerTest.class.getResource("/images/af4.jpg").getFile());
        // TODO? gif colors are different (af3.gif compared with af4.jpg)
        assertEquals(true, ImageHandler.isDuplicate(image1, image2));

        image1 = ImageHandler.load(ImageHandlerTest.class.getResource("/images/af1.jpg").getFile());
        image2 = ImageHandler.load(ImageHandlerTest.class.getResource("/images/gf1.jpg").getFile());
        assertEquals(false, ImageHandler.isDuplicate(image1, image2));

        // TODO flags too similar
        // image1 = ImageHandler.load("data/test/images/af4.jpg");
        // image2 = ImageHandler.load("data/test/images/nzf1.jpg");
        // assertEquals(false, ImageHandler.isDuplicate(image1,image2));
    }
}