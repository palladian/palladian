package ws.palladian.extraction.location.disambiguation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.dt.BaggedDecisionTreeClassifier;
import ws.palladian.classification.dt.BaggedDecisionTreeModel;
import ws.palladian.extraction.location.ContextClassifier.ClassifiedAnnotation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.disambiguation.LocationFeatureExtractor.LocationInstance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Filter;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.processing.features.Annotation;

/**
 * <p>
 * {@link LocationDisambiguation} which combines a feature-based approach with the {@link HeuristicDisambiguation}; the
 * feature-based approach is used to filter do narrow down the potential location candidates, then the heuristic
 * strategy selects from the trimmed down candidate set.
 * </p>
 * 
 * @author Philipp Katz
 */
public class CombinedDisambiguation implements LocationDisambiguation {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CombinedDisambiguation.class);

    private final BaggedDecisionTreeClassifier classifier = new BaggedDecisionTreeClassifier();

    private final LocationFeatureExtractor featureExtractor = new LocationFeatureExtractor();

    private final BaggedDecisionTreeModel model;

    private final HeuristicDisambiguation heuristicDisambiguation = new HeuristicDisambiguation();

    public CombinedDisambiguation(BaggedDecisionTreeModel model) {
        Validate.notNull(model, "model must not be null");
        this.model = model;
    }

    @Override
    public List<LocationAnnotation> disambiguate(String text, MultiMap<ClassifiedAnnotation, Location> locations) {

        Set<LocationInstance> instances = featureExtractor.makeInstances(text, locations);
        final Map<Integer, Double> scoredLocations = CollectionHelper.newHashMap();

        for (LocationInstance instance : instances) {
            CategoryEntries classification = classifier.classify(instance, model);
            scoredLocations.put(instance.getId(), classification.getProbability("true"));
        }
        LOGGER.debug("# candidates before classification: {}", locations.allValues().size());

        for (Annotation annotation : locations.keySet()) {
            CollectionHelper.remove(locations.get(annotation), new Filter<Location>() {
                @Override
                public boolean accept(Location item) {
                    return scoredLocations.get(item.getId()) > 0;
                }
            });
        }
        LOGGER.debug("# candidates after classification: {}", locations.allValues().size());
        return heuristicDisambiguation.disambiguate(text, locations);
    }

    @Override
    public String toString() {
        return "CombinedDisambiguation";
    }

}
