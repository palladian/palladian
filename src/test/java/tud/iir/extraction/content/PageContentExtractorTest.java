package tud.iir.extraction.content;

import junit.framework.Assert;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Ignore;
import org.junit.Test;

import tud.iir.classification.page.ClassifierTest;
import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.extraction.content.PageContentExtractorException;
import tud.iir.helper.FileHelper;

public class PageContentExtractorTest {

    /**
     * Run with sample data and write results to text files.
     * 
     * @throws PageContentExtractorException
     */
    @Test
    @Ignore
    public void runPageContentExtractorWithTestFiles() throws PageContentExtractorException {
        PageContentExtractor e = new PageContentExtractor();
        // e.setWriteDump(true);
        FileHelper.writeToFile(ClassifierTest.class.getResource("/pageContentExtractor/result/test001.txt").getFile(), e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test001.html").getFile()).getResultText());
        FileHelper.writeToFile(ClassifierTest.class.getResource("/pageContentExtractor/result/test002.txt").getFile(), e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test002.html").getFile()).getResultText());
        FileHelper.writeToFile(ClassifierTest.class.getResource("/pageContentExtractor/result/test003.txt").getFile(), e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test003.html").getFile()).getResultText());
        FileHelper.writeToFile(ClassifierTest.class.getResource("/pageContentExtractor/result/test004.txt").getFile(), e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test004.html").getFile()).getResultText());
        FileHelper.writeToFile(ClassifierTest.class.getResource("/pageContentExtractor/result/test005.txt").getFile(), e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test005.html").getFile()).getResultText());
        FileHelper.writeToFile(ClassifierTest.class.getResource("/pageContentExtractor/result/test006.txt").getFile(), e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test006.html").getFile()).getResultText());
        FileHelper.writeToFile(ClassifierTest.class.getResource("/pageContentExtractor/result/test007.txt").getFile(), e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test007.html").getFile()).getResultText());
        FileHelper.writeToFile(ClassifierTest.class.getResource("/pageContentExtractor/result/test008.txt").getFile(), e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test008.html").getFile()).getResultText());
        FileHelper.writeToFile(ClassifierTest.class.getResource("/pageContentExtractor/result/test009.txt").getFile(), e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test009.html").getFile()).getResultText());
        FileHelper.writeToFile(ClassifierTest.class.getResource("/pageContentExtractor/result/test010.txt").getFile(), e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test010.html").getFile()).getResultText());
        FileHelper.writeToFile(ClassifierTest.class.getResource("/pageContentExtractor/result/test011.txt").getFile(), e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test011.html").getFile()).getResultText());
        FileHelper.writeToFile(ClassifierTest.class.getResource("/pageContentExtractor/result/test012.txt").getFile(), e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test012.html").getFile()).getResultText());
        FileHelper.writeToFile(ClassifierTest.class.getResource("/pageContentExtractor/result/test013.txt").getFile(), e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test013.html").getFile()).getResultText());
        FileHelper.writeToFile(ClassifierTest.class.getResource("/pageContentExtractor/result/test014.txt").getFile(), e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test014.html").getFile()).getResultText());
        FileHelper.writeToFile(ClassifierTest.class.getResource("/pageContentExtractor/result/test015.txt").getFile(), e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test015.html").getFile()).getResultText());
        FileHelper.writeToFile(ClassifierTest.class.getResource("/pageContentExtractor/result/test018.txt").getFile(), e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test018.html").getFile()).getResultText());
    }

    /**
     * Calculate hash values for test data.
     * 
     * @throws PageContentExtractorException
     */
    @Test
    @Ignore
    public void calculateHashesForTestFiles() throws PageContentExtractorException {
        PageContentExtractor e = new PageContentExtractor();
        System.out.println(DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test001.html").getFile()).getResultText()));
        System.out.println(DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test002.html").getFile()).getResultText()));
        System.out.println(DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test003.html").getFile()).getResultText()));
        System.out.println(DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test004.html").getFile()).getResultText()));
        System.out.println(DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test005.html").getFile()).getResultText()));
        System.out.println(DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test006.html").getFile()).getResultText()));
        System.out.println(DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test007.html").getFile()).getResultText()));
        System.out.println(DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test008.html").getFile()).getResultText()));
        System.out.println(DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test009.html").getFile()).getResultText()));
        System.out.println(DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test010.html").getFile()).getResultText()));
        System.out.println(DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test011.html").getFile()).getResultText()));
        System.out.println(DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test012.html").getFile()).getResultText()));
        System.out.println(DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test013.html").getFile()).getResultText()));
        System.out.println(DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test014.html").getFile()).getResultText()));
        System.out.println(DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test015.html").getFile()).getResultText()));
        System.out.println(DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test018.html").getFile()).getResultText()));
    }

    /**
     * Test results. In case this test fails, manually compare result to data/test/pageContentExtractor/result/... fixed ... this test fails when run from
     * Maven, but succeeds from Eclipse. Why? See Mail from David, 2010-06-02 Turned out that this was an issue with an outdated Apache Commons Codec library,
     * see https://issues.apache.org/jira/browse/CODEC-73 Updated pom.xml to use the latest version of the library, test should succeed now on all systems --
     * 2010-06-10.
     */
    @Test
    // @Ignore
    public void testPageContentExtractorWithHashes() throws PageContentExtractorException {
        PageContentExtractor e = new PageContentExtractor();
        Assert.assertEquals("test001.html", "a078a28fd8d1a59d9364b53c4818539b", DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test001.html").getFile()).getResultText()));
        Assert.assertEquals("test002.html", "0211a72e522229abdb6b4019279ea128", DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test002.html").getFile()).getResultText()));
        Assert.assertEquals("test003.html", "2865c50e6223a110e0de79c76a45e5fa", DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test003.html").getFile()).getResultText()));
        Assert.assertEquals("test004.html", "39a5fb4526d7ab362b21384a86902665", DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test004.html").getFile()).getResultText()));
        Assert.assertEquals("test005.html", "12eb1af518752d13d9af10bb7b4da3f9", DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test005.html").getFile()).getResultText()));
        Assert.assertEquals("test006.html", "797e0ebf8d89e9c93762a888f0b4bd64", DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test006.html").getFile()).getResultText()));
        Assert.assertEquals("test007.html", "6c2ff6c964a3b57ed1e483014b477353", DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test007.html").getFile()).getResultText()));
        Assert.assertEquals("test008.html", "d0d155ebd785848a27509257e8fe2726", DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test008.html").getFile()).getResultText()));
        Assert.assertEquals("test009.html", "11b3f8cbd9bc774588da6327f280a1ab", DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test009.html").getFile()).getResultText()));
        Assert.assertEquals("test010.html", "829687fa8a170b3971db50f76d41a8ca", DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test010.html").getFile()).getResultText()));
        Assert.assertEquals("test011.html", "b17d23137b7693a92e02646c0e83dd12", DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test011.html").getFile()).getResultText()));
        Assert.assertEquals("test012.html", "3f8eb27a5bc33d2d71f1314595e7594b", DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test012.html").getFile()).getResultText()));
        Assert.assertEquals("test013.html", "af25ba7317e8df1aca75fc1956372d54", DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test013.html").getFile()).getResultText()));
        Assert.assertEquals("test014.html", "afeabbbfb8c3f1c3943a05ba772da59f", DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test014.html").getFile()).getResultText()));
        Assert.assertEquals("test015.html", "1788005daaedfcb997c64802f5c42a46", DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test015.html").getFile()).getResultText()));
        Assert.assertEquals("test018.html", "70bf83d80e0757a8f99fe4331a5244a6", DigestUtils.md5Hex(e.setDocument(ClassifierTest.class.getResource("/pageContentExtractor/test018.html").getFile()).getResultText()));
    }
    
    // main method to produce data for test case ...
    /*public static void main(String[] args) throws PageContentExtractorException {
        PageContentExtractor e = new PageContentExtractor();
        String resultText = e.setDocument("data/test/pageContentExtractor/test018.html").getResultText();
        System.out.println(DigestUtils.md5Hex(resultText));
        FileHelper.writeToFile("data/test/pageContentExtractor/result/test018.txt", resultText);
    }*/
    

}
