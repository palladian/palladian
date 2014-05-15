package ws.palladian.extraction.content.evaluation;

import java.io.File;

import ws.palladian.extraction.content.PalladianContentExtractor;
import ws.palladian.extraction.content.WebPageContentExtractor;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.JaroWinklerSimilarity;
import ws.palladian.helper.nlp.LevenshteinSimilarity;
import ws.palladian.helper.nlp.CharacterNGramSimilarity;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.helper.nlp.StringSimilarity;

public class CleanevalDatasetEvaluation {

    private static void evaluate(String datasetDirectory, WebPageContentExtractor extractor) throws Exception {

        double totalScore1 = 0.;
        double totalScore2 = 0.;
        double totalScore3 = 0.;

        StringSimilarity similarity1 = new LevenshteinSimilarity();
        StringSimilarity similarity2 = new CharacterNGramSimilarity(5);
        StringSimilarity similarity3 = new JaroWinklerSimilarity();

        String resultFileName = extractor.getExtractorName() + "_results.csv";

        FileHelper.delete(resultFileName);

        File[] textFiles = FileHelper.getFiles(datasetDirectory, ".txt");
        for (int i = 0; i < textFiles.length; i++) {

            File expectedFile = textFiles[i];
            File htmlFile = new File(expectedFile.getAbsolutePath().replace(".txt", ".html"));

            try {
                extractor.setDocument(htmlFile);
            } catch (Exception e) {
                e.printStackTrace();
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

            ProgressHelper.printProgress(i, textFiles.length, 0);
        }

        double totalScore = (totalScore1 + totalScore2 + totalScore3) / (3 * textFiles.length);
        System.out.println("Total Score: " + totalScore);
    }

    private static final String cleanup(String expectedText) {
        expectedText = expectedText.replaceAll("URL: [^ ]+", "");
        expectedText = expectedText.replaceAll("\\<.+?\\>", "");
        expectedText = StringHelper.replaceProtectedSpace(expectedText);
        expectedText = StringHelper.removeLineBreaks(expectedText);
        expectedText = expectedText.replaceAll("\\s+", " ");
        expectedText = expectedText.trim();
        return expectedText;
    }

    public static void main(String[] args) throws Exception {
        // evaluate("C:\\Workspace\\data\\GoldStandard", new ReadabilityContentExtractor());

        // 0,626
        // => no namespace forcing 152 =>
        // 0,589784541

        // 0.8159125297381042

        evaluate("C:\\Workspace\\data\\GoldStandard", new PalladianContentExtractor()); // 0626
        // evaluate("C:\\Workspace\\data\\GoldStandard", new ReadabilityContentExtractor()); //
        // evaluate(ResourceHelper.getResourcePath("/WebPages/"), new ReadItLaterContentExtractor(
        // "a62g2W68p36ema12fvTc410Td1A1Na62"));
        // evaluate(ResourceHelper.getResourcePath("/WebPages/"), new ReadabilityContentExtractor());
        // evaluate(ResourceHelper.getResourcePath("/WebPages/"), new PalladianContentExtractor());
        // evaluate(ResourceHelper.getResourcePath("/WebPages/"), new BoilerpipeContentExtractor());
    }

}
