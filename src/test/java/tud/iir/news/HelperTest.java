package tud.iir.news;

import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Test a helper.
 * 
 * @author Philipp Katz
 * 
 */
public class HelperTest {

    // // this functionality has been moved to the Crawler and can be removed in the future.
    // @Test
    // public void testUrlConversion() {
    //
    // Assert.assertEquals("http://www.xyz.de/page.html", Helper.getFullUrl("http://www.xyz.de", "", "page.html"));
    // Assert.assertEquals("http://www.xyz.de/page.html", Helper.getFullUrl("http://www.xyz.de", null, "page.html"));
    // Assert.assertEquals("http://www.xyz.de/page.html", Helper.getFullUrl("http://www.xyz.de/index.html", "",
    // "page.html"));
    // Assert.assertEquals("http://www.xyz.de/page.html", Helper.getFullUrl("http://www.xyz.de/index.html",
    // "/directory", "/page.html"));
    // Assert.assertEquals("http://www.xyz.de/directory/page.html", Helper.getFullUrl("http://www.xyz.de/index.html",
    // "/directory", "./page.html"));
    // Assert.assertEquals("http://www.xyz.de/directory/page.html", Helper.getFullUrl("http://www.xyz.de/index.html",
    // "/directory/directory", "../page.html"));
    //
    // Assert.assertEquals("http://www.abc.de/page.html", Helper.getFullUrl("http://www.xyz.de", "",
    // "http://www.abc.de/page.html"));
    // Assert.assertEquals("http://www.abc.de/page.html", Helper.getFullUrl("http://www.xyz.de", "http://www.abc.de/",
    // "/page.html"));
    //
    // }

    // @Test
    // public void testGetDurationString() {
    // Assert.assertEquals("3d 4h 3min 43s 872ms", Helper.getDurationString(273823872));
    // Assert.assertEquals("0ms", Helper.getDurationString(0));
    // }

    @Test
    public void testGetFirstWords() {
        Assert.assertEquals("the quick brown fox jumps", Helper.getFirstWords(
                "the quick brown fox jumps over the lazy dog", 5));
        Assert.assertEquals("the quick brown fox jumps over the lazy dog", Helper.getFirstWords(
                "the quick brown fox jumps over the lazy dog", 15));
        Assert.assertEquals("", Helper.getFirstWords("", 10));
        Assert.assertEquals("", Helper.getFirstWords(null, 10));
    }

    // @Test
    // public void testSplitXPath() {
    // Assert.assertTrue(Arrays.equals(new String[] { "", "a", "b", "c[@attribute='x/yz']" },
    // Helper.splitXPath("/a/b/c[@attribute='x/yz']")));
    // }
    //
    // @Test
    // public void testCommonXPath() {
    // Assert.assertEquals("a/b/c/d", Helper.getLargestCommonXPath("a/b/c/d/e/f", "a/b/c/d/f/e"));
    // Assert.assertEquals("a/b/c/d", Helper.getLargestCommonXPath("a/b/c/d/e/f", "a/b/c/d"));
    // Assert.assertEquals("/a/b", Helper.getLargestCommonXPath("/a/b/c", "/a/b/d"));
    // Assert.assertEquals("//a/b", Helper.getLargestCommonXPath("//a/b/c", "//a/b/d"));
    // Assert.assertEquals("//a[1]/b[2]", Helper.getLargestCommonXPath("//a[1]/b[2]/c[3]", "//a[1]/b[2]/c[4]"));
    // }

    @Test
    public void testCountOccurences() {
        Assert.assertEquals(2, Helper.countOccurences("The quick brown fox jumps over the lazy dog", "the", true));
        Assert.assertEquals(1, Helper.countOccurences("The quick brown fox jumps over the lazy dog", "the", false));
        Assert.assertEquals(0, Helper.countOccurences("The quick brown fox jumps over the lazy dog", "cat", false));
        Assert.assertEquals(5, Helper.countOccurences("aaaaa", "a", false));
        Assert.assertEquals(2, Helper.countOccurences("aaaaa", "aa", false));
    }

    @Test
    public void testReadableBytes() {
        Assert.assertEquals("0 B", Helper.getReadibleBytes(0));
        Assert.assertEquals("512 B", Helper.getReadibleBytes(512));
        Assert.assertEquals("1.00 KiB", Helper.getReadibleBytes(1024));
        Assert.assertEquals("1.00 MiB", Helper.getReadibleBytes(1048576));
        Assert.assertEquals("46.69 MiB", Helper.getReadibleBytes(48956748));
    }

}
