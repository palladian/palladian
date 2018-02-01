package ws.palladian.classification.text.vector;

import java.util.Set;

import de.bwaldvogel.liblinear.Parameter;
import ws.palladian.classification.liblinear.LibLinearClassifier;
import ws.palladian.classification.liblinear.LibLinearLearner;
import ws.palladian.classification.liblinear.LibLinearModel;
import ws.palladian.classification.utils.NoNormalizer;
import ws.palladian.core.AbstractLearner;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Learner;
import ws.palladian.core.Model;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.extraction.text.vector.TextVectorizer;
import ws.palladian.helper.functional.Filters;

public class TextVectorClassifier<M extends Model> extends AbstractLearner<TextVectorClassifier.TextVectorModel<M>> implements Classifier<TextVectorClassifier.TextVectorModel<M>> {
	
	public static class TextVectorModel<M extends Model> implements Model {
		private static final long serialVersionUID = 1L;
		private final M model;

		public TextVectorModel(M model) {
			this.model = model;
		}

		@Override
		public Set<String> getCategories() {
			return model.getCategories();
		}
		
	}

	private final TextVectorizer vectorizer;
	private final Learner<M> learner;
	private final Classifier<M> classifier;

	public static TextVectorClassifier<LibLinearModel> libLinear(TextVectorizer vectorizer) {
		return new TextVectorClassifier<>(vectorizer, new LibLinearLearner(new NoNormalizer()),
				new LibLinearClassifier());
	}

	public static TextVectorClassifier<LibLinearModel> libLinear(TextVectorizer vectorizer, Parameter parameter) {
		return new TextVectorClassifier<>(vectorizer, new LibLinearLearner(parameter, 1, new NoNormalizer()),
				new LibLinearClassifier());
	}

	public TextVectorClassifier(TextVectorizer vectorizer, Learner<M> learner, Classifier<M> classifier) {
		this.vectorizer = vectorizer;
		this.learner = learner;
		this.classifier = classifier;
	}

	@Override
	public TextVectorModel<M> train(Dataset dataset) {
		Dataset transformedDataset = dataset.transform(vectorizer).filterFeatures(Filters.not(Filters.equal("text")));
		return new TextVectorModel<>(learner.train(transformedDataset));
	}

	@Override
	public CategoryEntries classify(FeatureVector featureVector, TextVectorModel<M> model) {
		FeatureVector vectorizedVector = vectorizer.compute(featureVector);
		return classifier.classify(vectorizedVector, model.model);
	}
	
	@Override
	public String toString() {
		return String.format("%s [learner=%s, vectorizer=%s]", this.getClass().getSimpleName(), learner, vectorizer);
	}

}
