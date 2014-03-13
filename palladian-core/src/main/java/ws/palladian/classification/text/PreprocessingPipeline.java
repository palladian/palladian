package ws.palladian.classification.text;

import java.util.Arrays;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.text.FeatureSetting.TextFeatureType;
import ws.palladian.extraction.feature.AbstractTokenRemover;
import ws.palladian.extraction.feature.CharNGramCreator;
import ws.palladian.extraction.feature.LengthTokenRemover;
import ws.palladian.extraction.feature.LowerCaser;
import ws.palladian.extraction.feature.NGramCreator;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * A {@link ProcessingPipeline} which carries out the feature extraction for the {@link PalladianTextClassifier}.
 * </p>
 * 
 * @author Philipp Katz
 */
@Deprecated
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
            connectToPreviousProcessor(new CharNGramCreator(minNGramLength, maxNGramLength, true,
                    featureSetting.getMaxTerms()));
        } else {
            connectToPreviousProcessor(new RegExTokenizer());
            connectToPreviousProcessor(new NGramCreator(minNGramLength, maxNGramLength, true,
                    featureSetting.getMaxTerms()));
        }

        if (featureSetting.isWordUnigrams()) {
            int minTermLength = featureSetting.getMinimumTermLength();
            int maxTermLength = featureSetting.getMaximumTermLength();
            connectToPreviousProcessor(new LengthTokenRemover(minTermLength, maxTermLength));
        }

        connectToPreviousProcessor(new UnwantedTokenRemover());
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
