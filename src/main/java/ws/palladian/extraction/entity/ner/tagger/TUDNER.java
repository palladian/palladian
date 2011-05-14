package ws.palladian.extraction.entity.ner.tagger;

import java.io.Serializable;
import java.util.ArrayList;
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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Dictionary;
import ws.palladian.classification.Instances;
import ws.palladian.classification.Term;
import ws.palladian.classification.UniversalClassifier;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.classification.numeric.NumericInstance;
import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.TextInstance;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.extraction.entity.ner.Annotation;
import ws.palladian.extraction.entity.ner.Annotations;
import ws.palladian.extraction.entity.ner.FileFormatParser;
import ws.palladian.extraction.entity.ner.NamedEntityRecognizer;
import ws.palladian.extraction.entity.ner.TaggingFormat;
import ws.palladian.extraction.entity.ner.evaluation.EvaluationResult;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.math.Matrix;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.helper.nlp.Tokenizer;
import ws.palladian.preprocessing.nlp.LingPipePOSTagger;
import ws.palladian.preprocessing.nlp.TagAnnotation;
import ws.palladian.preprocessing.nlp.TagAnnotations;
import ws.palladian.tagging.EntityList;
import ws.palladian.tagging.KnowledgeBaseCommunicatorInterface;
import ws.palladian.tagging.StringTagger;

/**
 * TUDLI => token-based, language independent
 * TUDEng => NED + NEC, English only
 * 
 * @author David
 * 
 */
public class TUDNER extends NamedEntityRecognizer implements Serializable {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(TUDNER.class);

    private static final long serialVersionUID = -8793232373094322955L;

    private Dictionary entityDictionary = null;
    private Map<String, Term> entityTermMap = new HashMap<String, Term>();

    /** The classifier to use for classifying the annotations. */
    private UniversalClassifier universalClassifier;

    private DictionaryClassifier contextClassifier;

    /** the connector to the knowledge base */
    private transient KnowledgeBaseCommunicatorInterface kbCommunicator;

    private CountMap leftContextMap = new CountMap();
    // private Map<String, CountMap> rightContextMap = new HashMap<String, CountMap>();

    private Matrix patternProbabilityMatrix = new Matrix();

    private Annotations removeAnnotations = new Annotations();

    private Dictionary caseDictionary = null;
    private Map<String, Term> tokenTermMap = new HashMap<String, Term>();

    // learning features
    private boolean removeDates = true;
    private boolean removeDateEntries = true;
    private boolean removeIncorrectlyTaggedInTraining = true;
    private boolean removeWrongEntityBeginnings = false;
    private boolean removeSentenceStartErrors = false;
    private boolean removeSentenceStartErrors2 = false;
    private boolean removeSingleNonNounEntities = false;
    private boolean switchTagAnnotationsUsingPatterns = true;
    private boolean switchTagAnnotationsUsingDictionary = true;
    private boolean unwrapEntities = true;
    private boolean unwrapEntitiesWithContext = true;
    private boolean retraining = true;

    public static boolean remove = true;

    public enum Mode {
        LanguageIndependent, English
    }

    public enum TrainingMode {
        Complete, Incomplete
    }

    // mode
    private Mode mode = Mode.English;

    // training mode
    private TrainingMode trainingMode = TrainingMode.Complete;

    public TUDNER(Mode mode) {
        this.mode = mode;
        setup();
    }

    public TUDNER() {
        setup();
    }

    private void setup() {
        setName("TUDNER (" + getMode() + ")");

        universalClassifier = new UniversalClassifier();
        universalClassifier.getTextClassifier().getClassificationTypeSetting()
        .setClassificationType(ClassificationTypeSetting.TAG);
        universalClassifier.getNumericClassifier().getClassificationTypeSetting()
        .setClassificationType(ClassificationTypeSetting.TAG);
        universalClassifier.getNominalClassifier().getClassificationTypeSetting()
        .setClassificationType(ClassificationTypeSetting.TAG);

        universalClassifier.getTextClassifier().getDictionary().setName("dictionary");
        // universalClassifier.getTextClassifier().getDictionary().setCaseSensitive(true);
        universalClassifier.getTextClassifier().getFeatureSetting().setMinNGramLength(3); // FIXME was 2
        universalClassifier.getTextClassifier().getFeatureSetting().setMaxNGramLength(5); // FIXME was 8

        universalClassifier.switchClassifiers(true, false, false);

        entityDictionary = new Dictionary("EntityDictionary", ClassificationTypeSetting.SINGLE);
        entityDictionary.setCaseSensitive(true);

        caseDictionary = new Dictionary("CaseDictionary", ClassificationTypeSetting.SINGLE);
        caseDictionary.setCaseSensitive(false);

        contextClassifier = new DictionaryClassifier();
        contextClassifier.getClassificationTypeSetting().setClassificationType(ClassificationTypeSetting.TAG);
        contextClassifier.getDictionary().setName("contextDictionary");
        contextClassifier.getFeatureSetting().setMinNGramLength(3); // FIXME was 5
        contextClassifier.getFeatureSetting().setMaxNGramLength(5); // FIXME was 8
    }

    @Override
    public String getModelFileEnding() {
        return "model";
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        return false;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        StopWatch stopWatch = new StopWatch();

        TUDNER n = (TUDNER) FileHelper.deserialize(configModelFilePath);

        this.entityDictionary = n.entityDictionary;
        this.entityTermMap = n.entityTermMap;

        this.caseDictionary = n.caseDictionary;
        this.tokenTermMap = n.tokenTermMap;

        this.universalClassifier = n.universalClassifier;

        this.contextClassifier = n.contextClassifier;

        this.kbCommunicator = n.kbCommunicator;

        this.leftContextMap = n.leftContextMap;

        this.patternProbabilityMatrix = n.patternProbabilityMatrix;

        this.removeAnnotations = n.removeAnnotations;

        // learning features
        this.removeDates = n.removeDates;
        this.removeDateEntries = n.removeDateEntries;
        this.removeIncorrectlyTaggedInTraining = n.removeIncorrectlyTaggedInTraining;
        this.removeWrongEntityBeginnings = n.removeWrongEntityBeginnings;
        this.removeSentenceStartErrors = n.removeSentenceStartErrors;
        this.removeSentenceStartErrors2 = n.removeSentenceStartErrors2;
        this.removeSingleNonNounEntities = n.removeSingleNonNounEntities;
        this.switchTagAnnotationsUsingPatterns = n.switchTagAnnotationsUsingPatterns;
        this.switchTagAnnotationsUsingDictionary = n.switchTagAnnotationsUsingDictionary;
        this.unwrapEntities = n.unwrapEntities;
        this.unwrapEntitiesWithContext = n.unwrapEntitiesWithContext;

        TUDNER.remove = n.remove;

        setModel(this);
        LOGGER.info("model " + configModelFilePath + " successfully loaded in " + stopWatch.getElapsedTimeString());

        return true;
    }

