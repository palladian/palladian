package ws.palladian.retrieval.feeds.rome;

import com.sun.syndication.feed.module.Module;

/**
 * <p>This module keeps raw date information from feed items as string. This allows us to process the date strings with our
 * own date parsing techniques, as ROME itself is very with date parsing and only parses date formats which are defined
 * by the respective Atom or RSS specifications.</p>
 * 
 * <p>The module itself is configured in the rome.properties file. For more information see the attached links.</p>
 * 
 * <p>This is a hack, we are basically abusing ROME's module mechanism, but it works quite well :).</p>
 * 
 * @see http://sujitpal.blogspot.com/2007/10/custom-modules-with-rome.html
 * @see http://java.net/projects/rome/lists/dev/archive/2005-02/message/73
 * 
 * @author Philipp Katz
 * 
 */
public interface RawDateModule extends Module {

    static final String URI = "ws.palladian.retrieval.feeds.rome.RawDateModule";

    String getRawDate();

    void setRawDate(String rawDate);

}
