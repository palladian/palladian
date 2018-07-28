package ws.palladian.kaggle.restaurants.classifier.nn;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.params.DefaultParamInitializer;

import ws.palladian.classification.utils.NoNormalizer;
import ws.palladian.classification.utils.Normalization;
import ws.palladian.core.Model;

public class MultiLayerNetworkModel implements Model {

	private static final long serialVersionUID = 1L;

	private final MultiLayerNetwork model;

	private final List<String> categoryNames;

	private final List<String> featureNames;
	
	private final Normalization normalization;

	MultiLayerNetworkModel(MultiLayerNetwork model, List<String> categoryNames, List<String> featureNames) {
		this(model, categoryNames, featureNames, NoNormalizer.NO_NORMALIZATION);
	}
	
	MultiLayerNetworkModel(MultiLayerNetwork model, List<String> categoryNames, List<String> featureNames, Normalization normalization) {
		this.model = model;
		this.categoryNames = categoryNames;
		this.featureNames = featureNames;
		this.normalization = normalization;
	}

	MultiLayerNetwork getModel() {
		return model;
	}

	List<String> getCategoryNames() {
		return Collections.unmodifiableList(categoryNames);
	}

	List<String> getFeatureNames() {
		return Collections.unmodifiableList(featureNames);
	}

	@Override
	public Set<String> getCategories() {
		return new HashSet<>(categoryNames);
	}
	
	Normalization getNormalization() {
		return normalization;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		for (Layer layer : model.getLayers()) {
			stringBuilder.append(layer.getParam(DefaultParamInitializer.WEIGHT_KEY));
			stringBuilder.append('\n');
		}
		return stringBuilder.toString();
	}

}
