package ws.palladian.retrieval.feeds.evaluation.encodingFix;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * Test for EncodingFixer2 with Sandro's gold standard.
 * 
 * @author Philipp Katz
 * 
 */
public class EncodingFixer2Test {

    private static final String ENCODING_BUG_FILE_EXTENSION = ".encodingBug.csv";
    private static final String GOLD_STANDARD_FILE_EXTENSION = ".goldstandard.csv";

    /*
     * Comment on 2_http___00-oth-man-00_skyrock_c:
     * Feed-Anbieter hatte scheinbar 2 Probleme:
     * manchmal wechselnde SessionIDs, die sich im Pfad hinter /f/ verbergen,
     * scheinbar hat der Anbieter selbst (das Encoding) hin und wieder verhauen, denn die Zeile
     * "1287142740000;"Article post? le vendredi 15 octobre 2010 07:35";"
     * kommt in 3 Versionen vor, bei denen sich auch noch der in den Titel integeriete Zeitstempel ändert, 4:35....
     * ...so the gold standard is not perfect but the best we can do so far
     */
    private static final String[] TESTS = {
            "/feedDataset/EncodingBug_Goldstandard/113_http___0_tqn_com_6_g_hepatitis",
            "/feedDataset/EncodingBug_Goldstandard/115_http___0_tqn_com_6_g_ent_b_rss",
            "/feedDataset/EncodingBug_Goldstandard/2_http___00-oth-man-00_skyrock_c",
            "/feedDataset/EncodingBug_Goldstandard/25_http___0misspumpkin_blogspot_c",
            "/feedDataset/EncodingBug_Goldstandard/27_http___0taku_planet_ee_feed_at",
            "/feedDataset/EncodingBug_Goldstandard/28_http___0tattakata0_blog28_fc2_",
            "/feedDataset/EncodingBug_Goldstandard/41_http___0_tqn_com_6_g_ancienthi",
            "/feedDataset/EncodingBug_Goldstandard/62_http___0_tqn_com_6_g_patients_",
            "/feedDataset/EncodingBug_Goldstandard/63_http___0_tqn_com_6_g_babyparen",
            "/feedDataset/EncodingBug_Goldstandard/70_http___0_tqn_com_6_g_proicehoc",
            "/feedDataset/EncodingBug_Goldstandard/78_http___0_tqn_com_6_g_jewelryma",
            "/feedDataset/EncodingBug_Goldstandard/83_http___0_tqn_com_6_g_useconomy",
            "/feedDataset/EncodingBug_Goldstandard/86040_http___orlando-hiphop_forumoti",
            "/feedDataset/EncodingBug_Goldstandard/90_http___0_tqn_com_6_g_golftrave",
            "/feedDataset/EncodingBug_Goldstandard/92_http___0_tqn_com_6_g_journalis",
            "/feedDataset/EncodingBug_Goldstandard/889_http___44_agendaculturel_fr_rs",
            "/feedDataset/EncodingBug_Goldstandard/142924_d3p_co_jp_rss_mobile_rdf" };

    @Test
    public void testEncodingFix2() {

        for (String test : TESTS) {

            Logger.getRootLogger().info("testing " + test);

            EncodingFixer2 fixer = new EncodingFixer2();
            String testCsvFile = getFile(test + GOLD_STANDARD_FILE_EXTENSION);
            String bugCsvFile = getFile(test + ENCODING_BUG_FILE_EXTENSION);
            List<String> gold = EncodingFixer2.readCsv(testCsvFile);
            List<String> bug = EncodingFixer2.readCsv(bugCsvFile);
            List<String> result = fixer.deduplicate(bug, bugCsvFile);

            // make sure, that the test files were actually read
            // this would not be necessary, if we had adequate exceptions ... but ... well :)
            assertFalse("gold is empty : " + test, gold.isEmpty());
            assertFalse("bug is empty : " + test, bug.isEmpty());

            // the acutal test comparing the expected result from the gold standard
            // with the result from the deduplication algorithm
            assertTrue("result different from gold standard " + test, gold.equals(result));

        }

    }

    @Test
    public void testIsDuplicate() {
        assertTrue(EncodingFixer2
                .isDuplicate("?Feliz Pascua de Resurrecci?n!", "¡Feliz Pascua de Resurrección!"));
        assertTrue(EncodingFixer2.isDuplicate("æü¶ü•hkˆ‹ªê¸Êë­ãé¯¿ü ¬Áãª nputSemicolonHereÏ’l‹",
                "?????hk????????????? ???? nputSemicolonHere??l?"));
    }

    private static String getFile(String filePath) {
        return EncodingFixer2Test.class.getResource(filePath).getFile();
    }

}
