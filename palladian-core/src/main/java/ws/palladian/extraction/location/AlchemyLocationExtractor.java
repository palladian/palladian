package ws.palladian.extraction.location;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.tagger.AlchemyNer;
import ws.palladian.helper.collection.CollectionHelper;

public class AlchemyLocationExtractor extends WebBasedLocationExtractor {

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
        super(new AlchemyNer(apiKey));
        setName("Alchemy Location Extractor");
    }

    @Override
    public String getModelFileEnding() {
        throw new UnsupportedOperationException(
                "this location detector does not support training and does not work with model files");
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        return false;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        throw new UnsupportedOperationException(
                "this location detector does not support training and does not work with model files");
    }

    @Override
    public Annotations getAnnotations(String inputText, String configModelFilePath) {
        LOGGER.warn("the configModelFilePath is ignored");
        return getAnnotations(inputText);
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {
        throw new UnsupportedOperationException(
                "this location detector does not support training and does not work with model files");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        AlchemyLocationExtractor alchemyLocationExtractor = new AlchemyLocationExtractor("get your own key");
        List<Location> detectedLocations = alchemyLocationExtractor
                .detectLocations("Dresden and Berlin are cities in Germany which lies in Europe on planet Earth");
        CollectionHelper.print(detectedLocations);
    }
}
