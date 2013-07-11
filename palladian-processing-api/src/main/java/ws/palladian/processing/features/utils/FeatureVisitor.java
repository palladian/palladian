/**
 * Created on: 22.05.2013 09:34:39
 */
package ws.palladian.processing.features.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;

import ws.palladian.processing.Classifiable;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * Traverses a {@link FeatureVector} and calls its {@link #template(Feature, FeaturePath)} method for every feature it
 * encounters.
 * </p>
 * <p>
 * Modifying the {@link FeatureVector} during processing will cause {@link ConcurrentModificationException}s.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 2.1.0
 */
public abstract class FeatureVisitor {
    /**
     * <p>
     * Visits each {@link Feature} in the provided {@link FeatureVector}.
     * </p>
     * 
     * @param vector The {@link FeatureVector} to visit.
     */
    public void visit(FeatureVector vector) {
        internalVisit(vector, Collections.<FeatureDescriptor> emptyList());
    }

    /**
     * <p>
     * Recursively visits every {@link Feature} in the {@link FeatureVector}. Returns {@code false} if processing the
     * vector should stop after the method was called. This enables the implementing algorithm to break on errors or if
     * it found what it was searching for to reduce computation cost.
     * </p>
     * <p>
     * The search algorithm is implemented as a depth first search.
     * </p>
     * 
     * @param vector The {@link FeatureVector} to process.
     * @param parentPath The {@link FeaturePath} to the parent {@link Feature}.
     * @return {@code false} if processing the vector should stop at this point; {@code true} otherwise.
     */
    protected boolean internalVisit(FeatureVector vector, List<FeatureDescriptor> parentFeatures) {
        for (Feature<?> feature : vector.getAll()) {
            if (!template(feature, parentFeatures)) {
                return false;
            }

            if (feature instanceof Classifiable) {
                Classifiable classifiable = (Classifiable)feature;
                List<FeatureDescriptor> path = new ArrayList<FeatureDescriptor>(parentFeatures);
                path.add(new FeatureDescriptor(feature.getName(), feature.getValue().toString()));
                if (!internalVisit(classifiable.getFeatureVector(), path)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * <p>
     * Runs the algorithm to carry out on every {@link Feature} in this {@link FeatureVector}.
     * </p>
     * 
     * @param feature The {@link Feature} to process.
     * @param parentFeatures The {@link FeaturePath} to the parent feature.
     * @return
     */
    protected abstract boolean template(Feature<?> feature, List<FeatureDescriptor> parentFeatures);
}
