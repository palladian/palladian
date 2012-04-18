package ws.palladian.extraction.feature;

import java.util.regex.Pattern;

import ws.palladian.model.features.Annotation;

/**
 * <p>
 * A {@link AbstractTokenRemover} which removes those tokens which match for the supplied RegEx. E.g. initializing this class
 * with a {@link Pattern} <i>[^A-Za-z0-9]+</i> will remove all token {@link Annotation}s which do not exclusively
 * consist of alphanumeric characters.
 * </p>
 * 
 * @author Philipp Katz
 */
public class RegExTokenRemover extends AbstractTokenRemover {

    private static final long serialVersionUID = 1L;
    
    private final Pattern pattern;

    /**
     * <p>
     * Create a new {@link RegExTokenRemover} with the specified {@link Pattern}.
     * </p>
     * 
     * @param pattern The {@link Pattern} to use for removing tokens.
     */
    public RegExTokenRemover(Pattern pattern) {
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
        this(Pattern.compile(pattern));
    }

    @Override
    protected boolean remove(Annotation annotation) {
        return pattern.matcher(annotation.getValue()).matches();
    }

}
