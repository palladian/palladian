package ws.palladian.helper.constants;

/**
 * <p>
 * Enumeration for languages.
 * </p>
 * 
 * @author Philipp Katz
 */
public enum Language {

    ENGLISH("en"), //
    GERMAN("de"), //
    FRENCH("fr"), //
    CHINESE("zh"), //
    JAPANESE("ja"), //
    RUSSIAN("ru"), //
    SPANISH("es"), //
    HINDI("hi"), //
    ARABIC("ar"), //
    PORTUGESE("pt"), //
    BENGALI("bn"), //
    PUNJABI("pa"), //
    JAVANESE("jv");

    private final String iso6391;

    private Language(String iso6391) {
        this.iso6391 = iso6391;
    }

    /**
     * <p>
     * Get an <a href="http://www.loc.gov/standards/iso639-2/php/code_list.php">ISO 639-1</a> Code of the language.
     * </p>
     * 
     * @return
     */
    public String getIso6391() {
        return iso6391;
    }

    /**
     * <p>
     * Retrieve a {@link Language} by its <a href="http://www.loc.gov/standards/iso639-2/php/code_list.php">ISO
     * 639-1</a> Code.
     * </p>
     * 
     * @param iso6391 The ISO 639-1 Code, not <code>null</code>.
     * @return The {@link Language} for the specified code, or <code>null</code> if no matching language was found.
     */
    public static Language getByIso6391(String iso6391) {
        for (Language language : values()) {
            if (language.getIso6391().equalsIgnoreCase(iso6391)) {
                return language;
            }
        }
        return null;
    }

}
