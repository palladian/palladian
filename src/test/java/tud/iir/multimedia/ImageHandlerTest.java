package tud.iir.multimedia;

import java.awt.image.BufferedImage;

import tud.iir.helper.FileHelper;
import tud.iir.multimedia.ImageHandler;

import junit.framework.TestCase;

/**
 * Test cases for the image handler.
 * 
 * @author David Urbansky
 */
public class ImageHandlerTest extends TestCase {

    public ImageHandlerTest(String name) {
        super(name);
    }

    public void testRescaleImage() {
        BufferedImage bufferedImage = null;

        bufferedImage = ImageHandler.load("data/test/images/tdk1.jpg");
        bufferedImage = ImageHandler.rescaleImage(bufferedImage, 200);
        assertEquals(200, bufferedImage.getWidth());

        bufferedImage = ImageHandler.load("data/test/images/tdk5.jpg");
        bufferedImage = ImageHandler.rescaleImage(bufferedImage, 200);
        assertEquals(200, bufferedImage.getWidth());

        bufferedImage = ImageHandler.load("data/test/images/batman3.png");
        bufferedImage = ImageHandler.rescaleImage(bufferedImage, 200);
        assertEquals(200, bufferedImage.getWidth());

        bufferedImage = ImageHandler.load("data/test/images/homer.gif");
        bufferedImage = ImageHandler.rescaleImage(bufferedImage, 200);
        assertEquals(200, bufferedImage.getWidth());
    }

    public void testSaveImage() {
        BufferedImage bufferedImage = null;

        bufferedImage = ImageHandler.load("data/test/images/tdk1.jpg");
        assertEquals(true, ImageHandler.saveImage(bufferedImage, "jpg", "data/test/images/generated0.jpg"));
        assertEquals(true, FileHelper.fileExists("data/test/images/generated0.jpg"));
        assertEquals(true, FileHelper.delete("data/test/images/generated0.jpg"));

        bufferedImage = ImageHandler.load("data/test/images/tdk5.jpg");
        assertEquals(true, ImageHandler.saveImage(bufferedImage, "jpg", "data/test/images/generated1.jpg"));
        assertEquals(true, FileHelper.fileExists("data/test/images/generated1.jpg"));
        assertEquals(true, FileHelper.delete("data/test/images/generated1.jpg"));

        bufferedImage = ImageHandler.load("data/test/images/batman3.png");
        assertEquals(true, ImageHandler.saveImage(bufferedImage, "png", "data/test/images/generated2.png"));
        assertEquals(true, FileHelper.fileExists("data/test/images/generated2.png"));
        assertEquals(true, FileHelper.delete("data/test/images/generated2.png"));

        bufferedImage = ImageHandler.load("data/test/images/homer.gif");
        assertEquals(true, ImageHandler.saveImage(bufferedImage, "gif", "data/test/images/generated3.gif"));
        assertEquals(true, FileHelper.fileExists("data/test/images/generated3.gif"));
        assertEquals(true, FileHelper.delete("data/test/images/generated3.gif"));
    }

    public void testIsDuplicate() {
        BufferedImage image1;
        BufferedImage image2;

        image1 = ImageHandler.load("data/test/images/jc1.jpg");
        image2 = ImageHandler.load("data/test/images/jc2.jpg");
        assertEquals(true, ImageHandler.isDuplicate(image1, image2));

        image1 = ImageHandler.load("data/test/images/tdk1.jpg");
        image2 = ImageHandler.load("data/test/images/tdk2.jpg");
        assertEquals(false, ImageHandler.isDuplicate(image1, image2));

        image1 = ImageHandler.load("data/test/images/tdk3.jpg");
        image2 = ImageHandler.load("data/test/images/tdk4.jpg");
        assertEquals(true, ImageHandler.isDuplicate(image1, image2));

        image1 = ImageHandler.load("data/test/images/tdk4.jpg");
        image2 = ImageHandler.load("data/test/images/tdk5.jpg");
        assertEquals(true, ImageHandler.isDuplicate(image1, image2));

        image1 = ImageHandler.load("data/test/images/af1.jpg");
        image2 = ImageHandler.load("data/test/images/af2.jpg");
        assertEquals(true, ImageHandler.isDuplicate(image1, image2));

        // image1 = ImageHandler.load("data/test/images/af2.jpg");
        // image2 = ImageHandler.load("data/test/images/af3.gif");
        // assertEquals(true, ImageHandler.isDuplicate(image1,image2));

        image1 = ImageHandler.load("data/test/images/af3.jpg");
        image2 = ImageHandler.load("data/test/images/af4.jpg");
        // TODO? gif colors are different (af3.gif compared with af4.jpg)
        assertEquals(true, ImageHandler.isDuplicate(image1, image2));

        image1 = ImageHandler.load("data/test/images/af1.jpg");
        image2 = ImageHandler.load("data/test/images/gf1.jpg");
        assertEquals(false, ImageHandler.isDuplicate(image1, image2));

        // TODO flags too similar
        // image1 = ImageHandler.load("data/test/images/af4.jpg");
        // image2 = ImageHandler.load("data/test/images/nzf1.jpg");
        // assertEquals(false, ImageHandler.isDuplicate(image1,image2));
    }
}