    public static TUDNER load(String modelPath) {

        LOGGER.info("deserialzing model from " + modelPath);

        TUDNER tagger;

        tagger = (TUDNER) FileHelper.deserialize(modelPath);

        LOGGER.info("loaded tagger");

        return tagger;
    }

    protected void saveModel(String modelFilePath) {

        LOGGER.info("entity dictionary contains " + entityDictionary.size() + " entities");
        entityDictionary.saveAsCSV();

        LOGGER.info("case dictionary contains " + caseDictionary.size() + " entities");
        caseDictionary.saveAsCSV();

        LOGGER.info("serializing NERCer");
        FileHelper.serialize(this, modelFilePath);

        LOGGER.info("dictionary size: " + universalClassifier.getTextClassifier().getDictionary().size());

        // write model meta information
        StringBuilder supportedConcepts = new StringBuilder();
        for (Category c : universalClassifier.getTextClassifier().getDictionary().getCategories()) {
            supportedConcepts.append(c.getName()).append("\n");
        }

        FileHelper.writeToFile(FileHelper.getFilePath(modelFilePath) + FileHelper.getFileName(modelFilePath)
                + "_meta.txt", supportedConcepts);
        LOGGER.info("model meta information written");
    }

    /**
     * Save training entities in a dedicated dictionary.
     * 
     * @param annotation The complete annotation from the training data.
     */
    private void addToEntityDictionary(Annotation annotation) {
        String en = annotation.getEntity();
        Term term = entityTermMap.get(en);
        if (term == null) {
            term = new Term(en);
            entityTermMap.put(en, term);
        }
        entityDictionary.updateWord(term, annotation.getInstanceCategoryName(), 1);
    }

