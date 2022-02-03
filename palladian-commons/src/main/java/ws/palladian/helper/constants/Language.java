package ws.palladian.helper.constants;

import org.apache.commons.lang3.StringUtils;
import ws.palladian.helper.StopWatch;

import java.util.*;

/**
 * <p>
 * Enumeration for languages including ISO 639-1 and ISO 639-2.
 * </p>
 * 
 * @see <a href="http://en.wikipedia.org/wiki/ISO_639">Wikipedia: ISO 639</a>.
 * @see <a href="http://www.loc.gov/standards/iso639-2/php/code_list.php">ISO 639-2 Language Code List</a>.
 * 
 * @author Philipp Katz, David Urbansky
 */
public enum Language {
    AFRIKAANS("af", "afr", "Afrikaans"), //
    ALBANIAN("sq", "sqi", "Albanian", Script.LATIN), //
    AMHARIC("am", "amh", "Amharic"), //
    ARABIC("ar", "ara", "Arabic", Script.ARABIC), //
    ARAGONESE("an", "arg", "Aragonese"), //
    ARMENIAN("hy", "hye", "Armenian", Script.ARMENIAN), //
    AZERBAIJANI("az", "aze", "Azerbaijani", Script.LATIN), //
    BASQUE("eu", "eus", "Basque", Script.LATIN), //
    BELARUSIAN("be", "bel", "Belarusian", Script.CYRILLIC), //
    BENGALI("bn", "ben", "Bengali"), //
    BOSNIAN("bs", "bos", "Bosnian", Script.LATIN), //
    BRETON("br", "bre", "Breton", Script.LATIN), //
    BULGARIAN("bg", "bul", "Bulgarian", Script.CYRILLIC), //
    CATALAN("ca", "cat", "Catalan"), //
    CEBUANO(null, "ceb", "Cebuano"), //
    CHAMORRO(null, "cha", "Chamorro"), //
    CHEROKEE(null, "chr", "Cherokee"), //
    CHINESE("zh", "zho", "Chinese", Script.CHINESE), //
    CHUVASH("cv", "chv", "Chuvash"), //
    CROATIAN("hr", "hrv", "Croatian", Script.LATIN), //
    CZECH("cs", "ces", "Czech", Script.LATIN), //
    DAKOTA(null, "dak", "Dakota"), //
    DANISH("da", "dan", "Danish", Script.LATIN), //
    DUTCH("nl", "nld", "Dutch", Script.LATIN), //
    ENGLISH("en", "eng", "English", Script.LATIN), //
    ESPERANTO("eo", "epo", "Esperanto", Script.LATIN), //
    ESTONIAN("et", "est", "Estonian", Script.LATIN), //
    FAROESE("fo", "fao", "Faroese"), //
    FIJIAN("fj", "fij", "Fijian"), //
    FINNISH("fi", "fin", "Finnish", Script.LATIN), //
    FRENCH("fr", "fra", "French", Script.LATIN), //
    FULFULDE("ff", "ful", "Fulfulde"), //
    GALICIAN("gl", "glg", "Galician"), //
    GEORGIAN("ka", "kat", "Georgian", Script.GEORGIAN), //
    GERMAN("de", "deu", "German", Script.LATIN), //
    GREEK("el", "ell", "Greek", Script.GREEK), //
    GUERRERO_AMUZGO(null, "amu", "Guerrero Amuzgo"), //
    GUJARATI("gu", "guj", "Gujarati"), //
    HAITIAN_CREOLE("ht", "hat", "Haitian Creole"), //
    HAUSA("ha", "hau", "Hausa"), //
    HAWAIIAN(null, "haw", "Hawaiian"), //
    HEBREW("he", "heb", "Hebrew", Script.HEBREW), //
    HILIGAYNON(null, "hil", "Hiligaynon"), //
    HINDI("hi", "hin", "Hindi", Script.DEVANGARI), //
    HUNGARIAN("hu", "hun", "Hungarian", Script.LATIN), //
    ICELANDIC("is", "isl", "Icelandic", Script.LATIN), //
    IDO("io", "ido", "Ido"), //
    INDONESIAN("id", "ind", "Indonesian"), //
    IRISH("ga", "gle", "Irish", Script.LATIN), //
    ITALIAN("it", "ita", "Italian", Script.LATIN), //
    JAKALTEK(null, "jac", "Jakaltek"), //
    JAPANESE("ja", "jpn", "Japanese", Script.KANA), //
    JAVANESE("jv", "jav", "Javanese"), //
    KABYLE(null, "kab", "Kabyle"), //
    KAQCHIKEL(null, "cak", "Kaqchikel"), //
    KEKCHI(null, "kek", "Q’eqchi’"), //
    KOREAN("ko", "kor", "Korean", Script.HANGEUL), //
    KURDISH("ku", "kur", "Kurdish", Script.ARABIC), //
    KYRGYZ("ky", "kir", "Kyrgyz"), //
    LATIN("la", "lat", "Latin", Script.LATIN), //
    LATVIAN("lv", "lav", "Latvian"), //
    LITHUANIAN("lt", "lit", "Lithuanian", Script.LATIN), //
    LOW_GERMAN(null, "nds", "Low German", Script.LATIN), //
    LUXEMBOURGISH("lb", "ltz", "Luxembourgish", Script.LATIN), //
    MACEDONIAN("mk", "mkd", "Macedonian", Script.CYRILLIC), //
    MALAY("ms", "msa", "Malay", Script.LATIN), //
    MALAYALAM("ml", "mal", "Malayalam", Script.LATIN), //
    MALTESE("mt", "mlt", "Maltese"), //
    MAORI("mi", "mri", "Māori"), //
    MARATHI("mr", "mar", "Marathi", Script.DEVANGARI), //
    MICMAC(null, "mic", "Mi'kmaq"), //
    MOSSI(null, "mos", "Mossi"), //
    NEPALI("ne", "nep", "Nepali", Script.DEVANGARI), //
    NORTHERN_NDEBELE("nd", "nde", "Northern Ndebele"), //
    NORWEGIAN("no", "nor", "Norwegian"), //
    NORWEGIAN_BOKMAL("nb", "nob", "Norwegian"), //
    NORWEGIAN_NYNORSK("nn", "nno", "Norwegian"), //
    OCCITAN("oc", "oci", "Occitan"), //
    OJIBWE("oj", "oji", "Ojibwe"), //
    PASHTO("ps", "pus", "Pashto", Script.ARABIC), //
    PERSIAN("fa", "fas", "Persian", Script.ARABIC), //
    POLISH("pl", "pol", "Polish", Script.LATIN), //
    PORTUGUESE("pt", "por", "Portuguese", Script.LATIN), //
    PUNJABI("pa", "pan", "Punjabi", Script.ARABIC), //
    QUECHUA("qu", "que", "Quechua"), //
    ROMANI(null, "rom", "Romani"), //
    ROMANIAN("ro", "ron", "Romanian", Script.LATIN), //
    RUSSIAN("ru", "rus", "Russian", Script.CYRILLIC), //
    SERBIAN("sr", "srp", "Serbian", Script.CYRILLIC), //
    SHONA("sn", "sna", "Shona"), //
    SHUAR(null, "jiv", "Shuar"), //
    SLOVAK("sk", "slk", "Slovak", Script.LATIN), //
    SLOVENE("sl", "slv", "Slovene", Script.LATIN), //
    SOMALI("so", "som", "Somali", Script.LATIN), //
    SONGE(null, "sop", "Songe"), //
    SOUTHERN_NDEBELE("nr", "nbl", "Southern Ndebele"), //
    SPANISH("es", "spa", "Spanish", Script.LATIN), //
    SUNDANESE("su", "sun", "Sundanese"), //
    SWAHILI("sw", "swa", "Swahili", Script.LATIN), //
    SWEDISH("sv", "swe", "Swedish", Script.LATIN), //
    TAGALOG("tl", "tgl", "Tagalog"), //
    TAMIL("ta", "tam", "Tamil"), //
    TELUGU("te", "tel", "Telugu"), //
    THAI("th", "tha", "Thai", Script.THAI), //
    TIBETAN("bo", "bod", "Standard Tibetan", Script.TIBETAN), //
    TURKISH("tr", "tur", "Turkish", Script.LATIN), //
    UKRAINIAN("uk", "ukr", "Ukrainian", Script.CYRILLIC), //
    URDU("ur", "urd", "Urdu", Script.ARABIC), //
    USPANTEK(null, "usp", "Uspantek"), //
    VIETNAMESE("vi", "vie", "Vietnamese", Script.LATIN), //
    VOLAPUEK("vo", "vol", "Volapük"), //
    WALLOON("wa", "wln", "Walloon"), //
    WELSH("cy", "cym", "Welsh", Script.LATIN), //
    WEST_FRISIAN("fy", "fry", "West Frisian", Script.LATIN), //
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
    private final Script script;

