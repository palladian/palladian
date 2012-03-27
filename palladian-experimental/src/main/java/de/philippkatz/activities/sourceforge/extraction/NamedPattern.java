package de.philippkatz.activities.sourceforge.extraction;

import java.util.regex.Pattern;

/**
 * A pattern with a name.
 * @author Philipp Katz
 */
public class NamedPattern {
    
    private final Pattern pattern;
    private final String name;
    public NamedPattern(Pattern pattern, String name) {
        this.pattern = pattern;
        this.name = name;
    }
    public NamedPattern(String pattern, String name) {
        this(Pattern.compile(pattern), name);
    }
    public Pattern getPattern() {
        return pattern;
    }
    public String getName() {
        return name;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NamedPattern [pattern=");
        builder.append(pattern);
        builder.append(", name=");
        builder.append(name);
        builder.append("]");
        return builder.toString();
    }
    

}
