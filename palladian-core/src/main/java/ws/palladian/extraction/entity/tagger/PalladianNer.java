package ws.palladian.extraction.entity.tagger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSetting.TextFeatureType;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.extraction.entity.Annotation;
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
import ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.collection.CountMatrix;
import ws.palladian.helper.constants.RegExp;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.ClassifiedTextDocument;
import ws.palladian.processing.features.Annotated;

/**
 * <p>
 * This is the Named Entity Recognizer from Palladian. It is based on rule-based entity delimination (for English
 * texts), a text classification approach, and analyzes the contexts around annotations. The major different to other
 * NERs is that it can be learned on seed entities (just the names) or classically using supervised learning on a tagged
 * dataset.
 * </p>
 * 
 * <p>
 * Palladian NER provides two language modes:
 * <ol>
 * <li>TUDLI => token-based, language independent, that is you can learn any language, the performance is rather poor
 * though. Consider using another recognizer.</li>
 * <li>TUDEng => NED + NEC, English only, this recognizer has shown to reach similar performance on the CoNLL 2003
 * dataset as the state-of-the-art. It works on English texts only.</li>
 * </p>
 * 
 * <p>
 * Palladian NER provides two learning modes:
 * <ol>
 * <li>Complete => you must have a tagged corpus in column format where the first colum is the token and the second
 * column (separated by a tabstop) is the entity type.</li>
 * <li>Sparse => you just need a set of seed entities per concept (the same number per concept is preferred) and you can
 * learn a sparse training file with the {@link DatasetCreator} to learn on. Alternatively you can also learn on the
 * seed entities alone but no context information can be learned which results in a slightly worse performance.</li>
 * </p>
 * 
 * <p>
 * Parameters for performance tuning:
 * <ul>
 * <li>n-gram size of the entity classifier (2-8 seems good)</li>
 * <li>n-gram size of the context classifier (4-6 seems good)</li>
 * <li>window size of the Annotation: {@link Annotation.WINDOW_SIZE}</li>
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class PalladianNer extends TrainableNamedEntityRecognizer implements Serializable {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianNer.class);

    /** The serial version id. */
    private static final long serialVersionUID = -8793232373094322955L;

    private transient PalladianTextClassifier entityClassifier;

    private transient PalladianTextClassifier contextClassifier;

    /** This dictionary contains the entity terms as they are. */
    private DictionaryModel entityDictionary;

    /** A list containing the order of likelihood of the concepts. */
    private List<String> conceptLikelihoodOrder = new ArrayList<String>();

    /** This dictionary contains the n-grams of the entity terms, create by the text classifier. */
    private DictionaryModel annotationModel;

    // use a context classifier for the left and right context around the annotations
    private DictionaryModel contextModel;

    private DictionaryModel caseDictionary;

    private CountMap<String> leftContextMap = CountMap.create();

    private CountMatrix<String> patternProbabilityMatrix = CountMatrix.create();

    private List<String> removeAnnotations = CollectionHelper.newArrayList();

    // learning features
    private boolean removeDates = true;
    private boolean removeDateEntries = true;
    private boolean removeIncorrectlyTaggedInTraining = true;
    private boolean removeSentenceStartErrorsCaseDictionary = false;
    private boolean switchTagAnnotationsUsingPatterns = true;
    private boolean switchTagAnnotationsUsingDictionary = true;
    private boolean unwrapEntities = true;
    private boolean unwrapEntitiesWithContext = true;
    private final boolean retraining = true;

    /** Whether the tagger should tag URLs. */
    private boolean tagUrls = true;

    /** Whether the tagger should tag dates. */
    private boolean tagDates = true;

    private final static String NO_ENTITY = "###NO_ENTITY###";

    /**
     * The language mode, language independent uses more generic regexp to detect entities, while there are more
     * specific ones for English texts.
     * 
     * @author David Urbansky
     * 
     */
    public enum LanguageMode {
        LanguageIndependent, English
    }

    /**
     * The two possible learning modes. Complete requires fully tagged data, sparse needs only some entities tagged in
     * the training file.
     * 
     * @author David Urbansky
     * 
     */
    public enum TrainingMode {
        Complete, Sparse
    }

    private static final LanguageMode DEFAULT_LANGUAGE_MODE = LanguageMode.English;

    private static final TrainingMode DEFAULT_TRAINING_MODE = TrainingMode.Complete;

    /** The language mode. */
    private final LanguageMode languageMode;

    /** The training mode. */
    private final TrainingMode trainingMode;

    // /////////////////// Constructors /////////////////////
    public PalladianNer(LanguageMode languageMode, TrainingMode trainingMode) {
        Validate.notNull(languageMode, "languageMode must not be null");
        Validate.notNull(trainingMode, "trainingMode must not be null");

        // hold entities in a dictionary that are learned from the training data
        entityDictionary = new DictionaryModel(null);

        // keep the case dictionary from the training data
        caseDictionary = new DictionaryModel(null);

        // the n-gram settings for the entity classifier should be tuned, they do not have a big influence on the size
        // of the model (3-5 to 2-8 => 2MB)
        entityClassifier = new PalladianTextClassifier(new FeatureSetting(TextFeatureType.CHAR_NGRAMS, 2, 8));

        // be careful with the n-gram sizes, they heavily influence the model size
        contextClassifier = new PalladianTextClassifier(new FeatureSetting(TextFeatureType.CHAR_NGRAMS, 4, 5));

        conceptLikelihoodOrder = CollectionHelper.newArrayList();

        // with entity 2-8 and context 4-7: 173MB model
        // precision MUC: 79.93%, recall MUC: 85.55%, F1 MUC: 82.64%
        // precision exact: 70.66%, recall exact: 75.63%, F1 exact: 73.06%

        // with entity 3-5 and context 4-5: 25MB model
        // with entity 3-5 and context 4-6: 43MB model
        // precision MUC: 74.94%, recall MUC: 80.58%, F1 MUC: 77.66%
        // precision exact: 62.08%, recall exact: 66.76%, F1 exact: 64.34%
        // with entity 2-8 and context 4-6: 45MB model
        // precision MUC: 75.09%, recall MUC: 81.12%, F1 MUC: 77.98%
        // precision exact: 62.39%, recall exact: 67.4%, F1 exact: 64.8%
        // with entity 2-8 and context 2-6: 46MB model
        // precision MUC: 74.71%, recall MUC: 80.71%, F1 MUC: 77.59%
        // precision exact: 61.68%, recall exact: 66.64%, F1 exact: 64.06%
        // with entity 2-8 and context 4-7: 66MB model
        // precision MUC: 75.04%, recall MUC: 81.06%, F1 MUC: 77.93%
        // precision exact: 62.33%, recall exact: 67.33%, F1 exact: 64.73%
        // with entity 2-8 and context 4-5: 29MB model
        // precision MUC: 75.05%, recall MUC: 81.08%, F1 MUC: 77.95%
        // precision exact: 62.36%, recall exact: 67.37%, F1 exact: 64.77%
        // with entity 2-8 and context 4-5, window size 40 was 120 in previous tests: 23MB model
        // precision MUC: 75.17%, recall MUC: 81.2%, F1 MUC: 78.07%
        // precision exact: 62.54%, recall exact: 67.56%, F1 exact: 64.95%

        this.languageMode = languageMode;
        this.trainingMode = trainingMode;
    }

    public PalladianNer(LanguageMode languageMode) {
        this(languageMode, DEFAULT_TRAINING_MODE);
    }

    public PalladianNer(TrainingMode trainingMode) {
        this(DEFAULT_LANGUAGE_MODE, trainingMode);
    }

    public PalladianNer() {
        this(DEFAULT_LANGUAGE_MODE, DEFAULT_TRAINING_MODE);
    }

    // //////////////////////////////////////////////////////

    public static String getModelFileEndingStatic() {
        return "model.gz";
    }

    @Override
    public String getModelFileEnding() {
        return getModelFileEndingStatic();
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        return false;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        StopWatch stopWatch = new StopWatch();

        if (!configModelFilePath.endsWith(getModelFileEnding())) {
            configModelFilePath += "." + getModelFileEnding();
        }

        // set current variables null to save memory otherwise we have those things twice in memory when deserializing
        this.entityDictionary = null;
        this.caseDictionary = null;
        this.leftContextMap = null;
        this.patternProbabilityMatrix = null;
        this.removeAnnotations = null;

        PalladianNer n = (PalladianNer)FileHelper.deserialize(configModelFilePath);

        // assign all properties from the loaded model to the current instance
        this.entityDictionary = n.entityDictionary;
        this.conceptLikelihoodOrder = n.conceptLikelihoodOrder;
        this.annotationModel = n.annotationModel;
        this.caseDictionary = n.caseDictionary;
        this.leftContextMap = n.leftContextMap;
        this.contextModel = n.contextModel;
        this.patternProbabilityMatrix = n.patternProbabilityMatrix;
        this.removeAnnotations = n.removeAnnotations;

        // assign the learning features
        this.removeDates = n.removeDates;
        this.removeDateEntries = n.removeDateEntries;
        this.removeIncorrectlyTaggedInTraining = n.removeIncorrectlyTaggedInTraining;
        this.removeSentenceStartErrorsCaseDictionary = n.removeSentenceStartErrorsCaseDictionary;
        this.switchTagAnnotationsUsingPatterns = n.switchTagAnnotationsUsingPatterns;
        this.switchTagAnnotationsUsingDictionary = n.switchTagAnnotationsUsingDictionary;
        this.unwrapEntities = n.unwrapEntities;
        this.unwrapEntitiesWithContext = n.unwrapEntitiesWithContext;

        // assign tagging options
        this.tagDates = n.tagDates;
        this.tagUrls = n.tagUrls;

        LOGGER.info("model " + configModelFilePath + " successfully loaded in " + stopWatch.getElapsedTimeString());

        return true;
    }

    /**
     * Load a complete tagger from disk.
     * 
     * @param modelPath The path of the tagger.
     * @return The tagger instance.
     */
    public static PalladianNer load(String modelPath) {
        StopWatch stopWatch = new StopWatch();

        LOGGER.info("deserialzing model from " + modelPath);

        if (!modelPath.endsWith(getModelFileEndingStatic())) {
            modelPath += "." + getModelFileEndingStatic();
        }

        PalladianNer tagger = (PalladianNer)FileHelper.deserialize(modelPath);
        LOGGER.info("loaded tagger successfully in " + stopWatch.getElapsedTimeString());

        return tagger;
    }

    /**
     * Save the tagger to the specified file.
     * 
     * @param modelFilePath The file where the tagger should be saved to. You do not need to add the file ending but if
     *            you do, it should be "model.gz".
     */
    protected void saveModel(String modelFilePath) {

        LOGGER.info("entity dictionary contains " + entityDictionary.getNumTerms() + " entities");
        // entityDictionary.saveAsCSV();

        LOGGER.info("case dictionary contains " + caseDictionary.getNumTerms() + " entities");
        // caseDictionary.saveAsCSV();

        LOGGER.info("serializing Palladian NER to " + modelFilePath);
        if (!modelFilePath.endsWith(getModelFileEnding())) {
            modelFilePath = modelFilePath + "." + getModelFileEnding();
        }
        FileHelper.serialize(this, modelFilePath);

        LOGGER.info("dictionary size: " + annotationModel.getNumTerms());

        // write model meta information
        LOGGER.info("write model meta information");
        StringBuilder supportedConcepts = new StringBuilder();
        for (String c : annotationModel.getCategories()) {
            supportedConcepts.append(c).append("\n");
        }
        FileHelper.writeToFile(FileHelper.getFilePath(modelFilePath) + FileHelper.getFileName(modelFilePath)
                + "_meta.txt", supportedConcepts);

        LOGGER.info("all Palladian NER files written");
    }

    /**
     * Save training entities in a dedicated dictionary.
     * 
     * @param annotation The complete annotation from the training data.
     */
    private void addToEntityDictionary(Annotated annotation) {
        addToEntityDictionary(annotation.getValue(), annotation.getTag());
    }

    private void addToEntityDictionary(String entity, String concept) {
        entityDictionary.updateTerm(entity, concept);
    }

    /**
     * Add a token to the case dictionary.
     * 
     * @param token The token to add.
     */
    private void addToCaseDictionary(String token) {
        token = StringHelper.trim(token);
        if (token.length() < 2) {
            return;
        }
        String caseSignature = StringHelper.getCaseSignature(token);
        if (caseSignature.equals("Aa") || caseSignature.equals("A") || caseSignature.equals("a")) {
            caseDictionary.updateTerm(token.toLowerCase(), caseSignature);
        }
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
    public boolean train(String trainingFilePath, List<? extends Annotated> annotations, String modelFilePath) {

        LOGGER.info("Start creating {} annotations for training", annotations.size());

        // save training entities in a dedicated dictionary
        for (Annotated annotation : annotations) {
            addToEntityDictionary(annotation);
        }

        if (languageMode.equals(LanguageMode.English)) {
            return trainEnglish(trainingFilePath, modelFilePath, annotations);
        } else {
            return trainLanguageIndependent(trainingFilePath, modelFilePath);
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
        this.entityDictionary = new DictionaryModel(null);

        StopWatch stopWatch = new StopWatch();
        List<String> dictionaryEntries = FileHelper.readFileToArray(filePath);

        int i = 1;
        for (String dictionaryEntry : dictionaryEntries) {

            // fill the likelihood list
            if (i == 1) {
                conceptLikelihoodOrder = CollectionHelper.newArrayList();
                conceptLikelihoodOrder.addAll(Arrays.asList(dictionaryEntry.split("\\>")));
                i++;
                continue;
            }

            String[] split = dictionaryEntry.split("###");
            if (split.length < 2) {
                continue;
            }
            addToEntityDictionary(split[1], split[0]);
            ProgressHelper.printProgress(i++, dictionaryEntries.size(), 1, stopWatch);
        }

        LOGGER.info("Added {} entities to the dictionary in {}", i - 2, stopWatch.getElapsedTimeString());
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

    public boolean trainLanguageIndependent(Annotations<ContextAnnotation> annotations,
            Annotations<ContextAnnotation> combinedAnnotations, String modelFilePath) {

        List<ClassifiedTextDocument> textInstances = CollectionHelper.newArrayList();

        LOGGER.info("Start creating {} annotations for training", annotations.size());
        for (Annotated annotation : annotations) {
            ClassifiedTextDocument document = new ClassifiedTextDocument(annotation.getTag(), annotation.getValue());
            textInstances.add(document);
        }

        // save training entities in a dedicated dictionary
        for (Annotated annotation : combinedAnnotations) {
            addToEntityDictionary(annotation);
        }

        // train the text classifier
        trainAnnotationClassifier(textInstances);

        saveModel(modelFilePath);

        return true;
    }

    private void trainAnnotationClassifier(List<ClassifiedTextDocument> textInstances) {
        LOGGER.info("start training classifiers now...");
        annotationModel = entityClassifier.train(textInstances);
    }

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
            List<ContextAnnotation> additionalTrainingAnnotations) {

        // get all training annotations
        Annotations<ContextAnnotation> annotations = FileFormatParser
                .getAnnotationsFromColumnTokenBased(trainingFilePath);

        // get annotations combined, e.g. "Phil Simmons", not "Phil" and "Simmons"
        Annotations<ContextAnnotation> combinedAnnotations = FileFormatParser
                .getAnnotationsFromColumn(trainingFilePath);

        // add the additional training annotations, they will be used for the context analysis too
        annotations.addAll(additionalTrainingAnnotations);
        combinedAnnotations.addAll(additionalTrainingAnnotations);

        analyzeContexts(trainingFilePath, annotations);

        return trainLanguageIndependent(annotations, combinedAnnotations, modelFilePath);
    }

    private boolean trainLanguageIndependent(String trainingFilePath, String modelFilePath) {
        return trainLanguageIndependent(trainingFilePath, modelFilePath, Collections.<ContextAnnotation> emptyList());
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
            List<? extends Annotated> additionalTrainingAnnotations) {

        // get all training annotations
        LOGGER.info("get annotations from column-formatted training file");
        Annotations<ContextAnnotation> annotations = FileFormatParser.getAnnotationsFromColumn(trainingFilePath);

        // add the additional training annotations, they will be used for the context analysis too
        // annotations.addAll(additionalTrainingAnnotations);
        for (Annotated annotated : additionalTrainingAnnotations) {
            annotations.add(new ContextAnnotation(annotated));
        }

        // create instances with nominal and numeric features
        List<ClassifiedTextDocument> textInstances = CollectionHelper.newArrayList();

        LOGGER.info("add additional training annotations");
        int c = 1;
        for (ContextAnnotation annotation : annotations) {
            ClassifiedTextDocument textInstance = new ClassifiedTextDocument(annotation.getTag(), annotation.getValue());
            textInstances.add(textInstance);
            addToEntityDictionary(annotation);
            ProgressHelper.printProgress(c++, annotations.size(), 1);
        }
        LOGGER.info("add {} additional training annotations", c);

        // fill the case dictionary
        List<String> tokens = Tokenizer.tokenize(FileFormatParser.getText(trainingFilePath, TaggingFormat.COLUMN));
        for (String token : tokens) {
            addToCaseDictionary(token);
        }

        // in complete training mode, the tagger is learned twice on the training data
        if (retraining) {
            LOGGER.info("start retraining (because of complete dataset, no sparse annotations)");

            // //////////////////////////////////////////// wrong entities //////////////////////////////////////
            trainAnnotationClassifier(textInstances);
            saveModel(modelFilePath);

            removeAnnotations.clear();
            EvaluationResult evaluationResult = evaluate(trainingFilePath, TaggingFormat.COLUMN);
            Annotations<ContextAnnotation> goldStandard = FileFormatParser.getAnnotations(trainingFilePath,
                    TaggingFormat.COLUMN);
            goldStandard.sort();

            // get only those annotations that were incorrectly tagged and were never a real entity that is they have to
            // be in ERROR1 set and NOT in the gold standard
            for (Annotated wrongAnnotation : evaluationResult.getAnnotations(ResultType.ERROR1)) {

                // for the numeric classifier it is better if only annotations are removed that never appeared in the
                // gold standard for the text classifier it is better to remove annotations that are just wrong even
                // when they were correct in the gold standard at some point
                boolean addAnnotation = true;

                // check if annotation happens to be in the gold standard, if so, do not declare it completely wrong
                String wrongName = wrongAnnotation.getValue().toLowerCase();
                for (Annotated gsAnnotation : goldStandard) {
                    if (wrongName.equals(gsAnnotation.getValue().toLowerCase())) {
                        addAnnotation = false;
                        break;
                    }
                }

                ClassifiedTextDocument textInstance = new ClassifiedTextDocument(NO_ENTITY, wrongAnnotation.getValue());
                textInstances.add(textInstance);

                if (addAnnotation) {
                    removeAnnotations.add(wrongAnnotation.getValue());
                }
            }
            LOGGER.info(removeAnnotations.size() + " annotations need to be completely removed");
            // //////////////////////////////////////////////////////////////////////////////////////////////////
        }

        trainAnnotationClassifier(textInstances);

        analyzeContexts(trainingFilePath, annotations);

        saveModel(modelFilePath);

        return true;
    }

    private boolean hasAssignedType(CategoryEntries ces) {
        String mostLikelyCategoryEntry = ces.getMostLikelyCategory();
        if (mostLikelyCategoryEntry == null) {
            return false;
        }
        return !mostLikelyCategoryEntry.equalsIgnoreCase(NO_ENTITY);
    }

    /**
     * Classify candidate annotations in English mode.
     * 
     * @param entityCandidates The annotations to be classified.
     * @return Classified annotations.
     */
    private Annotations<ContextAnnotation> classifyCandidatesEnglish(List<ContextAnnotation> entityCandidates) {
        Annotations<ContextAnnotation> annotations = new Annotations<ContextAnnotation>();
        int i = 1;
        for (ContextAnnotation annotation : entityCandidates) {

            List<ContextAnnotation> wrappedAnnotations = new Annotations<ContextAnnotation>();

            if (unwrapEntities) {
                wrappedAnnotations = unwrapAnnotations(annotation, annotations);
            }

            if (!wrappedAnnotations.isEmpty()) {
                for (ContextAnnotation annotation2 : wrappedAnnotations) {
                    if (hasAssignedType(annotation2.getTags())) {
                        annotations.add(annotation2);
                    }
                }
            } else {
                CategoryEntries results = entityClassifier.classify(annotation.getValue(), annotationModel);
                if (hasAssignedType(results)) {
                    annotation.setTags(results);
                    annotations.add(annotation);
                }
            }

            ProgressHelper.printProgress(i++, entityCandidates.size(), 1);
        }

        return annotations;
    }

    /**
     * Classify candidate annotations in language independent mode.
     * 
     * @param entityCandidates The annotations to be classified.
     * @return Classified annotations.
     */
    private Annotations<ContextAnnotation> classifyCandidatesLanguageIndependent(
            List<ContextAnnotation> entityCandidates) {
        Annotations<ContextAnnotation> annotations = new Annotations<ContextAnnotation>();

        int i = 1;
        for (ContextAnnotation annotation : entityCandidates) {

            CategoryEntries results = entityClassifier.classify(annotation.getValue(), annotationModel);
            if (hasAssignedType(results)) {
                annotation.setTags(results);
                annotations.add(annotation);
            }

            ProgressHelper.printProgress(i++, entityCandidates.size(), 1);
        }

        return annotations;
    }

    @Override
    public List<Annotated> getAnnotations(String inputText) {
        StopWatch stopWatch = new StopWatch();

        Annotations<Annotated> annotations = new Annotations<Annotated>();

        if (languageMode.equals(LanguageMode.English)) {
            annotations.addAll(getAnnotationsEnglish(inputText));
        } else {
            annotations.addAll(getAnnotationsLanguageIndependent(inputText));
        }

        // recognize and add URLs, remove annotations that were part of a URL
        if (isTagUrls()) {
            UrlTagger urlTagger = new UrlTagger();
            annotations.addAll(urlTagger.tagUrls(inputText));
            annotations.removeNested();
        }

        // recognize and add dates, remove annotations that were part of a date
        if (isTagDates()) {
            DateAndTimeTagger datTagger = new DateAndTimeTagger();
            annotations.addAll(datTagger.tagDateAndTime(inputText));
            annotations.removeNested();
        }

        // FileHelper.writeToFile("data/temp/ner/palladianNerOutput.txt", tagText(inputText, annotations));

        annotations.removeNested();
        annotations.sort();

        LOGGER.info("Got {} annotations in {}", annotations.size(), stopWatch.getElapsedTimeString());

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

        LOGGER.debug("start post processing annotations");

        StopWatch stopWatch = new StopWatch();

        Annotations<ContextAnnotation> toRemove = new Annotations<ContextAnnotation>();

        // remove dates
        if (removeDates) {
            stopWatch.start();
            int c = 0;
            for (ContextAnnotation annotation : annotations) {
                if (containsDateFragment(annotation.getValue())) {
                    toRemove.add(annotation);
                    c++;
                }
            }
            LOGGER.debug("removed " + c + " purely date annotations in " + stopWatch.getElapsedTimeString());
        }

        // remove date entries in annotations, such as "July Peter Jackson" => "Peter Jackson"
        if (removeDateEntries) {
            stopWatch.start();
            int c = 0;
            for (ContextAnnotation annotation : annotations) {

                Object[] result = removeDateFragment(annotation.getValue());
                String entity = (String)result[0];

                annotation.setEntity(entity);
                annotation.setOffset(annotation.getStartPosition() + (Integer)result[1]);
                annotation.setLength(annotation.getValue().length());

                if ((Integer)result[1] > 0) {
                    c++;
                }
            }
            LOGGER.debug("removed " + c + " partial date annotations in " + stopWatch.getElapsedTimeString());
        }

        // remove annotations that were found to be incorrectly tagged in the training data
        if (removeIncorrectlyTaggedInTraining) {
            stopWatch.start();
            for (String removeAnnotation : removeAnnotations) {
                String removeName = removeAnnotation.toLowerCase();
                for (ContextAnnotation annotation : annotations) {
                    if (removeName.equals(annotation.getValue().toLowerCase())) {
                        toRemove.add(annotation);
                    }
                }
            }
            LOGGER.debug("removed " + removeAnnotations.size() + " incorrectly tagged entities in training data in "
                    + stopWatch.getElapsedTimeString());
        }

        // similar to removeSentenceStartErrorsPos but we use a learned case dictionary to remove possibly incorrectly
        // tagged sentence starts. For example ". This" is removed since "this" is usually spelled using lowercase
        // characters only. This is done NOT only for words at sentence start but all single token words.
        int c = 0;
        if (removeSentenceStartErrorsCaseDictionary) {
            stopWatch.start();

            for (ContextAnnotation annotation : annotations) {

                if (/*
                     * // if the annotation is at the start of a sentence
                     * Boolean.valueOf(annotation.getNominalFeatures().get(0))
                     * &&
                     */annotation.getValue().indexOf(" ") == -1) {

                    double upperCaseToLowerCaseRatio = 2;

                    // CategoryEntries ces = caseDictionary.get(tokenTermMap.get(annotation.getValue().toLowerCase()));
                    CategoryEntries ces = caseDictionary.getCategoryEntries(annotation.getValue().toLowerCase());
                    if (ces != null && ces.iterator().hasNext()) {
                        double allUpperCase = 0.0;
                        double upperCase = 0.0;
                        double lowerCase = 0.0;

                        if (ces.getProbability("A") > 0) {
                            allUpperCase = ces.getProbability("A");
                        }

                        if (ces.getProbability("Aa") > 0) {
                            upperCase = ces.getProbability("Aa");
                        }

                        if (ces.getProbability("a") > 0) {
                            lowerCase = ces.getProbability("a");
                        }

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
                        LOGGER.debug("remove word using the case signature: " + annotation.getValue() + " (ratio:"
                                + upperCaseToLowerCaseRatio + ") | " + annotation.getRightContext());
                    }

                }
            }

            LOGGER.debug("removed " + c + " words at beginning of sentence in " + stopWatch.getElapsedTimeString());
        }

        LOGGER.debug("remove " + toRemove.size() + " entities");
        annotations.removeAll(toRemove);

        // switch using pattern information
        int changed = 0;
        if (switchTagAnnotationsUsingPatterns) {
            stopWatch.start();

            for (ContextAnnotation annotation : annotations) {

                String tagNameBefore = annotation.getTag();

                applyContextAnalysis(annotation);

                if (!annotation.getTag().equalsIgnoreCase(tagNameBefore)) {
                    LOGGER.debug("changed " + annotation.getValue() + " from " + tagNameBefore + " to "
                            + annotation.getTag() + ", left context: " + annotation.getLeftContext() + "____"
                            + annotation.getRightContext());
                    changed++;
                }

            }
            LOGGER.debug("changed " + MathHelper.round(100 * changed / (annotations.size() + 0.000000000001), 2)
                    + "% of the entities using patterns in " + stopWatch.getElapsedTimeString());

        }

        // switch annotations that are in the dictionary
        changed = 0;
        if (switchTagAnnotationsUsingDictionary) {
            stopWatch.start();

            for (Annotation annotation : annotations) {

                CategoryEntries categoryEntries = entityDictionary.getCategoryEntries(annotation.getValue());
                if (categoryEntries != null && categoryEntries.iterator().hasNext()) {

                    // get only the most likely concept
                    CategoryEntriesMap mostLikelyCes = new CategoryEntriesMap();
                    if (conceptLikelihoodOrder != null) {
                        ol: for (String conceptName : conceptLikelihoodOrder) {
                            for (String categoryEntry : categoryEntries) {
                                if (categoryEntries.getProbability(categoryEntry) > 0
                                        && categoryEntry.equalsIgnoreCase(conceptName)) {
                                    mostLikelyCes.set(categoryEntry, categoryEntries.getProbability(categoryEntry));
                                    break ol;
                                }
                            }
                        }
                        if (mostLikelyCes.iterator().hasNext()) {
                            categoryEntries = mostLikelyCes;
                        }
                    }

                    annotation.setTags(categoryEntries);
                    changed++;
                }

            }
            LOGGER.debug("changed with entity dictionary "
                    + MathHelper.round(100 * changed / (annotations.size() + 0.000000000001), 2)
                    + "% of the entities (total entities: " + annotations.size() + ") in "
                    + stopWatch.getElapsedTimeString());
        }

        Annotations<ContextAnnotation> toAdd = new Annotations<ContextAnnotation>();

        stopWatch.start();
        LinkedHashMap<String, Integer> sortedMap = leftContextMap.getSortedMapDescending();

        for (ContextAnnotation annotation : annotations) {

            // remove all annotations with "DOCSTART- " in them because that is for format purposes
            if (annotation.getValue().toLowerCase().indexOf("docstart") > -1) {
                toRemove.add(annotation);
                continue;
            }

            // if all uppercase, try to find known annotations
            // if (StringHelper.isCompletelyUppercase(annotation.getValue().substring(10,
            // Math.min(12, annotation.getValue().length())))) {

            if (unwrapEntities) {
                Annotations<ContextAnnotation> wrappedAnnotations = unwrapAnnotations(annotation, annotations);

                if (!wrappedAnnotations.isEmpty()) {
                    for (ContextAnnotation annotation2 : wrappedAnnotations) {
                        if (hasAssignedType(annotation2.getTags())) {
                            toAdd.add(annotation2);
                            // LOGGER.debug("add " + annotation2.getValue());
                        }
                    }
                    String debugString = "tried to unwrap again " + annotation.getValue();
                    for (Annotation wrappedAnnotation : wrappedAnnotations) {
                        debugString += " | " + wrappedAnnotation.getValue();
                    }
                    LOGGER.debug(debugString);
                }
            }

            // unwrap annotations containing context patterns, e.g. "President Obama" => "President" is known left
            // context for people
            // XXX move this up?
            if (unwrapEntitiesWithContext) {
                for (Entry<String, Integer> leftContextEntry : sortedMap.entrySet()) {

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
                        for (String term : entityDictionary.getTerms()) {

                            int indexPrefix = annotation.getValue().substring(0, index + length).indexOf(term + " ");
                            if (indexPrefix > -1 && term.length() > 2) {
                                ContextAnnotation wrappedAnnotation2 = new ContextAnnotation(
                                        annotation.getStartPosition() + indexPrefix, term, entityDictionary
                                                .getCategoryEntries(term).getMostLikelyCategory());
                                toAdd.add(wrappedAnnotation2);

                                LOGGER.debug("add from prefix " + wrappedAnnotation2.getValue());
                                break;
                            }

                        }

                        toRemove.add(annotation);
                        LOGGER.debug("add " + wrappedAnnotation.getValue() + ", delete " + annotation.getValue()
                                + " (left context:" + leftContext + ", " + leftContextEntry.getValue() + ")");

                        break;
                    }

                }
            }
        }
        LOGGER.debug("Unwrapped entities in {}", stopWatch.getElapsedTimeString());

        LOGGER.debug("Add {} entities", toAdd.size());
        annotations.addAll(toAdd);

        LOGGER.debug("Remove {} entities", toRemove.size());
        annotations.removeAll(toRemove);
    }

    public Annotations<ContextAnnotation> getAnnotationsEnglish(String inputText) {

        // use the the string tagger to tag entities in English mode
        Annotations<ContextAnnotation> entityCandidates = StringTagger.getTaggedEntities(inputText);

        // classify annotations with the UniversalClassifier
        Annotations<ContextAnnotation> annotations = classifyCandidatesEnglish(entityCandidates);

        postProcessAnnotations(annotations);

        return annotations;
    }

    public Annotations<ContextAnnotation> getAnnotationsLanguageIndependent(String inputText) {

        removeDates = false;
        removeDateEntries = false;
        removeIncorrectlyTaggedInTraining = false;
        switchTagAnnotationsUsingPatterns = false;
        switchTagAnnotationsUsingDictionary = true;
        unwrapEntities = false;
        unwrapEntitiesWithContext = false;

        // get the candates, every token is potentially a (part of) an entity
        Annotations<ContextAnnotation> annotations = StringTagger.getTaggedEntities(inputText,
                Tokenizer.TOKEN_SPLIT_REGEX);

        // classify annotations with the UniversalClassifier
        annotations = classifyCandidatesLanguageIndependent(annotations);

        // filter annotations
        postProcessAnnotations(annotations);

        // combine annotations that are right next to each other having the same tag
        Annotations<ContextAnnotation> combinedAnnotations = new Annotations<ContextAnnotation>();
        annotations.sort();
        Annotated lastAnnotation = new Annotation(-2, "", "");
        Annotated lastCombinedAnnotation = null;

        for (ContextAnnotation annotation : annotations) {
            if (!annotation.getTag().equalsIgnoreCase("o")
                    && annotation.getTag().equalsIgnoreCase(lastAnnotation.getTag())
                    && annotation.getStartPosition() == lastAnnotation.getEndPosition() + 1) {

                if (lastCombinedAnnotation == null) {
                    lastCombinedAnnotation = lastAnnotation;
                }

                ContextAnnotation combinedAnnotation = new ContextAnnotation(lastCombinedAnnotation.getStartPosition(),
                        lastCombinedAnnotation.getValue() + " " + annotation.getValue(), annotation.getTag());
                combinedAnnotations.add(combinedAnnotation);
                lastCombinedAnnotation = combinedAnnotation;
                combinedAnnotations.remove(lastCombinedAnnotation);
            } else {
                combinedAnnotations.add(annotation);
                lastCombinedAnnotation = null;
            }

            lastAnnotation = annotation;
        }

        // remove all "O"
        Annotations<ContextAnnotation> cleanAnnotations = new Annotations<ContextAnnotation>();
        for (ContextAnnotation annotation : combinedAnnotations) {
            if (!annotation.getTag().equalsIgnoreCase("o") && annotation.getValue().length() > 1) {
                cleanAnnotations.add(annotation);
            }
        }

        return cleanAnnotations;
    }

    private void applyContextAnalysis(ContextAnnotation annotation) {

        // get the left and right context patterns and merge them into one context pattern list
        String[] leftContexts = annotation.getLeftContexts();
        String[] rightContexts = annotation.getRightContexts();

        List<String> contexts = new ArrayList<String>();
        for (String pattern : leftContexts) {
            contexts.add(pattern);
        }
        for (String pattern : rightContexts) {
            contexts.add(pattern);
        }

        // the probability map holds the probability for the pattern(s) and a certain entity type
        Map<String, Double> probabilityMap = new HashMap<String, Double>();

        // initialize all entity types with one
        for (String string : patternProbabilityMatrix.getKeysX()) {
            probabilityMap.put(string, 0.0);
        }

        // check all context patterns left and right
        for (String contextPattern : contexts) {

            contextPattern = contextPattern.toLowerCase();

            // skip empty patterns
            if (contextPattern.length() == 0) {
                continue;
            }

            // count the number of matching patterns per entity type
            CountMap<String> matchingPatternMap = CountMap.create();

            int sumOfMatchingPatterns = 0;
            for (String string : patternProbabilityMatrix.getKeysX()) {

                Integer matches = patternProbabilityMatrix.get(string, contextPattern);
                if (matches == null) {
                    matchingPatternMap.set(string, 0);
                } else {
                    matchingPatternMap.add(string, matches);
                    sumOfMatchingPatterns += matches;
                }

            }

            if (sumOfMatchingPatterns == 0) {
                continue;
            }

            for (String string : patternProbabilityMatrix.getKeysX()) {
                Double probability = probabilityMap.get(string);
                probability += matchingPatternMap.getCount(string) / (double)sumOfMatchingPatterns;
                probabilityMap.put(string, probability);
            }
        }

        CategoryEntriesMap ce = new CategoryEntriesMap();

        double sum = 0;

        for (String string : patternProbabilityMatrix.getKeysX()) {
            sum += probabilityMap.get(string);
        }
        if (sum == 0) {
            sum = 1;
        }
        for (String string : patternProbabilityMatrix.getKeysX()) {
            ce.set(string, probabilityMap.get(string) / sum);
        }

        CategoryEntries ceMerge = CategoryEntriesMap.merge(ce, annotation.getTags());
        annotation.setTags(ceMerge);
    }

    /**
     * <p>
     * Check whether the given text contains a date fragment. For example "June John Hiatt" would return true.
     * </p>
     * 
     * @param text The text to check for date fragments.
     * @return <tt>True</tt>, if the text contains a date fragment, <tt>false</tt> otherwise.
     */
    private boolean containsDateFragment(String text) {
        text = text.toLowerCase();
        String[] regExps = RegExp.DATE_FRAGMENTS;

        for (String regExp : regExps) {
            if (text.replaceAll(regExp.toLowerCase(), "").trim().isEmpty()) {
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
     * @param text The text to be cleased of date fragments.
     * @return An object array containing the new cleased text on position 0 and the offset which was caused by the
     *         removal on position 1.
     */
    private Object[] removeDateFragment(String text) {
        String[] regExps = RegExp.DATE_FRAGMENTS;

        Object[] result = new Object[2];
        int offsetChange = 0;

        for (String regExp : regExps) {
            int textLength = text.length();

            // for example "Apr John Hiatt"
            if (StringHelper.countRegexMatches(text, "^" + regExp + " ") > 0) {
                text = text.replaceAll("^" + regExp + " ", "").trim();
                offsetChange += textLength - text.length();
            }
            if (StringHelper.countRegexMatches(text, " " + regExp + "$") > 0) {
                text = text.replaceAll(" " + regExp + "$", "").trim();
            }

            // for example "Apr. John Hiatt"
            if (StringHelper.countRegexMatches(text, "^" + regExp + "\\. ") > 0) {
                text = text.replaceAll("^" + regExp + "\\. ", "").trim();
                offsetChange += textLength - text.length();
            }
            if (StringHelper.countRegexMatches(text, " " + regExp + "\\.$") > 0) {
                text = text.replaceAll(" " + regExp + "\\.$", "").trim();
            }
        }

        result[0] = text;
        result[1] = offsetChange;

        return result;
    }

    /**
     * <p>
     * Analyze the context around the annotations. The context classifier will be trained and left context patterns will
     * be stored.
     * </p>
     * 
     * @param trainingFilePath The path to the training data.
     * @param trainingAnnotations The training annotations.
     */
    private void analyzeContexts(String trainingFilePath, List<? extends Annotated> trainingAnnotations) {

        LOGGER.debug("start analyzing contexts");

        Map<String, CountMap<String>> contextMap = new TreeMap<String, CountMap<String>>();
        CountMap<String> leftContextMapCountMap = CountMap.create();
        leftContextMap = CountMap.create();
        CountMap<String> tagCounts = CountMap.create();

        // get all training annotations including their features
        Annotations<ContextAnnotation> annotations = FileFormatParser.getAnnotationsFromColumn(trainingFilePath);

        List<ClassifiedTextDocument> trainingInstances = CollectionHelper.newArrayList();

        // iterate over all annotations and analyze their left and right contexts for patterns
        int c = 1;
        for (ContextAnnotation annotation : annotations) {

            String tag = annotation.getTag();

            // the left patterns containing 1-3 words
            String[] leftContexts = annotation.getLeftContexts();

            // the right patterns containing 1-3 words
            String[] rightContexts = annotation.getRightContexts();

            if (contextMap.get(tag) == null) {
                contextMap.put(tag, CountMap.<String> create());
            }

            // add the left contexts to the map
            contextMap.get(tag).add(leftContexts[0]);
            contextMap.get(tag).add(leftContexts[1]);
            contextMap.get(tag).add(leftContexts[2]);

            leftContextMapCountMap.add(leftContexts[0]);
            leftContextMapCountMap.add(leftContexts[1]);
            leftContextMapCountMap.add(leftContexts[2]);

            // add the right contexts to the map
            contextMap.get(tag).add(rightContexts[0]);
            contextMap.get(tag).add(rightContexts[1]);
            contextMap.get(tag).add(rightContexts[2]);

            tagCounts.add(tag);

            String text = annotation.getLeftContext() + "__" + annotation.getRightContext();
            ClassifiedTextDocument trainingInstance = new ClassifiedTextDocument(tag, text);
            trainingInstances.add(trainingInstance);

            ProgressHelper.printProgress(c++, annotations.size(), 1);
        }

        // fill the leftContextMap with the context and the ratio of inside annotation / outside annotation
        for (String leftContext : leftContextMapCountMap.uniqueItems()) {
            int outside = leftContextMapCountMap.getCount(leftContext);
            int inside = 0;

            for (Annotation annotation : annotations) {
                if (annotation.getValue().startsWith(leftContext + " ") || annotation.getValue().equals(leftContext)) {
                    inside++;
                }
            }

            double ratio = (double)inside / (double)outside;
            if (ratio >= 1 || outside < 2) {
                leftContextMap.add(leftContext, 0);
            } else {
                leftContextMap.add(leftContext, 1);
            }

        }

        trainContextClassifier(trainingInstances);

        StringBuilder csv = new StringBuilder();
        for (Entry<String, CountMap<String>> entry : contextMap.entrySet()) {

            int tagCount = tagCounts.getCount(entry.getKey());
            CountMap<String> patterns = contextMap.get(entry.getKey());
            LinkedHashMap<String, Integer> sortedMap = patterns.getSortedMap();

            csv.append(entry.getKey()).append("###").append(tagCount).append("\n");

            // print the patterns and their count for the current tag
            for (Entry<String, Integer> patternEntry : sortedMap.entrySet()) {
                if (patternEntry.getValue() > 0) {
                    csv.append(patternEntry.getKey()).append("###").append(patternEntry.getValue()).append("\n");
                }
            }

            csv.append("++++++++++++++++++++++++++++++++++\n\n");
        }

        // tagMap to matrix
        for (Entry<String, CountMap<String>> patternEntry : contextMap.entrySet()) {

            for (String tagEntry : patternEntry.getValue().uniqueItems()) {
                int count = patternEntry.getValue().getCount(tagEntry);
                patternProbabilityMatrix.set(patternEntry.getKey(), tagEntry.toLowerCase(), count);
            }

        }

        // FileHelper.writeToFile("data/temp/tagPatternAnalysis.csv", csv);
    }

    private void trainContextClassifier(List<ClassifiedTextDocument> trainingInstances) {
        contextModel = contextClassifier.train(trainingInstances);
    }

    public LanguageMode getLanguageMode() {
        return languageMode;
    }

    // public void setLanguageMode(LanguageMode languageMode) {
    // this.languageMode = languageMode;
    // }
    //
    // public void setTrainingMode(TrainingMode trainingMode) {
    // this.trainingMode = trainingMode;
    // if (trainingMode == TrainingMode.Sparse) {
    // removeDates = true;
    // removeDateEntries = true;
    // removeIncorrectlyTaggedInTraining = false;
    // removeSentenceStartErrorsCaseDictionary = true;
    // switchTagAnnotationsUsingPatterns = true;
    // switchTagAnnotationsUsingDictionary = true;
    // unwrapEntities = true;
    // unwrapEntitiesWithContext = true;
    // retraining = false;
    // }
    // }

    public TrainingMode getTrainingMode() {
        return trainingMode;
    }

    // ////////////// accessors for testing only. /////////////////////

    DictionaryModel getEntityDictionary() {
        return entityDictionary;
    }

    DictionaryModel getCaseDictionary() {
        return caseDictionary;
    }

    CountMap<String> getLeftContextMap() {
        return leftContextMap;
    }

    List<String> getRemoveAnnotations() {
        return removeAnnotations;
    }

    DictionaryModel getContextModel() {
        return contextModel;
    }

    DictionaryModel getAnnotationModel() {
        return annotationModel;
    }

    // /////////////////////////////////////////////////////////////////////

    public void setTagUrls(boolean tagUrls) {
        this.tagUrls = tagUrls;
    }

    public boolean isTagUrls() {
        return tagUrls;
    }

    public void setTagDates(boolean tagDates) {
        this.tagDates = tagDates;
    }

    public boolean isTagDates() {
        return tagDates;
    }

    /**
     * <p>
     * Try to find which of the given annotation are part of this entity. For example: "New York City and Dresden"
     * contains two entities that might be in the given annotation set. If so, we return the found annotations.
     * </p>
     * 
     * @param annotation The annotation to check.
     * @param annotations The annotations we are searching for in this entity.
     * @return A set of annotations found in this annotation.
     */
    private Annotations<ContextAnnotation> unwrapAnnotations(Annotated annotation, List<ContextAnnotation> annotations) {
        Annotations<ContextAnnotation> unwrappedAnnotations = new Annotations<ContextAnnotation>();

        boolean isAllUppercase = StringHelper.isCompletelyUppercase(annotation.getValue());

        if (!isAllUppercase) {
            return unwrappedAnnotations;
        }

        String entityName = annotation.getValue().toLowerCase();
        int length = entityName.length();

        for (Annotated currentAnnotation : annotations) {
            if (currentAnnotation.getValue().length() < length) {
                int index = entityName.indexOf(" " + currentAnnotation.getValue().toLowerCase() + " ");
                if (index > -1 && currentAnnotation.getValue().length() > 2) {
                    ContextAnnotation wrappedAnnotation = new ContextAnnotation(annotation.getStartPosition() + index
                            + 1, currentAnnotation.getValue(), currentAnnotation.getTag());
                    unwrappedAnnotations.add(wrappedAnnotation);
                }

                index = entityName.indexOf(currentAnnotation.getValue().toLowerCase() + " ");
                if (index == 0 && currentAnnotation.getValue().length() > 2) {
                    ContextAnnotation wrappedAnnotation = new ContextAnnotation(annotation.getStartPosition() + index,
                            currentAnnotation.getValue(), currentAnnotation.getTag());
                    unwrappedAnnotations.add(wrappedAnnotation);
                }

                index = entityName.indexOf(" " + currentAnnotation.getValue().toLowerCase());
                if (index == entityName.length() - currentAnnotation.getValue().length() - 1
                        && currentAnnotation.getValue().length() > 2) {
                    ContextAnnotation wrappedAnnotation = new ContextAnnotation(annotation.getStartPosition() + index
                            + 1, currentAnnotation.getValue(), currentAnnotation.getTag());
                    unwrappedAnnotations.add(wrappedAnnotation);
                }
            }
        }

        // go through the entity dictionary
        for (String term : entityDictionary.getTerms()) {
            if (term.length() < length) {
                int index = entityName.indexOf(" " + term.toLowerCase() + " ");
                CategoryEntries categoryEntries = entityDictionary.getCategoryEntries(term);
                String mostLikelyCategory = categoryEntries.getMostLikelyCategory();
                if (index > -1 && term.length() > 2) {
                    ContextAnnotation wrappedAnnotation = new ContextAnnotation(annotation.getStartPosition() + index
                            + 1, term, mostLikelyCategory);
                    unwrappedAnnotations.add(wrappedAnnotation);
                }

                index = entityName.indexOf(term.toLowerCase() + " ");
                if (index == 0 && term.length() > 2) {
                    ContextAnnotation wrappedAnnotation = new ContextAnnotation(annotation.getStartPosition() + index,
                            term, mostLikelyCategory);
                    unwrappedAnnotations.add(wrappedAnnotation);
                }

                index = entityName.indexOf(" " + term.toLowerCase());
                if (index == entityName.length() - term.length() - 1 && term.length() > 2) {
                    ContextAnnotation wrappedAnnotation = new ContextAnnotation(annotation.getStartPosition() + index
                            + 1, term, mostLikelyCategory);
                    unwrappedAnnotations.add(wrappedAnnotation);
                }
            }
        }

        return unwrappedAnnotations;
    }

    @Override
    public String getName() {
        return "Palladian NER (" + languageMode + "," + trainingMode + ")";
    }

    /**
     * @param args
     */
    @SuppressWarnings({"static-access", "unused"})
    public static void main(String[] args) {

        PalladianNer tagger = new PalladianNer();

        if (args.length > 0) {

            Options options = new Options();
            options.addOption(OptionBuilder.withLongOpt("mode").withDescription("whether to tag or train a model")
                    .create());

            OptionGroup modeOptionGroup = new OptionGroup();
            modeOptionGroup.addOption(OptionBuilder.withArgName("tg").withLongOpt("tag").withDescription("tag a text")
                    .create());
            modeOptionGroup.addOption(OptionBuilder.withArgName("tr").withLongOpt("train")
                    .withDescription("train a model").create());
            modeOptionGroup.addOption(OptionBuilder.withArgName("ev").withLongOpt("evaluate")
                    .withDescription("evaluate a model").create());
            modeOptionGroup.addOption(OptionBuilder.withArgName("dm").withLongOpt("demo")
                    .withDescription("demo mode of the tagger").create());
            modeOptionGroup.setRequired(true);
            options.addOptionGroup(modeOptionGroup);

            options.addOption(OptionBuilder.withLongOpt("trainingFile")
                    .withDescription("the path and name of the training file for the tagger (only if mode = train)")
                    .hasArg().withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder
                    .withLongOpt("testFile")
                    .withDescription(
                            "the path and name of the test file for evaluating the tagger (only if mode = evaluate)")
                    .hasArg().withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder.withLongOpt("configFile")
                    .withDescription("the path and name of the config file for the tagger").hasArg()
                    .withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder.withLongOpt("inputText")
                    .withDescription("the text that should be tagged (only if mode = tag)").hasArg()
                    .withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder.withLongOpt("outputFile")
                    .withDescription("the path and name of the file where the tagged text should be saved to").hasArg()
                    .withArgName("text").withType(String.class).create());

            HelpFormatter formatter = new HelpFormatter();

            CommandLineParser parser = new PosixParser();
            CommandLine cmd = null;
            try {
                cmd = parser.parse(options, args);

                if (cmd.hasOption("tag")) {

                    tagger.loadModel(cmd.getOptionValue("configFile"));
                    String taggedText = tagger.tag(cmd.getOptionValue("inputText"));

                    if (cmd.hasOption("outputFile")) {
                        FileHelper.writeToFile(cmd.getOptionValue("outputFile"), taggedText);
                    } else {
                        System.out.println("No output file given so tagged text will be printed to the console:");
                        System.out.println(taggedText);
                    }

                } else if (cmd.hasOption("train")) {

                    tagger.train(cmd.getOptionValue("trainingFile"), cmd.getOptionValue("configFile"));

                } else if (cmd.hasOption("evaluate")) {

                    tagger.loadModel(cmd.getOptionValue("configFile"));
                    tagger.evaluate(cmd.getOptionValue("trainingFile"), TaggingFormat.XML);

                }

            } catch (ParseException e) {
                LOGGER.debug("Command line arguments could not be parsed!");
                formatter.printHelp("FeedChecker", options);
            }

        }

        // ################################# HOW TO USE #################################

        // set mode (English or language independent)
        // set type of training set (complete supervised or sparse semi-supervised)
        tagger = new PalladianNer(LanguageMode.English, TrainingMode.Complete);

        /**
         * ained = data/models/palladian/ner/palladianNerTudCs4Annotations
         * models.palladian.nerWebTrained = data/models/palladian/ner/
         */

        // // training the tagger
        // needs to point to a column separated file
        String trainingPath = "data/datasets/ner/conll/training.txt";
        // trainingPath = "data/temp/seedsTest100.txt";
        trainingPath = "data/datasets/ner/tud/tud2011_train.txt";
        String modelPath = "data/temp/palladianNerTudCs4Annotations";
        modelPath = "data/temp/palladianNerTudCs4";
        // modelPath = "data/temp/palladianNerConllAnnotations";
        // modelPath = "data/temp/palladianNerConll";
        // modelPath = "data/temp/palladianNerWebTrained100Annotations";

        // set whether to tag dates
        tagger.setTagDates(false);
        // tagger.setTagDates(true);

        // set whether to tag URLs
        tagger.setTagUrls(false);
        // tagger.setTagUrls(true);

        // create a dictionary from a dictionary txt file
        // tagger.makeDictionary("mergedDictComplete.csv");

        // we can add annotations without any context to the tagger to improve internal evidence features
        String trainingSeedFilePath = PalladianNer.class.getResource("/nerSeeds.txt").getFile();
        List<ContextAnnotation> trainingAnnotations = FileFormatParser.getSeedAnnotations(trainingSeedFilePath, -1);

        // train the tagger on the training file (with or without additional training annotations)
        // tagger.train(trainingPath, trainingAnnotations, modelPath);
        tagger.train(trainingPath, modelPath);

        System.exit(0);

        // // using a trained tagger
        // load a trained tagger
        tagger.loadModel(modelPath);

        // load an additional entity dictionary
        // StopWatch sw2 = new StopWatch();
        // Dictionary dict = FileHelper.deserialize("dict.ser.gz");
        // LOGGER.info(sw2.getTotalElapsedTimeString());
        // tagger.setEntityDictionary(dict);

        // tag a sentence
        String inputText = "Peter J. Johnson lives in New York City in the U.S.A.";
        String taggedText = tagger.tag(inputText);
        System.out.println(taggedText);

        CollectionHelper.print(tagger.getAnnotations(inputText));

        System.exit(0);

        // // evaluate a tagger
        String testPath = "data/datasets/ner/conll/test_final.txt";
        testPath = "data/datasets/ner/tud/tud2011_test.txt";
        EvaluationResult evr = tagger.evaluate(testPath, TaggingFormat.COLUMN);
        System.out.println(evr.getMUCResultsReadable());
        System.out.println(evr.getExactMatchResultsReadable());

        // CoNLL
        // without the dictionary
        // precision MUC: 76.19%, recall MUC: 82.25%, F1 MUC: 79.1%
        // precision exact: 64.54%, recall exact: 69.67%, F1 exact: 67.01%

        // with the dbpedia dictionary BUT the types of conll and dbpedia do not match therefore a worse result
        // precision MUC: 57.09%, recall MUC: 62.96%, F1 MUC: 59.88%
        // precision exact: 30.41%, recall exact: 33.54%, F1 exact: 31.9%

        // TUDCS4
        // without the dictionary
        // precision MUC: 52.12%, recall MUC: 53.36%, F1 MUC: 52.73%
        // precision exact: 29.4%, recall exact: 30.11%, F1 exact: 29.75%

        // with additional training annotations (src/main/resources/nerSeeds.txt)
        // precision MUC: 51.92%, recall MUC: 64.46%, F1 MUC: 57.52%
        // precision exact: 31.95%, recall exact: 39.66%, F1 exact: 35.39%

        // learned from automatically generated training data, sparse (100 seeds)
        // precision MUC: 42.56%, recall MUC: 51.76%, F1 MUC: 46.71%
        // precision exact: 16.49%, recall exact: 20.05%, F1 exact: 18.09%

        System.exit(0);

        // EvaluationResult er = tagger.evaluate("data/datasets/ner/conll/test_validation.txt",
        // "data/temp/tudner.model",
        // TaggingFormat.COLUMN);
        // EvaluationResult er = tagger.evaluate("data/datasets/ner/conll/test_final.txt", "", TaggingFormat.COLUMN);
        // EvaluationResult er = tagger.evaluate(testFilePath, "", TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
        //
        // System.out.println(stopWatch.getElapsedTimeString());
        //
        // HashSet<String> trainingTexts = new HashSet<String>();
        // trainingTexts
        // .add("Australia is a country and a continent at the same time. New Zealand is also a country but not a continent");
        // trainingTexts
        // .add("Many countries, such as Germany and Great Britain, have a strong economy. Other countries such as Iceland and Norway are in the north and have a smaller population");
        // trainingTexts.add("In south Europe, a nice country named Italy is formed like a boot.");
        // trainingTexts.add("In the western part of Europe, the is a country named Spain which is warm.");
        // trainingTexts
        // .add("Bruce Willis is an actor, Jim Carrey is an actor too, but Trinidad is a country name and and actor name as well.");
        // trainingTexts.add("In west Europe, a warm country named Spain has good seafood.");
        // trainingTexts.add("Another way of thinking of it is to drive to another coutry and have some fun.");
        //
        // // possible tags
        // CollectionHelper.print(tagger.getModelTags("data/models/tudner/tudner.model"));
        //
        // // train
        // tagger.train("data/datasets/ner/sample/trainingColumn.tsv", "data/models/tudner/tudner.model");
        //
        // // tag
        // tagger.loadModel("data/models/tudner/tudner.model");
        // tagger.tag("John J. Smith and the Nexus One location iphone 4 mention Seattle in the text John J. Smith lives in Seattle.");
        //
        // tagger.tag(
        // "John J. Smith and the Nexus One location iphone 4 mention Seattle in the text John J. Smith lives in Seattle.",
        // "data/models/tudner/tudner.model");
        //
        // // evaluate
        // tagger.evaluate("data/datasets/ner/sample/testingColumn.tsv", "data/models/tudner/tudner.model",
        // TaggingFormat.COLUMN);

        // /////////////////////////// train and test /////////////////////////////
        // tagger.train("data/datasets/ner/politician/text/training.tsv", "data/models/tudner/tudner.model");
        // EvaluationResult er = tagger.evaluate("data/datasets/ner/politician/text/testing.tsv",
        // "data/models/tudner/tudner.model", TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        // using a column trainig and testing file
        String trainingFilePath = "data/temp/autoGeneratedDataConll/seedsTest1.txt";
        // trainingFilePath = "data/temp/autoGeneratedDataTUD2/seedsTest1.txt";
        // trainingFilePath = "data/temp/autoGeneratedDataTUD4/seedsTest50.txt";
        // trainingFilePath = "data/datasets/ner/conll/training_small.txt";
        // trainingFilePath = "data/temp/seedsTest100.txt";

        String testFilePath = "data/datasets/ner/conll/test_final.txt";
        testFilePath = "data/datasets/ner/tud/tud2011_test.txt";

        String seedFilePath = "data/datasets/ner/conll/training.txt";
        seedFilePath = "data/datasets/ner/tud/manuallyPickedSeeds/seedListC.txt";

        StopWatch stopWatch = new StopWatch();

        // /////////////////////// evaluation purposes //////////////////////////
        StringBuilder evaluationResults = new StringBuilder();
        // Annotations ignoreAnnotations = FileFormatParser.getSeedAnnotations(trainingFilePath, -1);
        Set<String> ignoreAnnotations = new HashSet<String>();
        for (Annotation annotation : FileFormatParser.getSeedAnnotations(trainingFilePath, -1)) {
            ignoreAnnotations.add(annotation.getValue());
        }
        String datasetFolder = "data/temp/autoGeneratedDataTUD4/";
        datasetFolder = "data/temp/autoGeneratedDataConll/";
        datasetFolder = "data/temp/autoGeneratedDataTUD/";

        // for (int i = 1; i <= 100; i += 10) {
        for (int i = 1; i <= 5; i++) {

            int j = i;
            if (j > 1) {
                j *= 10;
            }
            j = 10;
            trainingFilePath = datasetFolder + "newDataset" + j + ".txt";

            tagger = new PalladianNer(LanguageMode.English, TrainingMode.Sparse);

            // Annotations annotations = FileFormatParser.getSeedAnnotations(seedFilePath, i);
            // tagger.train(trainingFilePath, annotations, "data/temp/tudner2.model");
            tagger.train(trainingFilePath, "data/temp/tudner");
            // tagger.train(annotations, "data/temp/tudner2.model");
            tagger.loadModel("data/temp/tudner");

            EvaluationResult er = tagger.evaluate(testFilePath, TaggingFormat.COLUMN, ignoreAnnotations);

            evaluationResults.append(er.getPrecision(EvaluationMode.EXACT_MATCH)).append(";");
            evaluationResults.append(er.getRecall(EvaluationMode.EXACT_MATCH)).append(";");
            evaluationResults.append(er.getF1(EvaluationMode.EXACT_MATCH)).append(";");
            evaluationResults.append(er.getPrecision(EvaluationMode.MUC)).append(";");
            evaluationResults.append(er.getRecall(EvaluationMode.MUC)).append(";");
            evaluationResults.append(er.getF1(EvaluationMode.MUC)).append(";");

            evaluationResults.append("\n");
            FileHelper.writeToFile("results.txt", evaluationResults);
        }
        System.exit(0);
        // 2-8, 4-5, 040: 0.3912314995811226;
        // 2-8, 4-5, 040, seedsText2: 0.40139470013947
        // 2-8, 4-5, 120: 0.39039374476403244;
        // 2-8, 4-7, 040: 0.3937447640323932;
        // 2-8, 4-5, 040, leftContext 31 count: 0.41089385474860335
        // 2-8, 4-5, 040: seedsText50, 0.44447560291643295
        // 2-8, 4-5, 040: no seed text, 0.33059735522115824
        // //////////////////////////////////////////////////////////////////////

        // tagger.setTrainingMode(TrainingMode.Complete);
        // tagger.train("data/datasets/ner/conll/training.txt", "data/temp/tudner.model");
        // tagger.train("data/temp/nerEvaluation/www_eval_2_cleansed/allColumn.txt", "data/temp/tudner.model");

        tagger = new PalladianNer(LanguageMode.English, TrainingMode.Complete);

        List<ContextAnnotation> annotations = FileFormatParser.getSeedAnnotations(
                "data/datasets/ner/tud/manuallyPickedSeeds/seedListC.txt", 50);
        // tagger.train(annotations, "data/temp/tudner");
        tagger.train(trainingFilePath, "data/temp/tudner");
        // tagger.train(trainingFilePath, annotations, "data/temp/tudner2.model");
        // System.exit(0);
        // TUDNER.remove = true;
        tagger.loadModel("data/temp/tudner");
        // System.exit(0);

        // tagger = TUDNER.load("data/temp/tudner.model");

        // EvaluationResult er = tagger.evaluate("data/datasets/ner/conll/test_validation.txt",
        // "data/temp/tudner.model",
        // TaggingFormat.COLUMN);
        // EvaluationResult er = tagger.evaluate("data/datasets/ner/conll/test_final.txt", "", TaggingFormat.COLUMN);
        EvaluationResult er = tagger.evaluate(testFilePath, TaggingFormat.COLUMN);
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());

        System.out.println(stopWatch.getElapsedTimeString());

        // Dataset trainingDataset = new Dataset();
        // trainingDataset.setPath("data/datasets/ner/www_test2/index_split1.txt");
        // tagger.train(trainingDataset, "data/models/tudner/tudner.model");
        //
        // Dataset testingDataset = new Dataset();
        // testingDataset.setPath("data/datasets/ner/www_test2/index_split2.txt");
        // EvaluationResult er = tagger.evaluate(testingDataset, "data/models/tudner/tudner.model");
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
    }

}
