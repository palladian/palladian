package ws.palladian.retrieval.feeds.rome;

import org.jdom.Element;

import com.sun.syndication.feed.module.Module;
import com.sun.syndication.io.ModuleParser;

public class RawDateParserModule implements ModuleParser{

    @Override
    public String getNamespaceUri() {
        return null;
    }

    @Override
    public Module parse(Element element) {
        RawDateModule rawDateModule = new RawDateModuleImpl();
        Element child = element.getChild("pubDate");
        if (child != null) {
            rawDateModule.setRawDate(child.getText());
        }
        return rawDateModule;
    }

}
