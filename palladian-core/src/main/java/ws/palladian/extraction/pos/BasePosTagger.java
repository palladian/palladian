package ws.palladian.extraction.pos;

import java.util.Iterator;
import java.util.List;

import ws.palladian.core.FeatureVector;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.tagger.NerHelper;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.Tagger;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.ImmutableAnnotation;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * This is the abstract base class for all Part of Speech taggers offered by Palladian. It implements two interfaces:
 * 
 * <ol>
 * <li>{@link PosTagger}, which is the "traditional" API used in Palladian. It allows POS tagging for text supplied as
 * String. In this case, the text is tokenized using a default {@link BaseTokenizer} implementation specific for the
 * respective POS tagger. Subclasses may override {@link #getTokenizer()} if they require a specific tokenizer.</li>
 * 
 * <li>{@link PipelineProcessor}, which works based on token annotations provided by an {@link AnnotationFeature}. This
 * means, that the input document must be tokenized in advance, using one of the available {@link BaseTokenizer}
 * implementations. In this mode, the POS tags are appended to the token's {@link FeatureVector}s and can be retrieved
 * later using the {@link #PROVIDED_FEATURE_DESCRIPTOR}.</li>
 * </ol>
 * </p>
 * 
 * @author Martin Wunderwald
 * @author David Urbansky
 * @author Philipp Katz
 * @author Klemens Muthmann
 */
public abstract class BasePosTagger implements Tagger {

    /** The default {@link BaseTokenizer} used if not overridden. */
    private static final BaseTokenizer DEFAULT_TOKENIZER = new RegExTokenizer();

    @Override
    public List<Annotation> getAnnotations(String text) {
        List<Annotation> tokenAnnotations = getTokenizer().getAnnotations(text);
        List<String> tokens = CollectionHelper.convertList(tokenAnnotations, AnnotationValueConverter.INSTANCE);
        List<String> posTags = getTags(tokens);
        List<Annotation> result = CollectionHelper.newArrayList();
        Iterator<String> tagsIterator = posTags.iterator();
        for (Annotation annotation : tokenAnnotations) {
            String tag = tagsIterator.next().toUpperCase();
            result.add(new ImmutableAnnotation(annotation.getStartPosition(), annotation.getValue(), tag));
        }
        return result;
    }

    public String getTaggedString(String text) {
        return NerHelper.tag(text, getAnnotations(text), TaggingFormat.SLASHES);
    }

    /**
     * <p>
     * Return the {@link BaseTokenizer} which this {@link PosTagger} uses when tagging String using {@link #tag(String)}
     * . Per default, a {@link RegExTokenizer} is returned, subclasses may override this method, if a specific
     * {@link BaseTokenizer} is required.
     * </p>
     * 
     * @return The {@link BaseTokenizer} to use.
     */
    protected BaseTokenizer getTokenizer() {
        return DEFAULT_TOKENIZER;
    }

    /**
     * <p>
     * Subclasses implement this method to perform the POS tagging. The POS tags can be assigned to each annotation
     * using the provided convenience method {@link #assignTag(PositionAnnotation, String)}.
     * </p>
     * 
     * @param tokens The list of annotations to process, this is the tokenized text.
     * @return
     */
    protected abstract List<String> getTags(List<String> tokens);

    protected static String normalizeTag(String tag) {
        return tag.replaceAll("-.*", "");
    }

    public abstract String getName();

}
