package ws.palladian.helper.nlp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * <p>
 * Test cases for the {@link StringHelper} class.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class StringHelperTest {

    @Test
    public void testNumberToWord() {
        assertEquals(null, StringHelper.numberToWord(3.57));
        assertEquals(null, StringHelper.numberToWord(-1.));
        assertEquals("three", StringHelper.numberToWord(3.));
        assertEquals("seven", StringHelper.numberToWord(7.));
        assertEquals("twelve", StringHelper.numberToWord(12.));
    }

    @Test
    public void testFirstWord() {
        assertEquals("samsung", StringHelper.getFirstWord("samsung galaxy s4"));
        assertEquals("samsung", StringHelper.getFirstWord("samsung"));
        assertEquals("galaxy s4", StringHelper.removeFirstWord("samsung galaxy s4"));
        assertEquals("", StringHelper.removeFirstWord("samsung"));
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
    public void testContainsWord() {

        assertEquals(true, StringHelper.containsWord("test", "a test b"));
        assertEquals(true, StringHelper.containsWord("test", "test"));
        assertEquals(true, StringHelper.containsWord("yes", "Yes, he went there."));
        assertEquals(true, StringHelper.containsWord("there", "Yes, he went there?"));
        assertEquals(true, StringHelper.containsWord("there", "Yes, he went there!"));
        assertEquals(true, StringHelper.containsWord("there", "Yes, he went there."));
        assertEquals(true, StringHelper.containsWord("Nokia N9", "hello, this (Nokia N9) is pretty cool."));
        assertEquals(false, StringHelper.containsWord("cab", "Copacabana, he went there."));

        assertEquals(true, StringHelper.containsWordRegExp("test", "a test b"));
        assertEquals(true, StringHelper.containsWordRegExp("test", "test"));
        assertEquals(true, StringHelper.containsWordRegExp("yes", "Yes, he went there."));
        assertEquals(true, StringHelper.containsWordRegExp("there", "Yes, he went there?"));
        assertEquals(true, StringHelper.containsWordRegExp("there", "Yes, he went there!"));
        assertEquals(true, StringHelper.containsWordRegExp("there", "Yes, he went there."));
        assertEquals(true, StringHelper.containsWordRegExp("Nokia N9", "hello, this (Nokia N9) is pretty cool."));
        assertEquals(true, StringHelper.containsWordRegExp("Nokia N9", "hello, this [Nokia N9] is pretty cool."));
        assertEquals(false, StringHelper.containsWordRegExp("cab", "Copacabana, he went there."));
        assertEquals(true, StringHelper.containsWordRegExp("Deutsche.Bahn", "Die Deutsche Bahn"));
        assertEquals(true, StringHelper.containsWordRegExp("Deutsche.Bahn", "Die Deutsche-Bahn"));
        assertEquals(true, StringHelper.containsWordRegExp("Deutsche.Bahn", "Deutsche&Bahn"));
        assertEquals(false, StringHelper.containsWordRegExp("Deutsche.Bahn", "DeutscheBahn"));
        assertEquals(false, StringHelper.containsWordRegExp("Deutsche.Bahn", "Deutsche..Bahn"));

    }

    @Test
    public void testReplaceWord() {
        assertEquals("a b", StringHelper.removeWord("test", "a test b"));
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
    }

    @Test
    public void testClean() {
        assertEquals("", StringHelper.clean(""));
        assertEquals("There is nothing to clean here", StringHelper.clean("There is nothing to clean here"));
        assertEquals("This is crözy text", StringHelper.clean("' This is crözy    text"));
        assertEquals("abcödef ghjiåjkl <mno å ???",
                StringHelper.clean("abc\u00f6def ghji\u00e5jkl &lt;mno \u00e5 ???:::"));
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
        assertEquals(4, (int)list.get(0));
        assertEquals(7, (int)list.get(1));
        assertEquals(9, (int)list.get(2));
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
        assertEquals(true, StringHelper.containsNumber("120"));
        assertEquals(true, StringHelper.containsNumber("120.2 GB"));
        assertEquals(false, StringHelper.containsNumber("A bc de2f GB"));
        assertEquals(false, StringHelper.containsNumber("A-1 GB"));
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
        // assertEquals("' 2\"", StringHelper.trim("' 2\""));
        assertEquals("abc", StringHelper.trim("\"abc\""));
        assertEquals("abc\"def", StringHelper.trim("\"abc\"def\""));
        assertEquals("abc", StringHelper.trim("\"abc"));
        // TODO? assertEquals(StringHelper.trim("'80GB'))"),"80GB");
        // assertEquals(StringHelper.trim("2\""),"2\"");
    }

    @Test
    public void testEscapeForRegularExpression() {
        assertEquals("\\(2008\\)", StringHelper.escapeForRegularExpression("(2008)"));
        // String containing RegEx meta characters which need to be escaped
        String s = "(the) [quick] {brown} fox$ ^jumps+ \n ov|er the? l-a\\zy ]dog[";
        // test successful escape by matching escaped RegEx ...
        assertTrue(s.matches(StringHelper.escapeForRegularExpression(s)));
    }

    @Test
    public void testGetSubstringBetween() {
        assertEquals("all the lilacs", StringHelper.getSubstringBetween("all the lilacs in ohio", null, " in ohio"));
        assertEquals("the lilacs in ohio", StringHelper.getSubstringBetween("all the lilacs in ohio", "all ", null));
        assertEquals("the lilacs", StringHelper.getSubstringBetween("all the lilacs in ohio", "all ", " in ohio"));
        assertEquals("", StringHelper.getSubstringBetween("all the lilacs in ohio", "allt ", "in ohio"));
        assertEquals("", StringHelper.getSubstringBetween("all the lilacs in ohio", " in ohio", "all "));
        assertEquals(
                2,
                StringHelper.getSubstringsBetween("all the lilacs in ohio all the lilacs in ohio all the lilacs",
                        "the ", " in").size());
        assertEquals(Arrays.asList("1", "2", "3", "4", "5"),
                StringHelper.getSubstringsBetween("(1) (2) (3) (4) (5) (6", "(", ")"));
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
        assertEquals("the quick brown fox jumps",
                StringHelper.getFirstWords("the quick brown fox jumps over the lazy dog", 5));
        assertEquals("the quick brown fox jumps over the lazy dog",
                StringHelper.getFirstWords("the quick brown fox jumps over the lazy dog", 15));
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
    }

    @Test
    public void testIsCompletelyUppercase() {
        assertEquals(true, StringHelper.isCompletelyUppercase("ABC"));
        assertEquals(false, StringHelper.isCompletelyUppercase("AbC"));
        assertEquals(true, StringHelper.isCompletelyUppercase("A BC"));
    }

    @Test
    public void testIsNumber() {
        assertEquals(false, StringHelper.isNumber("44.000."));
        assertEquals(false, StringHelper.isNumber("44 000"));
        assertEquals(true, StringHelper.isNumber("44.000"));
        assertEquals(true, StringHelper.isNumber("41"));
        assertEquals(true, StringHelper.isNumberOrNumberWord("45"));
        assertEquals(true, StringHelper.isNumberOrNumberWord("one"));
        assertEquals(true, StringHelper.isNumberOrNumberWord("two"));
        assertEquals(true, StringHelper.isNumberOrNumberWord("three"));
        assertEquals(true, StringHelper.isNumberOrNumberWord("four"));
        assertEquals(true, StringHelper.isNumberOrNumberWord("five"));
        assertEquals(true, StringHelper.isNumberOrNumberWord("six"));
        assertEquals(true, StringHelper.isNumberOrNumberWord("seven"));
        assertEquals(true, StringHelper.isNumberOrNumberWord("eight"));
        assertEquals(true, StringHelper.isNumberOrNumberWord("nine"));
        assertEquals(true, StringHelper.isNumberOrNumberWord("ten"));
        assertEquals(true, StringHelper.isNumberOrNumberWord("eleven"));
        assertEquals(true, StringHelper.isNumberOrNumberWord("twelve"));
    }

    @Test
    public void testIsNumericExpression() {
        assertEquals(false, StringHelper.isNumericExpression("44a000."));
        assertEquals(false, StringHelper.isNumericExpression("44 000 also"));
        assertEquals(true, StringHelper.isNumericExpression("44.000%"));
        assertEquals(true, StringHelper.isNumericExpression("41 %"));
        assertEquals(true, StringHelper.isNumericExpression("345,234,231"));
        assertEquals(true, StringHelper.isNumericExpression("$12,21€"));
        assertEquals(false, StringHelper.isNumericExpression("TBC"));

    }

    @Test
    public void testCalculateSimilarity() {
        assertEquals(1.0, StringHelper.calculateSimilarity("http://www.blu-ray.com/movies/movies.php?genre=action",
                "http://www.blu-ray.com/movies/movies.php?genre=action"), 0);
        assertEquals(1.0, StringHelper.calculateSimilarity("abc", "abcd"), 0);
        assertEquals(0.0, StringHelper.calculateSimilarity("", "abcd"), 0);
    }

    @Test
    public void testIsTimeExpression() {
        assertEquals(true, StringHelper.isTimeExpression("3:22 pm"));
        assertEquals(true, StringHelper.isTimeExpression("23:1am"));
        assertEquals(false, StringHelper.isTimeExpression("abc 23:13!"));
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
        assertEquals(
                "This text's purpose is to test apostrophes normalized by StringHelper's normalizeQuotes",
                StringHelper
                        .normalizeQuotes("This text‘s purpose is to test apostrophes normalized by StringHelper’s normalizeQuotes"));
        assertEquals("This text contains longer dashes - like this - and this",
                StringHelper.normalizeQuotes("This text contains longer dashes – like this — and this"));
    }

}
