package ws.palladian.kaggle.fisheries.utils.hash;

import org.junit.Test;
import ws.palladian.extraction.multimedia.ImageHandler;
import ws.palladian.helper.io.ResourceHelper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;

public class BlockhashTest {

    /** Pattern for image extensions. */
    private static final String IMG_SUFFIX = "\\.(jpg|png)";

    @Test
    public void testBlockHash_bmvbhash_even() throws IOException {
        testWithMethod(1);
    }

    @Test
    public void testBlockHash_bmvbhash() throws IOException {
        testWithMethod(2);
    }

    private static void testWithMethod(int method) throws IOException {
        File testData = ResourceHelper.getResourceFile("blockhash-data");
        File[] images = testData.listFiles((dir, name) -> name.matches(".*" + IMG_SUFFIX));
        for (File imageFile : images) {
            BufferedImage image = ImageHandler.load(imageFile.getAbsolutePath());

            // e.g. image file: 00002701.jpg
            // hash: 00002701_16_1.txt and 00002701_16_2.txt
            File hashFile = new File(imageFile.getAbsolutePath().replaceAll(IMG_SUFFIX, "_16_" + method + ".txt"));
            String expectedHash = new String(Files.readAllBytes(hashFile.toPath())).split(" ")[1].trim();
            String calculatedHash = Blockhash.blockhashData(image, 16, method);

            int hammingDistance = HashUtil.hammingDistance(calculatedHash, expectedHash);
            // System.out.println(hammingDistance);
            assertTrue("Hamming distance from expected hash was " + hammingDistance, hammingDistance < 15);
        }
    }

}
