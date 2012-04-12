package ws.palladian.extraction.entity.dataset;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;

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

    /**
     * Split a dataset file of any format 50/50 with alternating assignments of the documents to split 1 and 2.
     * So all even documents go into the first split, the rest in the second split.
     */
    public void splitFileByDocuments(String datasetPath, String documentSeparator) {

        StopWatch sw = new StopWatch();

        String filename = FileHelper.getFileName(datasetPath);
        String content = FileHelper.readFileToString(datasetPath);

        String[] documents = content.split(documentSeparator);

        StringBuilder split1 = new StringBuilder();
        StringBuilder split2 = new StringBuilder();

        for (int i = 0; i < documents.length; i++) {

            // the first document must be empty since the document separator is before each document
            if (i == 0) {
                continue;
            }

            String document = documents[i];

            if (i % 2 == 0) {
                split1.append(documentSeparator);
                split1.append(document);
            } else {
                split2.append(documentSeparator);
                split2.append(document);
            }

        }

        String splitFilename = FileHelper.getFilePath(datasetPath) + filename + "_part1.txt";
        FileHelper.writeToFile(splitFilename, split1);

        splitFilename = FileHelper.getFilePath(datasetPath) + filename + "_part2.txt";
        FileHelper.writeToFile(splitFilename, split2);

        LOGGER.info("split file " + datasetPath + " in " + sw.getElapsedTimeString());

    }

    /**
     * Create several smaller training sets from a larger automatically generated one. The dataset must have documents
     * separated with "=-DOCSTART-" and in XML format. Use the {@link FileFormatParser} to generate it if needed.<br />
     * For example, seedsTest50.txt contains 50 seeds per type, if you call the method with (seedsTest50.txt, 1,50,10)
     * it will create 5 smaller datasets.
     * 
     * @param datasetPath The path to the dataset from which the smaller datasets should be created from.
     * @param minSeeds The minimum number of seed entities.
     * @param maxSeeds The maximum number of seed entities.
     * @param stepSize The step size from minimum to maximum seed entities.
     */
    public void createSubsets(String datasetPath, int minSeeds, int maxSeeds, int stepSize) {

        StopWatch sw = new StopWatch();

        String content = FileHelper.readFileToString(datasetPath);
        String[] documents = content.split("=-DOCSTART-");

        String columnFilePath = FileHelper.getFilePath(datasetPath) + "columnInput.txt";
        FileFormatParser.xmlToColumn(datasetPath, columnFilePath, "\t");

        int originalStepSize = stepSize;
        for (int numberOfSeeds = minSeeds; numberOfSeeds <= maxSeeds; numberOfSeeds += stepSize) {

            if (numberOfSeeds == 1) {
                stepSize -= 1;
            } else {
                stepSize = originalStepSize;
            }

            StringBuilder newDataset = new StringBuilder();
            Annotations annotations = FileFormatParser.getSeedAnnotations(columnFilePath, numberOfSeeds);

            for (String document : documents) {

                // check whether only one of the allowed annotations is in the document, remove all others
                boolean addDocument = false;

                document = HtmlHelper.stripHtmlTags(document);

                for (Annotation annotation : annotations) {
                    String tagName = annotation.getInstanceCategoryName().toUpperCase();
                    document = document.replace(annotation.getEntity(), "<" + tagName + ">" + annotation.getEntity()
                            + "</" + tagName + ">");
                    if (document.indexOf("<" + tagName + ">") > -1) {
                        addDocument = true;
                    }
                }

                if (addDocument) {
                    newDataset.append("=-DOCSTART-");
                    newDataset.append(document);
                }

            }

            String outputPath = FileHelper.getFilePath(datasetPath) + "/newDataset" + numberOfSeeds + ".txt";
            FileHelper.writeToFile(outputPath, newDataset);
            FileFormatParser.xmlToColumn(outputPath, outputPath, "\t");

            String columnContent = FileHelper.readFileToString(outputPath);
            columnContent = columnContent.replaceAll("=-\tO\nDOCSTART\tO\n-\tO", "=-DOCSTART-\tO");
            FileHelper.writeToFile(outputPath, columnContent);

        }

        LOGGER.info("processing the dataset took " + sw.getElapsedTimeString());
    }

    public static void main(String[] args) {

        DatasetProcessor dp = new DatasetProcessor();

        // split the conll 2003 training file into 50 documents containing 1 to 50 documents
        // dp.splitFile("data/datasets/ner/conll/training.txt", "=-DOCSTART-\tO", 1, 50, 1);

        // dp.splitFileByDocuments("data/datasets/ner/tud/tud2011Complete.xml", "=-DOCSTART-");

        // dp.createSubsets("data/temp/autoGeneratedDataTUD4/seedsTest50_temp.txt", 1, 50, 10);

        FileFormatParser.columnToXml("data/temp/autoGeneratedDataConll/seedsTest50.txt",
                "data/temp/autoGeneratedDataConll/seedsTest50_temp.txt", "\t");
        dp.createSubsets("data/temp/autoGeneratedDataConll/seedsTest50_temp.txt", 1, 50, 10);

        // FileFormatParser.xmlToColumn("data/datasets/ner/tud/tud2011Complete_part1.txt",
        // "data/datasets/ner/tud/tud2011_train.txt", "\t");
        // FileFormatParser.xmlToColumn("data/datasets/ner/tud/tud2011Complete_part2.txt",
        // "data/datasets/ner/tud/tud2011_test.txt", "\t");

    }

}
