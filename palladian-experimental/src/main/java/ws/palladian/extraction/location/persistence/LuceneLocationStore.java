package ws.palladian.extraction.location.persistence;

import static ws.palladian.extraction.location.persistence.LuceneLocationSource.ANALYZER;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_ANCESTOR_IDS;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_ID;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_LAT;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_LAT_LNG_POINT;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_LAT_LNG_SORT;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_LNG;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_NAME;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_POPULATION;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_TYPE;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.HIERARCHY_SEPARATOR;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.NAME_LANGUAGE_SEPARATOR;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LatLonDocValuesField;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.constants.Language;

public final class LuceneLocationStore implements LocationStore {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneLocationStore.class);

    /** Commit the batch, after the specified number of documents has been added. */
    private static final int COMMIT_INTERVAL = 10000;

    /** Name of the field in temporary alternative language documents which stores the foreign location ID. */
    private static final String FIELD_ALT_ID = "alternativeId";

    /** Path to the finally created index. */
    private final File indexFile;

    /** Temporary directory. */
    private final Directory directory;

    /** The writer for the index. */
    private final IndexWriter tempIndexWriter;

    /** The number of modifications since the last commit. */
    private int modificationCount;

    public LuceneLocationStore(File indexFile) {
        Validate.notNull(indexFile, "indexFile must not be null");
        if (indexFile.exists()) {
            throw new IllegalArgumentException(indexFile
                    + " already exists. Delete the index or specify a different path");
        }
        this.indexFile = indexFile;
        try {
            this.directory = FSDirectory.open(this.indexFile.toPath());
            this.tempIndexWriter = new IndexWriter(directory, new IndexWriterConfig(ANALYZER));
        } catch (IOException e) {
            throw new IllegalStateException("IOException when creating IndexWriter.", e);
        }
    }

    @Override
    public void save(Location location) {
        Document document = new Document();
        document.add(new StringField(FIELD_ID, String.valueOf(location.getId()), Field.Store.YES));
        document.add(new StringField(FIELD_TYPE, location.getType().toString(), Field.Store.YES));
        document.add(new NameField(FIELD_NAME, location.getPrimaryName()));
        location.getCoords().ifPresent(coordinate -> {
            document.add(new StoredField(FIELD_LAT, coordinate.getLatitude()));
            document.add(new StoredField(FIELD_LNG, coordinate.getLongitude()));
        });
        Long population = location.getPopulation();
        if (population != null && population > 0) { // TODO set null values already in importer
            document.add(new StringField(FIELD_POPULATION, population.toString(), Field.Store.YES));
        }
        if (location.getAncestorIds() != null && !location.getAncestorIds().isEmpty()) {
            List<Integer> tempHierarchyIds = new ArrayList<>(location.getAncestorIds());
            Collections.reverse(tempHierarchyIds);
            String ancestorString = StringUtils.join(tempHierarchyIds, HIERARCHY_SEPARATOR);
            document.add(new StringField(FIELD_ANCESTOR_IDS, ancestorString, Field.Store.YES));
        }
        addDocument(document);
        addAlternativeNames(location.getId(), location.getAlternativeNames());
    }

    /**
     * Add a {@link Document} to the index, and conditionally commit; in case the number of modifications have reached a
     * modulus of {@value #COMMIT_INTERVAL}.
     * 
     * @param document The document to add, not <code>null</code>.
     * @throws IllegalStateException In case, adding or committing fails.
     */
    private void addDocument(Document document) {
        try {
            tempIndexWriter.addDocument(document);
            if (++modificationCount % COMMIT_INTERVAL == 0) {
                LOGGER.trace("Added {} documents to index, committing ...", modificationCount);
                tempIndexWriter.commit();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Encountered IOException while adding document " + document, e);
        }
    }

    @Override
    public void addAlternativeNames(int locationId, Collection<AlternativeName> alternativeNames) {
        if (alternativeNames.isEmpty()) {
            return;
        }
        Document document = new Document();
        document.add(new StringField(FIELD_ALT_ID, String.valueOf(locationId), Field.Store.YES));
        for (AlternativeName altName : alternativeNames) {
            String langString = altName.getLang().map(Language::getIso6391).orElse("");
            String nameString = altName.getName() + NAME_LANGUAGE_SEPARATOR + langString;
            document.add(new NameField(FIELD_NAME, nameString));
        }
        addDocument(document);
    }

    @Override
    public int getHighestId() {
        throw new UnsupportedOperationException("#getHighestId is not supported.");
    }
    
    @Override
    public void startImport() {
        // nothing to do
    }

    @Override
    public void finishImport(ProgressReporter progress) {
        try {
            build(progress);
        } catch (IOException e) {
            throw new IllegalStateException("Encountered IOException while optimizing the index", e);
        }
    }

    private void build(ProgressReporter progress) throws IOException {
        LOGGER.debug("Committing and closing temporary IndexWriter.");
        tempIndexWriter.commit();
        tempIndexWriter.close();

        IndexWriterConfig config = new IndexWriterConfig(ANALYZER);
        try (IndexWriter resultWriter = new IndexWriter(directory, config);
                IndexReader tempReader = DirectoryReader.open(directory)) {

            IndexSearcher tempSearcher = new IndexSearcher(tempReader);

            // loop through all locations in index
            progress.startTask("Building index", tempReader.numDocs());
            LOGGER.debug("Creating optimized index, # of documents in temporary index: {}", tempReader.numDocs());
            for (int docId = 0; docId < tempReader.maxDoc(); docId++) {
                Document document = tempReader.document(docId);
                String locationId = document.get(FIELD_ID);
                if (locationId != null) {
                    // get alternative names for the current location and add them directly into the location document
                    SimpleCollector nameCollector = new SimpleCollector();
                    TermQuery altNamesQuery = new TermQuery(new Term(FIELD_ALT_ID, locationId));
                    tempSearcher.search(altNamesQuery, nameCollector);
                    for (int nameDocId : nameCollector.docs) {
                        Document alternativeDocument = tempReader.document(nameDocId);
                        for (IndexableField nameField : alternativeDocument.getFields(FIELD_NAME)) {
                            document.add(nameField);
                        }
                    }
                    IndexableField latField = document.getField(FIELD_LAT);
                    IndexableField lngField = document.getField(FIELD_LNG);
                    if (latField != null && lngField != null) {
                        double lat = latField.numericValue().doubleValue();
                        double lng = lngField.numericValue().doubleValue();
                        // this is used for querying by lat/lon
                        document.add(new LatLonPoint(FIELD_LAT_LNG_POINT, lat, lng));
                        // this is used for sorting by lat/lon
                        document.add(new LatLonDocValuesField(FIELD_LAT_LNG_SORT, lat, lng));
                    }
                    // remove the intermediate alternative names document
                    resultWriter.deleteDocuments(altNamesQuery);
                    // update the current document
                    resultWriter.updateDocument(new Term(FIELD_ID, locationId), document);
                }
                progress.increment();
            }
            // combine all segments into one
            LOGGER.debug("Merging index");
            resultWriter.forceMerge(1);
        }
    }

    private static final class NameField extends Field {
        private static final FieldType FIELD_TYPE = new FieldType();
        static {
            // we don't need frequencies, etc. b/c there's no scoring
            FIELD_TYPE.setIndexOptions(IndexOptions.DOCS);
            FIELD_TYPE.setTokenized(true);
            FIELD_TYPE.setStored(true);
            // according to The Internet(tm) this saves some space
            // https://stackoverflow.com/a/10081260
            FIELD_TYPE.setOmitNorms(true);
            FIELD_TYPE.freeze();
        }

        NameField(String name, String value) {
            super(name, value, FIELD_TYPE);
        }
    }

}
