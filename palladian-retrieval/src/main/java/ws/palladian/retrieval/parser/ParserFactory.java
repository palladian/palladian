package ws.palladian.retrieval.parser;

/**
 * <p>
 * A factory for obtaining {@link DocumentParser}.
 * </p>
 * 
 * @author Philipp Katz
 * 
 */
public final class ParserFactory {

    private ParserFactory() {
        // prevent instantiation.
    }

    /**
     * <p>
     * Create an (X)HTML parser, which can be used for parsing potentially dirty and mal-formed (X)HTML content to valid
     * DOM documents.
     * </p>
     * 
     * @return
     */
    public static DocumentParser createHtmlParser() {
        return new ValidatorNuParser();
    }

    /**
     * <p>
     * Create and XML parser, which can be used for parsing well-formed XML content to DOM documents.
     * </p>
     * 
     * @return
     */
    public static DocumentParser createXmlParser() {
        return new XmlParser();
    }

}
