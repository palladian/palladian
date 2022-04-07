package ws.palladian.extraction.location.persistence;

import static ws.palladian.extraction.location.persistence.LuceneLocationSource.ANALYZER;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_ANCESTOR_IDS;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_ID;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_LAT;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_LAT_LNG;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_LNG;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_NAME;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_POPULATION;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_TYPE;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.HIERARCHY_SEPARATOR;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.NAME_LANGUAGE_SEPARATOR;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.PRIMARY_NAME_MARKER;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.lucene.codecs.lucene87.Lucene87Codec;
import org.apache.lucene.codecs.lucene87.Lucene87Codec.Mode;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
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
import ws.palladian.helper.NoProgress;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;

public final class LuceneLocationStore implements LocationStore {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneLocationStore.class);

    /** Commit the batch, after the specified number of documents has been added. */
    private static final int COMMIT_INTERVAL = 10000;

    /** Name of the field in temporary alternative language documents which stores the foreign location ID. */
    private static final String FIELD_ALT_ID = "alternativeId";

    /** Temporary field for lat/lng pair during index building. */
    private static final String FIELD_LAT_LNG_TEMP = "latLng";

    /** Separator character between lat/lng value. */
    private static final String LAT_LNG_SEPARATOR = "#";

    /** Path to the finally created index. */
    private final File indexFile;

    /** Path to the temporary index. */
    private final File tempIndexFile;

    /** Temporary directory. */
    private final Directory tempDirectory;

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
        this.tempIndexFile = FileHelper.getTempFile();
        LOGGER.debug("Temporary index = {}", tempIndexFile);
        try {
            this.tempDirectory = FSDirectory.open(this.tempIndexFile.toPath());
            this.tempIndexWriter = new IndexWriter(tempDirectory, new IndexWriterConfig(ANALYZER));
        } catch (IOException e) {
            throw new IllegalStateException("IOException when creating IndexWriter.", e);
        }
    }

    @Override
    public void save(Location location) {
        Document document = new Document();
        document.add(new StringField(FIELD_ID, String.valueOf(location.getId()), Field.Store.YES));
        document.add(new StringField(FIELD_TYPE, location.getType().toString(), Field.Store.YES));
        document.add(new NameField(FIELD_NAME, location.getPrimaryName() + PRIMARY_NAME_MARKER));
        location.getCoords().ifPresent(coordinate -> {
            String latLng = coordinate.getLatitude() + LAT_LNG_SEPARATOR + coordinate.getLongitude();
            document.add(new StringField(FIELD_LAT_LNG_TEMP, latLng, Field.Store.YES));
        });
        Long population = location.getPopulation();
        if (population != null) {
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
        for (AlternativeName altName : alternativeNames) {
            Document document = new Document();
            Language altLang = altName.getLanguage();
            document.add(new StringField(FIELD_ALT_ID, String.valueOf(locationId), Field.Store.YES));
            String nameString = altName.getName()
                    + (altLang != null ? NAME_LANGUAGE_SEPARATOR + altLang.getIso6391() : "");
            document.add(new NameField(FIELD_NAME, nameString));
            addDocument(document);
        }
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
    public void finishImport() {
        try {
            optimize();
        } catch (IOException e) {
            throw new IllegalStateException("Encountered IOException while optimizing the index", e);
        }
    }

    private void optimize() throws IOException {
        LOGGER.debug("Committing and closing temporary IndexWriter.");
        tempIndexWriter.commit();
        tempIndexWriter.close();

        IndexWriterConfig config = new IndexWriterConfig(ANALYZER);
        config.setCodec(new Lucene87Codec(Mode.BEST_COMPRESSION));
        try (FSDirectory resultDirectory = FSDirectory.open(indexFile.toPath());
                IndexWriter resultWriter = new IndexWriter(resultDirectory, config);
                IndexReader tempReader = DirectoryReader.open(tempDirectory)) {

            int resultModificationCount = 0;
            IndexSearcher tempSearcher = new IndexSearcher(tempReader);

            // loop through all locations in index
            ProgressReporter progress = tempReader.numDocs() > 10000 ? new ProgressMonitor() : NoProgress.INSTANCE;
            progress.startTask("Optimizing index", tempReader.numDocs());
            LOGGER.debug("Creating optimized index, # of documents in temporary index: {}", tempReader.numDocs());
            for (int docId = 0; docId < tempReader.maxDoc() - 1; docId++) {
                Document document = tempReader.document(docId);
                String locationId = document.get(FIELD_ID);
                if (locationId != null) {
                    // get alternative names for the current location and add them directly into the location document
                    SimpleCollector nameCollector = new SimpleCollector();
                    tempSearcher.search(new TermQuery(new Term(FIELD_ALT_ID, locationId)), nameCollector);
                    for (int nameDocId : nameCollector.docs) {
                        Document alternativeDocument = tempReader.document(nameDocId);
                        for (IndexableField nameField : alternativeDocument.getFields(FIELD_NAME)) {
                            document.add(nameField);
                        }
                    }
                    // although Lucene offers numeric fields, e.g. IntField, in most cases, except for
                    // latitude/longitude, we intentionally use StringFields. The JavaDoc says, that those field are
                    // less space consuming. The numeric fields only need to be used, in case one wants sorting or range
                    // filtering of the values.
                    String latLng = document.get(FIELD_LAT_LNG_TEMP);
                    if (latLng != null) {
                        String[] split = latLng.split(LAT_LNG_SEPARATOR);
                        double lat = Double.parseDouble(split[0]);
                        double lng = Double.parseDouble(split[1]);
                        document.add(new StoredField(FIELD_LAT, lat));
                        document.add(new StoredField(FIELD_LNG, lng));
                        document.add(new LatLonPoint(FIELD_LAT_LNG, lat, lng));
                    }
                    document.removeField(FIELD_LAT_LNG_TEMP);
                    resultWriter.addDocument(document);
                    if (++resultModificationCount % COMMIT_INTERVAL == 0) {
                        resultWriter.commit();
                    }
                }
                progress.increment();
            }
        }

        LOGGER.debug("Deleting temporary index: {}", tempIndexFile);
        FileHelper.delete(tempIndexFile.getPath(), true);
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
