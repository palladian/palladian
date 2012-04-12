package ws.palladian.classification.language.evaluation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.retrieval.DocumentRetriever;

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

    /** The logger for this class. */
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

        DocumentRetriever crawler = new DocumentRetriever();
        Document document = crawler.getXmlDocument(xmlFile.getPath());

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

            if (includeLanguages != null && !Arrays.asList(includeLanguages).contains(currentLanguageCode)) {
                LOGGER.info("skip language " + currentLanguageCode);
                continue;
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

        FileWriter indexFile = new FileWriter(FileHelper.appendToFileName(indexFilePath, "_ipc" + instancesPerClass));

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

                if (((CountMap) obj[1]).get(parts[1]) >= (Integer) obj[2]) {
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
     * Split the index file into 2 parts (for training and testing).
     * 
     * @param indexFilePath The path to the file which should be split.
     * @param splitPercentage The percentage of the first part. The second part is 100 - splitPercentage.
     * @throws IOException
     */
    public void splitIndex(String indexFilePath, int splitPercentage) throws IOException {

        StopWatch sw = new StopWatch();

        FileWriter splitFile1 = new FileWriter(FileHelper.appendToFileName(indexFilePath, "_split1"));
        FileWriter splitFile2 = new FileWriter(FileHelper.appendToFileName(indexFilePath, "_split2"));

        // a map holding all file links for each class
        Map<String, Set<String>> classMap = new HashMap<String, Set<String>>();

        List<String> lines = FileHelper.readFileToArray(indexFilePath);
        for (String line : lines) {

            String[] parts = line.split(" ");
            Set<String> links = classMap.get(parts[1]);
            if (links == null) {
                links = new HashSet<String>();
                links.add(parts[0]);
                classMap.put(parts[1], links);
            } else {
                links.add(parts[0]);
            }
        }

        // make the split
        for (Entry<String, Set<String>> entry : classMap.entrySet()) {

            Set<String> links = entry.getValue();
            int maxEntriesSplit1 = (int) (links.size() * splitPercentage / (double) 100);
            int entriesSplit1 = 0;
            for (String string : links) {

                if (entriesSplit1 < maxEntriesSplit1) {
                    splitFile1.write(string);
                    splitFile1.write(" ");
                    splitFile1.write(entry.getKey());
                    splitFile1.write("\n");
                    splitFile1.flush();
                    entriesSplit1++;
                } else {
                    splitFile2.write(string);
                    splitFile2.write(" ");
                    splitFile2.write(entry.getKey());
                    splitFile2.write("\n");
                    splitFile2.flush();
                }

            }

        }

        splitFile1.close();
        splitFile2.close();

        LOGGER.info("file " + indexFilePath + " splitted in " + sw.getElapsedTimeString());
    }

    /**
     * Delete all files that are empty.
     * 
     * @param corpusRootFolderPath The path to the root of the corpus.
     */
    public void cleanDataset(String corpusRootFolderPath) {

        StopWatch sw = new StopWatch();

        LOGGER.info("cleaning the dataset...");

        int deletedFiles = 0;

        // iterate over all languages
        File[] languageFolders = FileHelper.getFiles(corpusRootFolderPath);
        for (File languageFolder : languageFolders) {

            File[] textFiles = FileHelper.getFiles(languageFolder.getPath());
            for (File file : textFiles) {
                if (file.length() == 0) {
                    file.delete();
                    deletedFiles++;
                }
            }
        }

        LOGGER.info("dataset cleansed (" + deletedFiles + " files deleted) in " + sw.getElapsedTimeString());
    }

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        JRCCorpusConverter cc = new JRCCorpusConverter();
        // cc.convertAllFiles("C:\\Safe\\Datasets\\jrc language data\\","C:\\Safe\\Datasets\\jrc language data converted\\");
        // cc.cleanDataset("C:\\Safe\\Datasets\\jrc language data converted\\");
        // cc.createIndex("C:\\Safe\\Datasets\\jrc language data converted\\");
        cc.createIndexExcerpt("C:\\Safe\\Datasets\\jrc language data converted\\indexAll22Languages.txt", 1000);
        // cc.splitIndex("C:\\Safe\\Datasets\\jrc language data converted\\indexAll22Languages_ipc100.txt", 50);

    }

}
