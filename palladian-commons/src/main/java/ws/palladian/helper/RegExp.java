package ws.palladian.helper;

/**
 * This class maps the data types (xsd) to regular expressions.<br>
 * <br>
 * Also holds possible date strings as regular expressions. If you enter new ones, make sure you add it to the correct
 * get-method at the correct position.
 * 
 * @author David Urbansky
 * @author Martin Gregor
 */
public class RegExp {

    // TODO: Warning (10/11/2010): changed regexp without further testing, was:
    // ([A-Z]{1}([A-Za-z-üäößãáàúùíìîéèê0-9.]*))+(( )?[A-Z0-9]+([A-Za-z-üäößãáàúùíìîéèê0-9]*))*
    public static final String ENTITY = "([A-Z]{1}([A-Za-z-üäößãáàúùíìîéèê0-9.]*))+(( )?[A-Z0-9]+([A-Za-z-üäößãáàúùíìîéèê0-9]*)){0,10}";

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

    public static final String NUMBER = "(?<!(\\w)-)(?<!(\\w))((\\d){1,}((,|\\.|\\s))?){1,}(?!((\\d)+-(\\d)+))(?!-(\\d)+)";
    public static final String BOOLEAN = "(?<!(\\w))(?i)(yes|no)(?!(\\w))";
    // TODO catch special chars differently
    public static final String STRING = "([A-Z.]{1}([A-Za-z-üäößãáàúùíìîéèê0-9.]*)(\\s)?)+([A-Z.0-9]+([A-Za-z-üäößãáàúùíìîéèê0-9.]*)(\\s)?)*";
    public static final String MIXED = "(.)*";
    public static final String URI = "(\\w)\\.(\\w)\\.(\\w){1,4}(\\/(\\w))*"; // TODO test
    public static final String IMAGE = "src=\"";
    public static final String DATE_ALL = "((\\d){4}-(\\d){2}-(\\d){2})|((\\d){1,2}[\\.|/|-](\\d){1,2}[\\.|/|-](\\d){1,4})|((?<!(\\d){2})(\\d){1,2}(th)?(\\.)?(\\s)?([A-Za-z]){3,9}((\\,)|(\\s))+(['])?(\\d){2,4})|((\\w){3,9}\\s(\\d){1,2}(th)?((\\,)|(\\s))+(['])?(\\d){2,4})"; // date

