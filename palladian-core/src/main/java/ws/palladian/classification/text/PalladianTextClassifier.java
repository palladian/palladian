package ws.palladian.classification.text;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesBuilder;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Learner;
import ws.palladian.classification.text.DictionaryModel.TermCategoryEntries;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * This text classifier builds a dictionary from a pre-categorized list of text documents which can then be used to
 * categorize new, uncategorized text documents. During learning, a weighted term look up table is created, to learn how
 * probable each n-gram is for a given category. This look up table is used by during classification.
 * 
 * <p>
 * This classifier won the first Research Garden competition where the goal was to classify product descriptions into
 * eight different categories. See <a href=
 * "https://web.archive.org/web/20120122045250/http://www.research-garden.de/c/document_library/get_file?uuid=e60fa8da-4f76-4e64-a692-f74d5ffcf475&amp;groupId=10137"
 * >press release</a> (via archive.org).
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class PalladianTextClassifier implements Learner<DictionaryModel>, Classifier<DictionaryModel> {

    public static interface Scorer {
        double score(String term, String category, double probability, int documentCount, int termCount);
    }

    public static final class DefaultScorer implements Scorer {
        @Override
        public double score(String term, String category, double probability, int documentCount, int termCount) {
            return probability * probability;
        }
    }

    private final ProcessingPipeline pipeline;

    private final FeatureSetting featureSetting;

    private final Scorer scorer;

    public PalladianTextClassifier(FeatureSetting featureSetting) {
        this(featureSetting, new DefaultScorer());
    }

    public PalladianTextClassifier(FeatureSetting featureSetting, Scorer scorer) {
        Validate.notNull(featureSetting, "featureSetting must not be null");
        Validate.notNull(scorer, "scorer must not be null");
        this.featureSetting = featureSetting;
        this.pipeline = new PreprocessingPipeline(featureSetting);
        this.scorer = scorer;
    }

    @Override
    public DictionaryModel train(Iterable<? extends Trainable> trainables) {
        DictionaryModel model = new DictionaryModel(featureSetting);
        for (Trainable trainable : trainables) {
            updateModel(trainable, model);
        }
        return model;
    }

    private void updateModel(Trainable trainable, DictionaryModel model) {
//        process(trainable);
        String targetClass = trainable.getTargetClass();
//        @SuppressWarnings("unchecked")
//        ListFeature<PositionAnnotation> annotations = trainable.getFeatureVector().get(ListFeature.class,
//                BaseTokenizer.PROVIDED_FEATURE);
        String content = ((TextDocument)trainable).getContent();
        Iterator<String> iterator = new NGramIterator(content, featureSetting.getMinNGramLength(), featureSetting.getMaxNGramLength());
        iterator=CollectionHelper.limit(iterator, featureSetting.getMaxTerms());
        Set<String> terms = CollectionHelper.newHashSet();
        while (iterator.hasNext()) {
            terms.add(iterator.next());
        }
//        if (annotations != null) {
//            for (PositionAnnotation annotation : annotations) {
//                terms.add(annotation.getValue());
//            }
//        }
        model.addDocument(terms, targetClass);
    }

    @Override
    public CategoryEntries classify(Classifiable classifiable, DictionaryModel model) {

//        process(classifiable);

        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();

        // iterate through all terms in the document
//        @SuppressWarnings("unchecked")
//        ListFeature<PositionAnnotation> annotations = classifiable.getFeatureVector().get(ListFeature.class,
//                BaseTokenizer.PROVIDED_FEATURE);
        
        String content = ((TextDocument)classifiable).getContent();
        Iterator<String> iterator = new NGramIterator(content, featureSetting.getMinNGramLength(), featureSetting.getMaxNGramLength());
        iterator=CollectionHelper.limit(iterator, featureSetting.getMaxTerms());
        Bag<String>counts=Bag.create();
        while (iterator.hasNext()) {
            String term = iterator.next();
            counts.add(term);
//            TermCategoryEntries categoryEntries = model.getCategoryEntries(term);
//            for (Category category : categoryEntries) {
//                double score = scorer.score(term, category.getName(), category.getProbability(),
//                        categoryEntries.getTotalCount());
//                builder.add(category.getName(), score);
//            }
        }
        for (Entry<String, Integer> entry : counts.unique()) {
          String term = entry.getKey();
        TermCategoryEntries categoryEntries = model.getCategoryEntries(term);
        int termCount=entry.getValue();
          for (Category category : categoryEntries) {
              double score = scorer.score(term, category.getName(), category.getProbability(),
                      categoryEntries.getTotalCount(), termCount);
              builder.add(category.getName(), score);
          }
        }

//        if (annotations != null) {
//            for (PositionAnnotation annotation : annotations) {
//                TermCategoryEntries categoryEntries = model.getCategoryEntries(annotation.getValue());
//                String term = annotation.getValue();
//                for (Category category : categoryEntries) {
//                    double score = scorer.score(term, category.getName(), category.getProbability(),
//                            categoryEntries.getTotalCount());
//                    builder.add(category.getName(), score);
//                }
//            }
//        }

        // If we have a category weight by matching terms from the document, use them to create the probability
        // distribution. Else wise return the prior probability distribution of the categories.
        if (builder.getTotalScore() == 0) {
            return model.getPriors();
        }

        return builder.create();
    }

    // XXX ugly -- in case we have text documents and feature settings have been defined, do the preprocessing here
    // FIXME!!!
    private void process(Classifiable classifiable) {
        if (classifiable instanceof TextDocument) {
            pipeline.process((TextDocument)classifiable);
        }
    }

    public CategoryEntries classify(String text, DictionaryModel model) {
        return classify(new TextDocument(text), model);
    }

}
