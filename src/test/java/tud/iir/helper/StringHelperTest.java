package tud.iir.helper;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;
import tud.iir.knowledge.RegExp;

/**
 * Test cases for the String Helper class.
 * 
 * @author David Urbansky
 */
public class StringHelperTest extends TestCase {

    public StringHelperTest(String name) {
        super(name);
    }

    public void testRemoveNumbering() {
        assertEquals("Text", StringHelper.removeNumbering("Text"));
        assertEquals("Text", StringHelper.removeNumbering("1 Text"));
        assertEquals("Text", StringHelper.removeNumbering(" 1 Text"));
        assertEquals("Text", StringHelper.removeNumbering("1. Text"));
        assertEquals("Text", StringHelper.removeNumbering("1.      Text"));
        assertEquals("Text", StringHelper.removeNumbering("1.2.3.Text"));
        assertEquals("Text", StringHelper.removeNumbering("1.2.3.4     Text"));
    }

    public void testGetLongestCommonString() {
        assertEquals("abc", StringHelper.getLongestCommonString("abcd", "abcefg", false, false));
        assertEquals("abcdEf", StringHelper.getLongestCommonString("abcdEfE", "abcdEfefg", true, false));
        assertEquals("BCD", StringHelper.getLongestCommonString("ABCD", "BCDE", true, true));
        assertEquals("", StringHelper.getLongestCommonString("ABCD", "BCDE", false, false));

    }

    public void testReverseString() {
        assertEquals("fe dcBA", StringHelper.reverseString("ABcd ef"));
    }

    public void testCalculateCharNGrams() {
        assertEquals(16, StringHelper.calculateCharNGrams("allthelilacsinohio", 3).size());
        assertEquals(3, StringHelper.calculateCharNGrams("hiatt", 3).size());
        assertEquals(81, StringHelper.calculateAllCharNGrams("allthelilacsinohio", 3, 8).size());
        assertEquals(1, StringHelper.calculateCharNGrams("hiatt", 5).size());
        assertEquals(0, StringHelper.calculateCharNGrams("hiatt", 6).size());
    }

    public void testCalculateWordNgrams() {
        assertEquals(4, StringHelper.calculateWordNGrams("all the lilacs in ohio", 2).size());
        assertEquals(3, StringHelper.calculateWordNGrams("all the lilacs in ohio", 3).size());
        assertEquals(2, StringHelper.calculateWordNGrams("all the lilacs in ohio", 4).size());
        assertEquals(1, StringHelper.calculateWordNGrams("all the lilacs in ohio", 5).size());
        assertEquals(0, StringHelper.calculateWordNGrams("all the lilacs in ohio", 6).size());
    }

    public void testRename() {
        // System.out.println(FileHelper.rename(new
        // File("data/test/sampleTextForTagging.txt"),"sampleTextForTagging_tagged"));
        String renamedFile = FileHelper.rename(new File("data/test/sampleTextForTagging.txt"),
                "sampleTextForTagging_tagged");
        renamedFile = renamedFile.substring(renamedFile.lastIndexOf(File.separatorChar) + 1);
        assertEquals("sampleTextForTagging_tagged.txt", renamedFile);
    }

    public void testIsFileName() {
        assertEquals(true, FileHelper.isFileName(" website.html"));
        assertEquals(true, FileHelper.isFileName("test.ai "));
        assertEquals(false, FileHelper.isFileName(".just a sentence. "));
        assertEquals(false, FileHelper.isFileName("everything..."));
    }

