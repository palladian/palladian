/**
 * Created on: 16.06.2012 18:44:37
 */
package ws.palladian.extraction.feature;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.AbstractFeatureProvider;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * Takes a tokenized (see {@link BaseTokenizer}) as input and annotates all token matching one item from a dictionary.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class DictionaryAnnotator extends AbstractFeatureProvider<String> {

    /**
     * <p>
     * The dictionary to match token against.
     * </p>
     */
    private final Set<String> dictionary;

    /**
     * <p>
     * Creates a new {@code DictionaryAnnotator} saving all {@link PositionAnnotation}s to a new {@link AnnotationFeature}
     * identified by {@code featureDescriptor}. The new {@code DictionaryAnnotator} also uses the provided
     * {@code dictionary} to match token.
     * </p>
     * 
     * @param featureDescriptor The {@link FeatureDescriptor} used to save matching tokens as new {@link PositionAnnotation}s.
     * @param dictionary The dictionary to match token agains.
     */
    public DictionaryAnnotator(String featureIdentifier, String[] dictionary) {
        super(featureIdentifier);

        Validate.notNull(dictionary, "dictionary must not be null");

        this.dictionary = new HashSet<String>(Arrays.asList(dictionary));
    }

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        PipelineDocument<String> document = getDefaultInput();

        List<PositionAnnotation> annotations = document.getFeatureVector().getAll(PositionAnnotation.class, BaseTokenizer.PROVIDED_FEATURE);
        List<PositionAnnotation> matchingToken = CollectionHelper.newArrayList();
        for (PositionAnnotation tokenAnnotation : annotations) {
            String token = tokenAnnotation.getValue();
            if (dictionary.contains(token)) {
                int startPosition = tokenAnnotation.getStartPosition();
                int endPosition = tokenAnnotation.getEndPosition();
                PositionAnnotation match = new PositionAnnotation(getDescriptor(), startPosition, endPosition, 0, token);
                matchingToken.add(match);
            }
        }
        
        document.getFeatureVector().addAll(matchingToken);

        // document.addFeature(new TextAnnotationFeature(getDescriptor(), matchingToken));
        setDefaultOutput(document);
    }

}
