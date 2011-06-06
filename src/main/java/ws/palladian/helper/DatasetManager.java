package ws.palladian.helper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import ws.palladian.classification.page.evaluation.Dataset;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;

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
    public String createIndex(String corpusRootFolderPath, String[] includeClasses) throws IOException {

        StopWatch sw = new StopWatch();

        if (!corpusRootFolderPath.endsWith("/")) {
            corpusRootFolderPath += "/";
        }
        String indexName = "index";
        if (includeClasses != null) {
            indexName += "_" + Arrays.toString(includeClasses);
        }
        indexName += ".txt";
        FileWriter indexFile = new FileWriter(corpusRootFolderPath + indexName);

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

        return indexName;
    }

    /**
     * Create a smaller subset of an index with exactly the same number of instances per class.
     * 
     * @param indexFilePath The path to the index file.
     * @param instancesPerClass The number of instances per class.
     * @throws IOException
     */
    public String createIndexExcerpt(String indexFilePath, int instancesPerClass) throws IOException {

        StopWatch sw = new StopWatch();

        String indexFilename = FileHelper.appendToFileName(indexFilePath, "_ipc" + instancesPerClass);
        FileWriter indexFile = new FileWriter(indexFilename);

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

        return indexFilename;
    }

    /**
     * <p>
     * Create splits of the dataset so that it can be used for cross validation evaluation.
     * </p>
     * <p>
     * For example, if crossValidationFolds = 3 the following files will be created in the dataset root folder:<br>
     * 
     * <pre>
     * crossValidation_training1.txt (containing the first 1/3rd of the data)
     * crossValidation_test1.txt (containing the last 2/3rd of the data)
     * crossValidation_training2.txt (containing the second 1/3rd of the data)
     * crossValidation_test2.txt (containing the rest 2/3rd of the data)
     * crossValidation_training3.txt (containing the last 1/3rd of the data)
     * crossValidation_test3.txt (containing the rest 2/3rd of the data)
     * </pre>
     * 
     * This would be returned as 3 entries in the array with each entry containing the path to the training and test
     * data.
     * 
     * </p>
     * 
     * @param dataset The dataset to prepare for cross validation.
     * @param crossValidationFolds The number of folds for the cross validation.
     * @return The list of files used for the folds.
     */
    public List<String[]> splitForCrossValidation(Dataset dataset, int crossValidationFolds) {

        List<String[]> fileSplits = new ArrayList<String[]>();

        List<String> lines = FileHelper.readFileToArray(dataset.getPath());
        int numberOfTrainingLines = lines.size() / crossValidationFolds;

        for (int fold = 1; fold <= crossValidationFolds; fold++) {

            // the numbers that refer to the lines in the index file which bound the training data part
            int startLineTraining = (fold - 1) * numberOfTrainingLines;
            int endLineTraining = fold * numberOfTrainingLines;

            StringBuilder trainingData = new StringBuilder();
            StringBuilder testData = new StringBuilder();

            int lineNumber = 0;
            for (String line : lines) {

                if (lineNumber >= startLineTraining && lineNumber < endLineTraining) {
                    trainingData.append(line).append("\n");
                } else {
                    testData.append(line).append("\n");
                }

                lineNumber++;
            }

            String trainingFilePath = dataset.getRootPath() + dataset.getName() + "_crossValidation_training" + fold
            + ".txt";
            String testFilePath = dataset.getRootPath() + dataset.getName() + "_crossValidation_test" + fold + ".txt";

            FileHelper.writeToFile(trainingFilePath, trainingData);
            FileHelper.writeToFile(testFilePath, testData);

            String[] filePaths = new String[2];
            filePaths[0] = trainingFilePath;
            filePaths[1] = testFilePath;
            fileSplits.add(filePaths);
        }

        return fileSplits;
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
     * <p>
     * Split the index file into x parts where x is the highest number after CONCEPT_X.
     * </p>
     * <p>
     * For example:<br>
     * filePath politician_part1<br>
     * filePath politician_part2<br>
     * => should be split in two files, one containing only part1 and the other only part2.
     * </p>
     * 
     * @param indexFilePath The path to the file which should be split.
     * @throws IOException
     */
    public void splitIndexParts(String indexFilePath) throws IOException {

        StopWatch sw = new StopWatch();

        // map containing the parts with the file links
        Map<String, Set<String>> partsMap = new HashMap<String, Set<String>>();

        List<String> lines = FileHelper.readFileToArray(indexFilePath);
        for (String line : lines) {

            if (line.length() == 0) {
                continue;
            }

            String[] lineParts = line.split(" ");
            String part = lineParts[1].substring(lineParts[1].lastIndexOf("_") + 1);

            Set<String> links = partsMap.get(part);
            if (links == null) {
                links = new HashSet<String>();
                links.add(lineParts[0]);
                partsMap.put(part, links);
            } else {
                links.add(lineParts[0]);
            }
        }

        // write x file where x is the number of parts per concept
        for (Entry<String, Set<String>> partEntry : partsMap.entrySet()) {

            String partNumber = partEntry.getKey().substring(partEntry.getKey().lastIndexOf("part") + 4);

            FileWriter splitFile = new FileWriter(FileHelper.appendToFileName(indexFilePath, "_part" + partNumber));

            for (String link : partEntry.getValue()) {
                String conceptName = link.substring(0, link.indexOf("/"));
                splitFile.write(link);
                splitFile.write(" ");
                splitFile.write(conceptName);
                splitFile.write("\n");
                splitFile.flush();
            }

            splitFile.close();
        }

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

        // iterate over all folders
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
     * Split a dataset file which contains the complete data (not only links to the data files). The output will be two
     * files "training" and "test" in the dataset root folder.
     * 
     * @param dataset The dataset to split.
     * @param percentageTraining The percentage that should be used for training, e.g. 0.3 = 30%.
     */
    public void splitDataset(Dataset dataset, double percentageTraining) {

        if (dataset.isFirstFieldLink()) {
            LOGGER.warn("can only split datasets which consist of one file");
            return;
        }

        if (percentageTraining > 1) {
            percentageTraining /= 100;
        }

        List<String> lines = FileHelper.readFileToArray(dataset.getPath());
        java.util.Collections.shuffle(lines);
        List<String> linesTraining = new ArrayList<String>();
        List<String> linesTest = new ArrayList<String>();

        int trainingLines = (int) (percentageTraining * lines.size());
        for (int i = 0; i < lines.size(); i++) {

            if (i < trainingLines) {
                linesTraining.add(lines.get(i));
            } else {
                linesTest.add(lines.get(i));
            }

        }

        FileHelper.writeToFile(dataset.getRootPath() + "training.csv", linesTraining);
        FileHelper.writeToFile(dataset.getRootPath() + "test.csv", linesTest);

    }


    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        DatasetManager dsm = new DatasetManager();

        String corpusRootFolderPath = "data/datasets/ner/www_test2/";
        dsm.cleanDataset(corpusRootFolderPath);
        dsm.createIndex(corpusRootFolderPath);
        dsm.splitIndex(corpusRootFolderPath + "index.txt", 50);
        // dsm.splitIndexParts(corpusRootFolderPath + "index_split1.txt");

    }
}
