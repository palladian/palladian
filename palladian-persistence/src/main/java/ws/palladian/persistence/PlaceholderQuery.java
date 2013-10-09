package ws.palladian.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * A {@link PlaceholderQuery} can be used to write prepared statements with named parameter markers. Later, parameters
 * can be assigned using those markers to build a {@link List} with arguments necessary for the {@link DatabaseManager}.
 * Parameters markers in the query start with @ followed by a combination of alphanumeric characters and underscores.
 * Instances of {@link PlaceholderQuery} are immutable and Thread-safe and should usually be created as constants,
 * {@link ArgumentBuilder}s on the other hand are created method-scoped and discarded after use. Example usage looks
 * like this:
 * </p>
 * 
 * <pre>
 * // create queries as constants
 * private static final PlaceholderQuery PLACEHOLDER = new PlaceholderQuery(
 *         &quot;SELECT * FROM table WHERE col1 = @value1 AND col2 = @value2&quot;);
 * 
 * // usage within method
 * Query query = PLACEHOLDER.newArgs().set(&quot;value1&quot;, 1).set(&quot;value2&quot;, 2).create();
 * List&lt;Result&gt; result = databaseManager.runQuery(CONVERTER, query);
 * </pre>
 * 
 * @author Philipp Katz
 */
public final class PlaceholderQuery {

    private static final String PLACEHOLDER_PATTERN = "@[A-Za-z0-9_]+";

    private final String originalQuery;
    private final String query;
    private final List<String> placeholders;

    /**
     * <p>
     * Create a new {@link PlaceholderQuery} from the given string. Placeholders start with an @ sign followed by
     * alphanumeric characters and underscores.
     * </p>
     * 
     * @param query The query, not <code>null</code> or empty.
     */
    public PlaceholderQuery(String query) {
        Validate.notEmpty(query, "query must not be empty");
        this.originalQuery = query;
        this.query = query.replaceAll(PLACEHOLDER_PATTERN, "?");
        this.placeholders = parsePlaceholders(query);
    }

    private static final List<String> parsePlaceholders(String query) {
        List<String> placeholders = new ArrayList<String>();
        Pattern pattern = Pattern.compile(PLACEHOLDER_PATTERN);
        Matcher matcher = pattern.matcher(query);
        while (matcher.find()) {
            placeholders.add(matcher.group().substring(1));
        }
        return Collections.unmodifiableList(placeholders);
    }

    List<String> getPlaceholders() {
        return placeholders;
    }

    /**
     * <p>
     * Get the SQL query for creating the PreparedStatement. In the query, all placeholders have been replaced by
     * question marks.
     * </p>
     * 
     * @return The SQL query.
     */
    public String getSql() {
        return query;
    }

    /**
     * <p>
     * Create a new {@link ArgumentBuilder} for assigning values to placeholders.
     * </p>
     * 
     * @return A new {@link ArgumentBuilder}.
     */
    public ArgumentBuilder newArgs() {
        return new ArgumentBuilder(query, placeholders);
    }

    @Override
    public String toString() {
        return originalQuery;
    }

    public static final class ArgumentBuilder {

        private final Map<String, Object> parameters = new HashMap<String, Object>();
        private final String sql;
        private final List<String> placeholders;

        private ArgumentBuilder(String sql, List<String> placeholders) {
            this.sql = sql;
            this.placeholders = placeholders;
        }

        /**
         * <p>
         * Set the placeholder in the query to the given value.
         * </p>
         * 
         * @param placeholder Name of the placeholder to set, not <code>null</code>.
         * @param value Value to set for the placeholder.
         * @return This instance for builder style.
         * @throws IllegalArgumentException In case the given placeholder does not exist in the query.
         */
        public ArgumentBuilder set(String placeholder, Object value) {
            Validate.notNull(placeholder, "key must not be null");
            if (placeholders.contains(placeholder)) {
                parameters.put(placeholder, value);
                return this;
            } else {
                throw new IllegalArgumentException("'" + placeholder + "' is not a placeholder");
            }
        }

        /**
         * <p>
         * Create the final argument list which can be supplied to the {@link DatabaseManager}.
         * </p>
         * 
         * @return The list of parameters in the correct order.
         * @throws IllegalStateException In case not all placeholders have been set.
         */
        public List<Object> createArgs() {
            List<Object> arguments = new ArrayList<Object>();
            for (String placeholder : placeholders) {
                if (parameters.containsKey(placeholder)) {
                    Object value = parameters.get(placeholder);
                    arguments.add(value);
                } else {
                    throw new IllegalStateException("Placeholder '" + placeholder + "' has not been set");
                }
            }
            return arguments;
        }

        /**
         * <p>
         * Create the final query which can be supplied to the {@link DatabaseManager}.
         * </p>
         * 
         * @return The query.
         * @throws IllegalStateException In case not all placeholders have been set.
         */
        public Query create() {
            return new BasicQuery(sql, createArgs());
        }

    }

}
