package ws.palladian.extraction.content.evaluation;

import java.io.File;
import java.util.List;

import ws.palladian.extraction.content.AlchemyApiContentExtractor;
import ws.palladian.extraction.content.PalladianContentExtractor;
import ws.palladian.extraction.content.ReadItLaterContentExtractor;
import ws.palladian.extraction.content.WebPageContentExtractor;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.helper.nlp.JaroWinklerSimilarity;
import ws.palladian.helper.nlp.LevenshteinSimilarity;
import ws.palladian.helper.nlp.CharacterNGramSimilarity;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.helper.nlp.StringSimilarity;

public class TudDatasetEvaluation {

    private static String getUrl(List<String> index, String fileName) {
        for (String string : index) {
            String[] split = string.split(";");
            if (fileName.contains(split[0])) {
                return split[1];
            }
        }

        return "";
    }

    public static double evaluate(String datasetDirectory, WebPageContentExtractor extractor) throws Exception {

        double totalScore1 = 0.;
        double totalScore2 = 0.;
        double totalScore3 = 0.;

        StringSimilarity similarity1 = new LevenshteinSimilarity();
        StringSimilarity similarity2 = new CharacterNGramSimilarity(5);
        StringSimilarity similarity3 = new JaroWinklerSimilarity();

        String resultFileName = extractor.getExtractorName() + "_results.csv";

        FileHelper.delete(resultFileName);
        FileHelper.appendFile(resultFileName, "file;levenshtein;5gram;jaroWinkler;startCorrect;endCorrect\n");
        
        // for online services, we need the URL to evaluate
        List<String> index = FileHelper.readFileToArray(datasetDirectory + "___index.csv");
        

        File[] textFiles = FileHelper.getFiles(datasetDirectory, ".txt");
        ProgressMonitor progressMonitor = new ProgressMonitor(textFiles.length);
        for (int i = 0; i < textFiles.length; i++) {

            File expectedFile = textFiles[i];
            File htmlFile = new File(expectedFile.getAbsolutePath().replace(".txt", ".html"));

            if (extractor instanceof ReadItLaterContentExtractor || extractor instanceof AlchemyApiContentExtractor) {
                String url = getUrl(index, expectedFile.getName());
                extractor.setDocument(url);
            } else {
                extractor.setDocument(htmlFile);
            }

            String expectedText = FileHelper.readFileToString(expectedFile);
            expectedText = cleanup(expectedText);

            String extractedText = extractor.getResultText();
            extractedText = cleanup(extractedText);

            double score1 = similarity1.getSimilarity(expectedText, extractedText);
            double score2 = similarity2.getSimilarity(expectedText, extractedText);
            double score3 = similarity3.getSimilarity(expectedText, extractedText);
            boolean startCorrect = false;
            boolean endCorrect = false;

            totalScore1 += score1;
            totalScore2 += score2;
            totalScore3 += score3;

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

            progressMonitor.incrementAndPrintProgress();
        }

        double totalScore = (totalScore1 + totalScore2 + totalScore3) / (3 * textFiles.length);
        System.out.println("Total Score: " + totalScore);

        return totalScore;
    }

    private static final String cleanup(String expectedText) {
        expectedText = StringHelper.replaceProtectedSpace(expectedText);
        expectedText = StringHelper.removeLineBreaks(expectedText);
        expectedText = expectedText.replaceAll("\\s+", " ");
        expectedText = expectedText.trim();
        return expectedText;
    }

    public static void main(String[] args) throws Exception {
        // evaluate(ResourceHelper.getResourcePath("/WebPages/"), new AlchemyApiContentExtractor(
        // "b0ec6f30acfb22472f458eec1d1acf7f8e8da4f5"));
        // evaluate(ResourceHelper.getResourcePath("/WebPages/"), new ReadItLaterContentExtractor(
        // "a62g2W68p36ema12fvTc410Td1A1Na62"));
        // evaluate(ResourceHelper.getResourcePath("/WebPages/"), new ReadabilityContentExtractor());
        evaluate(ResourceHelper.getResourcePath("/WebPages/"), new PalladianContentExtractor());
        // evaluate(ResourceHelper.getResourcePath("/WebPages/"), new BoilerpipeContentExtractor());
    }

}
