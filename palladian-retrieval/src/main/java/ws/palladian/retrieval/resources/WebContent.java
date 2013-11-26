package ws.palladian.retrieval.resources;

import java.util.Date;
import java.util.Set;

import ws.palladian.extraction.location.GeoCoordinate;

/**
 * <p>
 * An arbitrary instance of content from the Web (like a Web page, feed entry, link, image, etc.)
 * </p>
 * 
 * @author Philipp Katz
 */
public interface WebContent {
    
    /**
     * @return Internal identifier of this content, used in case this item is stored in a database, or <code>-1</code>,
     *         in case no identifier exists or the item has not been persisted.
     */
    int getId();

    /**
     * @return The URL pointing to this content.
     */
    String getUrl();

    /**
     * @return The title of this content.
     */
    String getTitle();

    /**
     * @return A textual summary of this content.
     */
    String getSummary();

    /**
     * @return The publication date of this content.
     */
    Date getPublished();

    /**
     * @return The geographic coordinate assigned with this content.
     */
    GeoCoordinate getCoordinate();

    /**
     * @return A source-specific identifier of this content.
     */
    String getIdentifier();

    /**
     * @return A set of (usually) human-assigned tags or keyword about the content, or an empty set if no tags were
     *         assigned.
     */
    Set<String> getTags();

    /**
     * @return Name of the source, from which this {@link WebContent} was acquired.
     */
    String getSource();

}
