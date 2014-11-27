package ws.palladian.extraction.location.persistence;

import static ws.palladian.extraction.location.LocationFilters.radius;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
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
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationBuilder;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.SingleQueryLocationSource;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;

/**
 * A location source backed by a Lucene index.
 * 
 * @author pk
 */
public class LuceneLocationSource extends SingleQueryLocationSource implements Closeable {

    /**
     * The Lucene analyzer used by this class; beside lower casing and transforming diacritical characters to their
     * plain ASCII variant (e.g. "Liège" becomes "liege") we keep the original token as it is (i.e. no tokenizing).
     */
    private static final class LowerCaseKeywordAnalyzer extends Analyzer {
        @Override
        protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
            Tokenizer tokenizer = new KeywordTokenizer(reader);
            TokenFilter tokenFilter = new LowerCaseFilter(LUCENE_VERSION, tokenizer);
            tokenFilter = new ASCIIFoldingFilter(tokenFilter);
            return new TokenStreamComponents(tokenizer, tokenFilter);
        }
    }

    /**
     * This class collects search results; we need no scoring and accept the documents in any order here, which yields
     * in a great performance boost in contrast to Lucene's default hit-collecting logic.
     * 
     * @author pk
     */
    private static final class SimpleCollector extends Collector {
        final Set<Integer> docs = new HashSet<>();
        int docBase;

        @Override
        public void setScorer(Scorer scorer) throws IOException {
            // no scoring
        }

        @Override
        public void collect(int doc) throws IOException {
            docs.add(docBase + doc);
        }

        @Override
        public void setNextReader(AtomicReaderContext context) throws IOException {
            this.docBase = context.docBase;
        }

        @Override
        public boolean acceptsDocsOutOfOrder() {
            return true;
        }

    }

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneLocationSource.class);

    /** Identifier for the field containing the location id. */
    static final String FIELD_ID = "id";

    /** Identifier for the field containing the location type. */
    static final String FIELD_TYPE = "type";

    /** Identifier for the field containing the location name. */
    static final String FIELD_PRIMARY_NAME = "primaryName";

    static final String FIELD_ALT_ID = "alternativeId";

    /** Identifier for the field containing the location alternative name. */
    static final String FIELD_ALT_NAME = "alternativeName";

    /** Identifier for the field containing the alternative names' languages. */
    static final String FIELD_ALT_LANG = "alternativeLanguage";

    /** Identifier for the field containing the geo latitude. */
    static final String FIELD_LAT = "lat";

    /** Identifier for the field containing the geo longitude. */
    static final String FIELD_LNG = "lng";

    /** Identifier for the field containing the population. */
    static final String FIELD_POPULATION = "population";

    /** Identifier for the field with the ancestor ids. */
    static final String FIELD_ANCESTOR_IDS = "ancestorIds";

    /** Separator character between IDs in hierarchy. */
    static final char HIERARCHY_SEPARATOR = '/';

    /** Placeholder, in case a language for an alternative name was not specified. */
    static final String UNSPECIFIED_LANG = "*";

    /** The used Lucene version. */
    static final Version LUCENE_VERSION = Version.LUCENE_47;

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

    @Override
    public Collection<Location> getLocations(String locationName, Set<Language> languages) {
        try {
            // location name also needs to be processed by analyzer; after all we could just lowercase here,
            // but in case we change our analyzer, this method keeps it consistent
            String analyzedName = analyze(locationName);

            // 1) search by alternative name, get ids-of-locations
            Collection<Integer> locationIds = queryByAlternativeNames(analyzedName, languages);

            // 2) search by (ids-of-locations || name)
            BooleanQuery nameIdQuery = new BooleanQuery();
            nameIdQuery.setMinimumNumberShouldMatch(1);
            nameIdQuery.add(new TermQuery(new Term(FIELD_PRIMARY_NAME, analyzedName)), Occur.SHOULD);
            for (Integer locationId : locationIds) {
                nameIdQuery.add(new TermQuery(new Term(FIELD_ID, locationId.toString())), Occur.SHOULD);
            }
            return queryLocations(nameIdQuery);
        } catch (IOException e) {
            throw new IllegalStateException("Encountered IOException while getting locations", e);
        }

    }

    /**
     * Query for location IDs by their alternative names.
     * 
     * @param name The alternative name.
     * @param languages The set of languages which should match.
     * @return A {@link Collection} with location IDs, or an empty collection, never <code>null</code>.
     * @throws IOException In case, the query fails.
     */
    private Collection<Integer> queryByAlternativeNames(String name, Set<Language> languages) throws IOException {
        BooleanQuery languageQuery = new BooleanQuery();
        languageQuery.setMinimumNumberShouldMatch(1);
        // without language determiner
        languageQuery.add(new TermQuery(new Term(FIELD_ALT_LANG, UNSPECIFIED_LANG)), Occur.SHOULD);
        for (Language language : languages) { // explicitly specified languages
            languageQuery.add(new TermQuery(new Term(FIELD_ALT_LANG, language.getIso6391())), Occur.SHOULD);
        }

        BooleanQuery nameQuery = new BooleanQuery();
        nameQuery.add(new TermQuery(new Term(FIELD_ALT_NAME, name)), Occur.MUST);
        nameQuery.add(languageQuery, Occur.MUST);

        Collection<Integer> locationIds = new HashSet<>();
        SimpleCollector collector = new SimpleCollector();
        searcher.search(nameQuery, collector);
        for (int docId : collector.docs) {
            locationIds.add(Integer.valueOf(searcher.doc(docId).get(FIELD_ALT_ID)));
        }
        return locationIds;
    }

    @Override
    public Iterator<Location> getLocations() {
        return new AbstractIterator<Location>() {
            final int maxDoc = reader.maxDoc();
            final Bits liveDocs = MultiFields.getLiveDocs(reader);
            int currentDoc = -1;

            @Override
            protected Location getNext() throws Finished {
                while (currentDoc < maxDoc - 1) {
                    currentDoc++;
                    if (liveDocs != null && liveDocs.get(currentDoc)) {
                        // document was deleted from index
                        continue;
                    }
                    try {
                        Document document = reader.document(currentDoc);
                        if (document.get(FIELD_ID) == null) {
                            continue;
                        }
                        return retrieveLocation(document);
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
                throw FINISHED;
            }
        };
    }

    /**
     * Use the analyzer for processing the location name.
     * 
     * @param locationName The location name to process.
     * @return The processed location name (e.g. lower cased, depending on the actual {@link Analyzer} used.
     * @throws IOException In case something goes wrong.
     */
    private static String analyze(String locationName) throws IOException {
        TokenStream stream = null;
        try {
            stream = ANALYZER.tokenStream(null, new StringReader(locationName));
            stream.reset();
            if (stream.incrementToken()) {
                return stream.getAttribute(CharTermAttribute.class).toString();
            }
        } finally {
            stream.end();
            stream.close();
        }
        return locationName;
    }

    /**
     * Use the given query to find locations in the index.
     * 
     * @param query The query.
     * @return A {@link Collection} with matching {@link Location}s, or an empty Collection, never <code>null</code>.
     */
    private Collection<Location> queryLocations(Query query) {
        StopWatch stopWatch = new StopWatch();
        try {
            Collection<Location> locations = new HashSet<>();
            SimpleCollector collector = new SimpleCollector();
            searcher.search(query, collector);
            for (int docId : collector.docs) {
                Document document = searcher.doc(docId);
                locations.add(retrieveLocation(document));
            }
            LOGGER.trace("query {} took {}", query, stopWatch);
            return locations;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Retrieve a {@link Location} by its Lucene document ID.
     * 
     * @param document The Lucene document.
     * @return A fully parsed Location.
     * @throws IOException In case the retrieval fails.
     */
    private Location retrieveLocation(Document document) throws IOException {
        LocationBuilder builder = new LocationBuilder();
        builder.setPrimaryName(document.get(FIELD_PRIMARY_NAME));
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
        builder.setAlternativeNames(retrieveAlternativeNames(document.get(FIELD_ID)));
        return builder.create();
    }

    /**
     * Retrieve {@link AlternativeName}s for a given location ID.
     * 
     * @param locationId The location ID.
     * @return A collection with alternative names, or an empty collection if no such exist.
     * @throws IOException In case the retrieval fails.
     */
    private Collection<AlternativeName> retrieveAlternativeNames(String locationId) throws IOException {
        Collection<AlternativeName> alternativeNames = new HashSet<>();
        SimpleCollector collector = new SimpleCollector();
        TermQuery query = new TermQuery(new Term(FIELD_ALT_ID, locationId));
        searcher.search(query, collector);
        for (int altDocId : collector.docs) {
            Document document = searcher.doc(altDocId);
            String altName = document.get(FIELD_ALT_NAME);
            String altLangString = document.get(FIELD_ALT_LANG);
            Language altLang = altLangString != null ? Language.getByIso6391(altLangString) : null;
            alternativeNames.add(new AlternativeName(altName, altLang));
        }
        return alternativeNames;
    }

    @Override
    public Location getLocation(int locationId) {
        Query query = new TermQuery(new Term(FIELD_ID, String.valueOf(locationId)));
        return CollectionHelper.getFirst(queryLocations(query));
    }

    @Override
    public List<Location> getLocations(final GeoCoordinate coordinate, double distance) {
        // TODO make use of spatial functionalities;
        // http://lucene.apache.org/core/4_7_0/spatial/
        // http://stackoverflow.com/questions/13628602/how-to-use-lucene-4-0-spatial-api
        // http://www.mhaller.de/archives/156-Spatial-search-with-Lucene.html
        double[] box = coordinate.getBoundingBox(distance);
        BooleanQuery query = new BooleanQuery();
        // we're using floats here, see comment in
        // ws.palladian.extraction.location.persistence.LuceneLocationStore.save(Location)
        query.add(NumericRangeQuery.newFloatRange(FIELD_LAT, (float)box[0], (float)box[2], true, true), Occur.MUST);
        query.add(NumericRangeQuery.newFloatRange(FIELD_LNG, (float)box[1], (float)box[3], true, true), Occur.MUST);
        Collection<Location> retrievedLocations = queryLocations(query);
        // remove locations out of the box
        List<Location> filtered = CollectionHelper.filterList(retrievedLocations, radius(coordinate, distance));
        // sort them by distance to given coordinate
        Collections.sort(filtered, new Comparator<Location>() {
            @Override
            public int compare(Location o1, Location o2) {
                double d1 = o1.getCoordinate().distance(coordinate);
                double d2 = o2.getCoordinate().distance(coordinate);
                return Double.compare(d1, d2);
            }
        });
        return filtered;
    }

    @Override
    public int size() {
        // XXX can we make this more performant?
        return CollectionHelper.count(getLocations());
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
