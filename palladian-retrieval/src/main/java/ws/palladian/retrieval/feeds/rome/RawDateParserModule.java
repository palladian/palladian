package ws.palladian.retrieval.feeds.rome;

import java.util.List;
import java.util.Locale;

import org.jdom2.Element;

import com.rometools.rome.feed.module.Module;
import com.rometools.rome.io.ModuleParser;

public abstract class RawDateParserModule implements ModuleParser {

    /** The logger for this class. */
    // private static final Logger LOGGER = Logger.getLogger(RawDateParserModule.class);

    private static final String ATOM_NS = "http://www.w3.org/2005/Atom";
    private static final String RSS_20_NS = "http://backend.userland.com/rss2";
    private static final String RSS_10_NS = "http://purl.org/rss/1.0/";

    public static class RawDateParserModuleRss extends RawDateParserModule {
        @Override
        public String getNamespaceUri() {
            return null;
        }
    }

    public static class RawDateParserModuleRss20NS extends RawDateParserModule {
        @Override
        public String getNamespaceUri() {
            return RSS_20_NS;
        }
    }

    public static class RawDataParserModuleRss10 extends RawDateParserModule {
        @Override
        public String getNamespaceUri() {
            return RSS_10_NS;
        }
    }

    public static class RawDateParserModuleAtom extends RawDateParserModule {
        @Override
        public String getNamespaceUri() {
            return ATOM_NS;
        }
    }

    @Override
    public Module parse(Element element, Locale locale) {
        RawDateModule rawDateModule = new RawDateModuleImpl();
        List<Element> children = element.getChildren();
        for (Element childElement : children) {
            // search for a node containing "date" in its name
            if (childElement.getName().toLowerCase().contains("date")) {
                // LOGGER.debug("found " + childElement.getName() + " in element " + element.getTextNormalize());
                rawDateModule.setRawDate(childElement.getText());
                break;
            }
        }
        return rawDateModule;
    }

}
