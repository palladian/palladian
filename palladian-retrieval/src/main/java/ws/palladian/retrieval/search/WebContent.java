package ws.palladian.retrieval.search;

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
	 * @return The URL pointing to this Web content.
	 */
	String getUrl();
	
	String getTitle();
	
	String getSummary();
	
	Date getDate();

}
