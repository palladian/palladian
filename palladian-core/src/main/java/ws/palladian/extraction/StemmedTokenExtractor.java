package ws.palladian.extraction;

import java.util.HashMap;
import java.util.Map;

import ws.palladian.extraction.feature.Annotation;
import ws.palladian.extraction.feature.AnnotationFeature;
import ws.palladian.extraction.feature.DuplicateTokenRemover;
import ws.palladian.extraction.feature.FrequencyCalculator;
import ws.palladian.extraction.feature.LengthTokenRemover;
import ws.palladian.extraction.feature.RegExTokenRemover;
import ws.palladian.extraction.feature.StemmerAnnotator;
import ws.palladian.extraction.feature.StopTokenRemover;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.extraction.token.TokenizerInterface;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.model.features.NominalFeature;
import ws.palladian.model.features.NumericFeature;

/**
 * <p>
 * A simple {@link ProcessingPipeline} for extracting stemmed tokens from documents.
 * </p>
 * 
 * @author Philipp Katz
 */
public class StemmedTokenExtractor extends ProcessingPipeline {

    private static final long serialVersionUID = 1L;

    /**
     * <p>
     * Create a new {@link StemmedTokenExtractor} for the specified language.
     * </p>
     * 
     * @param language The language for which to perform stemming and stop word removal.
     */
    public StemmedTokenExtractor(Language language) {
        add(new RegExTokenizer());
        add(new StemmerAnnotator(language));
        add(new StopTokenRemover(language));
        add(new LengthTokenRemover(2));
        add(new RegExTokenRemover("[A-Za-z0-9\\.]"));
        add(new FrequencyCalculator());
        add(new DuplicateTokenRemover());
    }

    /**
     * <p>
     * Get weighted tokens for the specified text.
     * </p>
     * 
     * @param text The text for which to extract weighted tokens.
     * @return {@link Map} containing the stemmed token values as keys, their frequencies as values.
     */
    public Map<String, Double> getTokens(String text) {
        PipelineDocument document = process(new PipelineDocument(text));
        AnnotationFeature feature = document.getFeatureVector().get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
        Map<String, Double> result = new HashMap<String, Double>();
        for (Annotation annotation : feature.getValue()) {
            // String value = annotation.getValue();
            NominalFeature stemmedValue = annotation.getFeatureVector().get(
                    StemmerAnnotator.PROVIDED_FEATURE_DESCRIPTOR);
            NumericFeature frequencyFeature = annotation.getFeatureVector().get(
                    FrequencyCalculator.PROVIDED_FEATURE_DESCRIPTOR);
            result.put(stemmedValue.getValue(), frequencyFeature.getValue());
        }
        return result;
    }

    public static void main(String[] args) {
        StemmedTokenExtractor stemmedTokenExtractor = new StemmedTokenExtractor(Language.GERMAN);
        Map<String, Double> tokens = stemmedTokenExtractor
                .getTokens("Die vom Verein für Internet-Benutzer Österreichs gestartete Bürgerinitiative hat bereits 4.471 Unterschriften auf Papier gesammelt und ans Parlament übermittelt. Nun muss sich der Nationalratsausschuss für Petitionen und Bürgerinitiativen damit befassen.");
        CollectionHelper.print(tokens);
    }

}
