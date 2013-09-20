package ws.palladian.retrieval.wikipedia;

import java.util.Map;

import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * Template (infobox, geobox, etc.) on a Wikipedia page.
 * </p>
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Help:Template">Help:Template</a>
 * @author katz
 */
public class WikipediaTemplate {

    private final String name;
    private final Map<String, String> content;

    public WikipediaTemplate(String name, Map<String, String> content) {
        this.name = name;
        this.content = content;
    }

    /**
     * @return The name of this template, in case it is a infobox or a geobox, that value is trimmed (e.g. type
     *         <code>geobox|river</code> returns <code>river</code>.
     * @deprecated Prefer getting the complete template name using {@link #getTemplateName()}.
     */
    @Deprecated
    public String getName() {
        if (name == null) {
            return null;
        }
        return name.replaceAll("^(?:infobox\\s+|geobox\\|)", "").toLowerCase();
    }

    public String getTemplateName() {
        return name;
    }

    public String getEntry(String key) {
        return content.get(key);
    }

    public String getEntry(String... keys) {
        return CollectionHelper.getTrying(content, keys);
    }

    public int size() {
        return content.size();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WikipediaInfobox [name=");
        builder.append(name);
        builder.append(", content=");
        builder.append(content);
        builder.append("]");
        return builder.toString();
    }

}