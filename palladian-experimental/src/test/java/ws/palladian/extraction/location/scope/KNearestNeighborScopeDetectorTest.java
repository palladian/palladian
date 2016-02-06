package ws.palladian.extraction.location.scope;

import static org.junit.Assert.assertEquals;
import static ws.palladian.extraction.location.scope.KNearestNeighborScopeDetector.BOOLEAN_QUERY_CREATOR;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;

import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.extraction.location.scope.KNearestNeighborScopeDetector.NearestNeighborScopeDetectorLearner;
import ws.palladian.extraction.location.scope.KNearestNeighborScopeDetector.NearestNeighborScopeModel;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;

public class KNearestNeighborScopeDetectorTest {

    private static final double DELTA = 0.001;

    private static final String TEST_TEXT = "Chinese Chinese Chinese Tokyo Japan";

    @Test
    @SuppressWarnings("resource")
    public void testNearestNeighborScopeDetector() throws IOException {
        FeatureSetting featureSetting = FeatureSettingBuilder.words().create();
        List<LocationDocument> docs = getTestDocs();
        NearestNeighborScopeModel model = new NearestNeighborScopeDetectorLearner(new RAMDirectory(), featureSetting)
                .train(docs);

        ScopeDetector detector = new KNearestNeighborScopeDetector(model, 1, BOOLEAN_QUERY_CREATOR);
        GeoCoordinate scope = detector.getScope(TEST_TEXT);
        assertEquals(35.684, scope.getLatitude(), DELTA);
        assertEquals(139.774, scope.getLongitude(), DELTA);

        detector = new KNearestNeighborScopeDetector(model, 3, BOOLEAN_QUERY_CREATOR);
        scope = detector.getScope(TEST_TEXT);
        assertEquals(35.684, scope.getLatitude(), DELTA);
        assertEquals(139.774, scope.getLongitude(), DELTA);
    }

    private static List<LocationDocument> getTestDocs() {
        LocationDocument doc1 = createDoc("Chinese Beijing Chinese", 39.928887, 116.388338);
        LocationDocument doc2 = createDoc("Chinese Chinese Shanghai", 31.233333, 121.466667);
        LocationDocument doc3 = createDoc("Chinese Macao", 22.198611, 113.544722);
        LocationDocument doc4 = createDoc("Tokyo Japan Chinese", 35.683889, 139.774444);
        List<LocationDocument> docs = Arrays.asList(doc1, doc2, doc3, doc4);
        return docs;
    }

    private static LocationDocument createDoc(String text, double lat, double lng) {
        GeoCoordinate coordinate = new ImmutableGeoCoordinate(lat, lng);
        Location location = new ImmutableLocation(0, StringUtils.EMPTY, LocationType.UNDETERMINED, coordinate, null);
        return new LocationDocument(StringUtils.EMPTY, text, Collections.<LocationAnnotation> emptyList(), location);
    }

}
