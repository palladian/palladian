package ws.palladian.helper.constants;

import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Container for a date format and associated regex.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class DateFormat {

    private final String regex;
    private final String format;
    private Pattern pattern;

    /**
     * <p>
     * Initialize a new {@link DateFormat} with the specified regex and format description. Constructor is
     * package-private as it is intended to create instances of this class only as constants in {@link RegExp}.
     * </p>
     * 
     * @param regex The regex to match the DateFormat, not <code>null</code>.
     * @param format The format description (e.g. <code>YYYY-MM-DD HH:MM:SS</code>), not <code>null</code>.
     */
    DateFormat(String regex, String format) {
        Validate.notEmpty(regex, "regex must not be empty");
        Validate.notEmpty(format, "format must not be empty");

        this.regex = regex;
        this.format = format;
        this.pattern = null;
    }

    /**
     * @return the regex
     */
    public String getRegex() {
        return regex;
    }

    /**
     * @return the format
     */
    public String getFormat() {
        return format;
    }

    public Pattern getPattern() {
        // cache the Pattern, but initialize lazily.
        if (pattern == null) {
            pattern = Pattern.compile(regex);
        }
        return pattern;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DateFormat [regex=");
        builder.append(regex);
        builder.append(", format=");
        builder.append(format);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((format == null) ? 0 : format.hashCode());
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
        DateFormat other = (DateFormat)obj;
        if (format == null) {
            if (other.format != null)
                return false;
        } else if (!format.equals(other.format))
            return false;
        return true;
    }

}