    public void testWordToSingular() {
        assertEquals("elephant", StringHelper.wordToSingular("elephants"));
        assertEquals("city", StringHelper.wordToSingular("cities"));
        assertEquals("enemy", StringHelper.wordToSingular("enemies"));
        assertEquals("tray", StringHelper.wordToSingular("trays"));
        assertEquals("studio", StringHelper.wordToSingular("studios"));
        assertEquals("box", StringHelper.wordToSingular("boxes"));
        assertEquals("church", StringHelper.wordToSingular("churches"));
        assertEquals("fish", StringHelper.wordToSingular("fish"));
        assertEquals("lady", StringHelper.wordToSingular("ladies"));
        assertEquals("thief", StringHelper.wordToSingular("thieves"));
        assertEquals("wife", StringHelper.wordToSingular("wives"));
        assertEquals("shelf", StringHelper.wordToSingular("shelves"));
        assertEquals("tomato", StringHelper.wordToSingular("tomatoes"));
        assertEquals("hero", StringHelper.wordToSingular("heroes"));
        assertEquals("piano", StringHelper.wordToSingular("pianos"));
        assertEquals("article", StringHelper.wordToSingular("articles"));
        assertEquals("kiss", StringHelper.wordToSingular("kisses"));
        assertEquals("dish", StringHelper.wordToSingular("dishes"));
        assertEquals("phase", StringHelper.wordToSingular("phases"));
        assertEquals("vertex", StringHelper.wordToSingular("vertices"));
        assertEquals("index", StringHelper.wordToSingular("indices"));
        assertEquals("matrix", StringHelper.wordToSingular("matrices"));
        assertEquals("movie", StringHelper.wordToSingular("movies"));
        assertEquals("status", StringHelper.wordToSingular("status"));
        
        // this transformation makes no sense, but it caused a StringIndexOutOfBoundsException which I fixed.
        assertEquals("yf", StringHelper.wordToSingular("yves"));        
    }

