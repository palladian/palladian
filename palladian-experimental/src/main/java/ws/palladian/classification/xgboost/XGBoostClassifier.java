package ws.palladian.classification.xgboost;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;

public class XGBoostClassifier implements Classifier<XGBoostModel> {

    @Override
    public CategoryEntries classify(FeatureVector featureVector, XGBoostModel model) {

        TFloatArrayList data = new TFloatArrayList();
        TIntArrayList index = new TIntArrayList();

        XGBoostLearner.makeRow(model.getFeatureIndices(), data, index, featureVector);

        long[] headers = {0, data.size()};

        try {

            DMatrix matrix = new DMatrix(headers, index.toArray(), data.toArray(), DMatrix.SparseType.CSR);
            float[][] predictionMatrix = model.getBooster().predict(matrix);
            float[] prediction = predictionMatrix[0];

            CategoryEntriesBuilder builder = new CategoryEntriesBuilder();

            if (prediction.length == 1) {
                builder.set(model.getLabel(0), 1 - prediction[0]);
                builder.set(model.getLabel(1), prediction[0]);
            } else {
                for (int i = 0; i < prediction.length; i++) {
                    builder.set(model.getLabel(i), prediction[i]);
                }
            }

            return builder.create();

        } catch (XGBoostError e) {
            throw new IllegalStateException(e);
        }
    }

}
