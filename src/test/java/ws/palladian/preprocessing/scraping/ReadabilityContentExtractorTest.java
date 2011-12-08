package ws.palladian.preprocessing.scraping;

import java.io.FileNotFoundException;

import junit.framework.Assert;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import ws.palladian.helper.ResourceHelper;

/**
 * <p>
 * Test for {@link ReadabilityContentExtractor}. The tests are done via hashes; in case those test should fail becase of
 * minor changes to the code, there are original sample files of the desired results, which can be found in the test
 * resources, under <tt>pageContentExtractor/result/...</tt>
 * </p>
 * 
 * @author Philipp Katz
 */
public class ReadabilityContentExtractorTest {

    @Test
    public void testPageContentExtractorWithHashes() throws PageContentExtractorException, FileNotFoundException {
        ReadabilityContentExtractor e = new ReadabilityContentExtractor();
        Assert.assertEquals("test001.html", "a078a28fd8d1a59d9364b53c4818539b", DigestUtils.md5Hex(e.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test001.html")).getResultText()));
        Assert.assertEquals("test002.html", "0211a72e522229abdb6b4019279ea128", DigestUtils.md5Hex(e.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test002.html")).getResultText()));
        Assert.assertEquals("test003.html", "2865c50e6223a110e0de79c76a45e5fa", DigestUtils.md5Hex(e.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test003.html")).getResultText()));
        Assert.assertEquals("test004.html", "39a5fb4526d7ab362b21384a86902665", DigestUtils.md5Hex(e.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test004.html")).getResultText()));
        Assert.assertEquals("test005.html", "12eb1af518752d13d9af10bb7b4da3f9", DigestUtils.md5Hex(e.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test005.html")).getResultText()));
        Assert.assertEquals("test006.html", "797e0ebf8d89e9c93762a888f0b4bd64", DigestUtils.md5Hex(e.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test006.html")).getResultText()));
        Assert.assertEquals("test007.html", "6c2ff6c964a3b57ed1e483014b477353", DigestUtils.md5Hex(e.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test007.html")).getResultText()));
        Assert.assertEquals("test008.html", "d0d155ebd785848a27509257e8fe2726", DigestUtils.md5Hex(e.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test008.html")).getResultText()));
        Assert.assertEquals("test009.html", "11b3f8cbd9bc774588da6327f280a1ab", DigestUtils.md5Hex(e.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test009.html")).getResultText()));
        Assert.assertEquals("test010.html", "829687fa8a170b3971db50f76d41a8ca", DigestUtils.md5Hex(e.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test010.html")).getResultText()));
        Assert.assertEquals("test011.html", "b17d23137b7693a92e02646c0e83dd12", DigestUtils.md5Hex(e.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test011.html")).getResultText()));
        Assert.assertEquals("test012.html", "3f8eb27a5bc33d2d71f1314595e7594b", DigestUtils.md5Hex(e.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test012.html")).getResultText()));
        Assert.assertEquals("test013.html", "af25ba7317e8df1aca75fc1956372d54", DigestUtils.md5Hex(e.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test013.html")).getResultText()));
        Assert.assertEquals("test014.html", "afeabbbfb8c3f1c3943a05ba772da59f", DigestUtils.md5Hex(e.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test014.html")).getResultText()));
        Assert.assertEquals("test015.html", "1788005daaedfcb997c64802f5c42a46", DigestUtils.md5Hex(e.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test015.html")).getResultText()));
        Assert.assertEquals("test018.html", "70bf83d80e0757a8f99fe4331a5244a6", DigestUtils.md5Hex(e.setDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test018.html")).getResultText()));
        Assert.assertEquals("website100.html", "724bf03db67176bf761727343bfa0cff", DigestUtils.md5Hex(e.setDocument(ResourceHelper.getResourcePath("/webPages/website100.html")).getResultText()));
        Assert.assertEquals("website101.html", "7dd8bce48bb8a98653d4554ec4aac31a", DigestUtils.md5Hex(e.setDocument(ResourceHelper.getResourcePath("/webPages/website101.html")).getResultText()));
    }
    
    public static void main(String[] args) throws PageContentExtractorException, FileNotFoundException {
        WebPageContentExtractor e = new ReadabilityContentExtractor();
        String resultText = e.setDocument(ResourceHelper.getResourcePath("/webPages/website101.html")).getResultText();
        System.out.println(resultText);
        System.out.println(DigestUtils.md5Hex(resultText));
    }

}
