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

    /**
     * <p>
     * Create a new AlternativeName without specified language.
     * </p>
     * 
     * @param name The name, not <code>null</code> or empty.
     */
    public AlternativeName(String name) {
        this(name, null);
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((language == null) ? 0 : language.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AlternativeName other = (AlternativeName)obj;
        if (language != other.language)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

}
