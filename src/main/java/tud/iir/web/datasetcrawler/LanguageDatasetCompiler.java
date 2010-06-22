package tud.iir.web.datasetcrawler;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.web.Crawler;

/**
 * This class compiles a training set of web pages with certain languages. This training set can then be used to learn a language classifier.
 * 
 * @author David Urbansky
 * 
 */
public class LanguageDatasetCompiler {

    /** the logger for this class */
    private static final Logger logger = Logger.getLogger(LanguageDatasetCompiler.class);

    /** list of languages and the Google code to get results in those languages */
    private HashMap<String, String> languages = new HashMap<String, String>();

    /** path where the files should be saved */
    private final String DIRECTORY_PATH = "data/benchmarkSelection/language/";

    public LanguageDatasetCompiler() {
        // all available languages from Google web search
        /*
         * languages.put("Arabic", "lang_ar"); languages.put("Bulgarian", "lang_bg"); languages.put("Catalan", "lang_ca"); languages.put("Chinese (Simplified)",
         * "lang_zh-CN"); languages.put("Chinese (Traditional)", "lang_zh-TW"); languages.put("Croation", "lang_hr"); languages.put("Czech", "lang_cs");
         * languages.put("Danish", "lang_da"); languages.put("Dutch", "lang_nl"); languages.put("English", "lang_en"); languages.put("Estonian", "lang_et");
         * languages.put("Finnish", "lang_fi"); languages.put("French", "lang_fr"); languages.put("German", "lang_de"); languages.put("Greek", "lang_el");
         * languages.put("Hebrew", "lang_iw"); languages.put("Hungarian", "lang_hu"); languages.put("Icelandic", "lang_is"); languages.put("Indonesian",
         * "lang_id"); languages.put("Italian", "lang_it"); languages.put("Japanese", "lang_ja"); languages.put("Korean", "lang_ko"); languages.put("Latvian",
         * "lang_lv"); languages.put("Lithuanian", "lang_lt"); languages.put("Norwegian", "lang_no"); languages.put("Polish", "lang_pl");
         * languages.put("Portuguese", "lang_pt"); languages.put("Romanian", "lang_ro"); languages.put("Russian", "lang_ru"); languages.put("Serbian",
         * "lang_sr"); languages.put("Slovak", "lang_sk"); languages.put("Slovenian", "lang_sl"); languages.put("Spanish", "lang_es"); languages.put("Swedish",
         * "lang_sv"); languages.put("Turkish", "lang_tr");
         */

        // language codes from Wikipedia (http://wikipedia.org/) with more than 10,000 articles (left some out)
        languages.put("Afrikaans", "ar");
        languages.put("Albanian", "sq");
        languages.put("Arabic", "ar");
        languages.put("Aragonese", "an");
        languages.put("Azerbaijani", "az");
        languages.put("Basque", "eu");
        languages.put("Belarusian", "be");
        languages.put("Bengali", "bn");
        languages.put("Bosnian", "bs");
        languages.put("Breton", "br");
        languages.put("Bulgarian", "bg");
        languages.put("Catalan", "ca");
        languages.put("Chinese", "zh");
        languages.put("Chuvash", "cv");
        languages.put("Croation", "hr");
        languages.put("Czech", "cs");
        languages.put("Danish", "da");
        languages.put("Dutch", "nl");
        languages.put("English", "en");
        languages.put("Esperanto", "eo");
        languages.put("Estonian", "et");
        languages.put("Finnish", "fi");
        languages.put("French", "fr");
        languages.put("Galician", "gl");
        languages.put("German", "de");
        languages.put("Greek", "el");
        languages.put("Gujarati", "gu");
        languages.put("Haitian", "ht");
        languages.put("Hebrew", "he");
        languages.put("Hindi", "hi");
        languages.put("Hungarian", "hu");
        languages.put("Icelandic", "is");
        languages.put("Ido", "io");
        languages.put("Indonesian", "id");
        languages.put("Irish", "ga");
        languages.put("Italian", "it");
        languages.put("Japanese", "ja");
        languages.put("Javanese", "jv");
        languages.put("Kartuli", "ka");
        languages.put("Korean", "ko");
        languages.put("Kurdish", "ku");
        languages.put("Latin", "la");
        languages.put("Latvian", "lv");
        languages.put("Lithuanian", "lt");
        languages.put("Luxembourgish", "lb");
        languages.put("Macedonian", "mk");
        languages.put("Malay", "ms");
        languages.put("Malayalam", "ml");
        languages.put("Marathi", "mr");
        languages.put("Nepali", "ne");
        languages.put("Norwegian", "no");
        languages.put("Occitan", "oc");
        languages.put("Persian", "fa");
        languages.put("Polish", "pl");
        languages.put("Portuguese", "pt");
        languages.put("Quechua", "qu");
        languages.put("Romanian", "ro");
        languages.put("Russian", "ru");
        languages.put("Serbian", "sr");
        languages.put("Slovak", "sk");
        languages.put("Slovenian", "sl");
        languages.put("Spanish", "es");
        languages.put("Sundanese", "su");
        languages.put("Swahili", "sw");
        languages.put("Swedish", "sv");
        languages.put("Tagalog", "tl");
        languages.put("Tamil", "ta");
        languages.put("Telugu", "te");
        languages.put("Thai", "th");
        languages.put("Turkish", "tr");
        languages.put("Ukrainian", "uk");
        languages.put("Urdu", "ur");
        languages.put("Vietnamese", "vi");
        languages.put("Volapuek", "vo");
        languages.put("Walloon", "wa");
        languages.put("Welsh", "cy");
        languages.put("Western_Frisian", "fy");

        // // create the directory structure at the target path
        new File(DIRECTORY_PATH).mkdirs();
        for (Entry<String, String> entry : languages.entrySet()) {
            new File(DIRECTORY_PATH + entry.getValue()).mkdir();
        }
    }

    /**
     * Compiles a dataset for learning a classifier. It processes the following steps: 1. Query Google for each language to obtain web pages in the given
     * language 2. Download x web pages and generate an entry in the dataset file. 3. Save the dataset file.
     * 
     * @param pagesPerLanguage Number of pages per language.
     */
    public void compileDataset(int pagesPerLanguage) {
        long t1 = System.currentTimeMillis();

        StringBuilder dataSetFile = new StringBuilder();

        Crawler c = new Crawler();

        for (Entry<String, String> entry : languages.entrySet()) {

            logger.info("### get pages for the language " + entry.getKey());

            int counter = 0;

            for (int i = 0; i < pagesPerLanguage; i++) {

                // download random wikipedia page in the given language
                String filePath = DIRECTORY_PATH + entry.getValue() + "/" + counter + ".html";
                if (c.downloadAndSave("http://" + entry.getValue() + ".wikipedia.org/wiki/Special:Random", filePath)) {
                    // write entry to dataset
                    dataSetFile.append(filePath).append(" ").append(entry.getKey()).append("\n");
                    counter++;
                }

            }

        }

        FileHelper.writeToFile(DIRECTORY_PATH + "languageDocumentIndex.txt", dataSetFile);

        logger.info("language dataset compiled in " + DateHelper.getRuntime(t1) + ", tried to download " + (languages.size() * pagesPerLanguage) + " files ("
                + c.getTotalDownloadSize(Crawler.MEGA_BYTES) + "MB)");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        LanguageDatasetCompiler ldc = new LanguageDatasetCompiler();
        ldc.compileDataset(100);

        // Crawler c = new Crawler();
        // c.downloadAndSave("data/benchmarkSelection/language/da/1.html", "abc.html");
    }

}
