package ws.palladian.extraction.entity;

/**
 * <p>
 * Different formats for named entity tagging a text.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public enum TaggingFormat {

    /**
     * Tag text with xml. For example: The Nexus One is expensive. => The &lt;PHONE&gt;Nexus One&lt;/PHONE&gt; is
     * expensive.
     */
    XML,

    /**
     * Tag text in two columns where the first column is the token and the second is the tag.
     * For example: The Nexus One is expensive. =><br>
     * The O
     * Nexus PHONE
     * One PHONE
     * is O
     * expensive O
     * . O
     */
    COLUMN,

    /** Tag text with brackets. For example: The Nexus One is expensive. => The [PHONE Nexus One ] is expensive. */
    BRACKETS,

    /** Tag text with brackets. For example: The Nexus One is expensive. => The Nexus/PHONE One/PHONE is expensive. */
    SLASHES;

}