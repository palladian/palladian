package ws.palladian.extraction.token;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.ImmutableAnnotation;

/**
 * <p>
 * A {@link AbstractTokenizer} implementation based on regular expressions. Tokens are matched against the specified regular
 * expressions.
 * </p>
 * 
 * @author Philipp Katz
 * @author Klemens Muthmann
 * @version 2.0
 * @since 0.1.7
 */
public final class RegExTokenizer extends AbstractTokenizer {

    /** The pattern that needs to match for a token to be extracted as a new {@code Annotation}. */
    private final Pattern pattern;

    /**
     * <p>
     * Creates a new {@code RegExTokenizer} creating token {@code Annotation}s with the provided identifier and
     * annotating token matching the provided {@code pattern}.
     * </p>
     * 
     * @param pattern The pattern that needs to match for a token to be extracted as a new {@code Annotation}.
     */
    public RegExTokenizer(Pattern pattern) {
        Validate.notNull(pattern, "pattern must not be null");
        this.pattern = pattern;
    }

    /**
     * <p>
     * Creates a new {@code RegExTokenizer} creating token {@code Annotation}s with the provided identifier and
     * annotating token matching the provided regular expression.
     * </p>
     * 
     * @param regex The regular expression to annotate in the input document. Refer to the {@link Pattern} class
     *            documentation for the regex format.
     */
    public RegExTokenizer(String regex) {
        this(Pattern.compile(regex));
    }
    
    public RegExTokenizer() {
        this(Tokenizer.TOKEN_SPLIT_REGEX);
    }

    @Override
    public List<Annotation> getAnnotations(String text) {
        Matcher matcher = pattern.matcher(text);
        List<Annotation> annotations = CollectionHelper.newArrayList();
        while (matcher.find()) {
            annotations.add(new ImmutableAnnotation(matcher.start(), matcher.group(), StringUtils.EMPTY));
        }
        return annotations;
    }
}
