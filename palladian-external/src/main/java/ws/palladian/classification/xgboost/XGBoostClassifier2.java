package ws.palladian.classification.xgboost;
//package ws.palladian.kaggle.redhat.classifier.xgboost;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Set;
//
//import biz.k11i.xgboost.Predictor;
//import biz.k11i.xgboost.util.FVec;
//import ml.dmlc.xgboost4j.java.XGBoostError;
//import ws.palladian.core.CategoryEntries;
//import ws.palladian.core.CategoryEntriesBuilder;
//import ws.palladian.core.Classifier;
//import ws.palladian.core.FeatureVector;
//import ws.palladian.core.Model;
//import ws.palladian.core.value.NullValue;
//import ws.palladian.core.value.NumericValue;
//import ws.palladian.core.value.Value;
//import ws.palladian.helper.collection.Vector.VectorEntry;
//import ws.palladian.helper.io.FileHelper;
//
///**
// * Predictor for XGBoost2 using a pure Java-based implementation: <a href=
// * "https://github.com/komiya-atsushi/xgboost-predictor-java">xgboost-predictor-java</a>.
// * It claims that it is 6,000 to 10,000 times faster than xgboost4j itself.
// * 
// * To use this predictor, train the model using the orginary
// * {@link XGBoostLearner}. Then convert the model using
// * {@link #convertModel(XGBoostModel)} and supply it to this classifier.
// * 
// * @author pk
// */
//public class XGBoostClassifier2 implements Classifier<XGBoostClassifier2.XGBoostClassifier2Model> {
//
//	public static XGBoostClassifier2Model convertModel(XGBoostModel model) {
//		Objects.requireNonNull(model, "model was null");
//		File tempModelPath = FileHelper.getTempFile();
//		try {
//			model.getBooster().saveModel(tempModelPath.getAbsolutePath());
//			Predictor predictor = new Predictor(new FileInputStream(tempModelPath));
//			return new XGBoostClassifier2Model(predictor, model);
//		} catch (XGBoostError e) {
//			throw new IllegalStateException(e);
//		} catch (IOException e) {
//			throw new IllegalStateException(e);
//		}
//	}
//
//	public static class XGBoostClassifier2Model implements Model {
//		private static final long serialVersionUID = 1L;
//		private final Predictor predictor;
//		private final XGBoostModel wrapped;
//
//		private XGBoostClassifier2Model(Predictor predictor, XGBoostModel model) {
//			this.predictor = predictor;
//			this.wrapped = model;
//		}
//
//		@Override
//		public Set<String> getCategories() {
//			return wrapped.getCategories();
//		}
//
//		Predictor getPredictor() {
//			return predictor;
//		}
//
//		Map<String, Integer> getFeatureIndices() {
//			return wrapped.getFeatureIndices();
//		}
//
//		String getLabel(int index) {
//			return wrapped.getLabel(index);
//		}
//	}
//
//	@Override
//	public CategoryEntries classify(FeatureVector featureVector, XGBoostClassifier2Model model) {
//
//		Map<String, Integer> featureIndices = model.getFeatureIndices();
//		Map<Integer, Float> sparseVectorMap = new HashMap<>();
//
//		for (VectorEntry<String, Value> vectorEntry : featureVector) {
//			Value value = vectorEntry.value();
//			Integer featureIndex = featureIndices.get(vectorEntry.key());
//			if (featureIndex != null && value != NullValue.NULL && value instanceof NumericValue) {
//				float floatValue = ((NumericValue) value).getFloat();
//				if (isZero(floatValue)) {
//					continue;
//				}
//				sparseVectorMap.put(featureIndex, floatValue);
//			}
//		}
//
//		FVec fVecSparse = FVec.Transformer.fromMap(sparseVectorMap);
//		double[] prediction = model.getPredictor().predict(fVecSparse);
//
//		CategoryEntriesBuilder builder = new CategoryEntriesBuilder();
//		builder.set(model.getLabel(0), 1 - prediction[0]);
//		builder.set(model.getLabel(1), prediction[0]);
//		return builder.create();
//
//	}
//
//	private static boolean isZero(float floatValue) {
//		return Math.abs(floatValue) < 2 * Float.MIN_VALUE;
//	}
//
//}