    public void testWordToPlural() {
        assertEquals(StringHelper.wordToPlural("elephant"), "elephants");
        assertEquals(StringHelper.wordToPlural("city"), "cities");
        assertEquals(StringHelper.wordToPlural("enemy"), "enemies");
        assertEquals(StringHelper.wordToPlural("tray"), "trays");
        assertEquals(StringHelper.wordToPlural("studio"), "studios");
        assertEquals(StringHelper.wordToPlural("box"), "boxes");
        assertEquals(StringHelper.wordToPlural("church"), "churches");
        assertEquals("vertices", StringHelper.wordToPlural("vertex"));
        assertEquals("movies", StringHelper.wordToPlural("movie"));
        assertEquals("status", StringHelper.wordToPlural("status"));

        // http://www.esldesk.com/vocabulary/irregular-nouns
        assertEquals("addenda", StringHelper.wordToPlural("addendum"));
        assertEquals("algae", StringHelper.wordToPlural("alga"));
        assertEquals("alumnae", StringHelper.wordToPlural("alumna"));
        assertEquals("alumni", StringHelper.wordToPlural("alumnus"));
        assertEquals("analyses", StringHelper.wordToPlural("analysis"));
        assertEquals("antenna", StringHelper.wordToPlural("antennas"));
        assertEquals("apparatuses", StringHelper.wordToPlural("apparatus"));
        assertEquals("appendices", StringHelper.wordToPlural("appendix"));
        assertEquals("axes", StringHelper.wordToPlural("axis"));
        assertEquals("bacilli", StringHelper.wordToPlural("bacillus"));
        assertEquals("bacteria", StringHelper.wordToPlural("bacterium"));
        assertEquals("bases", StringHelper.wordToPlural("basis"));
        assertEquals("beaux", StringHelper.wordToPlural("beau"));
        assertEquals("bison", StringHelper.wordToPlural("bison"));
        assertEquals("buffalos", StringHelper.wordToPlural("buffalo"));
        assertEquals("bureaus", StringHelper.wordToPlural("bureau"));
        assertEquals("buses", StringHelper.wordToPlural("bus"));
        assertEquals("cactuses", StringHelper.wordToPlural("cactus"));
        assertEquals("calves", StringHelper.wordToPlural("calf"));
        assertEquals("children", StringHelper.wordToPlural("child"));
        assertEquals("corps", StringHelper.wordToPlural("corps"));
        assertEquals("corpuses", StringHelper.wordToPlural("corpus"));
        assertEquals("crises", StringHelper.wordToPlural("crisis"));
        assertEquals("criteria", StringHelper.wordToPlural("criterion"));
        assertEquals("curricula", StringHelper.wordToPlural("curriculum"));
        assertEquals("data", StringHelper.wordToPlural("datum"));
        assertEquals("deer", StringHelper.wordToPlural("deer"));
        assertEquals("dice", StringHelper.wordToPlural("die"));
        assertEquals("dwarfs", StringHelper.wordToPlural("dwarf"));
        assertEquals("diagnoses", StringHelper.wordToPlural("diagnosis"));
        assertEquals("echoes", StringHelper.wordToPlural("echo"));
        assertEquals("elves", StringHelper.wordToPlural("elf"));
        assertEquals("ellipses", StringHelper.wordToPlural("ellipsis"));
        assertEquals("embargoes", StringHelper.wordToPlural("embargo"));
        assertEquals("emphases", StringHelper.wordToPlural("emphasis"));
        assertEquals("errata", StringHelper.wordToPlural("erratum"));
        assertEquals("firemen", StringHelper.wordToPlural("fireman"));
        assertEquals("fish", StringHelper.wordToPlural("fish"));
        assertEquals("focuses", StringHelper.wordToPlural("focus"));
        assertEquals("feet", StringHelper.wordToPlural("foot"));
        assertEquals("formulas", StringHelper.wordToPlural("formula"));
        assertEquals("fungi", StringHelper.wordToPlural("fungus"));
        assertEquals("genera", StringHelper.wordToPlural("genus"));
        assertEquals("geese", StringHelper.wordToPlural("goose"));
        assertEquals("halves", StringHelper.wordToPlural("half"));
        assertEquals("heroes", StringHelper.wordToPlural("hero"));
        assertEquals("hippopotami", StringHelper.wordToPlural("hippopotamus"));
        assertEquals("hoofs", StringHelper.wordToPlural("hoof"));
        assertEquals("hypotheses", StringHelper.wordToPlural("hypothesis"));
        assertEquals("indices", StringHelper.wordToPlural("index"));
        assertEquals("knives", StringHelper.wordToPlural("knife"));
        assertEquals("leaves", StringHelper.wordToPlural("leaf"));
        assertEquals("lives", StringHelper.wordToPlural("life"));
        assertEquals("loaves", StringHelper.wordToPlural("loaf"));
        assertEquals("lice", StringHelper.wordToPlural("louse"));
        assertEquals("men", StringHelper.wordToPlural("man"));
        assertEquals("matrices", StringHelper.wordToPlural("matrix"));
        assertEquals("means", StringHelper.wordToPlural("means"));
        assertEquals("media", StringHelper.wordToPlural("medium"));
        assertEquals("memoranda", StringHelper.wordToPlural("memorandum"));
        assertEquals("milennia", StringHelper.wordToPlural("millennium"));
        assertEquals("moose", StringHelper.wordToPlural("moose"));
        assertEquals("mosquitoes", StringHelper.wordToPlural("mosquito"));
        assertEquals("mice", StringHelper.wordToPlural("mouse"));
        assertEquals("nebulas", StringHelper.wordToPlural("nebula"));
        assertEquals("neuroses", StringHelper.wordToPlural("neurosis"));
        assertEquals("nuclei", StringHelper.wordToPlural("nucleus"));
        assertEquals("oases", StringHelper.wordToPlural("oasis"));
        assertEquals("octopuses", StringHelper.wordToPlural("octopus"));
        assertEquals("ova", StringHelper.wordToPlural("ovum"));
        assertEquals("oxen", StringHelper.wordToPlural("ox"));
        assertEquals("paralyses", StringHelper.wordToPlural("paralysis"));
        assertEquals("parentheses", StringHelper.wordToPlural("parenthesis"));
        assertEquals("people", StringHelper.wordToPlural("person"));
        assertEquals("phenomena", StringHelper.wordToPlural("phenomenon"));
        assertEquals("potatoes", StringHelper.wordToPlural("potato"));
        assertEquals("radiuses", StringHelper.wordToPlural("radius"));
        assertEquals("scarfs", StringHelper.wordToPlural("scarf"));
        assertEquals("series", StringHelper.wordToPlural("series"));
        assertEquals("sheep", StringHelper.wordToPlural("sheep"));
        assertEquals("shelves", StringHelper.wordToPlural("shelf"));
        assertEquals("scissors", StringHelper.wordToPlural("scissors"));
        assertEquals("species", StringHelper.wordToPlural("species"));
        assertEquals("stimuli", StringHelper.wordToPlural("stimulus"));
        assertEquals("strata", StringHelper.wordToPlural("stratum"));
        assertEquals("syllabuses", StringHelper.wordToPlural("syllabus"));
        assertEquals("symposia", StringHelper.wordToPlural("symposium"));
        assertEquals("syntheses", StringHelper.wordToPlural("synthesis"));
        assertEquals("synopses", StringHelper.wordToPlural("synopsis"));
        assertEquals("tableaux", StringHelper.wordToPlural("tableau"));
        assertEquals("theses", StringHelper.wordToPlural("thesis"));
        assertEquals("thieves", StringHelper.wordToPlural("thief"));
        assertEquals("tomatoes", StringHelper.wordToPlural("tomato"));
        assertEquals("teeth", StringHelper.wordToPlural("tooth"));
        assertEquals("torpedoes", StringHelper.wordToPlural("torpedo"));
        assertEquals("vertebrae", StringHelper.wordToPlural("vertebra"));
        assertEquals("vetoes", StringHelper.wordToPlural("veto"));
        assertEquals("vitae", StringHelper.wordToPlural("vita"));
        assertEquals("watches", StringHelper.wordToPlural("watch"));
        assertEquals("wives", StringHelper.wordToPlural("wife"));
        assertEquals("wolves", StringHelper.wordToPlural("wolf"));
        assertEquals("women", StringHelper.wordToPlural("woman"));
    }

