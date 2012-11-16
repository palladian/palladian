package ws.palladian.processing.features;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * <p>
 * A class to describe collections of {@code Feature}s extracted from some document. Based on its {@code FeatureVector}
 * the document can be processed by Information Retrieval components like classifiers or clusterers.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class FeatureVector implements Iterable<Feature<?>> {

    /**
     * <p>
     * A map of all {@code Feature}s in this vector. It maps from the {@code Feature}s {@code FeatureVector} wide unique
     * identifier to an actual {@code Feature} instance containing the value. The value might be of any java object
     * type.
     * </p>
     */
    private final SortedMap<String, List<Feature<?>>> features;

    /**
     * <p>
     * Creates a new empty {@code FeatureVector}. To fill it with {@link Feature}s call {@link #add(String, Feature)}.
     * </p>
     */
    public FeatureVector() {
        features = new TreeMap<String, List<Feature<?>>>();
    }

    /**
     * <p>
     * Creates a new {@link FeatureVector} from the provided FeatureVector, i.e. a copy with all {@link Feature}s.
     * </p>
     * 
     * @param featureVector The feature vector which Features to copy.
     */
    public FeatureVector(FeatureVector featureVector) {
        features = new TreeMap<String, List<Feature<?>>>(featureVector.features);
    }

    /**
     * <p>
     * Adds a new {@code Feature} to this {@code FeatureVector}.
     * </p>
     * 
     * @param feature
     *            The actual {@code Feature} instance containing the value.
     */
    public void add(Feature<?> feature) {
        List<Feature<?>> list = features.get(feature.getName());
        if (list == null) {
            list = new ArrayList<Feature<?>>();
            features.put(feature.getName(), list);
        }
        list.add(feature);
    }
    
    public void addAll(Iterable<? extends Feature<?>> features) {
        for (Feature<?> feature : features) {
            add(feature);
        }
    }

//    /**
//     * <p>
//     * Provides the first feature matching the provided path ignoring the class of the {@code Feature}.
//     * </p>
//     * 
//     * @param featurePath
//     * @return
//     */
//    public Feature<?> getFeature(String featurePath) {
//        if (featurePath.startsWith("/")) {
//            featurePath = featurePath.substring(1);
//        }
//        String[] pathElements = featurePath.split("/");
//        List<Feature<?>> selectedFeatures = features.get(pathElements[0]);
//        if (selectedFeatures == null) {
//            return null;
//        }
//        Feature<?> selectedFeature = selectedFeatures.get(0);
//        if (pathElements.length > 1) {
//            AnnotationFeature<?> annotationFeature = (AnnotationFeature<?>)selectedFeature;
//            for (Annotation<?> annotation : annotationFeature.getAnnotations()) {
//                selectedFeature = annotation.getFeatureVector().getFeature(
//                        featurePath.substring(featurePath.indexOf("/") + 1));
//                if (selectedFeature != null) {
//                    return selectedFeature;
//                }
//            }
//        }
//
//        return selectedFeature;
//    }

    public <T extends Feature<?>> T getFeature(Class<T> type, String name) {
        List<T> selectedFeatures = getAll(type, name);
        if (selectedFeatures.isEmpty()) {
            return null;
        }
        return selectedFeatures.get(0);
    }
    
    public Feature<?> getFeature(String name) {
        List<Feature<?>> selectedFeatures = getAll(name);
        if (selectedFeatures.isEmpty()) {
            return null;
        }
        return selectedFeatures.get(0);
    }

    public <T extends Feature<?>> List<T> getAll(Class<T> type, String name) {
        List<T> selectedFeatures = new ArrayList<T>();
        for (Feature<?> feature : getAll(type)) {
            if (feature.getName().equals(name)) {
                selectedFeatures.add(type.cast(feature));
            }
        }
        // return selectedFeatures;
        // changed this to a immutable list, else wise it might cause confusion, because the returned list is not intended to be modified -- Philipp, 2012-11-16.
        return Collections.unmodifiableList(selectedFeatures);
    }

    /**
     * <p>
     * Provides all {@link Feature}s with the specified type from this {@link FeatureVector}.
     * </p>
     * 
     * @param type The type of the {@link Feature}s to retrieve.
     * @return A {@link List} of {@link Feature}s for the specified type or an empty List of no such {@link Feature}s
     *         exist, never <code>null</code>.
     */
    public <T extends Feature<?>> List<T> getAll(Class<T> type) {
        List<T> selectedFeatures = new ArrayList<T>();
        for (List<Feature<?>> list : features.values()) {
            for (Feature<?> feature : list) {
                if (type.isInstance(feature)) {
                    selectedFeatures.add(type.cast(feature));
                }
            }
        }
        return selectedFeatures;
    }
    
    public List<Feature<?>> getAll(String name) {
        List<Feature<?>> featureList = features.get(name);
        if (featureList != null) {
        return Collections.unmodifiableList(featureList);
        }
        return Collections.emptyList();
    }

//    /**
//     * <p>
//     * Provides a {@link Feature} from this {@link FeatureVector}.
//     * </p>
//     * 
//     * @param descriptor The {@link FeatureDescriptor} providing a unique identifier and the concrete type of the
//     *            requested {@link Feature}.
//     * @return The {@link Feature} for the specified {@link FeatureDescriptor} or <code>null</code> if no such
//     *         {@link Feature} exists.
//     * @deprecated Will be removed in the future.
//     */
//    @Deprecated
//    public <T extends Feature<?>> T get(FeatureDescriptor<T> descriptor) {
//        return getFeature(descriptor.getType(), descriptor.getIdentifier());
//    }

    @Override
    public String toString() {
        return features.values().toString();
    }

    /**
     * <p>
     * Converts this {@code FeatureVector} into an array of {@code Feature}s.
     * </p>
     * 
     * @return The vector as array.
     */
    public Feature<?>[] toArray() {
        // return features.values().toArray(new Feature[features.size()]);
        return getFlat().toArray(new Feature[0]);
    }

    /**
     * <p>
     * Get the dimension of this feature vector, i.e. how many {@link Feature}s the vector contains.
     * </p>
     * 
     * @return The size of this {@code FeatureVector}.
     */
    public int size() {
        return features.size();
    }

    /**
     * <p>
     * Removes all {@link Feature}s with the specified name from this {@link FeatureVector}.
     * </p>
     * 
     * @param name The name of the {@link Feature}s to remove.
     * @return <code>true</code> if the {@link Feature} was removed, <code>false</code> if there was no feature with the
     *         specified identifier to remove.
     */
    public boolean removeAll(String name) {
        return features.remove(name) != null;
    }

//    /**
//     * <p>
//     * Removes a {@link Feature} from this {@link FeatureVector}.
//     * </p>
//     * 
//     * @param descriptor The {@link FeatureDescriptor} providing a unique identifier and the concrete type of the
//     *            {@link Feature} to remove.
//     * @return <code>true</code> if the {@link Feature} was removed, <code>false</code> if there was no feature with the
//     *         specified identifier to remove.
//     */
//    @Deprecated
//    public boolean remove(FeatureDescriptor<?> featureDescriptor) {
//        return features.remove(featureDescriptor.getIdentifier()) != null;
//    }

    private List<Feature<?>> getFlat() {
        List<Feature<?>> result = new ArrayList<Feature<?>>();
        for (Entry<String, List<Feature<?>>> entry : features.entrySet()) {
            result.addAll(entry.getValue());
        }
        return result;
    }

//    public <T extends Feature<?>> List<T> getFeatures(Class<T> type, String path) {
//        if (path.startsWith("/")) {
//            path = path.substring(1);
//        }
//
//        String[] pathElements = path.split("/");
//        // System.out.println(Arrays.toString(pathElements));
//        List<T> collectedFeatures = new LinkedList<T>();
//
//        List<Feature<?>> selectedFeatures = features.get(pathElements[0]);
//        if (selectedFeatures != null) {
//
//            for (Feature<?> selectedFeature : selectedFeatures) {
//                if (selectedFeature instanceof AnnotationFeature) {
//                    collectedFeatures.addAll(((AnnotationFeature)selectedFeature).getFeatures(type,
//                            path.substring(path.indexOf("/") + 1)));
//                } else {
//                    collectedFeatures.add(type.cast(selectedFeature));
//                }
//            }
//        }
//
//        return collectedFeatures;
//    }

    public List<? extends Feature<?>> getFeatures(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        String[] pathElements = path.split("/");
        // System.out.println(Arrays.toString(pathElements));
        List<Feature<?>> collectedFeatures = new LinkedList<Feature<?>>();

        List<Feature<?>> selectedFeatures = features.get(pathElements[0]);
        if (selectedFeatures != null) {

            for (Feature<?> selectedFeature : selectedFeatures) {
//                if (selectedFeature instanceof AnnotationFeature) {
//                    collectedFeatures.addAll(((AnnotationFeature)selectedFeature).getFeatures(path.substring(path
//                            .indexOf("/") + 1)));
//                } else {
                    collectedFeatures.add(selectedFeature);
//                }
            }
        }

        return collectedFeatures;
    }

//    public List<? extends Feature<?>> getFeatures() {
//        List<Feature<?>> ret = new ArrayList<Feature<?>>();
//        for (Entry<String, List<Feature<?>>> entry : features.entrySet()) {
//            ret.addAll(entry.getValue());
//        }
//        return ret;
//    }

//    public <T> Set<? extends Feature<T>> getFeatureBag(Class<? extends Feature<T>> featureClass, String featurePath) {
//        return new HashSet<Feature<T>>(getFeatures(featureClass, featurePath));
//    }

    @Override
    public Iterator<Feature<?>> iterator() {
        return getFlat().iterator();
        // return new FeatureIterator(this);
    }


}

