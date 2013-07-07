package ws.palladian.extraction.feature;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.processing.features.PositionAnnotationFactory;

/**
 * <p>
 * The {@link CharNGramCreator} creates character level n-grams and stores them as {@link PositionAnnotation}s. To keep
 * memory footprint low when processing very large documents, use the constructor
 * {@link #CharNGramCreator(int, int, boolean, int)}, which allows to limit the maximum number of n-grams created. In
 * case, no frequency calculation is performed afterwards, you may also wish to set the <code>unique</code> parameter to
 * <code>true</code>.
 * </p>
 * 
 * @author Philipp Katz
 */
public class CharNGramCreator extends TextDocumentPipelineProcessor {

    private final int minLength;
    private final int maxLength;
    private final boolean unique;
    private final int limit;

    /**
     * <p>
     * Create a new {@link CharNGramCreator} which calculates [minLength, maxLength]-grams.
     * </p>
     * 
     * @param minLength Minimum length of the created n-grams, must be greater 0.
     * @param maxLength Maximum length of the created n-grams, must be greater minLength.
     * @param unique <code>true</code> to create only unique n-grams, <code>false</code> to keep duplicates.
     * @param limit The maximum number of n-grams to create, must be greater 0.
     */
    public CharNGramCreator(int minLength, int maxLength, boolean unique, int limit) {
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, minLength);
        Validate.inclusiveBetween(minLength, Integer.MAX_VALUE, maxLength);
        Validate.isTrue(limit > 0);

        this.minLength = minLength;
        this.maxLength = maxLength;
        this.unique = unique;
        this.limit = limit;
    }

    /**
     * <p>
     * Create a new {@link CharNGramCreator} which calculates [minLength, maxLength]-grams. Duplicate n-grams are kept.
     * </p>
     * 
     * @param minLength Minimum length of the created n-grams, must be greater 0.
     * @param maxLength Maximum length of the created n-grams, must be greater minLength.
     */
    public CharNGramCreator(int minLength, int maxLength) {
        this(minLength, maxLength, false, Integer.MAX_VALUE);
    }

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {

        PositionAnnotationFactory factory = new PositionAnnotationFactory(document);

        ListFeature<PositionAnnotation> gramTokens = new ListFeature<PositionAnnotation>(BaseTokenizer.PROVIDED_FEATURE);

        Set<String> uniqueTokens = CollectionHelper.newHashSet();

        int documentLength = document.getContent().length();
        out: for (int index = 0; index < documentLength; index++) {
            for (int offset = minLength; offset <= maxLength; offset++) {
                if (index + offset > documentLength) {
                    break;
                }
                String nGramValue = document.getContent().substring(index, index + offset);
                if (unique && !uniqueTokens.add(nGramValue)) {
                    continue;
                }
                gramTokens.add(factory.create(index, index + offset));
                if (gramTokens.size() >= limit) {
                    break out;
                }
            }
        }
        document.add(gramTokens);
    }

//    /**
//     * <p>
//     * Create n-grams of annotations with the specified length.
//     * </p>
//     * 
//     * @param document The document to process.
//     * @param n The length of the n-grams.
//     * @param unique Only unique ones.
//     * @return A list of n-grams for the specified document with a length of n.
//     */
//    private List<PositionAnnotation> createNGrams(TextDocument document, int n, boolean unique) {
//        List<PositionAnnotation> gramTokens = CollectionHelper.newArrayList();
//        Set<String> dedup = CollectionHelper.newHashSet();
//        PositionAnnotationFactory factory = new PositionAnnotationFactory(BaseTokenizer.PROVIDED_FEATURE, document);
//        for (int i = 0; i <= document.getContent().length() - n; i++) {
//            String nGramValue = document.getContent().substring(i, i + n);
//            if (unique && !dedup.add(nGramValue)) {
//                continue;
//            }
//            gramTokens.add(factory.create(i, i + n));
//        }
//        return gramTokens;
//    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CharNGramCreator [minLength=");
        builder.append(minLength);
        builder.append(", maxLength=");
        builder.append(maxLength);
        builder.append(", unique=");
        builder.append(unique);
        builder.append(", limit=");
        builder.append(limit);
        builder.append("]");
        return builder.toString();
    }

}