    private void addToCaseDictionary(String token) {
        token = StringHelper.trim(token);
        if (token.length() < 2) {
            return;
        }
        String caseSignature = StringHelper.getCaseSignature(token);
        if (caseSignature.equals("Aa") || caseSignature.equals("A") || caseSignature.equals("a")) {
            token = token.toLowerCase();
            Term term = tokenTermMap.get(token);
            if (term == null) {
                term = new Term(token);
                tokenTermMap.put(token, term);
            }
            caseDictionary.updateWord(term, caseSignature, 1);
        }
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {

        if (mode.equals(Mode.English)) {
            return trainEnglish(trainingFilePath, modelFilePath);
        } else {
            return trainLanguageIndependent(trainingFilePath, modelFilePath);
        }

    }

    public boolean train(String trainingFilePath, Annotations annotations, String modelFilePath) {

        // create instances, instances are annotations
        Instances<UniversalInstance> textInstances = new Instances<UniversalInstance>();

        LOGGER.info("start creating " + annotations.size() + " annotations for training");
        for (Annotation annotation : annotations) {
            UniversalInstance textInstance = new UniversalInstance(textInstances);
            textInstance.setTextFeature(annotation.getEntity());
            textInstance.setInstanceCategory(annotation.getInstanceCategory());
            textInstances.add(textInstance);
        }

        // save training entities in a dedicated dictionary
        for (Annotation annotation : annotations) {
            addToEntityDictionary(annotation);
        }

        universalClassifier.getTextClassifier().setTrainingInstances(textInstances);

        return train(trainingFilePath, modelFilePath);
    }

    public boolean train(Annotations annotations, String modelFilePath) {
        return trainLanguageIndependent(annotations, annotations, modelFilePath);
    }

    public boolean trainLanguageIndependent(Annotations annotations, Annotations combinedAnnotations,
            String modelFilePath) {

        // create instances, instances are annotations
        Instances<UniversalInstance> textInstances = new Instances<UniversalInstance>();

        LOGGER.info("start creating " + annotations.size() + " annotations for training");
        for (Annotation annotation : annotations) {
            UniversalInstance textInstance = new UniversalInstance(textInstances);
            textInstance.setTextFeature(annotation.getEntity());
            textInstance.setInstanceCategory(annotation.getInstanceCategory());
            textInstances.add(textInstance);
        }

        // save training entities in a dedicated dictionary
        for (Annotation annotation : combinedAnnotations) {
            addToEntityDictionary(annotation);
        }

        // train the text classifier
        universalClassifier.getTextClassifier().setTrainingInstances(textInstances);

        LOGGER.info("start training classifiers now...");
        universalClassifier.trainAll();

        // XXX can this be switched on? training with annotations only?
        // analyzeContexts(trainingFilePath);

        saveModel(modelFilePath);

        return true;
    }

    public boolean trainLanguageIndependent(String trainingFilePath, String modelFilePath) {

        // get all training annotations including their features
        Annotations annotations = FileFormatParser.getAnnotationsFromColumnTokenBased(trainingFilePath);

        // get annotations combined, e.g. "Phil Simmons", not "Phil" and "Simmons"
        Annotations combinedAnnotations = FileFormatParser.getAnnotationsFromColumn(trainingFilePath);

        return trainLanguageIndependent(annotations, combinedAnnotations, modelFilePath);
    }

    public boolean trainEnglish(String trainingFilePath, String modelFilePath) {

        // get all training annotations including their features
        Annotations annotations = FileFormatParser.getAnnotationsFromColumn(trainingFilePath);

        // create instances with nominal and numeric features
        Instances<UniversalInstance> textInstances = new Instances<UniversalInstance>();
        Instances<UniversalInstance> nominalInstances = new Instances<UniversalInstance>();
        Instances<NumericInstance> numericInstances = new Instances<NumericInstance>();

        // Set<String> seenEntityCategoryCombinations = new HashSet<String>();

        for (Annotation annotation : annotations) {

            // String entityCategoryCombination = annotation.getEntity() + "_" + annotation.getInstanceCategoryName();

            // if (seenEntityCategoryCombinations.add(entityCategoryCombination)) {
                UniversalInstance textInstance = new UniversalInstance(textInstances);
                textInstance.setTextFeature(annotation.getEntity());
                textInstance.setInstanceCategory(annotation.getInstanceCategory());
                textInstances.add(textInstance);
            // }

            UniversalInstance nominalInstance = new UniversalInstance(nominalInstances);
            nominalInstance.setNominalFeatures(annotation.getNominalFeatures());
            nominalInstance.setInstanceCategory(annotation.getInstanceCategory());
            nominalInstances.add(nominalInstance);

            NumericInstance numericInstance = new NumericInstance(numericInstances);
            numericInstance.setFeatures(annotation.getNumericFeatures());
            numericInstance.setInstanceCategory(annotation.getInstanceCategory());
            numericInstances.add(numericInstance);

            addToEntityDictionary(annotation);
        }

        // train the text classifier
        // universalClassifier.getTextClassifier().setTrainingInstances(textInstances); FIXME uncommented to test next
        // line
        universalClassifier.getTextClassifier().addTrainingInstances(textInstances);

        // train the numeric classifier with numeric features from the annotations
        universalClassifier.getNumericClassifier().setTrainingInstances(numericInstances);
        universalClassifier.getNumericClassifier().getTrainingInstances().normalize();

        // train the nominal classifier with nominal features from the annotations
        universalClassifier.getNominalClassifier().setTrainingInstances(nominalInstances);

        // fill the case dictionary
        List<String> tokens = Tokenizer.tokenize(FileFormatParser.getText(trainingFilePath, TaggingFormat.COLUMN));
        for (String token : tokens) {
            addToCaseDictionary(token);
        }

        if (retraining) {
            // //////////////////////////////////////////// wrong entities //////////////////////////////////////
            universalClassifier.trainAll();
            saveModel(modelFilePath);
            // String inputText = FileFormatParser.getText(trainingFilePath, TaggingFormat.COLUMN);
            // Annotations entityCandidates = StringTagger.getTaggedEntities(inputText);
            // Annotations classifiedAnnotations = verifyAnnotationsWithNumericClassifier(entityCandidates, inputText);
            removeAnnotations = new Annotations();
            EvaluationResult evaluationResult = evaluate(trainingFilePath, modelFilePath, TaggingFormat.COLUMN);

            // get only those annotations that were incorrectly tagged and were never a real entity that is they have to
            // be in ERROR1 set and NOT in the gold standard
            for (Annotation wrongAnnotation : evaluationResult.getErrorAnnotations().get(EvaluationResult.ERROR1)) {

                // for the numeric classifier it is better if only annotations are removed that never appeared in the
                // gold
                // standard
                // for the text classifier it is better to remove annotations that are just wrong even when they were
                // correct in the gold standard at some point
                boolean addAnnotationNumeric = true;

                // check if annotation happens to be in the gold standard, if so, do not declare it completely wrong
                String wrongName = wrongAnnotation.getEntity().toLowerCase();
                for (Annotation gsAnnotation : evaluationResult.getGoldStandardAnnotations()) {
                    if (wrongName.equals(gsAnnotation.getEntity().toLowerCase())) {
                        addAnnotationNumeric = false;
                        break;
                    }
                }

                UniversalInstance textInstance = new UniversalInstance(textInstances);
                textInstance.setTextFeature(wrongAnnotation.getEntity());
                textInstance.setInstanceCategory("###NO_ENTITY###");
                textInstances.add(textInstance);

                if (addAnnotationNumeric) {
                    NumericInstance numericInstance = new NumericInstance(numericInstances);
                    numericInstance.setFeatures(wrongAnnotation.getNumericFeatures());
                    numericInstance.setInstanceCategory("###NO_ENTITY###");
                    numericInstances.add(numericInstance);

                    removeAnnotations.add(wrongAnnotation);
                }
            }
            System.out.println(removeAnnotations.size() + " annotations need to be completely removed");
            // //////////////////////////////////////////////////////////////////////////////////////////////////
        }

        universalClassifier.getNumericClassifier().setTrainingInstances(numericInstances);
        universalClassifier.getNumericClassifier().getTrainingInstances().normalize();

        universalClassifier.getNominalClassifier().setTrainingInstances(nominalInstances);

        universalClassifier.getTextClassifier().setTrainingInstances(textInstances);

        universalClassifier.trainAll();

        analyzeContexts(trainingFilePath);

        saveModel(modelFilePath);

        return true;
    }

    private Annotations classifyCandidatesEnglish(Annotations entityCandidates, String inputText) {
        Annotations annotations = new Annotations();

        int i = 0;
        for (Annotation annotation : entityCandidates) {

            Annotations wrappedAnnotations = new Annotations();

            if (unwrapEntities) {
                wrappedAnnotations = annotation.unwrapAnnotations(annotations, entityDictionary);
            }

            if (!wrappedAnnotations.isEmpty()) {
                for (Annotation annotation2 : wrappedAnnotations) {
                    if (!annotation2.getMostLikelyTagName().equalsIgnoreCase("###NO_ENTITY###")) {
                        annotations.add(annotation2);
                    }
                }
                // System.out.print("tried to unwrap " + annotation.getEntity());
                // for (Annotation wrappedAnnotation : wrappedAnnotations) {
                // System.out.print(" | " + wrappedAnnotation.getEntity());
                // }
                // System.out.print("\n");
                // toRemove.add(annotation);
            } else {
                universalClassifier.classify(annotation);
                if (!annotation.getMostLikelyTagName().equalsIgnoreCase("###NO_ENTITY###")) {
                    annotations.add(annotation);
                }
            }

            if (i % 100 == 0) {
                LOGGER.info("classified " + MathHelper.round(100 * i / entityCandidates.size(), 0) + "%");
            }
            i++;
        }

        return annotations;
    }

    private Annotations classifyCandidatesLanguageIndependent(Annotations entityCandidates) {
        Annotations annotations = new Annotations();

        int i = 0;
        for (Annotation annotation : entityCandidates) {

            universalClassifier.classify(annotation);
            if (!annotation.getMostLikelyTagName().equalsIgnoreCase("###NO_ENTITY###")) {
                annotations.add(annotation);
            }

            if (i % 100 == 0) {
                LOGGER.info("classified " + MathHelper.round(100 * i / entityCandidates.size(), 0) + "%");
            }
            i++;
        }

        return annotations;
    }

    @Override
    public Annotations getAnnotations(String inputText) {
        StopWatch stopWatch = new StopWatch();

        Annotations annotations = new Annotations();

        if (mode.equals(Mode.English)) {
            annotations = getAnnotationsEnglish(inputText);
        } else {
            annotations = getAnnotationsLanguageIndependent(inputText);
        }

        FileHelper.writeToFile("data/test/ner/tudNEROutput.txt", tagText(inputText, annotations));

        LOGGER.info("got annotations in " + stopWatch.getElapsedTimeString());

        return annotations;
    }

    private void filterAnnotations(Annotations annotations) {

        LOGGER.info("start filtering annotations");

        StopWatch stopWatch = new StopWatch();

        Annotations toRemove = new Annotations();

        // remove dates
        if (removeDates) {
            stopWatch.start();
            int c = 0;
            for (Annotation annotation : annotations) {
                if (containsDateFragment(annotation.getEntity())) {
                    toRemove.add(annotation);
                    c++;
                }
            }
            LOGGER.info("removed " + c + " purely date annotations in " + stopWatch.getElapsedTimeString());
        }

        // remove date entries in annotations, such as "July Peter Jackson" => "Peter Jackson"
        if (removeDateEntries) {
            stopWatch.start();
            int c = 0;
            for (Annotation annotation : annotations) {

                Object[] result = removeDateFragment(annotation.getEntity());
                String entity = (String) result[0];

                annotation.setEntity(entity);
                annotation.setOffset(annotation.getOffset() + (Integer) result[1]);
                annotation.setLength(annotation.getEntity().length());

                if ((Integer) result[1] > 0) {
                    c++;
                }
            }
            LOGGER.info("removed " + c + " partial date annotations in " + stopWatch.getElapsedTimeString());
        }

        // remove annotations that were found to be incorrectly tagged in the training data
        if (removeIncorrectlyTaggedInTraining) {
            stopWatch.start();
            for (Annotation removeAnnotation : removeAnnotations) {
                String removeName = removeAnnotation.getEntity().toLowerCase();
                for (Annotation annotation : annotations) {
                    if (removeName.equals(annotation.getEntity().toLowerCase())) {
                        toRemove.add(annotation);
                    }
                }
            }
            LOGGER.info("removed " + removeAnnotations.size() + " incorrectly tagged entities in training data in "
                    + stopWatch.getElapsedTimeString());
        }

        // rule-based removal of possibly wrong beginnings of entities, for example "In Ireland" => "Ireland"
        LingPipePOSTagger lpt = new LingPipePOSTagger();
        lpt.loadModel();

        if (removeWrongEntityBeginnings) {
            stopWatch.start();
            for (Annotation annotation : annotations) {

                // if annotation starts at sentence AND if first token of entity has POS tag != NP, NN, JJ, and UH,
                // remove it
                String[] entityParts = annotation.getEntity().split(" ");
                if (entityParts.length > 1 && Boolean.valueOf(annotation.getNominalFeatures().get(0))) {
                    TagAnnotations ta = lpt.tag(entityParts[0]).getTagAnnotations();
                    if (ta.size() == 1 && ta.get(0).getTag().indexOf("NP") == -1
                            && ta.get(0).getTag().indexOf("NN") == -1 && ta.get(0).getTag().indexOf("JJ") == -1
                            && ta.get(0).getTag().indexOf("UH") == -1) {

                        StringBuilder shortEntity = new StringBuilder();
                        for (int i = 1; i < entityParts.length; i++) {
                            shortEntity.append(entityParts[i]).append(" ");
                        }

                        annotation.setEntity(shortEntity.toString().trim());
                        annotation.setOffset(annotation.getOffset() + entityParts[0].length() + 1);
                        annotation.setLength(annotation.getEntity().length());
                        LOGGER.debug("removing beginning: " + entityParts[0] + " => " + annotation.getEntity());
                    }
                }

            }

            LOGGER.info("removed wrong entity beginnings in " + stopWatch.getElapsedTimeString());
        }

        // remove annotations which are at the beginning of a sentence, are some kind of noun but because of the
        // following POS tag probably not an entity
        int c = 0;
        if (removeSentenceStartErrors) {
            stopWatch.start();

            for (Annotation annotation : annotations) {

                // if the annotation is at the start of a sentence
                if (Boolean.valueOf(annotation.getNominalFeatures().get(0))
                        && annotation.getEntity().indexOf(" ") == -1) {

                    TagAnnotations ta = lpt.tag(annotation.getEntity()).getTagAnnotations();
                    if (ta.size() >= 1 && ta.get(0).getTag().indexOf("NP") == -1
                            && ta.get(0).getTag().indexOf("NN") == -1 && ta.get(0).getTag().indexOf("JJ") == -1
                            && ta.get(0).getTag().indexOf("UH") == -1) {
                        continue;
                    }

                    String[] rightContextParts = annotation.getRightContext().split(" ");

                    if (rightContextParts.length == 0) {
                        continue;
                    }

                    ta = lpt.tag(rightContextParts[0]).getTagAnnotations();

                    Set<String> allowedPosTags = new HashSet<String>();
                    allowedPosTags.add("CD");
                    allowedPosTags.add("VB");
                    allowedPosTags.add("VBZ");
                    allowedPosTags.add("VBD");
                    allowedPosTags.add("VBN");
                    allowedPosTags.add("MD");
                    allowedPosTags.add("RB");
                    allowedPosTags.add("NN");
                    allowedPosTags.add("NNS");
                    allowedPosTags.add("NP");
                    allowedPosTags.add("HV");
                    allowedPosTags.add("HVD");
                    allowedPosTags.add("HVZ");
                    allowedPosTags.add("BED");
                    allowedPosTags.add("BER");
                    allowedPosTags.add("BEZ");
                    allowedPosTags.add("BEDZ");
                    allowedPosTags.add(",");
                    allowedPosTags.add("(");
                    allowedPosTags.add("-");
                    allowedPosTags.add("--");
                    allowedPosTags.add(".");
                    allowedPosTags.add("CC");
                    allowedPosTags.add("'");
                    allowedPosTags.add("AP");

                    if (ta.size() > 0 && !allowedPosTags.contains(ta.get(0).getTag())) {
                        c++;
                        toRemove.add(annotation);
                        LOGGER.debug("remove noun at beginning of sentence: " + annotation.getEntity() + "|"
                                + rightContextParts[0] + "|" + ta.get(0).getTag());
                    }

                }
            }

            LOGGER.info("removed " + c + " nouns at beginning of sentence in " + stopWatch.getElapsedTimeString());
        }

        if (removeSentenceStartErrors2) {
            stopWatch.start();

            for (Annotation annotation : annotations) {

                // if the annotation is at the start of a sentence
                if (/*
                 * Boolean.valueOf(annotation.getNominalFeatures().get(0))
                 * &&
                 */annotation.getEntity().indexOf(" ") == -1) {

                    double upperCaseToLowerCaseRatio = 2;

                    // if (annotation.getEntity().equals("NATO")) {
                    // System.out.println("wait");
                    // }

                    CategoryEntries ces = caseDictionary.get(tokenTermMap.get(annotation.getEntity().toLowerCase()));
                    if (ces != null && ces.size() > 0) {
                        double allUpperCase = 0.0;
                        double upperCase = 0.0;
                        double lowerCase = 0.0;

                        if (ces.getCategoryEntry("A") != null) {
                            allUpperCase = ces.getCategoryEntry("A").getRelevance();
                        }

                        if (ces.getCategoryEntry("Aa") != null) {
                            upperCase = ces.getCategoryEntry("Aa").getRelevance();
                        }

                        if (ces.getCategoryEntry("a") != null) {
                            lowerCase = ces.getCategoryEntry("a").getRelevance();
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
                        LOGGER.debug("remove word at beginning of sentence: " + annotation.getEntity() + " (ratio:"
                                + upperCaseToLowerCaseRatio + ") | "
                                + annotation.getRightContext());
                    }

                }
            }

            LOGGER.info("removed " + c + " words at beginning of sentence in " + stopWatch.getElapsedTimeString());
        }

        // remove entities which contain only one word which is not a noun
        if (removeSingleNonNounEntities) {
            stopWatch.start();

            c = 0;
            for (Annotation annotation : annotations) {

                TagAnnotations ta = lpt.tag(annotation.getEntity()).getTagAnnotations();
                if (ta.size() == 1 && ta.get(0).getTag().indexOf("NP") == -1 && ta.get(0).getTag().indexOf("NN") == -1
                        && ta.get(0).getTag().indexOf("JJ") == -1 && ta.get(0).getTag().indexOf("UH") == -1) {
                    toRemove.add(annotation);
                    c++;
                    // LOGGER.debug("removing: " + annotation.getEntity() + " has POS tag: " +
                    // ta.get(0).getTag());
                }

            }

            LOGGER.info("removed " + c + " non-noun entities in " + stopWatch.getElapsedTimeString());
        }

        LOGGER.info("remove " + toRemove.size() + " entities");
        annotations.removeAll(toRemove);

        // switch using pattern information
        int changed = 0;
        if (switchTagAnnotationsUsingPatterns) {
            stopWatch.start();

            for (Annotation annotation : annotations) {

                String tagNameBefore = annotation.getMostLikelyTagName();

                getMostLikelyTag(annotation, lpt);

                if (!annotation.getMostLikelyTagName().equalsIgnoreCase(tagNameBefore)) {
                    LOGGER.debug("changed " + annotation.getEntity() + " from " + tagNameBefore + " to "
                            + annotation.getMostLikelyTagName() + ", left context: " + annotation.getLeftContext()
                            + "____" + annotation.getRightContext());
                    changed++;
                }

            }
            LOGGER.info("changed " + MathHelper.round(100 * changed / annotations.size(), 2)
                    + "% of the entities using patterns in " + stopWatch.getElapsedTimeString());

        }

        // switch annotations that are in the dictionary
        changed = 0;
        if (switchTagAnnotationsUsingDictionary) {
            stopWatch.start();

            for (Annotation annotation : annotations) {

                CategoryEntries ces = entityDictionary.get(entityTermMap.get(annotation.getEntity()));
                // CategoryEntries ces = entityDictionary.get(annotation.getEntity());
                if (ces != null && ces.size() > 0) {
                    annotation.assignCategoryEntries(ces);
                    changed++;
                }

            }
            LOGGER.info("changed with entity dictionary " + MathHelper.round(100 * changed / annotations.size(), 2)
                    + "% of the entities (total entities: " + annotations.size() + ") in "
                    + stopWatch.getElapsedTimeString());
        }

        // remove all annotations with "DOCSTART- " in them because that is for format purposes
        Annotations toAdd = new Annotations();

        stopWatch.start();
        LinkedHashMap<Object, Integer> sortedMap = leftContextMap.getSortedMapDescending();

        for (Annotation annotation : annotations) {
            if (annotation.getEntity().toLowerCase().equals("docstart")) {
                toRemove.add(annotation);
                continue;
            }

            // if all uppercase, try to find known annotations
            // if (StringHelper.isCompletelyUppercase(annotation.getEntity().substring(10,
            // Math.min(12, annotation.getEntity().length())))) {

            if (unwrapEntities) {
                Annotations wrappedAnnotations = annotation.unwrapAnnotations(annotations, entityDictionary);

                if (!wrappedAnnotations.isEmpty()) {
                    for (Annotation annotation2 : wrappedAnnotations) {
                        if (!annotation2.getMostLikelyTagName().equalsIgnoreCase("###NO_ENTITY###")) {
                            toAdd.add(annotation2);
                            // LOGGER.debug("add " + annotation2.getEntity());
                        }
                    }
                    String debugString = "tried to unwrap again " + annotation.getEntity();
                    for (Annotation wrappedAnnotation : wrappedAnnotations) {
                        debugString += " | " + wrappedAnnotation.getEntity();
                    }
                    debugString += "\n";
                    LOGGER.debug(debugString);
                }
            }

            // unwrap annotations containing context patterns, e.g. "President Obama" => "President" is known left
            // context for people

            // TODO move this up?
            if (unwrapEntitiesWithContext) {
                for (Entry<Object, Integer> leftContextEntry : sortedMap.entrySet()) {

                    String leftContext = leftContextEntry.getKey().toString();

                    // FIXME: this is a magic number, not good!
                    if (leftContextEntry.getValue() < 31) {
                        break;
                    }

                    if (!StringHelper.startsUppercase(leftContext)) {
                        continue;
                    }

                    String entity = annotation.getEntity();

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
                        Annotation wrappedAnnotation = new Annotation(annotation.getOffset() + index + length,
                                annotation.getEntity().substring(index + length), annotation.getMostLikelyTagName(),
                                annotations);
                        toAdd.add(wrappedAnnotation);

                        // search for a known instance in the prefix
                        // go through the entity dictionary
                        for (Map.Entry<Term, CategoryEntries> termEntry : entityDictionary.entrySet()) {
                            String word = termEntry.getKey().getText();

                            int indexPrefix = annotation.getEntity().substring(0, index + length).indexOf(word + " ");
                            if (indexPrefix > -1 && word.length() > 2) {
                                Annotation wrappedAnnotation2 = new Annotation(annotation.getOffset() + indexPrefix,
                                        word,
                                        termEntry.getValue().getMostLikelyCategoryEntry().getCategory().getName(),
                                        annotations);
                                toAdd.add(wrappedAnnotation2);
                                LOGGER.debug("add from prefix " + wrappedAnnotation2.getEntity());
                                break;
                            }

                        }

                        toRemove.add(annotation);
                        LOGGER.debug("add " + wrappedAnnotation.getEntity() + ", delete " + annotation.getEntity()
                                + " (left context:" + leftContext + ", " + leftContextEntry.getValue() + ")");

                        break;
                    }

                }
            }
        }
        LOGGER.info("unwrapped entities in " + stopWatch.getElapsedTimeString());

        LOGGER.info("add " + toAdd.size() + " entities");
        annotations.addAll(toAdd);

        LOGGER.info("remove " + toRemove.size() + " entities");
        annotations.removeAll(toRemove);
    }

    public Annotations getAnnotationsEnglish(String inputText) {

        Annotations annotations = new Annotations();

        Annotations entityCandidates = StringTagger.getTaggedEntities(inputText);

        // Annotations kbRecognizedAnnotations = verifyEntitiesWithKB(entityCandidates);
        // annotations.addAll(kbRecognizedAnnotations);
        //
        // Annotations patternRecognizedAnnotations = verifyEntitiesWithPattern(entityCandidates, inputText);
        // annotations.addAll(patternRecognizedAnnotations);

        // Annotations dictionaryRecognizedAnnotations = verifyEntitiesWithDictionary(entityCandidates, inputText);
        // annotations.addAll(dictionaryRecognizedAnnotations);

        // classify annotations with the UniversalClassifier
        annotations.addAll(classifyCandidatesEnglish(entityCandidates, inputText));

        if (remove) {
            filterAnnotations(annotations);
        }

        // FileHelper.writeToFile("data/test/ner/palladianNEROutput.txt", tagText(inputText, annotations));

        return annotations;
    }

    public Annotations getAnnotationsLanguageIndependent(String inputText) {

        removeDates = false;
        removeDateEntries = false;
        removeIncorrectlyTaggedInTraining = false;
        removeWrongEntityBeginnings = false;
        removeSentenceStartErrors = false;
        removeSingleNonNounEntities = false;
        switchTagAnnotationsUsingPatterns = false;
        switchTagAnnotationsUsingDictionary = true;
        unwrapEntities = false;
        unwrapEntitiesWithContext = false;

        Annotations annotations = new Annotations();

        // get the candates, every token is potentially a (part of) an entity
        Annotations entityCandidates = StringTagger.getTaggedEntities(inputText, Tokenizer.SPLIT_REGEXP);

        // classify annotations with the UniversalClassifier
        annotations.addAll(classifyCandidatesLanguageIndependent(entityCandidates));

        // filter annotations
        filterAnnotations(annotations);

        // combine annotations that are right next to each other having the same tag
        Annotations combinedAnnotations = new Annotations();
        annotations.sort();
        Annotation lastAnnotation = new Annotation(-2, "", "");
        Annotation lastCombinedAnnotation = null;

        for (Annotation annotation : annotations) {
            if (!annotation.getMostLikelyTagName().equalsIgnoreCase("o")
                    && annotation.getMostLikelyTagName().equalsIgnoreCase(lastAnnotation.getMostLikelyTagName())
                    && annotation.getOffset() == lastAnnotation.getEndIndex() + 1) {

                if (lastCombinedAnnotation == null) {
                    lastCombinedAnnotation = lastAnnotation;
                }

                Annotation combinedAnnotation = new Annotation(lastCombinedAnnotation.getOffset(),
                        lastCombinedAnnotation.getEntity() + " " + annotation.getEntity(),
                        annotation.getMostLikelyTagName(), combinedAnnotations);
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
        Annotations cleanAnnotations = new Annotations();
        for (Annotation annotation : combinedAnnotations) {
            if (!annotation.getMostLikelyTagName().equalsIgnoreCase("o") && annotation.getLength() > 1) {
                cleanAnnotations.add(annotation);
            }
        }

        // FileHelper.writeToFile("data/temp/tudNER2Output.txt", tagText(inputText, cleanAnnotations));

        return cleanAnnotations;
    }

    private void getMostLikelyTag(Annotation annotation, LingPipePOSTagger lpt) {

        String[] leftContexts = annotation.getLeftContexts();
        String[] rightContexts = annotation.getRightContexts();

        List<String> contexts = new ArrayList<String>();
        for (String pattern : leftContexts) {
            contexts.add(pattern);
        }
        for (String pattern : rightContexts) {
            contexts.add(pattern);
        }

        double locProb = 1.0;
        double perProb = 1.0;
        double orgProb = 1.0;
        double miscProb = 1.0;

        String onePatternOnlyFor = "";
        int threshold = 2;
        int found = 0;
        for (String contextPattern : contexts) {

            contextPattern = contextPattern.toLowerCase();

            if (contextPattern.length() == 0 || StringHelper.countWhitespaces(contextPattern) < 0) {
                continue;
            }

            Integer lp = (Integer) patternProbabilityMatrix.get("LOC", contextPattern);
            if (lp == null) {
                lp = 0;
            }

            Integer pp = (Integer) patternProbabilityMatrix.get("PER", contextPattern);
            if (pp == null) {
                pp = 0;
            }

            Integer op = (Integer) patternProbabilityMatrix.get("ORG", contextPattern);
            if (op == null) {
                op = 0;
            }

            Integer mp = (Integer) patternProbabilityMatrix.get("MISC", contextPattern);
            if (mp == null) {
                mp = 0;
            }

            // locProb += lp / 7119.0;
            // perProb += pp / 6560.0;
            // orgProb += op / 6276.0;
            // miscProb += mp / 3371.0;

            int sum = lp + pp + op + mp;

            if (sum > 0) {
                found++;
            } else {
                continue;
            }

            if (lp == sum && sum >= threshold) {
                if (onePatternOnlyFor.length() == 0) {
                    onePatternOnlyFor = "LOC";
                } else if (!onePatternOnlyFor.equalsIgnoreCase("LOC")) {
                    onePatternOnlyFor = "-";
                }
            } else if (pp == sum && sum >= threshold) {
                if (onePatternOnlyFor.length() == 0) {
                    onePatternOnlyFor = "PER";
                } else if (!onePatternOnlyFor.equalsIgnoreCase("PER")) {
                    onePatternOnlyFor = "-";
                }
            } else if (op == sum && sum >= threshold) {
                if (onePatternOnlyFor.length() == 0) {
                    onePatternOnlyFor = "ORG";
                } else if (!onePatternOnlyFor.equalsIgnoreCase("ORG")) {
                    onePatternOnlyFor = "-";
                }
            } else if (mp == sum && sum >= threshold) {
                if (onePatternOnlyFor.length() == 0) {
                    onePatternOnlyFor = "MISC";
                } else if (!onePatternOnlyFor.equalsIgnoreCase("MISC")) {
                    onePatternOnlyFor = "-";
                }
            }

            // locProb *= (0.0000000001 + lp) / sum;
            // perProb *= (0.0000000001 + pp) / sum;
            // orgProb *= (0.0000000001 + op) / sum;
            // miscProb *= (0.0000000001 + mp) / sum;

            locProb += lp / (double) sum;
            perProb += pp / (double) sum;
            orgProb += op / (double) sum;
            miscProb += mp / (double) sum;

            // locProb += lp / 7119.0 * lp / sum;
            // perProb += pp / 6560.0 * pp / sum;
            // orgProb += op / 6276.0 * op / sum;
            // miscProb += mp / 3371.0 * mp / sum;

        }

        if (found == 0) {
            return;
        }
        CategoryEntries ce = new CategoryEntries();

        ce.add(new CategoryEntry(ce, new Category("LOC"), locProb));
        ce.add(new CategoryEntry(ce, new Category("PER"), perProb));
        ce.add(new CategoryEntry(ce, new Category("ORG"), orgProb));
        ce.add(new CategoryEntry(ce, new Category("MISC"), miscProb));

        String contextString = "";

        TagAnnotations tas = lpt.tag(annotation.getLeftContext() + "__" + annotation.getRightContext())
        .getTagAnnotations();
        for (TagAnnotation ta : tas) {
            contextString += ta.getTag() + " ";
        }
        contextString = contextString.trim();

        TextInstance ti = contextClassifier.classify(annotation.getLeftContext() + "__" + annotation.getRightContext());

        // UniversalInstance bayesClassifiedInstance = new UniversalInstance(null);
        // bayesClassifiedInstance.setNominalFeatures(annotation.getNominalFeatures());
        // universalClassifier.getNominalClassifier().classify(bayesClassifiedInstance);

        CategoryEntries ceMerge = new CategoryEntries();
        // ceMerge.addAllRelative(bayesClassifiedInstance.getAssignedCategoryEntries());
        ceMerge.addAllRelative(ce);
        ceMerge.addAllRelative(annotation.getAssignedCategoryEntries());
        ceMerge.addAllRelative(ti.getAssignedCategoryEntries());
        annotation.assignCategoryEntries(ceMerge);
    }

    @Override
    public Annotations getAnnotations(String inputText, String modelPath) {
        loadModel(modelPath);
        return getAnnotations(inputText);
    }

    public EntityList getTrainingEntities(double percentage) {
        if (kbCommunicator == null) {
            LOGGER.debug("could not get training entities because no KnowledgeBaseCommunicator has been defined");
            return new EntityList();
        }
        return kbCommunicator.getTrainingEntities(percentage);
    }

    private boolean containsDateFragment(String text) {
        text = text.toLowerCase();
        String[] regExps = RegExp.getDateFragmentRegExp();

        for (String regExp : regExps) {
            if (text.replaceAll(regExp.toLowerCase(), "").trim().isEmpty()) {
                return true;
            }

        }

        return false;
    }

    private Object[] removeDateFragment(String text) {
        String[] regExps = RegExp.getDateFragmentRegExp();

        Object[] result = new Object[2];
        int offsetChange = 0;

        for (String regExp : regExps) {
            int textLength = text.length();

            // for example "Apr John Hiatt"
            if (StringHelper.countOccurences(text, "^" + regExp + " ", false) > 0) {
                text = text.replaceAll("^" + regExp + " ", "").trim();
                offsetChange += textLength - text.length();
            }
            if (StringHelper.countOccurences(text, " " + regExp + "$", false) > 0) {
                text = text.replaceAll(" " + regExp + "$", "").trim();
            }

            // for example "Apr. John Hiatt"
            if (StringHelper.countOccurences(text, "^" + regExp + "\\. ", false) > 0) {
                text = text.replaceAll("^" + regExp + "\\. ", "").trim();
                offsetChange += textLength - text.length();
            }
            if (StringHelper.countOccurences(text, " " + regExp + "\\.$", false) > 0) {
                text = text.replaceAll(" " + regExp + "\\.$", "").trim();
            }
        }

        result[0] = text;
        result[1] = offsetChange;

        return result;
    }

    public KnowledgeBaseCommunicatorInterface getKbCommunicator() {
        return kbCommunicator;
    }

    public void setKbCommunicator(KnowledgeBaseCommunicatorInterface kbCommunicator) {
        this.kbCommunicator = kbCommunicator;
    }

    private void demo(String optionValue) {
        LOGGER.info(tag(
                "Homer Simpson likes to travel through his hometown Springfield. His friends are Moe and Barney.",
        "data/models/tudnerdemo.model"));
    }

    public void analyzeContexts(String trainingFilePath) {

        Map<String, CountMap> contextMap = new TreeMap<String, CountMap>();
        leftContextMap = new CountMap();
        // rightContextMap = new TreeMap<String, CountMap>();
        CountMap tagCounts = new CountMap();

        // get all training annotations including their features
        Annotations annotations = FileFormatParser.getAnnotationsFromColumn(trainingFilePath);

        Instances<UniversalInstance> trainingInstances = new Instances<UniversalInstance>();

        // iterate over all annotations and analyze their left and right contexts for patterns
        for (Annotation annotation : annotations) {

            String tag = annotation.getInstanceCategoryName();

            // the left patterns containing 1-3 words
            String[] leftContexts = annotation.getLeftContexts();

            // the right patterns containing 1-3 words
            String[] rightContexts = annotation.getRightContexts();

            // initialize tagMap
            // if (rightContextMap.get(tag) == null) {
            // rightContextMap.put(tag, new CountMap());
            // }
            if (contextMap.get(tag) == null) {
                contextMap.put(tag, new CountMap());
            }

            // add the left contexts to the map
            contextMap.get(tag).increment(leftContexts[0]);
            contextMap.get(tag).increment(leftContexts[1]);
            contextMap.get(tag).increment(leftContexts[2]);

            leftContextMap.increment(leftContexts[0]);
            leftContextMap.increment(leftContexts[1]);
            leftContextMap.increment(leftContexts[2]);

            // add the right contexts to the map
            contextMap.get(tag).increment(rightContexts[0]);
            contextMap.get(tag).increment(rightContexts[1]);
            contextMap.get(tag).increment(rightContexts[2]);

            // rightContextMap.get(tag).increment(rightContexts[0]);
            // rightContextMap.get(tag).increment(rightContexts[1]);
            // rightContextMap.get(tag).increment(rightContexts[2]);

            tagCounts.increment(tag);

            UniversalInstance trainingInstance = new UniversalInstance(trainingInstances);
            trainingInstance.setTextFeature(annotation.getLeftContext() + "__" + annotation.getRightContext());
            trainingInstance.setInstanceCategory(tag);
            trainingInstances.add(trainingInstance);
        }

        contextClassifier.setTrainingInstances(trainingInstances);
        contextClassifier.train();

        StringBuilder csv = new StringBuilder();
        for (Entry<String, CountMap> entry : contextMap.entrySet()) {

            int tagCount = tagCounts.get(entry.getKey());
            CountMap patterns = contextMap.get(entry.getKey());
            LinkedHashMap<Object, Integer> sortedMap = patterns.getSortedMap();

            csv.append(entry.getKey()).append("###").append(tagCount).append("\n");

            // print the patterns and their count for the current tag
            for (Entry<Object, Integer> patternEntry : sortedMap.entrySet()) {
                if (patternEntry.getValue() > 0) {
                    csv.append(patternEntry.getKey()).append("###").append(patternEntry.getValue()).append("\n");
                }
            }

            csv.append("++++++++++++++++++++++++++++++++++\n\n");
        }

        // tagMap to matrix
        for (Entry<String, CountMap> patternEntry : contextMap.entrySet()) {

            for (Entry<Object, Integer> tagEntry : patternEntry.getValue().entrySet()) {
                patternProbabilityMatrix.set(patternEntry.getKey(), tagEntry.getKey().toString().toLowerCase(),
                        tagEntry.getValue());
            }

        }

        FileHelper.writeToFile("data/temp/tagPatternAnalysis.csv", csv);
    }

    public void analyzeSentenceStarts(String trainingFilePath) {

        // get all training annotations including their features
        Annotations annotations = FileFormatParser.getAnnotationsFromColumn(trainingFilePath);

        CountMap posTagCounts = new CountMap();

        LingPipePOSTagger lpt = new LingPipePOSTagger();
        lpt.loadModel();

        for (Annotation annotation : annotations) {

            boolean startOfSentence = Boolean.valueOf(annotation.getNominalFeatures().get(0));

            if (startOfSentence) {

                String[] rightContextParts = annotation.getRightContext().split(" ");

                if (rightContextParts.length == 0) {
                    continue;
                }

                // TagAnnotations ta = lpt.tag(annotation.getEntity()).getTagAnnotations();
                // if (ta.size() == 1 && ta.get(0).getTag().indexOf("NP") == -1 && ta.get(0).getTag().indexOf("NN") ==
                // -1
                // && ta.get(0).getTag().indexOf("JJ") == -1 && ta.get(0).getTag().indexOf("UH") == -1) {
                // continue;
                // }

                if (annotation.getEntity().indexOf(" ") > -1) {
                    continue;
                }

                TagAnnotations ta = lpt.tag(rightContextParts[0]).getTagAnnotations();
                posTagCounts.increment(ta.get(0).getTag());

                System.out.println("--------------------");
                System.out.print(annotation.getEntity());
                System.out.print(" | " + rightContextParts[0]);
                System.out.println(" | " + ta.get(0).getTag());

            }

        }

        CollectionHelper.print(posTagCounts.getSortedMap());

    }

    public static void split() {
        String s = FileHelper.readFileToString("data/datasets/ner/conll/test_validation_t.txt");
        s = s.replace("=-DOCSTART-", "\n\n");
        FileHelper.writeToFile("data/datasets/ner/conll/test_validation_t_readable.txt", s);
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void setTrainingMode(TrainingMode trainingMode) {
        this.trainingMode = trainingMode;
        if (trainingMode == TrainingMode.Incomplete) {
            removeDates = true;
            removeDateEntries = true;
            removeIncorrectlyTaggedInTraining = false;
            removeWrongEntityBeginnings = false;
            removeSentenceStartErrors = false;
            removeSentenceStartErrors2 = true;
            removeSingleNonNounEntities = false;
            switchTagAnnotationsUsingPatterns = true;
            switchTagAnnotationsUsingDictionary = true;
            unwrapEntities = true;
            unwrapEntitiesWithContext = true;
            retraining = false;
        }
    }

    public TrainingMode getTrainingMode() {
        return trainingMode;
    }

    /**
     * @param args
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) {

        // // training can be done with NERCLearner also
        TUDNER tagger = new TUDNER();

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

                    String taggedText = tagger.tag(cmd.getOptionValue("inputText"), cmd.getOptionValue("configFile"));

                    if (cmd.hasOption("outputFile")) {
                        FileHelper.writeToFile(cmd.getOptionValue("outputFile"), taggedText);
                    } else {
                        System.out.println("No output file given so tagged text will be printed to the console:");
                        System.out.println(taggedText);
                    }

                } else if (cmd.hasOption("train")) {

                    tagger.train(cmd.getOptionValue("trainingFile"), cmd.getOptionValue("configFile"));

                } else if (cmd.hasOption("evaluate")) {

                    tagger.evaluate(cmd.getOptionValue("trainingFile"), cmd.getOptionValue("configFile"),
                            TaggingFormat.XML);

                } else if (cmd.hasOption("demo")) {

                    tagger.demo(cmd.getOptionValue("inputText"));

                }

            } catch (ParseException e) {
                LOGGER.debug("Command line arguments could not be parsed!");
                formatter.printHelp("FeedChecker", options);
            }

        }

        // ################################# HOW TO USE #################################
        // HashSet<String> trainingTexts = new HashSet<String>();
        // trainingTexts.add("Australia is a country and a continent at the same time. New Zealand is also a country but not a continent");
        // trainingTexts
        // .add("Many countries, such as Germany and Great Britain, have a strong economy. Other countries such as Iceland and Norway are in the north and have a smaller population");
        // trainingTexts.add("In south Europe, a nice country named Italy is formed like a boot.");
        // trainingTexts.add("In the western part of Europe, the is a country named Spain which is warm.");
        // trainingTexts.add("Bruce Willis is an actor, Jim Carrey is an actor too, but Trinidad is a country name and and actor name as well.");
        // trainingTexts.add("In west Europe, a warm country named Spain has good seafood.");
        // trainingTexts.add("Another way of thinking of it is to drive to another coutry and have some fun.");
        //
        // // set the kb communicator that knows the entities
        // nercer.setKbCommunicator(new TestKnowledgeBaseCommunicator());

        // possible tags
        // CollectionHelper.print(tagger.getModelTags("data/models/tudner/tudner.model"));

        // train
        // tagger.train("data/datasets/ner/sample/trainingColumn.tsv", "data/models/tudner/tudner.model");

        // tag
        // tagger.loadModel("data/models/tudner/tudner.model");
        // tagger.tag("John J. Smith and the Nexus One location iphone 4 mention Seattle in the text John J. Smith lives in Seattle.");

        // tagger.tag(
        // "John J. Smith and the Nexus One location iphone 4 mention Seattle in the text John J. Smith lives in Seattle.",
        // "data/models/tudner/tudner.model");

        // evaluate
        // tagger.evaluate("data/datasets/ner/sample/testingColumn.tsv", "data/models/tudner/tudner.model",
        // TaggingFormat.COLUMN);

        // /////////////////////////// train and test /////////////////////////////
        // tagger.train("data/datasets/ner/politician/text/training.tsv", "data/models/tudner/tudner.model");
        // EvaluationResult er = tagger.evaluate("data/datasets/ner/politician/text/testing.tsv",
        // "data/models/tudner/tudner.model", TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

        LOGGER.setLevel(Level.DEBUG);

        // using a column trainig and testing file
        StopWatch stopWatch = new StopWatch();
        tagger.setMode(Mode.English);
        // tagger.setTrainingMode(TrainingMode.Incomplete);
        tagger.setTrainingMode(TrainingMode.Complete);
        tagger.train("data/datasets/ner/conll/training.txt", "data/temp/tudner.model");
        // tagger.train("data/temp/nerEvaluation/www_eval_2_cleansed/allColumn.txt", "data/temp/tudner.model");

        // Annotations annotations =
        // FileFormatParser.getSeedAnnotations("data/datasets/ner/tud/manuallyPickedSeeds/seedListC.txt", 50);
        // tagger.train(annotations, "data/temp/tudner3.model");
        // tagger.train("data/temp/autoGeneratedDataTUD2/seedsTest2.txt", "data/temp/tudner2.model");
        // tagger.train("data/temp/autoGeneratedDataTUD2/seedsTest2.txt", annotations, "data/temp/tudner2.model");
        // System.exit(0);
        // TUDNER.remove = true;
        tagger.loadModel("data/temp/tudner.model");
        // System.exit(0);


        // tagger = TUDNER.load("data/temp/tudner.model");

        // EvaluationResult er = tagger.evaluate("data/datasets/ner/conll/test_validation.txt",
        // "data/temp/tudner.model",
        // TaggingFormat.COLUMN);
        EvaluationResult er = tagger.evaluate("data/datasets/ner/conll/test_final.txt", "", TaggingFormat.COLUMN);
        // EvaluationResult er = tagger.evaluate("data/datasets/ner/tud/tud2011_test.txt", "", TaggingFormat.COLUMN);
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