package ws.palladian.extraction.entity.tagger;

import static ws.palladian.extraction.entity.tagger.PalladianNerSettings.LanguageMode.LanguageIndependent;

import java.io.IOException;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.DictionaryModel.TermCategoryEntries;
import ws.palladian.classification.text.DictionaryTrieModel;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.classification.text.PalladianTextClassifier;
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
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CollectionHelper.Order;
import ws.palladian.helper.constants.RegExp;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Function;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
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
 * <li>{@link TrainingMode#Complete}: You must have a tagged corpus in column format where the first colum is the token
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
    private static final FeatureSetting ANNOTATION_FEATURE_SETTING = FeatureSettingBuilder.chars(2, 8).create();

    /** be careful with the n-gram sizes, they heavily influence the model size. */
    private static final FeatureSetting CONTEXT_FEATURE_SETTING = FeatureSettingBuilder.chars(4, 5).create();

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

        Bag<String> leftContexts = Bag.create();

        // XXX this does not really seem to have an impact on the accuracy (there's already contextModel, see above).
        DictionaryModel patternProbabilities;

        Set<String> removeAnnotations = CollectionHelper.newHashSet();

        PalladianNerSettings settings;

        /**
         * @return The tags which are supported by this model.
         */
        public Set<String> getTags() {
            return entityDictionary.getCategories();
        }

    }

