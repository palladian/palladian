package ws.palladian.classification.text;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

public class PreprocessingPipeline extends ProcessingPipeline {

    public PreprocessingPipeline(final FeatureSetting featureSetting) {

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

        connectToPreviousProcessor(new AbstractTokenRemover() {
            @Override
            protected boolean remove(PositionAnnotation annotation) {
                String tokenValue = annotation.getValue();
                return (StringHelper.containsAny(tokenValue, Arrays.asList("&", "/", "=")) || StringHelper
                        .isNumber(tokenValue));
            }
        });

        connectToPreviousProcessor(new TextDocumentPipelineProcessor() {
            @Override
            public void processDocument(TextDocument document) throws DocumentUnprocessableException {
                List<PositionAnnotation> terms = CollectionHelper.newArrayList(BaseTokenizer
                        .getTokenAnnotations(document));
                Collections.sort(terms, new Comparator<PositionAnnotation>() {
                    @Override
                    public int compare(PositionAnnotation a1, PositionAnnotation a2) {
                        return Integer.valueOf(a1.getStartPosition()).compareTo(a2.getStartPosition());
                    }
                });

                if (terms.size() > featureSetting.getMaxTerms()) {
                    // terms.removeAll(terms.subList(featureSetting.getMaxTerms(), terms.size()));
                    terms = CollectionHelper.newArrayList(terms.subList(0, featureSetting.getMaxTerms()));
                }
                
                document.getFeatureVector().removeAll(BaseTokenizer.PROVIDED_FEATURE);
                document.getFeatureVector().addAll(terms);
            }
        });
    }

}
