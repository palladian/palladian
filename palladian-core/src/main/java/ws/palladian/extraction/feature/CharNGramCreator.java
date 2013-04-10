package ws.palladian.extraction.feature;

import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.processing.features.PositionAnnotationFactory;

/**
 * <p>
 * The {@link CharNGramCreator} creates character level n-grams and stores them as {@link PositionAnnotation}s.
 * </p>
 * 
 * @author Philipp Katz
 */
public class CharNGramCreator extends TextDocumentPipelineProcessor {

    private final int minLength;
    private final int maxLength;

    /**
     * <p>
     * Create a new {@link CharNGramCreator} which calculates [minLength, maxLength]-grams.
     * </p>
     * 
     * @param minLength
     * @param maxLength
     */
    public CharNGramCreator(int minLength, int maxLength) {
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, minLength);
        Validate.inclusiveBetween(minLength, Integer.MAX_VALUE, maxLength);

        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        List<PositionAnnotation> gramTokens = CollectionHelper.newArrayList();
        for (int i = minLength; i <= maxLength; i++) {
            gramTokens.addAll(createNGrams(document, i));
        }
        document.getFeatureVector().addAll(gramTokens);
    }

    /**
     * <p>
     * Create n-grams of annotations with the specified length.
     * </p>
     * 
     * @param document The document to process.
     * @param n The length of the n-grams.
     * @return A list of n-grams for the specified document with a length of n.
     */
    private List<PositionAnnotation> createNGrams(TextDocument document, int n) {
        List<PositionAnnotation> gramTokens = CollectionHelper.newArrayList();
        PositionAnnotationFactory factory = new PositionAnnotationFactory(BaseTokenizer.PROVIDED_FEATURE, document);
        for (int i = 0; i <= document.getContent().length() - n; i++) {
            gramTokens.add(factory.create(i, i + n));
        }
        return gramTokens;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CharNGramCreator [minLength=");
        builder.append(minLength);
        builder.append(", maxLength=");
        builder.append(maxLength);
        builder.append("]");
        return builder.toString();
    }

}