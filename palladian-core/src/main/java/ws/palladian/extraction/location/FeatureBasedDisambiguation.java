package ws.palladian.extraction.location;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.dt.BaggedDecisionTreeClassifier;
import ws.palladian.classification.dt.BaggedDecisionTreeModel;
import ws.palladian.extraction.location.LocationFeatureExtraction.LocationInstance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.processing.features.Annotated;

public class FeatureBasedDisambiguation implements LocationDisambiguation {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureBasedDisambiguation.class);

    private static final double PROBABILITY_THRESHOLD = 0.5;

    private final BaggedDecisionTreeClassifier classifier = new BaggedDecisionTreeClassifier();

    private final LocationFeatureExtraction featureExtractor = new LocationFeatureExtraction();

    private final BaggedDecisionTreeModel model;

    public FeatureBasedDisambiguation(BaggedDecisionTreeModel model) {
        Validate.notNull(model, "model must not be null");
        this.model = model;
    }

    @Override
    public List<LocationAnnotation> disambiguate(String text, List<Annotated> annotations,
            MultiMap<String, Location> locations) {

        Set<LocationInstance> instances = featureExtractor.makeInstances(text, annotations, locations);
        Map<Integer, Double> scoredLocations = CollectionHelper.newHashMap();

        for (LocationInstance instance : instances) {
            CategoryEntries classification = classifier.classify(instance, model);
            scoredLocations.put(instance.getId(), classification.getProbability("true"));
        }

        List<LocationAnnotation> result = CollectionHelper.newArrayList();
        for (Annotated annotation : annotations) {
            String value = LocationExtractorUtils.normalizeName(annotation.getValue());
            Collection<Location> candidates = locations.get(value);

            double highestScore = 0;
            Location selectedLocation = null;

            for (Location location : candidates) {
                double score = scoredLocations.get(location.getId());
                if (score > highestScore) {
                    highestScore = score;
                    selectedLocation = location;
                }
            }

            if (selectedLocation != null && highestScore >= PROBABILITY_THRESHOLD) {
                result.add(new LocationAnnotation(annotation, selectedLocation));
                Object[] logArgs = new Object[] {annotation.getValue(), highestScore, selectedLocation};
                LOGGER.debug("[+] '{}' was classified as location with {}: {}", logArgs);
            } else {
                LOGGER.debug("[-] '{}' was classified as no location with {}", annotation.getValue(), highestScore);
            }
        }
        return result;
    }

}
