//package ws.palladian.kaggle.restaurants.classifier.nn;
//
//import static org.deeplearning4j.nn.api.Layer.TrainingMode.TEST;
//import static ws.palladian.kaggle.restaurants.classifier.nn.DataSetIteratorAdapter.createFvArray;
//
//import org.nd4j.linalg.api.ndarray.INDArray;
//
//import ws.palladian.core.AbstractClassifier;
//import ws.palladian.core.CategoryEntries;
//import ws.palladian.core.CategoryEntriesBuilder;
//import ws.palladian.core.FeatureVector;
//
//public class MultiLayerNetworkClassifier extends AbstractClassifier<MultiLayerNetworkModel> {
//
//	@Override
//	public CategoryEntries classify(FeatureVector featureVector, MultiLayerNetworkModel model) {
//		
//		FeatureVector normalizedFeatureVector = model.getNormalization().normalize(featureVector);
//		
//		INDArray result = model.getModel().output(createFvArray(normalizedFeatureVector, model.getFeatureNames()), TEST);
//		CategoryEntriesBuilder builder = new CategoryEntriesBuilder();
//		for (int i = 0; i < model.getCategories().size(); i++) {
//			String categoryName = model.getCategoryNames().get(i);
//			builder.set(categoryName, result.getDouble(i));
//		}
//		return builder.create();
//	}
//
//}