    // shortcuts
    private static final String LONG_YEAR = "((\\d){4})";
    private static final String SHOR_YEAR = "((\\d){2})"; // 00-99
    private static final String MONTH_NUMBER_DOUBLE = "((0[1-9])|(1[0-2]))"; // 01-12
    private static final String MONTH_NUMBER_NORMAL = "((1[0-2])|(0?[1-9]))"; // (0)1-12
    private static final String MONTH_NAME_SHORT_DT = "((Jan)|(Feb)|(Mär)|(Apr)|(Mai)|(Jun)|(Jul)|(Aug)|(Sep)|(Okt)|(Nov)|(Dez)|(JAN)|(FEB)|(MÄR)|(APR)|(MAI)|(JUN)|(JUL)|(AUG)|(SEP)|(OKT)|(NOV)|(DEZ)|(jan)|(feb)|(mär)|(apr)|(mai)|(jun)|(jul)|(aug)|(sep)|(okt)|(nov)|(dez))";
    private static final String MONTH_NAME_SHORT_ENG = "((Jan)|(Feb)|(Mar)|(Apr)|(May)|(Jun)|(Jul)|(Aug)|(Sep)|(Sept)|(Oct)|(Nov)|(Dec)|"
        + "(JAN)|(FEB)|(MAR)|(APR)|(MAY)|(JUN)|(JUL)|(AUG)|(SEP)|(SEPT)|(OCT)|(NOV)|(DEC)|"
        + "(jan)|(feb)|(mar)|(apr)|(may)|(jun)|(jul)|(aug)|(sep)|(sept)|(oct)|(nov)|(dec))";
    private static final String MONTH_NAME_SHORT = "(((" + MONTH_NAME_SHORT_ENG + ")|(" + MONTH_NAME_SHORT_DT
    + "))(\\.)?)";
    private static final String MONTH_NAME_SHORT2 = "((" + MONTH_NAME_SHORT_ENG + ")|(" + MONTH_NAME_SHORT_DT + "))";
    private static final String MONTH_NAME_LONG_ENG = "((January)|(February)|(March)|(April)|(May)|(June)|(July)|(August)|(September)|"
        + "(October)|(November)|(December)|(january)|(february)|(march)|(april)|(may)|(june)|(july)|(august)|(september)|"
        + "(october)|(november)|(december)|(JANUARY)|(FEBRUARY)|(MARCH)|(APRIL)|(MAY)|(JUNE)|(JULY)|(AUGUST)|(SEPTEMBER)|"
        + "(OCTOBER)|(NOVEMBER)|(DECEMBER))";
    private static final String MONTH_NAME_LONG_DT = "((Januar)|(Februar)|(März)|(April)|(Mai)|(Juni)|(Juli)|(August)|(September)|"
        + "(Oktober)|(November)|(Dezember)|(januar)|(februar)|(märz)|(april)|(mai)|(juni)|(juli)|(august)|(september)|"
        + "(oktober)|(november)|(dezember)|(JANUAR)|(FEBRUAR)|(MÄRZ)|(APRIL)|(MAI)|(JUNI)|(JULY)|(AUGUST)|(SEPTEMBER)|"
        + "(OKTOBER)|(NOVEMBER)|(DEZEMBER))";
    private static final String MONTH_NAME_LONG = "((" + MONTH_NAME_LONG_ENG + ")|(" + MONTH_NAME_LONG_DT + ")|("
    + MONTH_NAME_SHORT + "))";
    private static final String MONTH_NAME_LONG2 = "((" + MONTH_NAME_LONG_ENG + ")|(" + MONTH_NAME_LONG_DT + ")|("
    + MONTH_NAME_SHORT2 + "))";
    private static final String DAY_01_99 = "((0[1-9])|([1-9][0-9]))";
    private static final String DAY_00_99 = "([0-9][0-9])";
    private static final String DAY_001_099 = "(0" + DAY_01_99 + ")";
    private static final String DAY_100_299 = "([12]" + DAY_00_99 + ")";
    private static final String DAY_300_366 = "(3(([0-5][0-9])|(6[0-6])))";
    private static final String DAY_OF_YEAR = "(" + DAY_001_099 + "|" + DAY_100_299 + "|" + DAY_300_366 + ")";
    // "((0(0[1-9])|([1-9][0-9]))|" + "([12](//d){2})|(3([0-5][0-9])|(6[0-6])))"; // 001-366
    private static final String DAY_OF_MONTH = "((0[1-9])|([12][0-9])|(3[01]))"; // 01-31
    private static final String DAY_OF_MONTH_1 = "(([1-9])|([12][0-9])|(3[01]))"; // 1-31 one or two digits
    private static final String DAY_OF_MONTH_1_2 = "((" + DAY_OF_MONTH + ")|(" + DAY_OF_MONTH_1 + "))";
    // private static final String DAY_OF_MONTH_normal = "(([12][0-9])|(3[01])|(0?[1-9]))"; // (01-09)|(1-9)
    // to 31
    private static final String DAY_OF_WEEK = "([1-7])"; // 1-7
    private static final String WEEK_OF_YEAR = "(W((0[1-9])|([1-4][0-9])|(5[0-3])))"; // W01-W53
    private static final String WEEKDAY_NAME_SHORT = "((Mon)|(Tue)|(Wed)|(Thu)|(Fri)|(Sat)|(Sun))";
    private static final String WEEKDAY_NAME_LONG = "((Monday)|(Tuesday)|(Wednesday)|(Thursday)|(Friday)|(Saturday)|(Sunday))";

    private static final String HOUR = "((1[0-9])|(2[0-4])|(0[0-9]))";
    private static final String HOUR12 = "((1[0-2])|(0[0-9]))";
    private static final String HOUR_1 = "((1[0-9])|(2[0-4])|([0-9]))";
    private static final String HOUR12_1 = "((1[0-2])|([0-9]))";
    private static final String MIN = "((0[0-9])|([1-5][0-9]))";
    private static final String SEC = MIN;
    public static final String TIMEZONE = "((\\s)((\\sUTC)|(UTC)|(MEZ)|(GMT)|(Z)|(AEST)|(BST)|(EST)))";
    private static final String TIME_SEC = HOUR + ":" + MIN + ":" + SEC;
    private static final String FLOAT_SEC_OPT = "(((\\.)(\\d)*)?)";
    private static final String AM_PM = "(" + "(\\s)" + "((AM)|(PM)))";
    private static final String TIME24 = "(" + HOUR + "(:" + MIN + "(:" + SEC + FLOAT_SEC_OPT + ")?)?)";
    private static final String TIME24_1 = "(" + HOUR_1 + "(:" + MIN + "(:" + SEC + FLOAT_SEC_OPT + ")?)?)";
    private static final String TIME12 = "((" + HOUR12 + "(:" + MIN + "(:" + SEC + FLOAT_SEC_OPT + ")?)?)" + AM_PM + "?)";
    private static final String TIME12_1 = "((" + HOUR12_1 + "(:" + MIN + "(:" + SEC + FLOAT_SEC_OPT + ")?)?)" + AM_PM + "?)";
    private static final String TIME = "(" + TIME12 + "|" + TIME24 + "|" + TIME12_1 + "|" + TIME24_1 + ")";
    private static final String TIME_SEPARATOR = "((\\s)|(\\s)/(\\s))";
    private static final String GMT_opt = "((\\s)?((GMT)|(UTC)|(Z))?)";
    private static final String DIFF_UTC = "(" + GMT_opt + "(\\s)?((\\+)|(-))" + HOUR + "((:)?" + MIN + ")?)";
    private static final String ISO_TIME = "(((T)|(\\s))" + TIME24 + "(" + DIFF_UTC + "|(Z))?)";

