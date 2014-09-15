package ws.palladian.extraction.entity.tagger;

import static ws.palladian.classification.text.FeatureSettingBuilder.chars;
import static ws.palladian.extraction.entity.TaggingFormat.COLUMN;
import static ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType.ERROR1;
import static ws.palladian.extraction.entity.tagger.PalladianNerSettings.LanguageMode.LanguageIndependent;

import java.io.IOException;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.DictionaryModel.DictionaryEntry;
import ws.palladian.classification.text.DictionaryTrieModel;
import ws.palladian.classification.text.ExperimentalScorers;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.PalladianTextClassifier.Scorer;
import ws.palladian.core.Annotation;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.core.ClassifyingTagger;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.Tagger;
import ws.palladian.core.Token;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.DateAndTimeTagger;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.StringTagger;
import ws.palladian.extraction.entity.TrainableNamedEntityRecognizer;
import ws.palladian.extraction.entity.UrlTagger;
import ws.palladian.extraction.entity.dataset.DatasetCreator;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.tagger.PalladianNerSettings.LanguageMode;
import ws.palladian.extraction.entity.tagger.PalladianNerSettings.TrainingMode;
import ws.palladian.extraction.location.ClassifiedAnnotation;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.RegExp;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Function;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * Palladian's Named Entity Recognizer. It is based on rule-based entity delimitation (for English texts), a text
 * classification approach, and analyzes the contexts around annotations. The major difference to other NERs is that it
 * can be learned on seed entities (just the names) or classically using supervised learning on a tagged dataset.
 * 
 * <p>
 * Palladian NER provides two language modes:
 * 
 * <ol>
 * <li>{@link LanguageMode#LanguageIndependent}: token-based, that is you can learn any language, the performance is
 * rather poor though. Consider using another recognizer.
 * <li>{@link LanguageMode#English}: NED + NEC, English only, this recognizer has shown to reach similar performance on
 * the CoNLL 2003 dataset as the state-of-the-art. It works on English texts only.
 * </ol>
 * 
 * <p>
 * Palladian NER provides two learning modes:
 * 
 * <ol>
 * <li>{@link TrainingMode#Complete}: You must have a tagged corpus in column format where the first column is the token
 * and the second column (separated by a tabstop) is the entity type.
 * <li>{@link TrainingMode#Sparse}: You just need a set of seed entities per concept (the same number per concept is
 * preferred) and you can learn a sparse training file with the {@link DatasetCreator} to learn on. Alternatively you
 * can also learn on the seed entities alone but no context information can be learned which results in a slightly worse
 * performance.
 * </ol>
 * 
 * <p>
 * Parameters for performance tuning:
 * <ul>
 * <li>n-gram size of the entity classifier (2-8 seems good)
 * <li>n-gram size of the context classifier (4-6 seems good)
 * <li>window size of the Annotation: {@link #WINDOW_SIZE}
 * </ul>
 * 
 * @author David Urbansky
 */
public class PalladianNer extends TrainableNamedEntityRecognizer implements ClassifyingTagger {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianNer.class);

    /**
     * n-gram settings for the entity classifier should be tuned, they do not have a big influence on the size of the
     * model (3-5 to 2-8 => 2MB).
     */
    private static final FeatureSetting ANNOTATION_FEATURE_SETTING = chars(4, 8).characterPadding().create();

    /** be careful with the n-gram sizes, they heavily influence the model size. */
    private static final FeatureSetting CONTEXT_FEATURE_SETTING = chars(5).create();

    private static final int WINDOW_SIZE = 40;

    public static final class PalladianNerModel implements Serializable {
        /** The serial version id. */
        private static final long serialVersionUID = 2L;

        /** This dictionary contains the entity terms as they are. */
        DictionaryModel entityDictionary;
        /** A list containing the order of likelihood of the concepts. */
        List<String> conceptLikelihoodOrder;
        /** This dictionary contains the n-grams of the entity terms, create by the text classifier. */
        DictionaryModel annotationDictionary;
        /** Context classifier for the left and right context around the annotations. */
        DictionaryModel contextDictionary;
        /** keep the case dictionary from the training data */
        DictionaryModel caseDictionary;

        Set<String> leftContexts;

        Set<String> removeAnnotations;

        PalladianNerSettings settings;

        /**
         * @return The tags which are supported by this model.
         */
        public Set<String> getTags() {
            return entityDictionary.getCategories();
        }

    }

    private final static String NO_ENTITY = "###NO_ENTITY###";

    private PalladianNerModel model;

    public PalladianNer(PalladianNerSettings settings) {
        Validate.notNull(settings, "settings must not be null");
        model = new PalladianNerModel();
        model.settings = settings;
    }

    @Override
    public String getModelFileEnding() {
        return "model.gz";
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        return false;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        model = null; // save memory
        try {
            model = FileHelper.deserialize(configModelFilePath);
        } catch (IOException e) {
            throw new IllegalStateException("Error while loading model from \"" + configModelFilePath + "\".", e);
        }
        LOGGER.info("Model {} successfully loaded", configModelFilePath);
        return true;
    }

    /**
     * Save the tagger to the specified file.
     * 
     * @param modelFilePath The file where the tagger should be saved to. You do not need to add the file ending but if
     *            you do, it should be "model.gz".
     */
    private void saveModel(String modelFilePath) {
        LOGGER.info("Annotation dictionary size: {}", model.annotationDictionary.getNumUniqTerms());
        LOGGER.info("Entity dictionary size: {}", model.entityDictionary.getNumUniqTerms());
        LOGGER.info("Context dictionary size: {}", model.contextDictionary.getNumUniqTerms());
        if (model.caseDictionary != null) {
            LOGGER.info("Case dictionary size: {}", model.caseDictionary.getNumUniqTerms());
        }
        if (model.removeAnnotations != null) {
            LOGGER.info("Remove annotations: {}", model.removeAnnotations.size());
        }
        LOGGER.info("Tags: {}", StringUtils.join(model.getTags(), ", "));
        try {
            FileHelper.serialize(model, modelFilePath);
        } catch (IOException e) {
            throw new IllegalStateException("Error while serializing to \"" + modelFilePath + "\".", e);
        }
        LOGGER.info("Serialized Palladian NER to {}", modelFilePath);
    }

    /**
     * Build a case dictionary.
     * 
     * @param token The tokens to add.
     * @return The dictionary model with categories <code>A</code> and <code>a</code> for each token.
     */
    private static DictionaryModel buildCaseDictionary(String trainingFilePath) {
        LOGGER.info("Building case dictionary");
        List<String> tokens = Tokenizer.tokenize(FileFormatParser.getText(trainingFilePath, COLUMN));
        DictionaryTrieModel.Builder builder = new DictionaryTrieModel.Builder();
        for (String token : tokens) {
            String trimmedToken = StringHelper.trim(token);
            if (trimmedToken.length() > 1) {
                String caseSignature = StringHelper.getCaseSignature(trimmedToken);
                if (caseSignature.toLowerCase().startsWith("a")) {
                    builder.addDocument(Collections.singleton(trimmedToken.toLowerCase()),
                            caseSignature.substring(0, 1));
                }
            }
        }
        return builder.create();
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {
        train(trainingFilePath, Collections.<Annotation> emptyList(), modelFilePath);
        return true;
    }

    /**
     * <p>
     * Similar to {@link train(String trainingFilePath, String modelFilePath)} method but an additional set of
     * annotations can be given to learn the classifier.
     * </p>
     * 
     * @param trainingFilePath The file of the training file.
     * @param annotations A set of annotations which are used for learning: Improving the text classifier AND adding
     *            them to the entity dictionary.
     * @param modelFilePath The path where the model should be saved to.
     */
    public void train(String trainingFilePath, List<Annotation> annotations, String modelFilePath) {
        if (model.settings.getLanguageMode() == LanguageIndependent) {
            trainLanguageIndependent(trainingFilePath, annotations);
        } else {
            trainEnglish(trainingFilePath, annotations);
        }
        saveModel(modelFilePath);
    }

    /**
     * <p>
     * Replace the trained entity dictionary with the one from the file. The file must contain a header with information
     * about the concept importance as follows:
     * </p>
     * 
     * <pre>
     * CONCEPT1>CONCEPT2>CONCEPT3>CONCEPT4>CONCEPT5>...
     * per>org>country>city>loc
     * </pre>
     * 
     * <p>
     * The concept importance is used when a candidate is ambiguous. For example, "Buddha" is usually used to refer to
     * the person but it is also the name of a city. Increasing the importance of the person concept above the city
     * concept we can make sure it will not be tagged incorrectly.
     * </p>
     * 
     * <p>
     * All subsequent lines must contain one entity and concept in the following format:
     * </p>
     * 
     * <pre>
     *   CONCEPT###ENTITY
     *   City###Dresden
     * </pre>
     * 
     * @param filePath The path to the dictionary file.
     */
    public void setEntityDictionary(String filePath) {
        final DictionaryTrieModel.Builder entityDictionaryBuilder = new DictionaryTrieModel.Builder();
        FileHelper.performActionOnEveryLine(filePath, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                if (lineNumber == 0) {
                    model.conceptLikelihoodOrder = CollectionHelper.newArrayList(line.split("\\>"));
                    return;
                }
                String[] split = line.split("###");
                if (split.length == 2) {
                    entityDictionaryBuilder.addDocument(Collections.singleton(split[1]), split[0]);
                }
            }
        });
        model.entityDictionary = entityDictionaryBuilder.create();
        LOGGER.info("Added {} entities to the dictionary", model.entityDictionary.getNumTerms());
    }

    /**
     * <p>
     * Use only a set of annotations to learn, that is, no training file is required. Use this mostly in the English
     * language mode and do not expect great performance.
     * </p>
     * 
     * @param annotations A set of annotations which are used for learning.
     * @param modelFilePath The path where the model should be saved to.
     */
    public void train(List<Annotation> annotations, String modelFilePath) {
        model.entityDictionary = buildEntityDictionary(annotations);
        model.annotationDictionary = buildAnnotationDictionary(annotations);
        saveModel(modelFilePath);
    }

    private static DictionaryModel buildEntityDictionary(Iterable<Annotation> annotations) {
        LOGGER.info("Building entity dictionary");
        DictionaryTrieModel.Builder entityDictionaryBuilder = new DictionaryTrieModel.Builder();
        for (Annotation annotation : annotations) {
            entityDictionaryBuilder.addDocument(Collections.singleton(annotation.getValue()), annotation.getTag());
        }
        return entityDictionaryBuilder.create();
    }

    private static DictionaryModel buildAnnotationDictionary(Iterable<Annotation> annotations) {
        LOGGER.info("Building annotation dictionary");
        PalladianTextClassifier textClassifier = new PalladianTextClassifier(ANNOTATION_FEATURE_SETTING);
        Iterable<Instance> instances = CollectionHelper.convert(annotations, new Function<Annotation, Instance>() {
            @Override
            public Instance compute(Annotation input) {
                return new InstanceBuilder().setText(input.getValue()).create(input.getTag());
            }
        });
        return textClassifier.train(instances);
    }

    /**
     * Train the tagger in language independent mode.
     * 
     * @param trainingFilePath The path of the training file.
     * @param additionalTrainingAnnotations Additional annotations that can be used for training.
     */
    private void trainLanguageIndependent(String trainingFilePath, List<Annotation> additionalTrainingAnnotations) {
        String text = FileFormatParser.getText(trainingFilePath, COLUMN);

        // get all training annotations
        Annotations<Annotation> tokenAnnotations = FileFormatParser
                .getAnnotationsFromColumnTokenBased(trainingFilePath);
        tokenAnnotations.addAll(additionalTrainingAnnotations);

        // get annotations combined, e.g. "Phil Simmons", not "Phil" and "Simmons"
        Annotations<Annotation> combinedAnnotations = FileFormatParser.getAnnotationsFromColumn(trainingFilePath);
        combinedAnnotations.addAll(additionalTrainingAnnotations);

        model.leftContexts = buildLeftContexts(text, combinedAnnotations);
        model.contextDictionary = buildContextDictionary(text, combinedAnnotations);
        model.entityDictionary = buildEntityDictionary(combinedAnnotations);
        model.annotationDictionary = buildAnnotationDictionary(tokenAnnotations);
    }

    /**
     * Train the tagger in English mode.
     * 
     * @param trainingFilePath The path of the training file.
     * @param additionalTrainingAnnotations Additional annotations that can be used for training.
     */
    private void trainEnglish(String trainingFilePath, List<Annotation> additionalTrainingAnnotations) {
        Annotations<Annotation> fileAnnotations = FileFormatParser.getAnnotationsFromColumn(trainingFilePath);
        String text = FileFormatParser.getText(trainingFilePath, COLUMN);

        Annotations<Annotation> annotations = new Annotations<Annotation>(fileAnnotations);
        if (additionalTrainingAnnotations.size() > 0) {
            annotations.addAll(additionalTrainingAnnotations);
            LOGGER.info("Add {} additional training annotations", additionalTrainingAnnotations.size());
        }

        model.entityDictionary = buildEntityDictionary(annotations);
        model.caseDictionary = buildCaseDictionary(trainingFilePath);

        // in complete training mode, the tagger is learned twice on the training data
        if (model.settings.isRetraining()) {
            LOGGER.info("Start retraining (because of complete dataset, no sparse annotations)");
            model.annotationDictionary = buildAnnotationDictionary(annotations);
            model.removeAnnotations = CollectionHelper.newHashSet();
            EvaluationResult evaluationResult = evaluate(trainingFilePath, COLUMN);
            Set<String> goldAnnotations = CollectionHelper.convertSet(fileAnnotations, Token.STRING_CONVERTER);
            // get only those annotations that were incorrectly tagged and were never a real entity that is they have to
            // be in ERROR1 set and NOT in the gold standard
            for (Annotation wrongAnnotation : evaluationResult.getAnnotations(ERROR1)) {
                String wrongValue = wrongAnnotation.getValue();
                annotations.add(new ImmutableAnnotation(wrongAnnotation.getStartPosition(), wrongValue, NO_ENTITY));
                // check if annotation happens to be in the gold standard, if so, do not declare it completely wrong
                if (!goldAnnotations.contains(wrongValue)) {
                    model.removeAnnotations.add(wrongValue.toLowerCase());
                }
            }
            LOGGER.info("{} annotations need to be completely removed", model.removeAnnotations.size());
        }

        model.annotationDictionary = buildAnnotationDictionary(annotations);
        model.leftContexts = buildLeftContexts(text, fileAnnotations);
        model.contextDictionary = buildContextDictionary(text, fileAnnotations);
    }

    /**
     * Classify candidate annotations.
     * 
     * @param entityCandidates The annotations to be classified.
     * @return Classified annotations.
     */
    private Annotations<ClassifiedAnnotation> classifyCandidates(List<Annotation> entityCandidates) {
        PalladianTextClassifier classifier = new PalladianTextClassifier(model.annotationDictionary.getFeatureSetting());
        Annotations<ClassifiedAnnotation> annotations = new Annotations<ClassifiedAnnotation>();
        for (Annotation annotation : entityCandidates) {
            CategoryEntries categoryEntries = classifier.classify(annotation.getValue(), model.annotationDictionary);
            if (categoryEntries.getProbability(NO_ENTITY) < 0.5) {
                annotations.add(new ClassifiedAnnotation(annotation, categoryEntries));
            }
        }
        return annotations;
    }

    @Override
    public List<ClassifiedAnnotation> getAnnotations(String inputText) {
        Annotations<ClassifiedAnnotation> annotations = getAnnotationsInternal(inputText);
        // recognize and add URLs, remove annotations that were part of a URL
        if (model.settings.isTagUrls()) {
            LOGGER.info("Tagging URLs");
            annotations.addAll(getAnnotations(UrlTagger.INSTANCE, inputText));
        }
        // recognize and add dates, remove annotations that were part of a date
        if (model.settings.isTagDates()) {
            LOGGER.info("Tagging dates");
            annotations.addAll(getAnnotations(DateAndTimeTagger.DEFAULT, inputText));
        }
        annotations.removeNested();
        return annotations;
    }

    private static List<ClassifiedAnnotation> getAnnotations(Tagger tagger, String inputText) {
        List<ClassifiedAnnotation> result = CollectionHelper.newArrayList();
        for (Annotation annotation : tagger.getAnnotations(inputText)) {
            CategoryEntries categoryEntries = new CategoryEntriesBuilder().set(annotation.getTag(), 1).create();
            result.add(new ClassifiedAnnotation(annotation, categoryEntries));
        }
        return result;
    }

    /**
     * <p>
     * Here all classified annotations are processed again. Depending on the learning settings different actions are
     * performed: Entities are re-classified by their contexts or by a dictionary.
     * 
     * @param text The text.
     * @param annotations The classified annotations to process
     * @return The processed (and potentially re-classified) annotations.
     */
    private Annotations<ClassifiedAnnotation> postProcessAnnotations(String text,
            Annotations<ClassifiedAnnotation> annotations) {
        LOGGER.debug("Start post processing annotations");
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        // switch using pattern information
        if (model.settings.isSwitchTagAnnotationsUsingContext() && model.contextDictionary != null) {
            Annotations<ClassifiedAnnotation> switched = new Annotations<ClassifiedAnnotation>();
            int changed = 0;
            for (ClassifiedAnnotation annotation : annotations) {
                ClassifiedAnnotation result = applyContextAnalysis(annotation, text);
                if (!result.sameTag(annotation)) {
                    LOGGER.debug("Changed {} from {} to {}, context: {}", annotation.getValue(), annotation.getTag(),
                            result.getTag(), getContext(annotation, text, WINDOW_SIZE));
                    changed++;
                }
                switched.add(result);
            }
            double percentage = changed > 0 ? 100. * changed / annotations.size() : 0;
            LOGGER.debug("Changed {} % using patterns", format.format(percentage));
            annotations = switched;
        }
        // switch annotations that are in the dictionary
        if (model.settings.isSwitchTagAnnotationsUsingDictionary()) {
            Annotations<ClassifiedAnnotation> switched = new Annotations<ClassifiedAnnotation>();
            int changed = 0;
            for (ClassifiedAnnotation annotation : annotations) {
                CategoryEntries categoryEntries = model.entityDictionary.getCategoryEntries(annotation.getValue());
                if (categoryEntries.size() > 0) {
                    // get only the most likely concept
                    if (model.conceptLikelihoodOrder != null) {
                        for (String conceptName : model.conceptLikelihoodOrder) {
                            double probability = categoryEntries.getProbability(conceptName);
                            if (probability > 0) {
                                categoryEntries = new CategoryEntriesBuilder().set(conceptName, 1).create();
                                break;
                            }
                        }
                    }
                    if (!annotation.getTag().equals(categoryEntries.getMostLikelyCategory())) {
                        LOGGER.debug("Changed {} from {} to {} with dictionary", annotation.getValue(),
                                annotation.getTag(), categoryEntries.getMostLikelyCategory());
                        changed++;
                    }
                    annotation = new ClassifiedAnnotation(annotation, categoryEntries);
                }
                switched.add(annotation);
            }
            double percentage = changed > 0 ? 100. * changed / annotations.size() : 0;
            LOGGER.debug("Changed {} % using entity dictionary", format.format(percentage));
            annotations = switched;
        }
        return annotations;
    }

    private Annotations<ClassifiedAnnotation> getAnnotationsInternal(String inputText) {
        Annotations<Annotation> annotations;
        if (model.settings.getLanguageMode() == LanguageIndependent) {
            // get the candidates, every token is potentially a (part of) an entity
            annotations = StringTagger.getTaggedEntities(inputText, Tokenizer.TOKEN_SPLIT_REGEX);
        } else {
            // use the the string tagger to tag entities in English mode
            annotations = StringTagger.getTaggedEntities(inputText);
        }
        preProcessAnnotations(annotations);
        Annotations<ClassifiedAnnotation> classifiedAnnotations = classifyCandidates(annotations);
        classifiedAnnotations = postProcessAnnotations(inputText, classifiedAnnotations);
        CollectionHelper.remove(classifiedAnnotations, new Filter<Annotation>() {
            @Override
            public boolean accept(Annotation item) {
                return !item.getTag().equals(NO_ENTITY);
            }
        });
        if (model.settings.getLanguageMode() == LanguageIndependent) {
            classifiedAnnotations = combineAnnotations(classifiedAnnotations);
        }
        return classifiedAnnotations;
    }

    /**
     * Combine annotations that are right next to each other having the same tag.
     * 
     * @param annotations
     * @return
     */
    private static Annotations<ClassifiedAnnotation> combineAnnotations(Annotations<ClassifiedAnnotation> annotations) {
        Annotations<ClassifiedAnnotation> combinedAnnotations = new Annotations<ClassifiedAnnotation>();
        annotations.sort();
        ClassifiedAnnotation previous = null;
        ClassifiedAnnotation previousCombined = null;
        for (ClassifiedAnnotation current : annotations) {
            if (current.getTag().equalsIgnoreCase("o")) {
                continue;
            }
            if (previous != null && current.sameTag(previous)
                    && current.getStartPosition() == previous.getEndPosition() + 1) {
                if (previousCombined == null) {
                    previousCombined = previous;
                }
                int startPosition = previousCombined.getStartPosition();
                String value = previousCombined.getValue() + " " + current.getValue();
                ClassifiedAnnotation combined = new ClassifiedAnnotation(startPosition, value,
                        previous.getCategoryEntries());
                combinedAnnotations.add(combined);
                previousCombined = combined;
                combinedAnnotations.remove(previousCombined);
            } else {
                combinedAnnotations.add(current);
                previousCombined = null;
            }
            previous = current;
        }
        return combinedAnnotations;
    }

    private void preProcessAnnotations(Annotations<Annotation> annotations) {
        LOGGER.debug("Start pre processing annotations");
        if (model.settings.isRemoveIncorrectlyTaggedInTraining()) {
            removeIncorrectlyTaggedInTraining(annotations);
        }
        if (model.settings.isUnwrapEntities()) {
            unwrapEntities(annotations);
        }
        if (model.settings.isRemoveSentenceStartErrorsCaseDictionary() && model.caseDictionary != null) {
            removeSentenceStartErrors(annotations);
        }
        if (model.settings.isUnwrapEntitiesWithContext() && model.leftContexts != null) {
            unwrapWithContext(annotations);
        }
        if (model.settings.isRemoveDateFragments()) {
            removeDateFragments(annotations);
        }
        if (model.settings.isRemoveDates()) {
            removeDates(annotations);
        }
    }

    private static void removeDateFragments(Annotations<Annotation> annotations) {
        Annotations<Annotation> toAdd = new Annotations<Annotation>();
        Annotations<Annotation> toRemove = new Annotations<Annotation>();
        for (Annotation annotation : annotations) {
            Annotation result = removeDateFragment(annotation);
            if (result != null) {
                toRemove.add(annotation);
                toAdd.add(result);
            }
        }
        LOGGER.debug("Removed {} partial date annotations", toRemove.size());
        annotations.addAll(toAdd);
        annotations.removeAll(toRemove);
    }

    private static void removeDates(Annotations<Annotation> annotations) {
        int numRemoved = CollectionHelper.remove(annotations, new Filter<Annotation>() {
            @Override
            public boolean accept(Annotation annotation) {
                return !isDateFragment(annotation.getValue());
            }
        });
        LOGGER.debug("Removed {} purely date annotations", numRemoved);
    }

    private void unwrapWithContext(Annotations<Annotation> annotations) {
        Annotations<Annotation> toAdd = new Annotations<Annotation>();
        Annotations<Annotation> toRemove = new Annotations<Annotation>();
        for (Annotation annotation : annotations) {
            String entity = annotation.getValue();
            // do not unwrap, in case we have the value in the entity dictionary
            if (model.entityDictionary.getCategoryEntries(entity).getTotalCount() > 0) {
                continue;
            }
            for (String leftContext : model.leftContexts) {
                int index1 = entity.indexOf(leftContext + " ");
                int index2 = entity.indexOf(" " + leftContext + " ");
                int length = -1;
                int index = -1;
                if (index1 == 0) {
                    length = leftContext.length() + 1;
                    index = index1;
                } else if (index2 > -1) {
                    length = leftContext.length() + 2;
                    index = index2;
                }
                if (index != -1) {
                    // get the annotation after the index
                    int startPosition = annotation.getStartPosition() + index + length;
                    String value = annotation.getValue().substring(index + length);
                    toAdd.add(new ImmutableAnnotation(startPosition, value, annotation.getTag()));
                    // search for a known instance in the prefix by going through the entity dictionary
                    for (DictionaryEntry entry : model.entityDictionary) {
                        String term = entry.getTerm();
                        int indexPrefix = annotation.getValue().substring(0, index + length).indexOf(term + " ");
                        if (indexPrefix > -1 && term.length() > 2) {
                            int prefixStart = annotation.getStartPosition() + indexPrefix;
                            String tag = entry.getCategoryEntries().getMostLikelyCategory();
                            toAdd.add(new ImmutableAnnotation(prefixStart, term, tag));
                            LOGGER.debug("Add from prefix {}", term);
                            break;
                        }
                    }
                    toRemove.add(annotation);
                    LOGGER.debug("Add {}, delete {} (left context: {})", value, annotation.getValue(), leftContext);
                    break;
                }
            }
        }
        annotations.addAll(toAdd);
        annotations.removeAll(toRemove);
    }

    /**
     * Use a learned case dictionary to remove possibly incorrectly tagged sentence starts. For example ". This" is
     * removed since "this" is usually spelled using lowercase characters only. This is done NOT only for words at
     * sentence start but all single token words.
     * 
     * @param annotations
     */
    private void removeSentenceStartErrors(Annotations<Annotation> annotations) {
        int removed = CollectionHelper.remove(annotations, new Filter<Annotation>() {
            @Override
            public boolean accept(Annotation annotation) {
                if (annotation.getValue().indexOf(" ") == -1) {
                    CategoryEntries ces = model.caseDictionary.getCategoryEntries(annotation.getValue().toLowerCase());
                    double upperCase = ces.getProbability("A");
                    double lowerCase = ces.getProbability("a");
                    if (lowerCase > 0 && upperCase / lowerCase <= 1) {
                        if (LOGGER.isDebugEnabled()) {
                            NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
                            LOGGER.debug("Remove by case signature: {} (ratio:{})", annotation.getValue(),
                                    format.format(upperCase / lowerCase));
                        }
                        return false;
                    }
                }
                return true;
            }
        });
        LOGGER.debug("Removed {} words at beginning of sentence", removed);
    }

    private void removeIncorrectlyTaggedInTraining(Annotations<Annotation> annotations) {
        int removed = CollectionHelper.remove(annotations, new Filter<Annotation>() {
            @Override
            public boolean accept(Annotation annotation) {
                return !model.removeAnnotations.contains(annotation.getValue().toLowerCase());
            }
        });
        LOGGER.debug("Removed {} incorrectly tagged entities in training data", removed);
    }

    private void unwrapEntities(Annotations<Annotation> annotations) {
        Annotations<Annotation> toAdd = new Annotations<Annotation>();
        Annotations<Annotation> toRemove = new Annotations<Annotation>();
        for (Annotation annotation : annotations) {
            boolean isAllUppercase = StringHelper.isCompletelyUppercase(annotation.getValue());
            if (isAllUppercase) {
                Annotations<Annotation> unwrapped = unwrapAnnotations(annotation, annotations);
                if (unwrapped.size() > 0) {
                    toAdd.addAll(unwrapped);
                    toRemove.add(annotation);
                }
            }
        }
        annotations.removeAll(toRemove);
        annotations.addAll(toAdd);
        LOGGER.debug("Unwrapping removed {}, added {} entities", toRemove.size(), toAdd.size());
    }

    private ClassifiedAnnotation applyContextAnalysis(ClassifiedAnnotation annotation, String text) {
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();
        builder.add(annotation.getCategoryEntries());
        FeatureSetting featureSetting = model.contextDictionary.getFeatureSetting();
        Scorer scorer = new ExperimentalScorers.CategoryEqualizationScorer();
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting, scorer);
        String context = getContext(annotation, text, WINDOW_SIZE);
        if (context.trim().length() > 2) {
            CategoryEntries contextClassification = classifier.classify(context, model.contextDictionary);
            builder.add(contextClassification);
        }
        return new ClassifiedAnnotation(annotation, builder.create());
    }

    static String getContext(Annotation annotation, String text, int size) {
        int offset = annotation.getStartPosition();
        String entityName = annotation.getValue();
        int length = entityName.length();
        String leftContext = text.substring(Math.max(0, offset - size), offset).trim();
        String rightContext = text.substring(offset + length, Math.min(text.length(), offset + length + size)).trim();
        return leftContext + "__" + rightContext;
    }

    static List<String> getLeftContexts(Annotation annotation, String text, int size) {
        List<String> contexts = CollectionHelper.newArrayList();
        StringBuilder builder = new StringBuilder();
        for (int idx = annotation.getStartPosition() - 1; idx >= 0; idx--) {
            char ch = text.charAt(idx);
            builder.append(ch);
            if (ch == ' ' || idx == 0) {
                String value = builder.toString().trim().replaceAll("\\d", "§");
                if (value.length() > 0) {
                    contexts.add(StringHelper.reverseString(value));
                }
            }
            if (contexts.size() == size) {
                break;
            }
        }
        return contexts;
    }

    /**
     * Check whether the given text is a date fragment, e.g. "June".
     * 
     * @param value The value to check.
     * @return <code>true</code> in case the text is a date fragment.
     */
    static boolean isDateFragment(String value) {
        for (String dateFragment : RegExp.DATE_FRAGMENTS) {
            if (StringUtils.isBlank(value.replaceAll(dateFragment, " "))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Try to remove date fragments from the given annotation, e.g. "June John Hiatt" becomes "John Hiatt".
     * 
     * @param annotation The annotation to process.
     * @return A new annotation with removed date fragments and fixed offsets, or <code>null</code> in case the given
     *         annotation did not contain a date fragment.
     */
    static Annotation removeDateFragment(Annotation annotation) {
        String newValue = annotation.getValue();
        int newOffset = annotation.getStartPosition();
        for (String dateFragment : RegExp.DATE_FRAGMENTS) {
            String regExp = "(?:" + dateFragment + ")\\.?";
            String beginRegExp = "^" + regExp + " ";
            String endRegExp = " " + regExp + "$";
            int textLength = newValue.length();
            if (StringHelper.countRegexMatches(newValue, beginRegExp) > 0) {
                newValue = newValue.replaceAll(beginRegExp, " ").trim();
                newOffset += textLength - newValue.length();
            }
            if (StringHelper.countRegexMatches(newValue, endRegExp) > 0) {
                newValue = newValue.replaceAll(endRegExp, " ").trim();
            }
        }
        if (annotation.getValue().equals(newValue)) {
            return null;
        }
        return new ImmutableAnnotation(newOffset, newValue, annotation.getTag());
    }

    /**
     * Build a set with left contexts. These are tokens which appear to the left of an entity, e.g.
     * "President Barack Obama". From the available annotations we determine, whether "President" belongs to the entity,
     * or to the context. This information can be used later, to fix the boundaries of an annotation.
     * @param annotations The annotations.
     * 
     * @return A set with tokens which appear more often in the context, than within an entity (e.g. "President").
     */
    private static Set<String> buildLeftContexts(String text, Annotations<Annotation> annotations) {
        LOGGER.info("Building left contexts");
        Bag<String> leftContextCounts = Bag.create();
        Bag<String> insideAnnotationCounts = Bag.create();
        for (Annotation annotation : annotations) {
            leftContextCounts.addAll(getLeftContexts(annotation, text, 3));
            String[] split = annotation.getValue().split("\\s");
            StringBuilder partBuilder = new StringBuilder();
            for (int i = 0; i < split.length; i++) {
                if (i > 0) {
                    partBuilder.append(' ');
                }
                partBuilder.append(split[i]);
                insideAnnotationCounts.add(partBuilder.toString());
            }
        }
        Set<String> leftContexts = CollectionHelper.newHashSet();
        for (Entry<String, Integer> entry : leftContextCounts.unique()) {
            String leftContext = entry.getKey();
            if (StringHelper.startsUppercase(leftContext)) {
                int outside = entry.getValue();
                int inside = insideAnnotationCounts.count(leftContext);
                double ratio = (double)inside / outside;
                if (ratio < 1 && outside >= 2) {
                    leftContexts.add(leftContext);
                }
            }
        }
        return leftContexts;
    }

    private static DictionaryModel buildContextDictionary(final String text, Iterable<Annotation> annotations) {
        LOGGER.info("Building context dictionary");
        PalladianTextClassifier contextClassifier = new PalladianTextClassifier(CONTEXT_FEATURE_SETTING);
        Iterable<Instance> instances = CollectionHelper.convert(annotations, new Function<Annotation, Instance>() {
            @Override
            public Instance compute(Annotation input) {
                return new InstanceBuilder().setText(getContext(input, text, WINDOW_SIZE)).create(input.getTag());
            }
        });
        return contextClassifier.train(instances);
    }

    public PalladianNerModel getModel() {
        return model;
    }

    /**
     * <p>
     * If the annotation is completely upper case, like "NEW YORK CITY AND DRESDEN", try to find which of the given
     * annotation are part of this entity. The given example contains two entities that might be in the given annotation
     * set. If so, we return the found annotations.
     * 
     * @param annotation The annotation to check.
     * @param annotations The annotations we are searching for in this entity.
     * @return A set of annotations found in this annotation.
     */
    private Annotations<Annotation> unwrapAnnotations(Annotation annotation, List<Annotation> annotations) {
        Annotations<Annotation> unwrappedAnnotations = new Annotations<Annotation>();
        for (Annotation currentAnnotation : annotations) {
            if (!currentAnnotation.equals(annotation)) {
                String currentValue = currentAnnotation.getValue();
                String currentTag = currentAnnotation.getTag();
                unwrappedAnnotations.addAll(processUnwrap(annotation, currentValue, currentTag));
            }
        }
        for (DictionaryEntry categoryEntries : model.entityDictionary) {
            String term = categoryEntries.getTerm();
            String category = categoryEntries.getCategoryEntries().getMostLikelyCategory();
            unwrappedAnnotations.addAll(processUnwrap(annotation, term, category));
        }
        unwrappedAnnotations.removeNested();
        if (LOGGER.isDebugEnabled() && unwrappedAnnotations.size() > 0) {
            StringBuilder parts = new StringBuilder();
            for (Annotation unwrappedAnnotation : unwrappedAnnotations) {
                if (!unwrappedAnnotation.getValue().equalsIgnoreCase(annotation.getValue())) {
                    if (parts.length() > 0) {
                        parts.append(", ");
                    }
                    parts.append(unwrappedAnnotation.getValue());
                }
            }
            if (parts.length() > 0) {
                LOGGER.debug("Unwrapped {} in {} parts: {}", annotation.getValue(), unwrappedAnnotations.size(), parts);
            }
        }
        return unwrappedAnnotations;
    }

    private static List<Annotation> processUnwrap(Annotation annotation, String value, String tag) {
        int currentLength = value.length();
        String entityName = annotation.getValue().toLowerCase();
        int entityLength = entityName.length();
        int entityStart = annotation.getStartPosition();
        List<Annotation> unwrappedAnnotations = CollectionHelper.newArrayList();
        if (entityName.equalsIgnoreCase(value)) {
            unwrappedAnnotations.add(new ImmutableAnnotation(entityStart, value, tag));
        } else if (currentLength < entityLength) {
            int index = entityName.indexOf(" " + value.toLowerCase() + " ");
            if (index > -1) {
                unwrappedAnnotations.add(new ImmutableAnnotation(entityStart + index + 1, value, tag));
            }
            index = entityName.indexOf(value.toLowerCase() + " ");
            if (index == 0) {
                unwrappedAnnotations.add(new ImmutableAnnotation(entityStart + index, value, tag));
            }
            index = entityName.indexOf(" " + value.toLowerCase());
            if (index == entityLength - currentLength - 1) {
                unwrappedAnnotations.add(new ImmutableAnnotation(entityStart + index + 1, value, tag));
            }
        }
        return unwrappedAnnotations;
    }

    @Override
    public String getName() {
        return "Palladian NER (" + model.settings + ")";
    }

}
