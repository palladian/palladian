package tud.iir.classification.language.evaluation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import tud.iir.classification.language.AlchemyLangDetect;
import tud.iir.classification.language.GoogleLangDetect;
import tud.iir.classification.language.JLangDetect;
import tud.iir.classification.language.LanguageClassifier;
import tud.iir.classification.language.PalladianLangDetect;
import tud.iir.helper.FileHelper;
import tud.iir.helper.MathHelper;
import tud.iir.helper.StopWatch;

public class LanguageDetectionEvaluation {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(LanguageDetectionEvaluation.class);

    /**
     * Evaluate two language detectors on a set of strings.
     * The evaluation file must have the following structure for each line:<br>
     * string to classify ### language
     * 
     * @param evaluationFilePath The file with the evaluation data.
     */
    public void evaluate(String evaluationFilePath, Set<String> possibleClasses) {

        LOGGER.info("evaluate JLangDetect vs. Google vs. Alchemy vs. Palladian");
        StopWatch sw = new StopWatch();

        LanguageClassifier jLangDetectClassifier = new JLangDetect();
        LanguageClassifier googleLanguageClassifier = new GoogleLangDetect();
        LanguageClassifier alchemyLanguageClassifier = new AlchemyLangDetect();
        LanguageClassifier palladianClassifier = new PalladianLangDetect();

        // we tell Palladian that only a subset of the learned languages is allowed for this evaluation, otherwise
        // jLangDetect has an advantage
        ((PalladianLangDetect) palladianClassifier).setPossibleClasses(possibleClasses);

        List<String> lines = FileHelper.readFileToArray(evaluationFilePath);

        int totalDocuments = FileHelper.getNumberOfLines(evaluationFilePath);
        int jLangCorrect = 0;
        int googleCorrect = 0;
        int alchemyCorrect = 0;
        int palladianCorrect = 0;

        int lineCount = 1;
        for (String line : lines) {
            String[] parts = line.split("###");
            String document = parts[0];
            String correctLanguage = parts[1];

            boolean jlang = false;
            boolean google = false;
            boolean alchemy = false;
            boolean palladian = false;

            if (correctLanguage.equals(jLangDetectClassifier.classify(document))) {
                jLangCorrect++;
                jlang = true;
            }

            if (correctLanguage.equals(googleLanguageClassifier.classify(document))) {
                googleCorrect++;
                google = true;
            }

            if (correctLanguage.equals(alchemyLanguageClassifier.classify(document))) {
                alchemyCorrect++;
                alchemy = true;
            }

            if (correctLanguage.equals(palladianClassifier.classify(document))) {
                palladianCorrect++;
                palladian = true;
            }

            LOGGER.info("line " + lineCount + " (" + jLangDetectClassifier.mapLanguageCode(correctLanguage)
                    + ") -> jlang: " + jlang + " | google: " + google + " | alchemy: " + alchemy + " | palladian: "
                    + palladian);

            lineCount++;
        }

        LOGGER.info("evaluated over " + totalDocuments + " strings in " + sw.getElapsedTimeString());
        LOGGER.info("Accuracy JLangDetect: " + MathHelper.round(100 * jLangCorrect / (double) totalDocuments, 2));
        LOGGER.info("Accuracy Google     : " + MathHelper.round(100 * googleCorrect / (double) totalDocuments, 2));
        LOGGER.info("Accuracy Alchemy    : " + MathHelper.round(100 * alchemyCorrect / (double) totalDocuments, 2));
        LOGGER.info("Accuracy Palladian  : " + MathHelper.round(100 * palladianCorrect / (double) totalDocuments, 2));
    }


    /**
     * @param args
     */
    public static void main(String[] args) {

        LanguageDetectionEvaluation evaluator = new LanguageDetectionEvaluation();
        Set<String> possibleClasses = new HashSet<String>();
        possibleClasses.add("da");
        possibleClasses.add("de");
        possibleClasses.add("el");
        possibleClasses.add("en");
        possibleClasses.add("es");
        possibleClasses.add("fi");
        possibleClasses.add("fr");
        possibleClasses.add("it");
        possibleClasses.add("nl");
        possibleClasses.add("pt");
        possibleClasses.add("sv");
        evaluator.evaluate("data/evaluation/LanguageDetection11Languages_small.txt", possibleClasses);

    }

}
