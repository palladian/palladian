package ws.palladian.extraction.location.disambiguation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.ClassifiedAnnotation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;

/**
 * Wraps a {@link FeatureBasedDisambiguation} and performs the disambiguation
 * iteratively, starting from the candidates with the highest trust down to the
 * lowest trust -- in each iteration, only the fittest candidates above the
 * threshold will survive.
 *
 * The idea is to remove unfit candidates which would negatively impact other,
 * yet unclassified instances.
 *
 * This will give a little gain over a pure {@link FeatureBasedDisambiguation},
 * however at the expense of a considerable speed penalty (approx. 2x).
 *
 * TODO -- evaluate whether we can better train towards this approach (e.g. by
 * better exploiting sure-positive candidates, and not all)
 *
 * @author Philipp Katz
 */
public class DarwinDisambiguation implements LocationDisambiguation {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureBasedDisambiguation.class);

    private static final int DISAMBIGUATION_STEPS = 4;

    private final FeatureBasedDisambiguation base;

    public DarwinDisambiguation(FeatureBasedDisambiguation base) {
        this.base = Objects.requireNonNull(base, "base must not be null");
    }

    @Override
    public List<LocationAnnotation> disambiguate(String text, MultiMap<ClassifiedAnnotation, Location> locations) {

        // copy the locations
        MultiMap<ClassifiedAnnotation, Location> locationsCopy = DefaultMultiMap.createWithList();
        locationsCopy.addAll(locations);

        List<LocationAnnotation> result = Collections.emptyList();

        // perform iterative classification
        for (int step = DISAMBIGUATION_STEPS - 1; step >= 0; step--) {

            double minTrust = (double) step / DISAMBIGUATION_STEPS;

            result = base.disambiguate(text, locationsCopy);

            List<LocationAnnotation> fittestOnes = result.stream() //
                    .filter(annotation -> annotation.getTrust() >= minTrust) //
                    .collect(Collectors.toList());

            LOGGER.debug("Step {} with minTrust {}: Selected {} candidates", step, minTrust, fittestOnes.size());

            for (LocationAnnotation locationAnnotation : fittestOnes) {
                // if it occurs in the candidate set, resolve to this and discard all others
                for (Entry<ClassifiedAnnotation, Collection<Location>> entry : locationsCopy.entrySet()) {
                    if (entry.getValue().contains(locationAnnotation.getLocation())) {
                        entry.setValue(Collections.singletonList(locationAnnotation.getLocation()));
                    }
                }
            }
        }

        return result;
    }

    @Override
    public String toString() {
        return String.format("DarwinDisambiguation [base=%s]", base);
    }

}
