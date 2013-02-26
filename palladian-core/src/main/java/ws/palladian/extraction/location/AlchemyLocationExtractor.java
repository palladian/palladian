package ws.palladian.extraction.location;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.tagger.AlchemyNer;
import ws.palladian.helper.collection.CollectionHelper;

public class AlchemyLocationExtractor extends WebBasedLocationExtractor {

    private static final Map<String, LocationType> LOCATION_MAPPING;

    static {
        Map<String, LocationType> temp = CollectionHelper.newHashMap();
        temp.put("city", LocationType.CITY);
        temp.put("country", LocationType.COUNTRY);
        temp.put("facility", LocationType.POI);
        temp.put("geographicfeature", LocationType.LANDMARK);
        temp.put("region", LocationType.REGION);
        temp.put("stateorcounty", LocationType.UNIT);
        LOCATION_MAPPING = Collections.unmodifiableMap(temp);
    }

    public AlchemyLocationExtractor(String apiKey) {
        super(new AlchemyNer(apiKey), LOCATION_MAPPING);
    }

    public static void main(String[] args) {
        AlchemyLocationExtractor extractor = new AlchemyLocationExtractor("get your own key");
        String text = "Dresden and Berlin are cities in Germany which lies in Europe on planet Earth";
        List<Annotation> detectedLocations = extractor.getAnnotations(text);
        CollectionHelper.print(detectedLocations);
    }

}
