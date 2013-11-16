package ws.palladian.extraction.location.clavin;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.lucene.queryparser.classic.ParseException;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.ImmutableGeoCoordinate;
import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.evaluation.LocationExtractionEvaluator;
import ws.palladian.extraction.location.sources.importers.GeonamesUtil;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.ImmutableAnnotation;

import com.bericotech.clavin.GeoParser;
import com.bericotech.clavin.GeoParserFactory;
import com.bericotech.clavin.extractor.LocationOccurrence;
import com.bericotech.clavin.gazetteer.GeoName;
import com.bericotech.clavin.resolver.ResolvedLocation;

public final class ClavinLocationExtractor extends LocationExtractor {

    private static final String NAME = "CLAVIN";

    private final GeoParser parser;

    public ClavinLocationExtractor(String pathToLuceneIndex) {
        Validate.notNull(pathToLuceneIndex, "pathToLuceneIndex must not be null");
        try {
            this.parser = GeoParserFactory.getDefault(pathToLuceneIndex);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<LocationAnnotation> getAnnotations(String inputText) {
        try {
            List<LocationAnnotation> result = CollectionHelper.newArrayList();
            List<ResolvedLocation> resolvedLocations = parser.parse(inputText);
            for (ResolvedLocation resolvedLocation : resolvedLocations) {
                LocationOccurrence locOccurrence = resolvedLocation.location;
                int startPosition = locOccurrence.position;
                String value = locOccurrence.text;
                GeoName geoname = resolvedLocation.geoname;
                int id = geoname.geonameID;
                String primaryName = geoname.name;
                String featureClass = geoname.featureClass.toString();
                String featureType = geoname.featureCode.toString();
                LocationType type = GeonamesUtil.mapType(featureClass, featureType);
                GeoCoordinate coordinate = new ImmutableGeoCoordinate(geoname.latitude, geoname.longitude);
                Long population = geoname.population;
                Annotation annotation = new ImmutableAnnotation(startPosition, value, type.toString());
                Location location = new ImmutableLocation(id, primaryName, type, coordinate, population);
                result.add(new LocationAnnotation(annotation, location));
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
