/**
 * Created on: 16.06.2012 18:44:37
 */
package ws.palladian.extraction.feature;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.AbstractFeatureProvider;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.processing.features.PositionAnnotationFactory;

/**
 * <p>
 * Takes a tokenized (see {@link BaseTokenizer}) as input and annotates all token matching one item from a dictionary.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class DictionaryAnnotator extends AbstractFeatureProvider {

    private final static Logger LOGGER = LoggerFactory.getLogger(DictionaryAnnotator.class);

    /**
     * <p>
     * The dictionary to match token against.
     * </p>
     */
    private final Set<String> dictionary;

    /**
     * <p>
     * Creates a new {@code DictionaryAnnotator} saving all {@link PositionAnnotation}s to a new
     * {@link AnnotationFeature}. The new {@code DictionaryAnnotator} also uses the provided {@code dictionary} to match
     * token.
     * </p>
     * 
     * @param featureDescriptor The identifier used to save matching tokens as new {@link PositionAnnotation}s.
     * @param dictionary The dictionary to match token agains.
     */
    public DictionaryAnnotator(String featureIdentifier, String[] dictionary) {
        super(featureIdentifier);
        Validate.notNull(dictionary, "dictionary must not be null");
        this.dictionary = new HashSet<String>(Arrays.asList(dictionary));
    }

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        TextDocument document = (TextDocument)getInputPort(DEFAULT_INPUT_PORT_IDENTIFIER).poll();

        List<PositionAnnotation> annotations = document.get(ListFeature.class,
                BaseTokenizer.PROVIDED_FEATURE);
        if (annotations.isEmpty()) {
            LOGGER.warn("No tokens found. Did you specify a Tokenizer before using the DictionaryAnnotator?");
        }
        ListFeature<PositionAnnotation> matchingToken = new ListFeature<PositionAnnotation>(getCreatedFeatureName());
        PositionAnnotationFactory annotationFactory = new PositionAnnotationFactory(document);
        for (PositionAnnotation tokenAnnotation : annotations) {
            String token = tokenAnnotation.getValue();
            if (dictionary.contains(token)) {
                int startPosition = tokenAnnotation.getStartPosition();
                int endPosition = tokenAnnotation.getEndPosition();
                PositionAnnotation annotation = annotationFactory.create(startPosition, endPosition);
                matchingToken.add(annotation);
            }
        }

        document.add(matchingToken);

        // document.addFeature(new TextAnnotationFeature(getDescriptor(), matchingToken));
        getOutputPort(DEFAULT_OUTPUT_PORT_IDENTIFIER).put(document);
    }

}
