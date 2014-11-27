package ws.palladian.extraction.location.persistence;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
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
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
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
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationBuilder;
import ws.palladian.extraction.location.LocationFilters;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.SingleQueryLocationSource;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.persistence.DatabaseManagerFactory;

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
        final Set<Integer> docs = CollectionHelper.newHashSet();
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
    private static final String FIELD_ID = "id";

    /** Identifier for the field containing the location type. */
    private static final String FIELD_TYPE = "type";

    /** Identifier for the field containing the location name. */
    private static final String FIELD_NAME = "name";

    /** Identifier for the field containing the geo latitude. */
    private static final String FIELD_LAT = "lat";

    /** Identifier for the field containing the geo longitude. */
    private static final String FIELD_LNG = "lng";

    /** Identifier for the field containing the population. */
    private static final String FIELD_POPULATION = "population";

    /** Identifier for the field with the ancestor ids. */
    private static final String FIELD_ANCESTOR_IDS = "ancestorIds";

    /** Primary location names in the database are appended with this marker (e.g. "Berlin$"). */
    private static final String PRIMARY_NAME_MARKER = "$";

    /** Alternative names with language determiner are separated with this marker (e.g. "Berlin#de"). */
    private static final String NAME_LANGUAGE_SEPARATOR = "#";

    /** The used Lucene version. */
    private static final Version LUCENE_VERSION = Version.LUCENE_47;

    /** Commit the batch, after the specified number of documents has been added. */
    private static final int COMMIT_INTERVAL = 10000;

    /** The Lucene directory, supplied via class constructor. */
    private final Directory directory;

    /** IndexReader is Thread-safe; we keep the instance here, because initializing it with each request takes time. */
    private final IndexReader reader;

    /** IndexSearcher is Thread-safe. */
    private final IndexSearcher searcher;

    private static final Analyzer ANALYZER = new LowerCaseKeywordAnalyzer();

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
            throw new IllegalStateException();
        }
    }

    @Override
    public Collection<Location> getLocations(String locationName, Set<Language> languages) {
        try {
            // location name also needs to be processed by analyzer; after all we could just lowercase here,
            // but in case we change our analyzer, this method keeps it consistent
            String analyzedName = analyze(locationName);
            BooleanQuery query = new BooleanQuery();
            query.setMinimumNumberShouldMatch(1);
            // search for primary names
            query.add(new TermQuery(new Term(FIELD_NAME, analyzedName + PRIMARY_NAME_MARKER)), Occur.SHOULD);
            // search for alternative names without language determiner
            query.add(new TermQuery(new Term(FIELD_NAME, analyzedName)), Occur.SHOULD);
            // search for alternative names in all specified languages
            for (Language language : languages) {
                String nameLanguageString = analyzedName + NAME_LANGUAGE_SEPARATOR + language.getIso6391();
                query.add(new TermQuery(new Term(FIELD_NAME, nameLanguageString)), Occur.SHOULD);
            }
            return queryLocations(query, searcher, reader);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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
                        return parseLocation(reader.document(currentDoc));
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
     * @param searcher The Lucene searcher.
     * @param reader The Lucene reader.
     * @return A {@link Collection} with matching {@link Location}s, or an empty Collection, never <code>null</code>.
     */
    private static Collection<Location> queryLocations(Query query, IndexSearcher searcher, IndexReader reader) {
        StopWatch stopWatch = new StopWatch();
        try {
            SimpleCollector collector = new SimpleCollector();
            searcher.search(query, collector);
            Collection<Location> locations = CollectionHelper.newHashSet();
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
            if (value.endsWith(PRIMARY_NAME_MARKER)) {
                // we have the primary name; strip the "$" marker
                builder.setPrimaryName(value.substring(0, value.length() - 1));
                continue;
            }
            // we have alternative names (either like "New York", or "New York#en")
            String[] split = value.split(NAME_LANGUAGE_SEPARATOR);
            String name = split[0];
            Language language = null;
            if (split.length == 2) {
                language = Language.getByIso6391(split[1]);
            }
            builder.addAlternativeName(name, language);
        }
        builder.setId(Integer.parseInt(document.get(FIELD_ID)));
        int typeId = document.getField(FIELD_TYPE).numericValue().intValue();
        builder.setType(LocationType.values()[typeId]);
        IndexableField latField = document.getField(FIELD_LAT);
        IndexableField lngField = document.getField(FIELD_LNG);
        if (latField != null && lngField != null) {
            double lat = latField.numericValue().doubleValue();
            double lng = lngField.numericValue().doubleValue();
            builder.setCoordinate(lat, lng);
        }
        IndexableField populationField = document.getField(FIELD_POPULATION);
        if (populationField != null) {
            builder.setPopulation(populationField.numericValue().longValue());
        }
        IndexableField ancestorIdsField = document.getField(FIELD_ANCESTOR_IDS);
        if (ancestorIdsField != null) {
            builder.setAncestorIds(ancestorIdsField.stringValue());
        }
        Location location = builder.create();
        return location;
    }

    @Override
    public Location getLocation(int locationId) {
        Query query = new TermQuery(new Term(FIELD_ID, String.valueOf(locationId)));
        return CollectionHelper.getFirst(queryLocations(query, searcher, reader));
    }

    @Override
    public List<Location> getLocations(GeoCoordinate coordinate, double distance) {
        // TODO make use of spatial functionalities;
        // http://lucene.apache.org/core/4_7_0/spatial/
        // http://stackoverflow.com/questions/13628602/how-to-use-lucene-4-0-spatial-api
        // http://www.mhaller.de/archives/156-Spatial-search-with-Lucene.html
        double[] box = coordinate.getBoundingBox(distance);
        BooleanQuery query = new BooleanQuery();
        query.add(NumericRangeQuery.newDoubleRange(FIELD_LAT, box[0], box[2], true, true), Occur.MUST);
        query.add(NumericRangeQuery.newDoubleRange(FIELD_LNG, box[1], box[3], true, true), Occur.MUST);
        Collection<Location> retrievedLocations = queryLocations(query, searcher, reader);
        return CollectionHelper.filterList(retrievedLocations, LocationFilters.radius(coordinate, distance));
    }

    /**
     * <p>
     * Import locations into Lucene from a different location source.
     * 
     * @param source The {@link LocationSource} from which to import.
     * @param directory The destination {@link Directory}; will be closed after import.
     * @throws IOException In case, writing the index fails.
     */
    public static void importLocations(LocationSource source, Directory directory) throws IOException {
        Validate.notNull(source, "source must not be null");
        Validate.notNull(directory, "directory must not be null");
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(LUCENE_VERSION, ANALYZER);
        IndexWriter indexWriter = null;
        try {
            indexWriter = new IndexWriter(directory, indexWriterConfig);
            int counter = 0;
            Iterator<Location> iterator = source.getLocations();
            ProgressMonitor monitor = new ProgressMonitor(source.size(), 1);
            while (iterator.hasNext()) {
                Location location = iterator.next();
                Document document = new Document();
                document.add(new StringField(FIELD_ID, String.valueOf(location.getId()), Field.Store.YES));
                document.add(new IntField(FIELD_TYPE, location.getType().ordinal(), Field.Store.YES));
                document.add(new TextField(FIELD_NAME, location.getPrimaryName() + PRIMARY_NAME_MARKER, Field.Store.YES));
                for (AlternativeName altName : location.getAlternativeNames()) {
                    String languageString = altName.getLanguage() != null ? (NAME_LANGUAGE_SEPARATOR + altName
                            .getLanguage().getIso6391()) : "";
                    String nameString = altName.getName() + languageString;
                    document.add(new TextField(FIELD_NAME, nameString, Field.Store.YES));
                }
                GeoCoordinate coordinate = location.getCoordinate();
                if (coordinate != null) {
                    document.add(new DoubleField(FIELD_LAT, coordinate.getLatitude(), Field.Store.YES));
                    document.add(new DoubleField(FIELD_LNG, coordinate.getLongitude(), Field.Store.YES));
                }
                if (location.getPopulation() != null) {
                    document.add(new LongField(FIELD_POPULATION, location.getPopulation(), Field.Store.YES));
                }
                if (location.getAncestorIds() != null && location.getAncestorIds().size() > 0) {
                    List<Integer> tempHierarchyIds = new ArrayList<Integer>(location.getAncestorIds());
                    Collections.reverse(tempHierarchyIds);
                    String ancestorString = "/" + StringUtils.join(tempHierarchyIds, "/") + "/";
                    document.add(new StringField(FIELD_ANCESTOR_IDS, ancestorString, Field.Store.YES));
                }
                indexWriter.addDocument(document);
                if (++counter % COMMIT_INTERVAL == 0) {
                    LOGGER.trace("Wrote {} documents to index, committing ...", counter);
                    indexWriter.commit();
                }
                monitor.incrementAndPrintProgress();
            }
        } finally {
            FileHelper.close(indexWriter, directory);
        }
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

    public static void main(String[] args) throws IOException {
        Directory directory = new MMapDirectory(new File("/Users/pk/temp/luceneLocationDatabase"));
        LocationDatabase locationDatabase = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        importLocations(locationDatabase, directory);
    }

}
