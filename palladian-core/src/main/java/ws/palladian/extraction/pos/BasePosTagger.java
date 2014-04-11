package ws.palladian.extraction.pos;

import java.util.Iterator;
import java.util.List;

import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.tagger.NerHelper;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Tagger;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.ImmutableAnnotation;

/**
 * <p>
 * This is the abstract base class for all Part of Speech taggers offered by Palladian.
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
     * Get a list of tags for the given tokens.
     * 
     * @param tokens The tokens to tag.
     * @return The POS tags. The returned list must have the same size as the given tokens list.
     */
    protected abstract List<String> getTags(List<String> tokens);

    protected static String normalizeTag(String tag) {
        return tag.replaceAll("-.*", "");
    }

    public abstract String getName();

}