    private static final String APOSROPH = "('?)";
    private static final String ST_ND_RD_TH_OPT = "(((st)|(nd)|(rd)|(th))?)";
    private static final String YEAR_SHORT_LONG = "(" + LONG_YEAR + "|(" + APOSROPH + SHOR_YEAR + "))"; // YYYY|(')?YY
    private static final String URL_SYM = "[/\\._-]"; // [/._-] - symbols in URLs
    private static final String SEP_SYM = "([/\\._-])"; // [/._-] - symbols to separate dateparts

    // RegExp are a array with 2 fields, field one is the regExp; field two is the format
    // ISO8601

    /** Years in context. */
    public static final String[] DATE_CONTEXT_YYYY = {"(?<=(in|of|from|year|until|through|during)\\s)[0-9]{4}", "YYYY"};

    /** <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO8601<a> YYYY-MM-DD TIME+UTC. */
    public static final String[] DATE_ISO8601_YMD_T = {
        YEAR_SHORT_LONG + "-" + MONTH_NUMBER_DOUBLE + "-" + DAY_OF_MONTH + ISO_TIME, "YYYY-MM-DDTHH:MM:SS+HH:MM" };
    /** <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO8601<a> YYYY-MM-DD TIME+UTC. */

    private static final String DATE_ISO8601_YMD_SEPARATOR_T_1 = YEAR_SHORT_LONG + "/" + MONTH_NUMBER_DOUBLE + "/"
    + DAY_OF_MONTH + ISO_TIME;
    private static final String DATE_ISO8601_YMD_SEPARATOR_T_2 = YEAR_SHORT_LONG + "\\." + MONTH_NUMBER_DOUBLE + "\\."
    + DAY_OF_MONTH + ISO_TIME;
    private static final String DATE_ISO8601_YMD_SEPARATOR_T_3 = YEAR_SHORT_LONG + "_" + MONTH_NUMBER_DOUBLE + "_"
    + DAY_OF_MONTH + ISO_TIME;

    public static final String[] DATE_ISO8601_YMD_SEPARATOR_T = {
        "((" + DATE_ISO8601_YMD_SEPARATOR_T_1 + ")|(" + DATE_ISO8601_YMD_SEPARATOR_T_2 + ")|("
        + DATE_ISO8601_YMD_SEPARATOR_T_3 + "))", "YYYY-MM-DDTHH:MM:SS+HH:MM" };
    /** ISO8601 YYYY-MM-DD . */
    public static final String[] DATE_ISO8601_YMD = { YEAR_SHORT_LONG + "-" + MONTH_NUMBER_DOUBLE + "-" + DAY_OF_MONTH,
    "YYYY-MM-DD" };

    private static final String DATE_ISO8601_YMD_SEPARATOR_1 = YEAR_SHORT_LONG + "/" + MONTH_NUMBER_DOUBLE + "/"
    + DAY_OF_MONTH;
    private static final String DATE_ISO8601_YMD_SEPARATOR_2 = YEAR_SHORT_LONG + "\\." + MONTH_NUMBER_DOUBLE + "\\."
    + DAY_OF_MONTH;
    private static final String DATE_ISO8601_YMD_SEPARATOR_3 = YEAR_SHORT_LONG + "_" + MONTH_NUMBER_DOUBLE + "_"
    + DAY_OF_MONTH;

