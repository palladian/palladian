package ws.palladian.retrieval.feeds.rome;

import java.util.List;

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
        System.err.println("element");
        RawDateModule rawDateModule = new RawDateModuleImpl();
        @SuppressWarnings("unchecked")
        List<Element> children = element.getChildren();
        for (Element childElement : children) {
            // search for a node containing "date" in its name
            if (childElement.getName().toLowerCase().contains("date")) {
                System.err.println(childElement.getName());
                rawDateModule.setRawDate(childElement.getText());
                break;
            }
        }
//        Element child = element.getChild("pubDate");
//        if (child != null) {
//            rawDateModule.setRawDate(child.getText());
//        }
        return rawDateModule;
    }

}
