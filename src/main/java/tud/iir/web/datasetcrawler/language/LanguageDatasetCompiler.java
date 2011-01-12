package tud.iir.web.datasetcrawler.language;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import tud.iir.classification.language.evaluation.LanguageDetectionEvaluation;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StringHelper;

/**
 * This class compiles a training set of web pages with certain languages. This training set can then be used to learn a
 * language classifier.
 * 
 * @author David Urbansky
 * 
 */
public abstract class LanguageDatasetCompiler {

    /** The logger for this class. */
    static final Logger LOGGER = Logger.getLogger(LanguageDatasetCompiler.class);

    /** List of languages and the Google code to get results in those languages. */
    Map<String, String> languages = new HashMap<String, String>();

    /** Path where the files should be saved. */
    static final String DIRECTORY_PATH = "data/datasets/classification/language/";

    public LanguageDatasetCompiler() {
        // all available languages from Google web search
        /*
         * languages.put("Arabic", "lang_ar"); languages.put("Bulgarian", "lang_bg"); languages.put("Catalan",
         * "lang_ca"); languages.put("Chinese (Simplified)",
         * "lang_zh-CN"); languages.put("Chinese (Traditional)", "lang_zh-TW"); languages.put("Croation", "lang_hr");
         * languages.put("Czech", "lang_cs");
         * languages.put("Danish", "lang_da"); languages.put("Dutch", "lang_nl"); languages.put("English", "lang_en");
         * languages.put("Estonian", "lang_et");
         * languages.put("Finnish", "lang_fi"); languages.put("French", "lang_fr"); languages.put("German", "lang_de");
         * languages.put("Greek", "lang_el");
         * languages.put("Hebrew", "lang_iw"); languages.put("Hungarian", "lang_hu"); languages.put("Icelandic",
         * "lang_is"); languages.put("Indonesian",
         * "lang_id"); languages.put("Italian", "lang_it"); languages.put("Japanese", "lang_ja");
         * languages.put("Korean", "lang_ko"); languages.put("Latvian",
         * "lang_lv"); languages.put("Lithuanian", "lang_lt"); languages.put("Norwegian", "lang_no");
         * languages.put("Polish", "lang_pl");
         * languages.put("Portuguese", "lang_pt"); languages.put("Romanian", "lang_ro"); languages.put("Russian",
         * "lang_ru"); languages.put("Serbian",
         * "lang_sr"); languages.put("Slovak", "lang_sk"); languages.put("Slovenian", "lang_sl");
         * languages.put("Spanish", "lang_es"); languages.put("Swedish",
         * "lang_sv"); languages.put("Turkish", "lang_tr");
         */

    }

    void createDirectoryStructure() {
        // // create the directory structure at the target path
        new File(DIRECTORY_PATH).mkdirs();
        for (Entry<String, String> entry : languages.entrySet()) {
            new File(DIRECTORY_PATH + entry.getValue()).mkdir();
        }
    }

    /**
     * Compiles a dataset for learning a classifier.<br>
     * 
     * @param pagesPerLanguage Number of pages per language.
     */
    public abstract void compileDataset(int pagesPerLanguage);

    /**
     * Create a file with random sentences and the language code for evaluating language detectors in
     * {@link LanguageDetectionEvaluation}.
     */
    public void createRandomTestFile(double percentPerFile, Set<String> includeLanguages) {
        Set<Integer> sentenceHashes = new HashSet<Integer>();

        Pattern pattern1 = Pattern.compile("^\\p{L}.*", Pattern.MULTILINE);
        Pattern pattern2 = Pattern.compile("^\\d.*", Pattern.MULTILINE);

        FileWriter testFile;
        try {
            testFile = new FileWriter(DIRECTORY_PATH + "randomTestFile.txt");

            for (Entry<String, String> entry : languages.entrySet()) {

                if (!includeLanguages.contains(entry.getValue())) {
                    continue;
                }

                for (int i = 0; i < 100; i++) {
                    String filePath = DIRECTORY_PATH + entry.getValue() + "/" + i + ".txt";
                    List<String> sentences = FileHelper.readFileToArray(filePath);

                    for (String sentence : sentences) {

                        sentence = StringHelper.removeBrackets(sentence);
                        sentence = StringHelper.removeNumbering(sentence);
                        sentence = StringHelper.trim(sentence);

                        if (sentence.length() < 7 || sentence.indexOf(" ") == -1) {
                            continue;
                        }

                        if (!sentenceHashes.add(sentence.hashCode())) {
                            continue;
                        }

                        Matcher matcher1 = pattern1.matcher(sentence);
                        Matcher matcher2 = pattern2.matcher(sentence);
                        if (matcher2.matches() || !matcher1.matches()) {
                            continue;
                        }

                        if (Math.random() < percentPerFile) {
                            testFile.write(sentence);
                            testFile.write("###");
                            testFile.write(entry.getValue());
                            testFile.write("\n");
                        }
                    }
                }

            }

            testFile.flush();
            testFile.close();

        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        // String m =
        // "T&#244;i th&#7853;t ngu khi &#273;&#227; c&#7889;ng hi&#7871;n nhi&#7873;u s&#7913;c l&#7921;c v&#224; ch&#7845;t x&#225;m cho b&#7885;n cs to&#224;n tr&#7883; h&#432;&#7903;ng th&#7909;. Nay t&#244;i xin n&#234;u l&#234;n &#253; ki&#7871;n mong m&#7885;i ng&#432;&#7901;i d&#226;n VN h&#227;y d&#7915;ng ngay nh&#7919;ng c&#7889;ng hi&#7871;n c&#7911;a m&#236;nh cho ch&#7871; &#273;&#7897; cs d&#227; man &#273;ang to&#224;n tr&#7883; d&#226;n t&#7897;c VN. H&#227;y &#273;&#7875; b&#7885;n ch&#250;ng tho&#225;i ho&#225; v&#224; bi&#7871;n kh&#7887;i VN. ";
        // System.out.println(m);
        // System.out.println(StringEscapeUtils.unescapeHtml(m));
        // System.exit(0);

        // LanguageDatasetCompiler ldc = new WikipediaLanguageDatasetCompiler();
        LanguageDatasetCompiler ldc = new MicroBloggingLanguageDatasetCompiler();
        ldc.compileDataset(50);
        System.exit(0);

        Set<String> includeLanguages = new HashSet<String>();
        includeLanguages.add("da");
        includeLanguages.add("de");
        includeLanguages.add("el");
        includeLanguages.add("en");
        includeLanguages.add("es");
        includeLanguages.add("fi");
        includeLanguages.add("fr");
        includeLanguages.add("it");
        includeLanguages.add("nl");
        includeLanguages.add("pt");
        includeLanguages.add("sv");
        ldc.createRandomTestFile(0.05, includeLanguages);
    }

}