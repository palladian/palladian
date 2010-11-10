package tud.iir.classification.language.evaluation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import tud.iir.helper.CollectionHelper;
import tud.iir.helper.CountMap;
import tud.iir.helper.FileHelper;
import tud.iir.helper.LineAction;
import tud.iir.helper.StopWatch;
import tud.iir.helper.XPathHelper;
import tud.iir.web.Crawler;

/**
 * <p>
 * Parse the xml files from the <a href="http://wt.jrc.it/lt/Acquis/">JRC corpus</a> and save the body text in single
 * files. We can then further create training and testing sets with this class.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class JRCCorpusConverter {

    private static final Logger LOGGER = Logger.getLogger(JRCCorpusConverter.class);
    /**
     * <p>
     * Convert all xml files to text files containing only the body text.
     * </p>
     * <p>
     * The corpusRootFolderPath must point to a folder where each subfolder is named by the two digit language code and
     * has again subfolders which then contain the xml files.
     * </p>
     * 
     * @param corpusRootFolderPath The path to the root folder of the corpus files.
     * @param targetPath The path to the folder where the processed files should be saved to.
     */
    public void convertAllFiles(String corpusRootFolderPath, String targetPath) {

        StopWatch sw = new StopWatch();

        // create the path to the target folder
        if (!targetPath.endsWith("/")) {
            targetPath += "/";
        }
        new File(targetPath).mkdirs();

        // iterate over all languages
        File[] languageFolders = FileHelper.getFiles(corpusRootFolderPath);
        for (File languageFolder : languageFolders) {

            String currentLanguageCode = FileHelper.getFolderName(languageFolder.getPath());
            LOGGER.info("converting xml files from language: " + currentLanguageCode);

            int fileNumber = 1;

            // get all subfolders (ordered by year)
            File[] yearFolders = FileHelper.getFiles(languageFolder.getPath());
            for (File yearFolder : yearFolders) {

                LOGGER.info("converting xml files from language: " + currentLanguageCode + " and year "
                        + FileHelper.getFolderName(yearFolder.getPath()));

                // process each xml document in the folder
                File[] xmlFiles = FileHelper.getFiles(yearFolder.getPath());
                for (File xmlFile : xmlFiles) {

                    convertAndSave(xmlFile, targetPath + currentLanguageCode + "/", fileNumber + ".txt");
                    fileNumber++;

                }

            }

        }

        LOGGER.info("converted all files in " + sw.getElapsedTimeString());
    }

    private void convertAndSave(File xmlFile, String targetPath, String fileName) {

        new File(targetPath).mkdirs();

        Crawler crawler = new Crawler();
        Document document = crawler.getXMLDocument(xmlFile.getPath());

        List<Node> textNodes = XPathHelper.getNodes(document, "//text/body//div[@type='body']/p");

        StringBuilder textContent = new StringBuilder();
        for (Node node : textNodes) {
            textContent.append(node.getTextContent()).append("\n");
        }

        FileHelper.writeToFile(targetPath + "/" + fileName, textContent);

    }

    /**
     * Create an index of file location [space] language code for all languages.
     * 
     * @param corpusRootFolder The path to the root folder of the dataset.
     * @throws IOException
     */
    public void createIndex(String corpusRootFolder) throws IOException {
        createIndex(corpusRootFolder, null);
    }

    /**
     * Create an index of file location [space] language code for all languages specified in the array.
     * 
     * @param corpusRootFolderPath The path to the root folder of the dataset.
     * @param includeLanguages The language codes for languages that should be included in the index.
     * @throws IOException
     */
    public void createIndex(String corpusRootFolderPath, String[] includeLanguages) throws IOException {

        StopWatch sw = new StopWatch();

        if (!corpusRootFolderPath.endsWith("/")) {
            corpusRootFolderPath += "/";
        }
        String indexName = "index";
        if (includeLanguages == null) {
            indexName += "All22Languages";
        } else {
            indexName += "_" + Arrays.toString(includeLanguages);
        }
        FileWriter indexFile = new FileWriter(corpusRootFolderPath + indexName + ".txt");

        // iterate over all languages
        File[] languageFolders = FileHelper.getFiles(corpusRootFolderPath);
        for (File languageFolder : languageFolders) {

            String currentLanguageCode = FileHelper.getFolderName(languageFolder.getPath());

            if (includeLanguages != null && !CollectionHelper.contains(includeLanguages, currentLanguageCode)) {
                LOGGER.info("skip language " + currentLanguageCode);
            }

            // process each text document in the folder
            File[] textFiles = FileHelper.getFiles(languageFolder.getPath());
            for (File textFile : textFiles) {
                indexFile.write(currentLanguageCode + "/" + textFile.getName() + " " + currentLanguageCode);
                indexFile.write("\n");
                indexFile.flush();
            }
        }

        indexFile.close();

        LOGGER.info("index file created in " + sw.getElapsedTimeString());
    }

    /**
     * Create a smaller subset of an index with exactly the same number of instances per class.
     * 
     * @param indexFilePath The path to the index file.
     * @param instancesPerClass The number of instances per class.
     * @throws IOException
     */
    public void createIndexExcerpt(String indexFilePath, int instancesPerClass) throws IOException {

        StopWatch sw = new StopWatch();

        FileWriter indexFile = new FileWriter(FileHelper.getFilePath(indexFilePath)
                + FileHelper.appendToFileName(indexFilePath, "_ipc" + instancesPerClass) + ".txt");

        final Object[] obj = new Object[3];
        obj[0] = indexFile;

        // number of instances for each class
        obj[1] = new CountMap();

        obj[2] = instancesPerClass;

        LineAction la = new LineAction(obj) {

            @Override
            public void performAction(String line, int lineNumber) {
                String[] parts = line.split(" ");
                if (parts.length < 2) {
                    return;
                }

                if (((CountMap) obj[1]).get(parts[1]) >= ((Integer) obj[2])) {
                    return;
                }

                try {
                    ((FileWriter) obj[0]).write(line + "\n");
                } catch (IOException e) {
                    LOGGER.error(e.getMessage());
                }

                ((CountMap) obj[1]).increment(parts[1]);
            }

        };

        FileHelper.performActionOnEveryLine(indexFilePath, la);

        indexFile.close();

        LOGGER.info("index excerpt file created in " + sw.getElapsedTimeString());

    }

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        JRCCorpusConverter cc = new JRCCorpusConverter();
        cc.convertAllFiles("C:\\Safe\\Datasets\\jrc language data\\",
                "C:\\Safe\\Datasets\\jrc language data converted\\");
        // cc.createIndex("C:\\Users\\sky\\Desktop\\jrc-en-con\\");
        // cc.createIndexExcerpt("C:\\Users\\sky\\Desktop\\jrc-en-con\\indexAll22Languages.txt", 200);
    }

}
