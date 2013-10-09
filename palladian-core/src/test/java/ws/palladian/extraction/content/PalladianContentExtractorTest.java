package ws.palladian.extraction.content;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.resources.WebImage;

public class PalladianContentExtractorTest {

    @Test
    @Ignore
    // FIXME
    public void testPalladianContentExtractor() throws PageContentExtractorException, FileNotFoundException {
        PalladianContentExtractor extractor = new PalladianContentExtractor();

        String text = extractor.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test001.html"))
                .getResultText();
        // System.out.println(DigestUtils.md5Hex(text));

        assertEquals("80eff9d14c83b529212bd64e78bc1fe4", DigestUtils.md5Hex(text));

    }

    @Test
    public void testImageExtraction() throws PageContentExtractorException, FileNotFoundException {
        PalladianContentExtractor extractor = new PalladianContentExtractor();

        extractor.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test015.html"));
        // extractor.setDocument("http://gizmodo.com/5823937/holy-shit-maul-semi+automatic-shotgun-shoots-taser-cartridges-and-is-called-maul");
        // System.out.println(extractor.getResultText());

        List<WebImage> images = extractor.getImages();
        assertEquals(2, images.size());
        assertEquals(0, images.get(0).getWidth());

        // TODO this should not access the web
        // extractor.analyzeImages();
        // Assert.assertEquals(640, images.get(0).getWidth());

        // => http://www.bbc.co.uk/news/science-environment-14254856
        extractor.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test020.html"));
        images = extractor.getImages();

        assertEquals(4, images.size());
        assertEquals(624, images.get(1).getWidth());

        // CollectionHelper.print(images);
    }

}
