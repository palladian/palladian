package ws.palladian.classification.text.nbsvm;

import static ws.palladian.helper.functional.Predicates.equal;
import static ws.palladian.helper.functional.Predicates.not;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.SolverType;
import org.apache.commons.math3.util.FastMath;
import ws.palladian.classification.liblinear.LibLinearLearner;
import ws.palladian.classification.liblinear.LibLinearModel;
import ws.palladian.classification.utils.NoNormalizer;
import ws.palladian.core.AbstractLearner;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDatasetFeatureVectorTransformer;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.extraction.text.vector.TextVectorizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Vector.VectorEntry;

/**
 * Learner for the NBSVM (SVM with NB features) text classifier described in
 * "<a href=
 * "https://nlp.stanford.edu/pubs/sidaw12_simple_sentiment.pdf">Baselines and
 * Bigrams: Simple, Good Sentiment and Topic Classification</a>"; Sida Wang and
 * Christopher D. Manning.
 *
 * Attention: This currently assumes a binary classification problem. The class
 * names must exactly match <code>0</code> and <code>1</code>.
 *
 * @author Philipp Katz
 */
public class NbSvmLearner extends AbstractLearner<NbSvmModel> {

	// TODO names currently hard coded; make flexible!
	private static final String TRUE_CATEGORY = "1";
	private static final String FALSE_CATEGORY = "0";

	private static final float ALPHA = 1;

	private final TextVectorizer vectorizer;
	private final LibLinearLearner learner;

	public NbSvmLearner(TextVectorizer vectorizer) {
		this(vectorizer, new Parameter(SolverType.L2R_LR, 1.0, 0.01));
	}

	public NbSvmLearner(TextVectorizer vectorizer, Parameter parameter) {
		this.vectorizer = vectorizer;
		learner = new LibLinearLearner(parameter, 1, new NoNormalizer());
	}

	@Override
	public NbSvmModel train(Dataset dataset) {

		Dataset vectorizedDataset = dataset.transform(vectorizer).filterFeatures(not(equal("text")));

		Set<String> allTokens = new HashSet<>();
		for (Instance instance : vectorizedDataset) {
			for (VectorEntry<String, Value> entry : instance.getVector()) {
				allTokens.add(entry.key());
			}
		}
		final Map<String, Integer> dictionary = CollectionHelper.createIndexMap(new ArrayList<>(allTokens));

		int nTokens = allTokens.size();
		float[] p = new float[nTokens], q = new float[nTokens];
		Arrays.fill(p, ALPHA);
		Arrays.fill(q, ALPHA);
		for (Instance instance : vectorizedDataset) {
			for (VectorEntry<String, Value> entry : instance.getVector()) {
				String token = entry.key();
				float value = ((NumericValue) entry.value()).getFloat();
				if (instance.getCategory().equals(TRUE_CATEGORY)) {
					p[dictionary.get(token)] += value;
				} else if (instance.getCategory().equals(FALSE_CATEGORY)) {
					q[dictionary.get(token)] += value;
				} else {
					throw new IllegalStateException(String.format("Instance must currently be of category '%s' or '%s'",
							FALSE_CATEGORY, TRUE_CATEGORY));
				}
			}
		}

		float p_sum = 0, q_sum = 0;
		for (int i = 0; i < nTokens; i++) {
			p_sum += p[i];
			q_sum += q[i];
		}

		final float[] r = new float[nTokens];
		for (int i = 0; i < nTokens; i++) {
			r[i] = (float) FastMath.log(p[i] / p_sum / (q[i] / q_sum));
		}

		Dataset transformedDataset = vectorizedDataset.transform(new AbstractDatasetFeatureVectorTransformer() {
			@Override
			public FeatureVector apply(FeatureVector featureVector) {
				return transform(dictionary, r, featureVector);
			}
		});

		LibLinearModel libLinearModel = learner.train(transformedDataset);
		return new NbSvmModel(libLinearModel, dictionary, r);
	}

	static FeatureVector transform(Map<String, Integer> dictionary, float[] r, FeatureVector featureVector) {
		InstanceBuilder builder = new InstanceBuilder();
		for (VectorEntry<String, Value> entry : featureVector) {
			String token = entry.key();
			float value = ((NumericValue) entry.value()).getFloat();
			Integer index = dictionary.get(token);
			if (index != null) {
				builder.set(entry.key(), value * r[index]);
			}
		}
		return builder.create();
	}

	@Override
	public String toString() {
		return String.format("%s [learner=%s, vectorizer=%s]", this.getClass().getSimpleName(), learner, vectorizer);
	}

}
