package ws.palladian.extraction.location;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.constants.Language;

/**
 * <p>
 * An alternative name, usually in a specific language. For example, we might have the alternative name "Vienna" with
 * English {@link Language} for the city "Wien".
 * </p>
 * 
 * @author Philipp Katz
 */
public final class AlternativeName {

    private final String name;
    private final Language language;

    /**
     * <p>
     * Create a new AlternativeName.
     * </p>
     * 
     * @param name The name, not <code>null</code> or empty.
     * @param language The language, may be null if not specified.
     */
    public AlternativeName(String name, Language language) {
        Validate.notEmpty(name, "name must not be empty");
        this.name = name;
        this.language = language;
    }

    public String getName() {
        return name;
    }

    public Language getLanguage() {
        return language;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(name);
        if (language != null) {
            builder.append(" (").append(language).append(')');
        }
        return builder.toString();
    }
}
