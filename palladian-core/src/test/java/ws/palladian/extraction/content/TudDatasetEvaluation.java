package ws.palladian.extraction.content;

import java.io.File;

import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.helper.nlp.JaroWinklerSimilarity;
import ws.palladian.helper.nlp.LevenshteinSimilarity;
import ws.palladian.helper.nlp.NGramSimilarity;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.helper.nlp.StringSimilarity;

public class TudDatasetEvaluation {

    private static void evaluate(String datasetDirectory, WebPageContentExtractor extractor) throws Exception {

        StringSimilarity similarity1 = new LevenshteinSimilarity();
        StringSimilarity similarity2 = new NGramSimilarity(5);
        StringSimilarity similarity3 = new JaroWinklerSimilarity();

        String resultFileName = extractor.getExtractorName() + "_results.csv";

        FileHelper.delete(resultFileName);

        File[] textFiles = FileHelper.getFiles(datasetDirectory, ".txt");
        for (int i = 0; i < textFiles.length; i++) {

            File expectedFile = textFiles[i];
            File htmlFile = new File(expectedFile.getAbsolutePath().replace(".txt", ".html"));

            ProgressHelper.showProgress(i, textFiles.length, 0);

            String expectedText = FileHelper.readFileToString(expectedFile);
            expectedText = cleanup(expectedText);

            extractor.setDocument(htmlFile);

            String extractedText = extractor.getResultText();
            extractedText = cleanup(extractedText);

            double score1 = similarity1.getSimilarity(expectedText, extractedText);
            double score2 = similarity2.getSimilarity(expectedText, extractedText);
            double score3 = similarity3.getSimilarity(expectedText, extractedText);
            boolean startCorrect = false;
            boolean endCorrect = false;

            if (expectedText.length() > 25 && extractedText.length() > 25) {
                // check, whether the beginning/end of the text were extracted correctly:
                String expectedStart = expectedText.substring(0, 25);
                String extractedStart = extractedText.substring(0, 25);
                String expectedEnd = expectedText.substring(expectedText.length() - 25, expectedText.length());
                String extractedEnd = extractedText.substring(extractedText.length() - 25, extractedText.length());
                startCorrect = expectedStart.equals(extractedStart);
                endCorrect = expectedEnd.equals(extractedEnd);
            }

            String resultLine = String.format("%s;%f;%f;%f;%b;%b\n", htmlFile.getName(), score1, score2, score3,
                    startCorrect, endCorrect);

            FileHelper.appendFile(resultFileName, resultLine);
        }
    }

    private static final String cleanup(String expectedText) {
        expectedText = StringHelper.replaceProtectedSpace(expectedText);
        expectedText = StringHelper.removeLineBreaks(expectedText);
        expectedText = expectedText.replaceAll("\\s+", " ");
        expectedText = expectedText.trim();
        return expectedText;
    }

    public static void main(String[] args) throws Exception {
        evaluate(ResourceHelper.getResourcePath("/WebPages/"), new ReadabilityContentExtractor());
        evaluate(ResourceHelper.getResourcePath("/WebPages/"), new PalladianContentExtractor());
        evaluate(ResourceHelper.getResourcePath("/WebPages/"), new BoilerpipeContentExtractor());
    }

}
