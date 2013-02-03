package ws.palladian.helper.constants;

/**
 * <p>
 * Enumeration for languages.
 * </p>
 * 
 * @author Philipp Katz
 */
public enum Language {

    // TODO replace this by java.util.Locale?

    ENGLISH("en"), GERMAN("de"), FRENCH("fr"), CHINESE("zh");

    private final String iso6391;

    private Language(String iso6391) {
        this.iso6391 = iso6391;
    }

    /**
     * <p>
     * Get an ISO 639-1 Code of the language.
     * </p>
     * 
     * @return
     */
    public String getIso6391() {
        // http://www.loc.gov/standards/iso639-2/php/code_list.php
        return iso6391;
    }

    /**
     * <p>
     * Retrieve a {@link Language} by its ISO 639-1 Code.
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
