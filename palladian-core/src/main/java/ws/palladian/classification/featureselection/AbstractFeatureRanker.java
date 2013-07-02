/**
 * Created on 30.06.2013 12:28:52
 */
package ws.palladian.classification.featureselection;

import java.util.Collection;
import java.util.Set;

import ws.palladian.classification.Instance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.ListFeature;

/**
 * <p>
 * Abstract base class for all {@link FeatureRanker}s. Implements common base functionallity.
 * </p>
 *
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.2
 */
public abstract class AbstractFeatureRanker implements FeatureRanker {
    /**
     * <p>
     * Converts all features of the same feature type within a {@link FeatureVector} to a {@link Set}. For dense
     * {@link Feature}s the set consists of only one element. For sparse {@link Feature}s it contains all instances from
     * the {@link FeatureVector} exactly once. This means, for example, if the term 'the' occurs two times in a text,
     * this method will only include it once in the returned {@link Set}.
     * </p>
     * 
     * @param featureVector The {@link FeatureVector} containing the dense {@link Feature} or sparse {@link Feature}s
     * @param type The type of the {@link Feature} or {@link Feature}s to include in the {@link Set}.
     * @param name The {@link Feature} name used to find the desired {@link Feature} or {@link Feature}s in the {@link FeatureVector}.
     * @return the {@link Feature} or {@link Feature}s as a {@link Set}.
     */
    protected static <T extends Feature<?>> Set<T> convertToSet(final FeatureVector featureVector, final Class<T> type,
            final String name) {
        Feature<?> feature = featureVector.get(name);
        Set<T> ret = CollectionHelper.newHashSet();

        if (feature instanceof ListFeature<?>) {
            ListFeature<T> listFeature = (ListFeature<T>)feature;
            for (T element : listFeature) {
                ret.add(element);
            }
        } else {
            ret.add(type.cast(feature));
        }

        return ret;
    }

}
