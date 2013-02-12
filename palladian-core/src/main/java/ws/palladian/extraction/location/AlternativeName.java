package ws.palladian.extraction.location;

import ws.palladian.helper.constants.Language;

public final class AlternativeName {

    private String name;
    private Language language;

    /**
     * @param name
     * @param language
     */
    public AlternativeName(String name, Language language) {
        this.name = name;
        this.language = language;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(name);
        builder.append(" (").append(language).append(')');
        return builder.toString();
    }

}
