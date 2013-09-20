package ws.palladian.retrieval.wikipedia;

import java.util.Map;

import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * Infobox (or similar like geobox) on a Wikipedia page.
 * </p>
 */
public class WikipediaInfobox {

    private final String name;
    private final Map<String, String> content;

    public WikipediaInfobox(String name, Map<String, String> content) {
        this.name = name;
        this.content = content;
    }

    public String getName() {
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