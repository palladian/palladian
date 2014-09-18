package ws.palladian.extraction.pos;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.Annotation;
import ws.palladian.core.TextTokenizer;
import ws.palladian.extraction.token.TwokenizeTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import cmu.arktweetnlp.impl.Model;
import cmu.arktweetnlp.impl.ModelSentence;
import cmu.arktweetnlp.impl.Sentence;
import cmu.arktweetnlp.impl.features.FeatureExtractor;

/**
 * <p>
 * A POS tagger for Tweets, wrapping <a href="http://code.google.com/p/ark-tweet-nlp/">ark-tweet-nlp</a>.
 * <b>Important:</b> The following files from the original package need to be present in a directory <code>lib</code>:
 * <ul>
 * <li><code>lib/names</code></li>
 * <li><code>lib/embeddings.txt</code></li>
 * <li><code>lib/tagdict.txt</code></li>
 * <li><code>lib/tweetpos.model</code></li>
 * </ul>
 * </p>
 * 
 * @author Philipp Katz
 */
public class TweetNlpPosTagger extends AbstractPosTagger {

    private static final String TAGGER_NAME = "ark-tweet-nlp";

    private static final TwokenizeTokenizer TOKENIZER = new TwokenizeTokenizer();

    /** The default model loaded from the resources. */
    private static final String DEFAULT_MODEL = "/cmu/arktweetnlp/model.20120919";

    /** Instance {@link TweetNlpPosTagger} with the included default model. */
    public static final TweetNlpPosTagger DEFAULT = new TweetNlpPosTagger(DEFAULT_MODEL);

    private final Model model;

    private final FeatureExtractor featureExtractor;

    /**
     * Create a new {@link TweetNlpPosTagger} with a specified model file.
     * 
     * @param model The model file, not <code>null</code>.
     * @see TweetNlpPosTagger#DEFAULT
     */
    public TweetNlpPosTagger(File model) {
        this(model.getPath());
    }

    private TweetNlpPosTagger(String modelPath) {
        Validate.notNull(modelPath, "modelPath must not be null");
        try {
            this.model = Model.loadModelFromText(modelPath);
            this.featureExtractor = new FeatureExtractor(this.model, false);
        } catch (IOException e) {
            throw new IllegalStateException("IOException when trying to load model from \"" + modelPath + "\".", e);
        }
    }

    @Override
    public String getName() {
        return TAGGER_NAME;
    }

    @Override
    protected List<String> getTags(List<String> tokens) {
        Sentence sentence = new Sentence();
        sentence.tokens = tokens;
        ModelSentence ms = new ModelSentence(sentence.T());
        featureExtractor.computeFeatures(sentence, ms);
        model.greedyDecode(ms, false);
        List<String> tags = CollectionHelper.newArrayList();
        for (int t = 0; t < sentence.T(); t++) {
            tags.add(model.labelVocab.name(ms.labels[t]));
        }
        return tags;
    }

    @Override
    protected TextTokenizer getTokenizer() {
        return TOKENIZER;
    }

    public static void main(String[] args) {
        TweetNlpPosTagger posTagger = TweetNlpPosTagger.DEFAULT;
        List<Annotation> tags = posTagger
                .getAnnotations("I predict I won't win a single game I bet on. Got Cliff Lee today, so if he loses its on me RT @e_one: Texas (cont) http://tl.gd/6meogh");
        for (Annotation tagAnnotation : tags) {
            System.out.println(tagAnnotation);
        }
    }

}
