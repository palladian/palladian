/**
 * Created on: 21.11.2012 15:01:59
 */
package ws.palladian.processing.features;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import ws.palladian.processing.Classifiable;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since
 */
public final class FeatureUtils {

    private FeatureUtils() {
        throw new UnsupportedOperationException("Unable to instantiate utility class "
                + this.getClass().getCanonicalName());
    }

    public static <T extends Feature<?>> Set<T> convertToSet(FeatureVector vector, Class<T> featureClass,
            String featurePath) {
        return new HashSet<T>(getFeaturesAtPath(vector, featureClass, featurePath));
    }

    public static Iterable<Feature<?>> iterateRecursively(final FeatureVector vector) {
        return new Iterable<Feature<?>>() {
            @Override
            public Iterator<Feature<?>> iterator() {
                return new FeatureIterator(vector);
            }
        };
    }

    public static <T extends Feature<?>> List<T> getFeaturesAtPath(FeatureVector vector, Class<T> featureClass,
            String featurePath) {
        int slashIndex = featurePath.indexOf("/");
        String leadingPathPart = slashIndex == -1 ? featurePath : featurePath.substring(0, slashIndex);
        String trailingPathPart = slashIndex == -1 ? "" : featurePath.substring(slashIndex + 1, featurePath.length());
        List<Feature<?>> featureList = vector.getAll(leadingPathPart);

        List<T> ret = new ArrayList<T>();
        if (!trailingPathPart.isEmpty()) {
            for (Feature<?> feature : featureList) {
                if (feature instanceof Classifiable) {
                    Classifiable classifiable = (Classifiable)feature;
                    ret.addAll(getFeaturesAtPath(classifiable.getFeatureVector(), featureClass, trailingPathPart));
                }
            }
        } else {
            for (Feature<?> feature : featureList) {
                if (featureClass.isInstance(feature)) {
                    ret.add(featureClass.cast(feature));
                }
            }
        }
        return ret;
    }

    public static List<Feature<?>> getFeaturesAtPath(FeatureVector vector, String featurePath) {
        int slashIndex = featurePath.indexOf("/");
        String leadingPathPart = slashIndex == -1 ? featurePath : featurePath.substring(0, slashIndex);
        String trailingPathPart = slashIndex == -1 ? "" : featurePath.substring(slashIndex + 1, featurePath.length());
        List<Feature<?>> featureList = vector.getAll(leadingPathPart);

        List<Feature<?>> ret = new ArrayList<Feature<?>>();
        if (!trailingPathPart.isEmpty()) {
            for (Feature<?> feature : featureList) {
                if (feature instanceof Classifiable) {
                    Classifiable classifiable = (Classifiable)feature;
                    ret.addAll(getFeaturesAtPath(classifiable.getFeatureVector(), trailingPathPart));
                }
            }
        } else {
            for (Feature<?> feature : featureList) {
                ret.add(feature);
            }
        }
        return ret;
    }

    /**
     * <p>
     * Removes the feature with the specified value at the provided path from the {@link FeatureVector}.
     * </p>
     * 
     * @param featureVector the {@link FeatureVector} to remove a {@link Feature} from.
     * @param featurePath The path the the feature to remove.
     * @param value The value of the feature to remove at the path.
     */
    public static void removeFeatureAtPath(FeatureVector featureVector, String featurePath, String value) {
        int slashIndex = featurePath.indexOf("/");
        String leadingPathPart = slashIndex == -1 ? featurePath : featurePath.substring(0, slashIndex);
        String trailingPathPart = slashIndex == -1 ? "" : featurePath.substring(slashIndex + 1, featurePath.length());
        List<Feature<?>> featureList = new ArrayList<Feature<?>>(featureVector.getAll(leadingPathPart));

        if (!trailingPathPart.isEmpty()) {
            for (Feature<?> feature : featureList) {
                if (feature instanceof Classifiable) {
                    Classifiable classifiable = (Classifiable)feature;
                    removeFeatureAtPath(classifiable.getFeatureVector(), trailingPathPart, value);
                }
            }
        } else {
            for (Feature<?> feature : featureList) {
                if (feature.getValue().equals(value)) {
                    featureVector.remove(feature);
                }
            }
        }
    }

    /**
     * <p>
     * Finds all occurrences of the {@link Feature} inside the provided {@link FeatureVector} including its
     * {@link Feature}s {@link FeatureVector}s.
     * </p>
     * 
     * @param feature An instance of the {@link Feature} to search for.
     * @param featureVector The {@link FeatureVector} to search.
     * @return A {@link List} of the found {@link Feature}s.
     */
    public static List<? extends Feature<?>> find(Feature<?> feature, FeatureVector featureVector) {
        List<Feature<?>> ret = new ArrayList<Feature<?>>();
        for (Feature<?> existingFeature : featureVector) {
            if (existingFeature.getName().equals(feature.getName())) {
                ret.add(existingFeature);
            } else if (existingFeature instanceof Classifiable) {
                ret.addAll(find(feature, featureVector));
            }
        }
        return ret;
    }
}

/**
 * <p>
 * Recursively iterates over all {@link Feature}s of a {@link FeatureVector} also providing the {@link Feature}s of the
 * {@link Feature}s. This happens if a {@link Feature} is a classifiable and thus has got a {@link FeatureVector}
 * itself.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
class FeatureIterator implements Iterator<Feature<?>> {

    /**
     * <p>
     * A {@link Stack} storing the {@link Iterator}s for all {@link Classifiable} {@link Feature}s. The top of the
     * {@link Stack} stores the {@link Iterator} for the {@link FeatureVector} currently iterated over.
     * </p>
     */
    private final Stack<Iterator<? extends Feature<?>>> iteratorStack;

    /**
     * <p>
     * Creates a new completely initialized {@link FeatureIterator}.
     * </p>
     * 
     * @param The {@link FeatureVector} to iterate over.
     */
    public FeatureIterator(FeatureVector vector) {
        iteratorStack = new Stack<Iterator<? extends Feature<?>>>();
        iteratorStack.push(vector.getAll().iterator());
    }

    @Override
    public boolean hasNext() {
        return !iteratorStack.isEmpty();
    }

    @Override
    public Feature<?> next() {
        try {
            Iterator<? extends Feature<?>> currentIterator = iteratorStack.pop();
            Feature<?> feature = currentIterator.next();

            if (feature instanceof Classifiable) {
                Classifiable annotationFeature = (Classifiable)feature;
                iteratorStack.push(annotationFeature.getFeatureVector().iterator());
            }

            if (currentIterator.hasNext()) {
                iteratorStack.push(currentIterator);
            }

            return feature;
        } catch (EmptyStackException e) {
            throw new IllegalStateException("Iterator has no more elements");
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("This operation is not supported by the FeatureIterator");
    }

}
