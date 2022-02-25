package ws.palladian.extraction.multimedia;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class tests the similar image searcher
 *
 * @author David Urbansky
 * @since 21-Feb-22 at 11:30
 **/
public class SimilarImageSearcherTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @After
    public void tearDown() {
        FileHelper.delete("data/temp/images/similar-test/", true);
    }

    @Test
    public void testSimilarImageSearch() {
        // test building index from scratch
        SimilarImageSearcher similarImageSearcher = new SimilarImageSearcher(new File("data/temp/images/similar-test/"));

        List<String> imageUrls = new ArrayList<>();
        imageUrls.add("https://webknox.com/img/palladian-tests/teddy-1.jpg");
        imageUrls.add("https://webknox.com/img/palladian-tests/teddy-2.jpg");
        imageUrls.add("https://webknox.com/img/palladian-tests/camera-1.jpg");
        imageUrls.add("https://webknox.com/img/palladian-tests/camera-2.jpg");
        imageUrls.add("https://webknox.com/img/palladian-tests/high-heel-1.jpg");
        imageUrls.add("https://webknox.com/img/palladian-tests/high-heel-2.jpg");

        for (String imageUrl : imageUrls) {
            BufferedImage image = ImageHandler.load(imageUrl);
            String fileName = FileHelper.getFileName(imageUrl).replace(".jpg", "");
            similarImageSearcher.index(image, fileName);
        }
        test(similarImageSearcher);

        // test rebuilding index from folder
        similarImageSearcher = new SimilarImageSearcher(new File("data/temp/images/similar-test/"));
        test(similarImageSearcher);
    }

    private void test(SimilarImageSearcher sis) {
        BufferedImage teddyImage = ImageHandler.load("https://webknox.com/img/palladian-tests/teddy-3.jpg");
        List<String> identifiers = sis.search(teddyImage, 3);
        collector.checkThat(CollectionHelper.getFirst(identifiers), Matchers.containsString("teddy"));

        BufferedImage shoeImage = ImageHandler.load("https://webknox.com/img/palladian-tests/high-heel-3.jpg");
        identifiers = sis.search(shoeImage, 3);
        collector.checkThat(CollectionHelper.getFirst(identifiers), Matchers.containsString("high-heel"));

        BufferedImage cameraImage = ImageHandler.load("https://webknox.com/img/palladian-tests/camera-3.jpg");
        identifiers = sis.search(cameraImage, 3);
        collector.checkThat(CollectionHelper.getFirst(identifiers), Matchers.containsString("camera"));
    }
}