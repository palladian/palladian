package ws.palladian.helper.nlp;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import ws.palladian.helper.StopWatch;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * <p>
 * Test cases for the {@link StringHelper} class.
 * </p>
 *
 * @author David Urbansky
 * @author Philipp Katz
 */
public class StringHelperTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void testNumberToWord() {
        assertEquals(null, StringHelper.numberToWord(3.57));
        assertEquals(null, StringHelper.numberToWord(-1.));
        assertEquals("three", StringHelper.numberToWord(3.));
        assertEquals("seven", StringHelper.numberToWord(7.));
        assertEquals("twelve", StringHelper.numberToWord(12.));
    }

    //    @Test
    //    public void testNumbersToWords() {
    //        assertEquals("GameMaker KİTABI 90 Sayfalık GML dili One", StringHelper.numbersToNumberWords("GameMaker KİTABI 90 Sayfalık GML dili 1"));
    //    }

    @Test
    public void testFirstWord() {
        assertEquals("samsung", StringHelper.getFirstWord("samsung galaxy s4"));
        assertEquals("samsung", StringHelper.getFirstWord("samsung"));
        assertEquals("galaxy s4", StringHelper.removeFirstWord("samsung galaxy s4"));
        assertEquals("", StringHelper.removeFirstWord("samsung"));
    }

    @Test
    public void testRemoveWordsWithUnmodifiableList() {
        List<String> words = java.util.Collections.unmodifiableList(java.util.Arrays.asList("world", "hello"));
        try {
            StringHelper.removeWords(words, "hello world");
        } catch (UnsupportedOperationException e) {
            fail("removeWords modified the input list!");
        }
    }

    @Test
    public void testRemoveBrackets() {
        assertEquals("samsung s4", StringHelper.removeBrackets("samsung (galaxy) s4"));
        assertEquals("samsung s4", StringHelper.removeBrackets("samsung [galaxy] s4"));
        assertEquals("samsung s4", StringHelper.removeBrackets("samsung {galaxy} s4"));
        assertEquals("samsung s4 a", StringHelper.removeBrackets("samsung {galaxy} s4 (cool!) a {123}"));
        assertEquals("samsung s4 a", StringHelper.removeBrackets("samsung{galaxy} s4 (cool!)a {123}"));
        // TODO, nested, would require looping
        // assertEquals("samsung s4", StringHelper.removeBrackets("samsung (galaxy (pretty)) s4"));
    }

    @Test
    public void testIndexOfWord() {
        collector.checkThat(StringHelper.indexOfWordCaseSensitive("rice", "riceland yazmin rice"), is(16));
    }

    @Test
    public void testLastIndexOfWordCaseSensitive() {
        collector.checkThat(StringHelper.lastIndexOfWordCaseSensitive("test", "abc test and another test"), is(21));
    }

    @Test
    public void testGetContext() {
        collector.checkThat(StringHelper.getContext("test", "abc test and another bla bla bla", 10), is("abc test and anoth"));
    }

    @Test
    public void testContainsWord() {
        assertFalse(StringHelper.containsWord("5mm", "screw is 1.5mm long"));
        assertTrue(StringHelper.containsWord("organic", "Organic-pralines are tasty"));
        assertTrue(StringHelper.containsWord("pralines", "Organic-pralines are tasty"));
        assertTrue(StringHelper.containsWord("ich", "das finde ich persönlich nicht weiter tragisch"));

        assertTrue(StringHelper.containsWord("test", "a test b"));
        assertTrue(StringHelper.containsWord("test", "test"));
        assertTrue(StringHelper.containsWord("yes", "Yes, he went there."));
        assertTrue(StringHelper.containsWord("there", "Yes, he went there?"));
        assertTrue(StringHelper.containsWord("there", "Yes, he went there!"));
        assertTrue(StringHelper.containsWord("there", "Yes, he went there."));
        assertTrue(StringHelper.containsWord("Nokia N9", "hello, this (Nokia N9) is pretty cool."));
        assertFalse(StringHelper.containsWord("cab", "Copacabana, he went there."));

        assertTrue(StringHelper.containsWordCaseSensitive("test", "abtester ist test"));
        assertFalse(StringHelper.containsWordCaseSensitive("m", "gehaus aus --------- ø ca 40 mm"));
        assertTrue(StringHelper.containsWordCaseSensitive("test", "a test b"));
        assertFalse(StringHelper.containsWordCaseSensitive("test", "a Test b"));
        assertTrue(StringHelper.containsWordCaseSensitive("test", "test"));
        assertFalse(StringHelper.containsWordCaseSensitive("Test", "test"));

        assertFalse(StringHelper.containsWordCaseSensitive("tester", "abtester ist test"));
        assertFalse(StringHelper.containsWordCaseSensitive("a+", "energieklasse a++"));
        assertTrue(StringHelper.containsWordRegExp("test", "a test b"));
        assertTrue(StringHelper.containsWordRegExp("test", "test"));
        assertTrue(StringHelper.containsWordRegExp("yes", "Yes, he went there."));
        assertTrue(StringHelper.containsWordRegExp("there", "Yes, he went there?"));
        assertTrue(StringHelper.containsWordRegExp("there", "Yes, he went there!"));
        assertTrue(StringHelper.containsWordRegExp("there", "Yes, he went there."));
        assertTrue(StringHelper.containsWordRegExp("Nokia N9", "hello, this (Nokia N9) is pretty cool."));
        assertTrue(StringHelper.containsWordRegExp("Nokia N9", "hello, this [Nokia N9] is pretty cool."));
        assertFalse(StringHelper.containsWordRegExp("cab", "Copacabana, he went there."));
        assertTrue(StringHelper.containsWordRegExp("Deutsche.Bahn", "Die Deutsche Bahn"));
        assertTrue(StringHelper.containsWordRegExp("Deutsche.Bahn", "Die Deutsche-Bahn"));
        assertTrue(StringHelper.containsWordRegExp("Deutsche.Bahn", "Deutsche&Bahn"));
        assertFalse(StringHelper.containsWordRegExp("Deutsche.Bahn", "DeutscheBahn"));
        assertFalse(StringHelper.containsWordRegExp("Deutsche.Bahn", "Deutsche..Bahn"));

    }

    @Test
    public void testRemoveStemmedWord() {
        assertEquals("the in his car", StringHelper.removeStemmedWord("test", "the tester in his car"));
        assertEquals("the : all good", StringHelper.removeStemmedWord("test", "the tested: all good"));
        assertEquals("", StringHelper.removeStemmedWord("test", "testing"));
        assertEquals(", right on", StringHelper.removeStemmedWord("test", "test, right on"));
        assertEquals(", right on ", StringHelper.removeStemmedWord("test", "test, right on testing testost"));
    }

    @Test
    public void testReplaceWord() {
        assertEquals("a b", StringHelper.removeWord("test", "a TEST b"));
        StringBuilder stringBuilder = new StringBuilder("a test b");
        assertEquals("a  b", StringHelper.removeWordCaseSensitive("test", stringBuilder).toString());
        assertEquals("atest b", StringHelper.removeWord("test", "atest b"));
        assertEquals("atestb", StringHelper.removeWord("test", "atestb"));
        assertEquals("a testb", StringHelper.removeWord("test", "a testb"));
        assertEquals("", StringHelper.removeWord("test", "test"));
        assertEquals(".", StringHelper.removeWord("test", "test."));
        assertEquals("!", StringHelper.removeWord("test", "test!"));
        assertEquals("?", StringHelper.removeWord("test", "test?"));
        assertEquals(",", StringHelper.removeWord("test", "test,"));
        assertEquals(";", StringHelper.removeWord("test", "test;"));
        assertEquals("()", StringHelper.removeWord("test", "(test)"));
        assertEquals("", StringHelper.removeWord("", ""));
        assertEquals("", StringHelper.removeWord("abc", ""));
        assertEquals("abc", StringHelper.removeWord("", "abc"));
        assertEquals("it and or ", StringHelper.removeWord("abc", "it abc and Abc or aBc"));

        assertEquals("a (test) b", StringHelper.replaceWord("test", "(test)", "a test b"));
        assertEquals("a  b", StringHelper.replaceWord("test", "", "a test b"));
        assertEquals("a test b", StringHelper.replaceWord("", "", "a test b"));
        assertEquals("plug-in", StringHelper.replaceWord("in", "", "plug-in"));
    }

    @Test
    public void testClean() {
        assertEquals("Länge", StringHelper.clean("L\u00e4nge"));
        assertEquals("", StringHelper.clean(""));
        assertEquals("There is nothing to clean here", StringHelper.clean("There is nothing to clean here"));
        assertEquals("This is crözy text", StringHelper.clean("' This is crözy    text"));
        assertEquals("abcödef ghjiåjkl <mno å ???", StringHelper.clean("abc\u00f6def ghji\u00e5jkl &lt;mno \u00e5 ???:::"));
        assertEquals("here starts the <clean> \"text\" stop", StringHelper.clean("###here starts the &lt;clean&gt; &quot;text&quot; <b>stop</B>"));
        assertEquals("Say ‘hello’ to your horses for me", StringHelper.clean("Say &#8216;hello&#8217; to your horses for me"));
        assertEquals("Preheat oven to 375. Prepare a 8\" square", StringHelper.clean("Preheat oven to 375. Prepare a 8″ square"));
        assertEquals("Preheat oven to 375. Prepare a 8\" square", StringHelper.clean("Preheat oven\t\tto\n375. Prepare a 8″ square"));
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
    public void testRemoveNumbers() {
        assertEquals("Text whatever", StringHelper.removeNumbers("Text 1.2 whatever"));
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
    public void testContainsNumber() {
        assertTrue(StringHelper.containsNumber("120"));
        assertTrue(StringHelper.containsNumber("120.2 GB"));
        assertTrue(StringHelper.containsNumber("120.2GB"));
        assertTrue(StringHelper.containsNumber("$120"));
        assertFalse(StringHelper.containsNumber("A bc de2f GB"));
        assertFalse(StringHelper.containsNumber("A-1 GB"));
    }

    @Test
    public void testTrim() {
        // System.out.println(StringHelper.trim("'80GB'))"));
        assertEquals("Slave Zero X (Nintendo Switch)", StringHelper.trim("Slave Zero X (Nintendo Switch) –"));
        assertEquals("a++", StringHelper.trim("a++", "+"));
        assertEquals("++a++", StringHelper.trim("++a++", "+"));
        assertEquals("a", StringHelper.trim("++a++"));
        assertEquals("a++", StringHelper.trimLeft("++a++"));
        assertEquals("++a", StringHelper.trimRight("++a++"));

        assertEquals("", StringHelper.trim(","));
        assertEquals("", StringHelper.trim(""));
        assertEquals("", StringHelper.trim(". ,"));
        assertEquals("asd", StringHelper.trim(" ; asd ?-"));
        assertEquals("27 30 N, 90 30 E", StringHelper.trim("; ,.  27 30 N, 90 30 E -"));
        assertEquals("27 30 N, 90 30 E", StringHelper.trim(",.  27 30 N, 90 30 E  ##"));
        assertEquals("2", StringHelper.trim("' 2''"));
        // assertEquals("' 2\"", StringHelper.trim("' 2\""));
        assertEquals("abc", StringHelper.trim("\"abc\""));
        assertEquals("abc\"def", StringHelper.trim("\"abc\"def\""));
        assertEquals("abc", StringHelper.trim("\"abc"));
        // TODO? assertEquals(StringHelper.trim("'80GB'))"),"80GB");
        // assertEquals(StringHelper.trim("2\""),"2\"");
    }

    @Test
    public void testGetSubstringBetween() {
        assertEquals("2", StringHelper.getSubstringBetween("A: 1\nB: 2%", "B: ", "%"));
        assertEquals("all the lilacs", StringHelper.getSubstringBetween("all the lilacs in ohio", null, " in ohio"));
        assertEquals("the lilacs in ohio", StringHelper.getSubstringBetween("all the lilacs in ohio", "all ", null));
        assertEquals("the lilacs", StringHelper.getSubstringBetween("all the lilacs in ohio", "all ", " in ohio"));
        assertEquals("", StringHelper.getSubstringBetween("all the lilacs in ohio", "allt ", "in ohio"));
        assertEquals("", StringHelper.getSubstringBetween("all the lilacs in ohio", " in ohio", "all "));
        assertEquals(2, StringHelper.getSubstringsBetween("all the lilacs in ohio all the lilacs in ohio all the lilacs", "the ", " in").size());
        assertEquals(Arrays.asList("1", "2", "3", "4", "5"), StringHelper.getSubstringsBetween("(1) (2) (3) (4) (5) (6", "(", ")"));
    }

    @Test
    public void testCamelCaseToWords() {
        assertEquals("", StringHelper.camelCaseToWords(""));
        assertEquals("camel Case String", StringHelper.camelCaseToWords("camelCaseString"));
        assertEquals("camel.case.string", StringHelper.camelCaseToWords("camel.case.string"));
        assertEquals("camel_Case_String", StringHelper.camelCaseToWords("camelCaseString", "_"));
    }

    @Test
    public void testCountOccurrences() {
        assertEquals(1, StringHelper.countOccurrences("The quick brown fox jumps over the lazy dog", "the"));
        assertEquals(0, StringHelper.countOccurrences("The quick brown fox jumps over the lazy dog", "cat"));
        assertEquals(5, StringHelper.countOccurrences("aaaaa", "a"));
        assertEquals(2, StringHelper.countOccurrences("aaaaa", "aa"));
    }

    @Test
    public void testCountMatches() {
        assertEquals(2, StringHelper.countRegexMatches("The quick brown fox jumps over the lazy dog", "[Tt]he"));
        assertEquals(2, StringHelper.countRegexMatches("The quick brown fox jumps over the lazy dog", "fox|dog"));
        assertEquals(0, StringHelper.countRegexMatches("The quick brown fox jumps over the lazy dog", "cat"));
    }

    @Test
    public void testCountWhitespace() {
        assertEquals(0, StringHelper.countWhitespaces("nowhithespace"));
        assertEquals(1, StringHelper.countWhitespaces("one whitespace"));
        assertEquals(5, StringHelper.countWhitespaces(" five  whitespace  "));
    }

    @Test
    public void testGetFirstWords() {
        assertEquals("the quick brown fox jumps", StringHelper.getFirstWords("the quick brown fox jumps over the lazy dog", 5));
        assertEquals("the quick brown fox jumps over the lazy dog", StringHelper.getFirstWords("the quick brown fox jumps over the lazy dog", 15));
        assertEquals("", StringHelper.getFirstWords("", 10));
        assertEquals("", StringHelper.getFirstWords(null, 10));
    }

    @Test
    public void testGetLongest() {
        String s0 = "";
        String s1 = "a";
        String s2 = "aaa";
        String s3 = "aa";
        String s4 = null;
        assertEquals("aaa", StringHelper.getLongest(s0, s1, s2, s3, s4));
        assertEquals("", StringHelper.getLongest(s0, s4));
        assertEquals("", StringHelper.getLongest(s4, s0));
        assertEquals(null, StringHelper.getLongest(s4));
    }

    @Test
    public void testRemoveLineBreaks() {
        String test = "text\nwith\r\nline\rbreaks";
        assertEquals("text with line breaks", StringHelper.removeLineBreaks(test));
    }

    @Test
    public void testRemoveFourByteChars() {
        assertEquals("", StringHelper.removeFourByteChars("\uD83D\uDE01"));
        assertEquals("\u0021", StringHelper.removeFourByteChars("\u0021"));
        assertEquals("\u00B6", StringHelper.removeFourByteChars("\u00B6"));
        assertEquals("\u6771", StringHelper.removeFourByteChars("\u6771"));
        assertEquals("", StringHelper.removeFourByteChars("\uD801\uDC00"));
        assertEquals("Test ", StringHelper.removeFourByteChars("Test \uD83D\uDE01"));

        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 1000000; i++) {
            StringHelper.removeFourByteChars("Test \uD83D\uDE01 ; \u00B6");
        }
        System.out.println(stopWatch.getElapsedTimeString());

    }

    @Test
    public void testIsCompletelyUppercase() {
        assertTrue(StringHelper.isCompletelyUppercase("ABC"));
        assertFalse(StringHelper.isCompletelyUppercase("AbC"));
        assertTrue(StringHelper.isCompletelyUppercase("A BC"));
    }

    @Test
    public void testIsNumber() {
        assertTrue(StringHelper.isNumber("-2,3"));
        assertTrue(StringHelper.isNumber("100"));
        assertFalse(StringHelper.isNumber("100.000.00"));
        assertFalse(StringHelper.isNumber("44.000."));
        assertFalse(StringHelper.isNumber("44 000"));
        assertTrue(StringHelper.isNumber("44.000"));
        assertTrue(StringHelper.isNumber("41"));
        assertTrue(StringHelper.isNumber("-1"));
        assertTrue(StringHelper.isNumber("-1.3"));
        assertTrue(StringHelper.isNumber("-8787545,3"));
        assertTrue(StringHelper.isNumber("-8787545.3"));
        assertTrue(StringHelper.isNumber("-8787545,798435"));
        assertTrue(StringHelper.isNumber("-8787545.798435"));
        assertTrue(StringHelper.isNumber("3.4359738368E11"));
        assertTrue(StringHelper.isNumberOrNumberWord("45"));
        assertTrue(StringHelper.isNumberOrNumberWord("one"));
        assertTrue(StringHelper.isNumberOrNumberWord("two"));
        assertTrue(StringHelper.isNumberOrNumberWord("three"));
        assertTrue(StringHelper.isNumberOrNumberWord("four"));
        assertTrue(StringHelper.isNumberOrNumberWord("five"));
        assertTrue(StringHelper.isNumberOrNumberWord("six"));
        assertTrue(StringHelper.isNumberOrNumberWord("seven"));
        assertTrue(StringHelper.isNumberOrNumberWord("eight"));
        assertTrue(StringHelper.isNumberOrNumberWord("nine"));
        assertTrue(StringHelper.isNumberOrNumberWord("ten"));
        assertTrue(StringHelper.isNumberOrNumberWord("eleven"));
        assertTrue(StringHelper.isNumberOrNumberWord("twelve"));
    }

    @Test
    public void testIsNumericExpression() {
        assertFalse(StringHelper.isNumericExpression("44a000."));
        assertFalse(StringHelper.isNumericExpression("44 000 also"));
        assertTrue(StringHelper.isNumericExpression("44.000%"));
        assertTrue(StringHelper.isNumericExpression("41 %"));
        assertTrue(StringHelper.isNumericExpression("345,234,231"));
        assertTrue(StringHelper.isNumericExpression("$12,21€"));
        assertFalse(StringHelper.isNumericExpression("TBC"));

    }

    @Test
    public void testCalculateSimilarity() {
        assertEquals(1.0, StringHelper.calculateSimilarity("http://www.blu-ray.com/movies/movies.php?genre=action", "http://www.blu-ray.com/movies/movies.php?genre=action"), 0);
        assertEquals(1.0, StringHelper.calculateSimilarity("abc", "abcd"), 0);
        assertEquals(0.0, StringHelper.calculateSimilarity("", "abcd"), 0);
    }

    @Test
    public void testIsTimeExpression() {
        assertTrue(StringHelper.isTimeExpression("3:22 pm"));
        assertTrue(StringHelper.isTimeExpression("23:1am"));
        assertFalse(StringHelper.isTimeExpression("abc 23:13!"));
    }

    @Test
    public void testPutArticleInFront() {
        assertEquals("The Fog", StringHelper.putArticleInFront("Fog,the"));
        assertEquals("Los Amigos", StringHelper.putArticleInFront("Amigos, Los"));
    }

    @Test
    public void testContainsAny() {
        assertTrue(StringHelper.containsAny("the quick brown fox", Arrays.asList("cat", "dog", "fox")));
        assertFalse(StringHelper.containsAny("the quick brown fox", Arrays.asList("elephant", "tiger", "squirrel")));
    }

    @Test
    public void testRemoveEmptyLines() {
        assertEquals(3, StringHelper.removeEmptyLines("\n\nline1\n     line2\n \n \n \n \nline3").split("\n").length);
    }

    @Test
    public void testTrimLines() {
        assertEquals("line1\nline2\nline3", StringHelper.trimLines("\n\nline1\n     line2\n \n \n \n \nline3"));
    }

    @Test
    public void testNormalizeQuotes() {
        assertEquals("This is a sample text with \"different\" \"quotation\" \"marks\"",
                StringHelper.normalizeQuotes("This is a sample text with »different« „quotation“ “marks”"));
        assertEquals("This text's purpose is to test apostrophes normalized by StringHelper's normalizeQuotes",
                StringHelper.normalizeQuotes("This text‘s purpose is to test apostrophes normalized by StringHelper’s normalizeQuotes"));
        assertEquals("This text contains longer dashes - like this - and this", StringHelper.normalizeQuotes("This text contains longer dashes – like this — and this"));
    }

    @Test
    public void testGetSubphrases() {
        String text = "quick brown fox";
        List<String> subphrases = StringHelper.getSubPhrases(text);
        assertEquals(6, subphrases.size());
        assertTrue(subphrases.containsAll(asList("quick", "quick brown", "quick brown fox", "brown", "brown fox", "fox")));
        text = "";
        subphrases = StringHelper.getSubPhrases(text);
        assertEquals(0, subphrases.size());
    }

}
