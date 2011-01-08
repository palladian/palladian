package tud.iir.web.datasetcrawler.language;

import java.io.IOException;

import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.ThreadHelper;
import tud.iir.web.Crawler;

/**
 * Use the XMPP API of Collecta to monitor microblogging messages in 35 languages.
 * 
 * @author David Urbansky
 * 
 */
public class MicroBloggingLanguageDatasetCompiler extends LanguageDatasetCompiler {

    public MicroBloggingLanguageDatasetCompiler() {

        // language codes from Collecta (http://support.collecta.com/kb/guide/language)
        languages.put("Arabic", "ar");
        languages.put("Bulgarian", "bg");
        languages.put("Chinese", "zh");
        languages.put("Croation", "hr");
        languages.put("Czech", "cs");
        languages.put("Danish", "da");
        languages.put("Dutch", "nl");
        languages.put("English", "en");
        languages.put("Estonian", "et");
        languages.put("Farsi", "fa");
        languages.put("Finnish", "fi");
        languages.put("French", "fr");
        languages.put("German", "de");
        languages.put("Greek", "el");
        languages.put("Hebrew", "he");
        languages.put("Hindi", "hi");
        languages.put("Hungarian", "hu");
        languages.put("Icelandic", "is");
        languages.put("Ido", "io");
        languages.put("Irish", "ga");
        languages.put("Italian", "it");
        languages.put("Japanese", "ja");
        languages.put("Korean", "ko");
        languages.put("Norwegian", "no");
        languages.put("Polish", "pl");
        languages.put("Portuguese", "pt");
        languages.put("Romanian", "ro");
        languages.put("Russian", "ru");
        languages.put("Slovenian", "sl");
        languages.put("Spanish", "es");
        languages.put("Swedish", "sv");
        languages.put("Thai", "th");
        languages.put("Ukrainian", "uk");
        languages.put("Vietnamese", "vi");
        languages.put("Western_Frisian", "fy");

        createDirectoryStructure();
    }

    @Override
    public void compileDataset(int pagesPerLanguage) {

        StopWatch sw = new StopWatch();

        // assume to get 6 pages per minute
        int minutes = (int) (pagesPerLanguage / 6.0);

        StringBuilder dataSetIndex = new StringBuilder();

        for (String languageCode : languages.values()) {

            dataSetIndex.append(languageCode).append("/messages.txt").append(languageCode).append("\n");

            LOGGER.info("listening for messages for language " + languageCode + " for " + minutes + " minutes");

            CollectaClient cc = new CollectaClient(languageCode, DIRECTORY_PATH);
            Thread t = new Thread(cc);
            t.start();

            // collect data for language for x minutes
            ThreadHelper.sleep(minutes * DateHelper.MINUTE_MS);

            try {
                cc.fileWriter.close();
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }

            t.interrupt();
        }

        FileHelper.writeToFile(DIRECTORY_PATH + "languageDocumentIndex.txt", dataSetIndex);
        FileHelper.removeDuplicateLines(DIRECTORY_PATH + "languageDocumentIndex.txt", DIRECTORY_PATH
                + "languageDocumentIndex.txt");

        cleanDataset();

        LOGGER.info("language dataset compiled in " + sw.getElapsedTimeString() + ", tried to download "
                + languages.size() * pagesPerLanguage + " files (" + Crawler.getSessionDownloadSize(Crawler.MEGA_BYTES)
                + "MB)");

    }

    private void cleanDataset() {
        for (String languageCode : languages.values()) {

            String filePath = DIRECTORY_PATH + languageCode + "/messages.txt";
            FileHelper.removeDuplicateLines(filePath, filePath);

            String text = FileHelper.readFileToString(filePath);
            text = HTMLHelper.removeHTMLTags(text, true, true, true, false);
            FileHelper.writeToFile(filePath, text);
        }
    }

    public static void main(String[] args) {
        MicroBloggingLanguageDatasetCompiler dc = new MicroBloggingLanguageDatasetCompiler();
        dc.cleanDataset();
    }
}
