package ws.palladian.extraction.location.sources.importers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.wikipedia.MultiStreamBZip2InputStream;
import ws.palladian.retrieval.wikipedia.WikipediaPage;
import ws.palladian.retrieval.wikipedia.WikipediaPage.WikipediaInfobox;
import ws.palladian.retrieval.wikipedia.WikipediaPageCallback;
import ws.palladian.retrieval.wikipedia.WikipediaPageContentHandler;
import ws.palladian.retrieval.wikipedia.WikipediaUtil;
import ws.palladian.retrieval.wikipedia.WikipediaUtil.MarkupLocation;

/**
 * <p>
 * Import locations from <a href="http://www.wikipedia.org">Wikipedia</a> pages.
 * </p>
 * 
 * @see <a href="http://dumps.wikimedia.org/enwiki/latest/">English dumps</a>
 * @see <a href="http://en.wikipedia.org/wiki/Wikipedia:Obtaining_geographic_coordinates">Link 1</a>
 * @see <a href="http://en.wikipedia.org/wiki/Wikipedia:WikiProject_Geographical_coordinates">Link 2</a>
 * @author Philipp Katz
 */
public class WikipediaLocationImporter {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaLocationImporter.class);

    /** Pages with those titles will be ignored. */
    private static final Pattern IGNORED_PAGES = Pattern.compile("(?:Geography|Battle) of .*");
    
    private static final Map<String, LocationType> INFOBOX_MAPPING = loadMapping();

    private static Map<String, LocationType> loadMapping() {
        InputStream inputStream = null;
        try {
            final Map<String, LocationType> result = CollectionHelper.newHashMap();
            inputStream = WikipediaLocationImporter.class.getResourceAsStream("/wikipediaLocationInfoboxMappings.csv");
            FileHelper.performActionOnEveryLine(inputStream, new LineAction() {
                @Override
                public void performAction(String line, int lineNumber) {
                    if (line.isEmpty() || line.startsWith("#")) {
                        return;
                    }
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

    private final LocationStore locationStore;

    private final Map<String, Integer> locationNamesIds;

    private final SAXParserFactory saxParserFactory;

    private final int idOffset;

    /**
     * @param locationStore The {@link LocationStore} where to store the imported data.
     * @param idOffset The offset for the inserted IDs. This way, ID clashes with existing data can be avoided. Zero for
     *            no offset (keep original IDs).
     */
    public WikipediaLocationImporter(LocationStore locationStore, int idOffset) {
        Validate.notNull(locationStore, "locationStore must not be null");
        Validate.isTrue(idOffset >= 0);
        this.locationStore = locationStore;
        this.idOffset = idOffset;
        this.saxParserFactory = SAXParserFactory.newInstance();
        this.locationNamesIds = CollectionHelper.newHashMap();
    }

    /**
     * <p>
     * Import locations from Wikipedia dump files: Pages dump file (like "enwiki-latest-pages-articles.xml.bz2").
     * </p>
     * 
     * @param dumpXml Path to the XML pages dump file (of type bz2).
     * @throws IllegalArgumentException In case the given dumps cannot be read or are of wrong type.
     * @throws IllegalStateException In case of any error during import.
     */
    public void importDumpBz2(File dumpXml) {
        Validate.notNull(dumpXml, "dumpXml must not be null");

        if (!dumpXml.isFile()) {
            throw new IllegalArgumentException("At least one of the given dump paths does not exist or is no file");
        }
        if (!dumpXml.getName().endsWith(".bz2")) {
            throw new IllegalArgumentException("XML dump file must be of type .bz2");
        }

        StopWatch stopWatch = new StopWatch();

        InputStream in = null;
        InputStream in2 = null;
        try {

            in = new MultiStreamBZip2InputStream(new BufferedInputStream(new FileInputStream(dumpXml)));
            LOGGER.info("Reading location data from {}", dumpXml);
            importLocationPages(in);

            in2 = new MultiStreamBZip2InputStream(new BufferedInputStream(new FileInputStream(dumpXml)));
            LOGGER.info("Reading location alternative names from {}", dumpXml);
            importAlternativeNames(in2);

        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        } catch (SAXException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            FileHelper.close(in, in2);
        }
        LOGGER.info("Finished import in {}", stopWatch);
    }

    void importLocationPages(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException {
        final int[] counter = new int[] {0};
        SAXParser parser = saxParserFactory.newSAXParser();
        parser.parse(inputStream, new WikipediaPageContentHandler(new WikipediaPageCallback() {

            @Override
            public void callback(WikipediaPage page) {
                if (page.getNamespaceId() != WikipediaPage.MAIN_NAMESPACE) {
                    return;
                }
                if (page.isRedirect()) {
                    return;
                }
                if (IGNORED_PAGES.matcher(page.getTitle()).matches()) {
                    LOGGER.debug("Ignoring '{}' by blacklist", page.getTitle());
                    return;
                }

                String text = page.getText();
                
                List<WikipediaInfobox> infoboxes = page.getInfoboxes();
                if (infoboxes.isEmpty()) {
                    LOGGER.debug("Page '{}' has no infobox; skip", page.getTitle());
                    return;
                }
                LocationType type = null;
                for (WikipediaInfobox infobox : infoboxes) {
                    type = INFOBOX_MAPPING.get(infobox.getName());
                    if (type != null) {
                        break;
                    }
                }
                if (type == null) {
                    LOGGER.debug("Unmapped type for '{}'; ignore", page.getTitle());
                    return;
                }

                // first, try to extract coordinates from {{coord|...|display=title}} tags
                List<MarkupLocation> locations = WikipediaUtil.extractCoordinateTag(text);
                GeoCoordinate coordinate = null;
                Long population = null;
                for (MarkupLocation location : locations) {
                    String display = location.getDisplay();
                    if (display != null && (display.contains("title") || display.equals("t"))) {
                        coordinate = location;
                        population = location.getPopulation();
                    }
                }

                // fallback, use infobox/geobox:
                if (coordinate == null) {
                    for (WikipediaInfobox infobox : infoboxes) {
                        Set<GeoCoordinate> coordinates = WikipediaUtil.extractCoordinatesFromInfobox(infobox);
                        // XXX we might also want to extract population information here in the future
                        if (coordinates.size() > 0) {
                            coordinate = CollectionHelper.getFirst(coordinates);
                        }
                    }
                }

                // save:
                if (coordinate != null) {
                    String name = page.getCleanTitle();
                    locationStore.save(new ImmutableLocation(page.getPageId() + idOffset, name, type, coordinate
                            .getLatitude(), coordinate.getLongitude(), population));
                    LOGGER.trace("Saved location with ID {}, name {}", page.getPageId(), name);
                    locationNamesIds.put(page.getTitle(), page.getPageId());
                    counter[0]++;
                }

            }
        }));
        LOGGER.info("Finished importing {} locations", counter[0]);
    }

    /**
     * Import alternative names for the locations (which are given as Wikipedia redirects).
     * 
     * @param inputStream
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     */
    void importAlternativeNames(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException {
        SAXParser parser = saxParserFactory.newSAXParser();
        final int[] counter = new int[] {0};
        parser.parse(inputStream, new WikipediaPageContentHandler(new WikipediaPageCallback() {

            @Override
            public void callback(WikipediaPage page) {
                if (page.getNamespaceId() != WikipediaPage.MAIN_NAMESPACE) {
                    return;
                }
                if (!page.isRedirect()) {
                    return;
                }
                // ignore redirects pointing to an anchor (e.g. 'Ashmore and Cartier Islands/Government' -> Ashmore and
                // Cartier Islands#Government)
                String redirectTo = page.getRedirectTitle();
                if (redirectTo.contains("#")) {
                    LOGGER.debug("Skip anchor redirect '{}'", redirectTo);
                    return;
                }
                Integer id = locationNamesIds.get(redirectTo);
                if (id == null) {
                    return;
                }
                String name = page.getCleanTitle();
                if (name.startsWith(redirectTo + "/")) {
                    LOGGER.debug("Skip redirect from '{}' to '{}'", name, redirectTo);
                    return;
                }
                AlternativeName alternativeName = new AlternativeName(name, null);
                locationStore.addAlternativeNames(id + idOffset, Collections.singleton(alternativeName));
                LOGGER.debug("Save alternative name {} for location with ID {}", name, id);
                counter[0]++;
            }
        }));
        LOGGER.info("Finished importing {} alternative names", counter[0]);
    }

    public static void main(String[] args) throws Exception {
        LocationDatabase locationStore = DatabaseManagerFactory.create(LocationDatabase.class, "locations2");
        locationStore.truncate();

        WikipediaLocationImporter importer = new WikipediaLocationImporter(locationStore, 100000000);
        File dumpXml = new File("/Users/pk/Downloads/enwiki-latest-pages-articles.xml.bz2");
        importer.importDumpBz2(dumpXml);
    }

}
