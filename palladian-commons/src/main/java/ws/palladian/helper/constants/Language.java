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

    ENGLISH("en"), GERMAN("de"), CHINESE("zh");

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

}
