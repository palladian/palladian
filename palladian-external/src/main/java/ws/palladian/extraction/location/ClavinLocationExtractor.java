package ws.palladian.extraction.location;

import com.bericotech.clavin.ClavinException;
import com.bericotech.clavin.GeoParser;
import com.bericotech.clavin.GeoParserFactory;
import com.bericotech.clavin.extractor.LocationOccurrence;
import com.bericotech.clavin.gazetteer.GeoName;
import com.bericotech.clavin.resolver.ResolvedLocation;
import org.apache.commons.lang3.Validate;
import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.extraction.location.evaluation.LocationExtractionEvaluator;
import ws.palladian.extraction.location.sources.importers.GeonamesUtil;
import ws.palladian.helper.geo.GeoCoordinate;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Wrapper for CLAVIN (Cartographic Location And Vicinity INdexer) by Berico Technologies. Description from the website:
 * <i>CLAVIN (Cartographic Location And Vicinity INdexer) is an award-winning open source software package for document
 * geotagging and geoparsing that employs context-based geographic entity resolution.<br />
 *
 * It extracts location names from unstructured text and resolves them against a gazetteer to produce data-rich
 * geographic entities.<br />
 *
 * CLAVIN does not simply look up location names – it uses intelligent heuristics to identify exactly which Springfield
 * (for example) was intended by the author, based on the context of the document. CLAVIN also employs fuzzy search to
 * handle incorrectly-spelled location names, and it recognizes alternative names (e.g., Ivory Coast and Côte d'Ivoire)
 * as referring to the same geographic entity.<br />
 *
 * By enriching text documents with structured geo data, CLAVIN enables hierarchical geospatial search and advanced
 * geospatial analytics on unstructured data.</i>
 * </p>
 *
 * @author Philipp Katz
 * @see <a href="http://clavin.bericotechnologies.com">CLAVIN</a>
 */
public final class ClavinLocationExtractor extends LocationExtractor {

    /** The name of this {@link LocationExtractor}. */
    private static final String NAME = "CLAVIN";

    private final GeoParser parser;

    /**
     * <p>
     * Create a new {@link ClavinLocationExtractor}.
     * </p>
     *
     * @param pathToLuceneIndex The path to the previously created Lucene index. See <a
     *                          href="http://clavin.bericotechnologies.com/site/tutorials/installation.html">here, step 6</a>.
     */
    public ClavinLocationExtractor(String pathToLuceneIndex) {
        Validate.notEmpty(pathToLuceneIndex, "pathToLuceneIndex must not be empty");
        try {
            this.parser = GeoParserFactory.getDefault(pathToLuceneIndex, 1/* maxHitDepth */, 1/* maxContentWindow */, false);
        } catch (ClavinException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<LocationAnnotation> getAnnotations(String inputText) {
        try {
            List<LocationAnnotation> result = new ArrayList<>();
            List<ResolvedLocation> resolvedLocations = parser.parse(inputText);
            for (ResolvedLocation resolvedLocation : resolvedLocations) {
                result.add(makeLocationAnnotation(resolvedLocation));
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private LocationAnnotation makeLocationAnnotation(ResolvedLocation resolvedLocation) {
        LocationOccurrence locOccurrence = resolvedLocation.getLocation();
        int startPosition = locOccurrence.getPosition();
        String value = locOccurrence.getText();
        GeoName geoname = resolvedLocation.getGeoname();
        int id = geoname.getGeonameID();
        String primaryName = geoname.getName();
        String featureClass = geoname.getFeatureClass().toString();
        String featureType = geoname.getFeatureCode().toString();
        LocationType type = GeonamesUtil.mapType(featureClass, featureType);
        GeoCoordinate coordinate = GeoCoordinate.from(geoname.getLatitude(), geoname.getLongitude());
        Long population = geoname.getPopulation();
        Annotation annotation = new ImmutableAnnotation(startPosition, value, type.toString());
        Location location = new ImmutableLocation(id, primaryName, type, coordinate, population);
        return new LocationAnnotation(annotation, location);
    }

    @Override
    public String getName() {
        return NAME;
    }

    public static void main(String[] args) {
        LocationExtractionEvaluator evaluator = new LocationExtractionEvaluator();
        evaluator.addDataset("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/TUD-Loc-2013_V2/3-test");
        evaluator.addDataset("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/3-test");
        evaluator.addDataset("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/CLUST-converted/3-test");
        evaluator.addExtractor(new ClavinLocationExtractor("./IndexDirectory"));
        evaluator.runAll(true);
    }

}