    public void testContainsNumber() {
        assertEquals(true, StringHelper.containsNumber("120"));
        assertEquals(true, StringHelper.containsNumber("120.2 GB"));
        assertEquals(false, StringHelper.containsNumber("A bc de2f GB"));
        assertEquals(false, StringHelper.containsNumber("A-1 GB"));
    }

    public void testRemoveStopWords() {
        assertEquals("...neighborhoodthe ofrocking.", StringHelper
                .removeStopWords("...The neighborhoodthe is ofrocking of."));
        assertEquals("neighborhood; REALLY; rocking!", StringHelper
                .removeStopWords("The neighborhood is; IS REALLY; rocking of!"));
    }

    public void testGetSentence() {
        // System.out.println(StringHelper.getSentence("...now. Although, have 234 ft.lbs. of torque ... many of them (30.2%) are good. As long as",
        // 40));
        assertEquals(StringHelper.getPhraseToEndOfSentence("Although, many of them (30.2%) are good. As long as"),
                "Although, many of them (30.2%) are good.");
        assertEquals(StringHelper.getPhraseFromBeginningOfSentence("...now. Although, many of them (30.2%) are good"),
                "Although, many of them (30.2%) are good");
        assertEquals(StringHelper.getSentence(
                "...now. Although, have 234 ft.lbs. of torque ... many of them (30.2%) are good. As long as", 10),
                "Although, have 234 ft.lbs. of torque ... many of them (30.2%) are good.");
        assertEquals(StringHelper.getSentence(
                "...now. Although, have 234 ft.lbs. of torque ... many of them (30.2%) are good. As long as", 40),
                "Although, have 234 ft.lbs. of torque ... many of them (30.2%) are good.");
        assertEquals(StringHelper.getSentence("...now. Although, many of them (30.2%) are good. As long as", 10),
                "Although, many of them (30.2%) are good.");
        assertEquals(StringHelper.getSentence("...now. Although, many of them (30.2%) are good. As long as", 40),
                "Although, many of them (30.2%) are good.");
        assertEquals(StringHelper.getSentence("...now. Although, many of them (30.2%) are good.As long as", 40),
                "Although, many of them (30.2%) are good.");
        assertEquals(StringHelper.getSentence(
                "What is the largest city in usa, (30.2%) in population. - Yahoo! Answers,", 12),
                "What is the largest city in usa, (30.2%) in population. - Yahoo!");
        assertEquals(StringHelper.getSentence(
                "What is the largest city in usa, (30.2%) in population? - Yahoo! Answers,", 12),
                "What is the largest city in usa, (30.2%) in population?");
        assertEquals(StringHelper.getSentence(
                "...now. Although, has 234,423,234 sq.miles area many of them (30.2%) are good. As long as", 10),
                "Although, has 234,423,234 sq.miles area many of them (30.2%) are good.");
    }

