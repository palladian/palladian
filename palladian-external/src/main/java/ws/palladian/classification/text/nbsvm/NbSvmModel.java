package ws.palladian.classification.text.nbsvm;

import java.util.Map;
import java.util.Set;

import ws.palladian.classification.liblinear.LibLinearModel;
import ws.palladian.core.Model;

public class NbSvmModel implements Model {

	private static final long serialVersionUID = 1L;

	final LibLinearModel libLinearModel;
	final Map<String, Integer> dictionary;
	final float[] r;

	NbSvmModel(LibLinearModel libLinearModel, Map<String, Integer> dictionary, float[] r) {
		this.libLinearModel = libLinearModel;
		this.dictionary = dictionary;
		this.r = r;
	}

	@Override
	public Set<String> getCategories() {
		return libLinearModel.getCategories();
	}

}