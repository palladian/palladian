package ws.palladian.classification;

import java.util.Map;
import java.util.SortedMap;

import weka.core.Attribute;
import weka.core.SparseInstance;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.NominalValue;
import ws.palladian.core.NumericValue;
import ws.palladian.core.Value;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Vector.VectorEntry;

/**
 * <p>
 * Classifier wrapper for Weka.
 * </p>
 * 
 * @see <a href="http://www.cs.waikato.ac.nz/ml/weka/">Weka 3</a>
 * @author Philipp Katz
 * @author Klemens Muthmann
 * @version 3.1
 * @since 0.1.7
 */
public final class WekaClassifier implements Classifier<WekaModel> {

    @Override
    public CategoryEntries classify(FeatureVector featureVector, WekaModel model) {
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();

        SortedMap<Integer, Double> indices = CollectionHelper.newTreeMap();
        Map<String, Attribute> schema = model.getSchema();
        for (VectorEntry<String, Value> feature : featureVector) {
            Attribute attribute = schema.get(feature.key());
            int attributeIndex = attribute.index();
            Value featureValue = feature.value();
            if (featureValue instanceof NominalValue) {
                NominalValue nominalValue = (NominalValue)featureValue;
                int idx = attribute.indexOfValue(nominalValue.getString());
                if (idx >= 0) {
                    indices.put(attributeIndex, (double)idx);
                }
            } else if (featureValue instanceof NumericValue) {
                NumericValue numericValue = (NumericValue)featureValue;
                indices.put(attributeIndex, numericValue.getDouble());
            }
        }

        double[] valuesArray = new double[indices.size()];
        int[] indicesArray = new int[indices.size()];
        int index = 0;
        for (Map.Entry<Integer, Double> entry : indices.entrySet()) {
            valuesArray[index] = entry.getValue();
            indicesArray[index] = entry.getKey();
            index++;
        }
        SparseInstance instance = new SparseInstance(1.0, valuesArray, indicesArray, indices.size());
        instance.setDataset(model.getDataset());

        try {
            double[] distribution = model.getClassifier().distributionForInstance(instance);
            for (int i = 0; i < distribution.length; i++) {
                String className = model.getDataset().classAttribute().value(i);
                builder.set(className, distribution[i]);
            }
        } catch (Exception e) {
            throw new IllegalStateException("An exception occurred during classification.", e);
        }
        return builder.create();
    }

}
