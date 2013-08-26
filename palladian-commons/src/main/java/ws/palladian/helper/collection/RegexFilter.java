package ws.palladian.helper.collection;

import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * A {@link Filter} for {@link String}s using Regex.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class RegexFilter implements Filter<String> {

    private final Pattern pattern;

    public RegexFilter(Pattern pattern) {
        Validate.notNull(pattern, "pattern must not be null");
        this.pattern = pattern;
    }

    public RegexFilter(String pattern) {
        Validate.notNull(pattern, "pattern must not be null");
        this.pattern = Pattern.compile(pattern);
    }

    @Override
    public boolean accept(String item) {
        if (item == null) {
            return false;
        }
        return pattern.matcher(item).matches();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RegexFilter [pattern=");
        builder.append(pattern);
        builder.append("]");
        return builder.toString();
    }

}
