package ws.palladian.classification.xgboost;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import ml.dmlc.xgboost4j.java.*;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.core.AbstractLearner;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.statistics.DatasetStatistics;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Vector.VectorEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Learner for <a href="http://xgboost.readthedocs.io/en/latest/">XGBoost</a>:
 * "Scalable and Flexible Gradient Boosting".
 *
 * @author Philipp Katz
 */
public class XGBoostLearner extends AbstractLearner<XGBoostModel> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(XGBoostLearner.class);

    private final Map<String, Object> params;

    private final int rounds;

    private final IEvaluation evaluation;

    /**
     * Create a new {@link XGBoostLearner} instance with the supplied parameters and
     * a custom evaluation function.
     *
     * @param params     The parameters, see <a href=
     *                   "https://github.com/dmlc/xgboost/blob/master/doc/parameter.md">
     *                   XGBoost Parameters</a> for a description of all available
     *                   parameters.
     * @param rounds     The number of rounds for boosting.
     * @param evaluation The evaluation function.
     */
    public XGBoostLearner(Map<String, Object> params, int rounds, IEvaluation evaluation) {
        Validate.notNull(params, "params must not be null");
        Validate.isTrue(rounds > 0, "round must be at least 1");
        this.params = new HashMap<>(params);
        this.rounds = rounds;
        this.evaluation = evaluation;
    }

    /**
     * Create a new {@link XGBoostLearner} instance with the supplied parameters.
     *
     * @param params The parameters, see <a href=
     *               "https://github.com/dmlc/xgboost/blob/master/doc/parameter.md">
     *               XGBoost Parameters</a> for a description of all available
     *               parameters.
     * @param rounds The number of rounds for boosting.
     */
    public XGBoostLearner(Map<String, Object> params, int rounds) {
        this(params, rounds, null);
    }

    /**
     * Create a new {@link XGBoostModel} with settings which I took from some Kaggle
     * posts.
     */
    public XGBoostLearner() {
        Map<String, Object> params = new HashMap<>();
        params.put("objective", "binary:logistic");
        params.put("early_stopping_rounds", "50");
        params.put("eval_metric", "auc");
        params.put("booster", "gbtree");
        params.put("eta", 0.02);
        params.put("subsample", 0.7);
        params.put("colsample_bytree", 0.7);
        params.put("min_child_weight", 0);
        params.put("min_child_weight", 0);
        params.put("max_depth", 10);
        params.put("silent", 0);
        this.params = params;
        this.rounds = 100;
        this.evaluation = null;
    }

    @Override
    public XGBoostModel train(Dataset training, Dataset validation) {

        DatasetStatistics statistics = new DatasetStatistics(training);
        List<String> labelIndices = new ArrayList<>(statistics.getCategoryStatistics().getValues());
        List<String> featureIndices = new ArrayList<>(training.getFeatureInformation().getFeatureNamesOfType(NumericValue.class));
        if (featureIndices.isEmpty()) {
            throw new IllegalArgumentException("The training data contains no numeric features.");
        }

        Map<String, Object> paramsCopy = new HashMap<>(params);
        if (labelIndices.size() > 2) {
            LOGGER.debug("num_class = {}", labelIndices.size());
            paramsCopy.put("num_class", labelIndices.size());
        }

        // put indices into map
        Map<String, Integer> labelIndicesMap = CollectionHelper.createIndexMap(labelIndices);
        Map<String, Integer> featureIndicesMap = CollectionHelper.createIndexMap(featureIndices);

        try {

            DMatrix trainingMatrix = makeMatrix(training, labelIndicesMap, featureIndicesMap);

            Map<String, DMatrix> watches = new HashMap<>();
            watches.put("training", trainingMatrix);

            if (validation != null) {
                LOGGER.debug("Using dedicated validation set");
                DMatrix validationMatrix = makeMatrix(validation, labelIndicesMap, featureIndicesMap);
                watches.put("validation", validationMatrix);
            }

            Booster booster = XGBoost.train(trainingMatrix, paramsCopy, rounds, watches, null, evaluation);
            return new XGBoostModel(booster, labelIndices, featureIndicesMap);

        } catch (XGBoostError e) {
            throw new IllegalStateException(e);
        }

    }

    @Override
    public XGBoostModel train(Dataset dataset) {
        return train(dataset, null);
    }

    private static DMatrix makeMatrix(Dataset dataset, Map<String, Integer> labelIndices, Map<String, Integer> featureIndices) throws XGBoostError {
        TFloatArrayList labels = new TFloatArrayList();
        TFloatArrayList data = new TFloatArrayList();
        TLongArrayList headers = new TLongArrayList();
        TIntArrayList index = new TIntArrayList();

        long rowheader = 0;
        headers.add(rowheader);

        for (Instance instance : dataset) {
            Integer labelIndex = labelIndices.get(instance.getCategory());
            if (labelIndex == null) {
                continue;
            }
            labels.add((float) labelIndex);
            rowheader += makeRow(featureIndices, data, index, instance.getVector());
            headers.add(rowheader);
        }

        DMatrix matrix = new DMatrix(headers.toArray(), index.toArray(), data.toArray(), DMatrix.SparseType.CSR);
        matrix.setLabel(labels.toArray());
        return matrix;
    }

    static long makeRow(Map<String, Integer> featureIndices, TFloatArrayList data, TIntArrayList index, FeatureVector vector) {
        long rowheader = 0;
        for (VectorEntry<String, Value> vectorEntry : vector) {
            Value value = vectorEntry.value();
            Integer featureIndex = featureIndices.get(vectorEntry.key());
            if (featureIndex != null && value != NullValue.NULL && value instanceof NumericValue) {
                float floatValue = ((NumericValue) value).getFloat();
                if (Math.abs(floatValue) < 2 * Float.MIN_VALUE) {
                    continue;
                }
                data.add(floatValue);
                index.add(featureIndex);
                rowheader++;
            }
        }
        return rowheader;
    }

}
