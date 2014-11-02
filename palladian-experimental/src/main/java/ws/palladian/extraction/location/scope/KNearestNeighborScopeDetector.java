package ws.palladian.extraction.location.scope;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.TooManyClauses;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.GeoUtils;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;
import ws.palladian.helper.io.FileHelper;

/**
 * Nearest Neighbor taken literally; geo-tagged training documents are stored in an index. For extraction, we look up
 * similar document(s) and assign the coordinate of the similar document as scope.
 * 
 * @author pk
 * 
 */
public class KNearestNeighborScopeDetector implements ScopeDetector, Closeable {

    public interface QueryCreator {

        Query createQuery(String text, IndexReader reader, Analyzer analyzer) throws IOException;

    }

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(KNearestNeighborScopeDetector.class);

    private static final String FIELD_TEXT = "text";

    private static final String FIELD_LAT = "lat";

    private static final String FIELD_LNG = "lng";

    private static final Version LUCENE_VERSION = Version.LUCENE_47;

    private final Analyzer analyzer;

    private final int k;

    private final QueryCreator queryCreator;

    private final DirectoryReader reader;

    private final IndexSearcher searcher;

    public static final class NearestNeighborScopeModel implements TextClassifierScopeModel {

        private final Directory directory;

        public NearestNeighborScopeModel(Directory directory) {
            this.directory = directory;
        }

        /**
         * Open an existing Lucene nearest neighbor index, which was created using the
         * {@link NearestNeighborScopeDetectorLearner}.
         * 
         * @param indexPath The path to the index file, must point to a valid lucene index, not <code>null</code>.
         * @return The model
         */
        public static NearestNeighborScopeModel fromIndex(File indexPath) {
            Validate.notNull(indexPath, "indexPath must not be null");
            try {
                return new NearestNeighborScopeModel(FSDirectory.open(indexPath));
            } catch (IOException e) {
                throw new IllegalStateException("Error while trying to open '" + indexPath + "'.");
            }
        }

    }

    /**
     * Create a new {@link KNearestNeighborScopeDetector}.
     * 
     * @param model The model.
     * @param k The number of voting neighbors.
     * @param queryCreator The strategy for generating queries.
     */
    public KNearestNeighborScopeDetector(NearestNeighborScopeModel model, int k, QueryCreator queryCreator) {
        Validate.notNull(model, "model must not be null");
        Validate.isTrue(k > 0, "k must be greater zero");
        Validate.notNull(queryCreator, "queryCreator must not be null");

        this.k = k;
        this.queryCreator = queryCreator;
        try {
            this.reader = DirectoryReader.open(model.directory);
            this.analyzer = new FeatureSettingAnalyzer(new FeatureSetting(reader.getIndexCommit().getUserData()));
        } catch (IOException e) {
            throw new IllegalStateException();
        }
        this.searcher = new IndexSearcher(reader);
    }

    /**
     * Create a new {@link KNearestNeighborScopeDetector}.
     * 
     * @param model The model.
     * @param k The number of voting neighbors.
     */
    public KNearestNeighborScopeDetector(NearestNeighborScopeModel model, int k) {
        this(model, k, MORE_LIKE_THIS_QUERY_CREATOR);
    }

    /**
     * Learner, which is used for creating the necessary {@link NearestNeighborScopeModel}.
     * 
     * @author pk
     */
    public static final class NearestNeighborScopeDetectorLearner implements TextClassifierScopeDetectorLearner {

        private final FeatureSetting featureSetting;

        private final Directory directory;

