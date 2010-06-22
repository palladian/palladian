package tud.iir.knowledge;

/**
 * This class maps the data types (xsd) to regular expressions.
 * 
 * @author David Urbansky
 */
public class RegExp {

    // data types
    private static final String NUMBER = "(?<!(\\w)-)(?<!(\\w))((\\d){1,}((,|\\.|\\s))?){1,}(?!((\\d)+-(\\d)+))(?!-(\\d)+)";
    private static final String BOOLEAN = "(?<!(\\w))(?i)(yes|no)(?!(\\w))";
    private static final String STRING = "([A-Z.]{1}([A-Za-z-üäößãáàúùíìîéèê0-9.]*)(\\s)?)+([A-Z.0-9]+([A-Za-z-üäöáàúùíìêîã0-9.]*)(\\s)?)*";
    private static final String MIXED = "(.)*";
    private static final String URI = "(\\w)\\.(\\w)\\.(\\w){1,4}(\\/(\\w))*"; // TODO test
    private static final String IMAGE = "src=\"";
    private static final String DATE_ALL = "((\\d){4}-(\\d){2}-(\\d){2})|((\\d){1,2}[\\.|/|-](\\d){1,2}[\\.|/|-](\\d){1,4})|((?<!(\\d){2})(\\d){1,2}(th)?(\\.)?(\\s)?([A-Za-z]){3,9}((\\,)|(\\s))+(['])?(\\d){2,4})|((\\w){3,9}\\s(\\d){1,2}(th)?((\\,)|(\\s))+(['])?(\\d){2,4})"; // date
    // one
    // to
    // four
    // (order
    // is
    // important)

    public static final String ENTITY = "([A-Z]{1}([A-Za-z-üäößãáàúùíìîéèê0-9.]*))+((\\s)?[A-Z0-9]+([A-Za-z-üäöáàúùíìêîã0-9]*))*";

    // dates needed to normalize date found by general date pattern
    public static final String DATE0 = "(\\d){4}-(\\d){2}-(\\d){2} (\\d){2}:(\\d){2}:(\\d){2}"; // YYYY-MM-DD hh:mm:ss
    public static final String DATE1 = "(\\d){4}-(\\d){2}-(\\d){2}"; // YYYY-MM-DD
    public static final String DATE2 = "(\\d){1,2}[\\.|/|-](\\d){1,2}[\\.|/|-](\\d){1,4}"; // DD.MM.YYYY and varieties
    public static final String DATE3 = "(?<!(\\d){2})(\\d){1,2}(th)?(\\.)?(\\s)?([A-Za-z]){3,9}((\\,)|(\\s))+(['])?(\\d){2,4}"; // DD Monthname YYYY and
    // varieties
    public static final String DATE4 = "(\\w){3,9}\\s(\\d){1,2}(th)?((\\,)|(\\s))+(['])?(\\d){2,4}"; // Monthname DD YYYY and varieties

    // other patterns
    // public static final String COLON_FACT_REPRESENTATION =
    // "[A-Za-z0-9/ ]{1,20}:\\s?[0-9A-Za-z]{1,20}((\\s|,)+([0-9/,]*|[A-Z]*|[a-z]*))*([A-Za-z]{1}[a-z0-9,]*|[0-9]*)";
    // public static final String COLON_FACT_REPRESENTATION = "[A-Za-z0-9/ ]{1,20}:\\s?(([0-9]+|[A-Z]+|[a-z]+))+((\\s|,)+([0-9]+|[A-Z]+|[a-z]+))+";
    private static final String COLON_FACT_REPRESENTATION_VALUE = "([A-Z]+|[a-z]+|[0-9.]+[A-Z]{1,2}(\\s|,|$)|[0-9.]+[a-z]{1,4}|[0-9.]+)";
    public static final String COLON_FACT_REPRESENTATION = "[A-Za-z0-9/() ]{1,20}:\\s?(" + COLON_FACT_REPRESENTATION_VALUE + ")+((\\s|,)+"
            + COLON_FACT_REPRESENTATION_VALUE + ")*";

    public static String getRegExp(int valueType) {
        switch (valueType) {
            case Attribute.VALUE_NUMERIC:
                return NUMBER; // TODO include ranges? 3-5hours, TODO? do not match numbers in strings
            case Attribute.VALUE_DATE:
                return DATE_ALL;
            case Attribute.VALUE_STRING:
                // return "(.)*"; // TODO test if that in combination with string commons is better
                return STRING;
                // return "([A-Z.]{1}([A-Za-z-üäößãáàúùíìîéèê0-9.]{1,}){1,}(\\s,|,)?(\\s)?)+"; // TODO string list "," separated
            case Attribute.VALUE_MIXED:
                return MIXED; // TODO refine
            case Attribute.VALUE_IMAGE:
                return IMAGE; // TODO catch images
            case Attribute.VALUE_BOOLEAN:
                return BOOLEAN;
            case Attribute.VALUE_URI:
                return URI;
            default:
                return "(.)*";
        }
    }
}