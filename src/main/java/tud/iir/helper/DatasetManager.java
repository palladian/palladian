package tud.iir.helper;

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

public class DatasetManager {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DatasetManager.class);

    /**
     * Create an index of file location [space] class name.
     * 
     * @param corpusRootFolder The path to the root folder of the dataset.
     * @throws IOException
     */
    public void createIndex(String corpusRootFolder) throws IOException {
        createIndex(corpusRootFolder, null);
    }

    /**
     * Create an index of file location [space] class name for all classes specified in the array.
     * 
     * @param corpusRootFolderPath The path to the root folder of the dataset.
     * @param includeClasses The class names that should be included in the index.
     * @throws IOException
     */
    public void createIndex(String corpusRootFolderPath, String[] includeClasses) throws IOException {

        StopWatch sw = new StopWatch();

        if (!corpusRootFolderPath.endsWith("/")) {
            corpusRootFolderPath += "/";
        }
        String indexName = "index";
        if (includeClasses != null) {
            indexName += "_" + Arrays.toString(includeClasses);
        }
        FileWriter indexFile = new FileWriter(corpusRootFolderPath + indexName + ".txt");

        // iterate over all classes
        File[] classFolders = FileHelper.getFiles(corpusRootFolderPath);
        for (File classFolder : classFolders) {

            if (classFolder.isFile()) {
                continue;
            }

            String className = FileHelper.getFolderName(classFolder.getPath());

            if (includeClasses != null && !CollectionHelper.contains(includeClasses, className)) {
                LOGGER.info("skip class " + className);
                continue;
            }

            // process each text document in the folder
            File[] textFiles = FileHelper.getFiles(classFolder.getPath());
            for (File textFile : textFiles) {

                if (textFile.isDirectory()) {
                    continue;
                }

                indexFile.write(className + "/" + textFile.getName() + " " + className);
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
        File[] classFolders = FileHelper.getFiles(corpusRootFolderPath);
        for (File classFolder : classFolders) {

            File[] textFiles = FileHelper.getFiles(classFolder.getPath());
            for (File file : textFiles) {

                if (file.isDirectory()) {
                    continue;
                }

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
     */
    public static void main(String[] args) {
        DatasetManager dsm = new DatasetManager();

    }

}
