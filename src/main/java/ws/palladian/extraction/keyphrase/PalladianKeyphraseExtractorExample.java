//package ws.palladian.extraction.keyphrase;
//
//import java.util.List;
//import java.util.Set;
//
//import ws.palladian.extraction.keyphrase.KeyphraseExtractorSettings.AssignmentMode;
//import ws.palladian.extraction.keyphrase.extractors.PalladianKeyphraseExtractor;
//import ws.palladian.helper.collection.CollectionHelper;
//import ws.palladian.preprocessing.scraping.ReadabilityContentExtractor;
//
///**
// * Example on {@link PalladianKeyphraseExtractor} usage.
// * Assign at least 512 MB to the VM.
// * 
// * @author Philipp Katz
// * 
// */
//public class PalladianKeyphraseExtractorExample {
//
//    public static void main(String[] args) {
//
//        PalladianKeyphraseExtractor keyphraseExtractor = new PalladianKeyphraseExtractor();
//        KeyphraseExtractorSettings settings = keyphraseExtractor.getSettings();
//
//        // Path to Corpus+Classifier
//        settings.setModelPath("data/models/PalladianKeyphraseExtractor");
//
//        // extract 10 keyphrases per document
//        settings.setAssignmentMode(AssignmentMode.FIXED_COUNT);
//        settings.setKeyphraseCount(20);
//
//        // maximum length of extracted keyphrases
//        settings.setMinPhraseLength(1);
//        settings.setMaxPhraseLength(3);
//
//        // Pattern, which keyphrases have to match
//        settings.setPattern("[a-zA-Z\\s\\-]{3,}");
//
//        // load the model files
//        keyphraseExtractor.load();
//
//        // extract keyphrases
//        String text = getSampleText();
//        List<Keyphrase> keyphrases = keyphraseExtractor.extract(text);
//
//        // result
//        CollectionHelper.print(keyphrases);
//        // for (Keyphrase keyphrase : keyphrases) {
//        // System.out.println(keyphrase.getValue() + " " + keyphrase.getWeight());
//        // }
//
//    }
//
//    private static String getSampleText() {
//        ReadabilityContentExtractor contentExtractor = new ReadabilityContentExtractor();
//        String text = contentExtractor
//                .getResultText("http://arstechnica.com/web/news/2011/01/mozilla-google-take-different-approaches-to-user-tracking-opt-out.ars");
//        return text;
//    }
//
//}