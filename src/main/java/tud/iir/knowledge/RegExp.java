package tud.iir.knowledge;

/**
 * This class maps the data types (xsd) to regular expressions.
 * 
 * @author David Urbansky
 * @author Martin Gregor
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
    public static final String DATE3 = "(?<!(\\d){2})(\\d){1,2}(th)?(\\.)?(\\s)?([A-Za-z]){3,9}((\\,)|(\\s))+(['])?(\\d){2,4}"; // DD
    // Monthname
    // YYYY
    // and
    // varieties
    public static final String DATE4 = "(\\w){3,9}\\s(\\d){1,2}(th)?((\\,)|(\\s))+(['])?(\\d){2,4}"; // Monthname DD
    // YYYY and
    // varieties

    // shortcuts
    private static final String LONG_YEAR = "((199[3-9])|(20(\\d){2}))"; // 1993-20xx
    private static final String SHOR_YEAR = "((\\d){2})"; // 00-99
    private static final String MONTH_NUMBER = "((0[1-9])|(1[0-2]))"; // 01-12
    private static final String MONTH_NAME_SHORT_ENG = "((Jan)|(Feb)|(Mar)|(Apr)|(May)|(Jun)|(Jul)|(Aug)|(Sep)|(Oct)|(Nov)|(Dec))";
    private static final String MONTH_NAME_LONG_ENG = "((January)|(February)|(March)|(April)|(May)|(June)|(July)|(August)|(September)|"
            + "(October)|(November)|(December))";
    private static final String MONTH_NAME_LONG_DT = "((Januar)|(Februar)|(März)|(April)|(Mai)|(Juni)|(Juli)|(August)|(September)|"
            + "(Oktober)|(November)|(Dezember))";
    private static final String MONTH_NAME_LONG = "(" + MONTH_NAME_LONG_ENG + ")|(" + MONTH_NAME_LONG_DT + ")";
    private static final String DAY_01_99 = "((0[1-9])|([1-9][0-9]))";
    private static final String DAY_00_99 = "([0-9][0-9])";
    private static final String DAY_001_099 = "(0" + DAY_01_99 + ")";
    private static final String DAY_100_299 = "([12]" + DAY_00_99 + ")";
    private static final String DAY_300_366 = "(3(([0-5][0-9])|(6[0-6])))";
    private static final String DAY_OF_YEAR = "(" + DAY_001_099 + "|" + DAY_100_299 + "|" + DAY_300_366 + ")";
    // "((0(0[1-9])|([1-9][0-9]))|" + "([12](//d){2})|(3([0-5][0-9])|(6[0-6])))"; // 001-366
    private static final String DAY_OF_MONTH = "((0[1-9])|([12][0-9])|(3[01]))"; // 01-31
    private static final String DAY_OF_MONTH_1 = "(([1-9])|([12][0-9])|(3[01]))"; // 1-31 one or two digits
    private static final String DAY_OF_WEEK = "([1-7])"; // 1-7
    private static final String WEEK_OF_YEAR = "(W((0[1-9])|([1-4][0-9])|(5[0-3])))"; // W01-W53
    private static final String WEEKDAY_NAME_SHORT = "((Mon)|(Tue)|(Wed)|(Thu)|(Fri)|(Sat)|(Sun))";
    private static final String WEEKDAY_NAME_LONG = "((Monday)|(Tuesday)|(Wednesday)|(Thursday)|(Friday)|(Saturday)|(Sunday))";

    private static final String HOUR = "((0[0-9])|(1[0-9])|(2[0-4]))";
    private static final String MIN = "((0[0-9])|([1-5][0-9]))";
    private static final String SEC = MIN;
    private static final String TIMEZONE = "((UTC)|(MEZ)|(GMT))";
    private static final String TIME_SEC = HOUR + ":" + MIN + ":" + SEC;
    private static final String FLOAT_SEC_OPT = "(((\\.)(\\d)*)?)";
    private static final String TIME = "(" + HOUR + "(:" + MIN + "(:" + SEC + FLOAT_SEC_OPT + ")?)?)";
    private static final String DIFF_UTC = "(((\\+)|(-))" + HOUR + "(:" + MIN + ")?)";
    private static final String ISO_TIME = "(((T)|(\\s))" + TIME + "(" + DIFF_UTC + "|(Z))?)";

    private static final String APOSROPH = "('?)";
    private static final String YEAR_SHORT_LONG = "(" + LONG_YEAR + "|(" + APOSROPH + SHOR_YEAR + "))"; // YYYY|(')?YY
    private static final String URL_SYM = "[/._]"; // [/._] - symbols in URLs

    // RegExp are a array with 2 fields, field one is the regExp; field two is the format
    // ISO8601
    /** <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO8601<a> YYYY-MM-DD TIME+UTC. */
    public static final String[] DATE_ISO8601_YMD_T = {
            YEAR_SHORT_LONG + "-" + MONTH_NUMBER + "-" + DAY_OF_MONTH + ISO_TIME, "YYYY-MM-DDTHH:MM:SS+HH:MM" };
    /** ISO8601 YYYY-MM-DD . */
    public static final String[] DATE_ISO8601_YMD = { YEAR_SHORT_LONG + "-" + MONTH_NUMBER + "-" + DAY_OF_MONTH,
            "YYYY-MM-DD" };
    /** ISO8601 YYYY-MM . */
    public static final String[] DATE_ISO8601_YM = { YEAR_SHORT_LONG + "-" + MONTH_NUMBER, "YYYY-MM" };
    /** ISO8601 YYYY-WW-D TIME+UTC . */
    public static final String[] DATE_ISO8601_YWD_T = {
            YEAR_SHORT_LONG + "-" + WEEK_OF_YEAR + "-" + DAY_OF_WEEK + ISO_TIME, "YYYY-WW-DTHH:MM:SS+HH:MM" };
    /** ISO8601 YYYY-WW-D . */
    public static final String[] DATE_ISO8601_YWD = { YEAR_SHORT_LONG + "-" + WEEK_OF_YEAR + "-" + DAY_OF_WEEK,
            "YYYY-WW-D" };
    /** ISO8601 YYYY-WW . */
    public static final String[] DATE_ISO8601_YW = { YEAR_SHORT_LONG + "-" + WEEK_OF_YEAR, "YYYY-WW" };
    /** ISO8601 YYYY-DDD TIME+UTC. */
    public static final String[] DATE_ISO8601_YD_T = { YEAR_SHORT_LONG + "-" + DAY_OF_YEAR + ISO_TIME,
            "YYYY-DDDTHH:MM:SS+HH:MM" };
    /** ISO8601 YYYY-DDD . */
    public static final String[] DATE_ISO8601_YD = { YEAR_SHORT_LONG + "-" + DAY_OF_YEAR, "YYYY-DDD" };
    /**
     * Year, month and day written without separator.<br>
     * YYYYMMMDD
     */
    public static final String[] DATE_ISO8601_YMD_NO = { LONG_YEAR + MONTH_NUMBER + DAY_OF_MONTH, "YYYYMMDD" };
    /**
     * Year, month and day written without separator.<br>
     * YYYYWWD
     */
    public static final String[] DATE_ISO8601_YWD_NO = { YEAR_SHORT_LONG + WEEK_OF_YEAR + DAY_OF_WEEK, "YYYYWWD" };
    /**
     * Year and month written without separator.<br>
     * YYYYWW
     */
    public static final String[] DATE_ISO8601_YW_NO = { YEAR_SHORT_LONG + WEEK_OF_YEAR, "YYYYWW" };
    /**
     * Year and month written without separator.<br>
     * YYYYDDD
     */
    public static final String[] DATE_ISO8601_YD_NO = { YEAR_SHORT_LONG + DAY_OF_YEAR, "YYYYDDD" };

    // Possible dates in URLs.
    /**
     * Dates in URL. YYYY_MM_DD .<br>
     * "_" can also be "." or "-"
     */
    public static final String[] DATE_URL_D = { YEAR_SHORT_LONG + URL_SYM + MONTH_NUMBER + URL_SYM + DAY_OF_MONTH,
            "YYYY_MM_DD" };
    /**
     * Dates in URL. YYYY_MM .<br>
     * "_" can also be "." or "-"
     */
    public static final String[] DATE_URL = { YEAR_SHORT_LONG + URL_SYM + MONTH_NUMBER, "YYYY_MM" };

    /** Date in URL, that can be split by folders between year an month. */
    public static final String[] DATE_URL_SPLIT = { LONG_YEAR + "/(.)+/" + MONTH_NUMBER + URL_SYM + DAY_OF_MONTH,
            "YYYY.x.MM.DD" };

    // Europeandates also used worldwide
    /** European date. DD.MM.YYYY . */
    public static final String[] DATE_EU_D_MM_Y = { DAY_OF_MONTH + "." + MONTH_NUMBER + "." + YEAR_SHORT_LONG,
            "DD.MM.YYYY" };
    /** European date. MM.YYYY . */
    public static final String[] DATE_EU_MM_Y = { MONTH_NUMBER + "." + YEAR_SHORT_LONG, "MM.YYYY" };
    /** European date. DD.MM . */
    public static final String[] DATE_EU_D_MM = { DAY_OF_MONTH + "." + MONTH_NUMBER + ".", "DD.MM." };
    /** European date. DD. MMMM YYYY . */
    public static final String[] DATE_EU_D_MMMM_Y = { DAY_OF_MONTH + ". " + MONTH_NAME_LONG + " " + YEAR_SHORT_LONG,
            "DD. MMMM YYYY" };
    /** European date. DD.MMMM . */
    public static final String[] DATE_EU_D_MMMM = { DAY_OF_MONTH + ". " + MONTH_NAME_LONG, "DD.MMMM" };

    // US dates
    /** American date. MM/DD/YYYY . */
    public static final String[] DATE_USA_MM_D_Y = { MONTH_NUMBER + "/" + DAY_OF_MONTH + "/" + YEAR_SHORT_LONG,
            "MM/DD/YYYY" };
    /** American date. MM/YYYY . */
    public static final String[] DATE_USA_MM_Y = { MONTH_NUMBER + "/" + YEAR_SHORT_LONG, "MM/YYYY" };
    /** American date. MM/DD . */
    public static final String[] DATE_USA_MM_D = { MONTH_NUMBER + "/" + DAY_OF_MONTH + "/", "MM/DD" };
    /** American date. MMMM DD, YYYY . */
    public static final String[] DATE_USA_MMMM_D_Y = { MONTH_NAME_LONG + " " + DAY_OF_MONTH + ", " + YEAR_SHORT_LONG,
            "MMMM DD, YYYY" };
    /** American date. MMMM DD . */
    public static final String[] DATE_USA_MMMM_D = { MONTH_NAME_LONG + " " + DAY_OF_MONTH, "MMMM DD" };

    // US and European dates
    /** American and European date. "MMMM YYYY . */
    public static final String[] DATE_EUSA_MMMM_Y = { MONTH_NAME_LONG + " " + YEAR_SHORT_LONG, "MMMM YYYY" };

    /** RFC 1123. WD, DD MMM YYYY HH:MM:SS TZ . */
    public static final String[] DATE_RFC_1123 = {
            WEEKDAY_NAME_SHORT + ", " + DAY_OF_MONTH + " " + MONTH_NAME_SHORT_ENG + " " + LONG_YEAR + " " + TIME_SEC
                    + " " + TIMEZONE, "WD, DD MMM YYYY HH:MM:SS TZ" };
    /** RFC 1036. WWD, DD-MMM-YYYY HH:MM:SS TZ . */
    public static final String[] DATE_RFC_1036 = {
            WEEKDAY_NAME_LONG + ", " + DAY_OF_MONTH + "-" + MONTH_NAME_SHORT_ENG + "-" + SHOR_YEAR + " " + TIME_SEC
                    + " " + TIMEZONE, "WWD, DD-MMM-YY HH:MM:SS TZ" };
    /** ANSI C's ascitime. WD MMM DD_1 HH:MM:SS YYYY . */
    public static final String[] DATE_ANSI_C = {
            WEEKDAY_NAME_SHORT + " " + MONTH_NAME_SHORT_ENG + " " + DAY_OF_MONTH_1 + " " + TIME_SEC + " " + LONG_YEAR,
            "WD MMM DD_1 HH:MM:SS YYYY" };
    /** ANSI C's ascitime with time difference to UTC. WD MMM DD_1 HH:MM:SS YYYY +TZ. */
    public static final String[] DATE_ANSI_C_TZ = {
            WEEKDAY_NAME_SHORT + " " + MONTH_NAME_SHORT_ENG + " " + DAY_OF_MONTH_1 + " " + TIME_SEC + " " + LONG_YEAR
                    + " " + DIFF_UTC, "WD MMM DD_1 HH:MM:SS YYYY +TZ" };

    // other dateformates

    /*
     * //RegExp as optional
     * public static String regExpAsOpt(String regExp){
     * return "(" + regExp + ")+";
     * }
     */

    // other patterns
    // public static final String COLON_FACT_REPRESENTATION =
    // "[A-Za-z0-9/ ]{1,20}:\\s?[0-9A-Za-z]{1,20}((\\s|,)+([0-9/,]*|[A-Z]*|[a-z]*))*([A-Za-z]{1}[a-z0-9,]*|[0-9]*)";
    // public static final String COLON_FACT_REPRESENTATION =
    // "[A-Za-z0-9/ ]{1,20}:\\s?(([0-9]+|[A-Z]+|[a-z]+))+((\\s|,)+([0-9]+|[A-Z]+|[a-z]+))+";
    private static final String COLON_FACT_REPRESENTATION_VALUE = "([A-Z]+|[a-z]+|[0-9.]+[A-Z]{1,2}(\\s|,|$)|[0-9.]+[a-z]{1,4}|[0-9.]+)";
    public static final String COLON_FACT_REPRESENTATION = "[A-Za-z0-9/() ]{1,20}:\\s?("
            + COLON_FACT_REPRESENTATION_VALUE + ")+((\\s|,)+" + COLON_FACT_REPRESENTATION_VALUE + ")*";

    public static String getRegExp(int valueType) {
        switch (valueType) {
            case Attribute.VALUE_NUMERIC:
                return NUMBER; // TODO include ranges? 3-5hours, TODO? do not match numbers in strings
            case Attribute.VALUE_DATE:
                return DATE_ALL;
            case Attribute.VALUE_STRING:
                // return "(.)*"; // TODO test if that in combination with string commons is better
                return STRING;
                // return "([A-Z.]{1}([A-Za-z-Ã¼Ã¤Ã¶ÃŸÃ£Ã¡Ã ÃºÃ¹Ã­Ã¬Ã®Ã©Ã¨Ãª0-9.]{1,}){1,}(\\s,|,)?(\\s)?)+"; // TODO
                // string list "," separated
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

    /**
     * <h1>For URL-dates.</h1><br>
     * Get an ordered array of <a title="String[] = {regular expression, description}" >
     * <u>regular expressions</u> </a> <br>
     * to match the longest possible string.<br>
     * <br>
     * We need order because short regular expression matches also longer ones. <br>
     * E.g.: So we get for 2010-07-20 a match for YYYY-MM and YYYY-MM-DD. But last one would be more specific.
     * 
     * @return Array with <a title="String[] = {regular expression, description}" > <u>regular expressions</u> </a>
     */
    public static Object[] getURLRegExp() {
        final Object[] regExp = { RegExp.DATE_ISO8601_YMD, RegExp.DATE_URL_D, RegExp.DATE_ISO8601_YMD_NO,
                RegExp.DATE_URL_SPLIT, RegExp.DATE_ISO8601_YWD, RegExp.DATE_ISO8601_YD, RegExp.DATE_ISO8601_YM,
                RegExp.DATE_URL, RegExp.DATE_ISO8601_YW };
        return regExp;
    }

    /**
     * <h1>For HTTP-Header-dates.</h1><br>
     * 
     * Get an ordered array of <a title="String[] = {regular expression, description}" > <u>regular expressions</u> </a> <br>
     * to match the longest possible string.<br>
     * <br>
     * We need order because short regular expression matches also longer ones. <br>
     * E.g.: So we get for 2010-07-20 a match for YYYY-MM and YYYY-MM-DD. But last one would be more specific.
     * 
     * 
     * @return Array with <a title="String[] = {regular expression, description}" > <u>regular expressions</u> </a>
     */
    public static Object[] getHTTPRegExp() {
        final Object[] regExp = { RegExp.DATE_RFC_1036, RegExp.DATE_RFC_1123, RegExp.DATE_ANSI_C_TZ, RegExp.DATE_ANSI_C };
        return regExp;
    }

    /**
     * <h1>For HTML-head-dates..</h1><br>
     * Get an ordered array of <a title="String[] = {regular expression, description}" > <u>regular expressions</u> </a> <br>
     * to match the longest possible string.<br>
     * <br>
     * We need order because short regular expression matches also longer ones. <br>
     * E.g.: So we get for 2010-07-20 a match for YYYY-MM and YYYY-MM-DD. But last one would be more specific.
     * 
     * 
     * @return Array with <a title="String[] = {regular expression, description}" > <u>regular expressions</u> </a>
     */
    public static Object[] getHEADRegExp() {
        final Object[] regExp = { RegExp.DATE_RFC_1123, RegExp.DATE_RFC_1036, RegExp.DATE_ANSI_C_TZ,
                RegExp.DATE_ANSI_C, RegExp.DATE_ISO8601_YMD_T, RegExp.DATE_ISO8601_YMD, RegExp.DATE_ISO8601_YWD,
                RegExp.DATE_ISO8601_YD, RegExp.DATE_ISO8601_YM, RegExp.DATE_ISO8601_YW };
        return regExp;
    }
}