package tud.iir.web.datasetcrawler;

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
import org.w3c.dom.Document;

import tud.iir.classification.language.evaluation.LanguageDetectionEvaluation;
import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.extraction.content.PageContentExtractorException;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StringHelper;
import tud.iir.web.Crawler;
import tud.iir.web.URLDownloader;

/**
 * This class compiles a training set of web pages with certain languages. This training set can then be used to learn a
 * language classifier.
 * 
 * @author David Urbansky
 * 
 */
public class LanguageDatasetCompiler {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(LanguageDatasetCompiler.class);

    /** List of languages and the Google code to get results in those languages. */
    private Map<String, String> languages = new HashMap<String, String>();

    /** Path where the files should be saved. */
    private static final String DIRECTORY_PATH = "data/datasets/classification/language/";

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
     * Compiles a dataset for learning a classifier. It processes the following steps:<br>
     * <ol>
     * <li>Query Wikipedia for each language to obtain web pages in the given language</li>
     * <li>Download x web pages and generate an entry in the dataset file</li>
     * <li>Save the dataset file</li>
     * </ol>
     * 
     * @param pagesPerLanguage Number of pages per language.
     */
    public void compileDataset(int pagesPerLanguage) {
        long t1 = System.currentTimeMillis();

        StringBuilder dataSetFile = new StringBuilder();

        PageContentExtractor pce = new PageContentExtractor();

        for (Entry<String, String> entry : languages.entrySet()) {

            LOGGER.info("### get pages for the language " + entry.getKey());

            int counter = 0;

            // download random wikipedia page in the given language
            URLDownloader urlDownloader = new URLDownloader();

            for (int j = 0; j < pagesPerLanguage; j++) {
                urlDownloader.add("http://" + entry.getValue() + ".wikipedia.org/wiki/Special:Random/"
                        + Math.round(Math.random() * 10000000000l));
            }

            Set<Document> downloadedDocuments = urlDownloader.start(null);

            LOGGER.info("downloaded " + downloadedDocuments.size()
                    + " documents, start extracting their textual contents now");

            for (Document document : downloadedDocuments) {

                if (document == null) {
                    continue;
                }

                String filePath = DIRECTORY_PATH + entry.getValue() + "/" + counter + ".txt";
                try {
                    pce.setDocument(document);
                    String text = pce.getResultText();
                    FileHelper.writeToFile(filePath, text);

                    // write entry to dataset
                    dataSetFile.append(filePath).append(" ").append(entry.getValue()).append("\n");
                    counter++;
                } catch (PageContentExtractorException e) {
                    LOGGER.error("could not get content from URL " + document.getDocumentURI());
                } catch (Exception e) {
                    LOGGER.error("could not get content from URL " + document.getDocumentURI());
                }
            }

        }

        FileHelper.writeToFile(DIRECTORY_PATH + "languageDocumentIndex.txt", dataSetFile);

        LOGGER.info("language dataset compiled in " + DateHelper.getRuntime(t1) + ", tried to download "
                + languages.size() * pagesPerLanguage + " files (" + Crawler.getSessionDownloadSize(Crawler.MEGA_BYTES)
                + "MB)");
    }

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
        LanguageDatasetCompiler ldc = new LanguageDatasetCompiler();
        ldc.compileDataset(100);
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