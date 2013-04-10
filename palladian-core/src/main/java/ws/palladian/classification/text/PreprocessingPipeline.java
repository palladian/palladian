package ws.palladian.classification.text;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.text.FeatureSetting.TextFeatureType;
import ws.palladian.extraction.feature.AbstractTokenRemover;
import ws.palladian.extraction.feature.CharNGramCreator;
import ws.palladian.extraction.feature.DuplicateTokenRemover;
import ws.palladian.extraction.feature.LengthTokenRemover;
import ws.palladian.extraction.feature.LowerCaser;
import ws.palladian.extraction.feature.NGramCreator;
import ws.palladian.extraction.feature.TextDocumentPipelineProcessor;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * A {@link ProcessingPipeline} which carries out the feature extraction for the {@link PalladianTextClassifier}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class PreprocessingPipeline extends ProcessingPipeline {

    /**
     * <p>
     * Create a new {@link PreprocessingPipeline} for the specified {@link FeatureSetting}.
     * </p>
     * 
     * @param featureSetting The configuration for the feature extraction setup, not <code>null</code>.
     */
    public PreprocessingPipeline(FeatureSetting featureSetting) {
        Validate.notNull(featureSetting, "featureSetting must not be null");

        connectToPreviousProcessor(new LowerCaser());

        int minNGramLength = featureSetting.getMinNGramLength();
        int maxNGramLength = featureSetting.getMaxNGramLength();
        if (featureSetting.getTextFeatureType() == TextFeatureType.CHAR_NGRAMS) {
            connectToPreviousProcessor(new CharNGramCreator(minNGramLength, maxNGramLength));
        } else {
            connectToPreviousProcessor(new RegExTokenizer());
            connectToPreviousProcessor(new NGramCreator(minNGramLength, maxNGramLength));
        }

        if (featureSetting.getTextFeatureType() == TextFeatureType.WORD_NGRAMS) {
            int minTermLength = featureSetting.getMinimumTermLength();
            int maxTermLength = featureSetting.getMaximumTermLength();
            connectToPreviousProcessor(new LengthTokenRemover(minTermLength, maxTermLength));
        }

        connectToPreviousProcessor(new DuplicateTokenRemover());
        connectToPreviousProcessor(new UnwantedTokenRemover());
        connectToPreviousProcessor(new TokenLimiter(featureSetting.getMaxTerms()));
    }

    /** This PipelineProcessor limits the number of tokens to a specified count. */
    private static final class TokenLimiter extends TextDocumentPipelineProcessor {
        private final int maxTokens;

        private TokenLimiter(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        @Override
        public void processDocument(TextDocument document) throws DocumentUnprocessableException {
            List<PositionAnnotation> terms = CollectionHelper.newArrayList(BaseTokenizer.getTokenAnnotations(document));

            if (terms.size() > maxTokens) {
                // sort by occurrence positions
                Collections.sort(terms);

                terms = CollectionHelper.newArrayList(terms.subList(0, maxTokens));

                document.getFeatureVector().removeAll(BaseTokenizer.PROVIDED_FEATURE);
                document.getFeatureVector().addAll(terms);
            }
        }
    }

    /** This PipelineProcessor removes undesired tokens. */
    private static final class UnwantedTokenRemover extends AbstractTokenRemover {
        @Override
        protected boolean remove(PositionAnnotation annotation) {
            String tokenValue = annotation.getValue();
            return StringHelper.containsAny(tokenValue, Arrays.asList("&", "/", "="))
                    || StringHelper.isNumber(tokenValue);
        }
    }

}
