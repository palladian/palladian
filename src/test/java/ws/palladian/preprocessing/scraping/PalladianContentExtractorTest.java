package ws.palladian.preprocessing.scraping;

import junit.framework.Assert;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

public class PalladianContentExtractorTest {

    @Test
    public void testPalladianContentExtractor() throws PageContentExtractorException {
        PalladianContentExtractor extractor = new PalladianContentExtractor();
        
        String text = extractor.setDocument(getFile("/pageContentExtractor/test001.html")).getResultText();
        // System.out.println(DigestUtils.md5Hex(text));
        Assert.assertEquals("80eff9d14c83b529212bd64e78bc1fe4", DigestUtils.md5Hex(text));
        
    }

    private static String getFile(String path) {
        return PalladianContentExtractor.class.getResource(path).getFile();
    }

}