//class FeatureIterator implements Iterator<Feature<?>> {
//
//    private final FeatureVector vector;
//    private final Stack<Iterator<? extends Feature<?>>> iteratorStack;
//
//    /**
//     * <p>
//     * 
//     * </p>
//     * 
//     */
//    public FeatureIterator(FeatureVector vector) {
//        super();
//
//        this.vector = vector;
//        iteratorStack = new Stack<Iterator<? extends Feature<?>>>();
//        iteratorStack.push(vector.getFeatures().iterator());
//    }
//
//    @Override
//    public boolean hasNext() {
//        return !iteratorStack.isEmpty();
//    }
//
//    @Override
//    public Feature<?> next() {
//        try {
//            Iterator<? extends Feature<?>> currentIterator = iteratorStack.pop();
//            Feature<?> feature = currentIterator.next();
//
//            if (feature instanceof AnnotationFeature<?>) {
//                AnnotationFeature<?> annotationFeature = (AnnotationFeature)feature;
//                for (Annotation<?> annotation : annotationFeature.getAnnotations()) {
//                    iteratorStack.push(annotation.getFeatureVector().iterator());
//                }
//            }
//
//            if (currentIterator.hasNext()) {
//                iteratorStack.push(currentIterator);
//            }
//
//            return feature;
//        } catch (EmptyStackException e) {
//            throw new IllegalStateException("Iterator has no more elements");
//        }
//    }
//
//    @Override
//    public void remove() {
//        throw new UnsupportedOperationException("This operation is not supported by the FeatureIterator");
//    }
//
//}
