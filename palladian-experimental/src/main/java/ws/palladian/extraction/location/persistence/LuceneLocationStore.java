package ws.palladian.extraction.location.persistence;

import static ws.palladian.extraction.location.persistence.LuceneLocationSource.ANALYZER;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_ALT_ID;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_ALT_LANG;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_ALT_NAME;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_ANCESTOR_IDS;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_ID;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_LAT;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_LNG;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_POPULATION;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_PRIMARY_NAME;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.FIELD_TYPE;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.HIERARCHY_SEPARATOR;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.LUCENE_VERSION;
import static ws.palladian.extraction.location.persistence.LuceneLocationSource.UNSPECIFIED_LANG;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;

public final class LuceneLocationStore implements LocationStore, Closeable {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneLocationStore.class);

    /** Commit the batch, after the specified number of documents has been added. */
    private static final int COMMIT_INTERVAL = 10000;

    /** The writer for the index. */
    private final IndexWriter indexWriter;

    /** The number of modifications since the last commit. */
    private int modificationCounter;

    public LuceneLocationStore(Directory directory) {
        // XXX make sure that no index exists
        Validate.notNull(directory, "directory must not be null");
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(LUCENE_VERSION, ANALYZER);
        try {
            this.indexWriter = new IndexWriter(directory, indexWriterConfig);
        } catch (IOException e) {
            throw new IllegalStateException("IOException when creating IndexWriter.", e);
        }
    }

    @Override
    public void save(Location location) {
        try {
            Document document = new Document();
            // although Lucene offers numeric fields, e.g. IntField, in some cases we intentionally use StringFields.
            // The JavaDoc says, that those field are less space consuming. The numeric fields only need to be used, in
            // case one wants sorting or range filtering of the values.
            document.add(new StringField(FIELD_ID, String.valueOf(location.getId()), Field.Store.YES));
            document.add(new StringField(FIELD_TYPE, location.getType().toString(), Field.Store.YES));
            document.add(new TextField(FIELD_PRIMARY_NAME, location.getPrimaryName(), Field.Store.YES));
            GeoCoordinate coordinate = location.getCoordinate();
            if (coordinate != null) {
                // explicitely changed from DoubleField to a FloatField to save space;
                // when changing back to double, make sure to revert the range queries in 
                // ws.palladian.extraction.location.persistence.LuceneLocationSource.getLocations(GeoCoordinate, double)
                document.add(new FloatField(FIELD_LAT, (float)coordinate.getLatitude(), Field.Store.YES));
                document.add(new FloatField(FIELD_LNG, (float)coordinate.getLongitude(), Field.Store.YES));
            }
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
        } catch (IOException e) {
            throw new IllegalStateException("Encountered IOException while adding location", e);
        }
        addAlternativeNames(location.getId(), location.getAlternativeNames());
    }

    /**
     * Add a {@link Document} to the index, and conditionally commit; in case the number of modifications have reached a
     * modulus of {@value #COMMIT_INTERVAL}.
     * 
     * @param document The document to add, not <code>null</code>.
     * @throws IOException In case, adding or committing fails.
     */
    private void addDocument(Document document) throws IOException {
        indexWriter.addDocument(document);
        if (++modificationCounter % COMMIT_INTERVAL == 0) {
            LOGGER.trace("Added {} documents to index, committing ...", modificationCounter);
            indexWriter.commit();
        }
    }

    @Override
    public void addAlternativeNames(int locationId, Collection<AlternativeName> alternativeNames) {
        for (AlternativeName altName : alternativeNames) {
            Document document = new Document();
            document.add(new StringField(FIELD_ALT_ID, String.valueOf(locationId), Field.Store.YES));
            document.add(new TextField(FIELD_ALT_NAME, altName.getName(), Field.Store.YES));
            Language language = altName.getLanguage();
            String languageString = language != null ? language.getIso6391() : UNSPECIFIED_LANG;
            document.add(new StringField(FIELD_ALT_LANG, languageString, Field.Store.YES));
            try {
                addDocument(document);
            } catch (IOException e) {
                throw new IllegalStateException("Encountered IOException while adding alternative names", e);
            }
        }
    }

    @Override
    public int getHighestId() {
        throw new UnsupportedOperationException("#getHighestId is not supported.");
    }

    @Override
    public void close() throws IOException {
        LOGGER.debug("Committing and closing the IndexWriter.");
        try {
            indexWriter.commit();
        } finally {
            indexWriter.close();
        }
    }

}
