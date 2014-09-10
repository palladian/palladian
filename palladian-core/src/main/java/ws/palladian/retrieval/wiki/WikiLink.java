package ws.palladian.retrieval.wiki;

/**
 * <p>
 * Internal link on a Wikipedia page.
 * </p>
 */
public class WikiLink {

    private final String destination;
    private final String title;

    public WikiLink(String destination, String title) {
        this.destination = destination;
        this.title = title;
    }

    public String getDestination() {
        return destination;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WikiLink [");
        builder.append("destination=");
        builder.append(destination);
        if (title != null) {
            builder.append(", title=");
            builder.append(title);
        }
        builder.append("]");
        return builder.toString();
    }

}