    /** ISO8601 YYYY-MM-DD . */
    public static final String[] DATE_ISO8601_YMD_SEPARATOR = {
        "((" + DATE_ISO8601_YMD_SEPARATOR_1 + ")|(" + DATE_ISO8601_YMD_SEPARATOR_2 + ")|("
        + DATE_ISO8601_YMD_SEPARATOR_3 + "))", "YYYY-MM-DD" };
    
    
    /** ISO8601 YYYY-MM . */
    public static final String[] DATE_ISO8601_YM = { YEAR_SHORT_LONG + "-" + MONTH_NUMBER_DOUBLE, "YYYY-MM" };
    /** ISO8601 YYYY-WW-D TIME+UTC . */
    public static final String[] DATE_ISO8601_YWD_T = { LONG_YEAR + "-" + WEEK_OF_YEAR + "-" + DAY_OF_WEEK + ISO_TIME,
    "YYYY-WW-DTHH:MM:SS+HH:MM" };
    /** ISO8601 YYYY-WW-D . */
    public static final String[] DATE_ISO8601_YWD = { LONG_YEAR + "-" + WEEK_OF_YEAR + "-" + DAY_OF_WEEK, "YYYY-WW-D" };
    /** ISO8601 YYYY-WW . */
    public static final String[] DATE_ISO8601_YW = { LONG_YEAR + "-" + WEEK_OF_YEAR, "YYYY-WW" };
    /** ISO8601 YYYY-DDD TIME+UTC. */
    public static final String[] DATE_ISO8601_YD_T = { LONG_YEAR + "-" + DAY_OF_YEAR + ISO_TIME,
    "YYYY-DDDTHH:MM:SS+HH:MM" };
    /** ISO8601 YYYY-DDD . */
    public static final String[] DATE_ISO8601_YD = { LONG_YEAR + "-" + DAY_OF_YEAR, "YYYY-DDD" };
    /**
     * Year, month and day written without separator.<br>
     * YYYYMMMDD
     */
    public static final String[] DATE_ISO8601_YMD_NO = { LONG_YEAR + MONTH_NUMBER_DOUBLE + DAY_OF_MONTH, "YYYYMMDD" };
    /**
     * Year, month and day written without separator.<br>
     * YYYYWWD
     */
    public static final String[] DATE_ISO8601_YWD_NO = { LONG_YEAR + WEEK_OF_YEAR + DAY_OF_WEEK, "YYYYWWD" };
    /**
     * Year and month written without separator.<br>
     * YYYYWW
     */
    public static final String[] DATE_ISO8601_YW_NO = { LONG_YEAR + WEEK_OF_YEAR, "YYYYWW" };
    /**
     * Year and month written without separator.<br>
     * YYYYDDD
     */
    public static final String[] DATE_ISO8601_YD_NO = { LONG_YEAR + DAY_OF_YEAR, "YYYYDDD" };

    // Possible dates in URLs.
    /**
     * Dates in URL. YYYY_MM_DD .<br>
     * "_" can also be "." or "-"
     */
    private static final String DATE_URL_D_1 = YEAR_SHORT_LONG + "(/)" + MONTH_NUMBER_DOUBLE + "(/)" + DAY_OF_MONTH
    + "(/)";
    private static final String DATE_URL_D_2 = YEAR_SHORT_LONG + "(_)" + MONTH_NUMBER_DOUBLE + "(_)" + DAY_OF_MONTH;
    private static final String DATE_URL_D_3 = YEAR_SHORT_LONG + "(\\.)" + MONTH_NUMBER_DOUBLE + "(\\.)" + DAY_OF_MONTH;
    private static final String DATE_URL_D_4 = YEAR_SHORT_LONG + "(-)" + MONTH_NUMBER_DOUBLE + "(-)" + DAY_OF_MONTH;
    /**
     * Dates in URL. YYYY_MM_DD .<br>
     * "_" can also be "." or "-" or "/"
     */
    public static final String[] DATE_URL_D = { "((" + DATE_URL_D_1 + ")|(" + DATE_URL_D_2 + ")|(" + DATE_URL_D_3 + ")|(" + DATE_URL_D_4 + "))", "YYYY_MM_DD" };
    /**
     * Dates in URL. YYYY_MM_DD .<br>
     * "_" can also be "." or "-" or "/"
     */
    public static final String[] DATE_URL_MMMM_D = { YEAR_SHORT_LONG + "(/)" + MONTH_NAME_LONG + "(/)" + DAY_OF_MONTH + "(/)", "YYYY_MMMM_DD_URL" };

    /**
     * Dates in URL. YYYY_MM .<br>
     * "_" can also be "." or "-" or"/"
     */
    public static final String[] DATE_URL = { YEAR_SHORT_LONG + URL_SYM + MONTH_NUMBER_DOUBLE, "YYYY_MM" };

