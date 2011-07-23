package ws.palladian.preprocessing.scraping;

import java.util.List;

import junit.framework.Assert;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import ws.palladian.retrieval.resources.WebImage;

public class PalladianContentExtractorTest {

    @Test
    public void testPalladianContentExtractor() throws PageContentExtractorException {
        PalladianContentExtractor extractor = new PalladianContentExtractor();
        
        String text = extractor.setDocument(getFile("/pageContentExtractor/test001.html")).getResultText();
        // System.out.println(DigestUtils.md5Hex(text));
        Assert.assertEquals("80eff9d14c83b529212bd64e78bc1fe4", DigestUtils.md5Hex(text));
        
    }

    @Test
    public void testImageExtraction() throws PageContentExtractorException {
        PalladianContentExtractor extractor = new PalladianContentExtractor();

        extractor.setDocument(getFile("/pageContentExtractor/test015.html"));
        // extractor.setDocument("http://gizmodo.com/5823937/holy-shit-maul-semi+automatic-shotgun-shoots-taser-cartridges-and-is-called-maul");
        System.out.println(extractor.getResultText());

        List<WebImage> images = extractor.getImages();
        Assert.assertEquals(2, images.size());
        Assert.assertEquals(0, images.get(0).getWidth());

        extractor.analyzeImages();
        Assert.assertEquals(640, images.get(0).getWidth());

        // => http://www.bbc.co.uk/news/science-environment-14254856
        extractor.setDocument(getFile("/pageContentExtractor/test020.html"));
        images = extractor.getImages();

        Assert.assertEquals(4, images.size());
        Assert.assertEquals(624, images.get(1).getWidth());

        // CollectionHelper.print(images);
    }

    private static String getFile(String path) {
        return PalladianContentExtractor.class.getResource(path).getFile();
    }

}
