package ws.palladian.extraction.location.disambiguation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.dt.QuickDtClassifier;
import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.core.Annotation;
import ws.palladian.core.CategoryEntries;
import ws.palladian.extraction.location.ClassifiedAnnotation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.MultiMap;

/**
 * <p>
 * A disambiguation approach using machine learning. The required models can be created using the
 * {@link FeatureBasedDisambiguationLearner}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class FeatureBasedDisambiguation implements LocationDisambiguation {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureBasedDisambiguation.class);

    public static final double PROBABILITY_THRESHOLD = 0.15;
    
    /** The size of the disambiguation context. See {@link DisambiguationContext}. */
    public static final int CONTEXT_SIZE = 1000;

    private final double probabilityThreshold;

    private final QuickDtClassifier classifier = new QuickDtClassifier();

    private final LocationFeatureExtractor featureExtractor;

    private final QuickDtModel model;

    public FeatureBasedDisambiguation(QuickDtModel model) {
        this(model, PROBABILITY_THRESHOLD, CONTEXT_SIZE);
    }

    public FeatureBasedDisambiguation(QuickDtModel model, double probabilityThreshold, int contextSize) {
        this(model, probabilityThreshold, new DefaultLocationFeatureExtractor(contextSize));
    }

    public FeatureBasedDisambiguation(QuickDtModel model, double probabilityThreshold,
            LocationFeatureExtractor featureExtractor) {
        Validate.notNull(model, "model must not be null");
        Validate.inclusiveBetween(0., 1., probabilityThreshold,
                "probabilityThreshold must be between inclusive 0 and 1.");
        Validate.notNull(featureExtractor, "featureExtractor must not be null");
        this.model = model;
        this.probabilityThreshold = probabilityThreshold;
        this.featureExtractor = featureExtractor;
    }

    @Override
    public List<LocationAnnotation> disambiguate(String text, MultiMap<ClassifiedAnnotation, Location> locations) {

        Set<ClassifiableLocation> classifiableLocations = featureExtractor.extract(text, locations);
        Map<Integer, Double> scoredLocations = CollectionHelper.newHashMap();

        for (ClassifiableLocation classifiableLocation : classifiableLocations) {
            CategoryEntries classification = classifier.classify(classifiableLocation.getFeatureVector(), model);
            scoredLocations.put(classifiableLocation.getLocation().getId(), classification.getProbability("true"));
        }

        List<LocationAnnotation> result = CollectionHelper.newArrayList();
        for (Annotation annotation : locations.keySet()) {
            Collection<Location> candidates = locations.get(annotation);

            double highestScore = 0;
            Location selectedLocation = null;

            for (Location location : candidates) {
                double score = scoredLocations.get(location.getId());
                if (selectedLocation == null || score > highestScore) {
                    highestScore = score;
                    selectedLocation = location;
                }
            }

            if (selectedLocation != null && highestScore >= probabilityThreshold) {
                result.add(new LocationAnnotation(annotation, selectedLocation, highestScore));
                Object[] logArgs = new Object[] {annotation.getValue(), highestScore, selectedLocation};
                LOGGER.debug("[+] '{}' was classified as location with {}: {}", logArgs);
            } else {
                LOGGER.debug("[-] '{}' was classified as no location with {}", annotation.getValue(), highestScore);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FeatureBasedDisambiguation [probabilityThreshold=");
        builder.append(probabilityThreshold);
        builder.append(", featureExtractor=");
        builder.append(featureExtractor);
        builder.append("]");
        return builder.toString();
    }

}
