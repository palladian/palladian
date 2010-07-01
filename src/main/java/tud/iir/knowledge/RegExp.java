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
    private static final String STRING = "([A-Z.]{1}([A-Za-z-Ã¼Ã¤Ã¶ÃŸÃ£Ã¡Ã ÃºÃ¹Ã­Ã¬Ã®Ã©Ã¨Ãª0-9.]*)(\\s)?)+([A-Z.0-9]+([A-Za-z-Ã¼Ã¤Ã¶Ã¡Ã ÃºÃ¹Ã­Ã¬ÃªÃ®Ã£0-9.]*)(\\s)?)*";
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

    public static final String ENTITY = "([A-Z]{1}([A-Za-z-Ã¼Ã¤Ã¶ÃŸÃ£Ã¡Ã ÃºÃ¹Ã­Ã¬Ã®Ã©Ã¨Ãª0-9.]*))+((\\s)?[A-Z0-9]+([A-Za-z-Ã¼Ã¤Ã¶Ã¡Ã ÃºÃ¹Ã­Ã¬ÃªÃ®Ã£0-9]*))*";

    // dates needed to normalize date found by general date pattern
    public static final String DATE0 = "(\\d){4}-(\\d){2}-(\\d){2} (\\d){2}:(\\d){2}:(\\d){2}"; // YYYY-MM-DD hh:mm:ss
    public static final String DATE1 = "(\\d){4}-(\\d){2}-(\\d){2}"; // YYYY-MM-DD
    public static final String DATE2 = "(\\d){1,2}[\\.|/|-](\\d){1,2}[\\.|/|-](\\d){1,4}"; // DD.MM.YYYY and varieties
    public static final String DATE3 = "(?<!(\\d){2})(\\d){1,2}(th)?(\\.)?(\\s)?([A-Za-z]){3,9}((\\,)|(\\s))+(['])?(\\d){2,4}"; // DD Monthname YYYY and
    // varieties
    public static final String DATE4 = "(\\w){3,9}\\s(\\d){1,2}(th)?((\\,)|(\\s))+(['])?(\\d){2,4}"; // Monthname DD YYYY and varieties
    
    //shortcuts
    public static final String YYYY 	= "((199[3-9])|(20(\\d){2}))"; 			// 1993-20xx
    public static final String YY		= "([']+(\\d){2})"; 					// 00-99
    public static final String MM		= "((0[1-9])|(1[0-2]))";				// 01-12
    public static final String DDD		= "((0(0[1-9])|([1-9][0-9]))|([12](//d){2})|(3([0-5][0-9])|(6[0-6])))"; // 001-366
    public static final String DD		= "((0[1-9])|([12][0-9])|(3[01]))";		// 01-31
    public static final String D		= "([1-7])";							// 1-7
    public static final String WW		= "((0[1-9])|([1-4][0-9])|(5[0-3]))";	// 01-53
    
    public static final String YYYY_YY = "(" + YYYY + "|" + YY + ")";			// YYYY|YY
    public static final String URL_SYM = "[/._]";								// [/._] - symbols in URLs
    
    //ISO8601
    public static final String DATE_ISO8601_YMD = YYYY_YY + "-" + MM + "-" + DD;// (YYYY|YY)-MM-DD
    public static final String DATE_ISO8601_YM  = YYYY_YY + "-" + MM ;			// (YYYY|YY)-MM
    public static final String DATE_ISO8601_YWD	= YYYY_YY + "-" + WW + "-" + D; // (YYYY|YY)-WW-D
    public static final String DATE_ISO8601_YW	= YYYY_YY + "-" + WW;			// (YYYY|YY)-WW
    public static final String DATE_ISO8601_YD 	= YYYY_YY + "-" + DDD;			// (YYYY|YY)-DDD
    
    //Dates in URL
    public static final String URL_DATE_D = YYYY_YY + URL_SYM + MM + URL_SYM + DD; // (YYYY|YY) [/._] MM [/._] DD
    public static final String URL_DATE = YYYY_YY + URL_SYM + MM ; 				   // (YYYY|YY) [/._] MM
    
    //all date-regExp in one array
    public static final String[] DATE_ARRAY = {	DATE_ISO8601_YD,DATE_ISO8601_YMD,DATE_ISO8601_YW,
    											URL_DATE};
    public static final String[] DATE_FORMAT ={ "ISO8601_YD", "ISO8601_YMD", "ISO8601_YW",
    											"URL_DATES"};
    
    
    /*
    //RegExp as optional
    public static String regExpAsOpt(String regExp){
    	return "(" + regExp + ")+";
    }
    */
    
    	
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
                // return "([A-Z.]{1}([A-Za-z-Ã¼Ã¤Ã¶ÃŸÃ£Ã¡Ã ÃºÃ¹Ã­Ã¬Ã®Ã©Ã¨Ãª0-9.]{1,}){1,}(\\s,|,)?(\\s)?)+"; // TODO string list "," separated
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