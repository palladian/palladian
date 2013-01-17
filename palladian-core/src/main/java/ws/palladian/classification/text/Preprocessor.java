//package ws.palladian.classification.text;
//
//import java.util.Set;
//
//import ws.palladian.classification.text.evaluation.FeatureSetting;
//import ws.palladian.classification.universal.UniversalClassifier;
//import ws.palladian.extraction.token.Tokenizer;
//import ws.palladian.helper.nlp.StringHelper;
//import ws.palladian.processing.ProcessingPipeline;
//import ws.palladian.processing.features.FeatureVector;
//import ws.palladian.processing.features.NominalFeature;
//
///**
// * @author David Urbansky
// * @author Philipp Katz
// * @deprecated To be replaced by a {@link ProcessingPipeline}.
// */
//@Deprecated
//public final class Preprocessor {
//
//    private Preprocessor() {
//
//    }
//
//    /**
//     * Pre-process a string (such as a URL) and create a classification
//     * document. A map of n-grams is created for the document and added to it.
//     * If a n-gram term exists, it will be taken from the n-gram index.
//     * 
//     * @param text
//     *            The input string.
//     * @return The extracted set of terms.
//     */
//    public static FeatureVector preProcessDocument(String text, FeatureSetting featureSettings) {
//
//        Set<String> ngrams = null;
//
//        if (featureSettings.getTextFeatureType() == FeatureSetting.CHAR_NGRAMS) {
//
//            ngrams = Tokenizer.calculateAllCharNGrams(text, featureSettings.getMinNGramLength(),
//                    featureSettings.getMaxNGramLength());
//
//        } else if (featureSettings.getTextFeatureType() == FeatureSetting.WORD_NGRAMS) {
//
//            ngrams = Tokenizer.calculateAllWordNGrams(text, featureSettings.getMinNGramLength(),
//                    featureSettings.getMaxNGramLength());
//
//        } else {
//            throw new IllegalArgumentException(
//                    "Incorrect feature setting. Please set classifier to either use char ngrams or word ngrams.");
//        }
//
//        // create a new term map for the classification document
//        FeatureVector featureVector = new FeatureVector();
//        int termCount = 0;
//
//        for (String ngram : ngrams) {
//
//            // TODO, change that => do not add ngrams with some special chars or
//            // if it is only numbers
//            if (ngram.indexOf("&") > -1 || ngram.indexOf("/") > -1 || ngram.indexOf("=") > -1
//                    || StringHelper.isNumber(ngram)) {
//                continue;
//            }
//
//            if (featureSettings.getTextFeatureType() == FeatureSetting.WORD_NGRAMS
//                    && featureSettings.getMaxNGramLength() == 1
//                    && (ngram.length() < featureSettings.getMinimumTermLength() || ngram.length() > featureSettings
//                            .getMaximumTermLength()) || termCount >= featureSettings.getMaxTerms()) {
//                continue;
//            }
//
//            NominalFeature textFeature = new NominalFeature(UniversalClassifier.FEATURE_TERM, ngram.toLowerCase());
//            featureVector.add(textFeature);
//            termCount++;
//        }
//
//        return featureVector;
//
//    }
//
//
//}