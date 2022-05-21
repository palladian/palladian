package ws.palladian.extraction.location.persistence.lucene;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LatLonDocValuesField;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiBits;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationBuilder;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.SingleQueryLocationSource;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.AbstractIterator2;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;

/**
 * A location source backed by a Lucene index.
 *
 * @author Philipp Katz
 */
public class LuceneLocationSource extends SingleQueryLocationSource implements Closeable {

    /**
     * The Lucene analyzer used by this class; beside lower casing and transforming diacritical characters to their
     * plain ASCII variant (e.g. "Liège" becomes "liege") we keep the original token as it is (i.e. no tokenizing).
     */
    private static final class LowerCaseKeywordAnalyzer extends Analyzer {
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            Tokenizer tokenizer = new KeywordTokenizer();
            TokenFilter tokenFilter = new LowerCaseFilter(tokenizer);
            tokenFilter = new ASCIIFoldingFilter(tokenFilter);
            return new TokenStreamComponents(tokenizer, tokenFilter);
        }
    }

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneLocationSource.class);

    /** Identifier for the field containing the location id. */
    static final String FIELD_ID = "id";

    /** Identifier for the field containing the location type. */
    static final String FIELD_TYPE = "type";

    /** Identifier for the field containing the location name. */
    static final String FIELD_NAME = "name";

    /** LatLonPoint field (for queries, doesn't store the actual value). */
    static final String FIELD_LAT_LNG_POINT = "latLngPoint";

    /** LatLonDocValuesField for sorting results by distance. */
    static final String FIELD_LAT_LNG_SORT = "latLngSort";

    /** Identifier for the field which stores the latitude. */
    static final String FIELD_LAT = "lat";

    /** Identifier for the field which stores the longitude. */
    static final String FIELD_LNG = "lng";

    /** Identifier for the field containing the population. */
    static final String FIELD_POPULATION = "population";

    /** Identifier for the field with the ancestor ids. */
    static final String FIELD_ANCESTOR_IDS = "ancestorIds";

    /** Alternative names with language determiner are separated with this marker (e.g. "Berlin#de"). */
    static final String NAME_LANGUAGE_SEPARATOR = "#";

    /** Separator character between IDs in hierarchy. */
    static final char HIERARCHY_SEPARATOR = '/';

    /** The Lucene directory, supplied via class constructor. */
    private final Directory directory;

    /** IndexReader is Thread-safe; we keep the instance here, because initializing it with each request takes time. */
    private final IndexReader reader;

    /** IndexSearcher is Thread-safe. */
    private final IndexSearcher searcher;

    static final Analyzer ANALYZER = new LowerCaseKeywordAnalyzer();

    /**
     * <p>
     * Create a new Lucene location source, where data is provided from the given {@link Directory}.
     *
     * @param directory The Lucene directory with the location data, not <code>null</code>.
     * @throws IllegalStateException In case setting up the Lucene readers fails.
     * @see #open(String) for convenience.
     */
    public LuceneLocationSource(Directory directory) {
        Validate.notNull(directory, "directory must not be null");
        this.directory = directory;
        try {
            reader = DirectoryReader.open(directory);
            searcher = new IndexSearcher(reader);
        } catch (IOException e) {
            throw new IllegalStateException("IOException when opening DirectoryReader or IndexSearcher", e);
        }
    }
    
    /**
     * Convenience function to open an LLS.
     * 
     * @param directoryPath Path to the directory.
     * @return The LLS.
     * @throws IllegalStateException In case it cannot be opened for whatever
     *                               reason.
     */
    public static LuceneLocationSource open(String directoryPath) throws IOException {
        try {
            return new LuceneLocationSource(FSDirectory.open(Paths.get(directoryPath)));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Collection<Location> getLocations(String locationName, Set<Language> languages) {
        BooleanQuery query = createQuery(locationName, languages, null, 0);
        return queryLocations(query, searcher, reader);
    }

    @Override
    public Iterator<Location> getLocations() {
        return new AbstractIterator2<Location>() {
            final int maxDoc = reader.maxDoc();
            final Bits liveDocs = MultiBits.getLiveDocs(reader);
            int currentDoc = -1;

            @Override
            protected Location getNext() {
                while (currentDoc < maxDoc - 1) {
                    currentDoc++;
                    if (liveDocs != null && liveDocs.get(currentDoc)) {
                        // document was deleted from index
                        continue;
                    }
                    try {
                        Document document = reader.document(currentDoc);
                        return parseLocation(document);
                    } catch (IOException e) {
                        throw new IllegalStateException("Could not retrieve document with ID " + currentDoc, e);
                    }
                }
                return finished();
            }
        };
    }

    /**
     * Use the analyzer for processing the location name.
     *
     * @param locationName The location name to process.
     * @return The processed location name (e.g. lower cased, depending on the actual {@link Analyzer} used.
     * @throws IllegalStateException In case something goes wrong.
     */
    private static String analyze(String locationName) {
        try (TokenStream stream = ANALYZER.tokenStream(null, new StringReader(locationName))) {
            stream.reset();
            if (stream.incrementToken()) {
                return stream.getAttribute(CharTermAttribute.class).toString();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Encountered IOException while analyzing", e);
        }
        return locationName;
    }

    /**
     * Use the given query to find locations in the index.
     *
     * @param query The query.
     * @param searcher The Lucene searcher.
     * @param reader The Lucene reader.
     * @return A {@link Collection} with matching {@link Location}s, or an empty Collection, never <code>null</code>.
     */
    private static List<Location> queryLocations(Query query, IndexSearcher searcher, IndexReader reader) {
        StopWatch stopWatch = new StopWatch();
        try {
            SimpleCollector collector = new SimpleCollector();
            searcher.search(query, collector);
            List<Location> locations = new ArrayList<>();
            for (int docId : collector.docs) {
                locations.add(parseLocation(searcher.doc(docId)));
            }
            LOGGER.trace("query {} took {}", query, stopWatch);
            return locations;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Parse the Lucene {@link Document} and create a {@link Location} from the document's fields.
     *
     * @param document The Lucene document to convert.
     * @return The location instance with data from the document.
     */
    private static Location parseLocation(Document document) {
        LocationBuilder builder = new LocationBuilder();
        IndexableField[] nameFields = document.getFields(FIELD_NAME);
        for (IndexableField nameField : nameFields) {
            String value = nameField.stringValue();
            if (!value.contains(NAME_LANGUAGE_SEPARATOR)) {
                builder.setPrimaryName(value);
            } else {
                // we have alternative names (either like "New York#", or "New York#en")
                String[] split = value.split(NAME_LANGUAGE_SEPARATOR);
                String name = split[0];
                Language language = split.length == 2 ? Language.getByIso6391(split[1]) : null;
                builder.addAlternativeName(name, language);
            }
        }
        builder.setId(Integer.parseInt(document.get(FIELD_ID)));
        builder.setType(LocationType.map(document.get(FIELD_TYPE)));
        IndexableField latField = document.getField(FIELD_LAT);
        IndexableField lngField = document.getField(FIELD_LNG);
        if (latField != null && lngField != null) {
            double lat = latField.numericValue().doubleValue();
            double lng = lngField.numericValue().doubleValue();
            builder.setCoordinate(lat, lng);
        }
        String population = document.get(FIELD_POPULATION);
        if (population != null) {
            builder.setPopulation(Long.valueOf(population));
        }
        String ancestorIds = document.get(FIELD_ANCESTOR_IDS);
        if (ancestorIds != null) {
            builder.setAncestorIds(ancestorIds);
        }
        return builder.create();
    }

    @Override
    public Location getLocation(int locationId) {
        Query query = new TermQuery(new Term(FIELD_ID, String.valueOf(locationId)));
        return queryLocations(query, searcher, reader).stream().findFirst().orElse(null);
    }

    @Override
    public List<Location> getLocations(final GeoCoordinate coordinate, double distance) {
        BooleanQuery query = createQuery(null, null, coordinate, distance);
        Sort sort = new Sort(LatLonDocValuesField.newDistanceSort(FIELD_LAT_LNG_SORT, coordinate.getLatitude(),
                coordinate.getLongitude()));
        StopWatch stopWatch = new StopWatch();
        try {
            TopFieldDocs result = searcher.search(query, Integer.MAX_VALUE, sort);
            List<Location> locations = new ArrayList<>();
            for (ScoreDoc scoreDoc : result.scoreDocs) {
                locations.add(parseLocation(searcher.doc(scoreDoc.doc)));
            }
            LOGGER.trace("query {} took {}", query, stopWatch);
            return locations;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public MultiMap<String, Location> getLocations(Collection<String> locationNames, Set<Language> languages,
            GeoCoordinate coordinate, double distance) {
        MultiMap<String, Location> result = DefaultMultiMap.createWithSet();
        for (String locationName : locationNames) {
            BooleanQuery query = createQuery(locationName, languages, coordinate, distance);
            result.put(locationName, queryLocations(query, searcher, reader));
        }
        return result;
    }

    private static BooleanQuery createQuery(String locationName, Set<Language> languages, GeoCoordinate coordinate,
            double distance) {
        BooleanQuery.Builder query = new BooleanQuery.Builder();
        if (locationName != null) {
            // location name also needs to be processed by analyzer; after all we could just lowercase here,
            // but in case we change our analyzer, this method keeps it consistent
            String analyzedName = analyze(locationName);
            query.setMinimumNumberShouldMatch(1);
            // search for primary names
            query.add(new TermQuery(new Term(FIELD_NAME, analyzedName)), Occur.SHOULD);
            // search for alternative names without language determiner
            query.add(new TermQuery(new Term(FIELD_NAME, analyzedName + NAME_LANGUAGE_SEPARATOR)), Occur.SHOULD);
            // search for alternative names in all specified languages
            for (Language language : languages) {
                String nameLanguageString = analyzedName + NAME_LANGUAGE_SEPARATOR + language.getIso6391();
                query.add(new TermQuery(new Term(FIELD_NAME, nameLanguageString)), Occur.SHOULD);
            }
        }
        if (coordinate != null) {
            query.add(LatLonPoint.newDistanceQuery( //
                    FIELD_LAT_LNG_POINT, //
                    coordinate.getLatitude(), //
                    coordinate.getLongitude(), //
                    distance * 1000 //
            ), Occur.FILTER);
        }
        return query.build();
    }

    @Override
    public int size() {
        return reader.numDocs();
    }

    @Override
    public void close() throws IOException {
        directory.close();
        reader.close();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + directory;
    }

}
