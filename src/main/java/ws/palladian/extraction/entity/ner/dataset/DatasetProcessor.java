package ws.palladian.extraction.entity.ner.dataset;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;

public class DatasetProcessor {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DatasetProcessor.class);

    /**
     * This method separates a dataset file document in X new files where X = maxDocuments - minDocuments + 1.
     * The dataset file can be given in any format but the documentSeparator should always separate two documents.
     * The files will be stored in the same directory as the datasetPath and named following the convention
     * "Y_separated_X.txt" where Y is the name of the dataset file and X is the number of documents in the file.
     * 
     * @param datasetPath The path to the dataset file which contains several documents separated by a certain string.
     * @param documentSeparator The separation sequence which determines the beginning and end of a document.
     * @param minDocuments The minimal number of documents in a separated file.
     * @param maxDocuments The maximal number of documents in a separated file.
     * @param stepSize The size of the steps between minDocuments and maxDocuments.
     */
    public List<String> splitFile(String datasetPath, String documentSeparator, int minDocuments, int maxDocuments,
            int stepSize) {

        StopWatch sw = new StopWatch();

        List<String> splitFiles = new ArrayList<String>();

        String filename = FileHelper.getFileName(datasetPath);
        String content = FileHelper.readFileToString(datasetPath);

        String[] documents = content.split(documentSeparator);


        for (int x = minDocuments; x <= maxDocuments; x += stepSize) {

            StringBuilder concatenatedDocuments = new StringBuilder();

            int documentCount = 0;
            for (int i = 0; i < documents.length; i++) {

                // the first document must be empty since the document separator is before each document
                if (i == 0) {
                    continue;
                }

                String document = documents[i];

                if (documentCount < x) {
                    concatenatedDocuments.append(documentSeparator);
                    concatenatedDocuments.append(document);
                } else {
                    break;
                }

                documentCount++;
            }

            String splitFilename = FileHelper.getFilePath(datasetPath) + filename + "_sep_" + x + ".txt";
            FileHelper.writeToFile(splitFilename, concatenatedDocuments);

            splitFiles.add(splitFilename);
        }

        LOGGER.info("split file " + datasetPath + " in " + sw.getElapsedTimeString());

        return splitFiles;
    }

    public static void main(String[] args) {

        DatasetProcessor dp = new DatasetProcessor();

        // split the conll 2003 training file into 50 documents containing 1 to 50 documents
        dp.splitFile("data/datasets/ner/conll/training.txt", "=-DOCSTART-\tO", 1, 50, 1);


    }

}