    /**
     * Date in URL, that can be split by folders between year an month. <br>
     * YYYY\...\MM\DD
     */
    public static final String[] DATE_URL_SPLIT = {
        LONG_YEAR + "/(.)+/" + MONTH_NUMBER_DOUBLE + URL_SYM + DAY_OF_MONTH, "YYYY.x.MM.DD" };

    // Europeandates also used worldwide
    private static final String DATE_EU_D_MM_Y_1 = DAY_OF_MONTH_1_2 + "(\\.)" + MONTH_NUMBER_NORMAL + "(\\.)"
    + YEAR_SHORT_LONG;
    private static final String DATE_EU_D_MM_Y_2 = DAY_OF_MONTH_1_2 + "(/)" + MONTH_NUMBER_NORMAL + "(/)"
    + YEAR_SHORT_LONG;
    private static final String DATE_EU_D_MM_Y_3 = DAY_OF_MONTH_1_2 + "(_)" + MONTH_NUMBER_NORMAL + "(_)"
    + YEAR_SHORT_LONG;
    private static final String DATE_EU_D_MM_Y_4 = DAY_OF_MONTH_1_2 + "(-)" + MONTH_NUMBER_NORMAL + "(-)"
    + YEAR_SHORT_LONG;

    /** European date. DD.MM.YYYY . */
    public static final String[] DATE_EU_D_MM_Y = {
        "((" + DATE_EU_D_MM_Y_1 + ")|(" + DATE_EU_D_MM_Y_2 + ")|(" + DATE_EU_D_MM_Y_3 + ")|(" + DATE_EU_D_MM_Y_4
        + "))", "DD.MM.YYYY" };
    /** European date. DD.MM.YYYY HH:MM:SS+UTC. */
    public static final String[] DATE_EU_D_MM_Y_T = {
        "(" + DATE_EU_D_MM_Y[0] + TIME_SEPARATOR + TIME + "(" + DIFF_UTC + "|" + TIMEZONE + ")?)",
    "DD.MM.YYYY HH:MM:SS +UTC" };
    /** European date. MM.YYYY . */
    public static final String[] DATE_EU_MM_Y = { MONTH_NUMBER_NORMAL + SEP_SYM + YEAR_SHORT_LONG, "MM.YYYY" };
    /** European date. DD.MM. . */
    public static final String[] DATE_EU_D_MM = { DAY_OF_MONTH_1_2 + "\\." + MONTH_NUMBER_NORMAL + "\\.", "DD.MM." };

    /** European date. DD. MMMM YYYY . */
    public static final String[] DATE_EU_D_MMMM_Y = {
        DAY_OF_MONTH_1_2 + "(((\\.)?" + ST_ND_RD_TH_OPT + "\\s)|(-))" + MONTH_NAME_LONG + "(((,)?\\s)|(-))"
        + YEAR_SHORT_LONG, "DD. MMMM YYYY" };

    /** European date. DD.MMMM . */
    public static final String[] DATE_EU_D_MMMM = { DAY_OF_MONTH_1_2 + "(\\.)? " + MONTH_NAME_LONG, "DD.MMMM" };
    /** European date. DD. MMMM YYYY HH:MM:SS +UTC . */
    public static final String[] DATE_EU_D_MMMM_Y_T = {
        DAY_OF_MONTH_1_2 + "(((\\.)?\\s)|(-))" + MONTH_NAME_LONG + "(((,)?\\s)|(-))" + YEAR_SHORT_LONG
        + TIME_SEPARATOR + TIME + "(" + DIFF_UTC + "|" + TIMEZONE + ")?", "DD. MMMM YYYY HH:MM:SS +UTC" };

    // US dates
    /** American date. MM/DD/YYYY. */
    public static final String[] DATE_USA_MM_D_Y = {
        MONTH_NUMBER_NORMAL + "/" + DAY_OF_MONTH_1_2 + "/" + YEAR_SHORT_LONG, "MM/DD/YYYY" };
    /** American date MM/DD/YYYY. HH:MM:SS +UTC. */
    public static final String[] DATE_USA_MM_D_Y_T = {
        MONTH_NUMBER_NORMAL + "/" + DAY_OF_MONTH_1_2 + "/" + YEAR_SHORT_LONG + TIME_SEPARATOR + TIME + "("
        + DIFF_UTC + "|" + TIMEZONE + ")?", "MM/DD/YYYY HH:MM:SS +UTC" };

