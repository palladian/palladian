package ws.palladian.retrieval.feeds.rome;

import com.sun.syndication.feed.module.Module;

public interface RawDateModule extends Module {
    
    static final String URI = "ws.palladian.retrieval.feeds.rome.RawDateModule";
    
    String getRawDate();
    void setRawDate(String rawDate);

}
