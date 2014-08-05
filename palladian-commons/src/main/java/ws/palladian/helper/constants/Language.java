package ws.palladian.helper.constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Enumeration for languages including ISO 639-1 and ISO 639-2.
 * </p>
 * 
 * @see <a href="http://en.wikipedia.org/wiki/ISO_639">Wikipedia: ISO 639</a>.
 * @see <a href="http://www.loc.gov/standards/iso639-2/php/code_list.php">ISO 639-2 Language Code List</a>.
 * 
 * @author Philipp Katz
 */
public enum Language {

    AFRIKAANS("af", "afr", "Afrikaans"), //
    ALBANIAN("sq", "sqi", "Albanian"), //
    AMHARIC("am", "amh", "Amharic"), //
    ARABIC("ar", "ara", "Arabic"), //
    ARAGONESE("an", "arg", "Aragonese"), //
    ARMENIAN("hy", "hye", "Armenian"), //
    AZERBAIJANI("az", "aze", "Azerbaijani"), //
    BASQUE("eu", "eus", "Basque"), //
    BELARUSIAN("be", "bel", "Belarusian"), //
    BENGALI("bn", "ben", "Bengali"), //
    BOSNIAN("bs", "bos", "Bosnian"), //
    BRETON("br", "bre", "Breton"), //
    BULGARIAN("bg", "bul", "Bulgarian"), //
    CATALAN("ca", "cat", "Catalan"), //
    CEBUANO(null, "ceb", "Cebuano"), //
    CHAMORRO(null, "cha", "Chamorro"), //
    CHEROKEE(null, "chr", "Cherokee"), //
    CHINESE("zh", "zho", "Chinese"), //
    CHUVASH("cv", "chv", "Chuvash"), //
    CROATIAN("hr", "hrv", "Croatian"), //
    CZECH("cs", "ces", "Czech"), //
    DAKOTA(null, "dak", "Dakota"), //
    DANISH("da", "dan", "Danish"), //
    DUTCH("nl", "nld", "Dutch"), //
    ENGLISH("en", "eng", "English"), //
    ESPERANTO("eo", "epo", "Esperanto"), //
    ESTONIAN("et", "est", "Estonian"), //
    FAROESE("fo", "fao", "Faroese"), //
    FIJIAN("fj", "fij", "Fijian"), //
    FINNISH("fi", "fin", "Finnish"), //
    FRENCH("fr", "fra", "French"), //
    FULFULDE("ff", "ful", "Fulfulde"), //
    GALICIAN("gl", "glg", "Galician"), //
    GEORGIAN("ka", "kat", "Georgian"), //
    GERMAN("de", "deu", "German"), //
    GREEK("el", "ell", "Greek"), //
    GUERRERO_AMUZGO(null, "amu", "Guerrero Amuzgo"), //
    GUJARATI("gu", "guj", "Gujarati"), //
    HAITIAN_CREOLE("ht", "hat", "Haitian Creole"), //
    HAUSA("ha", "hau", "Hausa"), //
    HAWAIIAN(null, "haw", "Hawaiian"), //
    HEBREW("he", "heb", "Hebrew"), //
    HILIGAYNON(null, "hil", "Hiligaynon"), //
    HINDI("hi", "hin", "Hindi"), //
    HUNGARIAN("hu", "hun", "Hungarian"), //
    ICELANDIC("is", "isl", "Icelandic"), //
    IDO("io", "ido", "Ido"), //
    INDONESIAN("id", "ind", "Indonesian"), //
    IRISH("ga", "gle", "Irish"), //
    ITALIAN("it", "ita", "Italian"), //
    JAKALTEK(null, "jac", "Jakaltek"), //
    JAPANESE("ja", "jpn", "Japanese"), //
    JAVANESE("jv", "jav", "Javanese"), //
    KABYLE(null, "kab", "Kabyle"), //
    KAQCHIKEL(null, "cak", "Kaqchikel"), //
    KEKCHI(null, "kek", "Q’eqchi’"), //
    KOREAN("ko", "kor", "Korean"), //
    KURDISH("ku", "kur", "Kurdish"), //
    KYRGYZ("ky", "kir", "Kyrgyz"), //
    LATIN("la", "lat", "Latin"), //
    LATVIAN("lv", "lav", "Latvian"), //
    LITHUANIAN("lt", "lit", "Lithuanian"), //
    LOW_GERMAN(null, "nds", "Low German"), //
    LUXEMBOURGISH("lb", "ltz", "Luxembourgish"), //
    MACEDONIAN("mk", "mkd", "Macedonian"), //
    MALAY("ms", "msa", "Malay"), //
    MALAYALAM("ml", "mal", "Malayalam"), //
    MALTESE("mt", "mlt", "Maltese"), //
    MAORI("mi", "mri", "Māori"), //
    MARATHI("mr", "mar", "Marathi"), //
    MICMAC(null, "mic", "Mi'kmaq"), //
    MOSSI(null, "mos", "Mossi"), //
    NEPALI("ne", "nep", "Nepali"), //
    NORTHERN_NDEBELE("nd", "nde", "Northern Ndebele"), //
    NORWEGIAN("no", "nor", "Norwegian"), //
    OCCITAN("oc", "oci", "Occitan"), //
    OJIBWE("oj", "oji", "Ojibwe"), //
    PASHTO("ps", "pus", "Pashto"), //
    PERSIAN("fa", "fas", "Persian"), //
    POLISH("pl", "pol", "Polish"), //
    PORTUGUESE("pt", "por", "Portuguese"), //
    PUNJABI("pa", "pan", "Punjabi"), //
    QUECHUA("qu", "que", "Quechua"), //
    ROMANI(null, "rom", "Romani"), //
    ROMANIAN("ro", "ron", "Romanian"), //
    RUSSIAN("ru", "rus", "Russian"), //
    SERBIAN("sr", "srp", "Serbian"), //
    SHONA("sn", "sna", "Shona"), //
    SHUAR(null, "jiv", "Shuar"), //
    SLOVAK("sk", "slk", "Slovak"), //
    SLOVENE("sl", "slv", "Slovene"), //
    SOMALI("so", "som", "Somali"), //
    SONGE(null, "sop", "Songe"), //
    SOUTHERN_NDEBELE("nr", "nbl", "Southern Ndebele"), //
    SPANISH("es", "spa", "Spanish"), //
    SUNDANESE("su", "sun", "Sundanese"), //
    SWAHILI("sw", "swa", "Swahili"), //
    SWEDISH("sv", "swe", "Swedish"), //
    TAGALOG("tl", "tgl", "Tagalog"), //
    TAMIL("ta", "tam", "Tamil"), //
    TELUGU("te", "tel", "Telugu"), //
    THAI("th", "tha", "Thai"), //
    TIBETAN("bo", "bod", "Standard Tibetan"), //
    TURKISH("tr", "tur", "Turkish"), //
    UKRAINIAN("uk", "ukr", "Ukrainian"), //
    URDU("ur", "urd", "Urdu"), //
    USPANTEK(null, "usp", "Uspantek"), //
    VIETNAMESE("vi", "vie", "Vietnamese"), //
    VOLAPUEK("vo", "vol", "Volapük"), //
    WALLOON("wa", "wln", "Walloon"), //
    WELSH("cy", "cym", "Welsh"), //
    WEST_FRISIAN("fy", "fry", "West Frisian"), //
    WOLOF("wo", "wol", "Wolof"), //
    XHOSA("xh", "xho", "Xhosa"), //
    ZARMA(null, "dje", "Zarma"); //

