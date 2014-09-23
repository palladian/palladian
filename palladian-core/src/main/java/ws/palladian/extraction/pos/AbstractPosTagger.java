package ws.palladian.extraction.pos;

import java.util.Iterator;
import java.util.List;

import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.core.Token;
import ws.palladian.core.Tagger;
import ws.palladian.core.TextTokenizer;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.tagger.NerHelper;
import ws.palladian.extraction.token.WordTokenizer;
import ws.palladian.helper.collection.CollectionHelper;

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
public abstract class AbstractPosTagger implements Tagger {

    /** The default tokenizer used if not overridden. */
    private static final TextTokenizer DEFAULT_TOKENIZER = new WordTokenizer();

    @Override
    public List<Annotation> getAnnotations(String text) {
        Iterator<Token> tokenAnnotations = getTokenizer().iterateTokens(text);
        List<Token> tokenList = CollectionHelper.newArrayList(tokenAnnotations);
        List<String> tokens = CollectionHelper.convertList(tokenList, Token.STRING_CONVERTER);
        List<String> posTags = getTags(tokens);
        List<Annotation> result = CollectionHelper.newArrayList();
        Iterator<String> tagsIterator = posTags.iterator();
        for (Token annotation : tokenList) {
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
     * Return the {@link TextTokenizer} which this {@link PosTagger} uses when tagging String using {@link #tag(String)}
     * . Per default, a {@link RegExTokenizer} is returned, subclasses may override this method, if a specific
     * {@link TextTokenizer} is required.
     * </p>
     * 
     * @return The {@link TextTokenizer} to use.
     */
    protected TextTokenizer getTokenizer() {
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
