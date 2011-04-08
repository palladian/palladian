package ws.palladian.extraction.entity.ner.dataset;

import ws.palladian.helper.FileHelper;

public class DatasetProcessor {

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
     */
    public void splitFile(String datasetPath, String documentSeparator, int minDocuments, int maxDocuments) {

        String filename = FileHelper.getFileName(datasetPath);
        String content = FileHelper.readFileToString(datasetPath);

        String[] documents = content.split(documentSeparator);

        StringBuilder concatenatedDocuments = new StringBuilder();

        for (int x = minDocuments; x <= maxDocuments; x++) {

            int documentCount = 0;
            for (String document : documents) {
                if (documentCount++ < x) {
                    concatenatedDocuments.append(document);
                }

            }
            FileHelper.writeToFile(filename + "_sep_" + x + ".txt", concatenatedDocuments);

        }

    }

    public static void main(String[] args) {

        DatasetProcessor dp = new DatasetProcessor();

        // split the conll 2003 training file into 50 documents containing 1 to 50 documents
        dp.splitFile("data/datasets/ner/conll/training.txt", "DOCSTART", 1, 50);


    }

}