    public static final String DATE_USA_MM_D_Y_SEPARATOR_1 = MONTH_NUMBER_NORMAL + "\\." + DAY_OF_MONTH_1_2 + "\\."
    + YEAR_SHORT_LONG;
    public static final String DATE_USA_MM_D_Y_SEPARATOR_2 = MONTH_NUMBER_NORMAL + "-" + DAY_OF_MONTH_1_2 + "-"
    + YEAR_SHORT_LONG;
    public static final String DATE_USA_MM_D_Y_SEPARATOR_3 = MONTH_NUMBER_NORMAL + "_" + DAY_OF_MONTH_1_2 + "_"
    + YEAR_SHORT_LONG;

    /** American date. MM/DD/YYYY. */
    public static final String[] DATE_USA_MM_D_Y_SEPARATOR = {
        "((" + DATE_USA_MM_D_Y_SEPARATOR_1 + ")|(" + DATE_USA_MM_D_Y_SEPARATOR_2 + ")|("
        + DATE_USA_MM_D_Y_SEPARATOR_3 + "))", "MM/DD/YYYY" };
    
    /** American date. MM/DD/YYYY. */
    public static final String[] DATE_USA_MM_D_Y_T_SEPARATOR = {
        "((" + DATE_USA_MM_D_Y_SEPARATOR_1 + ")|(" + DATE_USA_MM_D_Y_SEPARATOR_2 + ")|("
        + DATE_USA_MM_D_Y_SEPARATOR_3 + "))"+ TIME_SEPARATOR + TIME + "("
        + DIFF_UTC + "|" + TIMEZONE + ")?", "MM/DD/YYYY HH:MM:SS +UTC" };


    /** American date. MM/YYYY . */
    public static final String[] DATE_USA_MM_Y = { MONTH_NUMBER_NORMAL + "/" + YEAR_SHORT_LONG, "MM/YYYY" };
    /** American date. MM/DD . */
    public static final String[] DATE_USA_MM_D = { MONTH_NUMBER_NORMAL + "/" + DAY_OF_MONTH_1_2, "MM/DD" };
    /** American date. MMMM DD(st), YYYY . */
    public static final String[] DATE_USA_MMMM_D_Y = {
        MONTH_NAME_LONG2 + "((\\s)|(\\.)|((\\.)(\\s)))" + DAY_OF_MONTH_1_2 + "(((" + ST_ND_RD_TH_OPT
        + ")(,)?)|(\\.)?)" + " " + YEAR_SHORT_LONG, "MMMM DD, YYYY" };
    public static final String[] DATE_USA_MMMM_D_Y_SEP = {
        MONTH_NAME_LONG + "-" + DAY_OF_MONTH_1_2 + "-" + YEAR_SHORT_LONG, "MMMM-DD-YYYY" };
    /** American date. MMMM DD(st), YYYY HH:MM:SS +UTC. */
    public static final String[] DATE_USA_MMMM_D_Y_T = {
        MONTH_NAME_LONG + " " + DAY_OF_MONTH_1_2 + ST_ND_RD_TH_OPT + ", " + YEAR_SHORT_LONG + "(,)?"
        + TIME_SEPARATOR + TIME + "(" + DIFF_UTC + "|" + TIMEZONE + ")?",
    "MMMM DD, YYYY YYYY HH:MM:SS +UTC" };
    /** American date. MMMM DD(st) . */
    public static final String[] DATE_USA_MMMM_D = { MONTH_NAME_LONG + " " + DAY_OF_MONTH_1_2 + ST_ND_RD_TH_OPT,
    "MMMM DD" };

    // US and European dates
    /** American and European date. "MMMM YYYY . */
    public static final String[] DATE_EUSA_MMMM_Y = { MONTH_NAME_LONG + " " + YEAR_SHORT_LONG, "MMMM YYYY" };
    /** US ans EU. YYYY-MMM-D*/
    public static final String[] DATE_EUSA_YYYY_MMM_D = {LONG_YEAR + "-" + MONTH_NAME_LONG + "-" + DAY_OF_MONTH_1_2, "YYYY-MMM-D"};

