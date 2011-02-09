package tud.iir.extraction.keyphrase;

import java.util.Set;

import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.extraction.keyphrase.KeyphraseExtractorSettings.AssignmentMode;
import tud.iir.helper.CollectionHelper;

/**
 * Example on {@link PalladianKeyphraseExtractor} usage.
 * Assign at least 512 MB to the VM.
 * 
 * @author Philipp Katz
 *
 */
public class PalladianKeyphraseExtractorExample {

    public static void main(String[] args) {

        PalladianKeyphraseExtractor keyphraseExtractor = new PalladianKeyphraseExtractor();
        KeyphraseExtractorSettings settings = keyphraseExtractor.getSettings();

        // Path to Corpus+Classifier
        settings.setModelPath("data/models/PalladianKeyphraseExtractor");

        // extract 10 keyphrases per document
        settings.setAssignmentMode(AssignmentMode.FIXED_COUNT);
        settings.setKeyphraseCount(20);

        // maximum length of extracted keyphrases
        settings.setMinPhraseLength(2);
        settings.setMaxPhraseLength(3);

        // Pattern, which keyphrases have to match
        settings.setPattern("[a-zA-Z\\s\\-]{3,}");

        // load the model files
        keyphraseExtractor.load();

        // extract keyphrases
        String text = getSampleText();
        Set<Keyphrase> keyphrases = keyphraseExtractor.extract(text);

        // result
        CollectionHelper.print(keyphrases);

    }

    private static String getSampleText() {
        PageContentExtractor contentExtractor = new PageContentExtractor();
        String text = contentExtractor.getResultText("http://arstechnica.com/web/news/2011/01/mozilla-google-take-different-approaches-to-user-tracking-opt-out.ars");
        return text;
    }

}