    /** Getting the language by iso 6391 is slow due to iterating through all values. Keep them in a map instead. */
    private static final Map<String, Language> iso6391Map = new HashMap<>();

    static {
        for (Language language : values()) {
            iso6391Map.put(language.getIso6391(), language);
        }
    }

    Language(String iso6391, String iso6392, String name) {
        this(iso6391, iso6392, name, null);
    }
    Language(String iso6391, String iso6392, String name, Script script) {
        this.iso6391 = iso6391;
        this.iso6392 = iso6392;
        this.name = name;
        this.script = script;
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
        return iso6391Map.get(iso6391.toLowerCase());
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

    /**
     * <p>
     * Retrieve a {@link Language} by name.
     * 
     * @param name The name, case insensitive.
     * @return The {@link Language}, or <code>null</code> if no matching language was found.
     */
    public static Language getByName(String name) {
        for (Language language : values()) {
            if (language.getName().equalsIgnoreCase(name)) {
                return language;
            }
        }
        return null;
    }

    public Script getScript() {
        return script;
    }

    public boolean isLatin() {
        return script != null && script == Script.LATIN;
    }

    public static void main(String[] args) {
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 1000000; i++) {
            Language de = Language.getByIso6391("de");
        }
        System.out.println(stopWatch.getElapsedTimeString());
    }
}