//    private final PalladianTextClassifier annotationClassifier = new PalladianTextClassifier(ANNOTATION_FEATURE_SETTING);

    private final PalladianTextClassifier contextClassifier = new PalladianTextClassifier(CONTEXT_FEATURE_SETTING);

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
     * <p>
     * Save the tagger to the specified file.
     * </p>
     * 
     * @param modelFilePath The file where the tagger should be saved to. You do not need to add the file ending but if
     *            you do, it should be "model.gz".
     */
    private void saveModel(String modelFilePath) {

        LOGGER.info("Entity dictionary contains {} entities", model.entityDictionary.getNumUniqTerms());
        // LOGGER.info("Case dictionary contains {} entities", model.caseDictionary.getNumUniqTerms());
        LOGGER.info("Dictionary size: {}", model.annotationModel.getNumUniqTerms());

        if (!modelFilePath.endsWith(getModelFileEnding())) {
            modelFilePath = modelFilePath + "." + getModelFileEnding();
        }
        LOGGER.info("Serializing Palladian NER to {}", modelFilePath);
        try {
            FileHelper.serialize(model, modelFilePath);
        } catch (IOException e) {
            throw new IllegalStateException("Error while serializing to \"" + modelFilePath + "\".", e);
        }
        LOGGER.info("All Palladian NER files written");
    }

    /**
     * Build a case dictionary.
     * 
     * @param token The tokens to add.
     * @return The dictionary model with categories <code>Aa</code>, <code>A</code>, and <code>a</code> for each token.
     */
    private static DictionaryModel buildCaseDictionary(List<String> tokens) {
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
//        for (Annotation annotation : annotations) {
//            model.entityDictionary.updateTerm(annotation.getValue(), annotation.getTag());
//        }
        if (model.settings.languageMode == LanguageIndependent) {
            return trainLanguageIndependent(trainingFilePath, modelFilePath, annotations);
        } else {
            return trainEnglish(trainingFilePath, modelFilePath, annotations);
        }
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
     * @return <tt>True</tt>, if all training worked, <tt>false</tt> otherwise.
     */
    public boolean train(Annotations<ContextAnnotation> annotations, String modelFilePath) {
        return trainLanguageIndependent(annotations, annotations, modelFilePath);
    }

    private boolean trainLanguageIndependent(Annotations<ContextAnnotation> annotations,
            Annotations<ContextAnnotation> combinedAnnotations, String modelFilePath) {

        List<Instance> textInstances = CollectionHelper.newArrayList();

        LOGGER.info("Start creating {} annotations for training", annotations.size());
        for (Annotation annotation : annotations) {
            textInstances.add(new InstanceBuilder().setText(annotation.getValue()).create(annotation.getTag()));
        }

        // save training entities in a dedicated dictionary
        model.entityDictionary = buildEntityDictionary(combinedAnnotations);
        
        model.annotationModel = buildAnnotationDictionary(annotations);

//        trainAnnotationClassifier(textInstances);
        saveModel(modelFilePath);
        return true;
    }

    private static DictionaryModel buildEntityDictionary(Iterable<? extends Annotation> annotations) {
        DictionaryTrieModel.Builder entityDictionaryBuilder = new DictionaryTrieModel.Builder();
        for (Annotation annotation : annotations) {
            entityDictionaryBuilder.addDocument(Collections.singleton(annotation.getValue()), annotation.getTag());
        }
        return entityDictionaryBuilder.create();
    }

    private static DictionaryModel buildAnnotationDictionary(Iterable<? extends Annotation> annotations) {
        DictionaryTrieModel.Builder builder = new DictionaryTrieModel.Builder();
        PalladianTextClassifier textClassifier = new PalladianTextClassifier(ANNOTATION_FEATURE_SETTING, builder);
        Iterable<Instance> instances = CollectionHelper.convert(annotations, new Function<Annotation,Instance>() {
            @Override
            public Instance compute(Annotation input) {
                return new InstanceBuilder().setText(input.getValue()).create(input.getTag());
            }
        });
        return textClassifier.train(instances);
    }

//    private void trainAnnotationClassifier(List<Instance> textInstances) {
//        LOGGER.debug("Start training classifiers now...");
//        model.annotationModel = annotationClassifier.train(textInstances);
//    }

    /**
     * <p>
     * Train the tagger in language independent mode.
     * </p>
     * 
     * @param trainingFilePath The apther of the training file.
     * @param modelFilePath The path where the model should be saved to.
     * @param additionalTrainingAnnotations Additional annotations that can be used for training.
     * @return <tt>True</tt>, if all training worked, <tt>false</tt> otherwise.
     */
    private boolean trainLanguageIndependent(String trainingFilePath, String modelFilePath,
            List<? extends Annotation> additionalTrainingAnnotations) {

        // get all training annotations
        Annotations<ContextAnnotation> annotations = FileFormatParser
                .getAnnotationsFromColumnTokenBased(trainingFilePath);

        // get annotations combined, e.g. "Phil Simmons", not "Phil" and "Simmons"
        Annotations<ContextAnnotation> combinedAnnotations = FileFormatParser
                .getAnnotationsFromColumn(trainingFilePath);

        // add the additional training annotations, they will be used for the context analysis too
        for (Annotation annotation : additionalTrainingAnnotations) {
            ContextAnnotation contextAnnotation = new ContextAnnotation(annotation);
            annotations.add(contextAnnotation);
            combinedAnnotations.add(contextAnnotation);
        }

        analyzeContexts(trainingFilePath);

        return trainLanguageIndependent(annotations, combinedAnnotations, modelFilePath);
    }

    /**
     * <p>
     * Train the tagger in English mode.
     * </p>
     * 
     * @param trainingFilePath The path of the training file.
     * @param modelFilePath The path where the model should be saved to.
     * @param additionalTrainingAnnotations Additional annotations that can be used for training.
     * @return <tt>True</tt>, if all training worked, <tt>false</tt> otherwise.
     */
    private boolean trainEnglish(String trainingFilePath, String modelFilePath,
            List<? extends Annotation> additionalTrainingAnnotations) {

        // get all training annotations
        LOGGER.info("Get annotations from column-formatted training file");
        Annotations<ContextAnnotation> annotations = FileFormatParser.getAnnotationsFromColumn(trainingFilePath);

        // add the additional training annotations, they will be used for the context analysis too
        for (Annotation annotation : additionalTrainingAnnotations) {
            annotations.add(new ContextAnnotation(annotation));
        }
        LOGGER.info("Add {} additional training annotations", additionalTrainingAnnotations.size());
        
        model.entityDictionary = buildEntityDictionary(annotations);

        List<Instance> textInstances = CollectionHelper.newArrayList();
        for (ContextAnnotation annotation : annotations) {
            textInstances.add(new InstanceBuilder().setText(annotation.getValue()).create(annotation.getTag()));
            // model.entityDictionary.updateTerm(annotation.getValue(), annotation.getTag());
        }

        List<String> tokens = Tokenizer.tokenize(FileFormatParser.getText(trainingFilePath, TaggingFormat.COLUMN));
        model.caseDictionary = buildCaseDictionary(tokens);

        // in complete training mode, the tagger is learned twice on the training data
        if (model.settings.retraining()) {
            LOGGER.info("Start retraining (because of complete dataset, no sparse annotations)");

            // //////////////////////////////////////////// wrong entities //////////////////////////////////////
//            trainAnnotationClassifier(textInstances);
            model.annotationModel = buildAnnotationDictionary(annotations);

            model.removeAnnotations.clear();
            EvaluationResult evaluationResult = evaluate(trainingFilePath, TaggingFormat.COLUMN);
            Annotations<ContextAnnotation> goldStandard = FileFormatParser.getAnnotations(trainingFilePath,
                    TaggingFormat.COLUMN);
            goldStandard.sort();

            // get only those annotations that were incorrectly tagged and were never a real entity that is they have to
            // be in ERROR1 set and NOT in the gold standard
            for (Annotation wrongAnnotation : evaluationResult.getAnnotations(ResultType.ERROR1)) {

//                textInstances.add(new InstanceBuilder().setText(wrongAnnotation.getValue()).create(NO_ENTITY));
                // FIXME
                annotations.add(new ContextAnnotation(wrongAnnotation.getStartPosition(), wrongAnnotation.getValue(), NO_ENTITY));

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
                    model.removeAnnotations.add(wrongAnnotation.getValue());
                }
            }
            LOGGER.info("{} annotations need to be completely removed", model.removeAnnotations.size());
            // //////////////////////////////////////////////////////////////////////////////////////////////////
        }

//        trainAnnotationClassifier(textInstances);
        model.annotationModel = buildAnnotationDictionary(annotations);
        analyzeContexts(trainingFilePath);
        saveModel(modelFilePath);
        return true;
    }

    private static boolean hasAssignedType(CategoryEntries entries) {
        return !NO_ENTITY.equalsIgnoreCase(entries.getMostLikelyCategory());
    }

    /**
     * Classify candidate annotations in English mode.
     * 
     * @param entityCandidates The annotations to be classified.
     * @return Classified annotations.
     */
    private Annotations<ContextAnnotation> classifyCandidates(List<ContextAnnotation> entityCandidates) {
        PalladianTextClassifier annotationClassifier = new PalladianTextClassifier(model.annotationModel.getFeatureSetting());
        
        Annotations<ContextAnnotation> annotations = new Annotations<ContextAnnotation>();
        for (ContextAnnotation annotation : entityCandidates) {

            List<ContextAnnotation> wrappedAnnotations = new Annotations<ContextAnnotation>();

            if (model.settings.unwrapEntities()) {
                wrappedAnnotations = unwrapAnnotations(annotation, annotations);
            }

            if (wrappedAnnotations.isEmpty()) {
                CategoryEntries results = annotationClassifier.classify(annotation.getValue(), model.annotationModel);
                if (hasAssignedType(results)) {
                    annotation.setTags(results);
                    annotations.add(annotation);
                }
            } else {
                for (ContextAnnotation annotation2 : wrappedAnnotations) {
                    if (hasAssignedType(annotation2.getTags())) {
                        annotations.add(annotation2);
                    }
                }
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
        if (model.settings.tagUrls) {
            annotations.addAll(UrlTagger.INSTANCE.getAnnotations(inputText));
        }

        // recognize and add dates, remove annotations that were part of a date
        if (model.settings.tagDates) {
            annotations.addAll(new DateAndTimeTagger().getAnnotations(inputText));
        }

        annotations.removeNested();
        annotations.sort();

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

        StopWatch stopWatch = new StopWatch();

        // switch using pattern information
        int changed = 0;
        if (model.settings.switchTagAnnotationsUsingPatterns()) {
            stopWatch.start();

            for (ContextAnnotation annotation : annotations) {

                String tagNameBefore = annotation.getTag();

                applyContextAnalysis(annotation);

                if (!annotation.getTag().equalsIgnoreCase(tagNameBefore)) {
                    LOGGER.debug("Changed {} from {} to {}, context: {} __ {}", annotation.getValue(), tagNameBefore,
                            annotation.getTag(), annotation.getLeftContext(), annotation.getRightContext());
                    changed++;
                }

            }
            LOGGER.debug("Changed {}% of the entities using patterns in {}",
                    MathHelper.round(100 * changed / (annotations.size() + 0.000000000001), 2), stopWatch);

        }

        // switch annotations that are in the dictionary
        changed = 0;
        if (model.settings.switchTagAnnotationsUsingDictionary()) {
            stopWatch.start();

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
            LOGGER.debug("Changed with entity dictionary {} % of the entities (total entities: {}) in {}",
                    MathHelper.round(100 * changed / (annotations.size() + 0.000000000001), 2), annotations.size(),
                    stopWatch);
        }

    }

    private Annotations<ContextAnnotation> getAnnotationsInternal(String inputText) {

        Annotations<ContextAnnotation> annotations;

        if (model.settings.languageMode == LanguageIndependent) {
            // get the candidates, every token is potentially a (part of) an entity
            annotations = StringTagger.getTaggedEntities(inputText, Tokenizer.TOKEN_SPLIT_REGEX);
        } else {
            // use the the string tagger to tag entities in English mode
            annotations = StringTagger.getTaggedEntities(inputText);
        }
        
        preProcessAnnotations(annotations);

        annotations = classifyCandidates(annotations);
        // CollectionHelper.print(annotations);

        postProcessAnnotations(annotations);

        if (model.settings.languageMode == LanguageIndependent) {

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
        if (model.settings.removeDates()) {
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
        if (model.settings.removeDateFragments()) {
            stopWatch.start();
            int c = 0;
            for (ContextAnnotation annotation : annotations) {

                Pair<String, Integer> result = removeDateFragment(annotation.getValue());
                String entity = result.getLeft();

                annotation.setValue(entity);
                annotation.setStartPosition(annotation.getStartPosition() + result.getRight());

                if (result.getRight() > 0) {
                    c++;
                }
            }
            LOGGER.debug("Removed {} partial date annotations in {}", c, stopWatch);
        }
        
        // remove annotations that were found to be incorrectly tagged in the training data
        if (model.settings.removeIncorrectlyTaggedInTraining()) {
            stopWatch.start();
            for (String removeAnnotation : model.removeAnnotations) {
                for (ContextAnnotation annotation : annotations) {
                    if (removeAnnotation.equalsIgnoreCase(annotation.getValue())) {
                        toRemove.add(annotation);
                    }
                }
            }
            LOGGER.debug("Removed {} incorrectly tagged entities in training data in {}",
                    model.removeAnnotations.size(), stopWatch);
        }

        // use a learned case dictionary to remove possibly incorrectly tagged sentence starts. For example ". This" is
        // removed since "this" is usually spelled using lowercase characters only. This is done NOT only for words at
        // sentence start but all single token words.
        int c = 0;
        if (model.settings.removeSentenceStartErrorsCaseDictionary()) {
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

        for (ContextAnnotation annotation : annotations) {

            if (model.settings.unwrapEntitiesWithContext()) {
                Bag<String> sortedMap = model.leftContexts.createSorted(Order.DESCENDING);
                for (Entry<String, Integer> leftContextEntry : sortedMap.unique()) {

                    String leftContext = leftContextEntry.getKey();

                    // 0 means the context appears more often inside an entity than outside so we should not delete it
                    if (leftContextEntry.getValue() == 0) {
                        // the map is sorted by number of occurrences so we can break as soon as the threshold is
                        // reached
                        break;
                    }

                    if (!StringHelper.startsUppercase(leftContext)) {
                        continue;
                    }

                    String entity = annotation.getValue();

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
                    if (index1 == 0 || index2 > -1) {

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

                        LOGGER.debug("Add {}, delete {} (left context: {}, {})", wrappedAnnotation.getValue(),
                                annotation.getValue(), leftContext, leftContextEntry.getValue());
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
        List<String> contexts = CollectionHelper.newArrayList();
        contexts.addAll(annotation.getLeftContexts());
        contexts.addAll(annotation.getRightContexts());
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();
        builder.add(annotation.getTags());
        if (model.patternProbabilities != null) {
            for (String contextPattern : contexts) {
                if (contextPattern.length() > 0) {
                    TermCategoryEntries patternClassificaiton = model.patternProbabilities
                            .getCategoryEntries(contextPattern.toLowerCase());
                    builder.add(patternClassificaiton);
                }
            }
        }

        if (model.contextModel != null) {
            String context = annotation.getLeftContext() + "__" + annotation.getRightContext();
            CategoryEntries contextClassificaiton = contextClassifier.classify(context, model.contextModel);
            builder.add(contextClassificaiton);
        }
        
        CategoryEntries result = builder.create();
//        LOGGER.debug("{} with context: {} -> {}", annotation.getValue(), annotation.getTag(), result.getMostLikelyCategory());
        annotation.setTags(result);
    }

    /**
     * Check whether the given text is a date fragment, e.g. "June".
     * 
     * @param text The text to check for date fragments.
     * @return <code>true</code> in case the text contained a date fragment.
     */
    static boolean isDateFragment(String text) {
        for (String regExp : RegExp.DATE_FRAGMENTS) {
            if (text.toLowerCase().replaceAll(regExp.toLowerCase(), "").trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * Remove date fragments from the given text.
     * </p>
     * 
     * @param text The text to be cleansed of date fragments.
     * @return An object array containing the new cleansed text on position 0 and the offset which was caused by the
     *         removal on position 1.
     */
    static Pair<String, Integer> removeDateFragment(String text) {
        int offsetChange = 0;
        for (String dateFragment : RegExp.DATE_FRAGMENTS) {
            String regExp = "(?:" + dateFragment + ")\\.?";
            String beginRegExp = "^" + regExp + " ";
            String endRegExp = " " + regExp + "$";
            int textLength = text.length();
            if (StringHelper.countRegexMatches(text, beginRegExp) > 0) {
                text = text.replaceAll(beginRegExp, "").trim();
                offsetChange += textLength - text.length();
            }
            if (StringHelper.countRegexMatches(text, endRegExp) > 0) {
                text = text.replaceAll(endRegExp, "").trim();
            }
        }
        return Pair.of(text, offsetChange);
    }

    /**
     * <p>
     * Analyze the context around the annotations. The context classifier will be trained and left context patterns will
     * be stored.
     * </p>
     * 
     * @param trainingFilePath The path to the training data.
     */
    private void analyzeContexts(String trainingFilePath) {

        LOGGER.debug("Start analyzing contexts");

        Map<String, Bag<String>> contextMap = new TreeMap<String, Bag<String>>();
        Bag<String> leftContextMapCountMap = Bag.create();

        // get all training annotations including their features
        Annotations<ContextAnnotation> annotations = FileFormatParser.getAnnotationsFromColumn(trainingFilePath);

        List<Instance> trainingInstances = CollectionHelper.newArrayList();

        // iterate over all annotations and analyze their left and right contexts for patterns
        for (ContextAnnotation annotation : annotations) {

            String tag = annotation.getTag();

            if (contextMap.get(tag) == null) {
                contextMap.put(tag, Bag.<String> create());
            }

            contextMap.get(tag).addAll(annotation.getLeftContexts());
            contextMap.get(tag).addAll(annotation.getRightContexts());
            leftContextMapCountMap.addAll(annotation.getLeftContexts());

            String text = annotation.getLeftContext() + "__" + annotation.getRightContext();
            trainingInstances.add(new InstanceBuilder().setText(text).create(tag));
        }

        // fill the leftContextMap with the context and the ratio of inside annotation / outside annotation
        
        ProgressMonitor monitor = new ProgressMonitor();
        monitor.startTask(null, leftContextMapCountMap.unique().size());

        for (Entry<String, Integer> entry : leftContextMapCountMap.unique()) {
            
            monitor.increment();

            
            String leftContext = entry.getKey();
            int outside = entry.getValue();
            int inside = 0;

            for (ContextAnnotation annotation : annotations) {
                if (annotation.getValue().startsWith(leftContext + " ") || annotation.getValue().equals(leftContext)) {
                    inside++;
                }
            }

            double ratio = (double)inside / outside;
            boolean value = ratio >= 1 || outside < 2;
            model.leftContexts.add(leftContext, value ? 0 : 1);
        }

        model.contextModel = contextClassifier.train(trainingInstances);
        
        DictionaryTrieModel.Builder patternProbabilitiesBuilder = new DictionaryTrieModel.Builder();

        // tagMap to matrix
        for (Entry<String, Bag<String>> patternEntry : contextMap.entrySet()) {

            for (Entry<String, Integer> tagEntry : patternEntry.getValue().unique()) {
                String pattern = tagEntry.getKey().toLowerCase();
                String tag = patternEntry.getKey();
                if (StringUtils.isNotBlank(tag) && StringUtils.isNotBlank(pattern)) {
                    int count = tagEntry.getValue();
                    patternProbabilitiesBuilder.addDocument(Collections.singleton(pattern), tag, count);
                }
            }
        }
        
        model.patternProbabilities = patternProbabilitiesBuilder.create();
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

        boolean isAllUppercase = StringHelper.isCompletelyUppercase(annotation.getValue());

        if (!isAllUppercase) {
            return unwrappedAnnotations;
        }

        String entityName = annotation.getValue().toLowerCase();
        int length = entityName.length();
        int start = annotation.getStartPosition();

        for (Annotation currentAnnotation : annotations) {
            String currentValue = currentAnnotation.getValue();
            int currentLength = currentValue.length();
            if (currentLength < length) {
                int index = entityName.indexOf(" " + currentValue.toLowerCase() + " ");
                String currentTag = currentAnnotation.getTag();
                if (index > -1 && currentLength > 2) {
                    unwrappedAnnotations.add(new ContextAnnotation(start + index + 1, currentValue, currentTag));
                }
                index = entityName.indexOf(currentValue.toLowerCase() + " ");
                if (index == 0 && currentLength > 2) {
                    unwrappedAnnotations.add(new ContextAnnotation(start + index, currentValue, currentTag));
                }
                index = entityName.indexOf(" " + currentValue.toLowerCase());
                if (index == length - currentLength - 1 && currentLength > 2) {
                    unwrappedAnnotations.add(new ContextAnnotation(start + index + 1, currentValue, currentTag));
                }
            }
        }

        // go through the entity dictionary
        for (TermCategoryEntries categoryEntries : model.entityDictionary) {
            String term = categoryEntries.getTerm();
            int termLength = term.length();
            if (termLength < length) {
                int index = entityName.indexOf(" " + term.toLowerCase() + " ");
                String category = categoryEntries.getMostLikelyCategory();
                if (index > -1 && termLength > 2) {
                    unwrappedAnnotations.add(new ContextAnnotation(start + index + 1, term, category));
                }
                index = entityName.indexOf(term.toLowerCase() + " ");
                if (index == 0 && termLength > 2) {
                    unwrappedAnnotations.add(new ContextAnnotation(start + index, term, category));
                }
                index = entityName.indexOf(" " + term.toLowerCase());
                if (index == entityName.length() - termLength - 1 && termLength > 2) {
                    unwrappedAnnotations.add(new ContextAnnotation(start + index + 1, term, category));
                }
            }
        }
        unwrappedAnnotations.removeNested();
        if (unwrappedAnnotations.size() > 0) {
            LOGGER.debug("Unwrapped {} to {}", annotation.getValue(), unwrappedAnnotations.size());
        }
        return unwrappedAnnotations;
    }

    @Override
    public String getName() {
        return "Palladian NER (" + model.settings + ")";
    }

}
