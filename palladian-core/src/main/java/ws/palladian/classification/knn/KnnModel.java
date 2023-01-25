package ws.palladian.classification.knn;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import ws.palladian.classification.utils.Normalization;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.Model;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.statistics.DatasetStatistics;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.MapMatrix;
import ws.palladian.helper.collection.Matrix;
import ws.palladian.helper.collection.Vector;

import java.io.Serial;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * The model used by KNN classification algorithms. Like the {@link KnnClassifier}.
 * </p>
 *
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class KnnModel implements Model {
    /** Used for serializing objects of this class. Should only change if the attribute set of the class changes. */
    @Serial
    private static final long serialVersionUID = 8995411185069694896L;

    /** The labels and their weight. */
    private final Object2FloatMap<String> numericFieldsAndWeights;
    private final Object2FloatMap<String> textualFieldsAndWeights;

    /**
     * ___________________|_feature1_|_feature2_...
     * training sample 1__|__________|_2.3______...
     * training sample 2__|__text____|__________...
     * ....
     */
    private MapMatrix<String, Value> trainingMatrix;

    /** The trained category names. */
    private final Set<String> categories;

    /**
     * An object carrying the information to normalize {@link FeatureVector}s based on the normalized trainingExamples.
     */
    private final Normalization normalization;

    /**
     * <p>
     * Creates a new unnormalized {@code KnnModel} based on a {@code List} of {@link Instance}s.
     * </p>
     *
     * @param trainingInstances The {@link Instance}s this model is based on.
     */
    KnnModel(Dataset trainingInstances, Normalization normalization, Object2FloatMap<String> numericFieldsAndWeights, Object2FloatMap<String> textualFieldsAndWeights) {
        DatasetStatistics statistics = new DatasetStatistics(trainingInstances);
        this.numericFieldsAndWeights = numericFieldsAndWeights;
        this.textualFieldsAndWeights = textualFieldsAndWeights;
        this.categories = new HashSet<>(statistics.getCategoryStatistics().getValues());
        initTrainingMatrix(trainingInstances, normalization);
        this.normalization = normalization;
    }

    private void initTrainingMatrix(Iterable<? extends Instance> instances, Normalization normalization) {
        trainingMatrix = new MapMatrix<>();

        int sampleNumber = 0;
        for (Instance instance : instances) {
            FeatureVector normalizedFeatureVector = normalization.normalize(instance.getVector());

            String y = String.valueOf(sampleNumber++);

            for (String numericField : numericFieldsAndWeights.keySet()) {
                Value value = normalizedFeatureVector.get(numericField);
                trainingMatrix.set(numericField, y, value);
            }

            for (String textualField : textualFieldsAndWeights.keySet()) {
                Value value = normalizedFeatureVector.get(textualField);
                trainingMatrix.set(textualField, y, value);
            }

            trainingMatrix.set("category", y, ImmutableStringValue.valueOf(instance.getCategory()));
        }
    }

    public FeatureVector getFeatureVector(String y) {
        Matrix.MatrixVector<String, Value> row = trainingMatrix.getRow(y);
        InstanceBuilder instanceBuilder = new InstanceBuilder();
        for (Vector.VectorEntry<String, Value> entry : row) {
            instanceBuilder.set(entry.key(), entry.value());
        }
        return instanceBuilder.create();
    }

    public Set<String> getRowKeys() {
        return trainingMatrix.getRowKeys();
    }

    public Object2FloatMap<String> getNumericFieldsAndWeights() {
        return numericFieldsAndWeights;
    }

    public Object2FloatMap<String> getTextualFieldsAndWeights() {
        return textualFieldsAndWeights;
    }

    @Override
    public Set<String> getCategories() {
        return Collections.unmodifiableSet(categories);
    }

    @Override
    public String toString() {
        return "KnnModel [" + "# trainingInstances=" + trainingMatrix.getColumnKeys().size() + " normalization=" + normalization + "]";
    }
}