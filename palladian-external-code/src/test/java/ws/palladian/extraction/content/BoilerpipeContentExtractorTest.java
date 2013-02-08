package ws.palladian.extraction.content;

import static org.junit.Assert.assertEquals;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import ws.palladian.helper.io.ResourceHelper;

public class BoilerpipeContentExtractorTest {

    @Test
    public void testBoilerpipeContentExtractor() throws Exception {
        BoilerpipeContentExtractor extractor = new BoilerpipeContentExtractor();
        extractor.setDocument(ResourceHelper.getResourceFile("/pageContentExtractor/test001.html"));
        assertEquals("5ff93a307fe92366326bcd1d801ab476", DigestUtils.md5Hex(extractor.getResultText()));
        assertEquals("e30074241dba8eb8258eab7be0ddea45", DigestUtils.md5Hex(extractor.getResultTitle()));
    }

}
