package ws.palladian.helper.nlp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Test;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.RegExp;

/**
 * Test cases for the StringHelper class.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class StringHelperTest {

    @Test
    public void testClean() {
        assertEquals("", StringHelper.clean(""));
        assertEquals("There is nothing to clean here", StringHelper.clean("There is nothing to clean here"));
        assertEquals("This is crözy text", StringHelper.clean("' This is crözy    text"));
        assertEquals("abcödef ghjiåjkl <mno å", StringHelper.clean("abc\u00f6def ghji\u00e5jkl &lt;mno \u00e5 ???:::"));
        assertEquals("here starts the <clean> \"text\" stop",
                StringHelper.clean("###here starts the &lt;clean&gt; &quot;text&quot; <b>stop</B>"));

        assertEquals("Say ‘hello’ to your horses for me",
                StringHelper.clean("Say &#8216;hello&#8217; to your horses for me"));
    }

    @Test
    public void testGetCaseSignature() {
        // System.out.println(StringHelper.getCaseSignature("Äpfelsüppchen"));
        assertEquals("Aa", StringHelper.getCaseSignature("Hello"));
        assertEquals("a a a", StringHelper.getCaseSignature("this is nice"));
        assertEquals("A 0", StringHelper.getCaseSignature("SUPER 8"));
        assertEquals("Aa- 0 Aa", StringHelper.getCaseSignature("Super!? 8 Zorro"));
        assertEquals("Aa", StringHelper.getCaseSignature("Äpfelsüppchen"));
        assertEquals("a-a-0-", StringHelper.getCaseSignature("amazing(grace){1}"));
        assertEquals("Aa -Aa- Aa", StringHelper.getCaseSignature("Bruce \"Batman\" Wayne"));
    }

    @Test
    public void testRemoveNumbering() {
        assertEquals("Text", StringHelper.removeNumbering("Text"));
        assertEquals("Text", StringHelper.removeNumbering("1 Text"));
        assertEquals("Text", StringHelper.removeNumbering(" 1 Text"));
        assertEquals("Text", StringHelper.removeNumbering("1. Text"));
        assertEquals("Text", StringHelper.removeNumbering("1.      Text"));
        assertEquals("Text", StringHelper.removeNumbering("1.2.3.Text"));
        assertEquals("Text", StringHelper.removeNumbering("1.2.3.4     Text"));
        assertEquals("Led Zeppelin", StringHelper.removeNumbering("#14 Led Zeppelin"));

    }

    @Test
    public void testGetOccurrenceIndices() {
        List<Integer> list = StringHelper.getOccurrenceIndices("This is a test.", " ");
        assertEquals(3, list.size());
        assertEquals(4, (int) list.get(0));
        assertEquals(7, (int) list.get(1));
        assertEquals(9, (int) list.get(2));
    }

    @Test
    public void testGetLongestCommonString() {
        assertEquals("abc", StringHelper.getLongestCommonString("abcd", "abcefg", false, false));
        assertEquals("abcdEf", StringHelper.getLongestCommonString("abcdEfE", "abcdEfefg", true, false));
        assertEquals("BCD", StringHelper.getLongestCommonString("ABCD", "BCDE", true, true));
        assertEquals("", StringHelper.getLongestCommonString("ABCD", "BCDE", false, false));

    }

    @Test
    public void testReverseString() {
        assertEquals("fe dcBA", StringHelper.reverseString("ABcd ef"));
    }

    @Test
    public void testRename() {
        // System.out.println(FileHelper.rename(new
        // File("data/test/sampleTextForTagging.txt"),"sampleTextForTagging_tagged"));
        String renamedFile = FileHelper.getRenamedFilename(
                new File(StringHelperTest.class.getResource("/sampleTextForTagging.txt").getFile()),
                "sampleTextForTagging_tagged");
        renamedFile = renamedFile.substring(renamedFile.lastIndexOf(File.separatorChar) + 1);
        assertEquals("sampleTextForTagging_tagged.txt", renamedFile);
    }

    @Test
    public void testIsFileName() {
        assertEquals(true, FileHelper.isFileName(" website.html"));
        assertEquals(true, FileHelper.isFileName("test.ai "));
        assertEquals(false, FileHelper.isFileName(".just a sentence. "));
        assertEquals(false, FileHelper.isFileName("everything..."));
    }

    @Test
    public void testContainsNumber() {
        assertEquals(true, StringHelper.containsNumber("120"));
        assertEquals(true, StringHelper.containsNumber("120.2 GB"));
        assertEquals(false, StringHelper.containsNumber("A bc de2f GB"));
        assertEquals(false, StringHelper.containsNumber("A-1 GB"));
    }

    @Test
    public void testRemoveStopWords() {
        assertEquals("...neighborhoodthe ofrocking.",
                StringHelper.removeStopWords("...The neighborhoodthe is ofrocking of."));
        assertEquals("neighborhood; REALLY; rocking!",
                StringHelper.removeStopWords("The neighborhood is; IS REALLY; rocking of!"));
    }

    @Test
    public void testTrim() {
        // System.out.println(StringHelper.trim("'80GB'))"));
        assertEquals("", StringHelper.trim(","));
        assertEquals("", StringHelper.trim(""));
        assertEquals("", StringHelper.trim(". ,"));
        assertEquals("asd", StringHelper.trim(" ; asd ?-"));
        assertEquals("27 30 N, 90 30 E", StringHelper.trim("; ,.  27 30 N, 90 30 E -"));
        assertEquals("27 30 N, 90 30 E", StringHelper.trim(",.  27 30 N, 90 30 E  ##"));
        assertEquals("2", StringHelper.trim("' 2''"));
        //assertEquals("' 2\"", StringHelper.trim("' 2\""));
        assertEquals("abc", StringHelper.trim("\"abc\""));
        assertEquals("abc\"def", StringHelper.trim("\"abc\"def\""));
        assertEquals("abc", StringHelper.trim("\"abc"));
        // TODO? assertEquals(StringHelper.trim("'80GB'))"),"80GB");
        // assertEquals(StringHelper.trim("2\""),"2\"");
    }

    @Test
    public void testLFEColonPattern() {

        assertEquals("Volume: 96 cc",
                StringHelper.concatMatchedString("Volume: 96 cc", "||", RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("Volume: 96",
                StringHelper.concatMatchedString("Volume: 96", "||", RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("Volume: 96 cc||Weight: 128 g",
                StringHelper.concatMatchedString("Volume: 96 ccWeight: 128 g", "||", RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("Volume : 96 cc||Weight : 128 g", StringHelper.concatMatchedString("Volume : 96 ccWeight : 128 g",
                "||", RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("Volume/V: 96 cc||Weight: 128 g", StringHelper.concatMatchedString("Volume/V: 96 ccWeight: 128 g",
                "||", RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("Volume:96 cc||Weight: 128 g",
                StringHelper.concatMatchedString("Volume:96 ccWeight: 128 g", "||", RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("Volume: 96 cc||Net Weight: 128 g", StringHelper.concatMatchedString(
                "Volume: 96 ccNet Weight: 128 g", "||", RegExp.COLON_FACT_REPRESENTATION));
        // assertEquals("Volume: 96 cc||Weight: 128 g",
        // StringHelper.concatMatchedString("Volume: 96 cc,Weight: 128 g","||",RegExp.COLON_FACT_REPRESENTATION));
        // assertEquals("Volume: 96 cc||Net weight: 128 g",
        // StringHelper.concatMatchedString("Volume: 96 cc,Net weight: 128 g","||",RegExp.COLON_FACT_REPRESENTATION));
        // assertEquals("Volume/V: 96 cc||Weight/W: 128 g",
        // StringHelper.concatMatchedString("Volume/V: 96 cc,Weight/W: 128 g","||",RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("V8: yes, 800kb",
                StringHelper.concatMatchedString("V8: yes, 800kb", "||", RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("V8: yes, 800kb",
                StringHelper.concatMatchedString("V8: yes, 800kbDimensions", "||", RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("Weight: 800, 600",
                StringHelper.concatMatchedString("Weight: 800, 600Dimensions", "||", RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("Weight: 800, 600", StringHelper.concatMatchedString("Weight: 800, 600MBDimensions", "||",
                RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("Weight: 800, 600MB", StringHelper.concatMatchedString("Weight: 800, 600MB Dimensions", "||",
                RegExp.COLON_FACT_REPRESENTATION));
        // assertEquals("General InfoNetwork: GSM 1900, UMTS, GSM 800",
        // StringHelper.concatMatchedString("General InfoNetwork:&nbsp;GSM 1900, UMTS, GSM 800","||",RegExp.COLON_FACT_REPRESENTATION));
        // assertEquals("Available Color(s): Black", StringHelper.concatMatchedString("Available Color(s):&nbsp;Black",
        // "||",
        // RegExp.COLON_FACT_REPRESENTATION));
        // assertEquals("General InfoNetwork: GSM 1900||Dimensions: 111 x 50 x 18.8 mm||Screen Size: 240 x 320 pixels||Color Depth: 16M colors, TFT||Weight: 114 g||Available Color(s): Black",
        // StringHelper.concatMatchedString("General InfoNetwork:&nbsp;GSM 1900Dimensions:&nbsp;111 x 50 x 18.8 mmScreen Size:&nbsp;240 x 320 pixelsColor Depth:&nbsp;16M colors, TFTWeight:&nbsp;114 gAvailable Color(s):&nbsp;Black","||",RegExp.COLON_FACT_REPRESENTATION));

    }

    @Test
    public void testEscapeForRegularExpression() {
        // String containing RegEx meta characters which need to be escaped
        String s = "(the) [quick] {brown} fox$ ^jumps+ \n ov|er the? l-a\\zy ]dog[";
        // test successful escape by matching escaped RegEx ...
        assertTrue(s.matches(StringHelper.escapeForRegularExpression(s)));
    }

    @Test
    public void testGetSubstringBetween() {
        assertEquals("the lilacs", StringHelper.getSubstringBetween("all the lilacs in ohio", "all ", " in ohio"));
        assertEquals("", StringHelper.getSubstringBetween("all the lilacs in ohio", "allt ", "in ohio"));
    }

    @Test
    public void testCamelCaseToWords() {
        assertEquals("", StringHelper.camelCaseToWords(""));
        assertEquals("camel Case String", StringHelper.camelCaseToWords("camelCaseString"));
        assertEquals("camel.case.string", StringHelper.camelCaseToWords("camel.case.string"));
        assertEquals("camel_Case_String", StringHelper.camelCaseToWords("camelCaseString", "_"));
    }

    @Test
    public void testCountOccurences() {
        assertEquals(2, StringHelper.countOccurences("The quick brown fox jumps over the lazy dog", "the", true));
        assertEquals(1, StringHelper.countOccurences("The quick brown fox jumps over the lazy dog", "the", false));
        assertEquals(0, StringHelper.countOccurences("The quick brown fox jumps over the lazy dog", "cat", false));
        assertEquals(5, StringHelper.countOccurences("aaaaa", "a", false));
        assertEquals(2, StringHelper.countOccurences("aaaaa", "aa", false));
    }

    @Test
    public void testGetFirstWords() {
        assertEquals("the quick brown fox jumps",
                StringHelper.getFirstWords("the quick brown fox jumps over the lazy dog", 5));
        assertEquals("the quick brown fox jumps over the lazy dog",
                StringHelper.getFirstWords("the quick brown fox jumps over the lazy dog", 15));
        assertEquals("", StringHelper.getFirstWords("", 10));
        assertEquals("", StringHelper.getFirstWords(null, 10));
    }

    @Test
    public void testExtractUrls() {

        String text = "The quick brown fox jumps over the lazy dog. Check out: http://microsoft.com, www.apple.com, google.com. (www.tu-dresden.de), http://arstechnica.com/open-source/news/2010/10/mozilla-releases-firefox-4-beta-for-maemo-and-android.ars.";
        List<String> urls = StringHelper.extractUrls(text);

        assertEquals(4, urls.size());
        assertEquals("http://microsoft.com", urls.get(0));
        assertEquals("www.apple.com", urls.get(1));
        // assertEquals("google.com", urls.get(2)); // not recognized
        assertEquals("www.tu-dresden.de", urls.get(2));
        assertEquals(
                "http://arstechnica.com/open-source/news/2010/10/mozilla-releases-firefox-4-beta-for-maemo-and-android.ars",
                urls.get(3));

    }

}