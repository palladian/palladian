/**
 * Created on 30.06.2013 12:28:52
 */
package ws.palladian.classification.featureselection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.discretization.Binner;
import ws.palladian.core.Instance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
/**
 * <p>
 * Abstract base class for all {@link FeatureRanker}s. Implements common base functionality.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.2
 */
public abstract class AbstractFeatureRanker implements FeatureRanker {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureRanker.class);

//    /**
//     * <p>
//     * Converts all features within a {@link FeatureVector} to a {@link Set}. For dense {@link Feature}s the set is
//     * extended by only one element. For sparse {@link Feature}s it contains all instances from the
//     * {@link FeatureVector} exactly once. This means, for example, if the term 'the' occurs two times in a text, this
//     * method will only include it once in the returned {@link Set}.
//     * </p>
//     * <p>
//     * The method is used for deduplication of features.
//     * </p>
//     * 
//     * @param featureVector The {@link FeatureVector} containing the dense {@link Feature} or sparse {@link Feature}s
//     * @return the {@link Feature} or {@link Feature}s as a {@link Set}.
//     */
//    protected Set<Feature<?>> convertToSet(final FeatureVector featureVector,
//            final Collection<? extends Trainable> dataset) {
//        Set<Feature<?>> ret = CollectionHelper.newHashSet();
//
//        for (final Feature<?> feature : featureVector) {
            // if (feature instanceof ListFeature<?>) {
            // ListFeature<Feature<?>> listFeature = (ListFeature<Feature<?>>)feature;
            // for (final Feature<?> element : listFeature) {
            // if (element instanceof NumericFeature) {
            // Binner binner = binnerCache.get(element.getName());
            // if (binner == null) {
            // binner = discretize(element.getName(), dataset, new Comparator<Trainable>() {
            //
            // @Override
            // public int compare(Trainable i1, Trainable i2) {
            // ListFeature<NumericFeature> i1ListFeature = i1.getFeatureVector().get(
            // ListFeature.class, feature.getName());
            // ListFeature<NumericFeature> i2ListFeature = i2.getFeatureVector().get(
            // ListFeature.class, feature.getName());
            //
            // NumericFeature i1Feature = i1ListFeature.getFeatureWithName(element.getName());
            // NumericFeature i2Feature = i2ListFeature.getFeatureWithName(element.getName());
            // Double i1FeatureValue = i1Feature == null ? Double.MIN_VALUE : i1Feature.getValue();
            // Double i2FeatureValue = i2Feature == null ? Double.MIN_VALUE : i2Feature.getValue();
            // return i1FeatureValue.compareTo(i2FeatureValue);
            // }
            // });
            // binnerCache.put(element.getName(), binner);
            // }
            // ret.add(binner.bin((NumericFeature)element));
            // } else {
            // ret.add(element);
            // }
            // }
            // } else
    //
    // }
    //
    // return ret;
    // }








}
