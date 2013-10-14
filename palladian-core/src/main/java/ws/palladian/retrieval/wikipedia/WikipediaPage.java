package ws.palladian.retrieval.wikipedia;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * A page in the Wikipedia.
 * </p>
 * 
 * @author Philipp Katz
 */
public class WikipediaPage extends WikipediaPageReference {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaPage.class);

    private final String text;

    /**
     * <p>
     * Create a new {@link WikipediaPage}.
     * </p>
     * 
     * @param pageId The unique identifier of this page.
     * @param namespaceId The namespace to which this page belongs.
     * @param title The title of this page.
     * @param text The raw markup of this page.
     */
    public WikipediaPage(int pageId, int namespaceId, String title, String text) {
        super(pageId, namespaceId, title);
        this.text = text;
    }

    /**
     * @return The text on this Wiki page, as raw Mediawiki markup.
     */
    public String getText() {
        return text;
    }

    /**
     * <p>
     * Get the individual sections from the page. The beginning of the article is also added to the result, even if it
     * does not start with a section heading.
     * </p>
     * 
     * @return List with sections, starting with the original section headings, or empty list if no sections were found,
     *         never <code>null</code> however.
     */
    public List<String> getSections() {
        List<String> result = CollectionHelper.newArrayList();
        Matcher matcher = WikipediaUtil.HEADING_PATTERN.matcher(text);
        int start = 0;
        while (matcher.find()) {
            int end = matcher.start();
            result.add(text.substring(start, end));
            start = end;
        }
        result.add(text.substring(start));
        return result;
    }

    /**
     * @return <code>true</code> in case this page redirects to another, <code>false</code> otherwise.
     */
    public boolean isRedirect() {
        return getRedirectTitle() != null;
    }

    /**
     * @return The title of the page to which this redirects, or <code>null</code> in case this page is no redirect.
     */
    public String getRedirectTitle() {
        Matcher matcher = WikipediaUtil.REDIRECT_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * <p>
     * Extract the markup content of the first infobox on the page.
     * </p>
     * 
     * @return The markup of the infobox, if found, or <code>null</code>.
     * @deprecated Use {@link #getInfoboxes()} instead.
     */
    @Deprecated
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
     * @deprecated Use {@link #getInfoboxes()} instead.
     */
    @Deprecated
    public String getInfoboxType() {
        String infoboxMarkup = getInfoboxMarkup();
        if (infoboxMarkup == null) {
            return null;
        }
        // Pattern pattern = Pattern.compile("infobox\\s(\\w+)");
        return getInfoboxType(infoboxMarkup);
    }

    private static final String getInfoboxType(String infoboxMarkup) {
        Pattern pattern = Pattern.compile("(?:infobox|geobox)[\\s|]([^|<}]+)");
        Matcher matcher = pattern.matcher(infoboxMarkup.toLowerCase());
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * @return A list of {@link WikipediaTemplate}es on the page, or an empty list in case no such exist, never
     *         <code>null</code>.
     * @deprecated Prefer using the more flexible {@link #getTemplates(String...)} and specify what to extract.
     */
    @Deprecated
    public List<WikipediaTemplate> getInfoboxes() {
        return getTemplates("infobox", "geobox");
    }

    /**
     * @param templateNames The name(s) of the templates to retrieve, not <code>null</code>.
     * @return A list of {@link WikipediaTemplate}s with the given name(s) on the page, or an empty list in case no such
     *         exist, never <code>null</code>.
     */
    public List<WikipediaTemplate> getTemplates(String... templateNames) {
        Validate.notNull(templateNames, "templateNames must not be null");
        List<WikipediaTemplate> infoboxes = CollectionHelper.newArrayList();
        try {
            List<String> infoboxesMarkup = WikipediaUtil.getNamedMarkup(text, templateNames);
            for (String infoboxMarkup : infoboxesMarkup) {
                WikipediaTemplate template = WikipediaUtil.extractTemplate(infoboxMarkup);
                infoboxes.add(template);
            }
        } catch (StringIndexOutOfBoundsException e) {
            LOGGER.warn("{} when getting infobox markup; this is usually caused by invalid markup.", e.getMessage());
        }
        return infoboxes;
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
        if (getTitle().endsWith("(disambiguation)")) {
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
        List<WikipediaLink> result = CollectionHelper.newArrayList();
        Matcher matcher = WikipediaUtil.INTERNAL_LINK_PATTERN.matcher(text);
        while (matcher.find()) {
            String target = matcher.group(1);
            // strip fragments
            int idx = target.indexOf('#');
            if (idx >= 0) {
                target = target.substring(0, idx);
            }
            String text = matcher.group(2);
            // ignore category links here
            if (target.toLowerCase().startsWith("category:")) {
                continue;
            }
            result.add(new WikipediaLink(target, text));
        }
        return result;
    }
    
    @Override
    public GeoCoordinate getCoordinate() {
        return CollectionHelper.getFirst(WikipediaUtil.extractCoordinateTag(text));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WikipediaPage [pageId=");
        builder.append(getIdentifier());
        builder.append(", namespaceId=");
        builder.append(getNamespaceId());
        builder.append(", title=");
        builder.append(getTitle());
        builder.append(", redirectTitle=");
        builder.append(getRedirectTitle());
        builder.append("]");
        return builder.toString();
    }

}
