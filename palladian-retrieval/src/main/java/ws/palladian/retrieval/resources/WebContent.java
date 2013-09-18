package ws.palladian.retrieval.resources;

import java.util.Date;

/**
 * <p>
 * An arbitrary instance of content from the Web (like a Web page, feed entry, link, image, etc.)
 * </p> 
 * 
 * @author Philipp Katz
 */
public interface WebContent {
	
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

}