    // RFC standards
    /** RFC 1123. WD, DD MMM YYYY HH:MM:SS TZ . */
    public static final String[] DATE_RFC_1123 = {
        WEEKDAY_NAME_SHORT + ", " + DAY_OF_MONTH + " " + MONTH_NAME_SHORT_ENG + " " + LONG_YEAR + " " + TIME_SEC
        + TIMEZONE, "WD, DD MMM YYYY HH:MM:SS TZ" };
    /** RFC 1036. WWD, DD-MMM-YYYY HH:MM:SS TZ . */
    public static final String[] DATE_RFC_1036 = {
        WEEKDAY_NAME_LONG + ", " + DAY_OF_MONTH + "-" + MONTH_NAME_SHORT_ENG + "-" + SHOR_YEAR + " " + TIME_SEC
        + TIMEZONE, "WWD, DD-MMM-YY HH:MM:SS TZ" };
    /** RFC 1123. WD, DD MMM YYYY HH:MM:SS +UTC . */
    public static final String[] DATE_RFC_1123_UTC = {
        WEEKDAY_NAME_SHORT + ", " + DAY_OF_MONTH + " " + MONTH_NAME_SHORT_ENG + " " + LONG_YEAR + " " + TIME_SEC
        + " " + DIFF_UTC, "WD, DD MMM YYYY HH:MM:SS +UTC" };
    /** RFC 1036. WWD, DD-MMM-YYYY HH:MM:SS +UTC . */
    public static final String[] DATE_RFC_1036_UTC = {
        WEEKDAY_NAME_LONG + ", " + DAY_OF_MONTH + "-" + MONTH_NAME_SHORT_ENG + "-" + SHOR_YEAR + " " + TIME_SEC
        + " " + DIFF_UTC, "WWD, DD-MMM-YY HH:MM:SS +UTC" };
    /** ANSI C's ascitime. WD MMM DD_1 HH:MM:SS YYYY . */
    public static final String[] DATE_ANSI_C = {
        WEEKDAY_NAME_SHORT + " " + MONTH_NAME_SHORT_ENG + " " + DAY_OF_MONTH_1 + " " + TIME_SEC + " " + LONG_YEAR,
    "WD MMM DD_1 HH:MM:SS YYYY" };
    /** ANSI C's ascitime with time difference to UTC. WD MMM DD_1 HH:MM:SS YYYY +UTC. */
    public static final String[] DATE_ANSI_C_TZ = {
        WEEKDAY_NAME_SHORT + " " + MONTH_NAME_SHORT_ENG + " " + DAY_OF_MONTH_1 + " " + TIME_SEC + " " + LONG_YEAR
        + " " + DIFF_UTC, "WD MMM DD_1 HH:MM:SS YYYY +UTC" };

    /*relative dates like 14 hours ago or 3 days ago*/
    public static final String MINUTEUNIT = "((minute)|(minutes))";
    public static final String HOURUNIT = "((hour)|(hours))";
    public static final String DAYUNIT = "((day)|(days))";
    public static final String MONTHUNIT = "((month)|(months))";
    public static final String YEARUNIT = "((year)|(years))";
    
    public static final String[] RELATIVEDATEMIN = {"\\d* " + MINUTEUNIT + " ago", "min"};
    public static final String[] RELATIVEDATEHOUR = {"\\d* " + HOURUNIT + " ago", "hour"};
    public static final String[] RELATIVEDATEDAY = {"\\d* " + DAYUNIT + " ago", "day"};
    public static final String[] RELATIVEDATEMON = {"\\d* " + MONTHUNIT + " ago", "mon"};
    public static final String[] RELATIVEDATEYEAR = {"\\d* " + YEARUNIT + " ago", "year"};

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

    /**
     * Get all regular Expressions in a ordered array.
     * 
     * @return
     */
    public static Object[] getAllRegExp() {
        Object[] rfcRegExp = getRFCRegExp();
        Object[] timeRegExp = getIncTimeRegExp();
        Object[] regExp3 = get3PartRegExp();
        Object[] regExp2 = get2PartRegExp();
        Object[] regExp1 = get1PartRegExp();
        Object[] regExp = new Object[rfcRegExp.length + timeRegExp.length + regExp3.length + regExp2.length
                                     + regExp1.length];
        System.arraycopy(rfcRegExp, 0, regExp, 0, rfcRegExp.length);
        System.arraycopy(timeRegExp, 0, regExp, rfcRegExp.length, timeRegExp.length);
        System.arraycopy(regExp3, 0, regExp, rfcRegExp.length + timeRegExp.length, regExp3.length);
        System.arraycopy(regExp2, 0, regExp, rfcRegExp.length + timeRegExp.length + regExp3.length, regExp2.length);
        System.arraycopy(regExp1, 0, regExp, rfcRegExp.length + timeRegExp.length + regExp3.length + regExp2.length,
                regExp1.length);
        return regExp;
    }