    /**
     * Ten of the most spoken languages (determined by number of countries, see <a
     * href="http://en.wikipedia.org/wiki/List_of_most_widely_spoken_languages_(by_number_of_countries)">List of most
     * widely spoken languages (by number of countries)</a>).
     */
    public static final List<Language> TEN_MOST_SPOKEN = Collections.unmodifiableList(Arrays.asList( //
            ENGLISH, FRENCH, ARABIC, SPANISH, PORTUGUESE, RUSSIAN, PERSIAN, ITALIAN, GERMAN, CHINESE));

    private final String iso6391;
    private final String iso6392;
    private final String name;

    private Language(String iso6391, String iso6392, String name) {
        this.iso6391 = iso6391;
        this.iso6392 = iso6392;
        this.name = name;
    }

    /**
     * @return The ISO 639-1 code of the language, <code>null</code> in case no such code exists. Usually two letters,
     *         e.g. "en".
     */
    public String getIso6391() {
        return iso6391;
    }

    /**
     * @return The ISO 639-2 code of the language. (in case <a
     *         href="http://en.wikipedia.org/wiki/ISO_639-2#B_and_T_codes">B and T</a> codes exist, the T code is
     *         returned). Usually three letters, e.g. "eng".
     */
    public String getIso6392() {
        return iso6392;
    }

    /**
     * @return The human-readable name of the language.
     */
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Retrieve a {@link Language} by its ISO 639-1 code. For example, "en" for English.
     * </p>
     * 
     * @param iso6391 The ISO 639-1 code.
     * @return The {@link Language} for the specified code, or <code>null</code> if no matching language was found.
     */
    public static Language getByIso6391(String iso6391) {
        if (StringUtils.isBlank(iso6391)) {
            return null;
        }
        for (Language language : values()) {
            if (iso6391.equalsIgnoreCase(language.getIso6391())) {
                return language;
            }
        }
        return null;
    }

    /**
     * <p>
     * Retrieve a {@link Language} by its ISO 639-2 code. For example, "eng" for English.
     * </p>
     * 
     * @param iso6392 The ISO 639-2 code.
     * @return The {@link Language} for the specified code, or <code>null</code> if no matching language was found.
     */
    public static Language getByIso6392(String iso6392) {
        if (StringUtils.isBlank(iso6392)) {
            return null;
        }
        for (Language language : values()) {
            if (iso6392.equalsIgnoreCase(language.getIso6392())) {
                return language;
            }
        }
        return null;
    }

}
