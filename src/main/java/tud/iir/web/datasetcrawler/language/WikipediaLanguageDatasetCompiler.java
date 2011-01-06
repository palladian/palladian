package tud.iir.web.datasetcrawler.language;

import java.util.Map.Entry;
import java.util.Set;

import org.w3c.dom.Document;

import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.extraction.content.PageContentExtractorException;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.web.Crawler;
import tud.iir.web.URLDownloader;

/**
 * Query Wikipedia's random article page to compile a dataset of 76 languages.
 * 
 * @author David Urbansky
 * 
 */
public class WikipediaLanguageDatasetCompiler extends LanguageDatasetCompiler {

    public WikipediaLanguageDatasetCompiler() {

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

        createDirectoryStructure();
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

                String filePath = entry.getValue() + "/" + counter + ".txt";
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
}
