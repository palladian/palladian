package ws.palladian.retrieval.wikipedia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A page in the Wikipedia.
 * </p>
 * 
 * @author Philipp Katz
 */
public class WikipediaPage {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaPage.class);

    private final int pageId;
    private final int namespaceId;
    private final String title;
    private final String text;
    private final String redirectTitle;

    public WikipediaPage(int pageId, int namespaceId, String title, String text, String redirectTitle) {
        this.pageId = pageId;
        this.namespaceId = namespaceId;
        this.title = title;
        this.text = text;
        this.redirectTitle = redirectTitle;
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
        return redirectTitle != null;
    }

    public String getRedirectTitle() {
        return redirectTitle;
    }

    /**
     * <p>
     * Extract the markup content of the first infobox on the page.
     * </p>
     * 
     * @return The markup of the infobox, if found, or <code>null</code>.
     */
    public String getInfoboxMarkup() {
        int startIdx = text.toLowerCase().indexOf("{{infobox");
        if (startIdx == -1) {
            return null;
        }
        try {
            int brackets = 0;
            int endIdx;
            for (endIdx = startIdx; startIdx < text.length(); endIdx++) {
                char current = text.charAt(endIdx);
                if (current == '{') {
                    brackets++;
                } else if (current == '}') {
                    brackets--;
                }
                if (brackets == 0) {
                    break;
                }
            }
            return text.substring(startIdx, endIdx);
        } catch (StringIndexOutOfBoundsException e) {
            LOGGER.warn("Encountered {} at '{}' (page id: {}), potentially caused by invalid markup.", new Object[] {e,
                    title, pageId});
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
        String clean = title.replaceAll("\\s\\([^)]*\\)", "");
        clean = clean.replaceAll(",.*", "");
        return clean;
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
        builder.append(redirectTitle);
        builder.append("]");
        return builder.toString();
    }

}
