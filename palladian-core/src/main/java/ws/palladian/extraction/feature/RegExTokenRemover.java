package ws.palladian.extraction.feature;

import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import ws.palladian.processing.features.Annotation;

/**
 * <p>
 * A {@link AbstractTokenRemover} which removes those tokens which do not match for the supplied RegEx. E.g.
 * initializing this class with a {@link Pattern} <i>[A-Za-z0-9]+</i> will remove all token {@link Annotation}s which do
 * not exclusively consist of alphanumeric and number characters.
 * </p>
 * 
 * @author Philipp Katz
 */
public class RegExTokenRemover extends AbstractTokenRemover {

    private static final long serialVersionUID = 1L;

    /** The {@link Pattern} used to determine whether to remove an {@link Annotation}. */
    private final Pattern pattern;

    /**
     * <p>
     * Create a new {@link RegExTokenRemover} with the specified {@link Pattern}.
     * </p>
     * 
     * @param pattern The {@link Pattern} to use for removing tokens.
     */
    public RegExTokenRemover(Pattern pattern) {
        Validate.notNull(pattern, "pattern must not be null");
        this.pattern = pattern;
    }

    /**
     * <p>
     * Create a new {@link RegExTokenRemover} with the specified pattern.
     * </p>
     * 
     * @param pattern The pattern to use for removing tokens.
     */
    public RegExTokenRemover(String pattern) {
        Validate.notEmpty(pattern, "pattern must not be null or empty");
        this.pattern = Pattern.compile(pattern);
    }

    @Override
    protected boolean remove(Annotation annotation) {
        return !pattern.matcher(annotation.getValue()).matches();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RegExTokenRemover [pattern=");
        builder.append(pattern);
        builder.append("]");
        return builder.toString();
    }

}