    /**
     * All regular expressions for RFC1036, RFC 1123 and ANSI'C
     * 
     * @return
     */
    public static Object[] getRFCRegExp() {
        Object[] regExp = { DATE_ANSI_C_TZ, DATE_ANSI_C, DATE_RFC_1036_UTC, DATE_RFC_1036, DATE_RFC_1123_UTC,
                DATE_RFC_1123, };
        return regExp;
    }

    /**
     * All regular expressions with time.<br>
     * ISO, US and EU standards. No RFCs!
     * 
     * @return
     */
    public static Object[] getIncTimeRegExp() {
        Object[] regExp = { DATE_ISO8601_YD_T, DATE_ISO8601_YMD_T, DATE_ISO8601_YWD_T, DATE_USA_MM_D_Y_T,
                DATE_EU_D_MM_Y_T, DATE_USA_MMMM_D_Y_T, DATE_EU_D_MMMM_Y_T, DATE_USA_MM_D_Y_T_SEPARATOR};
        return regExp;
    }

    /**
     * All regular expressions with three parts. (year, month and day).
     * 
     * @return
     */
    public static Object[] get3PartRegExp() {
        Object[] regExp = { DATE_ISO8601_YMD, DATE_USA_MM_D_Y, DATE_EU_D_MM_Y, DATE_USA_MMMM_D_Y,
                DATE_USA_MMMM_D_Y_SEP, DATE_EU_D_MMMM_Y, DATE_ISO8601_YWD, DATE_URL_D, DATE_USA_MM_D_Y_SEPARATOR, DATE_EUSA_YYYY_MMM_D, DATE_ISO8601_YMD_SEPARATOR };
        return regExp;
    }

    /**
     * All regular expressions with two parts. (year and month or year and day or year and month...).
     * 
     * @return
     */
    public static Object[] get2PartRegExp() {
        Object[] regExp = { DATE_ISO8601_YD, DATE_ISO8601_YM, DATE_ISO8601_YW, DATE_EUSA_MMMM_Y, DATE_USA_MM_D,
                DATE_USA_MM_Y, DATE_USA_MMMM_D, DATE_EU_D_MM, DATE_EU_D_MMMM, DATE_EU_MM_Y, DATE_URL, };
        return regExp;
    }

    /**
     * All regular expressions with one part. <br>
     * ISO standards like YYYYDDD and YYYYWW.
     * 
     * @return
     */
    public static Object[] get1PartRegExp() {
        Object[] regExp = { DATE_ISO8601_YD_NO, DATE_ISO8601_YMD_NO, DATE_ISO8601_YW_NO, DATE_ISO8601_YWD_NO };
        return regExp;
    }

    /**
     * Rest regular expressions.<br>
     * E.g.: URL date YYYY\..\MM\DD
     * 
     * @return
     */
    public static Object[] getOthersRegExp() {
        Object[] regExp = { DATE_URL_SPLIT };
        return regExp;
    }

    /**
     * Return all regular expressions for month and week days.
     * 
     * @return A set of regular expressions.
     */
    public static String[] getDateFragmentRegExp() {
        return new String[] { MONTH_NAME_SHORT_ENG, MONTH_NAME_LONG_ENG, WEEKDAY_NAME_SHORT, WEEKDAY_NAME_LONG };
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
        final Object[] regExp = { RegExp.DATE_URL_D, RegExp.DATE_URL_MMMM_D, RegExp.DATE_URL_SPLIT,
                RegExp.DATE_ISO8601_YMD_NO, RegExp.DATE_ISO8601_YWD, RegExp.DATE_ISO8601_YD, RegExp.DATE_URL,
                RegExp.DATE_ISO8601_YW };
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
        return getRFCRegExp();
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
    public static Object[] getHeadRegExp() {
        final Object[] regExp = { RegExp.DATE_RFC_1123, RegExp.DATE_RFC_1036, RegExp.DATE_ANSI_C_TZ,
                RegExp.DATE_ANSI_C, RegExp.DATE_ISO8601_YMD_T, DATE_ISO8601_YMD_SEPARATOR_T, RegExp.DATE_ISO8601_YMD,
                DATE_ISO8601_YMD_SEPARATOR, RegExp.DATE_ISO8601_YWD, RegExp.DATE_ISO8601_YD, RegExp.DATE_ISO8601_YM,
                RegExp.DATE_ISO8601_YW };
        return regExp;
    }

    public static Object[] getRelativeDates(){
    	return new Object[] {RELATIVEDATEMIN, RELATIVEDATEHOUR, RELATIVEDATEDAY, RELATIVEDATEMON, RELATIVEDATEYEAR};
    }
    
//    public static String getTimezones() {
//        return TIMEZONE;
//    }

}