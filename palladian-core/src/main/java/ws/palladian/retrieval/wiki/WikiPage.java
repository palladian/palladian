package ws.palladian.retrieval.wiki;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.html.HtmlHelper;

/**
 * <p>
 * A page in the Wikipedia.
 * </p>
 * 
 * @author Philipp Katz
 */
public class WikiPage extends WikiPageReference {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikiPage.class);

    private final String text;

    /**
     * <p>
     * Create a new {@link WikiPage}.
     * </p>
     * 
     * @param pageId The unique identifier of this page.
     * @param namespaceId The namespace to which this page belongs.
     * @param title The title of this page.
     * @param text The raw markup of this page.
     */
    public WikiPage(int pageId, int namespaceId, String title, String text) {
        super(pageId, namespaceId, title);
        this.text = text;
    }

    /**
     * @return The text on this Wiki page, as raw Mediawiki markup.
     */
    public String getMarkup() {
        return text;
    }

    /**
     * @return The clean text on this Wiki page.
     */
    public String getCleanText() {
        return MediaWikiUtil.stripMediaWikiMarkup(text);
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
        Matcher matcher = MediaWikiUtil.HEADING_PATTERN.matcher(text);
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
        Matcher matcher = MediaWikiUtil.REDIRECT_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * <p>
     * Get alternative titles for the current page. On the Wikipedia, they are given in the first paragraph of the page
     * in bold formatting. (example: <a href="http://en.wikipedia.org/wiki/United_States">United States</a>).
     * 
     * @return Alternative titles for the current page, or an empty list if no alternative titles were given.
     */
    public List<String> getAlternativeTitles() {
        List<String> sections = getSections();
        if (sections.size() > 0) {
            String firstSection = sections.get(0);
            firstSection = MediaWikiUtil.REF_PATTERN.matcher(firstSection).replaceAll("");
            firstSection = MediaWikiUtil.replaceLangPattern(firstSection);
            firstSection = StringEscapeUtils.unescapeHtml4(firstSection);
            firstSection = HtmlHelper.stripHtmlTags(firstSection);
            firstSection = MediaWikiUtil.processLinks(firstSection, MediaWikiUtil.EXTERNAL_LINK_PATTERN);
            firstSection = MediaWikiUtil.processLinks(firstSection, MediaWikiUtil.INTERNAL_LINK_PATTERN);
            firstSection = MediaWikiUtil.removeBetween(firstSection, '{', '{', '}', '}');
            firstSection = MediaWikiUtil.removeBetween(firstSection, '{', '|', '|', '}');
            firstSection = firstSection.trim();
            for (String split : firstSection.split("\n")) {
                List<String> titles = getStringsInBold(split);
                if (titles.size() > 0) {
                    return titles;
                }
            }
        }
        return Collections.emptyList();
    }

    private static final List<String> getStringsInBold(String text) {
        // Pattern pattern = Pattern.compile("'''([^']+)'''");
        // like this, it also works for bold text with a ' character:
//        Pattern pattern = Pattern.compile("'''([^'\n]+('[^'\n]+)?)'''");
//        Matcher matcher = pattern.matcher(text);
//        List<String> result = CollectionHelper.newArrayList();
//        while (matcher.find()) {
//            String group = matcher.group(1);
//            if (StringUtils.isNotBlank(group) && group.length() > 1) {
//                result.add(group.trim());
//            }
//        }
//        return result;
        
        final List<String> result = CollectionHelper.newArrayList();
        MediaWikiFormattingParser.parse(text, new MediaWikiFormattingParser.ParserAdapter() {
            StringBuilder buffer = new StringBuilder();
            boolean bold = false;

            @Override
            public void character(char ch) {
                if (bold) {
                    buffer.append(ch);
                }
            }

            @Override
            public void boldItalic() {
                bold();
            }

            @Override
            public void bold() {
                if (bold) {
                    result.add(buffer.toString().trim());
                    buffer = new StringBuilder();
                }
                bold ^= true;
            }
        });
        return result;
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
            return CollectionHelper.getFirst(MediaWikiUtil.getNamedMarkup(text, "infobox"));
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
     * @return A list of {@link WikiTemplate}es on the page, or an empty list in case no such exist, never
     *         <code>null</code>.
     */
    public List<WikiTemplate> getInfoboxes() {
        return getTemplates("infobox", "geobox");
    }

    /**
     * @param templateNames The name(s) of the templates to retrieve, not <code>null</code>.
     * @return A list of {@link WikiTemplate}s with the given name(s) on the page, or an empty list in case no such
     *         exist, never <code>null</code>.
     */
    public List<WikiTemplate> getTemplates(String... templateNames) {
        Validate.notNull(templateNames, "templateNames must not be null");
        List<WikiTemplate> infoboxes = CollectionHelper.newArrayList();
        try {
            List<String> infoboxesMarkup = MediaWikiUtil.getNamedMarkup(text, templateNames);
            for (String infoboxMarkup : infoboxesMarkup) {
                WikiTemplate template = MediaWikiUtil.extractTemplate(infoboxMarkup);
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
        Pattern pattern = Pattern.compile("\\[\\[(?:Category|Kategorie):([^|\\]]*)(?:\\|[^|\\]]*)?\\]\\]");
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
    public List<WikiLink> getLinks() {
        List<WikiLink> result = CollectionHelper.newArrayList();
        Matcher matcher = MediaWikiUtil.INTERNAL_LINK_PATTERN.matcher(text);
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
            result.add(new WikiLink(target, text));
        }
        return result;
    }
    
    /**
     * Extract a {@link GeoCoordinate} from this Wikipedia page. <code>display</code> type of the coordinate on the
     * Wikipedia page must be <code>title</code> or <code>t</code>.
     */
    @Override
    public MarkupCoordinate getCoordinate() {
        // return CollectionHelper.getFirst(WikipediaUtil.extractCoordinateTag(text));
        List<MarkupCoordinate> coordinates = CollectionHelper.newArrayList();
        coordinates.addAll(MediaWikiUtil.extractCoordinateTag(text));
        for (WikiTemplate infobox : getInfoboxes()) {
            coordinates.addAll(infobox.getCoordinates());
        }
        for (MarkupCoordinate coordinate : coordinates) {
            String display = coordinate.getDisplay();
            if (display != null && (display.contains("title") || display.equals("t"))) {
                return coordinate;
            }
        }
        return null;
    }
    
    @Override
    public Set<String> getTags() {
        return new HashSet<String>(getCategories());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WikiPage [pageId=");
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