    public void testGetSentences() {

        // this is the LingPipe example (last sentence ends with "!" to make it more difficult:
        // http://alias-i.com/lingpipe/demos/tutorial/sentences/read-me.html
        String inputText = "The induction of immediate-early (IE) response genes, such as egr-1, c-fos, and c-jun, occurs rapidly after the activation of T lymphocytes. The process of activation involves calcium mobilization, activation of protein kinase C (PKC), and phosphorylation of tyrosine kinases. p21(ras), a guanine nucleotide binding factor, mediates T-cell signal transduction through PKC-dependent and PKC-independent pathways. The involvement of p21(ras) in the regulation of calcium-dependent signals has been suggested through analysis of its role in the activation of NF-AT. We have investigated the inductions of the IE genes in response to calcium signals in Jurkat cells (in the presence of activated p21(ras)) and their correlated consequences!";
        List<String> sentences = StringHelper.getSentences(inputText);

        // System.out.println(DigestUtils.md5Hex("text"));

        assertEquals(5, sentences.size());
        assertEquals(
                "The induction of immediate-early (IE) response genes, such as egr-1, c-fos, and c-jun, occurs rapidly after the activation of T lymphocytes.",
                sentences.get(0));
        assertEquals(
                "We have investigated the inductions of the IE genes in response to calcium signals in Jurkat cells (in the presence of activated p21(ras)) and their correlated consequences!",
                sentences.get(4));

        inputText = "This Paragraph is more difficult...or isn't it?hm, well (!), I don't know!!! I really don't.";
        sentences = StringHelper.getSentences(inputText);
        // CollectionHelper.print(sentences);

        assertEquals(3, sentences.size());
        assertEquals("This Paragraph is more difficult...or isn't it?", sentences.get(0));
        assertEquals("hm, well (!), I don't know!!!", sentences.get(1));
        assertEquals("I really don't.", sentences.get(2));
        // CollectionHelper.print(sentences);
    }

    public void testTrim() {
        // System.out.println(StringHelper.trim("'80GB'))"));
        assertEquals("", StringHelper.trim(","));
        assertEquals("", StringHelper.trim(""));
        assertEquals("", StringHelper.trim(". ,"));
        assertEquals("asd", StringHelper.trim(" ; asd ?-"));
        assertEquals("27 30 N, 90 30 E", StringHelper.trim("; ,.  27 30 N, 90 30 E -"));
        assertEquals("27 30 N, 90 30 E", StringHelper.trim(",.  27 30 N, 90 30 E  ##"));
        assertEquals("' 2''", StringHelper.trim("' 2''"));
        assertEquals("' 2\"", StringHelper.trim("' 2\""));
        assertEquals("abc", StringHelper.trim("\"abc\""));
        assertEquals("abc\"def", StringHelper.trim("\"abc\"def\""));
        assertEquals("\"abc", StringHelper.trim("\"abc"));
        // TODO? assertEquals(StringHelper.trim("'80GB'))"),"80GB");
        // assertEquals(StringHelper.trim("2\""),"2\"");
    }

