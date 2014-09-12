package ws.palladian.extraction.entity.tagger;

import static ws.palladian.classification.text.FeatureSettingBuilder.chars;
import static ws.palladian.extraction.entity.tagger.PalladianNerSettings.LanguageMode.LanguageIndependent;

import java.io.IOException;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Arrays;
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
import ws.palladian.classification.text.DictionaryModel.TermCategoryEntries;
import ws.palladian.classification.text.DictionaryTrieModel;
import ws.palladian.classification.text.ExperimentalScorers;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.PalladianTextClassifier.Scorer;
import ws.palladian.core.Annotation;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.ContextAnnotation;
import ws.palladian.extraction.entity.DateAndTimeTagger;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.StringTagger;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.TrainableNamedEntityRecognizer;
import ws.palladian.extraction.entity.UrlTagger;
import ws.palladian.extraction.entity.dataset.DatasetCreator;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType;
import ws.palladian.extraction.entity.tagger.PalladianNerSettings.LanguageMode;
import ws.palladian.extraction.entity.tagger.PalladianNerSettings.TrainingMode;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.RegExp;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Function;
import ws.palladian.helper.io.FileHelper;
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
 * <li>window size of the Annotation: {@link FileFormatParser#WINDOW_SIZE}
 * </ul>
 * 
 * @author David Urbansky
 */
public class PalladianNer extends TrainableNamedEntityRecognizer {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianNer.class);

    /**
     * n-gram settings for the entity classifier should be tuned, they do not have a big influence on the size of the
     * model (3-5 to 2-8 => 2MB).
     */
    private static final FeatureSetting ANNOTATION_FEATURE_SETTING = chars(4, 8).characterPadding().create();

    /** be careful with the n-gram sizes, they heavily influence the model size. */
    private static final FeatureSetting CONTEXT_FEATURE_SETTING = chars(5).create();

    public static final class PalladianNerModel implements Serializable {
        /** The serial version id. */
        private static final long serialVersionUID = 1L;

        /** This dictionary contains the entity terms as they are. */
        DictionaryModel entityDictionary;
        /** A list containing the order of likelihood of the concepts. */
        List<String> conceptLikelihoodOrder;
        /** This dictionary contains the n-grams of the entity terms, create by the text classifier. */
        DictionaryModel annotationModel;
        /** Context classifier for the left and right context around the annotations. */
        DictionaryModel contextModel;
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
        StopWatch stopWatch = new StopWatch();
        model = null; // save memory
        try {
            model = FileHelper.deserialize(configModelFilePath);
        } catch (IOException e) {
            throw new IllegalStateException("Error while loading model from \"" + configModelFilePath + "\".", e);
        }
        LOGGER.info("Model {} successfully loaded in {}", configModelFilePath, stopWatch);
        return true;
    }

    /**
     * Save the tagger to the specified file.
     * 
     * @param modelFilePath The file where the tagger should be saved to. You do not need to add the file ending but if
     *            you do, it should be "model.gz".
     */
    private void saveModel(String modelFilePath) {
        LOGGER.info("Annotation dictionary size: {}", model.annotationModel.getNumUniqTerms());
        LOGGER.info("Entity dictionary size: {}", model.entityDictionary.getNumUniqTerms());
        LOGGER.info("Context dictionary size: {}", model.contextModel.getNumUniqTerms());
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
     * @return The dictionary model with categories <code>Aa</code>, <code>A</code>, and <code>a</code> for each token.
     */
    private static DictionaryModel buildCaseDictionary(List<String> tokens) {
        LOGGER.info("Building case dictionary");
        DictionaryTrieModel.Builder builder = new DictionaryTrieModel.Builder();
        for (String token : tokens) {
            String trimmedToken = StringHelper.trim(token);
            if (trimmedToken.length() > 1) {
                String caseSignature = StringHelper.getCaseSignature(trimmedToken);
                if (Arrays.asList("Aa", "A", "a").contains(caseSignature)) {
                    builder.addDocument(Collections.singleton(trimmedToken.toLowerCase()), caseSignature);
                }
            }
        }
        return builder.create();
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {
        return train(trainingFilePath, new Annotations<ContextAnnotation>(), modelFilePath);
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
     * @return <tt>True</tt>, if all training worked, <tt>false</tt> otherwise.
     */
    public boolean train(String trainingFilePath, List<? extends Annotation> annotations, String modelFilePath) {
        LOGGER.info("Start creating {} annotations for training", annotations.size());
        if (model.settings.getLanguageMode() == LanguageIndependent) {
            trainLanguageIndependent(trainingFilePath, modelFilePath, annotations);
        } else {
            trainEnglish(trainingFilePath, modelFilePath, annotations);
        }
        return true;
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
        DictionaryTrieModel.Builder entityDictionaryBuilder = new DictionaryTrieModel.Builder();

        StopWatch stopWatch = new StopWatch();
        List<String> dictionaryEntries = FileHelper.readFileToArray(filePath);

        int i = 1;
        for (String dictionaryEntry : dictionaryEntries) {

            // fill the likelihood list
            if (i == 1) {
                model.conceptLikelihoodOrder = CollectionHelper.newArrayList(dictionaryEntry.split("\\>"));
                i++;
                continue;
            }

            String[] split = dictionaryEntry.split("###");
            if (split.length < 2) {
                continue;
            }
            entityDictionaryBuilder.addDocument(Collections.singleton(split[1]), split[0]);
            i++;
        }
        model.entityDictionary = entityDictionaryBuilder.create();
        LOGGER.info("Added {} entities to the dictionary in {}", i - 2, stopWatch);
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
    public void train(Annotations<ContextAnnotation> annotations, String modelFilePath) {
        trainLanguageIndependent(annotations, annotations, modelFilePath);
    }

    private void trainLanguageIndependent(Annotations<ContextAnnotation> annotations,
            Annotations<ContextAnnotation> combinedAnnotations, String modelFilePath) {
        model.entityDictionary = buildEntityDictionary(combinedAnnotations);
        model.annotationModel = buildAnnotationDictionary(annotations);
        saveModel(modelFilePath);
    }

    private static DictionaryModel buildEntityDictionary(Iterable<? extends Annotation> annotations) {
        LOGGER.info("Building entity dictionary");
        DictionaryTrieModel.Builder entityDictionaryBuilder = new DictionaryTrieModel.Builder();
        for (Annotation annotation : annotations) {
            entityDictionaryBuilder.addDocument(Collections.singleton(annotation.getValue()), annotation.getTag());
        }
        return entityDictionaryBuilder.create();
    }

    private static DictionaryModel buildAnnotationDictionary(Iterable<? extends Annotation> annotations) {
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
     * <p>
     * Train the tagger in language independent mode.
     * </p>
     * 
     * @param trainingFilePath The path of the training file.
     * @param modelFilePath The path where the model should be saved to.
     * @param additionalTrainingAnnotations Additional annotations that can be used for training.
     */
    private void trainLanguageIndependent(String trainingFilePath, String modelFilePath,
            List<? extends Annotation> additionalTrainingAnnotations) {

        // get all training annotations
        Annotations<ContextAnnotation> tokenAnnotations = FileFormatParser
                .getAnnotationsFromColumnTokenBased(trainingFilePath);

        // get annotations combined, e.g. "Phil Simmons", not "Phil" and "Simmons"
        Annotations<ContextAnnotation> combinedAnnotations = FileFormatParser
                .getAnnotationsFromColumn(trainingFilePath);

        model.leftContexts = buildLeftContexts(combinedAnnotations);
        model.contextModel = buildContextDictionary(combinedAnnotations);

        // add the additional training annotations
        for (Annotation annotation : additionalTrainingAnnotations) {
            ContextAnnotation contextAnnotation = new ContextAnnotation(annotation);
            tokenAnnotations.add(contextAnnotation);
            combinedAnnotations.add(contextAnnotation);
        }

        trainLanguageIndependent(tokenAnnotations, combinedAnnotations, modelFilePath);
    }

    /**
     * Train the tagger in English mode.
     * 
     * @param trainingFilePath The path of the training file.
     * @param modelFilePath The path where the model should be saved to.
     * @param additionalTrainingAnnotations Additional annotations that can be used for training.
     */
    private void trainEnglish(String trainingFilePath, String modelFilePath,
            List<? extends Annotation> additionalTrainingAnnotations) {

        // get all training annotations
        LOGGER.info("Get annotations from column-formatted training file");
        Annotations<ContextAnnotation> fileAnnotations = FileFormatParser.getAnnotationsFromColumn(trainingFilePath);
        
        Annotations<Annotation> annotations = new Annotations<Annotation>(fileAnnotations);
        // add the additional training annotations, they will be used for the context analysis too
        for (Annotation annotation : additionalTrainingAnnotations) {
            annotations.add(new ContextAnnotation(annotation));
        }
        LOGGER.info("Add {} additional training annotations", additionalTrainingAnnotations.size());

        model.entityDictionary = buildEntityDictionary(annotations);

        List<String> tokens = Tokenizer.tokenize(FileFormatParser.getText(trainingFilePath, TaggingFormat.COLUMN));
        model.caseDictionary = buildCaseDictionary(tokens);

        // in complete training mode, the tagger is learned twice on the training data
        if (model.settings.isRetraining()) {
            LOGGER.info("Start retraining (because of complete dataset, no sparse annotations)");

            // //////////////////////////////////////////// wrong entities //////////////////////////////////////
            model.annotationModel = buildAnnotationDictionary(annotations);

            model.removeAnnotations = CollectionHelper.newHashSet();
            EvaluationResult evaluationResult = evaluate(trainingFilePath, TaggingFormat.COLUMN);
            Annotations<ContextAnnotation> goldStandard = FileFormatParser.getAnnotations(trainingFilePath,
                    TaggingFormat.COLUMN);
            goldStandard.sort();

            // get only those annotations that were incorrectly tagged and were never a real entity that is they have to
            // be in ERROR1 set and NOT in the gold standard
            for (Annotation wrongAnnotation : evaluationResult.getAnnotations(ResultType.ERROR1)) {

                annotations.add(new ContextAnnotation(wrongAnnotation.getStartPosition(), wrongAnnotation.getValue(),
                        NO_ENTITY));

                // check if annotation happens to be in the gold standard, if so, do not declare it completely wrong
                boolean addAnnotation = true;
                String wrongName = wrongAnnotation.getValue();
                for (Annotation gsAnnotation : goldStandard) {
                    if (wrongName.equalsIgnoreCase(gsAnnotation.getValue())) {
                        addAnnotation = false;
                        break;
                    }
                }
                if (addAnnotation) {
                    model.removeAnnotations.add(wrongAnnotation.getValue().toLowerCase());
                }
            }
            LOGGER.info("{} annotations need to be completely removed", model.removeAnnotations.size());
            // //////////////////////////////////////////////////////////////////////////////////////////////////
        }

        model.annotationModel = buildAnnotationDictionary(annotations);
        model.leftContexts = buildLeftContexts(fileAnnotations);
        model.contextModel = buildContextDictionary(fileAnnotations);
        saveModel(modelFilePath);
    }

    /**
     * Classify candidate annotations.
     * 
     * @param entityCandidates The annotations to be classified.
     * @return Classified annotations.
     */
    private Annotations<ContextAnnotation> classifyCandidates(List<ContextAnnotation> entityCandidates) {
        PalladianTextClassifier classifier = new PalladianTextClassifier(model.annotationModel.getFeatureSetting());
        Annotations<ContextAnnotation> annotations = new Annotations<ContextAnnotation>();
        for (ContextAnnotation annotation : entityCandidates) {
            CategoryEntries classificationResult = classifier.classify(annotation.getValue(), model.annotationModel);
            if (classificationResult.getProbability(NO_ENTITY) < 0.5) {
                annotation.setTags(classificationResult);
                annotations.add(annotation);
            }
        }
        return annotations;
    }

    @Override
    public List<Annotation> getAnnotations(String inputText) {
        StopWatch stopWatch = new StopWatch();

        Annotations<Annotation> annotations = new Annotations<Annotation>();

        annotations.addAll(getAnnotationsInternal(inputText));

        // recognize and add URLs, remove annotations that were part of a URL
        if (model.settings.isTagUrls()) {
            LOGGER.info("Tagging URLs");
            annotations.addAll(UrlTagger.INSTANCE.getAnnotations(inputText));
        }

        // recognize and add dates, remove annotations that were part of a date
        if (model.settings.isTagDates()) {
            LOGGER.info("Tagging dates");
            annotations.addAll(new DateAndTimeTagger().getAnnotations(inputText));
        }
        
        CollectionHelper.remove(annotations, new Filter<Annotation>() {
            @Override
            public boolean accept(Annotation item) {
                return !item.getTag().equals(NO_ENTITY);
            }
        });

        annotations.removeNested();
        LOGGER.info("Got {} annotations in {}", annotations.size(), stopWatch);
        return annotations;
    }

    /**
     * <p>
     * Here all classified annotations are processed again. Depending on the learning settings different actions are
     * performed. These are for example, removing date entries, unwrapping entities, using context patterns to switch
     * annotations or remove possibly incorrect annotations with the case dictionary.
     * </p>
     * 
     * @param annotations The classified annotations to process
     */
    private void postProcessAnnotations(List<ContextAnnotation> annotations) {

        LOGGER.debug("Start post processing annotations");
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);

        // switch using pattern information
        if (model.settings.isSwitchTagAnnotationsUsingPatterns() && model.contextModel != null) {
            int changed = 0;
            for (ContextAnnotation annotation : annotations) {
                String tagNameBefore = annotation.getTag();
                applyContextAnalysis(annotation);
                if (!annotation.getTag().equalsIgnoreCase(tagNameBefore)) {
                    LOGGER.debug("Changed {} from {} to {}, context: {} __ {}", annotation.getValue(), tagNameBefore,
                            annotation.getTag(), annotation.getLeftContext(), annotation.getRightContext());
                    changed++;
                }
            }
            double percentage = changed > 0 ? 100. * changed / annotations.size() : 0;
            LOGGER.debug("Changed {} % using patterns", format.format(percentage));
        }

        // switch annotations that are in the dictionary
        if (model.settings.isSwitchTagAnnotationsUsingDictionary()) {
            int changed = 0;
            for (ContextAnnotation annotation : annotations) {
                CategoryEntries categoryEntries = model.entityDictionary.getCategoryEntries(annotation.getValue());
                if (categoryEntries != null && categoryEntries.iterator().hasNext()) {
                    // get only the most likely concept
                    CategoryEntriesBuilder mostLikelyBuilder = new CategoryEntriesBuilder();
                    if (model.conceptLikelihoodOrder != null) {
                        ol: for (String conceptName : model.conceptLikelihoodOrder) {
                            for (Category categoryEntry : categoryEntries) {
                                if (categoryEntry.getProbability() > 0
                                        && categoryEntry.getName().equalsIgnoreCase(conceptName)) {
                                    mostLikelyBuilder.set(categoryEntry.getName(), categoryEntry.getProbability());
                                    break ol;
                                }
                            }
                        }
                        CategoryEntries mostLikelyCes = mostLikelyBuilder.create();
                        if (mostLikelyCes.iterator().hasNext()) {
                            categoryEntries = mostLikelyCes;
                        }
                    }
                    annotation.setTags(categoryEntries);
                    changed++;
                }
            }
            double percentage = changed > 0 ? 100. * changed / annotations.size() : 0;
            LOGGER.debug("Changed {} % using entity dictionary", format.format(percentage));
        }

    }

    private Annotations<ContextAnnotation> getAnnotationsInternal(String inputText) {

        Annotations<ContextAnnotation> annotations;

        if (model.settings.getLanguageMode() == LanguageIndependent) {
            // get the candidates, every token is potentially a (part of) an entity
            annotations = StringTagger.getTaggedEntities(inputText, Tokenizer.TOKEN_SPLIT_REGEX);
        } else {
            // use the the string tagger to tag entities in English mode
            annotations = StringTagger.getTaggedEntities(inputText);
        }
        
        preProcessAnnotations(annotations);

        annotations = classifyCandidates(annotations);

        postProcessAnnotations(annotations);

        if (model.settings.getLanguageMode() == LanguageIndependent) {

            // combine annotations that are right next to each other having the same tag
            Annotations<ContextAnnotation> combinedAnnotations = new Annotations<ContextAnnotation>();
            annotations.sort();

            Annotation previous = null;
            Annotation previousCombined = null;

            for (ContextAnnotation current : annotations) {
                if (!current.getTag().equalsIgnoreCase("o") && previous != null && current.sameTag(previous)
                        && current.getStartPosition() == previous.getEndPosition() + 1) {

                    if (previousCombined == null) {
                        previousCombined = previous;
                    }

                    ContextAnnotation combined = new ContextAnnotation(previousCombined.getStartPosition(),
                            previousCombined.getValue() + " " + current.getValue(), current.getTag());
                    combinedAnnotations.add(combined);
                    previousCombined = combined;
                    combinedAnnotations.remove(previousCombined);
                } else {
                    combinedAnnotations.add(current);
                    previousCombined = null;
                }
                previous = current;
            }

            // remove all "O"
            CollectionHelper.remove(combinedAnnotations, new Filter<ContextAnnotation>() {
                @Override
                public boolean accept(ContextAnnotation item) {
                    return !item.getTag().equalsIgnoreCase("o") && item.getValue().length() > 1;
                }
            });
            annotations = combinedAnnotations;
        }

        return annotations;
    }

    private void preProcessAnnotations(Annotations<ContextAnnotation> annotations) {
        LOGGER.debug("Start pre processing annotations");

        Annotations<ContextAnnotation> toRemove = new Annotations<ContextAnnotation>();
        Annotations<ContextAnnotation> toAdd = new Annotations<ContextAnnotation>();
        
        StopWatch stopWatch = new StopWatch();
        
        // remove dates
        if (model.settings.isRemoveDates()) {
            stopWatch.start();
            int c = 0;
            for (ContextAnnotation annotation : annotations) {
                if (isDateFragment(annotation.getValue())) {
                    toRemove.add(annotation);
                    c++;
                }
            }
            LOGGER.debug("Removed {} purely date annotations in {}", c, stopWatch);
        }

        // remove date entries in annotations, such as "July Peter Jackson" => "Peter Jackson"
        if (model.settings.isRemoveDateFragments()) {
            stopWatch.start();
            int c = 0;
            for (ContextAnnotation annotation : annotations) {
                ContextAnnotation result = removeDateFragment(annotation);
                if (result != null) {
                    c++;
                    toRemove.add(annotation);
                    toAdd.add(result);
                }
            }
            LOGGER.debug("Removed {} partial date annotations in {}", c, stopWatch);
        }

        // remove annotations that were found to be incorrectly tagged in the training data
        if (model.settings.isRemoveIncorrectlyTaggedInTraining()) {
            stopWatch.start();
            int c = 0;
            for (ContextAnnotation annotation : annotations) {
                if (model.removeAnnotations.contains(annotation.getValue().toLowerCase())) {
                    toRemove.add(annotation);
                    c++;
                }
            }
            LOGGER.debug("Removed {} incorrectly tagged entities in training data in {}", c, stopWatch);
        }

        if (model.settings.isUnwrapEntities()) {
            for (ContextAnnotation annotation : annotations) {
                boolean isAllUppercase = StringHelper.isCompletelyUppercase(annotation.getValue());
                if (isAllUppercase) {
                    Annotations<ContextAnnotation> unwrapped = unwrapAnnotations(annotation, annotations);
                    if (unwrapped.size() > 0) {
                        toAdd.addAll(unwrapped);
                        toRemove.add(annotation);
                    }
                }
            }
            annotations.removeAll(toRemove);
            annotations.addAll(toAdd);
        }

        // use a learned case dictionary to remove possibly incorrectly tagged sentence starts. For example ". This" is
        // removed since "this" is usually spelled using lowercase characters only. This is done NOT only for words at
        // sentence start but all single token words.
        int c = 0;
        if (model.caseDictionary != null && model.settings.isRemoveSentenceStartErrorsCaseDictionary()) {
            stopWatch.start();

            for (ContextAnnotation annotation : annotations) {

                if (annotation.getValue().indexOf(" ") == -1) {

                    double upperCaseToLowerCaseRatio = 2;

                    CategoryEntries ces = model.caseDictionary.getCategoryEntries(annotation.getValue().toLowerCase());
                    if (ces != null && ces.iterator().hasNext()) {
                        double allUpperCase = ces.getProbability("A");
                        double upperCase = ces.getProbability("Aa");
                        double lowerCase = ces.getProbability("a");
                        if (lowerCase > 0) {
                            upperCaseToLowerCaseRatio = upperCase / lowerCase;
                        }
                        if (allUpperCase > upperCase && allUpperCase > lowerCase) {
                            upperCaseToLowerCaseRatio = 2;
                        }
                    }
                    if (upperCaseToLowerCaseRatio <= 1) {
                        c++;
                        toRemove.add(annotation);
                        if (LOGGER.isDebugEnabled()) {
                            NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
                            LOGGER.debug("Remove word using the case signature: {} (ratio:{})", annotation.getValue(),
                                    format.format(upperCaseToLowerCaseRatio));
                        }
                    }
                }
            }
            LOGGER.debug("Removed {} words at beginning of sentence in {}", c, stopWatch);
        }

        if (model.settings.isUnwrapEntitiesWithContext() && model.leftContexts != null) {
            for (ContextAnnotation annotation : annotations) {

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
                        ContextAnnotation wrappedAnnotation = new ContextAnnotation(annotation.getStartPosition()
                                + index + length, annotation.getValue().substring(index + length), annotation.getTag());
                        toAdd.add(wrappedAnnotation);

                        // search for a known instance in the prefix
                        // go through the entity dictionary
                        for (TermCategoryEntries termEntries : model.entityDictionary) {
                            String term = termEntries.getTerm();

                            int indexPrefix = annotation.getValue().substring(0, index + length).indexOf(term + " ");
                            if (indexPrefix > -1 && term.length() > 2) {
                                ContextAnnotation wrappedAnnotation2 = new ContextAnnotation(
                                        annotation.getStartPosition() + indexPrefix, term,
                                        termEntries.getMostLikelyCategory());
                                toAdd.add(wrappedAnnotation2);
                                LOGGER.debug("Add from prefix {}", wrappedAnnotation2.getValue());
                                break;
                            }
                        }
                        toRemove.add(annotation);

                        LOGGER.debug("Add {}, delete {} (left context: {})", wrappedAnnotation.getValue(),
                                annotation.getValue(), leftContext);
                        break;
                    }
                }
            }
        }

        LOGGER.debug("Add {} entities", toAdd.size());
        annotations.addAll(toAdd);

        LOGGER.debug("Remove {} entities", toRemove.size());
        annotations.removeAll(toRemove);

    }

    private void applyContextAnalysis(ContextAnnotation annotation) {
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();
        builder.add(annotation.getTags());
        FeatureSetting featureSetting = model.contextModel.getFeatureSetting();
        Scorer scorer = new ExperimentalScorers.CategoryEqualizationScorer();
        PalladianTextClassifier classifier = new PalladianTextClassifier(featureSetting, scorer);
        String context = annotation.getLeftContext() + "__" + annotation.getRightContext();
        if (context.trim().length() > 2) {
            CategoryEntries contextClassification = classifier.classify(context, model.contextModel);
            builder.add(contextClassification);
        }
        annotation.setTags(builder.create());
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
    static ContextAnnotation removeDateFragment(ContextAnnotation annotation) {
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
        return new ContextAnnotation(newOffset, newValue, annotation.getTag(), annotation.getLeftContext(),
                annotation.getRightContext());
    }

    /**
     * Build a set with left contexts. These are tokens which appear to the left of an entity, e.g.
     * "President Barack Obama". From the available annotations we determine, whether "President" belongs to the entity,
     * or to the context. This information can be used later, to fix the boundaries of an annotation.
     * 
     * @param annotations The annotations.
     * @return A set with tokens which appear more often in the context, than within an entity (e.g. "President").
     */
    private static Set<String> buildLeftContexts(Annotations<ContextAnnotation> annotations) {
        LOGGER.info("Building left contexts");
        Bag<String> leftContextCounts = Bag.create();
        Bag<String> insideAnnotationCounts = Bag.create();
        for (ContextAnnotation annotation : annotations) {
            leftContextCounts.addAll(annotation.getLeftContexts());
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

    private static DictionaryModel buildContextDictionary(Iterable<? extends ContextAnnotation> annotations) {
        LOGGER.info("Building context dictionary");
        PalladianTextClassifier contextClassifier = new PalladianTextClassifier(CONTEXT_FEATURE_SETTING);
        Iterable<Instance> instances = CollectionHelper.convert(annotations,
                new Function<ContextAnnotation, Instance>() {
                    @Override
                    public Instance compute(ContextAnnotation input) {
                        String context = input.getLeftContext() + "__" + input.getRightContext();
                        return new InstanceBuilder().setText(context).create(input.getTag());
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
    private Annotations<ContextAnnotation> unwrapAnnotations(Annotation annotation, List<ContextAnnotation> annotations) {
        Annotations<ContextAnnotation> unwrappedAnnotations = new Annotations<ContextAnnotation>();
        for (Annotation currentAnnotation : annotations) {
            String currentValue = currentAnnotation.getValue();
            String currentTag = currentAnnotation.getTag();
            unwrappedAnnotations.addAll(processUnwrap(annotation, currentValue, currentTag));
        }
        for (TermCategoryEntries categoryEntries : model.entityDictionary) {
            String term = categoryEntries.getTerm();
            String category = categoryEntries.getMostLikelyCategory();
            unwrappedAnnotations.addAll(processUnwrap(annotation, term, category));
        }
        unwrappedAnnotations.removeNested();
        if (unwrappedAnnotations.size() > 0) {
            LOGGER.debug("Unwrapped {} in {} parts", annotation.getValue(), unwrappedAnnotations.size());
        }
        return unwrappedAnnotations;
    }

    private static List<ContextAnnotation> processUnwrap(Annotation annotation, String value, String tag) {
        int currentLength = value.length();
        String entityName = annotation.getValue().toLowerCase();
        int entityLength = entityName.length();
        int entityStart = annotation.getStartPosition();
        List<ContextAnnotation> unwrappedAnnotations = CollectionHelper.newArrayList();
        if (currentLength > 2 && currentLength < entityLength) {
            int index = entityName.indexOf(" " + value.toLowerCase() + " ");
            if (index > -1) {
                unwrappedAnnotations.add(new ContextAnnotation(entityStart + index + 1, value, tag));
            }
            index = entityName.indexOf(value.toLowerCase() + " ");
            if (index == 0) {
                unwrappedAnnotations.add(new ContextAnnotation(entityStart + index, value, tag));
            }
            index = entityName.indexOf(" " + value.toLowerCase());
            if (index == entityLength - currentLength - 1) {
                unwrappedAnnotations.add(new ContextAnnotation(entityStart + index + 1, value, tag));
            }
        }
        return unwrappedAnnotations;
    }

    @Override
    public String getName() {
        return "Palladian NER (" + model.settings + ")";
    }

}
