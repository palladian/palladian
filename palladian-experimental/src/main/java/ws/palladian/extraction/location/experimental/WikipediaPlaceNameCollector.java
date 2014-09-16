package ws.palladian.extraction.location.experimental;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.importers.WikipediaLocationImporter;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CollectionHelper.Order;
import ws.palladian.helper.functional.Consumer;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.retrieval.wiki.MediaWikiUtil;
import ws.palladian.retrieval.wiki.MultiStreamBZip2InputStream;
import ws.palladian.retrieval.wiki.WikiPage;

public class WikipediaPlaceNameCollector {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaPlaceNameCollector.class);

    // XXX copied from WikipediaLocationImporter
    private static final Map<String, LocationType> INFOBOX_MAPPING = loadMapping();

    private static Map<String, LocationType> loadMapping() {
        InputStream inputStream = null;
        try {
            final Map<String, LocationType> result = CollectionHelper.newHashMap();
            inputStream = WikipediaLocationImporter.class.getResourceAsStream("/wikipediaLocationInfoboxMappings.csv");
            FileHelper.performActionOnEveryLine(inputStream, new LineAction() {
                @Override
                public void performAction(String line, int lineNumber) {
                    String[] split = line.split("\\t");
                    String infoboxType = split[0];
                    LocationType locationType = LocationType.map(split[1]);
                    result.put(infoboxType, locationType);
                }
            });
            return result;
        } finally {
            FileHelper.close(inputStream);
        }
    }

    static void importLocationPages(InputStream inputStream) throws ParserConfigurationException, SAXException,
            IOException {
        MediaWikiUtil.parseDump(inputStream, new Consumer<WikiPage>() {

            @Override
            public void process(WikiPage page) {
                if (page.getNamespaceId() != WikiPage.MAIN_NAMESPACE) {
                    return;
                }
                if (page.isRedirect()) {
                    return;
                }

                String infoboxType = page.getInfoboxType();
                if (infoboxType == null) {
                    LOGGER.debug("Page '{}' has no infobox; skip", page.getTitle());
                    return;
                }
                LocationType type = INFOBOX_MAPPING.get(infoboxType);
                if (type == null) {
                    LOGGER.debug("Unmapped type for '{}'; ignore", page.getTitle());
                    return;
                }

                // ignore POI type here, because it causes too much confusion
                if (type == LocationType.POI) {
                    return;
                }

                FileHelper.appendFile("locationNames.txt", page.getCleanTitle() + "\n");
            }
        });
    }

    static void collectNameParts() {
        final Bag<String> counts = Bag.create();
        FileHelper.performActionOnEveryLine("locationNames.txt", new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split("\\s");
                counts.addAll(Arrays.asList(split));
            }
        });

        Bag<String> map = counts.createSorted(Order.DESCENDING);
        for (Entry<String, Integer> entry : map.unique()) {
            int count = entry.getValue();
            if (count > 200) {
                System.out.println(entry.getValue() + " (" + count + ")");
            }
        }
    }

    public static void main(String[] args) {
        collectNameParts();
        System.exit(0);

        InputStream in = null;
        try {
            String dumpXml = "/Users/pk/Downloads/enwiki-latest-pages-articles.xml.bz2";
            in = new MultiStreamBZip2InputStream(new BufferedInputStream(new FileInputStream(dumpXml)));
            importLocationPages(in);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        } catch (SAXException e) {
            throw new IllegalStateException(e);
        } finally {
            FileHelper.close(in);
        }
    }

}