    public void testLFEColonPattern() {

        assertEquals("Volume: 96 cc", StringHelper.concatMatchedString("Volume: 96 cc", "||",
                RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("Volume: 96", StringHelper.concatMatchedString("Volume: 96", "||",
                RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("Volume: 96 cc||Weight: 128 g", StringHelper.concatMatchedString("Volume: 96 ccWeight: 128 g",
                "||", RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("Volume : 96 cc||Weight : 128 g", StringHelper.concatMatchedString("Volume : 96 ccWeight : 128 g",
                "||", RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("Volume/V: 96 cc||Weight: 128 g", StringHelper.concatMatchedString("Volume/V: 96 ccWeight: 128 g",
                "||", RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("Volume:96 cc||Weight: 128 g", StringHelper.concatMatchedString("Volume:96 ccWeight: 128 g", "||",
                RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("Volume: 96 cc||Net Weight: 128 g", StringHelper.concatMatchedString(
                "Volume: 96 ccNet Weight: 128 g", "||", RegExp.COLON_FACT_REPRESENTATION));
        // assertEquals("Volume: 96 cc||Weight: 128 g",
        // StringHelper.concatMatchedString("Volume: 96 cc,Weight: 128 g","||",RegExp.COLON_FACT_REPRESENTATION));
        // assertEquals("Volume: 96 cc||Net weight: 128 g",
        // StringHelper.concatMatchedString("Volume: 96 cc,Net weight: 128 g","||",RegExp.COLON_FACT_REPRESENTATION));
        // assertEquals("Volume/V: 96 cc||Weight/W: 128 g",
        // StringHelper.concatMatchedString("Volume/V: 96 cc,Weight/W: 128 g","||",RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("V8: yes, 800kb", StringHelper.concatMatchedString("V8: yes, 800kb", "||",
                RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("V8: yes, 800kb", StringHelper.concatMatchedString("V8: yes, 800kbDimensions", "||",
                RegExp.COLON_FACT_REPRESENTATION));
        assertEquals("Weight: 800, 600", StringHelper.concatMatchedString("Weight: 800, 600Dimensions", "||",
                RegExp.COLON_FACT_REPRESENTATION));
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

    public void testCountTags() {
        assertEquals(4, StringHelper.countTags("everybody is <b>here</b> to do some <p>work</p>"));
        assertEquals(4, StringHelper.countTags("<br />everybody is <b>here</b> to do some <p>work"));
        assertEquals(4, StringHelper.countTags("<br />everybody<br /> is <b>here</b> to do some <p>work", true));
        assertEquals(7, StringHelper.countTags("<br /><a>abc</a>everybody<br /> is <b>here</b> to do some <p>work"));
        assertEquals(6, StringHelper.countTags(
                "<br /><a>abc</a>everybody<br /> is <b>here</b> to do some <a>abc</a> <p>work", true));
    }

    public void testEscapeForRegularExpression() {
        // String containing RegEx meta characters which need to be escaped
        String s = "(the) [quick] {brown} fox$ ^jumps+ \n ov|er the? l-a\\zy ]dog[";
        // test successful escape by matching escaped RegEx ...
        assertTrue(s.matches(StringHelper.escapeForRegularExpression(s)));
    }

    public void testRemoveHTMLTags() {
        String htmlContent = "<html lang=\"en-us\"> <script language=\"JavaScript\" type=\"text/javascript\">var MKTCOUNTRY = \"USA\"</script>this is relevant <!-- function open_doc (docHref) {document.location.href = '/sennheiser/home_de.nsf/' + docHref;}--> </html>";
        assertEquals("this is relevant", StringHelper.removeHTMLTags(htmlContent, true, true, true, false));

    }

    public void testGetSubstringBetween() {
        assertEquals("the lilacs", StringHelper.getSubstringBetween("all the lilacs in ohio", "all ", " in ohio"));
        assertEquals("", StringHelper.getSubstringBetween("all the lilacs in ohio", "allt ", "in ohio"));
    }
    
    public void testCamelCaseToWords() {
        assertEquals("", StringHelper.camelCaseToWords(""));
        assertEquals("camel Case String", StringHelper.camelCaseToWords("camelCaseString"));
        assertEquals("camel.case.string", StringHelper.camelCaseToWords("camel.case.string"));
        assertEquals("camel_Case_String", StringHelper.camelCaseToWords("camelCaseString", "_"));
    }

}
