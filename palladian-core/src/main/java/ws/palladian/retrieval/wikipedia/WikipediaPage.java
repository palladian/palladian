package ws.palladian.retrieval.wikipedia;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * A page in the Wikipedia.
 * </p>
 * 
 * @author Philipp Katz
 */
public class WikipediaPage {

    /**
     * <p>
     * Internal link on a Wikipedia page.
     * </p>
     */
    public static class WikipediaLink {

        private final String destination;
        private final String title;

        public WikipediaLink(String destination, String title) {
            this.destination = destination;
            this.title = title;
        }

        public String getDestination() {
            return destination;
        }

        public String getTitle() {
            // return title != null ? title : destination;
            return title;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("WikipediaLink [");
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
    
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaPage.class);

    /** The id of the main namespace with articles. Other namespaces contain meta pages, like discussions etc. */
    public static final int MAIN_NAMESPACE = 0;

    private final int pageId;
    private final int namespaceId;
    private final String title;
    private final String text;

    public WikipediaPage(int pageId, int namespaceId, String title, String text) {
        this.pageId = pageId;
        this.namespaceId = namespaceId;
        this.title = title;
        this.text = text;
    }

    public int getPageId() {
        return pageId;
    }

    public int getNamespaceId() {
        return namespaceId;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public boolean isRedirect() {
        return getRedirectTitle() != null;
    }

    public String getRedirectTitle() {
        return WikipediaUtil.getRedirect(text);
    }

    /**
     * <p>
     * Extract the markup content of the first infobox on the page.
     * </p>
     * 
     * @return The markup of the infobox, if found, or <code>null</code>.
     */
    public String getInfoboxMarkup() {
        try {
            return CollectionHelper.getFirst(WikipediaUtil.getNamedMarkup(text, "infobox"));
        } catch (StringIndexOutOfBoundsException e) {
            LOGGER.warn("{} when getting infobox markup; this is usually caused by invalid markup.", e.getMessage());
            return null;
        }
    }

    /**
     * <p>
     * Extract the type of the infobox on the page, if there is any infobox.
     * </p>
     * 
     * @return The type of the infobox, or <code>null</code> if the infobox has no type assigned, or the page contains
     *         no infobox at all.
     */
    public String getInfoboxType() {
        String infoboxMarkup = getInfoboxMarkup();
        if (infoboxMarkup == null) {
            return null;
        }
        // Pattern pattern = Pattern.compile("infobox\\s(\\w+)");
        Pattern pattern = Pattern.compile("infobox\\s([^|<}]+)");
        Matcher matcher = pattern.matcher(infoboxMarkup.toLowerCase());
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * @return The title of the page, but with text in parenthesis and after comma removed.
     */
    public String getCleanTitle() {
        return WikipediaUtil.cleanTitle(title);
    }

    /**
     * @return The categories links assigned to this page, or an empty List if no category links are present.
     */
    public List<String> getCategories() {
        List<String> categories = CollectionHelper.newArrayList();
        Pattern pattern = Pattern.compile("\\[\\[Category:([^|\\]]*)(?:\\|[^|\\]]*)?\\]\\]");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            categories.add(matcher.group(1));
        }
        return categories;
    }

    /**
     * @return <code>true</code> in case this page is marked as "disambiguation page".
     */
    public boolean isDisambiguation() {
        if (title.endsWith("(disambiguation)")) {
            return true;
        }
        String temp = text.toLowerCase();
        return temp.contains("{{disambig") || temp.contains("{{hndis") || temp.contains("{{geodis");
    }

    /**
     * @return A {@link List} with all internal links on the page (sans "category:" links; they can be retrieved using
     *         {@link #getCategories()}). Empty list, in case no links are on the page, never <code>null</code>.
     */
    public List<WikipediaLink> getLinks() {
        return WikipediaUtil.getLinks(text);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WikipediaPage [pageId=");
        builder.append(pageId);
        builder.append(", namespaceId=");
        builder.append(namespaceId);
        builder.append(", title=");
        builder.append(title);
        builder.append(", redirectTitle=");
        builder.append(getRedirectTitle());
        builder.append("]");
        return builder.toString();
    }

}