        public NearestNeighborScopeDetectorLearner(File indexFile, FeatureSetting featureSetting) {
            Validate.notNull(indexFile, "indexFile must not be null");
            Validate.notNull(featureSetting, "featureSetting must not be null");
            this.featureSetting = featureSetting;
            try {
                this.directory = FSDirectory.open(indexFile);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        public NearestNeighborScopeDetectorLearner(Directory directory, FeatureSetting featureSetting) {
            Validate.notNull(directory, "directory must not be null");
            Validate.notNull(featureSetting, "featureSetting must not be null");
            this.directory = directory;
            this.featureSetting = featureSetting;
        }

        @Override
        public NearestNeighborScopeModel train(Iterable<? extends LocationDocument> documentIterator) {
            Validate.notNull(documentIterator, "documentIterator must not be null");
            Analyzer analyzer = new FeatureSettingAnalyzer(featureSetting, LUCENE_VERSION);
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(LUCENE_VERSION, analyzer);
            IndexWriter indexWriter = null;
            try {
                indexWriter = new IndexWriter(directory, indexWriterConfig);
                for (LocationDocument locationDocument : documentIterator) {
                    Location location = locationDocument.getMainLocation();
                    if (location == null) {
                        continue;
                    }
                    GeoCoordinate coordinate = location.getCoordinate();
                    if (coordinate == null) {
                        continue;
                    }
                    Document document = new Document();
                    document.add(new TextField(FIELD_TEXT, locationDocument.getText(), Field.Store.NO));
                    document.add(new DoubleField(FIELD_LAT, coordinate.getLatitude(), DoubleField.TYPE_STORED));
                    document.add(new DoubleField(FIELD_LNG, coordinate.getLongitude(), DoubleField.TYPE_STORED));
                    indexWriter.addDocument(document);
                }
                Map<String, String> featureSettingData = featureSetting.toMap();
                indexWriter.setCommitData(featureSettingData);
                indexWriter.commit();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } finally {
                FileHelper.close(indexWriter);
            }
            return new NearestNeighborScopeModel(directory);
        }

    }

    @Override
    public GeoCoordinate getScope(String text) {
        try {
            Query query = queryCreator.createQuery(text, reader, analyzer);
            LOGGER.debug("{} = {}", query.getClass().getSimpleName(), query);
            TopDocs searchResult = searcher.search(query, k);
            if (searchResult.totalHits == 0) {
                return null;
            }
            List<GeoCoordinate> coordinates = CollectionHelper.newArrayList();
            float maxScore = searchResult.scoreDocs[0].score;
            for (int i = 0; i < searchResult.scoreDocs.length; i++) {
                ScoreDoc scoreDoc = searchResult.scoreDocs[i];
                Document document = searcher.doc(scoreDoc.doc);
                double lat = document.getField(FIELD_LAT).numericValue().doubleValue();
                double lng = document.getField(FIELD_LNG).numericValue().doubleValue();
                try {
                    GeoCoordinate coordinate = new ImmutableGeoCoordinate(lat, lng);
                    // multiply the score, so that we add n items to the list
                    // from which we determine the center
                    int factor = Math.round(10 * scoreDoc.score / maxScore);
                    LOGGER.debug("{} : {} (n={})", coordinate, scoreDoc.score / maxScore, factor);
                    coordinates.addAll(Collections.nCopies(factor, coordinate));
                } catch (IllegalArgumentException e) {
                    // weird shit
                }
            }
            if (coordinates.isEmpty()) {
                return null;
            }
            StopWatch stopWatch = new StopWatch();
            GeoCoordinate center = GeoUtils.getCenterOfMinimumDistance(coordinates);
            LOGGER.debug("calculation for {} took {}", coordinates.size(), stopWatch);
            return center;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Create a combined {@link BooleanQuery} from the document.
     */
    public static final QueryCreator BOOLEAN_QUERY_CREATOR = new QueryCreator() {
        private static final String NAME = "Boolean";

        @Override
        public Query createQuery(String text, IndexReader reader, Analyzer analyzer) throws IOException {
            BooleanQuery query = new BooleanQuery();
            TokenStream stream = analyzer.tokenStream(null, new StringReader(text));
            stream.reset();
            try {
                while (stream.incrementToken()) {
                    String token = stream.getAttribute(CharTermAttribute.class).toString();
                    try {
                        query.add(new TermQuery(new Term("text", token)), Occur.SHOULD);
                    } catch (TooManyClauses e) {
                        break;
                    }
                }
            } finally {
                stream.close();
            }
            return query;
        }

        @Override
        public String toString() {
            return NAME;
        };
    };

    /**
     * Use the {@link MoreLikeThis} implementation to create a query from a document.
     */
    public static final QueryCreator MORE_LIKE_THIS_QUERY_CREATOR = new QueryCreator() {
        private static final String NAME = "MoreLikeThis";

        @Override
        public Query createQuery(String text, IndexReader reader, Analyzer analyzer) throws IOException {
            MoreLikeThis moreLikeThis = new MoreLikeThis(reader);
            moreLikeThis.setFieldNames(new String[] {FIELD_TEXT});
            moreLikeThis.setAnalyzer(analyzer);
            moreLikeThis.setMinTermFreq(1);
            moreLikeThis.setMinDocFreq(1);
            return moreLikeThis.like(new StringReader(text), FIELD_TEXT);
        }

        @Override
        public String toString() {
            return NAME;
        };
    };

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [analyzer=" + analyzer + ", k=" + k + ", queryGenerator=" + queryCreator
                + ", numDocs=" + reader.numDocs() + "]";
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @SuppressWarnings("resource")
    public static void main(String[] args) throws IOException {
        // File trainingDirectory = new File("/Users/pk/Desktop/WikipediaScopeDataset-2014/split-1");
        // Iterable<LocationDocument> trainingSet = new WikipediaLocationScopeIterator(trainingDirectory);

        // NearestNeighborScopeModel model = KNearestNeighborScopeDetector.train(trainingSet,
        // FeatureSettingBuilder.words(1).create());
        // FileHelper.serialize(model, "nearestNeighborScope.model");

        File indexPath = new File("/Users/pk/temp/nearestNeighborScopeModel_wikipedia_90-train");
        NearestNeighborScopeModel model = NearestNeighborScopeModel.fromIndex(indexPath);
        // KNearestNeighborScopeDetector detector = new KNearestNeighborScopeDetector(model, 10, new
        // BooleanQueryCreator());
        KNearestNeighborScopeDetector detector = new KNearestNeighborScopeDetector(model, 10,
                MORE_LIKE_THIS_QUERY_CREATOR);
        String text = FileHelper.readFileToString("/Users/pk/Desktop/text_43259724.txt");
        // System.out.println(detector.getScope("Dresden is a city in Germany, Saxony."));
        // System.out.println(detector.getScope("Flein is a small town near Heilbronn in Germany"));
        System.out.println(detector.getScope(text));
    }

}
