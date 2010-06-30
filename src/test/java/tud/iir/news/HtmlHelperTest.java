package tud.iir.news;

import junit.framework.Assert;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.w3c.dom.Document;

import tud.iir.web.Crawler;

public class HtmlHelperTest {
    
    Crawler c = new Crawler();

    /*
    @Test
    public void testHtmlDocToStringHashResult() {
        Document doc = c.getWebDocument("data/test/pageContentExtractor/test001.html");
        String result = HtmlHelper.htmlDocToString(doc);
        System.out.println(DigestUtils.md5Hex(result));
    }*/
    
    @Test
    public void testHtmlDocToString() {
        Document doc = c.getWebDocument("data/test/pageContentExtractor/test001.html");
        String result = HtmlHelper.htmlDocToString(doc);
        Assert.assertEquals("489eb91cf94343d0b62e69c396bc6b6f", DigestUtils.md5Hex(result));
    }

}
